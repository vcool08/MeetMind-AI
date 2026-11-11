package com.example.model

data class AuthUser(
  val uid: String,
  val email: String,
  val displayName: String,
  val role: UserRole = UserRole.EMPLOYEE,
  val department: String = "Engineering",
  val photoUrl: String? = null
) {
  val avatarInitials: String
    get() = displayName.split(" ")
      .mapNotNull { it.firstOrNull()?.toString() }
      .take(2)
      .joinToString("")
      .uppercase()
      .ifEmpty { "U" }
}
