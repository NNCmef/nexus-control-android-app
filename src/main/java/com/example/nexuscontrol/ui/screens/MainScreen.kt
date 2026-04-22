package com.example.nexuscontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexuscontrol.network.Device
import com.example.nexuscontrol.viewmodel.NexusViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.delay

@Composable
fun MainScreen(viewModel: NexusViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val items = listOf("СЕТЬ", "МЕТРИКИ", "ТЕРМИНАЛ")
    val icons = listOf(Icons.Filled.List, Icons.Filled.Info, Icons.Filled.Build)

    // Периодическое обновление терминала
    LaunchedEffect(viewModel.selectedDeviceId, selectedTab) {
        if (selectedTab == 2 && viewModel.selectedDeviceId != null) {
            while (true) {
                delay(2000)
                viewModel.pollTerminal(viewModel.selectedDeviceId!!)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchDevices()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onBackground) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item, tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color(0xFFA0A0A0)) },
                        label = { Text(item, fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF333333), selectedTextColor = Color.White, unselectedTextColor = Color(0xFFA0A0A0))
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (selectedTab) {
                0 -> DevicesScreen(viewModel)
                1 -> MetricsScreen(viewModel)
                2 -> TerminalScreen(viewModel)
            }
            
            viewModel.globalError?.let {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).background(Color(0xFFFF5252), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Text(it, color = Color.White, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DevicesScreen(viewModel: NexusViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ваши устройства", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(viewModel.devices) { device ->
                DeviceCard(
                    device = device, 
                    isSelected = viewModel.selectedDeviceId == device.id,
                    onClick = { viewModel.selectedDeviceId = device.id }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(device: Device, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A), shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(10.dp), shape = androidx.compose.foundation.shape.CircleShape, color = if (device.active) MaterialTheme.colorScheme.primary else Color(0xFFFF5252)) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = device.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp))
                }
                Text(text = device.id, style = MaterialTheme.typography.bodySmall, color = Color(0xFFA0A0A0))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Активность: ${device.lastSeen}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA0A0A0))
        }
    }
}


@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFA0A0A0), fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp))
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp))
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp))
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun MetricsScreen(viewModel: NexusViewModel) {
    LaunchedEffect(viewModel.selectedDeviceId) {
        if(viewModel.selectedDeviceId != null) {
            viewModel.fetchMetrics(viewModel.selectedDeviceId!!)
        }
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(scrollState)) {
        Spacer(modifier = Modifier.height(16.dp))
        val selectedName = viewModel.devices.find { it.id == viewModel.selectedDeviceId }?.name ?: "Не выбрано"
        Text("Устройство: ${selectedName}", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFA0A0A0))
        Spacer(modifier = Modifier.height(24.dp))
        
        // CPU
        SectionCard("Процессор (CPU)") {
            InfoRow("Модель", viewModel.cpuModel)
            InfoRow("Частота", viewModel.cpuFreq)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (viewModel.cpuMetrics.isNotEmpty()) {
                    Chart(chart = lineChart(), model = entryModelOf(*viewModel.cpuMetrics.toTypedArray()), startAxis = rememberStartAxis(), bottomAxis = rememberBottomAxis())
                } else {
                    Text("Ожидание данных...", color = Color(0xFFA0A0A0), modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        // RAM
        SectionCard("Оперативная память (RAM)") {
            InfoRow("Объем", viewModel.ramTotal)
            InfoRow("Частота", viewModel.ramSpeed)
            InfoRow("Модель", viewModel.ramModel)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (viewModel.ramMetrics.isNotEmpty()) {
                    Chart(chart = lineChart(), model = entryModelOf(*viewModel.ramMetrics.toTypedArray()), startAxis = rememberStartAxis(), bottomAxis = rememberBottomAxis())
                } else {
                    Text("Ожидание данных...", color = Color(0xFFA0A0A0), modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        // DISK
        SectionCard("Накопители (Disk)") {
            InfoRow("Диски", viewModel.diskModels)
            Spacer(modifier = Modifier.height(8.dp))
            viewModel.diskInfoList.forEach { disk ->
                val dev = disk["device"]?.toString() ?: ""
                val total = disk["total_gb"]?.toString() ?: "0"
                val used = disk["used_gb"]?.toString() ?: "0"
                val perc = disk["percent"]?.toString() ?: "0"
                InfoRow("Раздел ${dev}", "Занято: ${used} ГБ из ${total} ГБ (${perc}%)")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (viewModel.diskChartSeries.isNotEmpty()) {
                    val entriesList = viewModel.diskChartSeries.map { series ->
                        series.mapIndexed { index, value -> com.patrykandpatrick.vico.core.entry.FloatEntry(index.toFloat(), value.toFloat()) }
                    }
                    if (entriesList.isNotEmpty() && entriesList.all { it.isNotEmpty() }) {
                        val producer = com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer(entriesList)
                        Chart(chart = lineChart(), chartModelProducer = producer, startAxis = rememberStartAxis(), bottomAxis = rememberBottomAxis())
                    } else {
                        Text("Ошибка данных графика", color = Color.Red, modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    Text("Ожидание данных...", color = Color(0xFFA0A0A0), modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: NexusViewModel) {
    val scrollState = rememberScrollState()
    
    LaunchedEffect(viewModel.terminalOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val selectedName = viewModel.devices.find { it.id == viewModel.selectedDeviceId }?.name ?: "Не выбрано"
        Text("Терминал устройства: $selectedName", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFA0A0A0))
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Output Terminal
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(15.dp).verticalScroll(scrollState)) {
                     Text(
                         text = viewModel.terminalOutput,
                         fontFamily = FontFamily.Monospace,
                         color = MaterialTheme.colorScheme.primary,
                         fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)
                     )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Line
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = viewModel.terminalInput,
                        onValueChange = { viewModel.terminalInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Введите команду (например: dir, whoami)...", color = Color(0xFFA0A0A0), maxLines = 1) },
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                            focusedBorderColor = Color(0xFF444444),
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { viewModel.selectedDeviceId?.let { viewModel.sendCommand(it) } },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.height(55.dp)
                    ) {
                        Text("Выполнить", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
