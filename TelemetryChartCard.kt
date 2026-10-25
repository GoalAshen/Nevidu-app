package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
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

enum class ChartMetricType(val title: String, val unit: String, val color: Color) {
  TEMPERATURE("Temperature", "°C", EcoAmber),
  HUMIDITY("Humidity", "%", EcoCyan),
  SOIL_MOISTURE("Soil Moisture", "%", EcoEmerald)
}

@Composable
fun TelemetryChartCard(
  historyPoints: List<TelemetryPoint>,
  modifier: Modifier = Modifier
) {
  var selectedRange by remember { mutableStateOf(ChartTimeRange.TWENTY_FOUR_HOURS) }
  var selectedMetric by remember { mutableStateOf(ChartMetricType.TEMPERATURE) }
  var touchedIndex by remember { mutableStateOf<Int?>(null) }

  // Filter points based on selected range
  val now = System.currentTimeMillis()
  val filteredPoints = remember(historyPoints, selectedRange) {
    val cutoff = now - selectedRange.durationMs
    val list = historyPoints.filter { it.timestampMs >= cutoff }
    if (list.size < 2) historyPoints.takeLast(10) else list
  }

  // Extract values
  val values = remember(filteredPoints, selectedMetric) {
    filteredPoints.map { pt ->
      when (selectedMetric) {
        ChartMetricType.TEMPERATURE -> pt.tempC.toFloat()
        ChartMetricType.HUMIDITY -> pt.humidity.toFloat()
        ChartMetricType.SOIL_MOISTURE -> pt.soilAvg.toFloat()
      }
    }
  }

  val minVal = values.minOrNull() ?: 0f
  val maxVal = values.maxOrNull() ?: 100f
  val currentVal = values.lastOrNull() ?: 0f

  val activePoint = touchedIndex?.let { idx ->
    filteredPoints.getOrNull(idx)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("telemetry_chart_card"),
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
      // Header: Title + Metric Selector Chips
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
              .background(selectedMetric.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.ShowChart,
              contentDescription = "Chart",
              tint = selectedMetric.color,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Telemetry Trends",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${selectedMetric.title} historical log",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Time Range Pills (1h / 24h / 7d)
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
                onClick = { selectedRange = range },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier.testTag("chart_range_${range.label}")
              ) {
                Text(
                  text = range.label,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      // Metric Switcher Tab Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ChartMetricType.entries.forEach { metric ->
          val isSelected = selectedMetric == metric
          Surface(
            onClick = {
              selectedMetric = metric
              touchedIndex = null
            },
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) metric.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) metric.color else Color.Transparent
              )
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("chart_metric_${metric.name}")
          ) {
            Row(
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(metric.color)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = metric.title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) metric.color else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // Value Summary Banner
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          val displayVal = if (activePoint != null) {
            when (selectedMetric) {
              ChartMetricType.TEMPERATURE -> "%.1f".format(activePoint.tempC)
              ChartMetricType.HUMIDITY -> "%.1f".format(activePoint.humidity)
              ChartMetricType.SOIL_MOISTURE -> "${activePoint.soilAvg}"
            }
          } else {
            "%.1f".format(currentVal)
          }

          val timeLabel = if (activePoint != null) {
            SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()).format(Date(activePoint.timestampMs))
          } else {
            "Latest Reading"
          }

          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = displayVal,
              fontSize = 26.sp,
              fontWeight = FontWeight.ExtraBold,
              color = selectedMetric.color
            )
            Text(
              text = selectedMetric.unit,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = selectedMetric.color,
              modifier = Modifier.padding(bottom = 3.dp)
            )
          }
          Text(
            text = timeLabel,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          StatPill(label = "MIN", value = "%.1f".format(minVal) + selectedMetric.unit)
          StatPill(label = "MAX", value = "%.1f".format(maxVal) + selectedMetric.unit)
        }
      }

      // Smooth Custom Canvas Line Chart
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
          .pointerInput(values) {
            detectTapGestures(
              onTap = { offset ->
                if (values.isNotEmpty()) {
                  val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                  touchedIndex = (fraction * (values.size - 1)).toInt()
                }
              }
            )
          }
          .pointerInput(values) {
            detectDragGestures { change, _ ->
              if (values.isNotEmpty()) {
                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                touchedIndex = (fraction * (values.size - 1)).toInt()
              }
            }
          }
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
          if (values.size < 2) return@Canvas

          val w = size.width
          val h = size.height
          val padding = 10f
          val chartHeight = h - (padding * 2)

          val vMin = minVal * 0.95f
          val vMax = (maxVal * 1.05f).coerceAtLeast(vMin + 1f)
          val vRange = vMax - vMin

          // Draw Horizontal Grid Lines
          val gridLines = 3
          for (i in 0..gridLines) {
            val y = padding + (chartHeight * (i.toFloat() / gridLines))
            drawLine(
              color = Color.Gray.copy(alpha = 0.15f),
              start = Offset(0f, y),
              end = Offset(w, y),
              strokeWidth = 1.dp.toPx()
            )
          }

          // Build Line & Area Paths
          val stepX = w / (values.size - 1)
          val linePath = Path()
          val fillPath = Path()

          fillPath.moveTo(0f, h)

          values.forEachIndexed { i, value ->
            val x = i * stepX
            val normalized = (value - vMin) / vRange
            val y = padding + (chartHeight * (1f - normalized))

            if (i == 0) {
              linePath.moveTo(x, y)
              fillPath.lineTo(x, y)
            } else {
              linePath.lineTo(x, y)
              fillPath.lineTo(x, y)
            }
          }

          fillPath.lineTo(w, h)
          fillPath.close()

          // Draw Filled Gradient
          drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
              colors = listOf(
                selectedMetric.color.copy(alpha = 0.35f),
                selectedMetric.color.copy(alpha = 0.0f)
              )
            )
          )

          // Draw Line Stroke
          drawPath(
            path = linePath,
            color = selectedMetric.color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
          )

          // Draw Touch Indicator Crosshair
          touchedIndex?.let { idx ->
            if (idx in values.indices) {
              val touchX = idx * stepX
              val touchVal = values[idx]
              val touchNorm = (touchVal - vMin) / vRange
              val touchY = padding + (chartHeight * (1f - touchNorm))

              // Vertical guideline
              drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(touchX, 0f),
                end = Offset(touchX, h),
                strokeWidth = 1.5.dp.toPx()
              )

              // Touch point dot
              drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(touchX, touchY)
              )
              drawCircle(
                color = selectedMetric.color,
                radius = 4.dp.toPx(),
                center = Offset(touchX, touchY)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatPill(label: String, value: String) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = label,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}
