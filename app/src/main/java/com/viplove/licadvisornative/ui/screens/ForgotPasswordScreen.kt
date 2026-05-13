package com.viplove.licadvisornative.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.viplove.licadvisornative.ui.viewmodel.ForgotPasswordViewModel

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    val uiState by forgotPasswordViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // --- ADDED ---
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is ForgotPasswordViewModel.ForgotPasswordState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                navController.popBackStack() // Go back to the login screen
            }
            is ForgotPasswordViewModel.ForgotPasswordState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Reset Password", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enter your email address and we will send you a link to reset your password.")
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            // --- ADDED ---
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    forgotPasswordViewModel.sendPasswordResetEmail(email)
                }
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { forgotPasswordViewModel.sendPasswordResetEmail(email) },
            enabled = uiState !is ForgotPasswordViewModel.ForgotPasswordState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is ForgotPasswordViewModel.ForgotPasswordState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Send Reset Link")
            }
        }
        TextButton(onClick = { navController.popBackStack() }) {
            Text("Back to Login")
        }
    }
}
