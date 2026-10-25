package com.example.data.remote

/**
 * EcoControl Firebase Configuration and Path Definitions.
 *
 * All Firebase configuration is kept centrally in this file per project requirements.
 * Runs in DEMO MODE with simulated live device telemetry by default until user keys are added.
 */
data class FirebaseSettings(
  val databaseUrl: String = "https://ecocontrol-default-rtdb.firebaseio.com",
  val apiKey: String = "",
  val projectId: String = "ecocontrol-system",
  val ownerEmail: String = "admin@ecocontrol.io",
  val authPassword: String = "",
  val isDemoMode: Boolean = true,
  val simulateHeartbeatTimeout: Boolean = false // If true, pauses heartbeat updates to demo the OFFLINE state (>60s)
) {
  val isConfigured: Boolean
    get() = databaseUrl.isNotBlank() && apiKey.isNotBlank()

  // Realtime Database REST URL builders for the device paths
  fun getSensorsUrl(deviceId: String): String =
    "${sanitizeUrl(databaseUrl)}/devices/$deviceId/sensors.json"

  fun getStatusUrl(deviceId: String): String =
    "${sanitizeUrl(databaseUrl)}/devices/$deviceId/status.json"

  fun getCommandsUrl(deviceId: String): String =
    "${sanitizeUrl(databaseUrl)}/devices/$deviceId/commands.json"

  fun getSettingsUrl(deviceId: String): String =
    "${sanitizeUrl(databaseUrl)}/devices/$deviceId/settings.json"

  fun getHistoryUrl(deviceId: String): String =
    "${sanitizeUrl(databaseUrl)}/devices/$deviceId/history.json"

  private fun sanitizeUrl(url: String): String {
    return url.trimEnd('/')
  }

  companion object {
    const val PATH_SENSORS = "/devices/{deviceId}/sensors"
    const val PATH_STATUS = "/devices/{deviceId}/status"
    const val PATH_COMMANDS = "/devices/{deviceId}/commands"
    const val PATH_SETTINGS = "/devices/{deviceId}/settings"
    const val PATH_HISTORY = "/devices/{deviceId}/history"

    val DEFAULT = FirebaseSettings()
  }
}
