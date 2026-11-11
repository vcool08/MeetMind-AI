package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AuthUser
import com.example.model.Meeting
import com.example.model.TaskItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreService(private val context: Context) {

  val firestore: FirebaseFirestore by lazy {
    val databaseId = context.getString(R.string.firestore_database_id)
    FirebaseFirestore.getInstance(databaseId)
  }

  suspend fun saveUser(user: AuthUser) = withContext(Dispatchers.IO) {
    try {
      firestore.collection("users").document(user.uid).set(user).await()
    } catch (e: Throwable) {
      // Fail-safe handling for network drops
    }
  }

  suspend fun saveMeeting(meeting: Meeting) = withContext(Dispatchers.IO) {
    try {
      val docData = hashMapOf(
        "id" to meeting.id,
        "title" to meeting.title,
        "language" to meeting.language,
        "durationSeconds" to meeting.durationSeconds,
        "dateFormatted" to meeting.dateFormatted,
        "timeFormatted" to meeting.timeFormatted,
        "participantIds" to meeting.participantIds,
        "executiveSummary" to (meeting.analysis?.executiveSummary ?: ""),
        "importantDecisions" to (meeting.analysis?.importantDecisions ?: emptyList<String>())
      )
      firestore.collection("meetings").document(meeting.id).set(docData).await()
    } catch (e: Throwable) {
      // Fail-safe handling
    }
  }

  suspend fun saveTask(task: TaskItem) = withContext(Dispatchers.IO) {
    try {
      val taskData = hashMapOf(
        "id" to task.id,
        "title" to task.title,
        "description" to task.description,
        "assignedPersonId" to (task.assignedPersonId ?: ""),
        "assignedPersonName" to (task.assignedPersonName ?: ""),
        "assignedById" to task.assignedById,
        "assignedByName" to task.assignedByName,
        "startDate" to task.startDate,
        "dueDate" to task.dueDate,
        "priority" to task.priority.name,
        "meetingId" to task.meetingId,
        "meetingTitle" to task.meetingTitle,
        "transcriptReferenceQuote" to task.transcriptReferenceQuote,
        "status" to task.status.name,
        "needsManagerApproval" to task.needsManagerApproval,
        "isApprovedByManager" to task.isApprovedByManager
      )
      firestore.collection("tasks").document(task.id).set(taskData).await()
    } catch (e: Throwable) {
      // Fail-safe handling
    }
  }
}
