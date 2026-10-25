package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SoilZoneState
import com.example.ui.theme.*

@Composable
fun SoilMoistureGaugesCard(
  zone1: Int,
  zone2: Int,
  zone3: Int,
  dryThreshold: Int,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("soil_moisture_gauges_card"),
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
              imageVector = Icons.Default.WaterDrop,
              contentDescription = "Soil Moisture",
              tint = EcoWaterBlue,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Soil Moisture Gauges",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "3-Zone Moisture Levels",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Legend Badge
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          LegendDot(color = EcoRed, label = "Dry (<$dryThreshold%)")
          LegendDot(color = EcoEmerald, label = "Good")
          LegendDot(color = EcoWaterBlue, label = "Wet (>70%)")
        }
      }

      // Three Gauge Columns
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        CircularSoilGauge(
          zoneName = "Zone 1",
          subTitle = "Topsoil / Bed A",
          percentage = zone1,
          dryThreshold = dryThreshold,
          modifier = Modifier.weight(1f)
        )
        CircularSoilGauge(
          zoneName = "Zone 2",
          subTitle = "Root Zone / Bed B",
          percentage = zone2,
          dryThreshold = dryThreshold,
          modifier = Modifier.weight(1f)
        )
        CircularSoilGauge(
          zoneName = "Zone 3",
          subTitle = "Hydro / Bed C",
          percentage = zone3,
          dryThreshold = dryThreshold,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Box(
      modifier = Modifier
        .size(7.dp)
        .clip(CircleShape)
        .background(color)
    )
    Text(
      text = label,
      fontSize = 9.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun CircularSoilGauge(
  zoneName: String,
  subTitle: String,
  percentage: Int,
  dryThreshold: Int,
  modifier: Modifier = Modifier
) {
  val zoneState = when {
    percentage < dryThreshold -> SoilZoneState.DRY
    percentage > 70 -> SoilZoneState.WET
    else -> SoilZoneState.GOOD
  }

  val targetColor = when (zoneState) {
    SoilZoneState.DRY -> EcoRed
    SoilZoneState.GOOD -> EcoEmerald
    SoilZoneState.WET -> EcoWaterBlue
  }

  val animatedColor by animateColorAsState(
    targetValue = targetColor,
    animationSpec = tween(500),
    label = "gaugeColor"
  )

  val animatedPercent by animateFloatAsState(
    targetValue = percentage.toFloat().coerceIn(0f, 100f),
    animationSpec = tween(800),
    label = "gaugePercent"
  )

  Column(
    modifier = modifier.padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Box(
      modifier = Modifier.size(92.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 8.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(diameter, diameter)

        // Background Track Arc (240 degrees sweep, starting from 150 deg)
        drawArc(
          color = targetColor.copy(alpha = 0.15f),
          startAngle = 150f,
          sweepAngle = 240f,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Active Value Arc
        val currentSweep = (animatedPercent / 100f) * 240f
        drawArc(
          brush = Brush.sweepGradient(
            0.0f to animatedColor.copy(alpha = 0.7f),
            1.0f to animatedColor,
            center = center
          ),
          startAngle = 150f,
          sweepAngle = currentSweep,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
      }

      // Center Percentage Text
      Column(
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "$percentage%",
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold,
          color = animatedColor
        )
        Text(
          text = when (zoneState) {
            SoilZoneState.DRY -> "DRY"
            SoilZoneState.GOOD -> "OPTIMAL"
            SoilZoneState.WET -> "WET"
          },
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = animatedColor
        )
      }
    }

    Text(
      text = zoneName,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold
    )
    Text(
      text = subTitle,
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
