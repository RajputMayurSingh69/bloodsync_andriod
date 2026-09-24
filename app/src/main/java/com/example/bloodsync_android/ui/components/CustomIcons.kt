package com.example.bloodsync_android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bloodsync_android.ui.theme.BloodRedPrimary

@Composable
fun BloodDropIcon(
    modifier: Modifier = Modifier,
    tint: Color = BloodRedPrimary,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height

        val path = Path().apply {
            moveTo(width / 2f, 0f)
            // Left curve downwards
            cubicTo(
                width * 0.45f, height * 0.25f,
                0f, height * 0.55f,
                0f, height * 0.72f
            )
            // Bottom circular arc
            cubicTo(
                0f, height * 0.95f,
                width * 0.2f, height,
                width / 2f, height
            )
            // Right circular arc
            cubicTo(
                width * 0.8f, height,
                width, height * 0.95f,
                width, height * 0.72f
            )
            // Right curve upwards to the peak
            cubicTo(
                width, height * 0.55f,
                width * 0.55f, height * 0.25f,
                width / 2f, 0f
            )
            close()
        }
        drawPath(path = path, color = tint, style = Fill)

        // Draw glossy reflection on top-left of the drop
        val highlightPath = Path().apply {
            moveTo(width * 0.35f, height * 0.35f)
            cubicTo(
                width * 0.22f, height * 0.52f,
                width * 0.20f, height * 0.68f,
                width * 0.25f, height * 0.75f
            )
        }
        drawPath(
            path = highlightPath,
            color = Color.White.copy(alpha = 0.45f),
            style = Stroke(width = width * 0.08f)
        )
    }
}

@Composable
fun MedicalCrossIcon(
    modifier: Modifier = Modifier,
    tint: Color = BloodRedPrimary,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val thickness = w * 0.30f
        val corner = thickness * 0.2f

        // Vertical bar
        drawRoundRect(
            color = tint,
            topLeft = Offset((w - thickness) / 2f, 0f),
            size = Size(thickness, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )
        // Horizontal bar
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, (h - thickness) / 2f),
            size = Size(w, thickness),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )
    }
}
