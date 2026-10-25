package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.TranslationStrings
import com.example.pairing.BlePairingStep
import com.example.pairing.DiscoveredBleNode
import com.example.ui.theme.*

@Composable
fun NodePairingScreen(
  pairingStep: BlePairingStep,
  discoveredNodes: List<DiscoveredBleNode>,
  selectedNode: DiscoveredBleNode?,
  statusMessage: String,
  strings: TranslationStrings,
  onStartScan: () -> Unit,
  onSelectNode: (DiscoveredBleNode) -> Unit,
  onProvisionWifi: (String, String) -> Unit,
  onFinish: () -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier
) {
  var wifiSsid by remember { mutableStateOf("EcoNet-2.4G") }
  var wifiPass by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("node_pairing_screen_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
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
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(EcoCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.BluetoothSearching,
              contentDescription = null,
              tint = EcoCyan,
              modifier = Modifier.size(22.dp)
            )
          }
          Column {
            Text(
              text = strings.pairNewNode,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = strings.pairSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (pairingStep != BlePairingStep.IDLE) {
          TextButton(onClick = onReset) {
            Text(text = "Reset", fontSize = 12.sp)
          }
        }
      }

      // Step-dependent content
      when (pairingStep) {
        BlePairingStep.IDLE -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = EcoCyan, modifier = Modifier.size(18.dp))
                  Text(text = strings.bleScanSubtitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(
                  text = "Power on your smart environment node within 5 meters. The app will scan via Bluetooth Low Energy, read hardware identifiers, and push local Wi-Fi credentials.",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 16.sp
                )
              }
            }

            Button(
              onClick = onStartScan,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("start_ble_scan_button"),
              colors = ButtonDefaults.buttonColors(containerColor = EcoEmerald, contentColor = Color.Black)
            ) {
              Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = strings.startScan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
          }
        }

        BlePairingStep.SCANNING -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Radar Animation
            RadarSweepAnimation()

            Text(
              text = strings.scanning,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = EcoCyan
            )

            if (discoveredNodes.isNotEmpty()) {
              Text(
                text = "${strings.nearbyNodesFound} (${discoveredNodes.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
              )

              discoveredNodes.forEach { node ->
                DiscoveredNodeItem(
                  node = node,
                  strings = strings,
                  onClick = { onSelectNode(node) }
                )
              }
            }
          }
        }

        BlePairingStep.CONNECTING, BlePairingStep.READING_INFO -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(color = EcoCyan, strokeWidth = 3.dp)
            Text(
              text = statusMessage,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        BlePairingStep.WIFI_CONFIG -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Hardware Info Card Read from Node
            selectedNode?.let { node ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = EcoCyan.copy(alpha = 0.1f),
                border = CardDefaults.outlinedCardBorder().copy(
                  brush = androidx.compose.ui.graphics.SolidColor(EcoCyan.copy(alpha = 0.4f))
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = strings.hardwareInfoTitle,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = EcoCyan
                    )
                    Surface(shape = RoundedCornerShape(6.dp), color = EcoEmerald.copy(alpha = 0.2f)) {
                      Text(
                        text = strings.verified,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EcoEmerald,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = strings.nodeId, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = node.id, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = strings.macAddress, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = node.macAddress, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = strings.signalStrength, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${node.rssiDbm} dBm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EcoEmerald)
                  }
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = strings.firmwareVersion, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = node.firmwareVersion, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            // Wi-Fi Configuration Inputs
            Text(
              text = strings.wifiProvisioningTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = strings.wifiProvisioningSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
              value = wifiSsid,
              onValueChange = { wifiSsid = it },
              label = { Text(strings.networkSsid) },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_wifi_ssid"),
              singleLine = true,
              leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) }
            )

            OutlinedTextField(
              value = wifiPass,
              onValueChange = { wifiPass = it },
              label = { Text(strings.wifiPassword) },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_wifi_pass"),
              singleLine = true,
              visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
              trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                  Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                  )
                }
              }
            )

            Button(
              onClick = {
                if (wifiSsid.isNotBlank()) {
                  onProvisionWifi(wifiSsid, wifiPass)
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_provision_button"),
              colors = ButtonDefaults.buttonColors(containerColor = EcoEmerald, contentColor = Color.Black)
            ) {
              Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = strings.provisionButton, fontWeight = FontWeight.Bold)
            }
          }
        }

        BlePairingStep.PROVISIONING -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            CircularProgressIndicator(color = EcoEmerald, strokeWidth = 3.5.dp)
            Text(
              text = strings.provisioningProgress,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        BlePairingStep.SUCCESS -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(EcoEmerald.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EcoEmerald, modifier = Modifier.size(40.dp))
            }

            Text(
              text = strings.pairSuccessTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = EcoEmerald
            )
            Text(
              text = strings.pairSuccessDesc,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
              onClick = onFinish,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("finish_pairing_button"),
              colors = ButtonDefaults.buttonColors(containerColor = EcoEmerald, contentColor = Color.Black)
            ) {
              Text(text = strings.finishPairing, fontWeight = FontWeight.Bold)
            }
          }
        }

        BlePairingStep.ERROR -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = EcoRed, modifier = Modifier.size(40.dp))
            Text(text = "Pairing Error", fontWeight = FontWeight.Bold, color = EcoRed)
            Text(text = statusMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onStartScan) {
              Text("Retry")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RadarSweepAnimation() {
  val infiniteTransition = rememberInfiniteTransition(label = "radar")
  val radarRadius by infiniteTransition.animateFloat(
    initialValue = 10f,
    targetValue = 90f,
    animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
    label = "radarRadius"
  )
  val radarAlpha by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
    label = "radarAlpha"
  )

  Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = EcoCyan.copy(alpha = radarAlpha),
        radius = radarRadius.dp.toPx(),
        style = Stroke(width = 2.dp.toPx())
      )
      drawCircle(
        color = EcoCyan,
        radius = 8.dp.toPx()
      )
    }
  }
}

@Composable
private fun DiscoveredNodeItem(
  node: DiscoveredBleNode,
  strings: TranslationStrings,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    border = CardDefaults.outlinedCardBorder(),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("discovered_node_${node.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(EcoEmerald.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Sensors, contentDescription = null, tint = EcoEmerald, modifier = Modifier.size(18.dp))
        }
        Column {
          Text(text = node.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text(text = "${node.id} • ${node.macAddress}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Surface(shape = RoundedCornerShape(6.dp), color = EcoEmerald.copy(alpha = 0.15f)) {
          Text(
            text = "${node.rssiDbm} dBm",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = EcoEmerald,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "Tap to pair", fontSize = 10.sp, color = EcoCyan, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}
