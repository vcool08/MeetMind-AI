package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeetMindRepository
import com.example.model.Meeting
import com.example.model.TaskItem
import com.example.model.TaskStatus
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
  repository: MeetMindRepository,
  onNavigateToRecord: () -> Unit,
  onNavigateToMeeting: (String) -> Unit,
  onNavigateToTasks: () -> Unit,
  onNavigateToAiMemory: () -> Unit,
  modifier: Modifier = Modifier
) {
  val meetings by repository.meetings.collectAsState()
  val tasks by repository.tasks.collectAsState()
  val teamMembers by repository.teamMembers.collectAsState()
  val currentUser = repository.getCurrentUser()

  val totalMeetings = meetings.size
  val totalTasks = tasks.size
  val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }
  val pendingTasks = tasks.count { it.status == TaskStatus.PENDING || it.status == TaskStatus.IN_PROGRESS }
  val overdueTasks = tasks.count { it.dueDate.contains("Nov 10") || it.dueDate.contains("Nov 05") }
  val needsApprovalCount = tasks.count { it.needsManagerApproval }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
  ) {
    // Top Greeting & Organization Pill
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Welcome, ${currentUser.name.substringBefore(" ")} 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "${repository.organizationName} • ${currentUser.role.label}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(
          onClick = onNavigateToAiMemory,
          modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .testTag("ai_memory_search_icon_button")
        ) {
          Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = "AI Meeting Memory",
            tint = ElectricBluePrimary
          )
        }
      }
    }

    // Hero: Quick Start Meeting Banner with Audio Wave Preview
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quick_start_meeting_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color(0xFF0A192F),
                  Color(0xFF0D254C),
                  ElectricBluePrimary
                )
              )
            )
            .padding(20.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElectricCyanAccent.copy(alpha = 0.2f)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = ElectricCyanAccent,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "AI Voice Recognition Ready",
                    color = ElectricCyanAccent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              Text(
                text = "11 Indian Languages",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall
              )
            }

            Text(
              text = "Record & Analyze Team Meeting",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )

            Text(
              text = "Live speaker diarization, instant task extraction, and executive summaries with verified participant consent.",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.85f),
              lineHeight = 18.sp
            )

            Button(
              onClick = onNavigateToRecord,
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF0A192F)
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("start_meeting_button")
            ) {
              Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = ElectricBluePrimary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Start AI Meeting Recording",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0A192F)
              )
            }
          }
        }
      }
    }

    // Pending Manager Approval Alert (if any)
    if (needsApprovalCount > 0) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = StatusWarning.copy(alpha = 0.12f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToTasks() }
            .testTag("manager_approval_alert")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PendingActions,
              contentDescription = null,
              tint = StatusWarning,
              modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "$needsApprovalCount Task(s) Need Manager Review",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = StatusWarning
              )
              Text(
                text = "AI extracted tasks with uncertain assignee/deadlines awaiting your approval.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = "Review",
              tint = StatusWarning
            )
          }
        }
      }
    }

    // Stats Grid Header
    item {
      Text(
        text = "Key Metrics & Productivity",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }

    // Stats 2x3 Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            title = "Total Meetings",
            value = totalMeetings.toString(),
            subtitle = "Recorded & Diarized",
            icon = Icons.Default.Videocam,
            color = ElectricBluePrimary,
            modifier = Modifier.weight(1f)
          )
          StatCard(
            title = "Total Tasks",
            value = totalTasks.toString(),
            subtitle = "AI Extracted",
            icon = Icons.AutoMirrored.Filled.Assignment,
            color = Color(0xFF0A192F),
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            title = "Completed",
            value = completedTasks.toString(),
            subtitle = "Tasks Finished",
            icon = Icons.Default.CheckCircle,
            color = StatusSuccess,
            modifier = Modifier.weight(1f)
          )
          StatCard(
            title = "Pending",
            value = pendingTasks.toString(),
            subtitle = "In Progress",
            icon = Icons.Default.Schedule,
            color = StatusPending,
            modifier = Modifier.weight(1f)
          )
          StatCard(
            title = "Overdue",
            value = overdueTasks.toString(),
            subtitle = "Needs Action",
            icon = Icons.Default.Warning,
            color = StatusError,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // AI Memory Ask Widget Banner
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToAiMemory() }
          .testTag("dashboard_ask_ai_memory_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(ElectricBluePrimary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Ask AI Memory About Past Meetings",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Search decisions, pending blockers & discuss history",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = ElectricBluePrimary
          )
        }
      }
    }

    // Recent Meetings Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Recent Meetings",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "View All",
          style = MaterialTheme.typography.labelMedium,
          color = ElectricBluePrimary,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.clickable { onNavigateToMeeting("") }
        )
      }
    }

    items(meetings.take(3)) { meeting ->
      MeetingItemCard(
        meeting = meeting,
        onClick = { onNavigateToMeeting(meeting.id) }
      )
    }

    // Upcoming Deadlines Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Upcoming Deadlines",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Manage Tasks",
          style = MaterialTheme.typography.labelMedium,
          color = ElectricBluePrimary,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.clickable { onNavigateToTasks() }
        )
      }
    }

    items(tasks.filter { it.status != TaskStatus.COMPLETED }.take(3)) { task ->
      UpcomingDeadlineItem(task = task, onClick = onNavigateToTasks)
    }
  }
}

@Composable
fun StatCard(
  title: String,
  value: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(16.dp)
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun MeetingItemCard(
  meeting: Meeting,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("meeting_item_${meeting.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = ElectricBluePrimary.copy(alpha = 0.12f)
        ) {
          Text(
            text = meeting.language,
            color = ElectricBluePrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "${meeting.dateFormatted} • ${meeting.formattedDuration}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Text(
        text = meeting.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      if (meeting.analysis != null) {
        Text(
          text = meeting.analysis.executiveSummary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${meeting.transcript.size} speech segments • ${meeting.analysis?.importantDecisions?.size ?: 0} decisions",
          style = MaterialTheme.typography.bodySmall,
          fontSize = 11.sp,
          color = ElectricBluePrimary,
          fontWeight = FontWeight.Medium
        )

        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
fun UpcomingDeadlineItem(
  task: TaskItem,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      val isOverdue = task.dueDate.contains("Nov 10") || task.dueDate.contains("Nov 05")
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(if (isOverdue) StatusError else ElectricBluePrimary)
      )

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = task.title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${task.assignedPersonName ?: "Unassigned"} • ${task.meetingTitle}",
          style = MaterialTheme.typography.bodySmall,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isOverdue) StatusError.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
      ) {
        Text(
          text = if (isOverdue) "Overdue (${task.dueDate})" else "Due ${task.dueDate}",
          color = if (isOverdue) StatusError else MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
      }
    }
  }
}
