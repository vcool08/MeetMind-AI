package com.example.model

data class TranscriptEntry(
  val id: String,
  val speakerId: String? = null,
  val speakerName: String,
  val text: String,
  val timestampFormatted: String,
  val isUnknownSpeaker: Boolean = false,
  val confidenceScore: Float = 0.92f
)
