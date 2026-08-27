package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.models.EdgeModel
import com.example.edgeaicore.core.models.ModelRegistry
import com.example.edgeaicore.core.models.ModelStatus
import com.example.edgeaicore.core.models.ProvisioningProgress
import com.example.edgeaicore.core.models.ProvisioningStage
import com.example.edgeaicore.core.models.calculateSha256
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModelProvisioningTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        edgeAI = EdgeAICore.getInstance(context)
    }

    @Test
    fun testModelRegistryContainsValidManifests() {
        val models = ModelRegistry.DEFAULT_MODELS
        assertTrue("Model registry must not be empty", models.isNotEmpty())

        for (model in models) {
            assertTrue("Model id must be non-blank", model.id.isNotBlank())
            assertTrue("Model name must be non-blank", model.name.isNotBlank())
            assertTrue("Size must be positive", model.sizeBytes > 0)
            assertTrue("Checksum must be a valid non-empty string", model.checksum.isNotBlank())
            assertTrue("Download URL must be a valid URL", model.downloadUrl.startsWith("http://") || model.downloadUrl.startsWith("https://"))
        }
    }

    @Test
    fun testStreamingSha256CalculationMatchesExpectedHash() {
        val testFile = File(context.cacheDir, "test_sha256.bin")
        val sampleData = "SWAYAM_SOVEREIGN_ON_DEVICE_EDGE_AI_TEST_PAYLOAD".toByteArray(Charsets.UTF_8)
        FileOutputStream(testFile).use { it.write(sampleData) }

        val md = MessageDigest.getInstance("SHA-256")
        val expectedHash = md.digest(sampleData).joinToString("") { "%02x".format(it) }

        val calculatedHash = calculateSha256(testFile)
        assertEquals("Calculated streaming SHA-256 hash must match expected digest", expectedHash, calculatedHash)
        testFile.delete()
    }

    @Test
    fun testLocalModelManagerAtomicInstallation() {
        runBlocking {
            val testModelId = "test-model-atomic"
            val downloadDir = File(context.cacheDir, "edge_models_tmp").apply { mkdirs() }

            val downloadFile = File(downloadDir, "$testModelId.download")
            val sampleData = "NEURAL_WEIGHTS_ATOMIC_INSTALL_TEST".toByteArray(Charsets.UTF_8)
            FileOutputStream(downloadFile).use { it.write(sampleData) }

            val expectedSha = calculateSha256(downloadFile)

            val installResult = edgeAI.models.manager.installModel(
                modelId = testModelId,
                sourceFile = downloadFile,
                expectedSha256 = expectedSha
            )

            assertTrue("Atomic installation must succeed: ${(installResult as? EdgeResult.Failure)?.error}", installResult is EdgeResult.Success<*>)
            assertFalse("Source download file must be cleaned up", downloadFile.exists())

            val installedModel = (installResult as EdgeResult.Success<EdgeModel>).data
            assertNotNull("Installed model metadata should not be null", installedModel)
            val installedFile = File(installedModel.localPath ?: "")
            assertTrue("Destination model file must exist at ${installedFile.absolutePath}", installedFile.exists())
            assertEquals("File size must match sample data", sampleData.size.toLong(), installedFile.length())

            installedFile.delete()
        }
    }

    @Test
    fun testProvisioningStateInitializesCorrectly() {
        val progress = edgeAI.provisioning.progress.value
        assertNotNull(progress)
        assertTrue(progress.stage in ProvisioningStage.values())
    }
}
