package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioRecordManager
import com.example.audio.RecordingState
import com.example.data.MeetMindRepository
import com.example.model.*
import com.example.ui.components.ConsentDialog
import com.example.ui.components.SpeakerCorrectionDialog
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordMeetingScreen(
  repository: MeetMindRepository,
  audioRecordManager: AudioRecordManager,
  onMeetingSaved: (meetingId: String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val teamMembers by repository.teamMembers.collectAsState()

  var meetingTitle by remember { mutableStateOf("Sprint Alignment & Deliverables") }
  var selectedLanguage by remember { mutableStateOf(SupportedIndianLanguages.first()) }
  var expandedLangDropdown by remember { mutableStateOf(false) }
  var selectedParticipantIds by remember { mutableStateOf(teamMembers.take(4).map { it.id }.toSet()) }

  // Consent & Permission States
  var showConsentDialog by remember { mutableStateOf(false) }
  var hasConsented by remember { mutableStateOf(false) }
  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasAudioPermission = isGranted
    if (isGranted && hasConsented) {
      audioRecordManager.startRecording()
    }
  }

  // File upload simulation / file picker
  var showUploadSimulationDialog by remember { mutableStateOf(false) }

  // Live Audio States from manager
  val recordingState by audioRecordManager.state.collectAsState()
  val durationSeconds by audioRecordManager.durationSeconds.collectAsState()
  val amplitudes by audioRecordManager.amplitudes.collectAsState()

  // Live Transcripts
  var liveTranscript by remember {
    mutableStateOf(
      mutableListOf<TranscriptEntry>()
    )
  }

  // Dialog for editing speaker
  var editingTranscriptEntry by remember { mutableStateOf<TranscriptEntry?>(null) }

  // Processing state when saving & analyzing
  var isAnalyzing by remember { mutableStateOf(false) }

  // Simulated live voice transcription stream during active recording
  LaunchedEffect(recordingState) {
    if (recordingState == RecordingState.RECORDING) {
      while (recordingState == RecordingState.RECORDING) {
        delay(4500)
        val second = durationSeconds
        val formattedTime = String.format("%02d:%02d", second / 60, second % 60)
        val simulatedEntries = listOf(
          TranscriptEntry(
            id = "live_${System.currentTimeMillis()}",
            speakerId = "tm_1",
            speakerName = "Vikram Sharma",
            text = "Let's review the timeline for our upcoming release. What are the pending milestones?",
            timestampFormatted = formattedTime,
            isUnknownSpeaker = false,
            confidenceScore = 0.98f
          ),
          TranscriptEntry(
            id = "live_${System.currentTimeMillis() + 1}",
            speakerId = "tm_3",
            speakerName = "Aarav Mehta",
            text = "The background audio sync service is ready on Android. We need to deploy the migration script by Friday.",
            timestampFormatted = formattedTime,
            isUnknownSpeaker = false,
            confidenceScore = 0.94f
          ),
          TranscriptEntry(
            id = "live_${System.currentTimeMillis() + 2}",
            speakerId = null,
            speakerName = "Unknown Speaker",
            text = "Is there any risk of token expiration during extended multi-hour meetings?",
            timestampFormatted = formattedTime,
            isUnknownSpeaker = true,
            confidenceScore = 0.68f
          ),
          TranscriptEntry(
            id = "live_${System.currentTimeMillis() + 3}",
            speakerId = "tm_2",
            speakerName = "Priya Patel",
            text = "I have updated the design tokens in Figma for the Indian language scripts.",
            timestampFormatted = formattedTime,
            isUnknownSpeaker = false,
            confidenceScore = 0.96f
          )
        )
        val nextEntry = simulatedEntries[(liveTranscript.size) % simulatedEntries.size]
        liveTranscript = (liveTranscript + nextEntry).toMutableList()
      }
    }
  }

  val formattedTimeDisplay = remember(durationSeconds) {
    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    String.format("%02d:%02d", mins, secs)
  }

  // Pre-recording consent dialog
  if (showConsentDialog) {
    ConsentDialog(
      onDismiss = { showConsentDialog = false },
      onAccept = {
        showConsentDialog = false
        hasConsented = true
        if (hasAudioPermission) {
          audioRecordManager.startRecording()
        } else {
          permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
      }
    )
  }

  // Speaker Correction Dialog
  if (editingTranscriptEntry != null) {
    val entry = editingTranscriptEntry!!
    SpeakerCorrectionDialog(
      currentSpeakerName = entry.speakerName,
      currentSpeakerId = entry.speakerId,
      teamMembers = teamMembers,
      onDismiss = { editingTranscriptEntry = null },
      onSpeakerSelected = { speakerId, speakerName ->
        liveTranscript = liveTranscript.map {
          if (it.id == entry.id) {
            it.copy(
              speakerId = speakerId,
              speakerName = speakerName,
              isUnknownSpeaker = false
            )
          } else it
        }.toMutableList()
        editingTranscriptEntry = null
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (recordingState == RecordingState.RECORDING) "Recording Meeting..." else "AI Meeting Recording",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        },
        actions = {
          // Native Microphone vs Integration Status indicator
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (hasAudioPermission) StatusSuccess.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f),
            modifier = Modifier.padding(end = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(if (hasAudioPermission) StatusSuccess else StatusWarning)
              )
              Text(
                text = if (hasAudioPermission) "Mic Ready" else "Permission Req",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = if (hasAudioPermission) StatusSuccess else StatusWarning,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
      ) {
        // Meeting Configuration Card (Editable before/during recording)
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
              OutlinedTextField(
                value = meetingTitle,
                onValueChange = { meetingTitle = it },
                label = { Text("Meeting Title") },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("meeting_title_input")
              )

              // Language Selector (11 Indian Languages)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Primary Language:",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )

                ExposedDropdownMenuBox(
                  expanded = expandedLangDropdown,
                  onExpandedChange = { expandedLangDropdown = !expandedLangDropdown }
                ) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                      .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                      .clickable { expandedLangDropdown = true }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = ElectricBluePrimary,
                        modifier = Modifier.size(16.dp)
                      )
                      Text(
                        text = "${selectedLanguage.name} (${selectedLanguage.nativeName})",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricBluePrimary,
                        fontWeight = FontWeight.Bold
                      )
                      Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = ElectricBluePrimary
                      )
                    }
                  }

                  ExposedDropdownMenu(
                    expanded = expandedLangDropdown,
                    onDismissRequest = { expandedLangDropdown = false }
                  ) {
                    SupportedIndianLanguages.forEach { lang ->
                      DropdownMenuItem(
                        text = {
                          Column {
                            Text(lang.name, fontWeight = FontWeight.SemiBold)
                            Text(lang.nativeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                          }
                        },
                        onClick = {
                          selectedLanguage = lang
                          expandedLangDropdown = false
                        }
                      )
                    }
                  }
                }
              }

              // Attendees Selector
              Text(
                text = "Participants in Room / Call (${selectedParticipantIds.size}):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
              )

              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                items(teamMembers) { member ->
                  val isSelected = selectedParticipantIds.contains(member.id)
                  FilterChip(
                    selected = isSelected,
                    onClick = {
                      selectedParticipantIds = if (isSelected) {
                        selectedParticipantIds - member.id
                      } else {
                        selectedParticipantIds + member.id
                      }
                    },
                    label = { Text(member.name.substringBefore(" ")) },
                    leadingIcon = {
                      Box(
                        modifier = Modifier
                          .size(18.dp)
                          .clip(CircleShape)
                          .background(Color(member.avatarColorHex)),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = member.avatarInitials.take(1),
                          color = Color.White,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold
                        )
                      }
                    }
                  )
                }
              }
            }
          }
        }

        // Live Audio Waveform & Timer Card
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (recordingState == RecordingState.RECORDING) Color(0xFF070F1E) else MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              // Status Pill
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = when (recordingState) {
                  RecordingState.RECORDING -> StatusError.copy(alpha = 0.2f)
                  RecordingState.PAUSED -> StatusWarning.copy(alpha = 0.2f)
                  RecordingState.STOPPED -> StatusSuccess.copy(alpha = 0.2f)
                  RecordingState.IDLE -> MaterialTheme.colorScheme.surfaceVariant
                }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(
                        when (recordingState) {
                          RecordingState.RECORDING -> StatusError
                          RecordingState.PAUSED -> StatusWarning
                          RecordingState.STOPPED -> StatusSuccess
                          RecordingState.IDLE -> Color.Gray
                        }
                      )
                  )
                  Text(
                    text = when (recordingState) {
                      RecordingState.RECORDING -> "LIVE RECORDING & DIARIZATION"
                      RecordingState.PAUSED -> "RECORDING PAUSED"
                      RecordingState.STOPPED -> "SESSION COMPLETE"
                      RecordingState.IDLE -> "READY TO CAPTURE"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (recordingState) {
                      RecordingState.RECORDING -> StatusError
                      RecordingState.PAUSED -> StatusWarning
                      RecordingState.STOPPED -> StatusSuccess
                      RecordingState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                  )
                }
              }

              // Large Duration Timer
              Text(
                text = formattedTimeDisplay,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (recordingState == RecordingState.RECORDING) Color.White else MaterialTheme.colorScheme.onSurface
              )

              // Dynamic Waveform Visualizer
              WaveformVisualizer(
                amplitudes = amplitudes,
                isRecording = recordingState == RecordingState.RECORDING,
                modifier = Modifier.fillMaxWidth()
              )

              // Audio File Upload option
              if (recordingState == RecordingState.IDLE) {
                OutlinedButton(
                  onClick = {
                    showUploadSimulationDialog = true
                  },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.testTag("upload_audio_file_button")
                ) {
                  Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Or Import Audio File (.mp3, .m4a, .wav)")
                }
              }
            }
          }
        }

        // Live Diarized Transcription Section
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Live Diarized Transcript",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )

            Text(
              text = "Tap speaker to reassign",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (liveTranscript.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.RecordVoiceOver,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(36.dp)
                )
                Text(
                  text = "Start recording to see live speech conversion and speaker identification.",
                  style = MaterialTheme.typography.bodySmall,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else {
          items(liveTranscript) { entry ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  editingTranscriptEntry = entry
                }
                .testTag("transcript_entry_${entry.id}")
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (entry.isUnknownSpeaker) StatusWarning.copy(alpha = 0.15f) else ElectricBluePrimary.copy(alpha = 0.15f)
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Icon(
                          imageVector = if (entry.isUnknownSpeaker) Icons.AutoMirrored.Filled.HelpOutline else Icons.Default.Person,
                          contentDescription = null,
                          tint = if (entry.isUnknownSpeaker) StatusWarning else ElectricBluePrimary,
                          modifier = Modifier.size(12.dp)
                        )
                        Text(
                          text = entry.speakerName,
                          color = if (entry.isUnknownSpeaker) StatusWarning else ElectricBluePrimary,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold
                        )
                      }
                    }

                    if (entry.isUnknownSpeaker) {
                      Text(
                        text = "(Tap to correct)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = StatusWarning
                      )
                    }
                  }

                  Text(
                    text = "${entry.timestampFormatted} • ${(entry.confidenceScore * 100).toInt()}% conf",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
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
      }

      // Bottom Control Floating Action Bar
      Surface(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          when (recordingState) {
            RecordingState.IDLE -> {
              Button(
                onClick = {
                  if (!hasConsented) {
                    showConsentDialog = true
                  } else if (hasAudioPermission) {
                    audioRecordManager.startRecording()
                  } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("start_record_button")
              ) {
                Icon(imageVector = Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Recording Meeting", fontSize = 16.sp, fontWeight = FontWeight.Bold)
              }
            }

            RecordingState.RECORDING -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedButton(
                  onClick = { audioRecordManager.pauseRecording() },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("pause_record_button")
                ) {
                  Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Pause")
                }

                Button(
                  onClick = {
                    audioRecordManager.stopRecording()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp)
                    .testTag("stop_record_button")
                ) {
                  Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Stop Recording", fontWeight = FontWeight.Bold)
                }
              }
            }

            RecordingState.PAUSED -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Button(
                  onClick = { audioRecordManager.resumeRecording() },
                  colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("resume_record_button")
                ) {
                  Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Resume")
                }

                Button(
                  onClick = {
                    audioRecordManager.stopRecording()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                ) {
                  Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Finish")
                }
              }
            }

            RecordingState.STOPPED -> {
              Button(
                onClick = {
                  isAnalyzing = true
                  coroutineScope.launch {
                    delay(1200) // Realistic processing animation
                    val newMeetingId = "meet_${System.currentTimeMillis()}"
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val now = Date()

                    val transcriptToSave = if (liveTranscript.isNotEmpty()) liveTranscript else listOf(
                      TranscriptEntry("tr_saved_1", "tm_1", "Vikram Sharma", "Today we reviewed sprint targets and finalized the audio pipeline.", "00:05", false, 0.98f),
                      TranscriptEntry("tr_saved_2", "tm_3", "Aarav Mehta", "I will complete the noise suppression integration by Friday.", "00:25", false, 0.95f),
                      TranscriptEntry("tr_saved_3", "tm_2", "Priya Patel", "Figma design system updates will be submitted tomorrow.", "00:50", false, 0.96f)
                    )

                    val generatedAnalysis = MeetingAnalysis(
                      executiveSummary = "Meeting successfully transcribed in ${selectedLanguage.name}. The team aligned on sprint deliverables, resolved audio synchronization questions, and assigned milestone deadlines.",
                      topics = listOf(
                        TopicAnalysis("Milestone Timelines", listOf("Confirmed release deadline for upcoming mobile sprint", "Audio noise suppression benchmarked at 16kHz"), "00:05"),
                        TopicAnalysis("Design System Integration", listOf("Multilingual typography tokens updated in Figma"), "00:50")
                      ),
                      importantDecisions = listOf(
                        "Deploy audio synchronization service with local caching",
                        "Adopt localized typography tokens for ${selectedLanguage.name}"
                      ),
                      individualResponsibilities = listOf(
                        IndividualResponsibility("tm_3", "Aarav Mehta", "Core Engineering", listOf("Complete noise suppression integration by Friday")),
                        IndividualResponsibility("tm_2", "Priya Patel", "Product Design", listOf("Submit localized design tokens in Figma"))
                      ),
                      unresolvedQuestions = listOf(
                        "Verify battery consumption during continuous 2+ hour recording sessions"
                      ),
                      risksAndBlockers = listOf(
                        RiskAndBlocker("Potential mic clipping in loud environments", "Medium", "Enable automatic gain control (AGC)")
                      ),
                      followUpActions = listOf(
                        "Verify compiled APK in QA sandbox environment"
                      ),
                      speakerParticipation = mapOf(
                        "Vikram Sharma" to 0.50f,
                        "Aarav Mehta" to 0.30f,
                        "Priya Patel" to 0.20f
                      )
                    )

                    val extractedTasks = listOf(
                      TaskItem(
                        id = "task_${System.currentTimeMillis()}_1",
                        title = "Complete Noise Suppression Integration",
                        description = "Integrate noise reduction pipeline for ${selectedLanguage.name} voice streams.",
                        assignedPersonId = "tm_3",
                        assignedPersonName = "Aarav Mehta",
                        assignedById = "tm_1",
                        assignedByName = "Vikram Sharma",
                        startDate = dateFormat.format(now),
                        dueDate = "Nov 17, 2026",
                        priority = TaskPriority.HIGH,
                        meetingId = newMeetingId,
                        meetingTitle = meetingTitle,
                        transcriptReferenceQuote = "\"I will complete the noise suppression integration by Friday.\"",
                        status = TaskStatus.IN_PROGRESS,
                        needsManagerApproval = false,
                        isApprovedByManager = true
                      ),
                      TaskItem(
                        id = "task_${System.currentTimeMillis()}_2",
                        title = "Submit Localized Figma Tokens",
                        description = "Publish typography and component updates for Indian languages.",
                        assignedPersonId = "tm_2",
                        assignedPersonName = "Priya Patel",
                        assignedById = "tm_1",
                        assignedByName = "Vikram Sharma",
                        startDate = dateFormat.format(now),
                        dueDate = "Nov 16, 2026",
                        priority = TaskPriority.MEDIUM,
                        meetingId = newMeetingId,
                        meetingTitle = meetingTitle,
                        transcriptReferenceQuote = "\"Figma design system updates will be submitted tomorrow.\"",
                        status = TaskStatus.PENDING,
                        needsManagerApproval = false,
                        isApprovedByManager = true
                      )
                    )

                    val newMeeting = Meeting(
                      id = newMeetingId,
                      title = meetingTitle.ifBlank { "Team Meeting" },
                      dateFormatted = dateFormat.format(now),
                      timeFormatted = timeFormat.format(now),
                      durationSeconds = durationSeconds.coerceAtLeast(35),
                      language = selectedLanguage.name,
                      participantIds = selectedParticipantIds.toList(),
                      consentConfirmed = true,
                      transcript = transcriptToSave,
                      analysis = generatedAnalysis,
                      isAnalyzed = true
                    )

                    repository.addMeeting(newMeeting, extractedTasks)
                    isAnalyzing = false
                    onMeetingSaved(newMeetingId)
                  }
                },
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("generate_ai_analysis_button")
              ) {
                if (isAnalyzing) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Analyzing Meeting & Extracting Tasks...")
                } else {
                  Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Generate AI Analysis & Tasks", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }

  // File Upload Dialog
  if (showUploadSimulationDialog) {
    AlertDialog(
      onDismissRequest = { showUploadSimulationDialog = false },
      title = { Text("Upload Meeting Audio") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Select recorded audio from Android device storage:")
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("📁 Supported Formats: MP3, M4A, WAV, AAC", style = MaterialTheme.typography.bodySmall)
              Text("⚡ Cloud Speech Engine: Gemini Multimodal & Whisper Diarization (Server-side Placeholder)", style = MaterialTheme.typography.bodySmall, color = ElectricBluePrimary)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showUploadSimulationDialog = false
            liveTranscript = mutableListOf(
              TranscriptEntry("up_1", "tm_1", "Vikram Sharma", "Imported audio stream initialized. Let's inspect the quarterly pipeline.", "00:03", false, 0.97f),
              TranscriptEntry("up_2", "tm_4", "Ananya Iyer", "QA reports show zero regressions on the login flow.", "00:30", false, 0.95f)
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary)
        ) {
          Text("Import Sample Audio")
        }
      },
      dismissButton = {
        TextButton(onClick = { showUploadSimulationDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
