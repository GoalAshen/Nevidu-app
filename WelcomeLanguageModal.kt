package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
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
import com.example.localization.EcoLanguage
import com.example.localization.EcoTranslations
import com.example.ui.theme.EcoEmerald

@Composable
fun WelcomeLanguageModal(
  currentLanguage: EcoLanguage,
  onSelectLanguage: (EcoLanguage) -> Unit,
  onDismiss: () -> Unit
) {
  var tempSelected by remember { mutableStateOf(currentLanguage) }
  val strings = remember(tempSelected) { EcoTranslations.get(tempSelected) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(EcoEmerald.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Language, contentDescription = null, tint = EcoEmerald, modifier = Modifier.size(20.dp))
        }
        Column {
          Text(
            text = strings.welcomeTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = strings.chooseLanguage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = strings.welcomeSubtitle,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(EcoLanguage.entries) { lang ->
            val isSelected = tempSelected == lang
            Surface(
              onClick = { tempSelected = lang },
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) EcoEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                  if (isSelected) EcoEmerald else Color.Transparent
                )
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("lang_option_${lang.code}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Text(text = lang.flagEmoji, fontSize = 22.sp)
                  Column {
                    Text(
                      text = lang.nativeName,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) EcoEmerald else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = lang.englishName,
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .size(22.dp)
                      .clip(CircleShape)
                      .background(EcoEmerald),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSelectLanguage(tempSelected)
          onDismiss()
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EcoEmerald, contentColor = Color.Black),
        modifier = Modifier.testTag("save_language_button")
      ) {
        Text(text = strings.getStarted, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(text = "Close")
      }
    }
  )
}
