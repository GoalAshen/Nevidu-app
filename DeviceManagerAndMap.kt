package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.ui.theme.*

@Composable
fun DeviceManagerAndMap(
  devices: List<Device>,
  selectedDeviceId: String,
  onSelectDevice: (String) -> Unit,
  onAddDevice: (String, String, Double, Double) -> Unit,
  onUpdateLocation: (String, Double, Double, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddDialog by remember { mutableStateOf(false) }
  val activeDevice = devices.find { it.id == selectedDeviceId } ?: devices.firstOrNull()

  var editLat by remember(activeDevice) { mutableStateOf(activeDevice?.latitude?.toString() ?: "37.7749") }
  var editLng by remember(activeDevice) { mutableStateOf(activeDevice?.longitude?.toString() ?: "-122.4194") }
  var editLocationName by remember(activeDevice) { mutableStateOf(activeDevice?.locationName ?: "") }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("device_manager_map_view"),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Device Switcher Header Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("device_switcher_card"),
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
                imageVector = Icons.Default.Sensors,
                contentDescription = "Devices",
                tint = EcoEmerald,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "EcoControl Node Fleet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${devices.size} active environmental nodes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          FilledTonalButton(
            onClick = { showAddDialog = true },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.testTag("add_device_button")
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Add Node", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Horizontal Device Selector Cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          devices.forEach { dev ->
            val isSelected = dev.id == selectedDeviceId
            Surface(
              onClick = { onSelectDevice(dev.id) },
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) EcoEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                  if (isSelected) EcoEmerald else Color.Transparent
                )
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("device_item_${dev.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) EcoEmerald else Color.Gray)
                  )
                  Column {
                    Text(
                      text = dev.name,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "${dev.id} • ${dev.locationName}",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = EcoEmerald,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "${dev.wifiRssi} dBm",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    }

    // 2. Interactive Map & Coordinate Setter
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("device_map_card"),
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
                imageVector = Icons.Default.Map,
                contentDescription = "Map",
                tint = EcoWaterBlue,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "Device Location Map",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Tap on map to reposition ${activeDevice?.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Custom Interactive Canvas Map
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F1E19))
            .pointerInput(activeDevice) {
              detectTapGestures { offset ->
                if (activeDevice != null) {
                  // Map canvas coordinate to lat/lng offset
                  val latOffset = (offset.y / size.height - 0.5) * -0.04
                  val lngOffset = (offset.x / size.width - 0.5) * 0.04
                  val newLat = ((37.7749 + latOffset) * 10000).toInt() / 10000.0
                  val newLng = ((-122.4194 + lngOffset) * 10000).toInt() / 10000.0

                  editLat = newLat.toString()
                  editLng = newLng.toString()
                  onUpdateLocation(activeDevice.id, newLat, newLng, editLocationName.ifBlank { activeDevice.locationName })
                }
              }
            }
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Draw Topographic / Grid Lines
            val gridStep = 40.dp.toPx()
            var x = 0f
            while (x < w) {
              drawLine(
                color = EcoEmerald.copy(alpha = 0.1f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
              )
              x += gridStep
            }
            var y = 0f
            while (y < h) {
              drawLine(
                color = EcoEmerald.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
              )
              y += gridStep
            }

            // Draw Decorative River / Stream Curve
            val streamPath = Path().apply {
              moveTo(0f, h * 0.6f)
              cubicTo(w * 0.3f, h * 0.55f, w * 0.7f, h * 0.75f, w, h * 0.5f)
            }
            drawPath(
              path = streamPath,
              color = EcoWaterBlue.copy(alpha = 0.25f),
              style = Stroke(width = 16.dp.toPx())
            )

            // Plot Device Location Pins
            devices.forEach { dev ->
              val isSelected = dev.id == selectedDeviceId
              // Normalize lat/lng to canvas
              val nx = ((dev.longitude - (-122.44)) / 0.05).toFloat().coerceIn(0.15f, 0.85f)
              val ny = ((37.79 - dev.latitude) / 0.03).toFloat().coerceIn(0.15f, 0.85f)

              val pinX = nx * w
              val pinY = ny * h

              // Pulse radar ring for active device
              if (isSelected) {
                drawCircle(
                  color = EcoEmerald.copy(alpha = 0.25f),
                  radius = 24.dp.toPx(),
                  center = Offset(pinX, pinY)
                )
              }

              // Pin Base
              drawCircle(
                color = if (isSelected) EcoEmerald else EcoCyan,
                radius = if (isSelected) 9.dp.toPx() else 6.dp.toPx(),
                center = Offset(pinX, pinY)
              )
              drawCircle(
                color = Color.White,
                radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                center = Offset(pinX, pinY)
              )
            }
          }

          // Map Compass & Coordinates Overlay
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color.Black.copy(alpha = 0.6f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "GPS: ${activeDevice?.latitude ?: 37.77}, ${activeDevice?.longitude ?: -122.41}",
              fontSize = 9.sp,
              color = Color.White
            )
          }
        }

        // Edit Coordinates Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Edit Coordinates for ${activeDevice?.name}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )

          OutlinedTextField(
            value = editLocationName,
            onValueChange = { editLocationName = it },
            label = { Text("Location Name / Sector") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_location_name"),
            singleLine = true
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = editLat,
              onValueChange = { editLat = it },
              label = { Text("Latitude") },
              modifier = Modifier
                .weight(1f)
                .testTag("input_latitude"),
              singleLine = true
            )
            OutlinedTextField(
              value = editLng,
              onValueChange = { editLng = it },
              label = { Text("Longitude") },
              modifier = Modifier
                .weight(1f)
                .testTag("input_longitude"),
              singleLine = true
            )
          }

          Button(
            onClick = {
              if (activeDevice != null) {
                val lat = editLat.toDoubleOrNull() ?: activeDevice.latitude
                val lng = editLng.toDoubleOrNull() ?: activeDevice.longitude
                onUpdateLocation(activeDevice.id, lat, lng, editLocationName.ifBlank { activeDevice.locationName })
              }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("save_location_button")
          ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Save Device Coordinates", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  // Add Device Dialog
  if (showAddDialog) {
    var newName by remember { mutableStateOf("") }
    var newLocation by remember { mutableStateOf("") }
    var newLat by remember { mutableStateOf("37.7750") }
    var newLng by remember { mutableStateOf("-122.4200") }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Register New Eco Node", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Device Name (e.g. Nursery Bed)") },
            singleLine = true
          )
          OutlinedTextField(
            value = newLocation,
            onValueChange = { newLocation = it },
            label = { Text("Location (e.g. East Field)") },
            singleLine = true
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = newLat,
              onValueChange = { newLat = it },
              label = { Text("Latitude") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
            OutlinedTextField(
              value = newLng,
              onValueChange = { newLng = it },
              label = { Text("Longitude") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newName.isNotBlank()) {
              val lat = newLat.toDoubleOrNull() ?: 37.7750
              val lng = newLng.toDoubleOrNull() ?: -122.4200
              onAddDevice(newName, newLocation.ifBlank { "Greenhouse Zone" }, lat, lng)
              showAddDialog = false
            }
          }
        ) {
          Text("Add Node")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
