package com.example

import com.example.data.model.*
import com.example.data.remote.FirebaseSettings
import com.example.data.repository.EcoRepository
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testHeartbeatOnlineOfflineThreshold() {
    val now = 1000000L
    val recentSensor = SensorData(
      temperatureC = 25.0,
      humidityPercent = 60.0,
      soilZone1 = 40,
      soilZone2 = 40,
      soilZone3 = 40,
      lightLux = 50000,
      isRaining = false,
      batteryPercent = 90,
      solarCharging = true,
      lastSeenEpochMs = now - 30_000L // 30 seconds ago
    )
    assertTrue("Should be online within 60s", recentSensor.isOnline(now))
    assertEquals(30L, recentSensor.secondsSinceLastSeen(now))

    val staleSensor = recentSensor.copy(lastSeenEpochMs = now - 65_000L) // 65 seconds ago
    assertFalse("Should be offline when older than 60s", staleSensor.isOnline(now))
    assertEquals(65L, staleSensor.secondsSinceLastSeen(now))
  }

  @Test
  fun testSoilMoistureZoneState() {
    val sensor = SensorData(
      temperatureC = 22.0,
      humidityPercent = 55.0,
      soilZone1 = 25, // Below 35 -> DRY
      soilZone2 = 50, // Between 35 and 70 -> GOOD
      soilZone3 = 78, // Above 70 -> WET
      lightLux = 30000,
      isRaining = false,
      batteryPercent = 85,
      solarCharging = true,
      lastSeenEpochMs = System.currentTimeMillis()
    )

    assertEquals(SoilZoneState.DRY, sensor.getZoneState(sensor.soilZone1, 35))
    assertEquals(SoilZoneState.GOOD, sensor.getZoneState(sensor.soilZone2, 35))
    assertEquals(SoilZoneState.WET, sensor.getZoneState(sensor.soilZone3, 35))
    assertEquals(51, sensor.soilAverage)
  }

  @Test
  fun testFirebasePaths() {
    val devId = "NODE-E82"
    val config = FirebaseSettings(databaseUrl = "https://ecocontrol-test-rtdb.firebaseio.com")
    assertEquals("https://ecocontrol-test-rtdb.firebaseio.com/devices/NODE-E82/sensors.json", config.getSensorsUrl(devId))
    assertEquals("https://ecocontrol-test-rtdb.firebaseio.com/devices/NODE-E82/status.json", config.getStatusUrl(devId))
    assertEquals("https://ecocontrol-test-rtdb.firebaseio.com/devices/NODE-E82/commands.json", config.getCommandsUrl(devId))
    assertEquals("https://ecocontrol-test-rtdb.firebaseio.com/devices/NODE-E82/settings.json", config.getSettingsUrl(devId))
    assertEquals("https://ecocontrol-test-rtdb.firebaseio.com/devices/NODE-E82/history.json", config.getHistoryUrl(devId))
  }

  @Test
  fun testCsvExportFormatting() {
    val repo = EcoRepository()
    val csv = repo.exportLogsToCsv()
    assertTrue(csv.startsWith("Timestamp,Date_Time,Device_ID,Event_Type,Title,Details"))
    assertTrue(csv.contains("NODE-E82"))
  }

  @Test
  fun testLanguageDictionariesAvailable() {
    val languages = com.example.localization.EcoLanguage.entries
    assertEquals(9, languages.size)
    for (lang in languages) {
      val strings = com.example.localization.EcoTranslations.get(lang)
      assertNotNull(strings.appName)
      assertTrue(strings.appName.isNotEmpty())
      assertNotNull(strings.navDashboard)
      assertNotNull(strings.navPair)
      assertNotNull(strings.soilMoisture)
      assertFalse(strings.appName.contains("ESP32", ignoreCase = true))
      assertFalse(strings.appSubtitle.contains("ESP32", ignoreCase = true))
    }
  }

  @Test
  fun testBlePairingManagerSimulation() = kotlinx.coroutines.runBlocking {
    val repo = EcoRepository()
    val manager = com.example.pairing.BlePairingManager(repo, this)

    assertEquals(com.example.pairing.BlePairingStep.IDLE, manager.step.value)
    manager.startScanning(null)
    assertEquals(com.example.pairing.BlePairingStep.SCANNING, manager.step.value)

    // Wait slightly for simulated nodes to arrive
    kotlinx.coroutines.delay(2200)
    assertTrue("Should have discovered simulated BLE nodes", manager.discoveredNodes.value.isNotEmpty())

    val targetNode = manager.discoveredNodes.value.first()
    manager.selectAndConnect(targetNode)
    assertEquals(targetNode, manager.selectedNode.value)

    // Wait for connection and hardware characteristics check to complete
    kotlinx.coroutines.delay(2500)
    assertEquals(com.example.pairing.BlePairingStep.WIFI_CONFIG, manager.step.value)

    // Provision Wi-Fi
    manager.provisionWifi("MyFarmNet", "SecurePass123")
    kotlinx.coroutines.delay(2500)
    assertEquals(com.example.pairing.BlePairingStep.SUCCESS, manager.step.value)
  }

  @Test
  fun testPlantHealthEvaluation() {
    val healthySensor = SensorData(
      temperatureC = 24.5,
      humidityPercent = 62.0,
      soilZone1 = 60,
      soilZone2 = 58,
      soilZone3 = 64,
      lightLux = 45000,
      isRaining = false,
      batteryPercent = 90,
      solarCharging = true,
      lastSeenEpochMs = System.currentTimeMillis()
    )

    val health = PlantHealthAnalysis.evaluate(healthySensor, isNodePoweredOn = true)
    assertTrue("Overall score should be high for optimal sensors", health.overallScore >= 85)
    assertEquals(PlantHealthTier.THRIVING, health.tier)
    assertFalse("Healthy plants do not require immediate relay watering", health.requiresRelayWatering)

    val drySensor = healthySensor.copy(
      soilZone1 = 20,
      soilZone2 = 18,
      soilZone3 = 24
    )
    val dryHealth = PlantHealthAnalysis.evaluate(drySensor, isNodePoweredOn = true)
    assertTrue("Dry soil should trigger relay watering requirement", dryHealth.requiresRelayWatering)
  }

  @Test
  fun testDevicePowerToggle() {
    val repo = EcoRepository()
    val devId = "NODE-E82"
    val initialDev = repo.devices.value.first { it.id == devId }
    assertTrue(initialDev.isPoweredOn)

    // Turn device OFF
    repo.toggleDevicePower(devId, false)
    val turnedOffDev = repo.devices.value.first { it.id == devId }
    assertFalse(turnedOffDev.isPoweredOn)

    // Turn device ON
    repo.toggleDevicePower(devId, true)
    val turnedOnDev = repo.devices.value.first { it.id == devId }
    assertTrue(turnedOnDev.isPoweredOn)
  }
}

