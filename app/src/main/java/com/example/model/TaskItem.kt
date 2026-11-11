package com.example.model

enum class TaskPriority(val label: String) {
  HIGH("High"),
  MEDIUM("Medium"),
  LOW("Low")
}

enum class TaskStatus(val label: String) {
  PENDING("Pending"),
  IN_PROGRESS("In Progress"),
  BLOCKED("Blocked"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled")
}

data class TaskItem(
  val id: String,
  val title: String,
  val description: String,
  val assignedPersonId: String?,
  val assignedPersonName: String?,
  val assignedById: String,
  val assignedByName: String,
  val startDate: String,
  val dueDate: String,
  val priority: TaskPriority,
  val meetingId: String,
  val meetingTitle: String,
  val transcriptReferenceQuote: String,
  val status: TaskStatus = TaskStatus.PENDING,
  val needsManagerApproval: Boolean = false, // True if assignee or deadline had uncertainty
  val isApprovedByManager: Boolean = true
)
