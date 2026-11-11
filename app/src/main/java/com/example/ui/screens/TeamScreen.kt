package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.model.TeamMember
import com.example.model.UserRole
import com.example.ui.components.InviteMemberDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
  repository: MeetMindRepository,
  onNavigateToTasksForMember: (memberId: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val teamMembers by repository.teamMembers.collectAsState()
  val tasks by repository.tasks.collectAsState()
  val currentUser = repository.getCurrentUser()
  val isManager = currentUser.role == UserRole.MANAGER || currentUser.role == UserRole.ADMIN

  var showInviteDialog by remember { mutableStateOf(false) }
  var selectedDepartmentFilter by remember { mutableStateOf("ALL") }

  val departments = listOf("ALL") + teamMembers.map { it.department }.distinct()

  val filteredMembers = teamMembers.filter { member ->
    selectedDepartmentFilter == "ALL" || member.department.equals(selectedDepartmentFilter, ignoreCase = true)
  }

  if (showInviteDialog) {
    InviteMemberDialog(
      onDismiss = { showInviteDialog = false },
      onInvite = { newMember ->
        repository.addTeamMember(newMember)
        showInviteDialog = false
      }
    )
  }

  Scaffold(
    floatingActionButton = {
      if (isManager) {
        FloatingActionButton(
          onClick = { showInviteDialog = true },
          containerColor = ElectricBluePrimary,
          contentColor = Color.White,
          modifier = Modifier.testTag("invite_team_member_fab")
        ) {
          Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Member")
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
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Team Management",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${repository.organizationName} • ${teamMembers.size} registered members",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Role Permissions Card
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = ElectricBluePrimary)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Role-Based Responsibilities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ElectricBluePrimary
              )
              Text(
                text = "• Managers review and assign AI extracted tasks\n• Employees complete assigned action items\n• Admins manage organization data isolation & retention",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                lineHeight = 16.sp
              )
            }
          }
        }
      }

      // Department Filter Chips
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(departments) { dept ->
            FilterChip(
              selected = selectedDepartmentFilter == dept,
              onClick = { selectedDepartmentFilter = dept },
              label = { Text(if (dept == "ALL") "All Departments" else dept) }
            )
          }
        }
      }

      items(filteredMembers, key = { it.id }) { member ->
        val memberTasks = tasks.filter { it.assignedPersonId == member.id }
        val pendingCount = memberTasks.count { it.status == com.example.model.TaskStatus.PENDING || it.status == com.example.model.TaskStatus.IN_PROGRESS }

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("team_member_${member.id}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(member.avatarColorHex)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = member.avatarInitials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = member.name,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )

                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = when (member.role) {
                    UserRole.ADMIN -> StatusError.copy(alpha = 0.15f)
                    UserRole.MANAGER -> ElectricBluePrimary.copy(alpha = 0.15f)
                    UserRole.EMPLOYEE -> StatusSuccess.copy(alpha = 0.15f)
                  }
                ) {
                  Text(
                    text = member.role.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (member.role) {
                      UserRole.ADMIN -> StatusError
                      UserRole.MANAGER -> ElectricBluePrimary
                      UserRole.EMPLOYEE -> StatusSuccess
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Text(
                text = member.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Text(
                text = "${member.department} • $pendingCount active tasks",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = ElectricBluePrimary,
                fontWeight = FontWeight.Medium
              )
            }

            IconButton(onClick = { onNavigateToTasksForMember(member.id) }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Assignment,
                contentDescription = "View Tasks",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
