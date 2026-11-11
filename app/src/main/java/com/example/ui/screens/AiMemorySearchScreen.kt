package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiSearchResult
import com.example.data.MeetMindRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMemorySearchScreen(
  repository: MeetMindRepository,
  onNavigateToMeeting: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var query by remember { mutableStateOf("") }
  var searchResult by remember { mutableStateOf<AiSearchResult?>(null) }
  var isSearching by remember { mutableStateOf(false) }

  val sampleQuestions = listOf(
    "What was decided about the payment gateway?",
    "What are Rohan's active marketing assignments?",
    "Compare decisions across sprint meetings",
    "Show all high severity blockers"
  )

  fun executeSearch(q: String) {
    if (q.isBlank()) return
    query = q
    isSearching = true
    searchResult = repository.searchAiMemory(q)
    isSearching = false
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = null,
              tint = ElectricBluePrimary
            )
            Text(
              text = "AI Meeting Memory",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
    ) {
      // Intro Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Cross-Meeting Intelligence",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = ElectricBluePrimary
            )
            Text(
              text = "Ask any question about previous team discussions, verify decisions across past quarters, and surface pending action items.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Search Query Box
      item {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("Ask MeetMind AI anything about past meetings...") },
          trailingIcon = {
            IconButton(
              onClick = { executeSearch(query) },
              modifier = Modifier.testTag("submit_ai_memory_search_button")
            ) {
              Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Search", tint = ElectricBluePrimary)
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_memory_query_input")
        )
      }

      // Suggested Prompts
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Suggested Questions:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sampleQuestions) { q ->
              SuggestionChip(
                onClick = { executeSearch(q) },
                label = { Text(q, fontSize = 11.sp) }
              )
            }
          }
        }
      }

      // Result Section
      if (searchResult != null) {
        val result = searchResult!!

        // Direct AI Synthesis Answer
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ElectricBluePrimary),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text("MeetMind AI Answer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }

              Text(
                text = result.directAnswer,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
              )
            }
          }
        }

        // Matched Decisions
        if (result.matchedDecisions.isNotEmpty()) {
          item {
            Text("Indexed Past Decisions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          items(result.matchedDecisions) { dec ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
              ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                Text(dec, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }

        // Referenced Meetings
        if (result.referencedMeetings.isNotEmpty()) {
          item {
            Text("Referenced Meeting Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          items(result.referencedMeetings) { meeting ->
            MeetingItemCard(
              meeting = meeting,
              onClick = { onNavigateToMeeting(meeting.id) }
            )
          }
        }
      }
    }
  }
}
