package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioRecordManager
import com.example.data.FirestoreService
import com.example.data.MeetMindRepository
import com.example.model.UserRole
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.MeetMindTheme

sealed class BottomNavItem(
  val route: String,
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  object Dashboard : BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
  object Meetings : BottomNavItem(Screen.Meetings.route, "Meetings", Icons.Filled.Videocam, Icons.Outlined.Videocam)
  object Tasks : BottomNavItem(Screen.Tasks.route, "Tasks", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment)
  object Team : BottomNavItem(Screen.Team.route, "Team", Icons.Filled.People, Icons.Outlined.PeopleOutline)
  object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

class MainActivity : ComponentActivity() {

  private lateinit var audioRecordManager: AudioRecordManager
  private lateinit var firestoreService: FirestoreService

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val repository = MeetMindRepository.getInstance()
    audioRecordManager = AudioRecordManager(applicationContext)

    // Connect Cloud Firestore service for real cloud data synchronization
    firestoreService = FirestoreService(applicationContext)
    repository.setFirestoreService(firestoreService)

    setContent {
      MeetMindTheme {
        val authViewModel: AuthViewModel = viewModel()
        val currentUser by authViewModel.currentUser.collectAsState()

        if (currentUser == null) {
          AuthScreen(
            authViewModel = authViewModel,
            onDemoLogin = { role ->
              val demoEmail = if (role == UserRole.MANAGER) "vikram.sharma@apexinnovations.in" else "priya.patel@apexinnovations.in"
              authViewModel.login(demoEmail, "apex@2026")
            }
          )
        } else {
          MeetMindApp(
            repository = repository,
            audioRecordManager = audioRecordManager,
            onSignOut = {
              authViewModel.signOut()
            }
          )
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    audioRecordManager.cleanup()
  }
}

@Composable
fun MeetMindApp(
  repository: MeetMindRepository,
  audioRecordManager: AudioRecordManager,
  onSignOut: () -> Unit
) {
  var currentRoute by remember { mutableStateOf(Screen.Dashboard.route) }
  var activeMeetingId by remember { mutableStateOf<String?>(null) }

  val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Meetings,
    BottomNavItem.Tasks,
    BottomNavItem.Team,
    BottomNavItem.Profile
  )

  val isBottomBarVisible = currentRoute in listOf(
    Screen.Dashboard.route,
    Screen.Meetings.route,
    Screen.Tasks.route,
    Screen.Team.route,
    Screen.Profile.route
  )

  // Back handling
  BackHandler(enabled = currentRoute != Screen.Dashboard.route) {
    when {
      currentRoute.startsWith("meeting_detail") ||
        currentRoute == Screen.RecordMeeting.route ||
        currentRoute == Screen.AiMemorySearch.route -> {
        currentRoute = Screen.Dashboard.route
      }
      else -> {
        currentRoute = Screen.Dashboard.route
      }
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      if (isBottomBarVisible) {
        NavigationBar(
          windowInsets = WindowInsets.navigationBars,
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 6.dp
        ) {
          bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
              selected = isSelected,
              onClick = {
                currentRoute = item.route
              },
              icon = {
                Icon(
                  imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = item.title,
                  tint = if (isSelected) ElectricBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              label = {
                Text(
                  text = item.title,
                  color = if (isSelected) ElectricBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              modifier = Modifier.testTag("nav_item_${item.route}")
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when {
        currentRoute == Screen.Dashboard.route -> {
          DashboardScreen(
            repository = repository,
            onNavigateToRecord = { currentRoute = Screen.RecordMeeting.route },
            onNavigateToMeeting = { meetingId ->
              if (meetingId.isNotBlank()) {
                activeMeetingId = meetingId
                currentRoute = Screen.MeetingDetail.createRoute(meetingId)
              } else {
                currentRoute = Screen.Meetings.route
              }
            },
            onNavigateToTasks = { currentRoute = Screen.Tasks.route },
            onNavigateToAiMemory = { currentRoute = Screen.AiMemorySearch.route }
          )
        }

        currentRoute == Screen.Meetings.route -> {
          MeetingsListScreen(
            repository = repository,
            onNavigateToRecord = { currentRoute = Screen.RecordMeeting.route },
            onNavigateToMeeting = { meetingId ->
              activeMeetingId = meetingId
              currentRoute = Screen.MeetingDetail.createRoute(meetingId)
            },
            onNavigateToAiMemory = { currentRoute = Screen.AiMemorySearch.route }
          )
        }

        currentRoute == Screen.Tasks.route -> {
          TasksScreen(
            repository = repository,
            onNavigateToMeeting = { meetingId ->
              activeMeetingId = meetingId
              currentRoute = Screen.MeetingDetail.createRoute(meetingId)
            }
          )
        }

        currentRoute == Screen.Team.route -> {
          TeamScreen(
            repository = repository,
            onNavigateToTasksForMember = { memberId ->
              currentRoute = Screen.Tasks.route
            }
          )
        }

        currentRoute == Screen.Profile.route -> {
          ProfileScreen(
            repository = repository,
            onSignOut = onSignOut
          )
        }

        currentRoute == Screen.RecordMeeting.route -> {
          RecordMeetingScreen(
            repository = repository,
            audioRecordManager = audioRecordManager,
            onMeetingSaved = { newMeetingId ->
              activeMeetingId = newMeetingId
              currentRoute = Screen.MeetingDetail.createRoute(newMeetingId)
            },
            onBack = { currentRoute = Screen.Dashboard.route }
          )
        }

        currentRoute.startsWith("meeting_detail") -> {
          MeetingDetailScreen(
            meetingId = activeMeetingId ?: "",
            repository = repository,
            onBack = { currentRoute = Screen.Meetings.route },
            onNavigateToTasks = { currentRoute = Screen.Tasks.route }
          )
        }

        currentRoute == Screen.AiMemorySearch.route -> {
          AiMemorySearchScreen(
            repository = repository,
            onNavigateToMeeting = { meetingId ->
              activeMeetingId = meetingId
              currentRoute = Screen.MeetingDetail.createRoute(meetingId)
            },
            onBack = { currentRoute = Screen.Dashboard.route }
          )
        }
      }
    }
  }
}
