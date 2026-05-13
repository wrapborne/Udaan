// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/ReviewScreen.kt
package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.viplove.licadvisornative.ui.viewmodel.ReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    reviewViewModel: ReviewViewModel = viewModel()
) {
    val uiState by reviewViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Lead Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ReviewViewModel.ReviewUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is ReviewViewModel.ReviewUiState.Error -> {
                    Text(text = state.message)
                }
                is ReviewViewModel.ReviewUiState.Success -> {
                    // Conditional logic to show the correct view based on user role
                    if (state.userRole == "admin") {
                        DataSheetView(dataSheet = state.dataSheet)
                    } else {
                        ReviewAndSubmitStep(dataSheet = state.dataSheet)
                    }
                }
            }
        }
    }
}
