package com.example

import com.example.data.AuthenticationRepository
import com.example.data.MeetMindRepository
import com.example.model.AuthUser
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.model.UserRole
import com.example.ui.auth.AuthUiState
import com.example.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAuthRepository : AuthenticationRepository {
  private val _currentUser = MutableStateFlow<AuthUser?>(null)
  override val currentUser: StateFlow<AuthUser?> = _currentUser

  override suspend fun login(email: String, password: String): Result<AuthUser> {
    val user = AuthUser(
      uid = "usr_1",
      email = email,
      displayName = "Vikram Sharma",
      role = UserRole.MANAGER,
      department = "Product & Operations"
    )
    _currentUser.value = user
    return Result.success(user)
  }

  override suspend fun register(
    name: String,
    email: String,
    password: String,
    role: UserRole,
    department: String
  ): Result<AuthUser> {
    val user = AuthUser(
      uid = "usr_2",
      email = email,
      displayName = name,
      role = role,
      department = department
    )
    _currentUser.value = user
    return Result.success(user)
  }

  override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> {
    val user = AuthUser(
      uid = "usr_google",
      email = "google@test.com",
      displayName = "Google User",
      role = UserRole.EMPLOYEE,
      department = "Engineering"
    )
    _currentUser.value = user
    return Result.success(user)
  }

  override suspend fun signOut() {
    _currentUser.value = null
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun testRepositoryInitialization() {
    val repository = MeetMindRepository.getInstance()
    assertTrue(repository.meetings.value.isNotEmpty())
    assertTrue(repository.tasks.value.isNotEmpty())
    assertTrue(repository.teamMembers.value.isNotEmpty())
  }

  @Test
  fun testManagerTaskApproval() {
    val repository = MeetMindRepository.getInstance()
    val unapproved = repository.tasks.value.firstOrNull { it.needsManagerApproval }
    assertNotNull(unapproved)

    repository.approveTask(
      taskId = unapproved!!.id,
      assignedPersonId = "tm_3",
      assignedPersonName = "Aarav Mehta",
      dueDate = "Nov 25, 2026",
      priority = TaskPriority.HIGH
    )

    val updated = repository.tasks.value.first { it.id == unapproved.id }
    assertEquals(TaskStatus.IN_PROGRESS, updated.status)
    assertEquals("Aarav Mehta", updated.assignedPersonName)
    assertEquals(false, updated.needsManagerApproval)
  }

  @Test
  fun testAiMemorySearch() {
    val repository = MeetMindRepository.getInstance()
    val result = repository.searchAiMemory("payment")
    assertTrue(result.directAnswer.contains("Razorpay") || result.directAnswer.contains("UPI"))
  }

  @Test
  fun testAuthViewModelValidation() {
    val authViewModel = AuthViewModel(FakeAuthRepository())

    // Login validation
    assertNotNull(authViewModel.validateLoginForm("", "pass123"))
    assertNotNull(authViewModel.validateLoginForm("invalid-email", "pass123"))
    assertNotNull(authViewModel.validateLoginForm("test@domain.com", "123"))
    assertNull(authViewModel.validateLoginForm("test@domain.com", "validPass123"))

    // Registration validation
    assertNotNull(authViewModel.validateRegistrationForm("", "test@domain.com", "pass123", "pass123"))
    assertNotNull(authViewModel.validateRegistrationForm("User", "test@domain.com", "pass123", "mismatch"))
    assertNull(authViewModel.validateRegistrationForm("John Doe", "john@apexinnovations.in", "pass1234", "pass1234"))
  }

  @Test
  fun testAuthViewModelLoginFlow() = runTest {
    val fakeRepo = FakeAuthRepository()
    val authViewModel = AuthViewModel(fakeRepo)

    authViewModel.login("vikram.sharma@apexinnovations.in", "password123")
    val user = fakeRepo.currentUser.value
    assertNotNull(user)
    assertEquals("vikram.sharma@apexinnovations.in", user!!.email)
    assertEquals(UserRole.MANAGER, user.role)
    assertTrue(authViewModel.uiState.value is AuthUiState.Authenticated)
  }

  @Test
  fun testAuthViewModelSignOut() = runTest {
    val fakeRepo = FakeAuthRepository()
    val authViewModel = AuthViewModel(fakeRepo)
    fakeRepo.login("test@test.com", "pass123")
    assertNotNull(fakeRepo.currentUser.value)

    authViewModel.signOut()
    assertNull(fakeRepo.currentUser.value)
    assertTrue(authViewModel.uiState.value is AuthUiState.Idle)
  }

  @Test
  fun testAuthViewModelGoogleSignIn() = runTest {
    val fakeRepo = FakeAuthRepository()
    val authViewModel = AuthViewModel(fakeRepo)

    authViewModel.signInWithGoogle("valid_token")
    assertTrue(authViewModel.uiState.value is AuthUiState.Authenticated)

    val googleUser = (authViewModel.uiState.value as AuthUiState.Authenticated).user
    assertEquals("google@test.com", googleUser.email)

    authViewModel.onGoogleSignInError("Google play services unavailable")
    assertTrue(authViewModel.uiState.value is AuthUiState.Error)
    assertEquals("Google play services unavailable", (authViewModel.uiState.value as AuthUiState.Error).message)
  }
}
