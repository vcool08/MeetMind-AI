package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricBluePrimary

@Composable
fun ConsentDialog(
  onDismiss: () -> Unit,
  onAccept: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.PrivacyTip,
        contentDescription = "Consent Notice",
        tint = ElectricBluePrimary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Participant Recording Consent",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "MeetMind AI records and processes meeting conversations to extract action items, generate executive summaries, and diarize speakers.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "Compliance",
              tint = ElectricBluePrimary,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "By proceeding, you verify that all meeting participants have consented to audio recording in compliance with Indian DPDPA & enterprise governance guidelines.",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 12.sp,
              lineHeight = 16.sp
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onAccept,
        colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("consent_accept_button")
      ) {
        Text("Consent & Start", color = Color.White, fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("consent_cancel_button")
      ) {
        Text("Cancel")
      }
    },
    shape = RoundedCornerShape(16.dp)
  )
}
