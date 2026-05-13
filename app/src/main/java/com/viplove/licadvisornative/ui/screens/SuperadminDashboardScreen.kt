package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.viplove.licadvisornative.model.User
import com.viplove.licadvisornative.ui.viewmodel.SuperadminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperadminDashboardScreen(
    navController: NavController,
    superadminViewModel: SuperadminViewModel = viewModel()
) {
    val uiState by superadminViewModel.uiState.collectAsState()
    val currentUser by superadminViewModel.currentUser.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    // --- UPDATED: Added "Forms" and "Circulars" tabs ---
    val tabs = listOf("Admin Management", "Graphics", "ULIP Plans", "Forms", "Circulars")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Superadmin Panel") },
                actions = {
                    IconButton(onClick = {
                        superadminViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            // --- UPDATED: 'when' block to show new screen ---
            when (selectedTabIndex) {
                0 -> AdminManagementTab(uiState, superadminViewModel)
                1 -> GraphicsManagementScreen()
                2 -> UlipPlanManagementScreen() // New screen for managing ULIPs
                3 -> {
                    FormsScreen(
                        userRole = "superadmin",
                        userEmail = currentUser?.email ?: "",
                        userName = currentUser?.name ?: ""
                    )
                }
                4 -> {
                    CircularsScreen(
                        userRole = "superadmin",
                        userEmail = currentUser?.email ?: "",
                        userName = currentUser?.name ?: ""
                    )
                }
            }
        }
    }
}

/**
 * Composable for the "Admin Management" tab content.
 */
@Composable
fun AdminManagementTab(
    uiState: SuperadminViewModel.UserListUiState,
    superadminViewModel: SuperadminViewModel
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is SuperadminViewModel.UserListUiState.Loading -> {
                CircularProgressIndicator()
            }
            is SuperadminViewModel.UserListUiState.Error -> {
                Text(text = state.message)
            }
            is SuperadminViewModel.UserListUiState.Success -> {
                AdminListContent(
                    admins = state.users,
                    onApproveClick = { uid ->
                        superadminViewModel.approveAdmin(uid)
                    }
                )
            }
        }
    }
}


@Composable
fun AdminListContent(admins: List<User>, onApproveClick: (String) -> Unit) {
    val pendingAdmins = admins.filter { !it.isApproved }
    val approvedAdmins = admins.filter { it.isApproved }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Pending Admin Registrations", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (pendingAdmins.isEmpty()) {
            item {
                Text("No pending admin registrations.", modifier = Modifier.padding(vertical = 8.dp))
            }
        } else {
            items(pendingAdmins) { admin ->
                AdminItemCard(user = admin, isPending = true, onApproveClick = onApproveClick)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("Manage Registered Admins", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (approvedAdmins.isEmpty()) {
            item {
                Text("No registered admins found.", modifier = Modifier.padding(vertical = 8.dp))
            }
        } else {
            items(approvedAdmins) { admin ->
                AdminItemCard(user = admin, isPending = false, onApproveClick = {})
            }
        }
    }
}

@Composable
fun AdminItemCard(user: User, isPending: Boolean, onApproveClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = user.email, modifier = Modifier.weight(1f))
            if (isPending) {
                Button(onClick = { onApproveClick(user.uid) }) {
                    Text("Approve")
                }
            }
        }
    }
}
