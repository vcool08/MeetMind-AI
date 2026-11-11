package com.example.data

import com.example.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AiSearchResult(
  val directAnswer: String,
  val matchedDecisions: List<String>,
  val referencedMeetings: List<Meeting>,
  val pendingTasksFound: List<TaskItem>
)

class MeetMindRepository private constructor() {

  private val _currentUserId = MutableStateFlow("tm_1") // Vikram Sharma (Manager)
  val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

  private val _teamMembers = MutableStateFlow<List<TeamMember>>(initialTeamMembers())
  val teamMembers: StateFlow<List<TeamMember>> = _teamMembers.asStateFlow()

  private val _meetings = MutableStateFlow<List<Meeting>>(initialMeetings())
  val meetings: StateFlow<List<Meeting>> = _meetings.asStateFlow()

  private val _tasks = MutableStateFlow<List<TaskItem>>(initialTasks())
  val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

  // User Organization info
  val organizationName = "Apex Innovations Tech Ltd."
  val organizationDomain = "apexinnovations.in"

  fun getCurrentUser(): TeamMember {
    return _teamMembers.value.firstOrNull { it.id == _currentUserId.value }
      ?: _teamMembers.value.first()
  }

  fun switchUser(userId: String) {
    _currentUserId.value = userId
  }

  private var firestoreService: FirestoreService? = null
  private val repoScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

  fun setFirestoreService(service: FirestoreService) {
    this.firestoreService = service
  }

  fun addTeamMember(member: TeamMember) {
    _teamMembers.update { it + member }
  }

  fun addMeeting(meeting: Meeting, newTasks: List<TaskItem> = emptyList()) {
    _meetings.update { listOf(meeting) + it }
    if (newTasks.isNotEmpty()) {
      _tasks.update { newTasks + it }
    }
    firestoreService?.let { fs ->
      repoScope.launch {
        fs.saveMeeting(meeting)
        newTasks.forEach { fs.saveTask(it) }
      }
    }
  }

  fun updateMeeting(updated: Meeting) {
    _meetings.update { list ->
      list.map { if (it.id == updated.id) updated else it }
    }
  }

  fun updateTranscriptSpeaker(meetingId: String, entryId: String, newSpeakerId: String?, newSpeakerName: String) {
    _meetings.update { meetingsList ->
      meetingsList.map { meeting ->
        if (meeting.id == meetingId) {
          val updatedTranscript = meeting.transcript.map { entry ->
            if (entry.id == entryId) {
              entry.copy(
                speakerId = newSpeakerId,
                speakerName = newSpeakerName,
                isUnknownSpeaker = false
              )
            } else entry
          }
          meeting.copy(transcript = updatedTranscript)
        } else meeting
      }
    }
  }

  fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
    _tasks.update { list ->
      list.map { if (it.id == taskId) it.copy(status = newStatus) else it }
    }
  }

  fun approveTask(taskId: String, assignedPersonId: String?, assignedPersonName: String?, dueDate: String, priority: TaskPriority) {
    _tasks.update { list ->
      list.map {
        if (it.id == taskId) {
          it.copy(
            assignedPersonId = assignedPersonId,
            assignedPersonName = assignedPersonName,
            dueDate = dueDate,
            priority = priority,
            needsManagerApproval = false,
            isApprovedByManager = true,
            status = TaskStatus.IN_PROGRESS
          )
        } else it
      }
    }
  }

  fun rejectTask(taskId: String) {
    _tasks.update { list ->
      list.map {
        if (it.id == taskId) {
          it.copy(
            status = TaskStatus.CANCELLED,
            needsManagerApproval = false,
            isApprovedByManager = false
          )
        } else it
      }
    }
  }

  fun addNewTask(task: TaskItem) {
    _tasks.update { listOf(task) + it }
  }

  fun searchAiMemory(query: String): AiSearchResult {
    val cleanQuery = query.trim().lowercase()
    val allMeetings = _meetings.value
    val allTasks = _tasks.value

    if (cleanQuery.isEmpty()) {
      return AiSearchResult(
        directAnswer = "Ask questions like 'What was decided about the payment gateway?', 'What are Rohan's tasks?', or 'Show unresolved blockers from Q4 sprint'.",
        matchedDecisions = emptyList(),
        referencedMeetings = emptyList(),
        pendingTasksFound = emptyList()
      )
    }

    val matchedMeetings = allMeetings.filter { meeting ->
      meeting.title.lowercase().contains(cleanQuery) ||
        meeting.transcript.any { it.text.lowercase().contains(cleanQuery) } ||
        (meeting.analysis?.executiveSummary?.lowercase()?.contains(cleanQuery) == true) ||
        (meeting.analysis?.topics?.any { it.topicTitle.lowercase().contains(cleanQuery) || it.keyPoints.any { p -> p.lowercase().contains(cleanQuery) } } == true)
    }

    val matchedDecisions = allMeetings.flatMap { meeting ->
      meeting.analysis?.importantDecisions?.filter { it.lowercase().contains(cleanQuery) } ?: emptyList()
    }

    val matchedTasks = allTasks.filter { task ->
      task.title.lowercase().contains(cleanQuery) ||
        task.description.lowercase().contains(cleanQuery) ||
        task.assignedPersonName?.lowercase()?.contains(cleanQuery) == true
    }

    val answer = when {
      cleanQuery.contains("payment") || cleanQuery.contains("razorpay") || cleanQuery.contains("security") ->
        "In the UPI Payment Gateway Review on Nov 8, the team finalized migrating fallback payments to Razorpay v3 with mandatory biometric authentication. Aarav is handling SDK benchmark testing, and Priya is updating the checkout UX states."
      cleanQuery.contains("rohan") || cleanQuery.contains("marketing") ->
        "Rohan Verma has 2 active assignments: coordinating regional app store marketing creatives and submitting the localization campaign press release before Nov 18."
      cleanQuery.contains("decision") || cleanQuery.contains("decide") ->
        "Across recent meetings, 6 key decisions were approved: 1) Deploy dual-layer tokenization for mobile transactions, 2) Roll out Marathi & Tamil voice transcription support first, 3) Deprecate legacy REST v1 by Dec 1st."
      cleanQuery.contains("blocker") || cleanQuery.contains("risk") ->
        "Critical blocker identified: Third-party SMS OTP delivery latency exceeding 8.4 seconds in tier-2 telecom circles. Mitigation: Fast-track WhatsApp fallback verification."
      else ->
        "Found ${matchedMeetings.size} related meetings and ${matchedTasks.size} associated tasks for '$query'. Meeting transcripts and AI extracted decisions have been indexed."
    }

    return AiSearchResult(
      directAnswer = answer,
      matchedDecisions = if (matchedDecisions.isNotEmpty()) matchedDecisions else listOf(
        "Dual-engine speech model fallback configured for Indian accent nuances",
        "Mandatory manager sign-off required for tasks lacking clear deadline"
      ),
      referencedMeetings = matchedMeetings.ifEmpty { allMeetings.take(2) },
      pendingTasksFound = matchedTasks
    )
  }

  companion object {
    @Volatile
    private var INSTANCE: MeetMindRepository? = null

    fun getInstance(): MeetMindRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: MeetMindRepository().also { INSTANCE = it }
      }
    }

    private fun initialTeamMembers(): List<TeamMember> = listOf(
      TeamMember("tm_1", "Vikram Sharma", "vikram.sharma@apexinnovations.in", "Product & Operations", UserRole.MANAGER, "VS", 0xFF0066FF),
      TeamMember("tm_2", "Priya Patel", "priya.patel@apexinnovations.in", "Product Design", UserRole.EMPLOYEE, "PP", 0xFF8B5CF6),
      TeamMember("tm_3", "Aarav Mehta", "aarav.mehta@apexinnovations.in", "Core Engineering", UserRole.EMPLOYEE, "AM", 0xFF10B981),
      TeamMember("tm_4", "Ananya Iyer", "ananya.iyer@apexinnovations.in", "QA & Reliability", UserRole.EMPLOYEE, "AI", 0xFFF59E0B),
      TeamMember("tm_5", "Rohan Verma", "rohan.verma@apexinnovations.in", "Growth & Marketing", UserRole.EMPLOYEE, "RV", 0xFF06B6D4),
      TeamMember("tm_6", "Rajesh Singhania", "rajesh.s@apexinnovations.in", "Executive Board", UserRole.ADMIN, "RS", 0xFFEF4444)
    )

    private fun initialTasks(): List<TaskItem> = listOf(
      TaskItem(
        id = "task_101",
        title = "Optimize Android Audio Capture Buffer",
        description = "Adjust AudioRecord sampling rate to 16kHz 16-bit PCM for noise-reduced streaming to speaker diarization model.",
        assignedPersonId = "tm_3",
        assignedPersonName = "Aarav Mehta",
        assignedById = "tm_1",
        assignedByName = "Vikram Sharma",
        startDate = "Nov 10, 2026",
        dueDate = "Nov 14, 2026",
        priority = TaskPriority.HIGH,
        meetingId = "meet_01",
        meetingTitle = "Q4 Mobile App Sprint & Architecture Sync",
        transcriptReferenceQuote = "\"Aarav, can you tune the 16kHz audio buffer pipeline before Thursday? We noticed slight clipping on low-end microphones.\"",
        status = TaskStatus.IN_PROGRESS,
        needsManagerApproval = false,
        isApprovedByManager = true
      ),
      TaskItem(
        id = "task_102",
        title = "Finalize Multilingual Recording Consent Modal",
        description = "Design localized consent notices for recording meetings in Hindi, Marathi, and Tamil with legal disclosure.",
        assignedPersonId = "tm_2",
        assignedPersonName = "Priya Patel",
        assignedById = "tm_1",
        assignedByName = "Vikram Sharma",
        startDate = "Nov 10, 2026",
        dueDate = "Nov 13, 2026",
        priority = TaskPriority.MEDIUM,
        meetingId = "meet_01",
        meetingTitle = "Q4 Mobile App Sprint & Architecture Sync",
        transcriptReferenceQuote = "\"Priya will ship the revised consent flow with regional language microcopy so participants clearly know audio is stored securely.\"",
        status = TaskStatus.COMPLETED,
        needsManagerApproval = false,
        isApprovedByManager = true
      ),
      TaskItem(
        id = "task_103",
        title = "Verify Offline Audio Fallback Storage",
        description = "Confirm that meeting audio cached locally during network drops uploads reliably once Wi-Fi reconnects.",
        assignedPersonId = "tm_4",
        assignedPersonName = "Ananya Iyer",
        assignedById = "tm_1",
        assignedByName = "Vikram Sharma",
        startDate = "Nov 09, 2026",
        dueDate = "Nov 15, 2026",
        priority = TaskPriority.HIGH,
        meetingId = "meet_01",
        meetingTitle = "Q4 Mobile App Sprint & Architecture Sync",
        transcriptReferenceQuote = "\"Ananya, please stress test the offline audio queue under simulated flaky 3G networks.\"",
        status = TaskStatus.PENDING,
        needsManagerApproval = false,
        isApprovedByManager = true
      ),
      TaskItem(
        id = "task_104",
        title = "Review Security Compliance for Cloud Voice Storage",
        description = "AI extracted task: Meeting mentioned evaluating cloud audio encryption standards. Assignee and deadline were ambiguous in transcript.",
        assignedPersonId = null,
        assignedPersonName = null,
        assignedById = "tm_1",
        assignedByName = "Vikram Sharma",
        startDate = "Nov 11, 2026",
        dueDate = "Nov 18, 2026",
        priority = TaskPriority.MEDIUM,
        meetingId = "meet_02",
        meetingTitle = "UPI Payment Gateway & Security Audit Review",
        transcriptReferenceQuote = "\"Someone from DevOps needs to double-check AES-256 cloud bucket retention rules whenever we archive Indian client sessions.\"",
        status = TaskStatus.PENDING,
        needsManagerApproval = true,
        isApprovedByManager = false
      ),
      TaskItem(
        id = "task_105",
        title = "Submit Regional App Store Localization Assets",
        description = "Translate promotional screenshots and feature bullet points into 6 Indian languages for Play Store submission.",
        assignedPersonId = "tm_5",
        assignedPersonName = "Rohan Verma",
        assignedById = "tm_1",
        assignedByName = "Vikram Sharma",
        startDate = "Nov 05, 2026",
        dueDate = "Nov 10, 2026", // Overdue!
        priority = TaskPriority.HIGH,
        meetingId = "meet_03",
        meetingTitle = "Regional Localization & Indian Language Rollout",
        transcriptReferenceQuote = "\"Rohan, let's target having all localized store creatives uploaded by the 10th.\"",
        status = TaskStatus.BLOCKED,
        needsManagerApproval = false,
        isApprovedByManager = true
      ),
      TaskItem(
        id = "task_106",
        title = "Investigate WhatsApp OTP Delivery Drop-off",
        description = "Work with gateway vendor to troubleshoot timeout errors for verification messages sent to rural regions.",
        assignedPersonId = "tm_3",
        assignedPersonName = "Aarav Mehta",
        assignedById = "tm_1",
        assignedByName = "Vikram Sharma",
        startDate = "Nov 08, 2026",
        dueDate = "Nov 12, 2026",
        priority = TaskPriority.MEDIUM,
        meetingId = "meet_02",
        meetingTitle = "UPI Payment Gateway & Security Audit Review",
        transcriptReferenceQuote = "\"Aarav, follow up with the SMS aggregator regarding the 8-second delay spike.\"",
        status = TaskStatus.IN_PROGRESS,
        needsManagerApproval = false,
        isApprovedByManager = true
      )
    )

    private fun initialMeetings(): List<Meeting> = listOf(
      Meeting(
        id = "meet_01",
        title = "Q4 Mobile App Sprint & Architecture Sync",
        dateFormatted = "Nov 10, 2026",
        timeFormatted = "10:30 AM IST",
        durationSeconds = 1845, // ~30 mins
        language = "Hinglish",
        participantIds = listOf("tm_1", "tm_2", "tm_3", "tm_4"),
        consentConfirmed = true,
        transcript = listOf(
          TranscriptEntry("tr_1", "tm_1", "Vikram Sharma", "Good morning everyone. Let's begin our Q4 sprint sync. Today our main agenda is audio streaming reliability and multilingual consent screens.", "00:04", false, 0.98f),
          TranscriptEntry("tr_2", "tm_3", "Aarav Mehta", "Haan Vikram, humne audio recording pipeline ko analyze kiya hai. The 16kHz buffer rate works well, but we should make sure background noise suppression is active.", "00:42", false, 0.94f),
          TranscriptEntry("tr_3", "tm_2", "Priya Patel", "From design perspective, I've designed the participant consent modal. It clearly informs users in Hindi and English before recording starts.", "01:25", false, 0.96f),
          TranscriptEntry("tr_4", null, "Unknown Speaker", "Is the cloud transcription processing GDPR and Indian Digital Personal Data Protection Act compliant?", "02:10", true, 0.72f),
          TranscriptEntry("tr_5", "tm_1", "Vikram Sharma", "Good point, that was Rajesh asking. Yes, enterprise audio is encrypted end-to-end with organization-level tenant isolation.", "02:35", false, 0.97f),
          TranscriptEntry("tr_6", "tm_4", "Ananya Iyer", "I am running stress tests on offline caching. If network drops mid-meeting, local PCM chunk queue keeps recording without losing audio.", "03:15", false, 0.95f),
          TranscriptEntry("tr_7", "tm_1", "Vikram Sharma", "Great! Aarav please tune the buffer before Thursday, and Priya will finalize the modal copy by tomorrow.", "04:02", false, 0.99f)
        ),
        analysis = MeetingAnalysis(
          executiveSummary = "Sprint sync focused on native Android audio capture performance, noise suppression, and compliance consent. The team agreed on 16kHz sampling, completed localized consent dialogs, and validated offline resilience.",
          topics = listOf(
            TopicAnalysis("Audio Capture Buffer Optimization", listOf("16kHz 16-bit PCM chosen for optimal bandwidth and voice clarity", "Noise suppression filter added for Indian office environments"), "00:42"),
            TopicAnalysis("DPDPA & GDPR Compliance Consent", listOf("Pre-recording consent modal required for all attendees", "Localized microcopy in English, Hindi, and Marathi"), "01:25"),
            TopicAnalysis("Offline Audio Queue Resilience", listOf("Local SQLite/file cache buffers audio chunks during flaky cellular coverage", "Automatic upload resumes once Wi-Fi connects"), "03:15")
          ),
          importantDecisions = listOf(
            "Standardize on 16kHz 16-bit PCM encoding for all meeting audio recordings",
            "Mandatory participant consent dialog must be accepted before microphone opens",
            "Organization tenant isolation enabled for all audio storage"
          ),
          individualResponsibilities = listOf(
            IndividualResponsibility("tm_3", "Aarav Mehta", "Core Engineering", listOf("Tune Android audio buffer", "Integrate noise cancellation pipeline")),
            IndividualResponsibility("tm_2", "Priya Patel", "Product Design", listOf("Deliver final multilingual consent UI components", "Polish task card animations")),
            IndividualResponsibility("tm_4", "Ananya Iyer", "QA Lead", listOf("Simulate network dropouts during 45-minute live sessions", "Verify speaker diarization accuracy"))
          ),
          unresolvedQuestions = listOf(
            "What is the maximum file size quota for continuous audio recordings exceeding 2 hours?",
            "Can participants join without consenting to voice storage while remaining on muted view?"
          ),
          risksAndBlockers = listOf(
            RiskAndBlocker("Low-end microphone distortion on older Android devices", "Medium", "Implement automatic gain control (AGC) via Android AudioRecord framework"),
            RiskAndBlocker("Offline cache storage overflowing on devices with low available disk space", "Low", "Add disk space pre-check with 200MB threshold before starting recording")
          ),
          followUpActions = listOf(
            "Share updated APK with QA team for real-device recording verification",
            "Schedule follow-up sync on Thursday at 3:00 PM IST"
          ),
          speakerParticipation = mapOf(
            "Vikram Sharma" to 0.42f,
            "Aarav Mehta" to 0.28f,
            "Priya Patel" to 0.18f,
            "Ananya Iyer" to 0.12f
          )
        )
      ),
      Meeting(
        id = "meet_02",
        title = "UPI Payment Gateway & Security Audit Review",
        dateFormatted = "Nov 08, 2026",
        timeFormatted = "02:00 PM IST",
        durationSeconds = 2410,
        language = "English",
        participantIds = listOf("tm_1", "tm_3", "tm_6"),
        consentConfirmed = true,
        transcript = listOf(
          TranscriptEntry("tr_201", "tm_6", "Rajesh Singhania", "Security and zero-trust data protection are non-negotiable for enterprise clients.", "00:10", false, 0.99f),
          TranscriptEntry("tr_202", "tm_1", "Vikram Sharma", "Agreed Rajesh. We have separated audio storage buckets with customer-managed encryption keys.", "00:45", false, 0.98f),
          TranscriptEntry("tr_203", "tm_3", "Aarav Mehta", "On UPI fallback, we observed 8-second delay with SMS OTP aggregator. We're adding WhatsApp OTP fallback.", "01:50", false, 0.96f)
        ),
        analysis = MeetingAnalysis(
          executiveSummary = "Executive audit review covering security architecture, zero-trust cloud data isolation, and UPI checkout reliability improvements.",
          topics = listOf(
            TopicAnalysis("Zero-Trust Architecture", listOf("Role-based permissions for Admin, Manager, and Employee", "Client session data purged upon user deletion request"), "00:10"),
            TopicAnalysis("Payment Gateway Redundancy", listOf("Razorpay v3 dual fallback routing", "WhatsApp OTP to bypass telecom carrier SMS congestion"), "01:50")
          ),
          importantDecisions = listOf(
            "Enforce strict role-based access control across all meeting transcripts and task boards",
            "Implement customer-managed encryption key support for enterprise tiers"
          ),
          individualResponsibilities = listOf(
            IndividualResponsibility("tm_3", "Aarav Mehta", "Core Engineering", listOf("Integrate WhatsApp verification webhook", "Run penetration tests on API endpoints"))
          ),
          unresolvedQuestions = listOf(
            "Should employees be allowed to delete their own meeting transcripts or require Manager sign-off?"
          ),
          risksAndBlockers = listOf(
            RiskAndBlocker("SMS gateway vendor SLA failure in tier-2 telecom circles", "High", "Prioritize biometrics and in-app authenticator codes")
          ),
          followUpActions = listOf(
            "Sign enterprise compliance addendum before next quarterly board meeting"
          ),
          speakerParticipation = mapOf(
            "Rajesh Singhania" to 0.45f,
            "Vikram Sharma" to 0.35f,
            "Aarav Mehta" to 0.20f
          )
        )
      ),
      Meeting(
        id = "meet_03",
        title = "Regional Localization & Indian Language Rollout",
        dateFormatted = "Nov 05, 2026",
        timeFormatted = "11:00 AM IST",
        durationSeconds = 1620,
        language = "Hindi",
        participantIds = listOf("tm_1", "tm_2", "tm_5"),
        consentConfirmed = true,
        transcript = listOf(
          TranscriptEntry("tr_301", "tm_1", "Vikram Sharma", "India mein multilingual adoption bahut tez badh raha hai. We must support 11 Indian languages seamlessly.", "00:05", false, 0.97f),
          TranscriptEntry("tr_302", "tm_5", "Rohan Verma", "Tier 2 and Tier 3 cities mein Hindi, Marathi, Gujarati, and Tamil requests sabse high hain.", "00:40", false, 0.96f),
          TranscriptEntry("tr_303", "tm_2", "Priya Patel", "We need custom font rendering that properly displays complex ligatures without layout breaks.", "01:15", false, 0.95f)
        ),
        analysis = MeetingAnalysis(
          executiveSummary = "Strategy meeting on expanding MeetMind AI's voice recognition and UI localization across 11 key Indian regional languages.",
          topics = listOf(
            TopicAnalysis("Language Rollout Priority", listOf("Tier 1: Hindi, English, Hinglish, Marathi, Tamil", "Tier 2: Gujarati, Telugu, Bengali, Kannada, Malayalam, Punjabi"), "00:40"),
            TopicAnalysis("Complex Script Typography", listOf("Ensure dynamic line heights and glyph bounds for Indic scripts"), "01:15")
          ),
          importantDecisions = listOf(
            "Launch with full support for 11 Indian languages plus colloquial Hinglish code-switching"
          ),
          individualResponsibilities = listOf(
            IndividualResponsibility("tm_5", "Rohan Verma", "Growth & Marketing", listOf("Prepare regional marketing campaigns", "Coordinate with local university testing focus groups"))
          ),
          unresolvedQuestions = listOf(
            "How do we handle code-switching when speakers switch between Tamil and English in the same sentence?"
          ),
          risksAndBlockers = listOf(
            RiskAndBlocker("High error rate on colloquial dialect accents", "Medium", "Train phonetic dictionary adaptation on Hinglish datasets")
          ),
          followUpActions = listOf(
            "Benchmark Whisper and Gemini multilingual transcription latency"
          ),
          speakerParticipation = mapOf(
            "Vikram Sharma" to 0.38f,
            "Rohan Verma" to 0.35f,
            "Priya Patel" to 0.27f
          )
        )
      )
    )
  }
}
