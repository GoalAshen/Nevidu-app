package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun NodePowerCard(
  device: Device?,
  isPoweredOn: Boolean,
  onTogglePower: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val powerColor = if (isPoweredOn) EcoEmerald else EcoRed

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("node_power_card"),
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
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(powerColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PowerSettingsNew,
              contentDescription = "Device Power",
              tint = powerColor,
              modifier = Modifier.size(22.dp)
            )
          }
          Column {
            Text(
              text = "Device Node Power",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (isPoweredOn) "Device is ACTIVE & transmitting" else "Device is in SLEEP / STANDBY",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Quick Switch
        Switch(
          checked = isPoweredOn,
          onCheckedChange = { onTogglePower(it) },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = EcoEmerald,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = EcoRed
          ),
          modifier = Modifier.testTag("node_power_switch")
        )
      }

      // Large tactile toggle card
      Surface(
        onClick = { onTogglePower(!isPoweredOn) },
        shape = RoundedCornerShape(14.dp),
        color = if (isPoweredOn) EcoEmerald.copy(alpha = 0.12f) else EcoRed.copy(alpha = 0.12f),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = androidx.compose.ui.graphics.SolidColor(
            if (isPoweredOn) EcoEmerald.copy(alpha = 0.5f) else EcoRed.copy(alpha = 0.5f)
          )
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("node_power_tactile_button")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isPoweredOn) "NODE IS POWERED ON" else "NODE IS POWERED OFF",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp,
              color = powerColor
            )
            Text(
              text = if (isPoweredOn) "Tap to put device into deep sleep standby" else "Tap to wake up device and resume telemetry",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Icon(
            imageVector = if (isPoweredOn) Icons.Default.CheckCircle else Icons.Default.PowerOff,
            contentDescription = null,
            tint = powerColor,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}

@Composable
fun RelayControlCard(
  relayState: RelayState?,
  thresholds: EcoThresholds?,
  currentSoilAvg: Int,
  isRaining: Boolean,
  onTogglePump: (Boolean) -> Unit,
  onSetMode: (RelayMode) -> Unit,
  modifier: Modifier = Modifier
) {
  val isPumpOn = relayState?.isPumpOn == true
  val mode = relayState?.mode ?: RelayMode.AUTO

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("relay_control_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header: Relay Title + Auto/Manual Mode Switch
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
              .background(EcoCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Power,
              contentDescription = "Relay",
              tint = EcoCyan,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Relay / 12V Load Switch",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Pump & Solenoid Output Control",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Mode Switcher (AUTO vs MANUAL)
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.padding(2.dp)
        ) {
          Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            ModeChip(
              label = "AUTO",
              selected = mode == RelayMode.AUTO,
              onClick = { onSetMode(RelayMode.AUTO) },
              modifier = Modifier.testTag("relay_mode_auto_chip")
            )
            ModeChip(
              label = "MANUAL",
              selected = mode == RelayMode.MANUAL,
              onClick = { onSetMode(RelayMode.MANUAL) },
              modifier = Modifier.testTag("relay_mode_manual_chip")
            )
          }
        }
      }

      // Big tactile Relay ON / OFF Button
      Surface(
        onClick = { onTogglePump(!isPumpOn) },
        shape = RoundedCornerShape(16.dp),
        color = if (isPumpOn) EcoCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = androidx.compose.ui.graphics.SolidColor(
            if (isPumpOn) EcoCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
          )
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(84.dp)
          .testTag("relay_big_toggle_button")
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isPumpOn) EcoCyan else MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, if (isPumpOn) EcoCyanLight else Color.Transparent, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Pump Power",
                tint = if (isPumpOn) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
              )
            }

            Column {
              Text(
                text = if (isPumpOn) "RELAY ACTIVE (12V ON)" else "RELAY IDLE (OFF)",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (isPumpOn) EcoCyan else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isPumpOn) {
                  val remainingSec = relayState?.remainingCountdownSeconds ?: 0
                  val mins = remainingSec / 60
                  val secs = remainingSec % 60
                  "Auto-shutoff in %02d:%02d".format(mins, secs)
                } else {
                  "Tap to switch relay ON / OFF"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Visual Indicator Switch
          Switch(
            checked = isPumpOn,
            onCheckedChange = { onTogglePump(it) },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = EcoCyan
            ),
            modifier = Modifier.testTag("relay_switch")
          )
        }
      }

      // Automated Irrigation Logic Info
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
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
              imageVector = Icons.Default.AutoMode,
              contentDescription = null,
              tint = EcoEmerald,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Automated Irrigation Mode",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = EcoEmerald
            )
          }

          val dryThreshold = thresholds?.soilDryPercent ?: 35
          val targetMoisture = thresholds?.soilTargetPercent ?: 65
          val isBelowDry = currentSoilAvg < dryThreshold

          Text(
            text = "• Rule 1: Relay turns ON when Soil Moisture < $dryThreshold% (Current: $currentSoilAvg%) AND Rain = Dry (${if (isRaining) "Raining ⚠️" else "Dry ✓"})",
            fontSize = 11.sp,
            color = if (isBelowDry && !isRaining) EcoEmerald else MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "• Rule 2: Relay turns OFF when Soil Moisture reaches $targetMoisture% OR Max Run Time (${thresholds?.maxPumpRunTimeMinutes ?: 15}m) expires",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun ModeChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (selected) EcoEmerald else Color.Transparent,
    modifier = modifier
  ) {
    Text(
      text = label,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun HardwareFirmwareLimitsCard(
  sensorData: SensorData?,
  thresholds: EcoThresholds?,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("hardware_firmware_limits_card"),
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
            imageVector = Icons.Default.Memory,
            contentDescription = "Firmware",
            tint = EcoAmber,
            modifier = Modifier.size(20.dp)
          )
        }
        Column {
          Text(
            text = "Autonomous Firmware Safety Limits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Hardcoded limits & buzzer/LED triggers on device",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Text(
        text = "The microcontroller device autonomously manages all acoustic buzzer alarms, LED visual indicators, and sensor safety limits directly on the hardware. The mobile app strictly reads telemetry from Firebase and allows power / relay toggling.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp
      )

      // Grid of firmware limits
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FirmwareLimitItem(
          label = "Soil Dry Threshold (Buzzer & Red LED)",
          limitText = "< ${thresholds?.soilDryPercent ?: 35}%",
          currentText = "Current: ${sensorData?.soilAverage ?: 0}%",
          isTriggered = (sensorData?.soilAverage ?: 100) < (thresholds?.soilDryPercent ?: 35),
          activeAlert = "Buzzer Alarm & Red LED Alert Triggered"
        )

        FirmwareLimitItem(
          label = "High Temperature Warning (Yellow LED)",
          limitText = "> ${thresholds?.highTempAlertThresholdC ?: 33.0}°C",
          currentText = "Current: ${sensorData?.temperatureC ?: 0.0}°C",
          isTriggered = (sensorData?.temperatureC ?: 0.0) > (thresholds?.highTempAlertThresholdC ?: 33.0),
          activeAlert = "Yellow Warning Indicator Active"
        )

        FirmwareLimitItem(
          label = "Low Battery Safety Cutoff",
          limitText = "< ${thresholds?.lowBatteryAlertPercent ?: 20}%",
          currentText = "Current: ${sensorData?.batteryPercent ?: 100}%",
          isTriggered = (sensorData?.batteryPercent ?: 100) < (thresholds?.lowBatteryAlertPercent ?: 20),
          activeAlert = "Low Battery Power Save Activated"
        )

        FirmwareLimitItem(
          label = "Normal Operational Heartbeat",
          limitText = "System Healthy",
          currentText = "Pulse: Green LED",
          isTriggered = false,
          activeAlert = ""
        )
      }
    }
  }
}

@Composable
private fun FirmwareLimitItem(
  label: String,
  limitText: String,
  currentText: String,
  isTriggered: Boolean,
  activeAlert: String
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isTriggered) EcoRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        if (isTriggered && activeAlert.isNotEmpty()) {
          Text(text = activeAlert, fontSize = 10.sp, color = EcoRed, fontWeight = FontWeight.Bold)
        } else {
          Text(text = currentText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isTriggered) EcoRed else MaterialTheme.colorScheme.surfaceVariant
      ) {
        Text(
          text = limitText,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = if (isTriggered) Color.White else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
  }
}

@Composable
fun DailyScheduleCard(
  schedule: DailySchedule,
  onUpdateSchedule: (DailySchedule) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("daily_schedule_card"),
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
              .background(EcoWaterBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = "Schedule",
              tint = EcoWaterBlue,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Daily Relay Irrigation Schedule",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Automated timer start time and duration",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Switch(
          checked = schedule.enabled,
          onCheckedChange = { onUpdateSchedule(schedule.copy(enabled = it)) },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = EcoWaterBlue
          ),
          modifier = Modifier.testTag("schedule_enable_switch")
        )
      }

      if (schedule.enabled) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Time Box
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "Start Time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = schedule.formattedTime(),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EcoWaterBlue
              )
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                  onClick = {
                    val nextHour = (schedule.startHour + 1) % 24
                    onUpdateSchedule(schedule.copy(startHour = nextHour))
                  },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text("+1h", fontSize = 10.sp)
                }
                Button(
                  onClick = {
                    val nextMin = (schedule.startMinute + 15) % 60
                    onUpdateSchedule(schedule.copy(startMinute = nextMin))
                  },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text("+15m", fontSize = 10.sp)
                }
              }
            }
          }

          // Duration Box
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "Run Duration",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${schedule.durationMinutes} min",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EcoEmerald
              )
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                  onClick = {
                    val d = (schedule.durationMinutes - 5).coerceAtLeast(5)
                    onUpdateSchedule(schedule.copy(durationMinutes = d))
                  },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text("-5m", fontSize = 10.sp)
                }
                Button(
                  onClick = {
                    val d = (schedule.durationMinutes + 5).coerceAtMost(60)
                    onUpdateSchedule(schedule.copy(durationMinutes = d))
                  },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text("+5m", fontSize = 10.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}
