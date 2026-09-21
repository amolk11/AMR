package com.amol.amr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amol.amr.communication.UsbConnectionState
import com.amol.amr.navigation.NavigationCommand
import com.amol.amr.util.PerformanceMetrics

@Composable
fun TelemetryPanel(
    modifier: Modifier = Modifier,
    command: NavigationCommand?,
    metrics: PerformanceMetrics,
    usbState: UsbConnectionState,
    isAutonomous: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header
            Text(
                text = "AMR TELEMETRY DASHBOARD",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Yellow,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Navigation Commands
            val steer = command?.steering?.let { Math.round(it) } ?: 0
            val speed = command?.speed ?: 0.0f
            val reason = command?.reason ?: "STANDBY"
            val isStop = command?.emergencyStop == true

            val statusColor = if (isStop) Color.Red else Color.Green

            Text(
                text = "CMD: ${if (isStop) "STOP" else "STEER: $steer° | SPD: ${String.format(java.util.Locale.US, "%.2f", speed)}"}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "PLAN: $reason",
                fontSize = 11.sp,
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace
            )

            // Diagnostics
            val crowd = command?.diagnostics?.get("crowd_density") ?: "EMPTY"
            val peopleCount = command?.diagnostics?.get("people_count") ?: 0
            val riskCount = command?.diagnostics?.get("collision_risk_count") ?: 0

            Text(
                text = "CUSTOMERS: $peopleCount | CROWD: $crowd | RISK: $riskCount",
                fontSize = 11.sp,
                color = Color.Cyan,
                fontFamily = FontFamily.Monospace
            )

            // Performance Metrics
            Text(
                text = "FPS: ${String.format(java.util.Locale.US, "%.1f", metrics.cameraFps)} | INF: ${String.format(java.util.Locale.US, "%.0f", metrics.inferenceLatencyMs)}ms | TOTAL: ${String.format(java.util.Locale.US, "%.0f", metrics.totalPipelineLatencyMs)}ms",
                fontSize = 11.sp,
                color = Color(0xFF00E676),
                fontFamily = FontFamily.Monospace
            )

            // Hardware & Mode Status
            val usbColor = when (usbState) {
                UsbConnectionState.CONNECTED -> Color.Green
                UsbConnectionState.CONNECTING -> Color.Yellow
                else -> Color.Red
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "USB: ${usbState.name}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = usbColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "MODE: ${if (isAutonomous) "AUTONOMOUS (ACTIVE)" else "MANUAL/PAUSED"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAutonomous) Color.Green else Color(0xFFFF9800),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
