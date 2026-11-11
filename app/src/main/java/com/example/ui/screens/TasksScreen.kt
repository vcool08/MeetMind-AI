package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeetMindRepository
import com.example.model.*
import com.example.ui.components.ApproveTaskDialog
import com.example.ui.components.TaskCard
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
  repository: MeetMindRepository,
  onNavigateToMeeting: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val tasks by repository.tasks.collectAsState()
  val teamMembers by repository.teamMembers.collectAsState()
  val currentUser = repository.getCurrentUser()
  val isManager = currentUser.role == UserRole.MANAGER || currentUser.role == UserRole.ADMIN

  var selectedMemberFilter by remember { mutableStateOf<String?>("ALL") } // "ALL", "MY", or memberId
  var selectedStatusFilter by remember { mutableStateOf<String>("ALL") } // "ALL", "NEEDS_APPROVAL", or TaskStatus name
  var searchQuery by remember { mutableStateOf("") }

  var reviewingTask by remember { mutableStateOf<TaskItem?>(null) }
  var showCreateTaskDialog by remember { mutableStateOf(false) }

  // Filter tasks
  val filteredTasks = tasks.filter { task ->
    // Member filter
    val matchesMember = when (selectedMemberFilter) {
      "ALL" -> true
      "MY" -> task.assignedPersonId == currentUser.id
      else -> task.assignedPersonId == selectedMemberFilter
    }

    // Status filter
    val matchesStatus = when (selectedStatusFilter) {
      "ALL" -> true
      "NEEDS_APPROVAL" -> task.needsManagerApproval
      else -> task.status.name == selectedStatusFilter
    }

    // Search query
    val matchesSearch = searchQuery.isBlank() ||
      task.title.contains(searchQuery, ignoreCase = true) ||
      task.description.contains(searchQuery, ignoreCase = true) ||
      task.assignedPersonName?.contains(searchQuery, ignoreCase = true) == true

    matchesMember && matchesStatus && matchesSearch
  }

  // Manager Task Review Dialog
  if (reviewingTask != null) {
    ApproveTaskDialog(
      task = reviewingTask!!,
      teamMembers = teamMembers,
      onDismiss = { reviewingTask = null },
      onApprove = { assignedPersonId, assignedPersonName, dueDate, priority ->
        repository.approveTask(reviewingTask!!.id, assignedPersonId, assignedPersonName, dueDate, priority)
        reviewingTask = null
      },
      onReject = {
        repository.rejectTask(reviewingTask!!.id)
        reviewingTask = null
      }
    )
  }

  // Manual Task Creation Dialog
  if (showCreateTaskDialog) {
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newAssigneeId by remember { mutableStateOf(teamMembers.firstOrNull()?.id) }
    var newDueDate by remember { mutableStateOf("Nov 20, 2026") }
    var newPriority by remember { mutableStateOf(TaskPriority.HIGH) }

    AlertDialog(
      onDismissRequest = { showCreateTaskDialog = false },
      title = { Text("Assign New Action Item", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("Task Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("new_task_title_input")
          )
          OutlinedTextField(
            value = newDesc,
            onValueChange = { newDesc = it },
            label = { Text("Task Description") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newDueDate,
            onValueChange = { newDueDate = it },
            label = { Text("Due Date") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newTitle.isNotBlank()) {
              val assignee = teamMembers.firstOrNull { it.id == newAssigneeId }
              val created = TaskItem(
                id = "task_${UUID.randomUUID().toString().take(6)}",
                title = newTitle.trim(),
                description = newDesc.trim().ifBlank { "Manually assigned task." },
                assignedPersonId = assignee?.id,
                assignedPersonName = assignee?.name,
                assignedById = currentUser.id,
                assignedByName = currentUser.name,
                startDate = "Nov 11, 2026",
                dueDate = newDueDate.trim(),
                priority = newPriority,
                meetingId = "manual_sync",
                meetingTitle = "Direct Management Assignment",
                transcriptReferenceQuote = "Assigned directly by manager: ${currentUser.name}",
                status = TaskStatus.IN_PROGRESS,
                needsManagerApproval = false,
                isApprovedByManager = true
              )
              repository.addNewTask(created)
              showCreateTaskDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary)
        ) {
          Text("Assign Task")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateTaskDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  Scaffold(
    floatingActionButton = {
      if (isManager) {
        FloatingActionButton(
          onClick = { showCreateTaskDialog = true },
          containerColor = ElectricBluePrimary,
          contentColor = Color.White,
          modifier = Modifier.testTag("create_task_fab")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
      // Header: Title & Stats
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Smart Task Management",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Verified action items extracted from team meetings & assigned with deadlines.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Search Box
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search tasks by keyword or assignee...") },
          leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("task_search_input")
        )
      }

      // Individual Employee Filter Row (Individual Task Distribution)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Individual Distribution:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
          )

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = selectedMemberFilter == "ALL",
                onClick = { selectedMemberFilter = "ALL" },
                label = { Text("All Team (${tasks.size})") }
              )
            }

            item {
              val myCount = tasks.count { it.assignedPersonId == currentUser.id }
              FilterChip(
                selected = selectedMemberFilter == "MY",
                onClick = { selectedMemberFilter = "MY" },
                label = { Text("My Tasks ($myCount)") },
                leadingIcon = {
                  Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                }
              )
            }

            items(teamMembers) { member ->
              val memberTasksCount = tasks.count { it.assignedPersonId == member.id }
              FilterChip(
                selected = selectedMemberFilter == member.id,
                onClick = { selectedMemberFilter = member.id },
                label = { Text("${member.name.substringBefore(" ")} ($memberTasksCount)") },
                leadingIcon = {
                  Box(
                    modifier = Modifier
                      .size(16.dp)
                      .clip(CircleShape)
                      .background(Color(member.avatarColorHex)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(member.avatarInitials.take(1), color = Color.White, fontSize = 8.sp)
                  }
                }
              )
            }
          }
        }
      }

      // Status Filter Row
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = selectedStatusFilter == "ALL",
              onClick = { selectedStatusFilter = "ALL" },
              label = { Text("All Status") }
            )
          }

          val needsApprovalTotal = tasks.count { it.needsManagerApproval }
          if (needsApprovalTotal > 0) {
            item {
              FilterChip(
                selected = selectedStatusFilter == "NEEDS_APPROVAL",
                onClick = { selectedStatusFilter = "NEEDS_APPROVAL" },
                label = { Text("Needs Approval ($needsApprovalTotal)") },
                leadingIcon = {
                  Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(14.dp))
                }
              )
            }
          }

          TaskStatus.values().forEach { status ->
            val count = tasks.count { it.status == status }
            item {
              FilterChip(
                selected = selectedStatusFilter == status.name,
                onClick = { selectedStatusFilter = status.name },
                label = { Text("${status.label} ($count)") }
              )
            }
          }
        }
      }

      // Tasks List
      if (filteredTasks.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(imageVector = Icons.Default.TaskAlt, contentDescription = null, tint = ElectricBluePrimary, modifier = Modifier.size(40.dp))
              Text("No tasks found matching criteria", fontWeight = FontWeight.SemiBold)
              Text("Try selecting a different team member or status filter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      } else {
        items(filteredTasks, key = { it.id }) { task ->
          TaskCard(
            task = task,
            onStatusChange = { newStatus ->
              repository.updateTaskStatus(task.id, newStatus)
            },
            onApproveClick = {
              reviewingTask = task
            },
            isManager = isManager
          )
        }
      }
    }
  }
}
