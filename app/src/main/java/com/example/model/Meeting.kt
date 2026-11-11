package com.example.model

data class Meeting(
  val id: String,
  val title: String,
  val dateFormatted: String,
  val timeFormatted: String,
  val durationSeconds: Int,
  val language: String,
  val participantIds: List<String>,
  val audioFilePath: String? = null,
  val consentConfirmed: Boolean = true,
  val transcript: List<TranscriptEntry> = emptyList(),
  val analysis: MeetingAnalysis? = null,
  val isAnalyzed: Boolean = true
) {
  val formattedDuration: String
    get() {
      val minutes = durationSeconds / 60
      val seconds = durationSeconds % 60
      return if (minutes >= 60) {
        val hours = minutes / 60
        val remainingMins = minutes % 60
        "${hours}h ${remainingMins}m"
      } else {
        "${minutes}m ${seconds}s"
      }
    }
}
