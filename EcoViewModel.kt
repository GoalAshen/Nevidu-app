package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.FirebaseSettings
import com.example.data.repository.EcoRepository
import com.example.localization.EcoLanguage
import com.example.localization.EcoTranslations
import com.example.localization.TranslationStrings
import com.example.pairing.BlePairingManager
import com.example.pairing.BlePairingStep
import com.example.pairing.DiscoveredBleNode
import kotlinx.coroutines.flow.*

class EcoViewModel(
  private val repository: EcoRepository = EcoRepository()
) : ViewModel() {

  val pairingManager = BlePairingManager(repository, viewModelScope)

  // Language State
  private val _currentLanguage = MutableStateFlow(EcoLanguage.ENGLISH)
  val currentLanguage: StateFlow<EcoLanguage> = _currentLanguage.asStateFlow()

  val translationStrings: StateFlow<TranslationStrings> = _currentLanguage.map {
    EcoTranslations.get(it)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, EcoTranslations.get(EcoLanguage.ENGLISH))

  private val _showWelcomeLanguageModal = MutableStateFlow(true)
  val showWelcomeLanguageModal: StateFlow<Boolean> = _showWelcomeLanguageModal.asStateFlow()

  // Pairing State Flows
  val pairingStep: StateFlow<BlePairingStep> = pairingManager.step
  val discoveredBleNodes: StateFlow<List<DiscoveredBleNode>> = pairingManager.discoveredNodes
  val selectedDiscoveredNode: StateFlow<DiscoveredBleNode?> = pairingManager.selectedNode
  val pairingStatusMessage: StateFlow<String> = pairingManager.statusMessage

  // Core Repository States
  val firebaseConfig: StateFlow<FirebaseSettings> = repository.firebaseConfig
  val connectionError: StateFlow<String?> = repository.connectionError
  val allDevices: StateFlow<List<Device>> = repository.devices
  val selectedDeviceId: StateFlow<String> = repository.selectedDeviceId
  val alerts: StateFlow<List<SystemAlert>> = repository.alerts
  val eventLogs: StateFlow<List<EventLog>> = repository.eventLogs
  val systemNotificationsEnabled: StateFlow<Boolean> = repository.systemNotificationsEnabled

  val selectedDevice: StateFlow<Device?> = combine(
    repository.devices,
    repository.selectedDeviceId
  ) { devices, id ->
    devices.find { it.id == id } ?: devices.firstOrNull()
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val currentSensorData: StateFlow<SensorData?> = combine(
    repository.sensorDataMap,
    repository.selectedDeviceId
  ) { map, id ->
    map[id]
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val currentRelayState: StateFlow<RelayState?> = combine(
    repository.relayStateMap,
    repository.selectedDeviceId
  ) { map, id ->
    map[id]
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val currentThresholds: StateFlow<EcoThresholds?> = combine(
    repository.thresholdsMap,
    repository.selectedDeviceId
  ) { map, id ->
    map[id]
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val currentHardwareControls: StateFlow<HardwareControls?> = combine(
    repository.hardwareControlsMap,
    repository.selectedDeviceId
  ) { map, id ->
    map[id]
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val currentSchedule: StateFlow<DailySchedule?> = combine(
    repository.scheduleMap,
    repository.selectedDeviceId
  ) { map, id ->
    map[id]
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val currentHistory: StateFlow<List<TelemetryPoint>> = combine(
    repository.telemetryHistoryMap,
    repository.selectedDeviceId
  ) { map, id ->
    map[id] ?: emptyList()
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val isCurrentDeviceOnline: StateFlow<Boolean> = combine(
    currentSensorData,
    repository.firebaseConfig
  ) { sensor, _ ->
    sensor?.isOnline() ?: false
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val currentPlantHealth: StateFlow<PlantHealthAnalysis> = combine(
    currentSensorData,
    selectedDevice
  ) { sensor, device ->
    PlantHealthAnalysis.evaluate(sensor, isNodePoweredOn = device?.isPoweredOn ?: true)
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    PlantHealthAnalysis.evaluate(null)
  )

  fun setLanguage(language: EcoLanguage) {
    _currentLanguage.value = language
  }

  fun dismissWelcomeLanguageModal() {
    _showWelcomeLanguageModal.value = false
  }

  fun showLanguagePicker() {
    _showWelcomeLanguageModal.value = true
  }

  // Pairing Actions
  fun startBleScan(context: Context? = null) {
    pairingManager.startScanning(context)
    vibrate(context, 40)
  }

  fun selectBleNode(node: DiscoveredBleNode) {
    pairingManager.selectAndConnect(node)
  }

  fun provisionNodeWifi(ssid: String, pass: String) {
    pairingManager.provisionWifi(ssid, pass)
  }

  fun resetPairing() {
    pairingManager.reset()
  }

  // Device & Control Actions
  fun selectDevice(deviceId: String) {
    repository.selectDevice(deviceId)
  }

  fun toggleDevicePower(turnOn: Boolean, context: Context? = null) {
    val devId = selectedDeviceId.value
    repository.toggleDevicePower(devId, turnOn)
    vibrate(context, 40)
    context?.let {
      val stateText = if (turnOn) "Device turned ON (Active)" else "Device turned OFF (Deep Sleep Standby)"
      Toast.makeText(it, stateText, Toast.LENGTH_SHORT).show()
    }
  }

  fun togglePump(on: Boolean, context: Context? = null) {
    val devId = selectedDeviceId.value
    repository.togglePump(devId, on)
    vibrate(context, 40)
  }

  fun setRelayMode(mode: RelayMode, context: Context? = null) {
    val devId = selectedDeviceId.value
    repository.setRelayMode(devId, mode)
    vibrate(context, 25)
  }

  fun updateThresholds(thresholds: EcoThresholds) {
    val devId = selectedDeviceId.value
    repository.updateThresholds(devId, thresholds)
  }

  fun updateSchedule(schedule: DailySchedule) {
    val devId = selectedDeviceId.value
    repository.updateDailySchedule(devId, schedule)
  }

  fun toggleLed(color: String, on: Boolean, context: Context? = null) {
    val devId = selectedDeviceId.value
    repository.toggleLed(devId, color, on)
    vibrate(context, 15)
  }

  fun triggerBuzzerTest(context: Context? = null) {
    val devId = selectedDeviceId.value
    repository.triggerBuzzer(devId)
    vibrate(context, 200)
    context?.let {
      Toast.makeText(it, "Buzzer Test sequence sent to $devId", Toast.LENGTH_SHORT).show()
    }
  }

  fun triggerSpeakerAlert(context: Context? = null) {
    val devId = selectedDeviceId.value
    repository.triggerSpeakerAlert(devId)
    vibrate(context, 350)
    context?.let {
      Toast.makeText(it, "Speaker Siren Alert broadcasted to $devId", Toast.LENGTH_SHORT).show()
    }
  }

  fun toggleRainSensor(context: Context? = null) {
    val devId = selectedDeviceId.value
    repository.toggleRainSensor(devId)
    vibrate(context, 30)
  }

  fun addDevice(name: String, location: String, lat: Double, lng: Double) {
    repository.addDevice(name, location, lat, lng)
  }

  fun updateDeviceLocation(deviceId: String, lat: Double, lng: Double, location: String) {
    repository.updateDeviceLocation(deviceId, lat, lng, location)
  }

  fun toggleSimulateHeartbeatTimeout(context: Context? = null) {
    repository.toggleSimulateHeartbeatTimeout()
    vibrate(context, 40)
    context?.let {
      val isPaused = repository.firebaseConfig.value.simulateHeartbeatTimeout
      val msg = if (isPaused) "Heartbeat paused! Device will show OFFLINE once past 60s." else "Heartbeat resumed! Device is ONLINE."
      Toast.makeText(it, msg, Toast.LENGTH_LONG).show()
    }
  }

  fun toggleDemoMode(enabled: Boolean) {
    repository.toggleDemoMode(enabled)
  }

  fun toggleSystemNotifications(enabled: Boolean, context: Context? = null) {
    repository.toggleSystemNotifications(enabled)
    context?.let {
      Toast.makeText(it, if (enabled) "System alerts enabled" else "System alerts muted", Toast.LENGTH_SHORT).show()
    }
  }

  fun triggerTestNotification(context: Context) {
    com.example.util.NotificationHelper.sendTestNotification(context)
    vibrate(context, 60)
  }

  fun saveFirebaseSettings(settings: FirebaseSettings, context: Context? = null) {
    repository.updateFirebaseConfig(settings)
    context?.let {
      Toast.makeText(it, "Firebase configuration saved", Toast.LENGTH_SHORT).show()
    }
  }

  fun dismissConnectionError() {
    repository.dismissConnectionError()
  }

  fun setAppContext(context: Context) {
    repository.setApplicationContext(context)
  }

  fun dismissAlert(alertId: String) {
    repository.dismissAlert(alertId)
  }

  fun clearEventLogs(context: Context? = null) {
    repository.clearEventLogs()
    context?.let {
      Toast.makeText(it, "Event logs cleared", Toast.LENGTH_SHORT).show()
    }
  }

  fun exportCsv(context: Context) {
    val csvContent = repository.exportLogsToCsv()
    try {
      val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, csvContent)
        putExtra(Intent.EXTRA_TITLE, "EcoControl_Logs_${System.currentTimeMillis()}.csv")
        type = "text/csv"
      }
      val shareIntent = Intent.createChooser(sendIntent, "Export EcoControl Event Logs CSV")
      shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(shareIntent)
    } catch (e: Exception) {
      Toast.makeText(context, "Export ready (length: ${csvContent.length} chars)", Toast.LENGTH_LONG).show()
    }
  }

  private fun vibrate(context: Context?, durationMs: Long) {
    if (context == null) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vibratorManager?.defaultVibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        @Suppress("DEPRECATION")
        vibrator?.vibrate(durationMs)
      }
    } catch (_: Exception) {}
  }
}
