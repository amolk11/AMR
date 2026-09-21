package com.amol.amr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.amol.amr.camera.CameraManager
import com.amol.amr.pipeline.PerceptionPipeline
import com.amol.amr.ui.DashboardScreen

class MainActivity : ComponentActivity() {

    private lateinit var pipeline: PerceptionPipeline
    private var cameraManager: CameraManager? = null
    private var previewView: PreviewView? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupCamera()
        } else {
            Toast.makeText(this, "Camera permission is required for AMR navigation", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during robot operation
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        pipeline = PerceptionPipeline(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val uiState by pipeline.uiState.collectAsState()
                    val metrics by pipeline.performanceMonitor.metrics.collectAsState()

                    DashboardScreen(
                        uiState = uiState,
                        metrics = metrics,
                        onPreviewViewCreated = { view ->
                            previewView = view
                            checkAndStartCamera()
                        },
                        onToggleAutonomous = { enabled ->
                            pipeline.setAutonomousMode(enabled)
                        },
                        onToggleEmergencyStop = { active ->
                            pipeline.triggerEmergencyStop(active)
                        },
                        onConnectUsb = {
                            pipeline.connectUsb()
                        }
                    )
                }
            }
        }
    }

    private fun checkAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupCamera() {
        cameraManager = CameraManager(
            context = this,
            lifecycleOwner = this,
            onFrameAvailable = { frame ->
                pipeline.onNewCameraFrame(frame)
            }
        ).apply {
            startCamera(previewView?.surfaceProvider)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.shutdown()
        pipeline.release()
    }
}
