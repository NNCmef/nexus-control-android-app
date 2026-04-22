package com.example.nexuscontrol.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexuscontrol.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

class NexusViewModel : ViewModel() {
    val api = RetrofitClient.api

    // Auth State
    var username by mutableStateOf("admin")
    var password by mutableStateOf("admin")
    var authError by mutableStateOf<String?>(null)
    var isAuthLoading by mutableStateOf(false)

    // Main App State
    var devices by mutableStateOf<List<Device>>(emptyList())
    var selectedDeviceId by mutableStateOf<String?>(null)
    
    // Metrics State
    var cpuMetrics by mutableStateOf<List<Float>>(emptyList())
    var cpuModel by mutableStateOf("N/A")
    var cpuFreq by mutableStateOf("N/A")
    
    var ramMetrics by mutableStateOf<List<Float>>(emptyList())
    var ramTotal by mutableStateOf("N/A")
    var ramSpeed by mutableStateOf("N/A")
    var ramModel by mutableStateOf("N/A")
    
    var diskModels by mutableStateOf("N/A")
    var diskChartSeries by mutableStateOf<List<List<Float>>>(emptyList())
    var diskInfoList by mutableStateOf<List<Map<String, Any>>>(emptyList())
    
    // Terminal State
    var terminalOutput by mutableStateOf("Nexus Remote Shell Connected.\nReady.\n")
    var terminalInput by mutableStateOf("")
    var globalError by mutableStateOf<String?>(null) // Added global error for reporting

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isAuthLoading = true
            authError = null
            try {
                val res = api.login(LoginRequest(username, password))
                if (res.isSuccessful) {
                    // Accepts any 200 OK response now, bypassing strict payload checks
                    onSuccess()
                    fetchDevices()
                } else {
                    authError = "Authentication failed! Code: ${res.code()}"
                }
            } catch (e: Exception) {
                authError = "Network error: ${e.message}"
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun fetchDevices() {
        viewModelScope.launch {
            try {
                val res = api.getDevices()
                devices = res.devices
                if (devices.isNotEmpty() && selectedDeviceId == null) {
                    selectedDeviceId = devices[0].id
                }
                globalError = null
            } catch (e: Exception) {
                globalError = "Error fetching devices: ${e.message}"
            }
        }
    }

    fun fetchMetrics(deviceId: String) {
        viewModelScope.launch {
            try {
                val cpuDef = async { api.getMetrics("cpu_usage", deviceId) }
                val ramDef = async { api.getMetrics("ram_usage", deviceId) }
                val cpuModDef = async { api.getMetrics("cpu_model", deviceId) }
                val cpuFreqDef = async { api.getMetrics("cpu_freq", deviceId) }
                val ramTotDef = async { api.getMetrics("ram_total", deviceId) }
                val ramSpdDef = async { api.getMetrics("ram_speed", deviceId) }
                val ramModDef = async { api.getMetrics("ram_model", deviceId) }
                val dModDef = async { api.getMetrics("disk_models", deviceId) }
                val dInfoDef = async { api.getMetrics("disks_info", deviceId) }

                try { cpuMetrics = cpuDef.await().values.map { it.asFloat } } catch(e:Exception){}
                try { ramMetrics = ramDef.await().values.map { it.asFloat } } catch(e:Exception){}
                
                try { cpuModel = cpuModDef.await().values.lastOrNull()?.asString ?: "N/A" } catch(e:Exception){}
                try { 
                    val fFreq = cpuFreqDef.await().values.lastOrNull()?.asString ?: "N/A"
                    cpuFreq = if (fFreq != "N/A") "$fFreq МГц" else fFreq
                } catch(e:Exception){}
                
                try { 
                    val rTot = ramTotDef.await().values.lastOrNull()?.asString ?: "N/A"
                    ramTotal = if (rTot != "N/A") "$rTot ГБ" else rTot
                } catch(e:Exception){}
                
                try {
                    val rSpd = ramSpdDef.await().values.lastOrNull()?.asString ?: "N/A"
                    ramSpeed = if (rSpd != "N/A" && rSpd != "Unknown") "$rSpd МГц" else rSpd
                } catch(e:Exception){}
                
                try { ramModel = ramModDef.await().values.lastOrNull()?.asString ?: "N/A" } catch(e:Exception){}
                
                try {
                    val dMod = dModDef.await().values.lastOrNull()?.asString ?: ""
                    diskModels = try {
                        val parsed = com.google.gson.Gson().fromJson(dMod, Array<String>::class.java)
                        parsed?.joinToString(", ") ?: "N/A"
                    } catch(e: Exception) { dMod.ifEmpty { "N/A" } }
                } catch(e:Exception){}

                try {
                    val diskRes = dInfoDef.await()
                    val gson = com.google.gson.Gson()
                    val t = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                    val dMap = mutableMapOf<String, MutableList<Float>>()
                    var lastD = listOf<Map<String, Any>>()
                    
                    diskRes.values.forEach { elem ->
                        try {
                            val parsed: List<Map<String, Any>> = gson.fromJson(elem.asString, t)
                            lastD = parsed
                            parsed.forEach { d ->
                                val n = d["device"].toString()
                                val p = (d["percent"] as Double).toFloat()
                                if(!dMap.containsKey(n)) dMap[n] = mutableListOf()
                                dMap[n]!!.add(p)
                            }
                        } catch(e: Exception){}
                    }
                    diskChartSeries = dMap.values.toList()
                    diskInfoList = lastD
                } catch(e:Exception){}
                
                globalError = null
            } catch (e: Exception) {
                globalError = "Error fetching metrics: ${e.message}"
            }
        }
    }

    fun sendCommand(deviceId: String) {
        if (terminalInput.isBlank()) return
        val currentCmd = terminalInput
        terminalInput = ""
        terminalOutput += "\n> $currentCmd"

        viewModelScope.launch {
            try {
                api.sendCommand(TerminalSendReq(deviceId, currentCmd))
            } catch (e: Exception) {
                terminalOutput += "\n[Network Error Sending Command]"
            }
        }
    }

    fun pollTerminal(deviceId: String) {
        viewModelScope.launch {
            try {
                val res = api.getTerminalResult(deviceId)
                if (res.output.isNotBlank()) {
                    terminalOutput += "\n${res.output}"
                }
            } catch (e: Exception) {
            }
        }
    }
}
