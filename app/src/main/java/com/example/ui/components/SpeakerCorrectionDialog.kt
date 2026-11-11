package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.TeamMember
import com.example.ui.theme.ElectricBluePrimary

@Composable
fun SpeakerCorrectionDialog(
  currentSpeakerName: String,
  currentSpeakerId: String?,
  teamMembers: List<TeamMember>,
  onDismiss: () -> Unit,
  onSpeakerSelected: (speakerId: String?, speakerName: String) -> Unit
) {
  var customName by remember { mutableStateOf("") }
  var showCustomInput by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Reassign Speaker",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Currently assigned to: $currentSpeakerName",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        Text(
          text = "Select Team Member:",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(teamMembers) { member ->
            val isSelected = member.id == currentSpeakerId
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onSpeakerSelected(member.id, member.name)
                },
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
              Row(
                modifier = Modifier
                  .padding(horizontal = 12.dp, vertical = 8.dp)
                  .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(member.avatarColorHex)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = member.avatarInitials,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                  )
                  Text(
                    text = "${member.role.label} • ${member.department}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = ElectricBluePrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }

        if (!showCustomInput) {
          TextButton(
            onClick = { showCustomInput = true },
            modifier = Modifier.align(Alignment.Start)
          ) {
            Text("+ Enter Custom Speaker Name")
          }
        } else {
          OutlinedTextField(
            value = customName,
            onValueChange = { customName = it },
            label = { Text("Guest / External Speaker") },
            placeholder = { Text("e.g., Client Representative") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("custom_speaker_input")
          )
        }
      }
    },
    confirmButton = {
      if (showCustomInput && customName.isNotBlank()) {
        Button(
          onClick = {
            onSpeakerSelected(null, customName.trim())
          },
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary)
        ) {
          Text("Assign Custom Name")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
    shape = RoundedCornerShape(16.dp)
  )
}
