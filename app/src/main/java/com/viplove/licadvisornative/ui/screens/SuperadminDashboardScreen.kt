package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.viplove.licadvisornative.model.User
import com.viplove.licadvisornative.ui.components.AppScaffold
import com.viplove.licadvisornative.ui.components.EmptyState
import com.viplove.licadvisornative.ui.components.PillChip
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.BrandSuccess
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.SuperadminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperadminDashboardScreen(
    navController: NavController,
    superadminViewModel: SuperadminViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by superadminViewModel.uiState.collectAsState()
    val currentUser by superadminViewModel.currentUser.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(SuperadminTab.ADMIN_MANAGEMENT) }

    AppScaffold(
        title = "LIC Udaan · Superadmin",
        actions = {
            IconButton(onClick = {
                superadminViewModel.logout()
                restartAppAfterLogout(context)
            }) {
                Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = Color.White)
            }
        }
    ) {
        Column {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenHPadding, vertical = Dimens.GutterSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
            ) {
                items(SuperadminTab.entries) { tab ->
                    PillChip(
                        label = tab.label(),
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
            // --- UPDATED: 'when' block to show new screen ---
            Crossfade(
                targetState = selectedTab,
                modifier = Modifier.fillMaxSize(),
                animationSpec = tween(TAB_FADE_MS),
                label = "dashboardTab"
            ) { tab ->
            when (tab) {
                SuperadminTab.ADMIN_MANAGEMENT -> AdminManagementTab(uiState, superadminViewModel)
                SuperadminTab.GRAPHICS -> GraphicsManagementScreen()
                SuperadminTab.ULIP_PLANS -> UlipPlanManagementScreen() // New screen for managing ULIPs
                SuperadminTab.FORMS -> {
                    FormsScreen(
                        userRole = "superadmin",
                        userEmail = currentUser?.email ?: "",
                        userName = currentUser?.name ?: ""
                    )
                }
                SuperadminTab.CIRCULARS -> {
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
        contentPadding = PaddingValues(Dimens.ScreenHPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterLg)
    ) {
        item {
            SectionCard(title = "Pending Admin Registrations") {
                if (pendingAdmins.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.PersonAdd,
                        title = "No pending admins",
                        message = "New DO registrations will appear here for approval."
                    )
                } else {
                    pendingAdmins.forEach { admin ->
                        AdminItemCard(user = admin, isPending = true, onApproveClick = onApproveClick)
                    }
                }
            }
        }

        item {
            SectionCard(title = "Manage Registered Admins") {
                if (approvedAdmins.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Groups,
                        title = "No registered admins",
                        message = "Approved DO accounts will be listed here."
                    )
                } else {
                    approvedAdmins.forEach { admin ->
                        AdminItemCard(user = admin, isPending = false, onApproveClick = {})
                    }
                }
            }
        }
    }
}

@Composable
fun AdminItemCard(user: User, isPending: Boolean, onApproveClick: (String) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.GutterXs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifBlank { "DO Admin" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isPending) {
                Button(onClick = { onApproveClick(user.uid) }) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimens.GutterXs))
                    Text("Approve")
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = BrandSuccess.copy(alpha = 0.14f),
                    contentColor = BrandSuccess
                ) {
                    Text(
                        "Approved",
                        modifier = Modifier.padding(horizontal = Dimens.GutterSm, vertical = Dimens.GutterXs),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** Superadmin dashboard tabs, in display order. Saved across navigation and rotation. */
private enum class SuperadminTab(val title: String) {
    ADMIN_MANAGEMENT("Admin Management"),
    GRAPHICS("Graphics"),
    ULIP_PLANS("ULIP Plans"),
    FORMS("Forms"),
    CIRCULARS("Circulars");

    @Composable
    fun label(): String = title
}
