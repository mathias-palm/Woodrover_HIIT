package com.example.woodrower.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.woodrower.bluetooth.RowingData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

// Custom LineChart that can be used with AndroidView
class CustomLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LineChart(context, attrs, defStyleAttr) {
    init {
        // Common chart configuration
        setDrawGridBackground(false)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        description.isEnabled = false
        
        // X-axis configuration
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.setDrawAxisLine(true)
        xAxis.granularity = 1f
        xAxis.labelCount = 10
        
        // Left Y-axis
        axisLeft.setDrawGridLines(true)
        axisLeft.setDrawAxisLine(true)
        axisLeft.isGranularityEnabled = true
        
        // Right Y-axis (disabled)
        axisRight.isEnabled = false
        
        // Legend
        legend.form = Legend.LegendForm.LINE
        legend.textSize = 12f
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
    }
}

// Helper function to create line data
fun createLineData(
    entries: List<Entry>,
    label: String,
    color: Int,
    lineWidth: Float = 2f,
    circleRadius: Float = 4f,
    drawCircles: Boolean = true,
    drawValues: Boolean = false
): LineData {
    val dataSet = LineDataSet(entries, label).apply {
        this.color = color
        this.lineWidth = lineWidth
        this.circleRadius = circleRadius
        this.setCircleColor(color)
        this.setDrawCircles(drawCircles)
        this.setDrawValues(drawValues)
        this.mode = LineDataSet.Mode.CUBIC_BEZIER
        this.cubicIntensity = 0.2f
        this.setDrawFilled(false)
    }
    
    return LineData(dataSet)
}

// Helper function to convert RowingData list to entries for x-axis = row number
fun List<RowingData>.toEntriesForSpeed(): List<Entry> {
    return this.mapIndexed { index, data -> Entry(index.toFloat(), data.speed) }
}

fun List<RowingData>.toEntriesForCadence(): List<Entry> {
    return this.mapIndexed { index, data -> Entry(index.toFloat(), data.cadence) }
}

fun List<RowingData>.toEntriesForAverageCadence(): List<Entry> {
    return this.mapIndexed { index, data -> Entry(index.toFloat(), data.averageCadence) }
}

fun List<RowingData>.toEntriesForPower(): List<Entry> {
    return this.mapIndexed { index, data -> Entry(index.toFloat(), data.power.toFloat()) }
}

fun List<RowingData>.toEntriesForResistance(): List<Entry> {
    return this.mapIndexed { index, data -> Entry(index.toFloat(), data.resistanceLevel.toFloat()) }
}

fun List<RowingData>.toEntriesForDistance(): List<Entry> {
    return this.mapIndexed { index, data -> Entry(index.toFloat(), data.totalDistance) }
}

@Composable
fun SpeedLineChart(
    data: List<RowingData>,
    modifier: Modifier = Modifier
) {
    val chartData = remember(data) {
        createLineData(
            entries = data.toEntriesForSpeed(),
            label = "Speed (m/s)",
            color = Color.RED,
            lineWidth = 3f,
            circleRadius = 4f,
            drawCircles = true,
            drawValues = false
        )
    }
    
    ChartWrapper(
        chartData = chartData,
        title = "Speed (m/s)",
        modifier = modifier
    )
}

@Composable
fun CadenceLineChart(
    data: List<RowingData>,
    modifier: Modifier = Modifier
) {
    val chartData = remember(data) {
        createLineData(
            entries = data.toEntriesForCadence(),
            label = "Cadence (spm)",
            color = Color.BLUE,
            lineWidth = 3f,
            circleRadius = 4f,
            drawCircles = true,
            drawValues = false
        )
    }
    
    ChartWrapper(
        chartData = chartData,
        title = "Cadence (spm)",
        modifier = modifier
    )
}

@Composable
fun AverageCadenceLineChart(
    data: List<RowingData>,
    modifier: Modifier = Modifier
) {
    val chartData = remember(data) {
        createLineData(
            entries = data.toEntriesForAverageCadence(),
            label = "Average Cadence (spm)",
            color = Color.GREEN,
            lineWidth = 3f,
            circleRadius = 4f,
            drawCircles = true,
            drawValues = false
        )
    }
    
    ChartWrapper(
        chartData = chartData,
        title = "Average Cadence (spm)",
        modifier = modifier
    )
}

@Composable
fun PowerLineChart(
    data: List<RowingData>,
    modifier: Modifier = Modifier
) {
    val chartData = remember(data) {
        createLineData(
            entries = data.toEntriesForPower(),
            label = "Power (W)",
            color = Color.argb(255, 255, 165, 0), // Orange
            lineWidth = 3f,
            circleRadius = 4f,
            drawCircles = true,
            drawValues = false
        )
    }
    
    ChartWrapper(
        chartData = chartData,
        title = "Power (W)",
        modifier = modifier
    )
}

@Composable
fun ResistanceLineChart(
    data: List<RowingData>,
    modifier: Modifier = Modifier
) {
    val chartData = remember(data) {
        createLineData(
            entries = data.toEntriesForResistance(),
            label = "Resistance",
            color = Color.MAGENTA,
            lineWidth = 3f,
            circleRadius = 4f,
            drawCircles = true,
            drawValues = true
        )
    }
    
    ChartWrapper(
        chartData = chartData,
        title = "Resistance",
        modifier = modifier
    )
}

@Composable
fun DistanceLineChart(
    data: List<RowingData>,
    modifier: Modifier = Modifier
) {
    val chartData = remember(data) {
        createLineData(
            entries = data.toEntriesForDistance(),
            label = "Distance (m)",
            color = Color.CYAN,
            lineWidth = 3f,
            circleRadius = 4f,
            drawCircles = true,
            drawValues = false
        )
    }
    
    ChartWrapper(
        chartData = chartData,
        title = "Distance (m)",
        modifier = modifier
    )
}

@Composable
fun ChartWrapper(
    chartData: LineData,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )
        AndroidView(
            factory = { context ->
                CustomLineChart(context).apply {
                    this.data = chartData
                    xAxis.labelCount = minOf(10, chartData.entryCount)
                    if (chartData.yMin != chartData.yMax) {
                        axisLeft.axisMinimum = chartData.yMin - chartData.yMin * 0.1f
                        axisLeft.axisMaximum = chartData.yMax + chartData.yMax * 0.1f
                    } else {
                        axisLeft.axisMinimum = chartData.yMin - 1f
                        axisLeft.axisMaximum = chartData.yMax + 1f
                    }
                    invalidate()
                }
            },
            update = { chart ->
                chart.data = chartData
                chart.xAxis.labelCount = minOf(10, chartData.entryCount)
                if (chartData.yMin != chartData.yMax) {
                    chart.axisLeft.axisMinimum = chartData.yMin - chartData.yMin * 0.1f
                    chart.axisLeft.axisMaximum = chartData.yMax + chartData.yMax * 0.1f
                } else {
                    chart.axisLeft.axisMinimum = chartData.yMin - 1f
                    chart.axisLeft.axisMaximum = chartData.yMax + 1f
                }
                chart.notifyDataSetChanged()
                chart.invalidate()
            },
            modifier = Modifier.fillMaxWidth().height(250.dp)
        )
    }
}
