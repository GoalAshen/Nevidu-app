package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Device(
  val id: String,
  val name: String,
  val locationName: String,
  val latitude: Double,
  val longitude: Double,
  val firmwareVersion: String = "NodeOS-v2.4.1",
  val wifiSsid: String = "EcoMesh-2.4G",
  val wifiRssi: Int = -58,
  val ipAddress: String = "192.168.1.142",
  val isPoweredOn: Boolean = true
)

enum class SoilZoneState {
  DRY,   // Below dry threshold (Red)
  GOOD,  // Normal moisture range (Green)
  WET    // High moisture > 70% (Blue)
}

data class SensorData(
  val temperatureC: Double,
  val humidityPercent: Double,
  val soilZone1: Int, // 0 - 100%
  val soilZone2: Int,
  val soilZone3: Int,
  val lightLux: Int,
  val isRaining: Boolean,
  val batteryPercent: Int, // 0 - 100% calculated by on-device node
  val solarCharging: Boolean,
  val lastSeenEpochMs: Long
) {
  val temperatureF: Double
    get() = (temperatureC * 9.0 / 5.0) + 32.0

  val lightPercent: Int
    get() = (lightLux / 1000).coerceIn(0, 100)

  val soilAverage: Int
    get() = ((soilZone1 + soilZone2 + soilZone3) / 3.0).toInt()

  fun getZoneState(zoneValue: Int, dryThreshold: Int): SoilZoneState {
    return when {
      zoneValue < dryThreshold -> SoilZoneState.DRY
      zoneValue > 70 -> SoilZoneState.WET
      else -> SoilZoneState.GOOD
    }
  }

  fun isOnline(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
    // Offline if lastSeen heartbeat is older than 60s
    return (currentTimeMs - lastSeenEpochMs) <= 60_000L
  }

  fun secondsSinceLastSeen(currentTimeMs: Long = System.currentTimeMillis()): Long {
    return ((currentTimeMs - lastSeenEpochMs) / 1000L).coerceAtLeast(0L)
  }

  fun formattedLastUpdate(): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(lastSeenEpochMs))
  }
}

enum class RelayMode {
  AUTO,
  MANUAL
}

data class RelayState(
  val isPumpOn: Boolean,
  val mode: RelayMode,
  val activeRunSeconds: Int = 0,
  val remainingCountdownSeconds: Int = 0,
  val triggeredByAuto: Boolean = false
)

data class EcoThresholds(
  val soilDryPercent: Int = 35,          // Pump triggers below this in AUTO
  val soilTargetPercent: Int = 65,       // Pump stops at this in AUTO
  val highTempAlertThresholdC: Double = 33.0,
  val lowBatteryAlertPercent: Int = 20,
  val maxPumpRunTimeMinutes: Int = 15
)

data class HardwareControls(
  val buzzerActive: Boolean = false,
  val speakerAlertActive: Boolean = false,
  val ledRed: Boolean = false,
  val ledBlue: Boolean = false,
  val ledGreen: Boolean = true,
  val ledYellow: Boolean = false
)

data class DailySchedule(
  val enabled: Boolean = true,
  val startHour: Int = 6,
  val startMinute: Int = 30,
  val durationMinutes: Int = 10,
  val activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7) // 1=Mon, 7=Sun
) {
  fun formattedTime(): String {
    val h = if (startHour < 10) "0$startHour" else "$startHour"
    val m = if (startMinute < 10) "0$startMinute" else "$startMinute"
    return "$h:$m"
  }
}

data class TelemetryPoint(
  val timestampMs: Long,
  val tempC: Double,
  val humidity: Double,
  val soilAvg: Int,
  val zone1: Int,
  val zone2: Int,
  val zone3: Int,
  val lightLux: Int = 45000,
  val batteryPercent: Int = 85
)

enum class AlertType {
  DRY_SOIL,
  HIGH_TEMP,
  RAIN,
  LOW_BATTERY,
  DEVICE_OFFLINE,
  SYSTEM_INFO
}

enum class AlertSeverity {
  INFO,
  WARNING,
  CRITICAL
}

data class SystemAlert(
  val id: String,
  val type: AlertType,
  val title: String,
  val message: String,
  val severity: AlertSeverity,
  val timestampMs: Long = System.currentTimeMillis(),
  val deviceId: String
)

data class EventLog(
  val id: String,
  val timestampMs: Long,
  val deviceId: String,
  val type: String, // PUMP_START, PUMP_STOP, RAIN_DETECTED, ALERT, THRESHOLD_CHANGE
  val title: String,
  val details: String
) {
  fun formattedTime(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestampMs))
  }
}

enum class ChartTimeRange(val label: String, val durationMs: Long) {
  ONE_HOUR("1h", 60 * 60 * 1000L),
  SIX_HOURS("6h", 6 * 60 * 60 * 1000L),
  TWENTY_FOUR_HOURS("24h", 24 * 60 * 60 * 1000L),
  SEVEN_DAYS("7d", 7 * 24 * 60 * 60 * 1000L),
  THIRTY_DAYS("30d", 30 * 24 * 60 * 60 * 1000L)
}
