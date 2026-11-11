package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.ui.theme.*

@Composable
fun TaskCard(
  task: TaskItem,
  onStatusChange: (TaskStatus) -> Unit,
  onApproveClick: (() -> Unit)? = null,
  isManager: Boolean = true,
  modifier: Modifier = Modifier
) {
  var showStatusMenu by remember { mutableStateOf(false) }

  val priorityColor = when (task.priority) {
    TaskPriority.HIGH -> StatusError
    TaskPriority.MEDIUM -> StatusWarning
    TaskPriority.LOW -> StatusSuccess
  }

  val statusColor = when (task.status) {
    TaskStatus.COMPLETED -> StatusSuccess
    TaskStatus.IN_PROGRESS -> ElectricBluePrimary
    TaskStatus.BLOCKED -> StatusError
    TaskStatus.PENDING -> StatusPending
    TaskStatus.CANCELLED -> Color.Gray
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("task_card_${task.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header: Priority, Approval badge, and Status Dropdown
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Priority Badge
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = priorityColor.copy(alpha = 0.15f)
          ) {
            Text(
              text = "${task.priority.label.uppercase()} PRIORITY",
              color = priorityColor,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          if (task.needsManagerApproval) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = StatusWarning.copy(alpha = 0.15f)
            ) {
              Text(
                text = "NEEDS APPROVAL",
                color = StatusWarning,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        // Status chip with menu
        Box {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = statusColor.copy(alpha = 0.12f),
            modifier = Modifier
              .clickable { showStatusMenu = true }
              .testTag("status_selector_${task.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(statusColor)
              )
              Text(
                text = task.status.label,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
              )
              Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Change Status",
                tint = statusColor,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          DropdownMenu(
            expanded = showStatusMenu,
            onDismissRequest = { showStatusMenu = false }
          ) {
            TaskStatus.values().forEach { statusOption ->
              DropdownMenuItem(
                text = { Text(statusOption.label) },
                onClick = {
                  onStatusChange(statusOption)
                  showStatusMenu = false
                },
                leadingIcon = {
                  val dotColor = when (statusOption) {
                    TaskStatus.COMPLETED -> StatusSuccess
                    TaskStatus.IN_PROGRESS -> ElectricBluePrimary
                    TaskStatus.BLOCKED -> StatusError
                    TaskStatus.PENDING -> StatusPending
                    TaskStatus.CANCELLED -> Color.Gray
                  }
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(dotColor)
                  )
                }
              )
            }
          }
        }
      }

      // Title & Description
      Text(
        text = task.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = task.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )

      // Transcript reference quote
      if (task.transcriptReferenceQuote.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.FormatQuote,
              contentDescription = "Transcript Quote",
              tint = ElectricBluePrimary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = task.transcriptReferenceQuote,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              fontStyle = FontStyle.Italic,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

      // Footer: Assignee & Due Date
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Assignee
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (task.assignedPersonName != null) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(ElectricBluePrimary),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = task.assignedPersonName.take(1),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = task.assignedPersonName,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium
            )
          } else {
            Icon(
              imageVector = Icons.Default.PersonOff,
              contentDescription = "Unassigned",
              tint = StatusWarning,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "Unassigned",
              style = MaterialTheme.typography.bodySmall,
              color = StatusWarning,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // Due date & Meeting tag
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Event,
            contentDescription = "Due Date",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "Due ${task.dueDate}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Manager Approval Action Bar (if required)
      if (task.needsManagerApproval && isManager && onApproveClick != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = StatusWarning.copy(alpha = 0.1f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Assignee/date requires review",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 12.sp,
              color = StatusWarning,
              fontWeight = FontWeight.Medium
            )
            Button(
              onClick = onApproveClick,
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("approve_task_button_${task.id}")
            ) {
              Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Review & Approve", fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}
