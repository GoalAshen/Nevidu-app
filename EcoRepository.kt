package com.example.data.repository

import android.content.Context
import com.example.data.model.*
import com.example.data.remote.FirebaseSettings
import com.example.util.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.random.Random

class EcoRepository(
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
  var appContext: Context? = null
    private set

  private val _firebaseConfig = MutableStateFlow(FirebaseSettings.DEFAULT)
  val firebaseConfig: StateFlow<FirebaseSettings> = _firebaseConfig.asStateFlow()

  private val _connectionError = MutableStateFlow<String?>(null)
  val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

  private val _systemNotificationsEnabled = MutableStateFlow(true)
  val systemNotificationsEnabled: StateFlow<Boolean> = _systemNotificationsEnabled.asStateFlow()

  private val _devices = MutableStateFlow<List<Device>>(
    listOf(
      Device(
        id = "NODE-E82",
        name = "Greenhouse Alpha",
        locationName = "North Wing, Greenhouse #1",
        latitude = 37.7749,
        longitude = -122.4194,
        firmwareVersion = "NodeOS v2.4.1",
        wifiSsid = "EcoNet-2.4G",
        wifiRssi = -54,
        ipAddress = "192.168.1.142"
      ),
      Device(
        id = "NODE-9A4",
        name = "Hydroponics Bay B",
        locationName = "Indoor Hydro Room",
        latitude = 37.7833,
        longitude = -122.4167,
        firmwareVersion = "NodeOS v2.3.8",
        wifiSsid = "EcoNet-2.4G",
        wifiRssi = -62,
        ipAddress = "192.168.1.145"
      ),
      Device(
        id = "NODE-1F0",
        name = "South Orchard",
        locationName = "Field Bed 3, Sector South",
        latitude = 37.7690,
        longitude = -122.4467,
        firmwareVersion = "NodeOS v2.4.0",
        wifiSsid = "EcoMesh-Extender",
        wifiRssi = -71,
        ipAddress = "192.168.1.189"
      )
    )
  )
  val devices: StateFlow<List<Device>> = _devices.asStateFlow()

  private val _selectedDeviceId = MutableStateFlow("NODE-E82")
  val selectedDeviceId: StateFlow<String> = _selectedDeviceId.asStateFlow()

  private val _sensorDataMap = MutableStateFlow<Map<String, SensorData>>(
    mapOf(
      "NODE-E82" to SensorData(
        temperatureC = 25.4,
        humidityPercent = 63.8,
        soilZone1 = 48,
        soilZone2 = 31, // Dry!
        soilZone3 = 76, // Wet
        lightLux = 48200,
        isRaining = false,
        batteryPercent = 91,
        solarCharging = true,
        lastSeenEpochMs = System.currentTimeMillis()
      ),
      "NODE-9A4" to SensorData(
        temperatureC = 23.1,
        humidityPercent = 71.2,
        soilZone1 = 65,
        soilZone2 = 62,
        soilZone3 = 68,
        lightLux = 28500,
        isRaining = false,
        batteryPercent = 84,
        solarCharging = true,
        lastSeenEpochMs = System.currentTimeMillis() - 8_000L
      ),
      "NODE-1F0" to SensorData(
        temperatureC = 28.6,
        humidityPercent = 45.0,
        soilZone1 = 28,
        soilZone2 = 25,
        soilZone3 = 33,
        lightLux = 64000,
        isRaining = false,
        batteryPercent = 61,
        solarCharging = true,
        lastSeenEpochMs = System.currentTimeMillis() - 14_000L
      )
    )
  )
  val sensorDataMap: StateFlow<Map<String, SensorData>> = _sensorDataMap.asStateFlow()

  private val _relayStateMap = MutableStateFlow<Map<String, RelayState>>(
    mapOf(
      "NODE-E82" to RelayState(isPumpOn = false, mode = RelayMode.AUTO),
      "NODE-9A4" to RelayState(isPumpOn = false, mode = RelayMode.AUTO),
      "NODE-1F0" to RelayState(isPumpOn = false, mode = RelayMode.AUTO)
    )
  )
  val relayStateMap: StateFlow<Map<String, RelayState>> = _relayStateMap.asStateFlow()

  private val _thresholdsMap = MutableStateFlow<Map<String, EcoThresholds>>(
    mapOf(
      "NODE-E82" to EcoThresholds(),
      "NODE-9A4" to EcoThresholds(),
      "NODE-1F0" to EcoThresholds()
    )
  )
  val thresholdsMap: StateFlow<Map<String, EcoThresholds>> = _thresholdsMap.asStateFlow()

  private val _hardwareControlsMap = MutableStateFlow<Map<String, HardwareControls>>(
    mapOf(
      "NODE-E82" to HardwareControls(),
      "NODE-9A4" to HardwareControls(),
      "NODE-1F0" to HardwareControls()
    )
  )
  val hardwareControlsMap: StateFlow<Map<String, HardwareControls>> = _hardwareControlsMap.asStateFlow()

  private val _scheduleMap = MutableStateFlow<Map<String, DailySchedule>>(
    mapOf(
      "NODE-E82" to DailySchedule(),
      "NODE-9A4" to DailySchedule(startHour = 7, startMinute = 0, durationMinutes = 15),
      "NODE-1F0" to DailySchedule(startHour = 6, startMinute = 0, durationMinutes = 20)
    )
  )
  val scheduleMap: StateFlow<Map<String, DailySchedule>> = _scheduleMap.asStateFlow()

  private val _telemetryHistoryMap = MutableStateFlow<Map<String, List<TelemetryPoint>>>(
    generateInitialTelemetryHistory()
  )
  val telemetryHistoryMap: StateFlow<Map<String, List<TelemetryPoint>>> = _telemetryHistoryMap.asStateFlow()

  private val _alerts = MutableStateFlow<List<SystemAlert>>(emptyList())
  val alerts: StateFlow<List<SystemAlert>> = _alerts.asStateFlow()

  private val _eventLogs = MutableStateFlow<List<EventLog>>(
    listOf(
      EventLog(
        id = UUID.randomUUID().toString(),
        timestampMs = System.currentTimeMillis() - 3600_000L,
        deviceId = "NODE-E82",
        type = "SYSTEM_BOOT",
        title = "System Booted",
        details = "Smart Node connected to EcoNet-2.4G. IP: 192.168.1.142"
      ),
      EventLog(
        id = UUID.randomUUID().toString(),
        timestampMs = System.currentTimeMillis() - 2400_000L,
        deviceId = "NODE-E82",
        type = "SCHEDULE_RUN",
        title = "Morning Schedule Completed",
        details = "10 min irrigation finished normally. Soil avg reached 64%"
      ),
      EventLog(
        id = UUID.randomUUID().toString(),
        timestampMs = System.currentTimeMillis() - 1200_000L,
        deviceId = "NODE-E82",
        type = "ALERT",
        title = "Dry Soil Threshold Breached",
        details = "Zone 2 fell to 31% (threshold: 35%)"
      )
    )
  )
  val eventLogs: StateFlow<List<EventLog>> = _eventLogs.asStateFlow()

  // Active Firebase Realtime Database Listeners & References
  private var activeSensorsRef: DatabaseReference? = null
  private var activeSensorsListener: ValueEventListener? = null

  private var activeStatusRef: DatabaseReference? = null
  private var activeStatusListener: ValueEventListener? = null

  private var activeHistoryRef: Query? = null
  private var activeHistoryListener: ValueEventListener? = null

  private var simulationJob: Job? = null
  private var historySyncJob: Job? = null
  private val lastAlertNotifiedTimes = mutableMapOf<String, Long>()

  init {
    startSimulationTicker()
    startPeriodicHistorySync()
  }

  fun setApplicationContext(context: Context) {
    this.appContext = context.applicationContext
  }

  fun selectDevice(deviceId: String) {
    _selectedDeviceId.value = deviceId
    if (!_firebaseConfig.value.isDemoMode) {
      attachFirebaseListeners(deviceId)
    }
  }

  fun dismissConnectionError() {
    _connectionError.value = null
  }

  fun updateFirebaseConfig(config: FirebaseSettings) {
    _firebaseConfig.value = config
    if (config.isDemoMode) {
      detachFirebaseListeners()
      startSimulationTicker()
    } else {
      stopSimulationTicker()
      authenticateAndConnectFirebase(config)
    }
  }

  fun toggleDemoMode(enabled: Boolean) {
    _firebaseConfig.update { it.copy(isDemoMode = enabled) }
    if (enabled) {
      detachFirebaseListeners()
      _connectionError.value = null
      startSimulationTicker()
      addEventLog(
        deviceId = _selectedDeviceId.value,
        type = "MODE_DEMO",
        title = "Switched to Demo Simulation Mode",
        details = "Simulating live fluctuating sensor readings and autonomous irrigation."
      )
    } else {
      stopSimulationTicker()
      authenticateAndConnectFirebase(_firebaseConfig.value)
      addEventLog(
        deviceId = _selectedDeviceId.value,
        type = "MODE_FIREBASE",
        title = "Connecting to Firebase RTDB Live",
        details = "Listening to /devices/${_selectedDeviceId.value}/sensors and status."
      )
    }
  }

  fun toggleSystemNotifications(enabled: Boolean) {
    _systemNotificationsEnabled.value = enabled
  }

  fun toggleSimulateHeartbeatTimeout() {
    _firebaseConfig.update { it.copy(simulateHeartbeatTimeout = !it.simulateHeartbeatTimeout) }
    val isTimeout = _firebaseConfig.value.simulateHeartbeatTimeout
    addEventLog(
      deviceId = _selectedDeviceId.value,
      type = "SIMULATION",
      title = if (isTimeout) "Heartbeat Paused (Offline Test)" else "Heartbeat Resumed (Online)",
      details = if (isTimeout) "Simulating network disconnection. Last seen will exceed 60s." else "Restored heartbeat sync."
    )
  }

  fun toggleDevicePower(deviceId: String, powerOn: Boolean) {
    _devices.update { list ->
      list.map {
        if (it.id == deviceId) it.copy(isPoweredOn = powerOn) else it
      }
    }
    if (!powerOn) {
      togglePump(deviceId, false)
    }

    // Write command to Firebase RTDB if live
    writeCommandsToFirebase(
      deviceId = deviceId,
      updates = mapOf(
        "power" to powerOn,
        "pump" to (if (!powerOn) false else (_relayStateMap.value[deviceId]?.isPumpOn ?: false))
      )
    )

    addEventLog(
      deviceId = deviceId,
      type = if (powerOn) "NODE_POWER_ON" else "NODE_POWER_OFF",
      title = if (powerOn) "Device Power Switched ON" else "Device Switched to Standby / OFF",
      details = if (powerOn) "Node active and sending telemetry." else "Node in ultra-low power standby."
    )
  }

  fun togglePump(deviceId: String, turnOn: Boolean) {
    _relayStateMap.update { current ->
      val currentRelay = current[deviceId] ?: RelayState(isPumpOn = false, mode = RelayMode.MANUAL)
      val thresholds = _thresholdsMap.value[deviceId] ?: EcoThresholds()
      val countdown = if (turnOn) thresholds.maxPumpRunTimeMinutes * 60 else 0
      current + (deviceId to currentRelay.copy(
        isPumpOn = turnOn,
        remainingCountdownSeconds = countdown,
        activeRunSeconds = 0,
        triggeredByAuto = false
      ))
    }

    // Write command to Firebase RTDB (/devices/{id}/commands: pump)
    writeCommandsToFirebase(
      deviceId = deviceId,
      updates = mapOf("pump" to turnOn)
    )

    addEventLog(
      deviceId = deviceId,
      type = if (turnOn) "PUMP_START_MANUAL" else "PUMP_STOP_MANUAL",
      title = if (turnOn) "Relay / 12V Load Switched ON" else "Relay / 12V Load Switched OFF",
      details = if (turnOn) "Manual override activated." else "Manual override turned load off."
    )
  }

  fun setRelayMode(deviceId: String, mode: RelayMode) {
    _relayStateMap.update { current ->
      val currentRelay = current[deviceId] ?: RelayState(isPumpOn = false, mode = mode)
      current + (deviceId to currentRelay.copy(mode = mode))
    }

    // Write command to Firebase RTDB (/devices/{id}/commands: mode)
    writeCommandsToFirebase(
      deviceId = deviceId,
      updates = mapOf("mode" to mode.name)
    )

    addEventLog(
      deviceId = deviceId,
      type = "MODE_CHANGE",
      title = "Relay Mode: ${mode.name}",
      details = if (mode == RelayMode.AUTO)
        "Auto irrigation will trigger when soil moisture < dry threshold and no rain."
      else
        "Manual control only. Automatic rules paused."
    )
  }

  fun updateThresholds(deviceId: String, thresholds: EcoThresholds) {
    _thresholdsMap.update { current ->
      current + (deviceId to thresholds)
    }

    // Write settings to Firebase RTDB (/devices/{id}/settings)
    writeSettingsToFirebase(
      deviceId = deviceId,
      settingsMap = mapOf(
        "soilDryPercent" to thresholds.soilDryPercent,
        "soilTargetPercent" to thresholds.soilTargetPercent,
        "highTempAlertThresholdC" to thresholds.highTempAlertThresholdC,
        "lowBatteryAlertPercent" to thresholds.lowBatteryAlertPercent,
        "maxPumpRunTimeMinutes" to thresholds.maxPumpRunTimeMinutes
      )
    )

    addEventLog(
      deviceId = deviceId,
      type = "SETTINGS_UPDATE",
      title = "Thresholds Updated",
      details = "Dry: ${thresholds.soilDryPercent}%, Target: ${thresholds.soilTargetPercent}%, Max Run: ${thresholds.maxPumpRunTimeMinutes}m"
    )
  }

  fun updateDailySchedule(deviceId: String, schedule: DailySchedule) {
    _scheduleMap.update { current ->
      current + (deviceId to schedule)
    }

    // Write schedule settings to Firebase RTDB (/devices/{id}/settings/schedule)
    val scheduleMap = mapOf(
      "enabled" to schedule.enabled,
      "startHour" to schedule.startHour,
      "startMinute" to schedule.startMinute,
      "durationMinutes" to schedule.durationMinutes,
      "activeDays" to schedule.activeDays.toList()
    )
    writeSettingsToFirebase(deviceId, mapOf("schedule" to scheduleMap))

    addEventLog(
      deviceId = deviceId,
      type = "SCHEDULE_UPDATE",
      title = "Schedule Updated",
      details = "Start: ${schedule.formattedTime()}, Duration: ${schedule.durationMinutes}min, Enabled: ${schedule.enabled}"
    )
  }

  fun toggleLed(deviceId: String, color: String, turnOn: Boolean) {
    _hardwareControlsMap.update { current ->
      val controls = current[deviceId] ?: HardwareControls()
      val updated = when (color.lowercase()) {
        "red" -> controls.copy(ledRed = turnOn)
        "blue" -> controls.copy(ledBlue = turnOn)
        "green" -> controls.copy(ledGreen = turnOn)
        "yellow" -> controls.copy(ledYellow = turnOn)
        else -> controls
      }
      current + (deviceId to updated)
    }

    val ledKey = when (color.lowercase()) {
      "red" -> "ledRed"
      "blue" -> "ledBlue"
      "green" -> "ledGreen"
      "yellow" -> "ledYellow"
      else -> "led$color"
    }
    writeCommandsToFirebase(deviceId, mapOf(ledKey to turnOn))

    addEventLog(
      deviceId = deviceId,
      type = "HARDWARE_LED",
      title = "Status LED $color ${if (turnOn) "ON" else "OFF"}",
      details = "Output indicator toggled."
    )
  }

  fun triggerBuzzer(deviceId: String) {
    coroutineScope.launch {
      _hardwareControlsMap.update { current ->
        val controls = current[deviceId] ?: HardwareControls()
        current + (deviceId to controls.copy(buzzerActive = true))
      }
      writeCommandsToFirebase(deviceId, mapOf("buzzer" to true))

      addEventLog(
        deviceId = deviceId,
        type = "HARDWARE_BUZZER",
        title = "Buzzer Test Fired",
        details = "Acoustic frequency pulse command sent."
      )
      delay(1500)
      _hardwareControlsMap.update { current ->
        val controls = current[deviceId] ?: HardwareControls()
        current + (deviceId to controls.copy(buzzerActive = false))
      }
      writeCommandsToFirebase(deviceId, mapOf("buzzer" to false))
    }
  }

  fun triggerSpeakerAlert(deviceId: String) {
    coroutineScope.launch {
      _hardwareControlsMap.update { current ->
        val controls = current[deviceId] ?: HardwareControls()
        current + (deviceId to controls.copy(speakerAlertActive = true))
      }
      writeCommandsToFirebase(deviceId, mapOf("speaker" to true))

      addEventLog(
        deviceId = deviceId,
        type = "HARDWARE_SPEAKER",
        title = "Speaker Alarm Sounded",
        details = "Audible alarm sequence broadcast command sent."
      )
      delay(2000)
      _hardwareControlsMap.update { current ->
        val controls = current[deviceId] ?: HardwareControls()
        current + (deviceId to controls.copy(speakerAlertActive = false))
      }
      writeCommandsToFirebase(deviceId, mapOf("speaker" to false))
    }
  }

  fun toggleRainSensor(deviceId: String) {
    _sensorDataMap.update { current ->
      val sensor = current[deviceId] ?: return@update current
      val newRainState = !sensor.isRaining
      current + (deviceId to sensor.copy(isRaining = newRainState))
    }

    val sensor = _sensorDataMap.value[deviceId]
    val isRaining = sensor?.isRaining == true
    addEventLog(
      deviceId = deviceId,
      type = "RAIN_SENSOR",
      title = if (isRaining) "Precipitation Detected" else "Rain Ceased (Dry)",
      details = if (isRaining) "Moisture detected. Auto-irrigation will hold." else "Rain sensor dry."
    )
  }

  fun addDevice(name: String, location: String, lat: Double, lng: Double) {
    val newId = "NODE-" + UUID.randomUUID().toString().take(4).uppercase()
    val newDevice = Device(
      id = newId,
      name = name,
      locationName = location,
      latitude = lat,
      longitude = lng
    )
    _devices.update { it + newDevice }
    _sensorDataMap.update {
      it + (newId to SensorData(
        temperatureC = 24.0,
        humidityPercent = 58.0,
        soilZone1 = 50,
        soilZone2 = 45,
        soilZone3 = 60,
        lightLux = 42000,
        isRaining = false,
        batteryPercent = 88,
        solarCharging = true,
        lastSeenEpochMs = System.currentTimeMillis()
      ))
    }
    _relayStateMap.update { it + (newId to RelayState(isPumpOn = false, mode = RelayMode.AUTO)) }
    _thresholdsMap.update { it + (newId to EcoThresholds()) }
    _hardwareControlsMap.update { it + (newId to HardwareControls()) }
    _scheduleMap.update { it + (newId to DailySchedule()) }
    _selectedDeviceId.value = newId

    addEventLog(
      deviceId = newId,
      type = "DEVICE_ADDED",
      title = "New Node Registered: $name",
      details = "ID: $newId at $location ($lat, $lng)"
    )
  }

  fun updateDeviceLocation(deviceId: String, lat: Double, lng: Double, locationName: String) {
    _devices.update { list ->
      list.map {
        if (it.id == deviceId) it.copy(latitude = lat, longitude = lng, locationName = locationName)
        else it
      }
    }
    addEventLog(
      deviceId = deviceId,
      type = "LOCATION_UPDATE",
      title = "Device Location Updated",
      details = "Coordinates: $lat, $lng ($locationName)"
    )
  }

  fun dismissAlert(alertId: String) {
    _alerts.update { list -> list.filter { it.id != alertId } }
  }

  fun clearEventLogs() {
    _eventLogs.value = emptyList()
  }

  fun exportLogsToCsv(): String {
    val header = "Timestamp,Date_Time,Device_ID,Event_Type,Title,Details\n"
    val rows = _eventLogs.value.joinToString("\n") { log ->
      val escapedTitle = log.title.replace("\"", "\"\"")
      val escapedDetails = log.details.replace("\"", "\"\"")
      "${log.timestampMs},\"${log.formattedTime()}\",\"${log.deviceId}\",\"${log.type}\",\"$escapedTitle\",\"$escapedDetails\""
    }
    return header + rows
  }

  private fun addEventLog(deviceId: String, type: String, title: String, details: String) {
    val log = EventLog(
      id = UUID.randomUUID().toString(),
      timestampMs = System.currentTimeMillis(),
      deviceId = deviceId,
      type = type,
      title = title,
      details = details
    )
    _eventLogs.update { (listOf(log) + it).take(150) }
  }

  // =========================================================================
  // FIREBASE REALTIME DATABASE & AUTH INTEGRATION
  // =========================================================================

  private fun getOrInitFirebaseApp(settings: FirebaseSettings): FirebaseApp? {
    return try {
      FirebaseApp.getInstance()
    } catch (_: Exception) {
      val ctx = appContext
      if (ctx != null && settings.databaseUrl.isNotBlank()) {
        try {
          val apiKey = settings.apiKey.ifBlank { "AIzaSyFakeKeyForLocalFallbackOnly" }
          val projectId = settings.projectId.ifBlank { "ecocontrol-system" }
          val options = FirebaseOptions.Builder()
            .setApplicationId(ctx.packageName)
            .setApiKey(apiKey)
            .setDatabaseUrl(settings.databaseUrl)
            .setProjectId(projectId)
            .build()
          FirebaseApp.initializeApp(ctx, options)
        } catch (_: Exception) {
          null
        }
      } else {
        null
      }
    }
  }

  private fun getDatabase(settings: FirebaseSettings): FirebaseDatabase? {
    val app = getOrInitFirebaseApp(settings)
    return try {
      if (app != null && settings.databaseUrl.isNotBlank()) {
        FirebaseDatabase.getInstance(app, settings.databaseUrl)
      } else if (settings.databaseUrl.isNotBlank()) {
        FirebaseDatabase.getInstance(settings.databaseUrl)
      } else {
        FirebaseDatabase.getInstance()
      }
    } catch (e: Exception) {
      _connectionError.value = "Realtime Database initialization failed: ${e.localizedMessage}"
      null
    }
  }

  private fun getAuth(settings: FirebaseSettings): FirebaseAuth? {
    val app = getOrInitFirebaseApp(settings)
    return try {
      if (app != null) FirebaseAuth.getInstance(app) else FirebaseAuth.getInstance()
    } catch (e: Exception) {
      _connectionError.value = "Firebase Auth initialization failed: ${e.localizedMessage}"
      null
    }
  }

  private fun authenticateAndConnectFirebase(settings: FirebaseSettings) {
    coroutineScope.launch {
      val auth = getAuth(settings)
      if (auth == null) {
        _connectionError.value = "Unable to initialize Firebase Authentication. Check credentials."
        return@launch
      }

      val email = settings.ownerEmail.trim()
      val password = settings.authPassword.trim()

      if (email.isNotBlank() && password.isNotBlank()) {
        try {
          auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
              _connectionError.value = null
              attachFirebaseListeners(_selectedDeviceId.value)
            }
            .addOnFailureListener { e ->
              _connectionError.value = "Firebase Auth failed (${email}): ${e.localizedMessage}"
            }
        } catch (e: Exception) {
          _connectionError.value = "Authentication error: ${e.localizedMessage}"
        }
      } else {
        // If credentials are blank, attempt direct listener connection (useful with test rules)
        attachFirebaseListeners(_selectedDeviceId.value)
      }
    }
  }

  private fun attachFirebaseListeners(deviceId: String) {
    detachFirebaseListeners()
    val db = getDatabase(_firebaseConfig.value) ?: return

    try {
      // 1. Read live /devices/{id}/sensors
      val sensorsRef = db.getReference("devices/$deviceId/sensors")
      activeSensorsRef = sensorsRef
      val sListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          if (!snapshot.exists()) return
          try {
            val temp = snapshot.child("temperatureC").getValue(Double::class.java)
              ?: snapshot.child("temperatureC").getValue(Long::class.java)?.toDouble()
              ?: 24.5
            val hum = snapshot.child("humidityPercent").getValue(Double::class.java)
              ?: snapshot.child("humidityPercent").getValue(Long::class.java)?.toDouble()
              ?: 60.0
            val z1 = snapshot.child("soilZone1").getValue(Long::class.java)?.toInt() ?: 45
            val z2 = snapshot.child("soilZone2").getValue(Long::class.java)?.toInt() ?: 45
            val z3 = snapshot.child("soilZone3").getValue(Long::class.java)?.toInt() ?: 45
            val light = snapshot.child("lightLux").getValue(Long::class.java)?.toInt() ?: 40000
            val raining = snapshot.child("isRaining").getValue(Boolean::class.java) ?: false
            val batteryPct = snapshot.child("batteryPercent").getValue(Long::class.java)?.toInt() ?: 85
            val solar = snapshot.child("solarCharging").getValue(Boolean::class.java) ?: false
            val lastSeen = snapshot.child("lastSeenEpochMs").getValue(Long::class.java)
              ?: System.currentTimeMillis()

            val updatedSensor = SensorData(
              temperatureC = temp,
              humidityPercent = hum,
              soilZone1 = z1,
              soilZone2 = z2,
              soilZone3 = z3,
              lightLux = light,
              isRaining = raining,
              batteryPercent = batteryPct.coerceIn(0, 100),
              solarCharging = solar,
              lastSeenEpochMs = lastSeen
            )

            _sensorDataMap.update { it + (deviceId to updatedSensor) }
            val now = System.currentTimeMillis()
            val thresholds = _thresholdsMap.value[deviceId] ?: EcoThresholds()
            evaluateAlerts(now)
            checkForegroundNotifications(deviceId, updatedSensor, thresholds, now)
          } catch (e: Exception) {
            _connectionError.value = "Error parsing sensors data: ${e.localizedMessage}"
          }
        }

        override fun onCancelled(error: DatabaseError) {
          _connectionError.value = "Sensors listener error: ${error.message} (${error.code})"
        }
      }
      sensorsRef.addValueEventListener(sListener)
      activeSensorsListener = sListener

      // 2. Read live /devices/{id}/status
      val statusRef = db.getReference("devices/$deviceId/status")
      activeStatusRef = statusRef
      val statListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          if (!snapshot.exists()) return
          try {
            val pumpOn = snapshot.child("pumpOn").getValue(Boolean::class.java) ?: false
            val modeStr = snapshot.child("mode").getValue(String::class.java) ?: "AUTO"
            val mode = if (modeStr.equals("MANUAL", ignoreCase = true)) RelayMode.MANUAL else RelayMode.AUTO
            val activeRun = snapshot.child("activeRunSeconds").getValue(Long::class.java)?.toInt() ?: 0

            _relayStateMap.update { current ->
              val prev = current[deviceId] ?: RelayState(isPumpOn = false, mode = mode)
              current + (deviceId to prev.copy(
                isPumpOn = pumpOn,
                mode = mode,
                activeRunSeconds = activeRun
              ))
            }
          } catch (e: Exception) {
            _connectionError.value = "Error parsing status data: ${e.localizedMessage}"
          }
        }

        override fun onCancelled(error: DatabaseError) {
          _connectionError.value = "Status listener error: ${error.message} (${error.code})"
        }
      }
      statusRef.addValueEventListener(statListener)
      activeStatusListener = statListener

      // 3. Read history /devices/{id}/history
      val historyQuery = db.getReference("devices/$deviceId/history").limitToLast(120)
      activeHistoryRef = historyQuery
      val histListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          if (!snapshot.exists()) return
          try {
            val points = mutableListOf<TelemetryPoint>()
            for (child in snapshot.children) {
              val ts = child.child("timestampMs").getValue(Long::class.java)
                ?: child.key?.toLongOrNull()
                ?: System.currentTimeMillis()
              val tempC = child.child("tempC").getValue(Double::class.java)
                ?: child.child("tempC").getValue(Long::class.java)?.toDouble()
                ?: 24.0
              val hum = child.child("humidity").getValue(Double::class.java)
                ?: child.child("humidity").getValue(Long::class.java)?.toDouble()
                ?: 60.0
              val sAvg = child.child("soilAvg").getValue(Long::class.java)?.toInt() ?: 50
              val z1 = child.child("zone1").getValue(Long::class.java)?.toInt() ?: 50
              val z2 = child.child("zone2").getValue(Long::class.java)?.toInt() ?: 50
              val z3 = child.child("zone3").getValue(Long::class.java)?.toInt() ?: 50
              val light = child.child("lightLux").getValue(Long::class.java)?.toInt() ?: 45000
              val batt = child.child("batteryPercent").getValue(Long::class.java)?.toInt() ?: 85

              points.add(
                TelemetryPoint(
                  timestampMs = ts,
                  tempC = tempC,
                  humidity = hum,
                  soilAvg = sAvg,
                  zone1 = z1,
                  zone2 = z2,
                  zone3 = z3,
                  lightLux = light,
                  batteryPercent = batt
                )
              )
            }
            if (points.isNotEmpty()) {
              _telemetryHistoryMap.update { it + (deviceId to points.sortedBy { p -> p.timestampMs }) }
            }
          } catch (_: Exception) {}
        }

        override fun onCancelled(error: DatabaseError) {
          // Non-critical, ignore or log
        }
      }
      historyQuery.addValueEventListener(histListener)
      activeHistoryListener = histListener

    } catch (e: Exception) {
      _connectionError.value = "Failed to attach RTDB listeners: ${e.localizedMessage}"
    }
  }

  private fun detachFirebaseListeners() {
    try {
      activeSensorsListener?.let { activeSensorsRef?.removeEventListener(it) }
      activeStatusListener?.let { activeStatusRef?.removeEventListener(it) }
      activeHistoryListener?.let { activeHistoryRef?.removeEventListener(it) }
    } catch (_: Exception) {}
    activeSensorsRef = null
    activeSensorsListener = null
    activeStatusRef = null
    activeStatusListener = null
    activeHistoryRef = null
    activeHistoryListener = null
  }

  private fun writeCommandsToFirebase(deviceId: String, updates: Map<String, Any>) {
    if (_firebaseConfig.value.isDemoMode) return
    val db = getDatabase(_firebaseConfig.value) ?: return
    try {
      db.getReference("devices/$deviceId/commands").updateChildren(updates)
        .addOnFailureListener { e ->
          _connectionError.value = "Command send error: ${e.localizedMessage}"
        }
    } catch (e: Exception) {
      _connectionError.value = "Command write failed: ${e.localizedMessage}"
    }
  }

  private fun writeSettingsToFirebase(deviceId: String, settingsMap: Map<String, Any>) {
    if (_firebaseConfig.value.isDemoMode) return
    val db = getDatabase(_firebaseConfig.value) ?: return
    try {
      db.getReference("devices/$deviceId/settings").updateChildren(settingsMap)
        .addOnFailureListener { e ->
          _connectionError.value = "Settings update error: ${e.localizedMessage}"
        }
    } catch (e: Exception) {
      _connectionError.value = "Settings write failed: ${e.localizedMessage}"
    }
  }

  private fun startPeriodicHistorySync() {
    historySyncJob?.cancel()
    historySyncJob = coroutineScope.launch {
      while (isActive) {
        delay(5 * 60 * 1000L) // Every 5 minutes
        if (!_firebaseConfig.value.isDemoMode) {
          val devId = _selectedDeviceId.value
          val sensor = _sensorDataMap.value[devId]
          if (sensor != null) {
            saveHistorySnapshotToFirebase(devId, sensor)
          }
        }
      }
    }
  }

  private fun saveHistorySnapshotToFirebase(deviceId: String, sensor: SensorData) {
    val db = getDatabase(_firebaseConfig.value) ?: return
    val now = System.currentTimeMillis()
    val pointData = mapOf(
      "timestampMs" to now,
      "tempC" to sensor.temperatureC,
      "humidity" to sensor.humidityPercent,
      "soilAvg" to sensor.soilAverage,
      "zone1" to sensor.soilZone1,
      "zone2" to sensor.soilZone2,
      "zone3" to sensor.soilZone3,
      "lightLux" to sensor.lightLux,
      "batteryPercent" to sensor.batteryPercent
    )
    try {
      db.getReference("devices/$deviceId/history/$now").setValue(pointData)
    } catch (_: Exception) {}
  }

  // =========================================================================
  // FOREGROUND-SAFE NOTIFICATION CHECKS
  // =========================================================================

  private fun checkForegroundNotifications(
    deviceId: String,
    sensor: SensorData,
    thresholds: EcoThresholds,
    now: Long
  ) {
    val ctx = appContext ?: return
    if (!_systemNotificationsEnabled.value) return
    val cooldownMs = 3 * 60 * 1000L // 3-minute cooldown per notification type

    fun shouldNotify(key: String): Boolean {
      val last = lastAlertNotifiedTimes[key] ?: 0L
      if (now - last > cooldownMs) {
        lastAlertNotifiedTimes[key] = now
        return true
      }
      return false
    }

    // 1. Dry soil
    val isDry = sensor.soilZone1 < thresholds.soilDryPercent ||
      sensor.soilZone2 < thresholds.soilDryPercent ||
      sensor.soilZone3 < thresholds.soilDryPercent
    if (isDry && shouldNotify("dry_$deviceId")) {
      NotificationHelper.sendAlertNotification(
        context = ctx,
        id = 1001,
        title = "Dry Soil Warning: $deviceId",
        message = "Moisture below ${thresholds.soilDryPercent}% threshold. Irrigation required.",
        alertType = AlertType.DRY_SOIL
      )
    }

    // 2. High temp
    if (sensor.temperatureC >= thresholds.highTempAlertThresholdC && shouldNotify("temp_$deviceId")) {
      NotificationHelper.sendAlertNotification(
        context = ctx,
        id = 1002,
        title = "High Temperature Alert: $deviceId",
        message = "Temperature reached ${sensor.temperatureC}°C (exceeds ${thresholds.highTempAlertThresholdC}°C).",
        alertType = AlertType.HIGH_TEMP
      )
    }

    // 3. Rain
    if (sensor.isRaining && shouldNotify("rain_$deviceId")) {
      NotificationHelper.sendAlertNotification(
        context = ctx,
        id = 1003,
        title = "Rain Detected: $deviceId",
        message = "Precipitation active. Auto irrigation paused safely.",
        alertType = AlertType.RAIN
      )
    }

    // 4. Low battery
    if (sensor.batteryPercent <= thresholds.lowBatteryAlertPercent && shouldNotify("batt_$deviceId")) {
      NotificationHelper.sendAlertNotification(
        context = ctx,
        id = 1004,
        title = "Low Battery Alert: $deviceId",
        message = "Battery capacity is at ${sensor.batteryPercent}% (threshold: ${thresholds.lowBatteryAlertPercent}%).",
        alertType = AlertType.LOW_BATTERY
      )
    }

    // 5. Offline (>60s heartbeat lost)
    if (!sensor.isOnline(now) && shouldNotify("offline_$deviceId")) {
      val sec = sensor.secondsSinceLastSeen(now)
      NotificationHelper.sendAlertNotification(
        context = ctx,
        id = 1005,
        title = "Device Offline Alert: $deviceId",
        message = "Heartbeat lost ${sec}s ago (>60s timeout). Check power or Wi-Fi.",
        alertType = AlertType.DEVICE_OFFLINE
      )
    }
  }

  // =========================================================================
  // SIMULATION ENGINE (FOR DEMO MODE)
  // =========================================================================

  private fun startSimulationTicker() {
    simulationJob?.cancel()
    simulationJob = coroutineScope.launch {
      while (isActive) {
        delay(2500L) // 2.5 second simulation tick
        val now = System.currentTimeMillis()
        val isTimeoutSimulated = _firebaseConfig.value.simulateHeartbeatTimeout

        _sensorDataMap.update { currentMap ->
          currentMap.mapValues { (deviceId, sensor) ->
            val relay = _relayStateMap.value[deviceId] ?: RelayState(isPumpOn = false, mode = RelayMode.AUTO)

            // 1. Heartbeat logic
            val updatedLastSeen = if (isTimeoutSimulated && deviceId == _selectedDeviceId.value) {
              sensor.lastSeenEpochMs
            } else {
              now
            }

            // 2. Physics & telemetry drift
            val tempDrift = (Random.nextDouble(-0.15, 0.15))
            val newTemp = (sensor.temperatureC + tempDrift).coerceIn(12.0, 48.0)

            val humDrift = (Random.nextDouble(-0.3, 0.3))
            val newHum = (sensor.humidityPercent + humDrift).coerceIn(20.0, 95.0)

            val lightDrift = Random.nextInt(-400, 450)
            val newLight = (sensor.lightLux + lightDrift).coerceIn(100, 95000)

            val isCharging = newLight > 15000

            // Soil Moisture & Battery logic based on Pump
            var z1 = sensor.soilZone1
            var z2 = sensor.soilZone2
            var z3 = sensor.soilZone3
            var battPct = sensor.batteryPercent

            if (relay.isPumpOn) {
              if (Random.nextBoolean()) z1 = (z1 + 1).coerceAtMost(98)
              if (Random.nextBoolean()) z2 = (z2 + 1).coerceAtMost(98)
              if (Random.nextBoolean()) z3 = (z3 + 1).coerceAtMost(98)

              if (!isCharging && Random.nextInt(10) == 0) {
                battPct = (battPct - 1).coerceAtLeast(5)
              }
            } else {
              if (Random.nextInt(12) == 0) z1 = (z1 - 1).coerceAtLeast(5)
              if (Random.nextInt(10) == 0) z2 = (z2 - 1).coerceAtLeast(5)
              if (Random.nextInt(14) == 0) z3 = (z3 - 1).coerceAtLeast(5)

              if (isCharging && Random.nextInt(10) == 0 && battPct < 100) {
                battPct = (battPct + 1).coerceAtMost(100)
              }
            }

            sensor.copy(
              temperatureC = ((newTemp * 10).toInt() / 10.0),
              humidityPercent = ((newHum * 10).toInt() / 10.0),
              soilZone1 = z1,
              soilZone2 = z2,
              soilZone3 = z3,
              lightLux = newLight,
              batteryPercent = battPct,
              solarCharging = isCharging,
              lastSeenEpochMs = updatedLastSeen
            )
          }
        }

        processRelayAndAutoRules(now)
        evaluateAlerts(now)
        val selectedSensor = _sensorDataMap.value[_selectedDeviceId.value]
        val selectedThresholds = _thresholdsMap.value[_selectedDeviceId.value] ?: EcoThresholds()
        if (selectedSensor != null) {
          checkForegroundNotifications(_selectedDeviceId.value, selectedSensor, selectedThresholds, now)
        }
        updateTelemetryHistory(now)
      }
    }
  }

  private fun stopSimulationTicker() {
    simulationJob?.cancel()
    simulationJob = null
  }

  private fun processRelayAndAutoRules(now: Long) {
    _relayStateMap.update { currentRelays ->
      currentRelays.mapValues { (deviceId, relay) ->
        val sensor = _sensorDataMap.value[deviceId] ?: return@mapValues relay
        val thresholds = _thresholdsMap.value[deviceId] ?: EcoThresholds()

        if (relay.isPumpOn) {
          val newCountdown = (relay.remainingCountdownSeconds - 2).coerceAtLeast(0)
          val newRunSeconds = relay.activeRunSeconds + 2

          val reachedTarget = sensor.soilZone1 >= thresholds.soilTargetPercent &&
            sensor.soilZone2 >= thresholds.soilTargetPercent &&
            sensor.soilZone3 >= thresholds.soilTargetPercent

          if (newCountdown <= 0 || reachedTarget) {
            addEventLog(
              deviceId = deviceId,
              type = "PUMP_AUTO_OFF",
              title = "Relay Pump Auto-Off Triggered",
              details = if (reachedTarget)
                "Target soil moisture (${thresholds.soilTargetPercent}%) reached across zones."
              else
                "Max pump run time limit reached (${thresholds.maxPumpRunTimeMinutes} min)."
            )
            return@mapValues relay.copy(
              isPumpOn = false,
              remainingCountdownSeconds = 0,
              activeRunSeconds = 0,
              triggeredByAuto = false
            )
          }

          if (relay.triggeredByAuto && sensor.isRaining) {
            addEventLog(
              deviceId = deviceId,
              type = "PUMP_AUTO_RAIN_ABORT",
              title = "Pump Stopped: Rain Detected",
              details = "Auto-irrigation paused due to natural rainfall."
            )
            return@mapValues relay.copy(
              isPumpOn = false,
              remainingCountdownSeconds = 0,
              activeRunSeconds = 0,
              triggeredByAuto = false
            )
          }

          relay.copy(
            remainingCountdownSeconds = newCountdown,
            activeRunSeconds = newRunSeconds
          )
        } else {
          if (relay.mode == RelayMode.AUTO) {
            val isBelowThreshold = sensor.soilAverage < thresholds.soilDryPercent ||
              sensor.soilZone1 < thresholds.soilDryPercent ||
              sensor.soilZone2 < thresholds.soilDryPercent ||
              sensor.soilZone3 < thresholds.soilDryPercent

            if (isBelowThreshold && !sensor.isRaining && sensor.isOnline(now)) {
              addEventLog(
                deviceId = deviceId,
                type = "PUMP_AUTO_ON",
                title = "Auto-Irrigation Pump Started",
                details = "Soil avg at ${sensor.soilAverage}% (below ${thresholds.soilDryPercent}% threshold) and no rain."
              )
              return@mapValues relay.copy(
                isPumpOn = true,
                remainingCountdownSeconds = thresholds.maxPumpRunTimeMinutes * 60,
                activeRunSeconds = 0,
                triggeredByAuto = true
              )
            }
          }
          relay
        }
      }
    }
  }

  private fun evaluateAlerts(now: Long) {
    val currentAlerts = mutableListOf<SystemAlert>()

    _sensorDataMap.value.forEach { (deviceId, sensor) ->
      val thresholds = _thresholdsMap.value[deviceId] ?: EcoThresholds()
      val deviceName = _devices.value.find { it.id == deviceId }?.name ?: deviceId

      if (sensor.soilZone1 < thresholds.soilDryPercent ||
        sensor.soilZone2 < thresholds.soilDryPercent ||
        sensor.soilZone3 < thresholds.soilDryPercent
      ) {
        val dryZones = mutableListOf<String>()
        if (sensor.soilZone1 < thresholds.soilDryPercent) dryZones.add("Zone 1 (${sensor.soilZone1}%)")
        if (sensor.soilZone2 < thresholds.soilDryPercent) dryZones.add("Zone 2 (${sensor.soilZone2}%)")
        if (sensor.soilZone3 < thresholds.soilDryPercent) dryZones.add("Zone 3 (${sensor.soilZone3}%)")

        currentAlerts.add(
          SystemAlert(
            id = "alert-dry-$deviceId",
            type = AlertType.DRY_SOIL,
            title = "Dry Soil Warning: $deviceName",
            message = "Moisture below ${thresholds.soilDryPercent}% in: ${dryZones.joinToString(", ")}.",
            severity = AlertSeverity.WARNING,
            timestampMs = now,
            deviceId = deviceId
          )
        )
      }

      if (sensor.temperatureC >= thresholds.highTempAlertThresholdC) {
        currentAlerts.add(
          SystemAlert(
            id = "alert-temp-$deviceId",
            type = AlertType.HIGH_TEMP,
            title = "High Temperature Alert: $deviceName",
            message = "Current temp is ${sensor.temperatureC}°C (exceeds ${thresholds.highTempAlertThresholdC}°C).",
            severity = AlertSeverity.CRITICAL,
            timestampMs = now,
            deviceId = deviceId
          )
        )
      }

      if (sensor.isRaining) {
        currentAlerts.add(
          SystemAlert(
            id = "alert-rain-$deviceId",
            type = AlertType.RAIN,
            title = "Precipitation Active: $deviceName",
            message = "Rain detected. Auto irrigation pump is safely inhibited.",
            severity = AlertSeverity.INFO,
            timestampMs = now,
            deviceId = deviceId
          )
        )
      }

      if (sensor.batteryPercent <= thresholds.lowBatteryAlertPercent) {
        currentAlerts.add(
          SystemAlert(
            id = "alert-batt-$deviceId",
            type = AlertType.LOW_BATTERY,
            title = "Low Battery: $deviceName",
            message = "Battery capacity is ${sensor.batteryPercent}% (threshold: ${thresholds.lowBatteryAlertPercent}%).",
            severity = AlertSeverity.WARNING,
            timestampMs = now,
            deviceId = deviceId
          )
        )
      }

      if (!sensor.isOnline(now)) {
        val sec = sensor.secondsSinceLastSeen(now)
        currentAlerts.add(
          SystemAlert(
            id = "alert-offline-$deviceId",
            type = AlertType.DEVICE_OFFLINE,
            title = "Node Offline: $deviceName",
            message = "Heartbeat lost $sec seconds ago (threshold: 60s). Check Wi-Fi or power supply.",
            severity = AlertSeverity.CRITICAL,
            timestampMs = now,
            deviceId = deviceId
          )
        )
      }
    }

    _alerts.value = currentAlerts
  }

  private fun updateTelemetryHistory(now: Long) {
    _telemetryHistoryMap.update { historyMap ->
      historyMap.mapValues { (deviceId, historyList) ->
        val sensor = _sensorDataMap.value[deviceId] ?: return@mapValues historyList
        val point = TelemetryPoint(
          timestampMs = now,
          tempC = sensor.temperatureC,
          humidity = sensor.humidityPercent,
          soilAvg = sensor.soilAverage,
          zone1 = sensor.soilZone1,
          zone2 = sensor.soilZone2,
          zone3 = sensor.soilZone3,
          lightLux = sensor.lightLux,
          batteryPercent = sensor.batteryPercent
        )
        (historyList + point).takeLast(240)
      }
    }
  }

  private fun generateInitialTelemetryHistory(): Map<String, List<TelemetryPoint>> {
    val now = System.currentTimeMillis()
    val map = mutableMapOf<String, List<TelemetryPoint>>()
    listOf("NODE-E82", "NODE-9A4", "NODE-1F0").forEach { devId ->
      val points = mutableListOf<TelemetryPoint>()
      for (i in 72 downTo 0) {
        val t = now - (i * 10 * 60 * 60 * 1000L)
        val wave = kotlin.math.sin(i * 0.3)
        val baseTemp = if (devId == "NODE-E82") 24.0 else 22.5
        val temp = baseTemp + (wave * 3.5) + (Random.nextDouble(-0.3, 0.3))
        val hum = 60.0 - (wave * 8.0) + (Random.nextDouble(-1.0, 1.0))
        val s1 = (45 + (wave * 12)).toInt().coerceIn(20, 85)
        val s2 = (35 + (wave * 10)).toInt().coerceIn(15, 75)
        val s3 = (70 + (wave * 8)).toInt().coerceIn(40, 95)
        val light = (40000 + (wave * 25000)).toInt().coerceIn(1500, 85000)
        val batt = (85 + (wave * 10)).toInt().coerceIn(40, 98)

        points.add(
          TelemetryPoint(
            timestampMs = t,
            tempC = ((temp * 10).toInt() / 10.0),
            humidity = ((hum * 10).toInt() / 10.0),
            soilAvg = (s1 + s2 + s3) / 3,
            zone1 = s1,
            zone2 = s2,
            zone3 = s3,
            lightLux = light,
            batteryPercent = batt
          )
        )
      }
      map[devId] = points
    }
    return map
  }
}
