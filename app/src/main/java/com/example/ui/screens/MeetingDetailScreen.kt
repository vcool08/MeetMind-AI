package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeetMindRepository
import com.example.model.*
import com.example.ui.components.ApproveTaskDialog
import com.example.ui.components.SpeakerCorrectionDialog
import com.example.ui.components.TaskCard
import com.example.ui.theme.*

enum class MeetingTab(val title: String) {
  SUMMARY("Summary"),
  TOPICS("Topics"),
  TRANSCRIPT("Transcript"),
  DECISIONS("Decisions"),
  TASKS("Tasks"),
  RISKS("Risks & Questions")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
  meetingId: String,
  repository: MeetMindRepository,
  onBack: () -> Unit,
  onNavigateToTasks: () -> Unit,
  modifier: Modifier = Modifier
) {
  val meetings by repository.meetings.collectAsState()
  val allTasks by repository.tasks.collectAsState()
  val teamMembers by repository.teamMembers.collectAsState()
  val currentUser = repository.getCurrentUser()

  val meeting = meetings.firstOrNull { it.id == meetingId } ?: meetings.firstOrNull()

  var selectedTab by remember { mutableStateOf(MeetingTab.SUMMARY) }
  var editingTranscriptEntry by remember { mutableStateOf<TranscriptEntry?>(null) }
  var reviewingTask by remember { mutableStateOf<TaskItem?>(null) }

  if (meeting == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Meeting not found")
    }
    return
  }

  val meetingTasks = allTasks.filter { it.meetingId == meeting.id }
  val analysis = meeting.analysis

  // Speaker Correction Dialog
  if (editingTranscriptEntry != null) {
    val entry = editingTranscriptEntry!!
    SpeakerCorrectionDialog(
      currentSpeakerName = entry.speakerName,
      currentSpeakerId = entry.speakerId,
      teamMembers = teamMembers,
      onDismiss = { editingTranscriptEntry = null },
      onSpeakerSelected = { newSpeakerId, newSpeakerName ->
        repository.updateTranscriptSpeaker(meeting.id, entry.id, newSpeakerId, newSpeakerName)
        editingTranscriptEntry = null
      }
    )
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

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = meeting.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
            Text(
              text = "${meeting.dateFormatted} • ${meeting.formattedDuration} • ${meeting.language}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = ElectricBluePrimary.copy(alpha = 0.12f),
            modifier = Modifier.padding(end = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricBluePrimary, modifier = Modifier.size(14.dp))
              Text("AI Diarized", color = ElectricBluePrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Scrollable Tabs
      ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = ElectricBluePrimary
      ) {
        MeetingTab.values().forEach { tab ->
          Tab(
            selected = selectedTab == tab,
            onClick = { selectedTab = tab },
            text = {
              Text(
                text = when (tab) {
                  MeetingTab.TASKS -> "${tab.title} (${meetingTasks.size})"
                  MeetingTab.TRANSCRIPT -> "${tab.title} (${meeting.transcript.size})"
                  else -> tab.title
                },
                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
        }
      }

      // Tab Content
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        when (selectedTab) {
          MeetingTab.SUMMARY -> {
            // Executive Summary Card
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(imageVector = Icons.Default.Summarize, contentDescription = null, tint = ElectricBluePrimary)
                    Text("Executive Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  }
                  Text(
                    text = analysis?.executiveSummary ?: "Meeting recorded and indexed.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                  )
                }
              }
            }

            // Speaker Participation Distribution
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(imageVector = Icons.Default.PieChart, contentDescription = null, tint = ElectricBluePrimary)
                    Text("Speaker Talk Time Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  }

                  analysis?.speakerParticipation?.forEach { (speaker, percentage) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Text(speaker, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = ElectricBluePrimary)
                      }
                      LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(8.dp)
                          .clip(RoundedCornerShape(4.dp)),
                        color = ElectricBluePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                      )
                    }
                  }
                }
              }
            }

            // Quick Responsibilities Breakdown
            if (analysis?.individualResponsibilities?.isNotEmpty() == true) {
              item {
                Text("Individual Responsibilities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }

              items(analysis.individualResponsibilities) { resp ->
                Card(
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                      text = "${resp.memberName} (${resp.role})",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = ElectricBluePrimary
                    )
                    resp.duties.forEach { duty ->
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                      ) {
                        Text("•", color = ElectricBluePrimary)
                        Text(duty, style = MaterialTheme.typography.bodySmall)
                      }
                    }
                  }
                }
              }
            }
          }

          MeetingTab.TOPICS -> {
            if (analysis?.topics.isNullOrEmpty()) {
              item { Text("No topics analyzed.") }
            } else {
              items(analysis!!.topics) { topic ->
                Card(
                  shape = RoundedCornerShape(12.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(topic.topicTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                      Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(topic.timestamp, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                      }
                    }
                    topic.keyPoints.forEach { point ->
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = ElectricBluePrimary, fontWeight = FontWeight.Bold)
                        Text(point, style = MaterialTheme.typography.bodyMedium)
                      }
                    }
                  }
                }
              }
            }
          }

          MeetingTab.TRANSCRIPT -> {
            item {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ElectricBluePrimary, modifier = Modifier.size(18.dp))
                  Text(
                    text = "Tap on any speaker name below to manually reassign or correct identity.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp
                  )
                }
              }
            }

            items(meeting.transcript) { entry ->
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { editingTranscriptEntry = entry }
                  .testTag("transcript_item_${entry.id}")
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (entry.isUnknownSpeaker) StatusWarning.copy(alpha = 0.15f) else ElectricBluePrimary.copy(alpha = 0.15f)
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Icon(
                          imageVector = if (entry.isUnknownSpeaker) Icons.AutoMirrored.Filled.HelpOutline else Icons.Default.Person,
                          contentDescription = null,
                          tint = if (entry.isUnknownSpeaker) StatusWarning else ElectricBluePrimary,
                          modifier = Modifier.size(14.dp)
                        )
                        Text(
                          text = entry.speakerName,
                          color = if (entry.isUnknownSpeaker) StatusWarning else ElectricBluePrimary,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold
                        )
                        Icon(
                          imageVector = Icons.Default.Edit,
                          contentDescription = "Edit Speaker",
                          tint = if (entry.isUnknownSpeaker) StatusWarning else ElectricBluePrimary,
                          modifier = Modifier.size(12.dp)
                        )
                      }
                    }

                    Text(
                      text = "${entry.timestampFormatted} • ${(entry.confidenceScore * 100).toInt()}% conf",
                      style = MaterialTheme.typography.bodySmall,
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }

          MeetingTab.DECISIONS -> {
            item {
              Text("Important Decisions Reached", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (analysis?.importantDecisions.isNullOrEmpty()) {
              item { Text("No formal decisions recorded.") }
            } else {
              items(analysis!!.importantDecisions) { decision ->
                Card(
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                    Text(decision, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                  }
                }
              }
            }

            item {
              Spacer(modifier = Modifier.height(8.dp))
              Text("Follow-up Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(analysis?.followUpActions ?: emptyList()) { action ->
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ElectricBluePrimary)
                  Text(action, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }
          }

          MeetingTab.TASKS -> {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Tasks Extracted from Conversation",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Go to Task Board",
                  style = MaterialTheme.typography.labelMedium,
                  color = ElectricBluePrimary,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.clickable { onNavigateToTasks() }
                )
              }
            }

            if (meetingTasks.isEmpty()) {
              item {
                Text(
                  text = "No action items extracted from this meeting.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            } else {
              items(meetingTasks) { task ->
                TaskCard(
                  task = task,
                  onStatusChange = { newStatus ->
                    repository.updateTaskStatus(task.id, newStatus)
                  },
                  onApproveClick = {
                    reviewingTask = task
                  },
                  isManager = currentUser.role == UserRole.MANAGER || currentUser.role == UserRole.ADMIN
                )
              }
            }
          }

          MeetingTab.RISKS -> {
            item {
              Text("Risks and Blockers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(analysis?.risksAndBlockers ?: emptyList()) { risk ->
              val severityColor = when (risk.severity.lowercase()) {
                "high" -> StatusError
                "medium" -> StatusWarning
                else -> StatusSuccess
              }
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(risk.issue, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(4.dp), color = severityColor.copy(alpha = 0.15f)) {
                      Text(
                        text = "${risk.severity.uppercase()} RISK",
                        color = severityColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(
                    text = "Mitigation: ${risk.suggestedAction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }

            item {
              Spacer(modifier = Modifier.height(8.dp))
              Text("Unresolved Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(analysis?.unresolvedQuestions ?: emptyList()) { question ->
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = StatusWarning)
                  Text(question, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }
          }
        }
      }
    }
  }
}
