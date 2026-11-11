package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.TeamMember
import com.example.model.UserRole
import com.example.ui.theme.ElectricBluePrimary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMemberDialog(
  onDismiss: () -> Unit,
  onInvite: (TeamMember) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var department by remember { mutableStateOf("Core Engineering") }
  var selectedRole by remember { mutableStateOf(UserRole.EMPLOYEE) }
  var expandedRoleDropdown by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.GroupAdd,
        contentDescription = null,
        tint = ElectricBluePrimary,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Add Team Member",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Full Name") },
          placeholder = { Text("e.g. Tanvi Deshmukh") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("member_name_input")
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Work Email") },
          placeholder = { Text("e.g. tanvi.d@apexinnovations.in") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("member_email_input")
        )

        OutlinedTextField(
          value = department,
          onValueChange = { department = it },
          label = { Text("Department") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text(
          text = "Organization Role:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          UserRole.values().forEach { role ->
            FilterChip(
              selected = role == selectedRole,
              onClick = { selectedRole = role },
              label = { Text(role.label) }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && email.isNotBlank()) {
            val newMember = TeamMember(
              id = "tm_${UUID.randomUUID().toString().take(6)}",
              name = name.trim(),
              email = email.trim(),
              department = department.trim(),
              role = selectedRole,
              avatarColorHex = 0xFF0066FF
            )
            onInvite(newMember)
          }
        },
        enabled = name.isNotBlank() && email.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("submit_invite_button")
      ) {
        Text("Add to Team", color = Color.White)
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
