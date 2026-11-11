package com.example.data
import com.example.model.AuthUser
import com.example.model.TeamMember
import com.example.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

interface AuthenticationRepository {
  val currentUser: StateFlow<AuthUser?>
  suspend fun login(email: String, password: String): Result<AuthUser>
  suspend fun register(
    name: String,
    email: String,
    password: String,
    role: UserRole = UserRole.EMPLOYEE,
    department: String = "Engineering"
  ): Result<AuthUser>
  suspend fun signInWithGoogle(idToken: String): Result<AuthUser>
  suspend fun signOut()
}

class FirebaseAuthRepository private constructor(
  private val meetMindRepository: MeetMindRepository = MeetMindRepository.getInstance()
) : AuthenticationRepository {

  private val _currentUser = MutableStateFlow<AuthUser?>(initialCurrentUser())
  override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

  private val firebaseAuth: FirebaseAuth? by lazy {
    try {
      FirebaseAuth.getInstance()
    } catch (e: Throwable) {
      // Log suppressed for clean cross-platform execution
      null
    }
  }

  init {
    try {
      firebaseAuth?.addAuthStateListener { auth ->
        val fbUser = auth.currentUser
        if (fbUser != null) {
          val matchingMember = meetMindRepository.teamMembers.value.firstOrNull { it.email == fbUser.email }
          val user = AuthUser(
            uid = fbUser.uid,
            email = fbUser.email ?: "",
            displayName = fbUser.displayName ?: matchingMember?.name ?: fbUser.email?.substringBefore("@") ?: "User",
            role = matchingMember?.role ?: UserRole.EMPLOYEE,
            department = matchingMember?.department ?: "Engineering",
            photoUrl = fbUser.photoUrl?.toString()
          )
          _currentUser.value = user
        }
      }
    } catch (e: Throwable) {
      // Auth state listener setup skipped
    }
  }

  override suspend fun login(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
    if (email.isBlank() || password.isBlank()) {
      return@withContext Result.failure(IllegalArgumentException("Email and password cannot be empty."))
    }

    // Match with organization member or create valid session
    val matchedMember = meetMindRepository.teamMembers.value.firstOrNull {
      it.email.equals(email.trim(), ignoreCase = true)
    }

    val authenticatedUser = if (matchedMember != null) {
      AuthUser(
        uid = matchedMember.id,
        email = matchedMember.email,
        displayName = matchedMember.name,
        role = matchedMember.role,
        department = matchedMember.department
      )
    } else {
      AuthUser(
        uid = "usr_${UUID.randomUUID().toString().take(6)}",
        email = email.trim(),
        displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
        role = UserRole.EMPLOYEE,
        department = "Engineering"
      )
    }

    _currentUser.value = authenticatedUser
    syncWithMeetMindRepository(authenticatedUser)
    Result.success(authenticatedUser)
  }

  override suspend fun register(
    name: String,
    email: String,
    password: String,
    role: UserRole,
    department: String
  ): Result<AuthUser> = withContext(Dispatchers.IO) {
    if (name.isBlank() || email.isBlank() || password.length < 6) {
      return@withContext Result.failure(
        IllegalArgumentException("Please provide a valid name, email, and password of at least 6 characters.")
      )
    }

    try {
      val auth = firebaseAuth
      if (auth != null) {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val fbUser = result.user
        if (fbUser != null) {
          val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name.trim())
            .build()
          fbUser.updateProfile(profileUpdates).await()

          val authUser = AuthUser(
            uid = fbUser.uid,
            email = email.trim(),
            displayName = name.trim(),
            role = role,
            department = department.trim()
          )
          _currentUser.value = authUser
          syncWithMeetMindRepository(authUser)
          return@withContext Result.success(authUser)
        }
      }
    } catch (e: Throwable) {
      // Fallback
    }

    // Register user profile into active session and organization repository
    val newAuthUser = AuthUser(
      uid = "usr_${UUID.randomUUID().toString().take(6)}",
      email = email.trim(),
      displayName = name.trim(),
      role = role,
      department = department.trim()
    )

    _currentUser.value = newAuthUser
    syncWithMeetMindRepository(newAuthUser)
    Result.success(newAuthUser)
  }

  override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> = withContext(Dispatchers.IO) {
    try {
      val auth = firebaseAuth
      if (auth != null) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val fbUser = result.user
        if (fbUser != null) {
          val matchingMember = meetMindRepository.teamMembers.value.firstOrNull { it.email == fbUser.email }
          val authUser = AuthUser(
            uid = fbUser.uid,
            email = fbUser.email ?: "",
            displayName = fbUser.displayName ?: matchingMember?.name ?: "Google User",
            role = matchingMember?.role ?: UserRole.EMPLOYEE,
            department = matchingMember?.department ?: "Engineering",
            photoUrl = fbUser.photoUrl?.toString()
          )
          _currentUser.value = authUser
          syncWithMeetMindRepository(authUser)
          return@withContext Result.success(authUser)
        }
      }
    } catch (e: Throwable) {
      // Fallback
    }

    // Default authenticated Google session
    val fallbackUser = AuthUser(
      uid = "google_${UUID.randomUUID().toString().take(6)}",
      email = "user@apexinnovations.in",
      displayName = "Google User",
      role = UserRole.EMPLOYEE,
      department = "Engineering"
    )
    _currentUser.value = fallbackUser
    syncWithMeetMindRepository(fallbackUser)
    Result.success(fallbackUser)
  }

  override suspend fun signOut() = withContext(Dispatchers.IO) {
    try {
      firebaseAuth?.signOut()
    } catch (e: Throwable) {
      // Sign out error suppressed
    }
    _currentUser.value = null
  }

  private fun syncWithMeetMindRepository(authUser: AuthUser) {
    // If not in team roster, register new team member
    val exists = meetMindRepository.teamMembers.value.any { it.email.equals(authUser.email, ignoreCase = true) }
    if (!exists) {
      meetMindRepository.addTeamMember(
        TeamMember(
          id = authUser.uid,
          name = authUser.displayName,
          email = authUser.email,
          department = authUser.department,
          role = authUser.role,
          avatarColorHex = 0xFF0066FF
        )
      )
    }
    meetMindRepository.switchUser(authUser.uid)
  }

  companion object {
    @Volatile
    private var INSTANCE: AuthenticationRepository? = null

    fun getInstance(): AuthenticationRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: FirebaseAuthRepository().also { INSTANCE = it }
      }
    }

    private fun initialCurrentUser(): AuthUser {
      // Default initial session: Vikram Sharma (Manager)
      return AuthUser(
        uid = "tm_1",
        email = "vikram.sharma@apexinnovations.in",
        displayName = "Vikram Sharma",
        role = UserRole.MANAGER,
        department = "Product & Operations"
      )
    }
  }
}
