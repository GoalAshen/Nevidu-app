package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.AlertSeverity
import com.example.data.model.AlertType
import com.example.data.model.EventLog
import com.example.data.model.SystemAlert
import com.example.ui.theme.*

@Composable
fun AlertsAndLogsView(
  alerts: List<SystemAlert>,
  eventLogs: List<EventLog>,
  notificationsEnabled: Boolean = true,
  onToggleNotifications: (Boolean) -> Unit = {},
  onSendTestNotification: () -> Unit = {},
  onDismissAlert: (String) -> Unit,
  onExportCsv: () -> Unit,
  onClearLogs: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf("ALL") }

  val filteredLogs = remember(eventLogs, selectedFilter) {
    if (selectedFilter == "ALL") eventLogs
    else eventLogs.filter { it.type.contains(selectedFilter, ignoreCase = true) }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("alerts_and_logs_view"),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Notification System Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("notification_system_card"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                .background(EcoCyan.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Notification System",
                tint = EcoCyan,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "System Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Real-time Android status bar alerts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = notificationsEnabled,
            onCheckedChange = onToggleNotifications,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = EcoCyan
            ),
            modifier = Modifier.testTag("toggle_system_notifications")
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Dispatches high-priority push notifications for dry soil, heat alerts, rain, and node connectivity drops.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
          )

          Spacer(modifier = Modifier.width(8.dp))

          FilledTonalButton(
            onClick = onSendTestNotification,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.testTag("send_test_notification_button")
          ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Test Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
    // 1. In-App Alerts Banner Section
    if (alerts.isNotEmpty()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("active_alerts_card"),
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(EcoRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = "Alerts",
                  tint = EcoRed,
                  modifier = Modifier.size(20.dp)
                )
              }
              Column {
                Text(
                  text = "Active System Alerts (${alerts.size})",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Real-time safety triggers & hardware notifications",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          alerts.forEach { alert ->
            AlertItemCard(alert = alert, onDismiss = { onDismissAlert(alert.id) })
          }
        }
      }
    }

    // 2. Event Log Table with CSV Export
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("event_log_table_card"),
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
                imageVector = Icons.Default.ListAlt,
                contentDescription = "Logs",
                tint = EcoEmerald,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "Event Log History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${filteredLogs.size} events captured",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // CSV Export Button
          FilledTonalButton(
            onClick = onExportCsv,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("export_csv_button")
          ) {
            Icon(
              imageVector = Icons.Default.FileDownload,
              contentDescription = "Export CSV",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Filter Pills Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("ALL", "PUMP", "RAIN", "ALERT", "SETTINGS").forEach { filter ->
            val isSelected = selectedFilter == filter
            Surface(
              onClick = { selectedFilter = filter },
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) EcoEmerald else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.testTag("log_filter_$filter")
            ) {
              Text(
                text = filter,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        // Event Log Items Table
        if (filteredLogs.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No events matching the selected filter",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            filteredLogs.take(20).forEach { log ->
              EventLogRow(log = log)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AlertItemCard(
  alert: SystemAlert,
  onDismiss: () -> Unit
) {
  val alertColor = when (alert.severity) {
    AlertSeverity.CRITICAL -> EcoRed
    AlertSeverity.WARNING -> EcoAmber
    AlertSeverity.INFO -> EcoWaterBlue
  }

  val alertIcon = when (alert.type) {
    AlertType.DRY_SOIL -> Icons.Default.WaterDrop
    AlertType.HIGH_TEMP -> Icons.Default.Thermostat
    AlertType.RAIN -> Icons.Default.Thunderstorm
    AlertType.LOW_BATTERY -> Icons.Default.BatteryAlert
    AlertType.DEVICE_OFFLINE -> Icons.Default.SensorsOff
    AlertType.SYSTEM_INFO -> Icons.Default.Info
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = alertColor.copy(alpha = 0.12f),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = androidx.compose.ui.graphics.SolidColor(alertColor.copy(alpha = 0.5f))
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(alertColor.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = alertIcon,
            contentDescription = null,
            tint = alertColor,
            modifier = Modifier.size(18.dp)
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = alert.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = alertColor
          )
          Text(
            text = alert.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Node: ${alert.deviceId}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      IconButton(
        onClick = onDismiss,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Dismiss",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Composable
private fun EventLogRow(log: EventLog) {
  val iconColor = when {
    log.type.contains("PUMP") -> EcoCyan
    log.type.contains("RAIN") -> EcoWaterBlue
    log.type.contains("ALERT") -> EcoRed
    log.type.contains("SETTINGS") -> EcoAmber
    else -> EcoEmerald
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(iconColor)
        )
        Column {
          Text(
            text = log.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "${log.deviceId} • ${log.details}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
          )
        }
      }

      Text(
        text = log.formattedTime().takeLast(8), // HH:mm:ss
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp)
      )
    }
  }
}
