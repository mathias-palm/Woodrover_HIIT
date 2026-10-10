package com.example.woodrower

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import android.widget.FrameLayout
import com.example.woodrower.BuildConfig
import com.example.woodrower.bluetooth.CorrectedReading
import com.example.woodrower.bluetooth.RowingBluetoothManager
import com.example.woodrower.bluetooth.RowingData
import com.example.woodrower.ui.DistanceChart
import com.example.woodrower.ui.SpeedLineChart
import com.example.woodrower.ui.CadenceLineChart
import com.example.woodrower.ui.AverageCadenceLineChart
import com.example.woodrower.ui.PowerLineChart
import com.example.woodrower.ui.ResistanceLineChart
import com.example.woodrower.ui.DistanceLineChart
import java.io.File
import com.example.woodrower.ui.RowingChart
import com.example.woodrower.ui.SpeedChart
import com.example.woodrower.ui.Tachometer
import com.example.woodrower.ui.theme.WoodrowerTheme

// Helper functions for formatting
fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

fun formatPace(speedMs: Float): String {
    if (speedMs <= 0f) return "N/A"
    val secondsPer500m = 500f / speedMs
    val minutes = (secondsPer500m / 60).toInt()
    val seconds = (secondsPer500m % 60).toInt()
    return String.format("%d:%02d / 500m", minutes, seconds)
}

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothManager: RowingBluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothManager = RowingBluetoothManager(applicationContext)

        setContent {
            WoodrowerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppContent(
                        modifier = Modifier.padding(innerPadding),
                        bluetoothManager = bluetoothManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        bluetoothManager.disconnect()
        super.onDestroy()
    }
}

@Composable
fun GraphsPage(
    bluetoothManager: RowingBluetoothManager,
    demoMode: Boolean
) {
    val data by bluetoothManager.data
    val history by bluetoothManager.history
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Use the existing MPAndroidChartWrappers
        SpeedLineChart(data = history)
        CadenceLineChart(data = history)
        AverageCadenceLineChart(data = history)
        PowerLineChart(data = history)
        ResistanceLineChart(data = history)
        DistanceLineChart(data = history)
    }
}

@Composable
fun AppContent(modifier: Modifier = Modifier, bluetoothManager: RowingBluetoothManager) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var demoMode by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        hasPermissions = permissionsMap.values.all { it }
    }

    LaunchedEffect(Unit) {
        hasPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }
    
    // Auto-start scanning when permissions are available and not in demo mode
    LaunchedEffect(hasPermissions, demoMode) {
        if (hasPermissions && !demoMode) {
            bluetoothManager.startAutoScan()
        } else {
            bluetoothManager.stopAutoScan()
        }
    }

    val isScanning by bluetoothManager.isScanning
    val isConnected by bluetoothManager.isConnected
    val isAutoScanning by bluetoothManager.isAutoScanning
    val status by bluetoothManager.status
    val data by bluetoothManager.data
    val history by bluetoothManager.history

    DisposableEffect(demoMode && isConnected) {
        if (demoMode && isConnected) {
            val demoData = listOf(
                RowingData(totalDistance = 100f, timestamp = System.currentTimeMillis() - 30000),
                RowingData(totalDistance = 250f, timestamp = System.currentTimeMillis() - 20000),
                RowingData(totalDistance = 400f, timestamp = System.currentTimeMillis() - 10000)
            )
            bluetoothManager.history.value = demoData
            bluetoothManager.data.value = demoData.last()
        }
        onDispose { }
    }
    


    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Status") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Graphs") }
            )
        }

        // Format build timestamp
        val buildTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd-HH:mm").format(
            java.util.Date(BuildConfig.BUILD_TIMESTAMP_MS)
        )

        when (selectedTab) {
            0 -> StatusPage(
                bluetoothManager = bluetoothManager,
                hasPermissions = hasPermissions,
                currentDemoMode = demoMode,
                onDemoModeChange = { newValue -> demoMode = newValue },
                appVersion = "1.0"
            )

            1 -> GraphsPage(
                bluetoothManager = bluetoothManager,
                demoMode = demoMode
            )
        }
    }
}

@Composable
fun StatusPage(
    bluetoothManager: RowingBluetoothManager,
    hasPermissions: Boolean,
    currentDemoMode: Boolean,
    onDemoModeChange: (Boolean) -> Unit,
    appVersion: String
) {
    val isScanning by bluetoothManager.isScanning
    val isConnected by bluetoothManager.isConnected
    val status by bluetoothManager.status
    val data by bluetoothManager.data
    val resistanceSchedule by bluetoothManager.resistanceScheduleState
    val currentResistance by bluetoothManager.currentResistance

    var resistanceInput by remember { mutableStateOf("") }
    var scheduleInput by remember { mutableStateOf("") }

    // Editable fields for manual entry from wood rower display
    var paceManual by remember { mutableStateOf("") }
    var cadenceManual by remember { mutableStateOf("") }
    var strokesManual by remember { mutableStateOf("") }
    var energyManual by remember { mutableStateOf("") }
    var distanceManual by remember { mutableStateOf("") }

    // Flash effect for resistance change
    var flashGreen by remember { mutableStateOf(false) }
    var lastResistanceValue by remember { mutableStateOf(currentResistance) }

    LaunchedEffect(currentResistance) {
        if (currentResistance != lastResistanceValue) {
            flashGreen = true
            lastResistanceValue = currentResistance
            delay(500)  // Flash for 500ms
            flashGreen = false
        }
    }

    // Get current orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Landscape layout - use Row with two columns
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(if (flashGreen) Modifier.background(Color.Green.copy(alpha = 0.3f)) else Modifier)
        ) {
            // Left column - main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        Text(
            text = "Woodrower - Domyos ROW-0658 $appVersion",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        // Connection Status
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                // Display all rowing data in requested order: Speed, AvgSpeed, Cadence, AvgCadence, Calories, Distance, Resistance, Strokes, Time
                Text(
                    text = "Speed: ${"%.2f".format(data.speed)} m/s",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Text(
                    text = "Avg Speed: ${"%.2f".format(data.averageSpeed)} m/s",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Text(
                    text = "Cadence: ${"%.1f".format(data.cadence)} spm",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Text(
                    text = "Avg Cadence: ${"%.1f".format(data.averageCadence)} spm",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Text(
                    text = "Calories: ${data.calories}",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Text(
                    text = "Distance: ${"%.1f".format(data.totalDistance)} m",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Text(
                    text = "Resistance: ${data.resistanceLevel}",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Button(
                    onClick = { bluetoothManager.increaseResistance() },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 32.dp, end = 32.dp),
                    enabled = isConnected
                ) {
                    Text("Increase Resistance")
                }
                Text(
                    text = "Strokes: ${data.totalStrokes}",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Text(
                    text = "Time: ${formatDuration(data.time)}",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Data Table
        Text(
            text = "Device Readings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Table header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Metric",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f).padding(8.dp)
                    )
                    Text(
                        text = "Device",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f).padding(8.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "Manual",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f).padding(8.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Stroke Rate row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stroke Rate",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Text(
                        text = "${data.cadence} spm",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    OutlinedTextField(
                        value = cadenceManual,
                        onValueChange = { cadenceManual = it },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        placeholder = { Text("spm") },
                        singleLine = true
                    )
                }

                // Average Stroke Rate row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Average Stroke Rate",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Text(
                        text = "${data.averageCadence} spm",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    OutlinedTextField(
                        value = cadenceManual,
                        onValueChange = { cadenceManual = it },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        placeholder = { Text("spm") },
                        singleLine = true
                    )
                }

                // Stroke Count row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stroke Count",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Text(
                        text = "${data.totalStrokes}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    OutlinedTextField(
                        value = strokesManual,
                        onValueChange = { strokesManual = it },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        placeholder = { Text("Enter") },
                        singleLine = true
                    )
                }

                // Power row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Power",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Text(
                        text = "${data.power} W",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    OutlinedTextField(
                        value = energyManual,
                        onValueChange = { energyManual = it },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        placeholder = { Text("Watts") },
                        singleLine = true
                    )
                }

                // Distance row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Text(
                        text = "${"%.1f".format(data.totalDistance)} m",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    OutlinedTextField(
                        value = distanceManual,
                        onValueChange = { distanceManual = it },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        placeholder = { Text("Enter") },
                        singleLine = true
                    )
                }

                // Calories row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calories",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Text(
                        text = "${data.calories} kcal",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    OutlinedTextField(
                        value = energyManual,
                        onValueChange = { energyManual = it },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        placeholder = { Text("kcal") },
                        singleLine = true
                    )
                }

                // Save button row - save device readings with manual corrections
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            bluetoothManager.saveCorrectedReading(
                                manualPace = paceManual.toFloatOrNull(),
                                manualCadence = cadenceManual.toFloatOrNull(),
                                manualStrokes = strokesManual.toIntOrNull(),
                                manualEnergy = energyManual.toIntOrNull(),
                                manualDistance = distanceManual.toFloatOrNull()
                            )
                            // Clear manual inputs after saving
                            paceManual = ""
                            cadenceManual = ""
                            strokesManual = ""
                            energyManual = ""
                            distanceManual = ""
                        },
                        enabled = isConnected
                    ) {
                        Text("Save Reading")
                    }
                }
            }
        }

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (isScanning) {
                        bluetoothManager.stopScan()
                    } else {
                        bluetoothManager.startScan()
                    }
                },
                enabled = hasPermissions
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(if (isScanning) "Stop Scan" else "Scan for Device")
            }

            if (isConnected) {
                Button(
                    onClick = { bluetoothManager.disconnect() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Disconnect")
                }

                Button(
                    onClick = {
                        bluetoothManager.startAutoRefresh(1)
                        bluetoothManager.resetSession()
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Start Session")
                }

                Button(
                    onClick = { bluetoothManager.clearCorrectedReadings() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Clear Saved Readings")
                }

                Button(
                    onClick = {
                        val file = bluetoothManager.saveCorrectedReadingsToFile()
                        if (file != null) {
                            // Show a toast or update status with full path
                            bluetoothManager.status.value = "Saved: ${file.absolutePath}"
                        } else {
                            bluetoothManager.status.value = "No readings to save"
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Export to File")
                }
            }

            Button(
                onClick = {
                    onDemoModeChange(!currentDemoMode)
                    if (!currentDemoMode) {
                        bluetoothManager.isConnected.value = true
                        bluetoothManager.status.value = "Demo Mode: Connected"
                        // Reset data on demo connection
                        bluetoothManager.resetSession()
                    } else {
                        bluetoothManager.isConnected.value = false
                        bluetoothManager.status.value = "Disconnected"
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(if (currentDemoMode) "Exit Demo Mode" else "Demo Mode (No Device)")
            }
        }

        Button(
            onClick = { 
                if (bluetoothManager.isAutoScanning.value) {
                    bluetoothManager.stopAutoScan()
                } else {
                    bluetoothManager.startAutoScan()
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(if (bluetoothManager.isAutoScanning.value) "Stop Auto-Scan" else "Start Auto-Scan")
        }

        // Saved Corrected Readings display
        val correctedReadings by bluetoothManager.correctedReadings
        val sessionStartTime by bluetoothManager.sessionStartTime
        if (correctedReadings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saved Readings (${correctedReadings.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Table header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text(
                            text = "Distance (m)",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f).padding(8.dp),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = "Stroke Rate (spm)",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f).padding(8.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    // Display last 5 saved readings (most recent first)
                    correctedReadings.takeLast(5).reversed().forEach { reading ->
                        val elapsedSeconds = if (sessionStartTime > 0) {
                            ((reading.timestamp - sessionStartTime) / 1000).toInt()
                        } else {
                            ((reading.timestamp - reading.deviceData.timestamp) / 1000).toInt()
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(elapsedSeconds),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                            Text(
                                text = "%.1f m".format(reading.correctedDistance),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "%.1f spm".format(reading.correctedCadence),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    if (correctedReadings.size > 5) {
                        Text(
                            text = "... and ${correctedReadings.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }
        }

        /*
        // Available reading files
        var readingFiles by remember { mutableStateOf(bluetoothManager.listReadingFiles()) }

        // Refresh file list when corrected readings change (user might have saved a new file)
        LaunchedEffect(correctedReadings.size) {
            readingFiles = bluetoothManager.listReadingFiles()
        }

        if (readingFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saved Files",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    readingFiles.forEach { file ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                            Button(
                                onClick = {
                                    val loaded = bluetoothManager.loadCorrectedReadingsFromFile(file)
                                    if (loaded != null) {
                                        bluetoothManager.correctedReadings.value = loaded
                                        bluetoothManager.status.value =
                                            "Loaded ${loaded.size} readings from ${file.name}"
                                    } else {
                                        bluetoothManager.status.value = "Failed to load ${file.name}"
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Load")
                            }
                        }
                    }
                }
            }
        }
        */

        Spacer(modifier = Modifier.height(16.dp))

        // Tachometer
        Text(
            text = "Current Speed",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        Tachometer(
            currentSpeed = data.speed,
            averageSpeed = data.averageSpeed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Time until next resistance change
        val timeUntilNextChange = bluetoothManager.getTimeUntilNextResistanceChange()
        if (timeUntilNextChange >= 0) {
            Text(
                text = "Next resistance change in: ${timeUntilNextChange}s",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        } else if (resistanceSchedule.isNotEmpty()) {
            Text(
                text = "All resistance changes completed",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resistance Control
        Text(
            text = "Resistance Control",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        /*
        // Direct resistance control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = resistanceInput,
                onValueChange = { resistanceInput = it },
                label = { Text("Resistance (1-15)") },
                placeholder = { Text("1-15") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    resistanceInput.toIntOrNull()?.let { level ->
                        if (level in 1..15) {
                            bluetoothManager.setResistance(level)
                            resistanceInput = ""
                        }
                    }
                },
                enabled = isConnected && resistanceInput.isNotEmpty()
            ) {
                Text("Set")
            }
        }
        */

        /*
        Spacer(modifier = Modifier.height(8.dp))

        // Schedule: Column 1 = time, Column 2 = resistance
        Text(
            text = "Auto Schedule: Enter time (seconds) and resistance (1-15) as: time:resistance (e.g., 60:5,120:10)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = scheduleInput,
                onValueChange = { scheduleInput = it },
                label = { Text("Schedule (time:resistance)") },
                placeholder = { Text("60:5,120:10") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    scheduleInput.split(",").forEach { entry ->
                        val parts = entry.split(":")
                        if (parts.size == 2) {
                            parts[0].toIntOrNull()?.let { seconds ->
                                parts[1].toIntOrNull()?.let { level ->
                                    if (seconds > 0 && level in 1..15) {
                                        bluetoothManager.addResistanceSchedule(seconds, level)
                                    }
                                }
                            }
                        }
                    }
                    scheduleInput = ""
                },
                enabled = isConnected && scheduleInput.isNotEmpty()
            ) {
                Text("Add")
            }
        }

        Button(
            onClick = { bluetoothManager.clearResistanceSchedule() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            enabled = isConnected
        ) {
            Text("Clear Schedule")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resistance Schedule Table
        Text(
            text = "Current Resistance Schedule",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (resistanceSchedule.isEmpty()) {
                    Text(
                        text = "No schedule entries. Add entries using the form above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    // Table header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Time (s)",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Resistance",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Table rows
                    resistanceSchedule.forEach { (time, level) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$time",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$level",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
        */

        Spacer(modifier = Modifier.weight(1f))

        if (!hasPermissions) {
            Text(
                text = "Waiting for Bluetooth permissions...",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    } // End of portrait layout Column
            
            // Right column - controls and additional info (landscape only)
            Column(
                modifier = Modifier
                    .width((LocalConfiguration.current.screenWidthDp / 2).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Resistance control section
                if (isConnected) {
                    Text(
                        text = "Quick Controls",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Current resistance display
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Current Resistance",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = currentResistance.toString(),
                                style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        } // End of Row (landscape layout)
    } else {
        // Portrait layout - original single column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
                .then(if (flashGreen) Modifier.background(Color.Green.copy(alpha = 0.3f)) else Modifier),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Woodrower - Domyos ROW-0658 $appVersion",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            // Connection Status
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Resistance: ${currentResistance}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                    Text(
                        text = "Time: ${formatDuration(data.time)}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
            
            // Add the rest of the status content here
            Spacer(modifier = Modifier.height(8.dp))
            
            // Basic device info for portrait
            Text(
                text = "Status: ${if (isConnected) "Connected" else "Disconnected"}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            // Display all rowing data in requested order: Speed, AvgSpeed, Cadence, AvgCadence, Calories, Distance, Resistance, Strokes, Time
            Text(
                text = "Speed: ${"%.2f".format(data.speed)} m/s",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Avg Speed: ${"%.2f".format(data.averageSpeed)} m/s",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Cadence: ${"%.1f".format(data.cadence)} spm",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Avg Cadence: ${"%.1f".format(data.averageCadence)} spm",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Calories: ${data.calories}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Distance: ${"%.1f".format(data.totalDistance)} m",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Resistance: ${data.resistanceLevel}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            Button(
                onClick = { bluetoothManager.increaseResistance() },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                enabled = isConnected
            ) {
                Text("Increase Resistance")
            }
            Text(
                text = "Strokes: ${data.totalStrokes}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Time: ${formatDuration(data.time)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    }




@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    val mockManager = RowingBluetoothManager(context)
    WoodrowerTheme {
        AppContent(bluetoothManager = mockManager)
    }
}
