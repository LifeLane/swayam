package com.example.edgeaicore.core.mcp

import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Transport layer abstraction for Model Context Protocol (MCP).
 * Isolates transport specifics (In-Memory loopback, HTTP SSE, WebSockets) from logic.
 */
interface McpTransport {
    val isConnected: Boolean
    suspend fun connect(): EdgeResult<Unit>
    suspend fun disconnect()
    suspend fun send(request: McpJsonRpcRequest): EdgeResult<McpJsonRpcResponse>
    fun incomingNotifications(): Flow<McpJsonRpcRequest>
}

/**
 * In-Memory loopback transport for internal on-device MCP Server.
 */
class InMemoryMcpTransport(
    private val serverHandler: suspend (McpJsonRpcRequest) -> McpJsonRpcResponse
) : McpTransport {
    private var _connected = false
    private val _notifications = MutableSharedFlow<McpJsonRpcRequest>(extraBufferCapacity = 16)

    override val isConnected: Boolean get() = _connected

    override suspend fun connect(): EdgeResult<Unit> {
        _connected = true
        return EdgeResult.Success(Unit)
    }

    override suspend fun disconnect() {
        _connected = false
    }

    override suspend fun send(request: McpJsonRpcRequest): EdgeResult<McpJsonRpcResponse> {
        if (!_connected) {
            return EdgeResult.Failure(EdgeAIError.McpProtocolError("in-memory", "Transport not connected"))
        }
        return try {
            val response = serverHandler(request)
            EdgeResult.Success(response)
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeAIError.McpProtocolError("in-memory", e.message ?: "Execution failed"))
        }
    }

    override fun incomingNotifications(): Flow<McpJsonRpcRequest> = _notifications.asSharedFlow()
}

/**
 * HTTP/HTTPS SSE Transport for Private & Remote MCP Servers.
 */
class HttpSseMcpTransport(
    val endpoint: String,
    val authToken: String? = null
) : McpTransport {
    private var _connected = false
    private val _notifications = MutableSharedFlow<McpJsonRpcRequest>(extraBufferCapacity = 16)

    override val isConnected: Boolean get() = _connected

    override suspend fun connect(): EdgeResult<Unit> {
        return try {
            // Validate endpoint format
            if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                return EdgeResult.Failure(EdgeAIError.McpProtocolError(endpoint, "Invalid endpoint URI"))
            }
            _connected = true
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            _connected = false
            EdgeResult.Failure(EdgeAIError.McpProtocolError(endpoint, e.message ?: "Failed to connect to MCP server"))
        }
    }

    override suspend fun disconnect() {
        _connected = false
    }

    override suspend fun send(request: McpJsonRpcRequest): EdgeResult<McpJsonRpcResponse> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!_connected) {
            return@withContext EdgeResult.Failure(EdgeAIError.McpProtocolError(endpoint, "MCP Transport is not connected"))
        }
        try {
            val url = java.net.URL(endpoint)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (!authToken.isNullOrBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $authToken")
            }
            val requestJson = org.json.JSONObject().apply {
                put("jsonrpc", request.jsonrpc)
                put("id", request.id)
                put("method", request.method)
                request.params?.let { p ->
                    put("params", org.json.JSONObject(p))
                }
            }.toString()

            conn.outputStream.use { os ->
                os.write(requestJson.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = org.json.JSONObject(responseBody)
                val id = if (json.has("id")) json.getString("id") else request.id
                val result = if (json.has("result")) {
                    val rObj = json.optJSONObject("result")
                    val map = mutableMapOf<String, Any?>()
                    if (rObj != null) {
                        rObj.keys().forEach { k -> map[k] = rObj.get(k) }
                    } else {
                        map["value"] = json.get("result")
                    }
                    map
                } else emptyMap()
                EdgeResult.Success(McpJsonRpcResponse(id = id, result = result))
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                conn.disconnect()
                EdgeResult.Failure(EdgeAIError.McpProtocolError(endpoint, "Remote MCP error ($code): $errBody"))
            }
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeAIError.McpProtocolError(endpoint, e.message ?: "Transport failure"))
        }
    }

    override fun incomingNotifications(): Flow<McpJsonRpcRequest> = _notifications.asSharedFlow()
}
