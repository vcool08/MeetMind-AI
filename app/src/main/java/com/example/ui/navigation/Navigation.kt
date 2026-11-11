package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
  object Dashboard : Screen("dashboard", "Dashboard")
  object Meetings : Screen("meetings", "Meetings")
  object Tasks : Screen("tasks", "Tasks")
  object Team : Screen("team", "Team")
  object Profile : Screen("profile", "Profile")
  object RecordMeeting : Screen("record_meeting", "Record Meeting")
  object MeetingDetail : Screen("meeting_detail/{meetingId}", "Meeting Analysis") {
    fun createRoute(meetingId: String) = "meeting_detail/$meetingId"
  }
  object AiMemorySearch : Screen("ai_memory_search", "AI Meeting Memory")
}
