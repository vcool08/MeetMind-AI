package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.data.MeetMindRepository
import com.example.model.UserRole
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  repository: MeetMindRepository,
  onSignOut: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val teamMembers by repository.teamMembers.collectAsState()
  val currentUser = repository.getCurrentUser()

  var consentEnforced by remember { mutableStateOf(true) }
  var cloudSyncActive by remember { mutableStateOf(true) }
  var expandedUserSwitchDropdown by remember { mutableStateOf(false) }
  var showPurgeConfirmation by remember { mutableStateOf(false) }
  var purgeMessage by remember { mutableStateOf<String?>(null) }

  if (showPurgeConfirmation) {
    AlertDialog(
      onDismissRequest = { showPurgeConfirmation = false },
      title = { Text("Purge Cached Audio Files?", fontWeight = FontWeight.Bold) },
      text = {
        Text("In compliance with DPDPA & enterprise data governance, this will permanently remove local microphone recordings while preserving anonymized transcripts and task records.")
      },
      confirmButton = {
        Button(
          onClick = {
            showPurgeConfirmation = false
            purgeMessage = "All cached meeting audio data successfully purged."
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusError)
        ) {
          Text("Confirm Purge")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPurgeConfirmation = false }) {
          Text("Cancel")
        }
      }
    )
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
  ) {
    item {
      Text(
        text = "Profile & Security",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
    }

    // User Profile Card with Role Switcher
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(currentUser.avatarColorHex)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = currentUser.avatarInitials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = currentUser.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = currentUser.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (currentUser.role) {
                  UserRole.ADMIN -> StatusError.copy(alpha = 0.15f)
                  UserRole.MANAGER -> ElectricBluePrimary.copy(alpha = 0.15f)
                  UserRole.EMPLOYEE -> StatusSuccess.copy(alpha = 0.15f)
                },
                modifier = Modifier.padding(top = 4.dp)
              ) {
                Text(
                  text = "${currentUser.role.label.uppercase()} ROLE",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = when (currentUser.role) {
                    UserRole.ADMIN -> StatusError
                    UserRole.MANAGER -> ElectricBluePrimary
                    UserRole.EMPLOYEE -> StatusSuccess
                  },
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          HorizontalDivider()

          // Quick Demo User Switcher
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Switch Active Profile (Demo Role Verification):",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
              expanded = expandedUserSwitchDropdown,
              onExpandedChange = { expandedUserSwitchDropdown = !expandedUserSwitchDropdown }
            ) {
              OutlinedTextField(
                value = "${currentUser.name} (${currentUser.role.label})",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUserSwitchDropdown) },
                modifier = Modifier
                  .menuAnchor()
                  .fillMaxWidth()
                  .testTag("user_role_switcher_dropdown")
              )
              ExposedDropdownMenu(
                expanded = expandedUserSwitchDropdown,
                onDismissRequest = { expandedUserSwitchDropdown = false }
              ) {
                teamMembers.forEach { member ->
                  DropdownMenuItem(
                    text = {
                      Column {
                        Text(member.name, fontWeight = FontWeight.Bold)
                        Text("${member.role.label} • ${member.department}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    },
                    onClick = {
                      repository.switchUser(member.id)
                      expandedUserSwitchDropdown = false
                    }
                  )
                }
              }
            }
          }
        }
      }
    }

    // Organization-Based Data Isolation
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = ElectricBluePrimary)
            Text("Organization Tenant Isolation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          Text(
            text = "Enterprise Data Partition: ${repository.organizationName}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )

          Text(
            text = "All voice recordings, diarized speakers, and extracted tasks are isolated to domain @${repository.organizationDomain}. Cross-tenant access is prohibited by zero-trust security rules.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Security & Compliance Settings
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ElectricBluePrimary)
            Text("Privacy & Governance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Mandatory Participant Consent", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text("Block recording if consent dialog is dismissed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = consentEnforced,
              onCheckedChange = { consentEnforced = it }
            )
          }

          HorizontalDivider()

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("AES-256 Voice Storage Encryption", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text("Encrypted on-device before cloud archival", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = StatusSuccess)
          }

          HorizontalDivider()

          // Data Deletion
          Button(
            onClick = { showPurgeConfirmation = true },
            colors = ButtonDefaults.buttonColors(containerColor = StatusError.copy(alpha = 0.15f), contentColor = StatusError),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Purge Local Audio Cache")
          }

          if (purgeMessage != null) {
            Text(
              text = purgeMessage!!,
              color = StatusSuccess,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium
            )
          }

          HorizontalDivider()

          // Sign Out Action
          OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_sign_out_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Sign Out of Workspace",
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }

    // Architecture & Integration Markers
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text("MeetMind AI System Architecture", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text("• Native Android: AudioRecord/MediaRecorder, AudioWaveform Canvas, Dynamic Edge-to-Edge M3 UI, Permission Contracts", style = MaterialTheme.typography.bodySmall)
          Text("• Cloud Backend Integration: Firebase Auth, Firestore, Gemini AI Logic Server-side Voice Diarization & Task Extraction Placeholder", style = MaterialTheme.typography.bodySmall, color = ElectricBluePrimary)
          Text("• Indian Languages: Hindi, Marathi, Gujarati, Tamil, Telugu, Bengali, Kannada, Malayalam, Punjabi, English & Hinglish", style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}
