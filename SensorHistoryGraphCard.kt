package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChartTimeRange
import com.example.data.model.TelemetryPoint
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HistorySensor(val label: String, val unit: String, val color: Color) {
  TEMPERATURE("Temperature", "°C", EcoAmber),
  HUMIDITY("Humidity", "%", EcoCyan),
  SOIL_AVG("Soil Average", "%", EcoEmerald),
  SOIL_ZONE_1("Zone 1 (Topsoil)", "%", EcoEmeraldLight),
  SOIL_ZONE_2("Zone 2 (Root)", "%", Color(0xFFFF9800)),
  SOIL_ZONE_3("Zone 3 (Subsurface)", "%", EcoWaterBlue),
  LIGHT("Sunlight", "kLux", EcoYellow),
  BATTERY("Battery", "%", Color(0xFFAB47BC))
}

@Composable
fun SensorHistoryGraphCard(
  historyPoints: List<TelemetryPoint>,
  modifier: Modifier = Modifier
) {
  var selectedRange by remember { mutableStateOf(ChartTimeRange.TWENTY_FOUR_HOURS) }
  var activeSensors by remember {
    mutableStateOf(setOf(HistorySensor.TEMPERATURE, HistorySensor.SOIL_AVG, HistorySensor.HUMIDITY))
  }
  var touchedIndex by remember { mutableStateOf<Int?>(null) }
  var showDataTable by remember { mutableStateOf(false) }

  val now = System.currentTimeMillis()
  val filteredPoints = remember(historyPoints, selectedRange) {
    val cutoff = now - selectedRange.durationMs
    val list = historyPoints.filter { it.timestampMs >= cutoff }
    if (list.size < 2) historyPoints.takeLast(12) else list
  }

  val activePoint = touchedIndex?.let { filteredPoints.getOrNull(it) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("sensor_history_graph_card"),
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
      // 1. Header: Title + Time Range Switcher
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
              imageVector = Icons.Default.Timeline,
              contentDescription = "Sensor History",
              tint = EcoEmerald,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Sensor History Graph",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Multi-Sensor Continuous Telemetry",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Time Range Pills (1h / 6h / 24h / 7d / 30d)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.padding(2.dp)
        ) {
          Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            ChartTimeRange.entries.forEach { range ->
              val isSelected = selectedRange == range
              Surface(
                onClick = {
                  selectedRange = range
                  touchedIndex = null
                },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) EcoEmerald else Color.Transparent,
                modifier = Modifier.testTag("history_range_${range.label}")
              ) {
                Text(
                  text = range.label,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      // 2. Sensor Toggle Chips (Select which curves to display on graph)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Visible Sensors (Tap to overlay):",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val primarySensors = listOf(
            HistorySensor.TEMPERATURE,
            HistorySensor.SOIL_AVG,
            HistorySensor.HUMIDITY,
            HistorySensor.LIGHT
          )
          primarySensors.forEach { sensor ->
            val isChecked = activeSensors.contains(sensor)
            FilterChip(
              selected = isChecked,
              onClick = {
                activeSensors = if (isChecked && activeSensors.size > 1) {
                  activeSensors - sensor
                } else {
                  activeSensors + sensor
                }
                touchedIndex = null
              },
              label = {
                Text(
                  text = sensor.label,
                  fontSize = 10.sp,
                  fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                )
              },
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) sensor.color else Color.Gray)
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = sensor.color.copy(alpha = 0.2f),
                selectedLabelColor = sensor.color
              ),
              modifier = Modifier.testTag("toggle_sensor_${sensor.name}")
            )
          }
        }

        // Secondary Sensors Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val secondarySensors = listOf(
            HistorySensor.SOIL_ZONE_1,
            HistorySensor.SOIL_ZONE_2,
            HistorySensor.SOIL_ZONE_3,
            HistorySensor.BATTERY
          )
          secondarySensors.forEach { sensor ->
            val isChecked = activeSensors.contains(sensor)
            FilterChip(
              selected = isChecked,
              onClick = {
                activeSensors = if (isChecked && activeSensors.size > 1) {
                  activeSensors - sensor
                } else {
                  activeSensors + sensor
                }
                touchedIndex = null
              },
              label = {
                Text(
                  text = sensor.label,
                  fontSize = 9.sp,
                  fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                )
              },
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) sensor.color else Color.Gray)
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = sensor.color.copy(alpha = 0.2f),
                selectedLabelColor = sensor.color
              ),
              modifier = Modifier.testTag("toggle_sensor_${sensor.name}")
            )
          }
        }
      }

      // 3. Floating HUD / Inspection Card on Touch
      if (activePoint != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(EcoEmerald.copy(alpha = 0.5f))
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(activePoint.timestampMs))
            Text(
              text = "Timestamp: $dateStr",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              activeSensors.forEach { s ->
                val (v, u) = getPointSensorValue(activePoint, s)
                Column {
                  Text(text = s.label, fontSize = 9.sp, color = s.color, fontWeight = FontWeight.SemiBold)
                  Text(text = "$v $u", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = s.color)
                }
              }
            }
          }
        }
      }

      // 4. Multi-Line Canvas Graph
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(210.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFF091410))
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
          .pointerInput(filteredPoints) {
            detectTapGestures(
              onTap = { offset ->
                if (filteredPoints.isNotEmpty()) {
                  val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                  touchedIndex = (fraction * (filteredPoints.size - 1)).toInt()
                }
              }
            )
          }
          .pointerInput(filteredPoints) {
            detectDragGestures { change, _ ->
              if (filteredPoints.isNotEmpty()) {
                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                touchedIndex = (fraction * (filteredPoints.size - 1)).toInt()
              }
            }
          }
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 14.dp)) {
          if (filteredPoints.size < 2) return@Canvas

          val w = size.width
          val h = size.height
          val stepX = w / (filteredPoints.size - 1)

          // Draw Horizontal Grid Lines with Scale Marks
          val gridSteps = 4
          for (i in 0..gridSteps) {
            val y = h * (i.toFloat() / gridSteps)
            drawLine(
              color = Color.White.copy(alpha = 0.08f),
              start = Offset(0f, y),
              end = Offset(w, y),
              strokeWidth = 1f
            )
          }

          // Render each active sensor's line
          activeSensors.forEach { sensor ->
            val path = Path()
            filteredPoints.forEachIndexed { idx, point ->
              val (value, _) = getPointSensorValue(point, sensor)
              val normalized = normalizeSensorValue(value, sensor)
              val x = idx * stepX
              val y = h * (1f - normalized)

              if (idx == 0) {
                path.moveTo(x, y)
              } else {
                path.lineTo(x, y)
              }
            }

            drawPath(
              path = path,
              color = sensor.color,
              style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
          }

          // Draw Active Touch Cursor
          touchedIndex?.let { idx ->
            if (idx in filteredPoints.indices) {
              val touchX = idx * stepX
              // Cursor line
              drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(touchX, 0f),
                end = Offset(touchX, h),
                strokeWidth = 1.5.dp.toPx()
              )

              // Draw dot for each active sensor at the cursor position
              val pt = filteredPoints[idx]
              activeSensors.forEach { s ->
                val (valNum, _) = getPointSensorValue(pt, s)
                val norm = normalizeSensorValue(valNum, s)
                val dotY = h * (1f - norm)
                drawCircle(
                  color = s.color,
                  radius = 4.dp.toPx(),
                  center = Offset(touchX, dotY)
                )
                drawCircle(
                  color = Color.White,
                  radius = 2.dp.toPx(),
                  center = Offset(touchX, dotY)
                )
              }
            }
          }
        }
      }

      // 5. Statistics Summary Pills for Active Sensors
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          activeSensors.take(3).forEach { s ->
            val vals = filteredPoints.map { getPointSensorValue(it, s).first }
            val avg = if (vals.isNotEmpty()) vals.average() else 0.0
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = s.color.copy(alpha = 0.12f),
              modifier = Modifier.padding(vertical = 2.dp)
            ) {
              Text(
                text = "${s.label.take(8)} Avg: ${"%.1f".format(avg)}${s.unit}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = s.color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        }

        // Toggle Data Points Table Button
        TextButton(
          onClick = { showDataTable = !showDataTable },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = if (showDataTable) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(text = if (showDataTable) "Hide Table" else "View Points", fontSize = 11.sp)
        }
      }

      // 6. Data Points Table View (Collapsible)
      if (showDataTable) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Recent Telemetry Point Stream (${filteredPoints.size} records)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold
            )
            filteredPoints.takeLast(8).reversed().forEach { pt ->
              val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(pt.timestampMs))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = timeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "${pt.tempC}°C", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoAmber)
                Text(text = "${pt.humidity}% RH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoCyan)
                Text(text = "${pt.soilAvg}% Soil", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoEmerald)
                Text(text = "${pt.batteryPercent}% Batt", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFAB47BC))
              }
              HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f), thickness = 0.5.dp)
            }
          }
        }
      }
    }
  }
}

private fun getPointSensorValue(point: TelemetryPoint, sensor: HistorySensor): Pair<Double, String> {
  return when (sensor) {
    HistorySensor.TEMPERATURE -> point.tempC to "°C"
    HistorySensor.HUMIDITY -> point.humidity to "%"
    HistorySensor.SOIL_AVG -> point.soilAvg.toDouble() to "%"
    HistorySensor.SOIL_ZONE_1 -> point.zone1.toDouble() to "%"
    HistorySensor.SOIL_ZONE_2 -> point.zone2.toDouble() to "%"
    HistorySensor.SOIL_ZONE_3 -> point.zone3.toDouble() to "%"
    HistorySensor.LIGHT -> (point.lightLux / 1000.0) to "kLux"
    HistorySensor.BATTERY -> point.batteryPercent.toDouble() to "%"
  }
}

private fun normalizeSensorValue(value: Double, sensor: HistorySensor): Float {
  return when (sensor) {
    HistorySensor.TEMPERATURE -> ((value - 10.0) / 40.0).toFloat().coerceIn(0.05f, 0.95f)
    HistorySensor.HUMIDITY -> (value / 100.0).toFloat().coerceIn(0.05f, 0.95f)
    HistorySensor.SOIL_AVG,
    HistorySensor.SOIL_ZONE_1,
    HistorySensor.SOIL_ZONE_2,
    HistorySensor.SOIL_ZONE_3 -> (value / 100.0).toFloat().coerceIn(0.05f, 0.95f)
    HistorySensor.LIGHT -> (value / 100.0).toFloat().coerceIn(0.05f, 0.95f)
    HistorySensor.BATTERY -> (value / 100.0).toFloat().coerceIn(0.05f, 0.95f)
  }
}
