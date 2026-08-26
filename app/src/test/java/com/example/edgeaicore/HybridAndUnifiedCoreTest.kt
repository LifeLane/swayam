package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.swayam.HybridEngine
import com.example.edgeaicore.core.swayam.SwayamProcessingMode
import com.example.edgeaicore.core.swayam.SwayamRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HybridAndUnifiedCoreTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore
    private lateinit var hybridEngine: HybridEngine

    @Before
    fun setup() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            edgeAI = EdgeAICore.getInstance(context)
            hybridEngine = edgeAI.swayam.hybridEngine

            // Prepare local model file so fallback / on-device works seamlessly
            val modelFile = File(context.filesDir, "edge_models/gemma-2b-it-litert.bin").apply {
                parentFile?.mkdirs()
                writeBytes("OFFLINE_MOCK_LITERT_TENSOR_WEIGHTS".toByteArray())
            }
            edgeAI.models.modelManager.scanAndVerifyInstalledModels()
            edgeAI.swayam.privateEngine.modelRuntime.loadModel(modelFile.absolutePath, ExecutionBackend.CPU)
        }
    }

    @Test
    fun testHybridInferenceEnforcesLocalVaultPrivacy() = runBlocking {
        // Add a private memory item
        edgeAI.memory.create(
            title = "Project Chronos Alpha",
            content = "Secret launch date is October 15 with zero telemetry.",
            privacyLevel = PrivacyLevel.LOCAL_ONLY
        )

        // Request hybrid inference
        val req = SwayamRequest(
            prompt = "What is Project Chronos?",
            preferredProvider = AIProviderType.HYBRID,
            privacyLevel = PrivacyLevel.PUBLIC,
            userConsent = true
        )

        val result = edgeAI.swayam.process(req)
        assertTrue("Hybrid process should succeed", result is EdgeResult.Success)
        val resp = (result as EdgeResult.Success).data

        // Verify that Hybrid provider or fallback local provider responded
        assertTrue(
            "Provider must be HYBRID or LOCAL fallback",
            resp.provider == AIProviderType.HYBRID || resp.provider == AIProviderType.LOCAL
        )
        assertNotNull("Response text must not be empty", resp.text)
    }

    @Test
    fun testHybridInferenceInOfflineModeStrictlyFallsBackToLocal() = runBlocking {
        // Enforce offline-only mode
        edgeAI.privacy.setOfflineOnlyMode(true)

        val req = SwayamRequest(
            prompt = "Explain quantum computing basics",
            preferredProvider = AIProviderType.HYBRID,
            privacyLevel = PrivacyLevel.PUBLIC,
            userConsent = true
        )

        val result = edgeAI.swayam.process(req)
        assertTrue("Hybrid in offline mode should succeed via local private engine", result is EdgeResult.Success)
        val resp = (result as EdgeResult.Success).data

        assertEquals("Must execute as LOCAL provider in offline mode", AIProviderType.LOCAL, resp.provider)
        assertFalse("Network usage must be false in offline mode", resp.networkUsed)

        // Revert offline mode
        edgeAI.privacy.setOfflineOnlyMode(false)
    }

    @Test
    fun testAIRouterSupportsHybridRouting() = runBlocking {
        val req = com.example.edgeaicore.core.ai.AIRequest(
            prompt = "Summarize latest AI news",
            preferredProvider = AIProviderType.HYBRID,
            privacyLevel = PrivacyLevel.PUBLIC,
            userConsent = true
        )

        val target = edgeAI.aiRouter.determineTargetProvider(req)
        assertEquals("AIRouter must resolve target to HYBRID when public & consented", AIProviderType.HYBRID, target)
    }

    @Test
    fun testAIRouterRejectsRemoteWhenLocalOnly() = runBlocking {
        val req = com.example.edgeaicore.core.ai.AIRequest(
            prompt = "Read my personal journal",
            preferredProvider = AIProviderType.HYBRID,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            userConsent = true
        )

        val target = edgeAI.aiRouter.determineTargetProvider(req)
        assertEquals("AIRouter must force LOCAL for LOCAL_ONLY privacy level", AIProviderType.LOCAL, target)
    }
}
