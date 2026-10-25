package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.remote.FirebaseSettings
import com.example.ui.theme.*

@Composable
fun FirebaseConfigCard(
  config: FirebaseSettings,
  isHeartbeatPaused: Boolean,
  onSaveConfig: (FirebaseSettings) -> Unit,
  onToggleDemoMode: (Boolean) -> Unit,
  onToggleSimulateHeartbeatTimeout: () -> Unit,
  modifier: Modifier = Modifier
) {
  var dbUrl by remember(config.databaseUrl) { mutableStateOf(config.databaseUrl) }
  var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
  var projectId by remember(config.projectId) { mutableStateOf(config.projectId) }
  var ownerEmail by remember(config.ownerEmail) { mutableStateOf(config.ownerEmail) }
  var authPassword by remember(config.authPassword) { mutableStateOf(config.authPassword) }
  var showPassword by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("firebase_config_card"),
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
      // Header
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
              imageVector = Icons.Default.Cloud,
              contentDescription = "Firebase",
              tint = EcoAmber,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Firebase RTDB & Backend",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Realtime Database & Telemetry Paths",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Demo Mode Badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (config.isDemoMode) EcoCyan.copy(alpha = 0.15f) else EcoEmerald.copy(alpha = 0.15f)
        ) {
          Text(
            text = if (config.isDemoMode) "DEMO MODE" else "FIREBASE LIVE",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (config.isDemoMode) EcoCyan else EcoEmerald
          )
        }
      }

      // Demo Mode Switch Card
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Demo Mode Simulation",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Generates live fluctuating sensor data and automatic relay rules until real Firebase keys are added.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = config.isDemoMode,
            onCheckedChange = onToggleDemoMode,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = EcoCyan
            ),
            modifier = Modifier.testTag("demo_mode_switch")
          )
        }
      }

      // Heartbeat Timeout Simulation Button (To test ONLINE / OFFLINE state)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isHeartbeatPaused) EcoRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = androidx.compose.ui.graphics.SolidColor(
            if (isHeartbeatPaused) EcoRed else Color.Transparent
          )
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isHeartbeatPaused) "Heartbeat Paused (Simulating Offline)" else "Test Device Offline State",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (isHeartbeatPaused) EcoRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Stops sending heartbeats. Once lastSeen exceeds 60 seconds, status automatically flips to OFFLINE.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          FilledTonalButton(
            onClick = onToggleSimulateHeartbeatTimeout,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = if (isHeartbeatPaused) EcoRed else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("test_offline_button")
          ) {
            Text(
              text = if (isHeartbeatPaused) "Resume" else "Cut Ping",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (isHeartbeatPaused) Color.White else Color.Black
            )
          }
        }
      }

      // Backend RTDB Schema Paths Visualizer
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Registered Node Data Paths (Owner Read/Write):",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val paths = listOf(
          FirebaseSettings.PATH_SENSORS to "Climate, Light, Rain, Battery, Soil Zones 1-3",
          FirebaseSettings.PATH_STATUS to "lastSeen, wifiRssi, ipAddress, firmwareVersion",
          FirebaseSettings.PATH_COMMANDS to "relayState, buzzerPulse, speakerAlarm, leds",
          FirebaseSettings.PATH_SETTINGS to "soilDryThreshold, maxRunTime, highTempAlert",
          FirebaseSettings.PATH_HISTORY to "timestamped telemetry log points"
        )
        paths.forEach { (path, desc) ->
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = path, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EcoEmerald)
              Text(text = desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      // Firebase Credentials Form
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Firebase Credentials & Project Keys",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
          value = dbUrl,
          onValueChange = { dbUrl = it },
          label = { Text("Database URL (RTDB)") },
          modifier = Modifier.fillMaxWidth().testTag("input_db_url"),
          singleLine = true
        )

        OutlinedTextField(
          value = apiKey,
          onValueChange = { apiKey = it },
          label = { Text("Firebase Web API Key") },
          modifier = Modifier.fillMaxWidth().testTag("input_api_key"),
          singleLine = true
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = projectId,
            onValueChange = { projectId = it },
            label = { Text("Project ID") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = ownerEmail,
            onValueChange = { ownerEmail = it },
            label = { Text("Owner Email") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        OutlinedTextField(
          value = authPassword,
          onValueChange = { authPassword = it },
          label = { Text("Auth Password (Firebase Auth)") },
          modifier = Modifier.fillMaxWidth().testTag("input_auth_password"),
          singleLine = true,
          visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
              Icon(
                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showPassword) "Hide password" else "Show password"
              )
            }
          }
        )

        Button(
          onClick = {
            onSaveConfig(
              config.copy(
                databaseUrl = dbUrl,
                apiKey = apiKey,
                projectId = projectId,
                ownerEmail = ownerEmail,
                authPassword = authPassword
              )
            )
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("save_firebase_config_button")
        ) {
          Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Save Firebase Configuration", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
