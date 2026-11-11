package com.example.model

data class TopicAnalysis(
  val topicTitle: String,
  val keyPoints: List<String>,
  val timestamp: String
)

data class RiskAndBlocker(
  val issue: String,
  val severity: String, // "High", "Medium", "Low"
  val suggestedAction: String
)

data class IndividualResponsibility(
  val memberId: String?,
  val memberName: String,
  val role: String,
  val duties: List<String>
)

data class MeetingAnalysis(
  val executiveSummary: String,
  val topics: List<TopicAnalysis>,
  val importantDecisions: List<String>,
  val individualResponsibilities: List<IndividualResponsibility>,
  val unresolvedQuestions: List<String>,
  val risksAndBlockers: List<RiskAndBlocker>,
  val followUpActions: List<String>,
  val speakerParticipation: Map<String, Float> // Name -> percentage e.g. 0.45f
)
