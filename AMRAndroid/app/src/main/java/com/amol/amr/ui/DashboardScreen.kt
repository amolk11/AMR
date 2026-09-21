package com.amol.amr.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.amol.amr.communication.UsbConnectionState
import com.amol.amr.navigation.PersonState
import com.amol.amr.pipeline.PipelineUiState
import com.amol.amr.util.PerformanceMetrics

@Composable
fun DashboardScreen(
    uiState: PipelineUiState,
    metrics: PerformanceMetrics,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onToggleAutonomous: (Boolean) -> Unit,
    onToggleEmergencyStop: (Boolean) -> Unit,
    onConnectUsb: () -> Unit
) {
    @Suppress("UNCHECKED_CAST")
    val personStates = (uiState.latestCommand?.diagnostics?.get("person_states") as? List<PersonState>) ?: emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Camera Feed with Overlays
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            detections = uiState.latestDetections,
            personStates = personStates,
            onPreviewViewCreated = onPreviewViewCreated
        )

        // 2. Top-Left Telemetry Overlay
        TelemetryPanel(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .widthIn(max = 380.dp),
            command = uiState.latestCommand,
            metrics = metrics,
            usbState = uiState.usbConnectionState,
            isAutonomous = uiState.isAutonomousActive
        )

        // 3. Top-Right USB Action Button
        if (uiState.usbConnectionState != UsbConnectionState.CONNECTED) {
            Button(
                onClick = onConnectUsb,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text(text = "Connect ESP32 USB", color = Color.White)
            }
        }

        // 4. Bottom Control Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Autonomous Run / Pause Toggle
            Button(
                onClick = { onToggleAutonomous(!uiState.isAutonomousActive) },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isAutonomousActive) Color(0xFFFF9800) else Color(0xFF2E7D32)
                )
            ) {
                Text(
                    text = if (uiState.isAutonomousActive) "PAUSE AUTO" else "START AUTONOMOUS",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            // Prominent Safety Emergency Stop
            EmergencyStopButton(
                modifier = Modifier.weight(1.5f),
                isStopActive = uiState.safetyState.isUserEmergencyStopActive,
                onToggleStop = onToggleEmergencyStop
            )
        }
    }
}
