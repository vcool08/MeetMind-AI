package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.TeamMember
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.StatusError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveTaskDialog(
  task: TaskItem,
  teamMembers: List<TeamMember>,
  onDismiss: () -> Unit,
  onApprove: (assignedPersonId: String?, assignedPersonName: String?, dueDate: String, priority: TaskPriority) -> Unit,
  onReject: () -> Unit
) {
  var selectedMemberId by remember { mutableStateOf(task.assignedPersonId ?: teamMembers.firstOrNull()?.id) }
  var dueDateText by remember { mutableStateOf(task.dueDate.ifBlank { "Nov 20, 2026" }) }
  var selectedPriority by remember { mutableStateOf(task.priority) }
  var expandedMemberDropdown by remember { mutableStateOf(false) }

  val selectedMember = teamMembers.firstOrNull { it.id == selectedMemberId }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Verified,
        contentDescription = "Approve Task",
        tint = ElectricBluePrimary,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Manager Task Review",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = task.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )

        Text(
          text = "Meeting quote: \"${task.transcriptReferenceQuote}\"",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // Assignee selection
        Text(
          text = "Assign Responsibility To:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )

        ExposedDropdownMenuBox(
          expanded = expandedMemberDropdown,
          onExpandedChange = { expandedMemberDropdown = !expandedMemberDropdown }
        ) {
          OutlinedTextField(
            value = selectedMember?.let { "${it.name} (${it.role.label})" } ?: "Select Team Member",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMemberDropdown) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("assignee_dropdown")
          )
          ExposedDropdownMenu(
            expanded = expandedMemberDropdown,
            onDismissRequest = { expandedMemberDropdown = false }
          ) {
            teamMembers.forEach { member ->
              DropdownMenuItem(
                text = { Text("${member.name} • ${member.department}") },
                onClick = {
                  selectedMemberId = member.id
                  expandedMemberDropdown = false
                }
              )
            }
          }
        }

        // Due date input
        OutlinedTextField(
          value = dueDateText,
          onValueChange = { dueDateText = it },
          label = { Text("Confirmed Due Date") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Priority selection
        Text(
          text = "Priority Level:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TaskPriority.values().forEach { priority ->
            val isSelected = priority == selectedPriority
            FilterChip(
              selected = isSelected,
              onClick = { selectedPriority = priority },
              label = { Text(priority.label) }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onApprove(
            selectedMember?.id,
            selectedMember?.name,
            dueDateText,
            selectedPriority
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("confirm_task_approval_button")
      ) {
        Text("Approve & Dispatch", color = Color.White)
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
          onClick = onReject,
          colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
        ) {
          Text("Reject Task")
        }
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    },
    shape = RoundedCornerShape(16.dp)
  )
}
