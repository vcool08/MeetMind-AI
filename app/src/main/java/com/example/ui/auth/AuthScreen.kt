package com.example.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun AuthScreen(
  authViewModel: AuthViewModel,
  onDemoLogin: (role: UserRole) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val userAuthManager = remember { UserAuthManager.getInstance(context) }

  var isRegisterMode by remember { mutableStateOf(false) }

  var email by remember { mutableStateOf("vikram.sharma@apexinnovations.in") }
  var password by remember { mutableStateOf("apex@2026") }
  var name by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var selectedRole by remember { mutableStateOf(UserRole.MANAGER) }
  var department by remember { mutableStateOf("Product & Operations") }
  var passwordVisible by remember { mutableStateOf(false) }

  val uiState by authViewModel.uiState.collectAsState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            DeepNavyBackgroundDark,
            Color(0xFF0F274E),
            DeepNavyBackgroundDark
          )
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(top = 48.dp, bottom = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Branding Header
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              colors = listOf(ElectricCyanAccent, ElectricBluePrimary)
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.GraphicEq,
          contentDescription = "MeetMind AI Logo",
          tint = Color.White,
          modifier = Modifier.size(40.dp)
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "MeetMind AI",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Intelligent Meeting Assistant & Task Engine",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.8f),
          textAlign = TextAlign.Center
        )
      }

      // Card for Sign In / Registration Form
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Tab Switcher
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(4.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (!isRegisterMode) ElectricBluePrimary else Color.Transparent)
                .clickable {
                  isRegisterMode = false
                  authViewModel.clearError()
                }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Sign In",
                color = if (!isRegisterMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
              )
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isRegisterMode) ElectricBluePrimary else Color.Transparent)
                .clickable {
                  isRegisterMode = true
                  authViewModel.clearError()
                }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Register",
                color = if (isRegisterMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Error banner
          if (uiState is AuthUiState.Error) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = StatusError.copy(alpha = 0.15f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                Text(
                  text = (uiState as AuthUiState.Error).message,
                  color = StatusError,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          if (isRegisterMode) {
            OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("Full Name") },
              singleLine = true,
              leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
              modifier = Modifier.fillMaxWidth()
            )
          }

          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Work Email") },
            singleLine = true,
            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = null
                )
              }
            },
            modifier = Modifier.fillMaxWidth()
          )

          if (isRegisterMode) {
            OutlinedTextField(
              value = confirmPassword,
              onValueChange = { confirmPassword = it },
              label = { Text("Confirm Password") },
              singleLine = true,
              visualTransformation = PasswordVisualTransformation(),
              leadingIcon = { Icon(imageVector = Icons.Default.LockClock, contentDescription = null) },
              modifier = Modifier.fillMaxWidth()
            )

            Text(
              text = "Organization Role:",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              UserRole.values().forEach { role ->
                FilterChip(
                  selected = role == selectedRole,
                  onClick = { selectedRole = role },
                  label = { Text(role.label) }
                )
              }
            }
          }

          // Submit Button
          Button(
            onClick = {
              if (isRegisterMode) {
                authViewModel.register(
                  name = name,
                  email = email,
                  password = password,
                  confirmPassword = confirmPassword,
                  role = selectedRole,
                  department = department
                )
              } else {
                authViewModel.login(email, password)
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("auth_submit_button")
          ) {
            if (uiState is AuthUiState.Loading) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Text(
                text = if (isRegisterMode) "Create Account" else "Sign In to Workspace",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

          // Google Sign-In Button
          OutlinedButton(
            onClick = {
              val activity = context as? Activity
              if (activity != null) {
                userAuthManager.launchGoogleSignIn(
                  activity = activity,
                  scope = coroutineScope,
                  onSuccess = { user ->
                    authViewModel.onAuthenticated(user)
                  },
                  onError = { errorMessage ->
                    authViewModel.onGoogleSignInError(errorMessage)
                  },
                  onCancelled = {
                    // Sign in was dismissed or cancelled by the user
                  }
                )
              } else {
                authViewModel.signInWithGoogle("sample_google_id_token")
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("google_signin_button")
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = null,
              tint = ElectricBluePrimary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
          }
        }
      }

      // Quick 1-Tap Demo Access Box (for users exploring without configuration)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = Color(0xFF0F2342).copy(alpha = 0.9f)
        )
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = ElectricCyanAccent,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "Quick 1-Tap Demo Access",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Text(
            text = "Explore the complete meeting recording, AI diarization, smart task extraction, and team dashboard immediately:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { onDemoLogin(UserRole.MANAGER) },
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBluePrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("demo_login_manager_button")
            ) {
              Text("Manager View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = { onDemoLogin(UserRole.EMPLOYEE) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("demo_login_employee_button")
            ) {
              Text("Employee View", fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}
