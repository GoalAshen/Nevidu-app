package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlantHealthAnalysis
import com.example.data.model.PlantHealthTier
import com.example.ui.theme.*

@Composable
fun PlantHealthCard(
  plantHealth: PlantHealthAnalysis,
  isRelayOn: Boolean,
  onToggleRelay: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val tierColor = when (plantHealth.tier) {
    PlantHealthTier.THRIVING -> EcoEmerald
    PlantHealthTier.GOOD -> EcoCyan
    PlantHealthTier.MODERATE_STRESS -> EcoAmber
    PlantHealthTier.CRITICAL -> EcoRed
  }

  val animatedScore by animateFloatAsState(
    targetValue = plantHealth.overallScore / 100f,
    animationSpec = tween(durationMillis = 800),
    label = "healthScore"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("plant_health_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header: Title & Tier Badge
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
              .background(EcoEmerald.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Spa,
              contentDescription = "Plant Health",
              tint = EcoEmerald,
              modifier = Modifier.size(22.dp)
            )
          }
          Column {
            Text(
              text = "Plant Health & Vitality",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Telemetry-driven agronomic health analysis",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Tier Badge
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = tierColor.copy(alpha = 0.15f),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(tierColor.copy(alpha = 0.6f))
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(text = plantHealth.tier.emoji, fontSize = 12.sp)
            Text(
              text = plantHealth.tier.label.uppercase(),
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              color = tierColor
            )
          }
        }
      }

      // Main Health Score Showcase
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Circular Score Meter
          Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(
              progress = { animatedScore },
              modifier = Modifier.fillMaxSize(),
              color = tierColor,
              trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
              strokeWidth = 7.dp,
              strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${plantHealth.overallScore}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tierColor
              )
              Text(
                text = "INDEX",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Headline & Summary Description
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = plantHealth.statusHeadline,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = tierColor
            )
            Text(
              text = plantHealth.summaryText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp
            )
          }
        }
      }

      // Three Environmental Factor Meters
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HealthFactorBar(
          label = "Soil Hydration",
          status = plantHealth.hydrationStatus,
          score = plantHealth.hydrationScore,
          icon = Icons.Default.WaterDrop,
          barColor = EcoCyan
        )

        HealthFactorBar(
          label = "Thermal Microclimate",
          status = plantHealth.thermalStatus,
          score = plantHealth.thermalScore,
          icon = Icons.Default.Thermostat,
          barColor = EcoAmber
        )

        HealthFactorBar(
          label = "Photosynthetic Light",
          status = plantHealth.sunlightStatus,
          score = plantHealth.sunlightScore,
          icon = Icons.Default.WbSunny,
          barColor = EcoEmerald
        )
      }

      // Actionable Recommendations
      if (plantHealth.recommendations.isNotEmpty()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = EcoAmber,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Care Guidance",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = EcoAmber
              )
            }

            plantHealth.recommendations.forEach { tip ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
              ) {
                Text(text = "•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                  text = tip,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 15.sp
                )
              }
            }
          }
        }
      }

      // Direct Relay Action Button if plants are thirsty
      if (plantHealth.requiresRelayWatering) {
        Button(
          onClick = { onToggleRelay(!isRelayOn) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isRelayOn) EcoCyan else EcoEmerald
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("plant_health_irrigate_button")
        ) {
          Icon(
            imageVector = if (isRelayOn) Icons.Default.PowerSettingsNew else Icons.Default.Water,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isRelayOn) "Stop Relay (Watering Active)" else "Irrigate Now (Turn Relay ON)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }
    }
  }
}

@Composable
private fun HealthFactorBar(
  label: String,
  status: String,
  score: Int,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  barColor: Color
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = barColor, modifier = Modifier.size(15.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      }
      Text(
        text = status,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = barColor
      )
    }

    LinearProgressIndicator(
      progress = { (score / 100f).coerceIn(0f, 1f) },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp)),
      color = barColor,
      trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
  }
}
