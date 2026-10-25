package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.data.model.SensorData
import com.example.ui.theme.*

@Composable
fun HeartbeatStatusHeader(
  device: Device?,
  sensorData: SensorData?,
  isOnline: Boolean,
  isDemoMode: Boolean,
  onDeviceSwitchClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("heartbeat_status_header"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Device Selector Pill
        Surface(
          onClick = onDeviceSwitchClick,
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.testTag("device_selector_pill")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Sensors,
              contentDescription = "Device",
              tint = EcoEmerald,
              modifier = Modifier.size(18.dp)
            )
            Column {
              Text(
                text = device?.name ?: "Select Device",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${device?.id ?: "NODE"} • ${device?.locationName ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Icon(
              imageVector = Icons.Default.ArrowDropDown,
              contentDescription = "Switch",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // ONLINE / OFFLINE Status Pill
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = if (isOnline) EcoEmerald.copy(alpha = 0.15f) else EcoRed.copy(alpha = 0.15f),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
              if (isOnline) EcoEmerald.copy(alpha = 0.5f) else EcoRed.copy(alpha = 0.5f)
            )
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .scale(if (isOnline) pulseScale else 1f)
                .clip(CircleShape)
                .background(if (isOnline) EcoEmerald else EcoRed)
            )
            Text(
              text = if (isOnline) "ONLINE" else "OFFLINE",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp,
              color = if (isOnline) EcoEmerald else EcoRed
            )
          }
        }
      }

      // Heartbeat & Telemetry Metadata
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val secondsAgo = sensorData?.secondsSinceLastSeen() ?: 0L
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Last seen",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = if (isOnline) {
              "Heartbeat: ${secondsAgo}s ago (${sensorData?.formattedLastUpdate() ?: "--"})"
            } else {
              "Heartbeat lost: ${secondsAgo}s ago (>60s timeout)"
            },
            fontSize = 11.sp,
            color = if (isOnline) MaterialTheme.colorScheme.onSurfaceVariant else EcoRed,
            fontWeight = if (!isOnline) FontWeight.Bold else FontWeight.Normal
          )
        }

        if (isDemoMode) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = EcoCyan.copy(alpha = 0.15f)
          ) {
            Text(
              text = "DEMO SIMULATOR",
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = EcoCyan
            )
          }
        }
      }
    }
  }
}

@Composable
fun TempHumidityCard(
  tempC: Double,
  humidity: Double,
  modifier: Modifier = Modifier
) {
  var useFahrenheit by remember { mutableStateOf(false) }
  val tempDisplay = if (useFahrenheit) {
    val f = (tempC * 9.0 / 5.0) + 32.0
    "%.1f°F".format(f)
  } else {
    "%.1f°C".format(tempC)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("temp_humidity_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(EcoAmber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Thermostat,
              contentDescription = "Temperature",
              tint = EcoAmber,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Climate",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Air Temperature & Relative Humidity",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // °C / °F Unit Toggle
        Surface(
          onClick = { useFahrenheit = !useFahrenheit },
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.testTag("temp_unit_toggle")
        ) {
          Text(
            text = if (useFahrenheit) "Show °C" else "Show °F",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Temperature Metric Box
        MetricBox(
          label = "Temperature",
          value = tempDisplay,
          subLabel = if (tempC > 30.0) "WARM" else "OPTIMAL",
          icon = Icons.Default.Thermostat,
          accentColor = if (tempC > 32.0) EcoRed else EcoAmber,
          modifier = Modifier.weight(1f)
        )

        // Humidity Metric Box
        MetricBox(
          label = "Humidity",
          value = "%.1f%%".format(humidity),
          subLabel = if (humidity in 45.0..70.0) "IDEAL" else "MONITOR",
          icon = Icons.Default.WaterDrop,
          accentColor = EcoCyan,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
fun LightAndRainCard(
  lightLux: Int,
  lightPercent: Int,
  isRaining: Boolean,
  onToggleRain: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("light_rain_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(EcoYellow.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.WbSunny,
              contentDescription = "Light & Rain",
              tint = EcoYellow,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Light & Rain",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Ambient Light & Precipitation",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Rain Toggle Test Button
        FilledTonalButton(
          onClick = onToggleRain,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isRaining) EcoWaterBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
          ),
          modifier = Modifier.testTag("toggle_rain_button")
        ) {
          Icon(
            imageVector = if (isRaining) Icons.Default.Thunderstorm else Icons.Default.Water,
            contentDescription = "Toggle Rain",
            modifier = Modifier.size(16.dp),
            tint = if (isRaining) EcoWaterBlue else MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isRaining) "Simulate Dry" else "Simulate Rain",
            fontSize = 11.sp
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Light Level Column
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.BrightnessMedium,
                contentDescription = null,
                tint = EcoYellow,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Light Level",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = "$lightLux Lux",
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            // Progress Bar for Light %
            LinearProgressIndicator(
              progress = { (lightPercent / 100f).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = EcoYellow,
              trackColor = EcoYellow.copy(alpha = 0.2f),
            )
            Text(
              text = "$lightPercent% Sunlight Intensity",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Rain Status Column
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (isRaining) EcoWaterBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = if (isRaining) Icons.Default.Thunderstorm else Icons.Default.WbSunny,
                contentDescription = null,
                tint = if (isRaining) EcoWaterBlue else EcoEmerald,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Rain Sensor",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = if (isRaining) "RAINING" else "DRY",
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              color = if (isRaining) EcoWaterBlue else EcoEmerald
            )
            Text(
              text = if (isRaining) "Precipitation active • Auto pump paused" else "Dry surface • Normal irrigation",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 12.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun PowerSystemCard(
  batteryPercent: Int,
  solarCharging: Boolean,
  modifier: Modifier = Modifier
) {
  val battColor = when {
    batteryPercent <= 20 -> EcoRed
    batteryPercent <= 40 -> EcoAmber
    else -> EcoEmerald
  }

  val battStatusLabel = when {
    solarCharging -> "SOLAR CHARGING"
    batteryPercent <= 20 -> "LOW BATTERY ALERT"
    batteryPercent <= 50 -> "NORMAL DISCHARGE"
    else -> "HEALTHY CAPACITY"
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("power_system_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(EcoEmerald.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = "Power",
              tint = EcoEmerald,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Battery Power",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "On-Device Calculated Battery Telemetry",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Solar charging indicator pill
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (solarCharging) EcoEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = if (solarCharging) Icons.Default.SolarPower else Icons.Default.Nightlight,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = if (solarCharging) EcoEmerald else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = if (solarCharging) "SOLAR CHARGING" else "IDLE / NIGHT",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (solarCharging) EcoEmerald else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Battery Percentage Metric Box
        MetricBox(
          label = "Battery Charge",
          value = "$batteryPercent%",
          subLabel = battStatusLabel,
          icon = if (solarCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
          accentColor = battColor,
          modifier = Modifier.weight(1f)
        )

        // Solar / Storage Status Box
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.EnergySavingsLeaf,
                contentDescription = null,
                tint = if (solarCharging) EcoEmerald else EcoCyan,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Charge State",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            LinearProgressIndicator(
              progress = { (batteryPercent / 100f).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = battColor,
              trackColor = battColor.copy(alpha = 0.2f),
            )
            Text(
              text = if (batteryPercent <= 20) "Low battery: recharge needed" else if (solarCharging) "Active solar replenishing" else "Running on storage",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
fun MetricBox(
  label: String,
  value: String,
  subLabel: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Text(
        text = value,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subLabel,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = accentColor
      )
    }
  }
}
