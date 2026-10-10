package com.example.woodrower.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Tachometer(
    currentSpeed: Float,
    averageSpeed: Float = 0f,
    minValue: Float = 0f,
    maxValue: Float = 10f,  // 10 m/s max for rowing
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2, height / 2)
            val radius = minOf(width, height) / 2 * 0.8f

            // Draw outer circle
            drawCircle(
                color = Color.LightGray,
                radius = radius,
                center = center,
                style = Stroke(width = 12f)
            )

            // Normalize values to 0-1 range
            val normalizedCurrent = (currentSpeed - minValue) / (maxValue - minValue).coerceAtLeast(0.01f)
            val normalizedAverage = (averageSpeed - minValue) / (maxValue - minValue).coerceAtLeast(0.01f)

            // Draw colored arc for current speed (0 at top, increases clockwise)
            val startAngle = 270f
            val sweepAngle = normalizedCurrent * 360f

            drawArc(
                color = when {
                    currentSpeed < 2 -> Color.Green
                    currentSpeed < 5 -> Color(0xFFFFA500)
                    else -> Color.Red
                },
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 12f)
            )

            // Draw scale ticks (0 at top, increasing clockwise)
            // 11 ticks: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 m/s
            repeat(11) { i ->
                val angle = startAngle + (i * 36f)
                val tickLength = if (i % 2 == 0) 20f else 10f

                val outerX = center.x + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
                val outerY = center.y + radius * sin(Math.toRadians(angle.toDouble())).toFloat()
                val innerX = center.x + (radius - tickLength) * cos(Math.toRadians(angle.toDouble())).toFloat()
                val innerY = center.y + (radius - tickLength) * sin(Math.toRadians(angle.toDouble())).toFloat()

                drawLine(
                    color = Color.Black,
                    start = Offset(outerX, outerY),
                    end = Offset(innerX, innerY),
                    strokeWidth = 3f
                )
            }

            // Draw pointer for CURRENT speed (red needle)
            val currentPointerAngle = startAngle + (normalizedCurrent * 360f)
            val pointerLength = radius * 0.7f
            val currentPointerEndX =
                center.x + pointerLength * cos(Math.toRadians(currentPointerAngle.toDouble())).toFloat()
            val currentPointerEndY =
                center.y + pointerLength * sin(Math.toRadians(currentPointerAngle.toDouble())).toFloat()

            drawLine(
                color = Color.Red,
                start = center,
                end = Offset(currentPointerEndX, currentPointerEndY),
                strokeWidth = 8f
            )

            // Draw pointer for AVERAGE speed (blue needle, slightly shorter)
            val averagePointerAngle = startAngle + (normalizedAverage * 360f)
            val averagePointerLength = radius * 0.5f
            val averagePointerEndX =
                center.x + averagePointerLength * cos(Math.toRadians(averagePointerAngle.toDouble())).toFloat()
            val averagePointerEndY =
                center.y + averagePointerLength * sin(Math.toRadians(averagePointerAngle.toDouble())).toFloat()

            drawLine(
                color = Color.Blue,
                start = center,
                end = Offset(averagePointerEndX, averagePointerEndY),
                strokeWidth = 6f
            )

            // Draw center circle
            drawCircle(
                color = Color.Red,
                radius = 15f,
                center = center
            )
        }

        // Draw scale labels using Text composables positioned around the circle
        // 0 at top (270 degrees), 2 at 324, 4 at 36, 6 at 108, 8 at 180, 10 at 252
        val labelRadius = 110f
        val centerX = 100f
        val centerY = 100f

        // Label positions (0 at top, then every 72 degrees clockwise)
        listOf(0, 2, 4, 6, 8, 10).forEachIndexed { idx, value ->
            val angle = 270f + (idx * 72f)
            val labelX = centerX + labelRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val labelY = centerY + labelRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

            Text(
                text = value.toString(),
                fontSize = 12.sp,
                color = Color.Black,
                modifier = Modifier.offset {
                    IntOffset(
                        (labelX - centerX).toInt(),
                        (labelY - centerY).toInt()
                    )
                }
            )
        }

        // Draw unit label at bottom
        Text(
            text = "m/s",
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
