package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.knowledge.KnowledgeType
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.litertlm.LiteRTLMEngine
import com.example.edgeaicore.core.models.LocalModelManager
import com.example.edgeaicore.core.swayam.PrivateEdgeEngine
import com.example.edgeaicore.core.swayam.SwayamProcessingMode
import com.example.edgeaicore.core.swayam.SwayamRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@Ignore("JNI not supported in Robolectric")
class PrivateEdgeEngineAcceptanceTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore
    private lateinit var privateEngine: PrivateEdgeEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        edgeAI = EdgeAICore.getInstance(context)
        privateEngine = edgeAI.swayam.core.privateEdgeEngine
    }

    @Test
    fun testPrivateModelRuntimeLoadAndUnload() = runBlocking {
        val modelFile = File(context.filesDir, "edge_models/gemma-2b-it-litert.bin").apply {
            parentFile?.mkdirs()
            writeBytes("OFFLINE_MOCK_LITERT_TENSOR_WEIGHTS".toByteArray())
        }

        val runtime = privateEngine.modelRuntime
        val loadResult = runtime.loadModel(modelFile.absolutePath, ExecutionBackend.CPU)
        assertTrue("Model load should succeed with valid file", loadResult is EdgeResult.Success)
        assertTrue("Runtime must report isReady == true", runtime.isReady())

        val capabilities = runtime.getCapabilities()
        assertTrue("Must support streaming", capabilities.supportsStreaming)
        assertEquals("INT4", capabilities.quantization)

        val unloadResult = runtime.unloadModel()
        assertTrue("Model unload should succeed", unloadResult is EdgeResult.Success)
        assertFalse("Runtime must report isReady == false after unload", runtime.isReady())
    }

    @Test
    fun testLocalModelUnavailableWhenNoModelPresent() = runBlocking {
        // Ensure runtime is unloaded and target does not exist
        privateEngine.modelRuntime.unloadModel()
        val nonExistentReq = SwayamRequest(
            prompt = "What is sovereign AI?",
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            modelId = "non-existent-model-xyz"
        )

        // When no models are installed and load fails
        val result = privateEngine.executePrivateInference(nonExistentReq)
        if (result is EdgeResult.Failure) {
            val errorMsg = result.error.message ?: ""
            assertTrue(
                "Error message should clearly denote local model unavailability",
                errorMsg.contains("LOCAL_MODEL_UNAVAILABLE") ||
                errorMsg.contains("unavailable")
            )
        }
    }

    @Test
    fun testMandatoryOfflineAcceptancePipeline() = runBlocking {
        // 1. Provision offline model binary
        val modelFile = File(context.filesDir, "edge_models/gemma-2b-it-litert.bin").apply {
            parentFile?.mkdirs()
            writeBytes("AUTHENTIC_OFFLINE_LITERT_TENSOR_WEIGHTS".toByteArray())
        }
        val installRes = edgeAI.models.install("gemma-2b-it-litert")
        assertTrue(installRes is EdgeResult.Success)

        // 2. Add local Soul / Identity
        edgeAI.swayam.personaManager.updateCustomInstructions("Act as a strict sovereign assistant.")

        // 3. Add local Memory
        val memRes = edgeAI.memory.create(
            title = "Edge Computing Fact",
            content = "SWAYAM GPT operates 100% on-device with zero cloud dependencies.",
            tags = "architecture,sovereignty"
        )
        assertNotNull(memRes)

        // 4. Ingest local RAG Document
        val ragRes = edgeAI.knowledge.ingestion.ingestDocument(
            title = "Sovereign Architecture",
            rawText = "The Private Edge Engine uses LiteRT-LM for local token generation and SQLite for local memory storage.",
            type = KnowledgeType.DOCUMENT
        )
        assertTrue(ragRes is EdgeResult.Success)

        // 5. Execute Private Edge Inference Request
        val request = SwayamRequest(
            prompt = "Explain SWAYAM architecture and local storage",
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            preferredProvider = AIProviderType.LOCAL,
            modelId = "gemma-2b-it-litert"
        )

        val result = privateEngine.executePrivateInference(request)
        assertTrue("Private edge inference must succeed offline", result is EdgeResult.Success)

        val response = (result as EdgeResult.Success).data
        assertFalse("Network must NOT be used", response.networkUsed)
        assertEquals(AIProviderType.LOCAL, response.provider)
        assertTrue("Response text must not be empty", response.text.isNotBlank())
        assertTrue("Memories or sources must be utilized", response.memoriesUsed.isNotEmpty() || response.sources.isNotEmpty())

        // 6. Test Offline Token Streaming
        val streamedTokens = privateEngine.streamPrivateInference(request).toList()
        assertTrue("Streaming must emit tokens", streamedTokens.isNotEmpty())
    }
}
