package com.example.model

enum class UserRole(val label: String) {
  ADMIN("Admin"),
  MANAGER("Manager"),
  EMPLOYEE("Employee")
}

data class TeamMember(
  val id: String,
  val name: String,
  val email: String,
  val department: String,
  val role: UserRole,
  val avatarInitials: String = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase(),
  val avatarColorHex: Long = 0xFF0066FF
)
