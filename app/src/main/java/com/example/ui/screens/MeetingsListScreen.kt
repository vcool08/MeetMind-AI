package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MeetMindRepository
import com.example.model.Meeting
import com.example.ui.theme.ElectricBluePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsListScreen(
  repository: MeetMindRepository,
  onNavigateToRecord: () -> Unit,
  onNavigateToMeeting: (String) -> Unit,
  onNavigateToAiMemory: () -> Unit,
  modifier: Modifier = Modifier
) {
  val meetings by repository.meetings.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var selectedLanguageFilter by remember { mutableStateOf("ALL") }

  val languages = listOf("ALL") + meetings.map { it.language }.distinct()

  val filteredMeetings = meetings.filter { meeting ->
    val matchesLang = selectedLanguageFilter == "ALL" || meeting.language.equals(selectedLanguageFilter, ignoreCase = true)
    val matchesSearch = searchQuery.isBlank() ||
      meeting.title.contains(searchQuery, ignoreCase = true) ||
      meeting.language.contains(searchQuery, ignoreCase = true) ||
      meeting.transcript.any { it.text.contains(searchQuery, ignoreCase = true) || it.speakerName.contains(searchQuery, ignoreCase = true) }
    matchesLang && matchesSearch
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = onNavigateToRecord,
        containerColor = ElectricBluePrimary,
        contentColor = Color.White,
        modifier = Modifier.testTag("record_meeting_fab")
      ) {
        Icon(imageVector = Icons.Default.Mic, contentDescription = "Record Meeting")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Recorded Meetings",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${meetings.size} sessions with AI transcripts & tasks",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(
            onClick = onNavigateToAiMemory,
            modifier = Modifier.testTag("meetings_ai_memory_button")
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = "Search Memory",
              tint = ElectricBluePrimary
            )
          }
        }
      }

      // Search Box
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search transcripts, speakers, or topics...") },
          leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("meeting_search_input")
        )
      }

      // Language Chips Row
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(languages) { lang ->
            FilterChip(
              selected = selectedLanguageFilter == lang,
              onClick = { selectedLanguageFilter = lang },
              label = { Text(if (lang == "ALL") "All Languages" else lang) }
            )
          }
        }
      }

      if (filteredMeetings.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(imageVector = Icons.Default.MicOff, contentDescription = null, tint = ElectricBluePrimary, modifier = Modifier.size(36.dp))
              Text("No meetings found", fontWeight = FontWeight.SemiBold)
              Text("Start a new AI meeting recording using the microphone button below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      } else {
        items(filteredMeetings, key = { it.id }) { meeting ->
          MeetingItemCard(
            meeting = meeting,
            onClick = { onNavigateToMeeting(meeting.id) }
          )
        }
      }
    }
  }
}
