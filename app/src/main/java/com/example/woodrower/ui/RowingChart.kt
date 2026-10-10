package com.example.woodrower.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.woodrower.bluetooth.RowingData
import kotlin.math.max

@Composable
fun DistanceChart(
    data: List<RowingData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {}
        return
    }

    val maxDistance = data.maxOfOrNull { it.totalDistance } ?: 1f
    val minDistance = data.minOfOrNull { it.totalDistance } ?: 0f
    val distanceRange = max(1f, maxDistance - minDistance)

    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val width = size.width
        val height = size.height

        // Draw grid
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Draw distance line (blue)
        val distancePoints = data.mapIndexed { index, rowingData ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val normalizedDistance = (rowingData.totalDistance - minDistance) / distanceRange
            val y = height - (normalizedDistance * height)
            Offset(x, y)
        }

        if (distancePoints.size > 1) {
            var path = androidx.compose.ui.graphics.Path()
            distancePoints.forEachIndexed { index, offset ->
                if (index == 0) path.moveTo(offset.x, offset.y)
                else path.lineTo(offset.x, offset.y)
            }
            drawPath(
                path = path,
                color = Color(0xFF2196F3),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun SpeedChart(
    data: List<RowingData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {}
        return
    }

    val speeds = data.map { it.speed }
    val maxSpeed = speeds.maxOfOrNull { it } ?: 1f
    val minSpeed = speeds.minOfOrNull { it } ?: 0f
    val speedRange = max(0.1f, maxSpeed - minSpeed)

    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val width = size.width
        val height = size.height

        // Draw grid
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Draw speed line (red)
        val speedPoints = data.mapIndexed { index, rowingData ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val normalizedSpeed = (rowingData.speed - minSpeed) / speedRange
            val y = height - (normalizedSpeed * height)
            Offset(x, y)
        }

        if (speedPoints.size > 1) {
            var path = androidx.compose.ui.graphics.Path()
            speedPoints.forEachIndexed { index, offset ->
                if (index == 0) path.moveTo(offset.x, offset.y)
                else path.lineTo(offset.x, offset.y)
            }
            drawPath(
                path = path,
                color = Color(0xFFE91E63),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun RowingChart(
    data: List<RowingData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Speed chart in a box
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Speed (m/s)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE91E63)
                )
                SpeedChart(
                    data = data,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }

        // Distance chart in a box
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Distance (m)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2196F3)
                )
                DistanceChart(
                    data = data,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }
    }
}
