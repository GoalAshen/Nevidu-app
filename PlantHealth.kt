package com.example.data.model

enum class PlantHealthTier(val label: String, val emoji: String) {
  THRIVING("Thriving", "🌱"),
  GOOD("Good", "🌿"),
  MODERATE_STRESS("Mild Stress", "⚠️"),
  CRITICAL("Critical Attention", "🚨")
}

data class PlantHealthAnalysis(
  val overallScore: Int, // 0 - 100
  val tier: PlantHealthTier,
  val statusHeadline: String,
  val summaryText: String,
  val hydrationScore: Int,      // 0 - 100
  val hydrationStatus: String,  // e.g. "Optimal (58% Avg)"
  val thermalScore: Int,        // 0 - 100
  val thermalStatus: String,    // e.g. "Comfortable (25.4°C)"
  val sunlightScore: Int,       // 0 - 100
  val sunlightStatus: String,   // e.g. "Full Sunlight (48k Lux)"
  val rainStatusText: String,   // e.g. "Precipitation Detected" or "Clear & Dry"
  val recommendations: List<String>,
  val requiresRelayWatering: Boolean
) {
  companion object {
    fun evaluate(sensorData: SensorData?, isNodePoweredOn: Boolean = true): PlantHealthAnalysis {
      if (sensorData == null) {
        return PlantHealthAnalysis(
          overallScore = 0,
          tier = PlantHealthTier.CRITICAL,
          statusHeadline = "No Sensor Data Received",
          summaryText = "Waiting for live telemetry from Firebase...",
          hydrationScore = 0,
          hydrationStatus = "Telemetry Pending",
          thermalScore = 0,
          thermalStatus = "Telemetry Pending",
          sunlightScore = 0,
          sunlightStatus = "Telemetry Pending",
          rainStatusText = "--",
          recommendations = listOf("Ensure the node is paired and connected to Firebase."),
          requiresRelayWatering = false
        )
      }

      if (!isNodePoweredOn) {
        return PlantHealthAnalysis(
          overallScore = 70,
          tier = PlantHealthTier.GOOD,
          statusHeadline = "Node in Standby / Powered Off",
          summaryText = "Hardware device is turned off in low-power deep sleep mode. Sensor stream is suspended.",
          hydrationScore = sensorData.soilAverage,
          hydrationStatus = "Last recorded: ${sensorData.soilAverage}%",
          thermalScore = 75,
          thermalStatus = "${sensorData.temperatureC}°C",
          sunlightScore = sensorData.lightPercent,
          sunlightStatus = "${sensorData.lightLux} Lux",
          rainStatusText = if (sensorData.isRaining) "Raining" else "Dry",
          recommendations = listOf("Switch ON Node Device Power to wake device and resume active sensor stream."),
          requiresRelayWatering = false
        )
      }

      val soilAvg = sensorData.soilAverage
      val minSoil = minOf(sensorData.soilZone1, sensorData.soilZone2, sensorData.soilZone3)

      // Hydration Score: 50% - 75% is optimal (100)
      val hydrationScore = when {
        soilAvg in 50..75 -> 100
        soilAvg in 36..49 -> 60 + ((soilAvg - 35) * 40 / 14)
        soilAvg in 76..88 -> 95 - ((soilAvg - 75) * 35 / 13)
        soilAvg <= 35 -> (soilAvg * 55 / 35).coerceAtLeast(10)
        else -> 45
      }

      val hydrationStatus = when {
        minSoil < 35 -> "Zone Alert: Minimum Zone at $minSoil% (Dry)"
        soilAvg in 50..75 -> "Optimal Hydration ($soilAvg% Avg)"
        soilAvg > 75 -> "Saturated Soil ($soilAvg% Avg)"
        else -> "Moderate Moisture ($soilAvg% Avg)"
      }

      // Thermal Score: 20-28°C is optimal
      val temp = sensorData.temperatureC
      val thermalScore = when {
        temp in 20.0..28.0 -> 100
        temp in 15.0..19.9 -> 80
        temp in 28.1..33.0 -> 75
        temp > 33.0 -> (60 - (temp - 33.0) * 8).toInt().coerceAtLeast(15)
        else -> 40
      }

      val thermalStatus = when {
        temp > 33.0 -> "High Heat Alert: %.1f°C (Firmware Alert)".format(temp)
        temp < 15.0 -> "Cool Conditions: %.1f°C".format(temp)
        else -> "Comfortable: %.1f°C • %.0f%% RH".format(temp, sensorData.humidityPercent)
      }

      // Sunlight Score
      val lux = sensorData.lightLux
      val sunlightScore = when {
        lux in 25000..70000 -> 100
        lux in 10000..24999 -> 80
        lux > 70000 -> 75
        else -> 50
      }

      val sunlightStatus = when {
        lux in 25000..70000 -> "Optimal Photosynthesis (%d Lux)".format(lux)
        lux > 70000 -> "Intense Sunlight (%d Lux)".format(lux)
        else -> "Low / Ambient Light (%d Lux)".format(lux)
      }

      val rainStatusText = if (sensorData.isRaining) "Precipitation Detected (Natural Watering)" else "Clear & Dry"

      // Weighted Total Health Score
      val overallScore = ((hydrationScore * 0.45) + (thermalScore * 0.30) + (sunlightScore * 0.25)).toInt().coerceIn(0, 100)

      val tier = when {
        overallScore >= 88 -> PlantHealthTier.THRIVING
        overallScore >= 72 -> PlantHealthTier.GOOD
        overallScore >= 50 -> PlantHealthTier.MODERATE_STRESS
        else -> PlantHealthTier.CRITICAL
      }

      val requiresRelay = minSoil < 35 && !sensorData.isRaining

      val recs = mutableListOf<String>()
      if (requiresRelay) {
        recs.add("Soil moisture dropped below limit ($minSoil%). Turn Relay ON to irrigate.")
      } else if (sensorData.isRaining) {
        recs.add("Natural rainfall detected. Keep Relay OFF to avoid root rot.")
      } else {
        recs.add("Soil moisture levels are well-balanced across root zones.")
      }

      if (temp > 33.0) {
        recs.add("Firmware High-Temp threshold reached (%.1f°C). Provide shade canopy.".format(temp))
      } else if (temp in 20.0..28.0) {
        recs.add("Atmospheric temperature and humidity are ideal for steady growth.")
      }

      if (lux < 15000) {
        recs.add("Light intensity is low. Check for canopy shadowing.")
      }

      val headline = when (tier) {
        PlantHealthTier.THRIVING -> "Plants Are Thriving"
        PlantHealthTier.GOOD -> "Plant Health Is Stable"
        PlantHealthTier.MODERATE_STRESS -> "Mild Crop Stress Detected"
        PlantHealthTier.CRITICAL -> "Urgent Plant Attention Needed"
      }

      val summary = when (tier) {
        PlantHealthTier.THRIVING -> "Optimal root hydration and atmospheric microclimate. Vigor index is high."
        PlantHealthTier.GOOD -> "Root moisture and microclimate parameters remain within healthy targets."
        PlantHealthTier.MODERATE_STRESS -> "One or more environmental parameters (soil dryness or heat) require attention."
        PlantHealthTier.CRITICAL -> "Immediate irrigation or environmental adjustment recommended to preserve plant vigor."
      }

      return PlantHealthAnalysis(
        overallScore = overallScore,
        tier = tier,
        statusHeadline = headline,
        summaryText = summary,
        hydrationScore = hydrationScore,
        hydrationStatus = hydrationStatus,
        thermalScore = thermalScore,
        thermalStatus = thermalStatus,
        sunlightScore = sunlightScore,
        sunlightStatus = sunlightStatus,
        rainStatusText = rainStatusText,
        recommendations = recs,
        requiresRelayWatering = requiresRelay
      )
    }
  }
}
