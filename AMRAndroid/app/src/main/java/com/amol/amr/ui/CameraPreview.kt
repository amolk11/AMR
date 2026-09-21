package com.amol.amr.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.viewinterop.AndroidView
import com.amol.amr.navigation.PersonDetection
import com.amol.amr.navigation.PersonState

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    detections: List<PersonDetection>,
    personStates: List<PersonState>,
    onPreviewViewCreated: (PreviewView) -> Unit
) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    onPreviewViewCreated(this)
                }
            }
        )

        // Overlay Canvas for Bounding Boxes, Zones, and Predicted Vectors
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Navigation Zone Boundaries (Yellow lines)
            val leftX = canvasW / 3.0f
            val rightX = 2.0f * canvasW / 3.0f

            drawLine(
                color = Color.Yellow.copy(alpha = 0.6f),
                start = Offset(leftX, 0f),
                end = Offset(leftX, canvasH),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Yellow.copy(alpha = 0.6f),
                start = Offset(rightX, 0f),
                end = Offset(rightX, canvasH),
                strokeWidth = 2f
            )

            // 2. Draw Person Bounding Boxes & Trajectories
            for (det in detections) {
                val scaleX = canvasW / 640.0f
                val scaleY = canvasH / 480.0f

                val x1 = det.x1 * scaleX
                val y1 = det.y1 * scaleY
                val w = (det.x2 - det.x1) * scaleX
                val h = (det.y2 - det.y1) * scaleY

                drawRect(
                    color = Color.Green,
                    topLeft = Offset(x1, y1),
                    size = Size(w, h),
                    style = Stroke(width = 3f)
                )
            }

            for (p in personStates) {
                val scaleX = canvasW / 640.0f
                val scaleY = canvasH / 480.0f

                val cx = p.cx * scaleX
                val cy = p.cy * scaleY
                val px = p.predX * scaleX
                val py = p.predY * scaleY

                val vectorColor = if (p.isPredictedInPath) Color.Red else Color.Cyan

                // Movement Vector
                drawLine(
                    color = vectorColor,
                    start = Offset(cx, cy),
                    end = Offset(px, py),
                    strokeWidth = 3f
                )

                // Center Dot
                drawCircle(
                    color = Color.Yellow,
                    radius = 5f,
                    center = Offset(cx, cy)
                )

                // Predicted Point Dot
                drawCircle(
                    color = vectorColor,
                    radius = 7f,
                    center = Offset(px, py)
                )
            }
        }
    }
}
