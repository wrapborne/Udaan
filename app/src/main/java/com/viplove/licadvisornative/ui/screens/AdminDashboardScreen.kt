
package com.viplove.licadvisornative.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.viplove.licadvisornative.model.ClientDataSheet
import com.viplove.licadvisornative.model.Policy // <-- Uses the main Policy model
import com.viplove.licadvisornative.model.User
import com.viplove.licadvisornative.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    // --- UPDATED: Added "Performance" tab ---
    val tabs = listOf("Proposals", "ULIP", "Data Analysis", "Forms", "Circulars", "Premium Summaries", "Performance", "Advisor Management", "Leads", "Analytics", "Checker", "Graphics")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DO's Dashboard") },
                actions = {
                    IconButton(onClick = { adminViewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                    IconButton(onClick = {
                        adminViewModel.logout()
                        navController.navigate("login") { popUpTo(0) }
                    }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Network monitoring
        val context = LocalContext.current
        val isOnline by remember {
            com.viplove.licadvisornative.util.NetworkMonitor.observeConnectivity(context)
        }.collectAsState(initial = true)

        Column(modifier = Modifier.padding(paddingValues)) {
            OfflineBanner(isOnline = isOnline)
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            // --- UPDATED: 'when' block to add UlipScreen ---
            when (selectedTabIndex) {
                0 -> ProposalsTab(adminViewModel)
                1 -> {
                    val proposalsState by adminViewModel.proposalsUiState.collectAsState()
                    val proposalsUiState = proposalsState as? AdminViewModel.ProposalsUiState
                    val ulipPolicies = proposalsUiState?.filteredPolicies?.filter { it.isUlip } ?: emptyList()
                    UlipScreen(policies = ulipPolicies)
                }
                2 -> {
                AdminDateAnalysisTab(adminViewModel)
            }
            3 -> {
                val currentUser by adminViewModel.currentAdminState.collectAsState()
                FormsScreen(
                    userRole = "admin",
                    userEmail = currentUser?.email ?: "",
                    userName = currentUser?.name ?: ""
                )
            }
            4 -> {
                val currentUser by adminViewModel.currentAdminState.collectAsState()
                CircularsScreen(
                    userRole = "admin",
                    userEmail = currentUser?.email ?: "",
                    userName = currentUser?.name ?: ""
                )
            }
            5 -> PremiumSummaryTab(adminViewModel)
            6 -> {
                val proposalsState by adminViewModel.proposalsUiState.collectAsState()
                val policies = (proposalsState as? AdminViewModel.ProposalsUiState)?.allPolicies ?: emptyList()
                AgentPerformanceTab(policies = policies)
            }
            7 -> AdvisorManagementTab(adminViewModel)
            8 -> LeadsTab(navController, adminViewModel)
            9 -> AnalyticsTab(adminViewModel)
            10 -> NonMedicalCheckerScreen()
            11 -> GraphicsSelectionScreen(navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsTab(navController: NavController, adminViewModel: AdminViewModel) {
    val leadSearchQuery by adminViewModel.leadSearchQuery.collectAsState()
    val leadsState by adminViewModel.leadsUiState.collectAsState()
    val agentState by adminViewModel.agentListState.collectAsState()
    val proposalsState by adminViewModel.proposalsUiState.collectAsState()
    val context = LocalContext.current
    var showDownloadDialog by remember { mutableStateOf<ClientDataSheet?>(null) }
    var showArchiveDialog by remember { mutableStateOf<ClientDataSheet?>(null) }
    var showRestoreDialog by remember { mutableStateOf<ClientDataSheet?>(null) }
    var showArchived by remember { mutableStateOf(false) }

    // --- ADDED: Focus Manager ---
    val focusManager = LocalFocusManager.current

    when (val state = leadsState) {
        is AdminViewModel.LeadsUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AdminViewModel.LeadsUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message)
            }
        }
        is AdminViewModel.LeadsUiState.Success -> {
            val agentInfoMap = (agentState as? AdminViewModel.AgentListUiState.Success)
                ?.agents?.associateBy { it.uid } ?: emptyMap()
            val agentCodeToNameMap = remember(proposalsState) {
                (proposalsState as? AdminViewModel.ProposalsUiState)?.allPolicies
                    ?.filter { it.agentName.isNotBlank() }
                    ?.associateBy({ it.agentCode }, { it.agentName }) ?: emptyMap()
            }
            val leadsToDisplay = remember(state.leads, showArchived, leadSearchQuery) {
                state.leads
                    .filter { it.isArchived == showArchived }
                    .filter { lead ->
                        if (leadSearchQuery.isBlank()) {
                            true
                        } else {
                            val agent = agentInfoMap[lead.createdByAdvisorId]
                            val agentCode = agent?.agencyCode ?: ""
                            val leadName = lead.proposerDetails.name
                            leadName.contains(leadSearchQuery, ignoreCase = true) ||
                                    agentCode.contains(leadSearchQuery, ignoreCase = true)
                        }
                    }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = leadSearchQuery,
                    onValueChange = { adminViewModel.onLeadSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { Text("Search by Lead Name or Advisor Code") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    // --- ADDED: Keyboard actions ---
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showArchived,
                        onClick = { showArchived = false },
                        label = { Text("Active Leads") },
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null) }
                    )
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = true },
                        label = { Text("Archived Leads") },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (leadsToDisplay.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (showArchived) "No archived leads found." else "No active leads found.")
                            }
                        }
                    } else {
                        items(leadsToDisplay) { lead ->
                            val agent = agentInfoMap[lead.createdByAdvisorId]
                            val agentName = agent?.let { user ->
                                agentCodeToNameMap[user.agencyCode] ?: user.email
                            } ?: "Unknown Agent"

                            LeadCard(
                                lead = lead,
                                agentName = agentName,
                                agentCode = agent?.agencyCode ?: "",
                                isArchived = lead.isArchived,
                                onViewClick = { navController.navigate("review_screen/${lead.id}") },
                                onDownloadClick = { showDownloadDialog = lead },
                                onArchiveActionClick = {
                                    if (lead.isArchived) {
                                        showRestoreDialog = lead
                                    } else {
                                        showArchiveDialog = lead
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- ADDED: Dialog handling logic ---
    showDownloadDialog?.let { formToDownload ->
        DownloadOptionsDialog(
            formName = formToDownload.proposerDetails.name,
            onDismiss = { showDownloadDialog = null },
            onDownloadCombined = {
                adminViewModel.downloadLeadAsCombinedPdf(formToDownload, context)
                showDownloadDialog = null
            },
            onDownloadSeparate = {
                adminViewModel.downloadLeadAsSeparatePdfs(formToDownload, context)
                showDownloadDialog = null
            }
        )
    }

    showArchiveDialog?.let { formToArchive ->
        AlertDialog(
            onDismissRequest = { showArchiveDialog = null },
            title = { Text("Archive Form?") },
            text = { Text("Are you sure you want to archive this form?") },
            confirmButton = {
                Button(onClick = {
                    adminViewModel.archiveForm(formToArchive.id)
                    showArchiveDialog = null
                }) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = null }) { Text("Cancel") }
            }
        )
    }

    showRestoreDialog?.let { formToRestore ->
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text("Restore Form?") },
            text = { Text("Are you sure you want to restore this form?") },
            confirmButton = {
                Button(onClick = {
                    adminViewModel.unarchiveForm(formToRestore.id)
                    showRestoreDialog = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = null }) { Text("Cancel") }
            }
        )
    }
}
@Composable
fun LeadCard(
    lead: ClientDataSheet,
    agentName: String,
    agentCode: String,
    isArchived: Boolean,
    onViewClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onArchiveActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onViewClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(lead.proposerDetails.name.ifEmpty { "New Lead" }, style = MaterialTheme.typography.titleLarge)
            Text("From: $agentName ($agentCode)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            val date = lead.lastUpdated?.let { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(it) } ?: "Not yet saved"
            Text("Last updated: $date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onViewClick) {
                    Icon(Icons.Default.Visibility, contentDescription = "View", modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onDownloadClick) {
                    Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onArchiveActionClick) {
                    if (isArchived) {
                        Icon(Icons.Default.Unarchive, contentDescription = "Unarchive", modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Archive, contentDescription = "Archive", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalsTab(adminViewModel: AdminViewModel) {
    val proposalsState by adminViewModel.proposalsUiState.collectAsState()
    val proposalsUiState = proposalsState as? AdminViewModel.ProposalsUiState

    val agentState by adminViewModel.agentListState.collectAsState()
    val showDatePicker = remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- ADDED: Focus Manager ---
    val focusManager = LocalFocusManager.current

    // --- THIS IS THE FIX: Show a Toast message on error ---
    LaunchedEffect(proposalsUiState?.error) {
        proposalsUiState?.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            // Optionally reset the error in the viewmodel
        }
    }

    val proposalFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { adminViewModel.processAndUploadFile(it, context.contentResolver) }
        }
    )
    val premiumFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { adminViewModel.processPremiumSummary(it, context.contentResolver) }
        }
    )

    if (proposalsUiState != null && proposalsUiState.duplicatePolicies != null && proposalsUiState.pendingUpload != null) {
        val duplicateCount = proposalsUiState.duplicatePolicies!!.size
        AlertDialog(
            onDismissRequest = { adminViewModel.cancelPolicyUpload() },
            title = { Text("Duplicates Found") },
            text = { Text("The file contains $duplicateCount polic(ies) that already exist in the database. How would you like to proceed?") },
            confirmButton = {
                Button(onClick = { adminViewModel.confirmPolicyUpload(overwrite = true) }) {
                    Text("Overwrite All")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { adminViewModel.cancelPolicyUpload() }) { Text("Cancel") }
                    TextButton(onClick = { adminViewModel.confirmPolicyUpload(overwrite = false) }) { Text("Skip Duplicates") }
                }
            }
        )
    }

    val agentCodeToEmailMap = (agentState as? AdminViewModel.AgentListUiState.Success)
        ?.agents?.associate { it.agencyCode to it.email } ?: emptyMap()

    val policiesByAgent = remember(proposalsUiState?.filteredPolicies, proposalsUiState?.proposalSortOption, proposalsUiState?.proposalSortOrder) {
        if (proposalsUiState == null) return@remember emptyList()
        val grouped = proposalsUiState.filteredPolicies.groupBy { it.agentCode to it.agentName }
        when (proposalsUiState.proposalSortOption) {
            AdminViewModel.ProposalSortOption.CODE -> {
                if (proposalsUiState.proposalSortOrder == AdminViewModel.SortOrder.ASC) {
                    grouped.toList().sortedBy { (agentIdentifier, _) -> agentIdentifier.first }
                } else {
                    grouped.toList().sortedByDescending { (agentIdentifier, _) -> agentIdentifier.first }
                }
            }
            AdminViewModel.ProposalSortOption.POLICY_COUNT -> {
                if (proposalsUiState.proposalSortOrder == AdminViewModel.SortOrder.ASC) {
                    grouped.toList().sortedBy { (_, policies) -> policies.size }
                } else {
                    grouped.toList().sortedByDescending { (_, policies) -> policies.size }
                }
            }
        }
    }

    var expandedAgentCodes by remember { mutableStateOf(setOf<String>()) }
    
    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                adminViewModel.refreshData()
                kotlinx.coroutines.delay(1000)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { proposalFilePicker.launch("text/plain") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Proposals")
                }
                Button(
                    onClick = { premiumFilePicker.launch("*/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Premium")
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item {
            OutlinedTextField(
                value = proposalsState.searchQuery,
                onValueChange = { adminViewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by Name, Policy No or Plan") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                // --- ADDED: Keyboard actions ---
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }

        // --- NEW: Late Policy Filter Checkbox ---
        if (proposalsUiState != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = proposalsUiState.showOnlyLate,
                        onCheckedChange = { adminViewModel.onLateFilterToggled(it) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Show Only Late Policies (>30 days overdue)",
                        modifier = Modifier.clickable { adminViewModel.onLateFilterToggled(!proposalsUiState.showOnlyLate) }
                    )
                }
            }
        }

        if (proposalsUiState != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterDropdown(
                        label = "Plan",
                        options = proposalsUiState.availablePlans,
                        selectedOption = proposalsState.selectedPlan,
                        onOptionSelected = { adminViewModel.onPlanSelected(it) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterDropdown(
                        label = "Mode",
                        options = proposalsUiState.availableModes,
                        selectedOption = proposalsState.selectedMode,
                        onOptionSelected = { adminViewModel.onModeSelected(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text("Filter by Date:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                OutlinedButton(
                    onClick = { showDatePicker.value = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date Range")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = formatDateRange(proposalsState.startDate, proposalsState.endDate))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterDropdown(
                        label = "Financial Year",
                        options = proposalsUiState.availableFinancialYears,
                        selectedOption = proposalsState.selectedFinancialYear,
                        onOptionSelected = { adminViewModel.onFinancialYearSelected(it) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterDropdown(
                        label = "Appraisal Year",
                        options = proposalsUiState.availableAppraisalYears,
                        selectedOption = proposalsState.selectedAppraisalYear,
                        onOptionSelected = { adminViewModel.onAppraisalYearSelected(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatCard("Total Proposals", proposalsState.filteredPolicies.size.toString())
                    StatCard("ANANDA Count", proposalsState.filteredPolicies.count { it.isAnanda }.toString())
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("Sort By:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    SortChip(
                        text = "Code",
                        isSelected = proposalsUiState.proposalSortOption == AdminViewModel.ProposalSortOption.CODE,
                        sortOrder = proposalsUiState.proposalSortOrder,
                        onClick = { adminViewModel.onProposalSortChanged(AdminViewModel.ProposalSortOption.CODE) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SortChip(
                        text = "Policy Count",
                        isSelected = proposalsUiState.proposalSortOption == AdminViewModel.ProposalSortOption.POLICY_COUNT,
                        sortOrder = proposalsUiState.proposalSortOrder,
                        onClick = { adminViewModel.onProposalSortChanged(AdminViewModel.ProposalSortOption.POLICY_COUNT) }
                    )
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        if (policiesByAgent.isEmpty() && proposalsUiState?.isLoading == false) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No policies found for the selected filters.")
                }
            }
        } else {
            policiesByAgent.forEach { (agentIdentifier, policies) ->
                val (agentCode, agentName) = agentIdentifier
                val isExpanded = agentCode in expandedAgentCodes

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedAgentCodes = if (isExpanded) {
                                    expandedAgentCodes - agentCode
                                } else {
                                    expandedAgentCodes + agentCode
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val policyCount = policies.size
                        val headerText = "${agentName.ifEmpty { agentCodeToEmailMap[agentCode] ?: "Unknown Advisor" }} ($agentCode) - $policyCount Policies"
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }

                if (isExpanded) {
                    items(policies) { policy ->
                        DetailedPolicyCard(policy = policy)
                    }
                }
            }
        }
        }
    }

    if (showDatePicker.value) {
        // --- FIX: Use rememberDateRangePickerState ---
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker.value = false
                    adminViewModel.onDateRangeSelected(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker.value = false }) { Text("Cancel") } }
        ) {
            // --- FIX: Use DateRangePicker ---
            DateRangePicker(state = dateRangePickerState)
        }
    }
}

@Composable
fun PremiumSummaryTab(adminViewModel: AdminViewModel) {
    val summaryState by adminViewModel.premiumSummaryUiState.collectAsState()

    // --- NEW: Error Handling ---
    val context = LocalContext.current
    LaunchedEffect(summaryState.error) {
        summaryState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val summariesToShow = remember(summaryState.summariesByMonth, summaryState.selectedMonth, summaryState.summarySortOption, summaryState.summarySortOrder) {
        val allSummaries = if (summaryState.selectedMonth == "All") {
            summaryState.summariesByMonth.values.flatten()
        } else {
            summaryState.summariesByMonth[summaryState.selectedMonth] ?: emptyList()
        }

        when (summaryState.summarySortOption) {
            AdminViewModel.SummarySortOption.CODE -> {
                if (summaryState.summarySortOrder == AdminViewModel.SortOrder.ASC) {
                    allSummaries.sortedBy { it.agencyCode }
                } else {
                    allSummaries.sortedByDescending { it.agencyCode }
                }
            }
            AdminViewModel.SummarySortOption.PREMIUM -> {
                if (summaryState.summarySortOrder == AdminViewModel.SortOrder.ASC) {
                    allSummaries.sortedBy { it.totalScheduledPremium }
                } else {
                    allSummaries.sortedByDescending { it.totalScheduledPremium }
                }
            }
        }
    }

    if (summaryState.duplicateSummaries != null && summaryState.pendingSummaryUpload != null) {
        val duplicateCount = summaryState.duplicateSummaries!!.size
        AlertDialog(
            onDismissRequest = { adminViewModel.cancelSummaryUpload() },
            title = { Text("Duplicates Found") },
            text = { Text("The file contains $duplicateCount premium summar(ies) for advisors that already exist in this month. How would you like to proceed?") },
            confirmButton = {
                Button(onClick = { adminViewModel.confirmSummaryUpload(overwrite = true) }) {
                    Text("Overwrite All")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { adminViewModel.cancelSummaryUpload() }) { Text("Cancel") }
                    TextButton(onClick = { adminViewModel.confirmSummaryUpload(overwrite = false) }) { Text("Skip Duplicates") }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        FilterDropdown(
            label = "Select Month",
            options = summaryState.availableMonths,
            selectedOption = summaryState.selectedMonth,
            onOptionSelected = { adminViewModel.onMonthSelected(it) }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text("Sort By:", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(8.dp))
            SortChip(
                text = "Code",
                isSelected = summaryState.summarySortOption == AdminViewModel.SummarySortOption.CODE,
                sortOrder = summaryState.summarySortOrder,
                onClick = { adminViewModel.onSummarySortChanged(AdminViewModel.SummarySortOption.CODE) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            SortChip(
                text = "Premium",
                isSelected = summaryState.summarySortOption == AdminViewModel.SummarySortOption.PREMIUM,
                sortOrder = summaryState.summarySortOrder,
                onClick = { adminViewModel.onSummarySortChanged(AdminViewModel.SummarySortOption.PREMIUM) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        when {
            summaryState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            summaryState.error != null && summariesToShow.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(summaryState.error!!) }
            else -> {
                if (summariesToShow.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No summaries found for the selected month/year.")
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(summariesToShow) { summary ->
                                PremiumSummaryCard(summary = summary)
                            }
                        }
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        ) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Monthly Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("₹${String.format("%.2f", summaryState.monthlyTotalPremium)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Period Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("₹${String.format("%.2f", summaryState.appraisalYearTotalPremium)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvisorManagementTab(adminViewModel: AdminViewModel) {
    val agentState by adminViewModel.agentListState.collectAsState()
    val adminState by adminViewModel.currentAdminState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            adminState?.let { admin ->
                AdminProfileCard(
                    admin = admin,
                    onSaveClick = { uid, newDate ->
                        adminViewModel.updateAdminStartDate(uid, newDate)
                    }
                )
            }
        }
        item {
            when (val state = agentState) {
                is AdminViewModel.AgentListUiState.Loading -> CircularProgressIndicator()
                is AdminViewModel.AgentListUiState.Error -> Text(text = state.message)
                is AdminViewModel.AgentListUiState.Success -> {
                    AgentList(
                        agents = state.agents,
                        onApproveClick = { adminViewModel.approveAgent(it) },
                        onDeleteClick = { adminViewModel.deleteAgent(it) },
                        onSaveClick = { uid, date -> adminViewModel.updateAgentStartDate(uid, date) }
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsTab(adminViewModel: AdminViewModel) {
    val analyticsState by adminViewModel.analyticsUiState.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val state = analyticsState) {
            is AdminViewModel.AnalyticsUiState.Loading -> CircularProgressIndicator()
            is AdminViewModel.AnalyticsUiState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    item {
                        Text("Policy Count by Plan", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text("Plan Name", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("Count", fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(state.planCounts) { planCount ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(planCount.planName, Modifier.weight(1f))
                            Text(planCount.count.toString())
                        }
                        HorizontalDivider()
                    }
                    item {
                        Text("Policy Count by Financial Advisor", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text("Advisor Email", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("Policy Count", fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(state.agentPerformance) { agentData ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(agentData.agentEmail, Modifier.weight(1f))
                            Text(agentData.policyCount.toString())
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun AgentList(
    agents: List<User>,
    onApproveClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onSaveClick: (String, Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Pending Advisor Registrations", style = MaterialTheme.typography.titleLarge)
        if (agents.none { !it.isApproved }) {
            Text("No pending advisor registrations.")
        } else {
            agents.filter { !it.isApproved }.forEach { agent ->
                AgentCard(user = agent, onApproveClick = onApproveClick, onDeleteClick = {}, onSaveClick = { _, _ -> })
            }
        }

        Text("Registered Financial Advisors", style = MaterialTheme.typography.titleLarge)
        if (agents.none { it.isApproved }) {
            Text("No registered advisors found.")
        } else {
            agents.filter { it.isApproved }.forEach { agent ->
                AgentCard(user = agent, onApproveClick = {}, onDeleteClick = onDeleteClick, onSaveClick = onSaveClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileCard(admin: User, onSaveClick: (String, Long) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedStartDate by remember { mutableStateOf(admin.startDate) }
    val hasChanges = selectedStartDate != admin.startDate

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("My Profile", style = MaterialTheme.typography.titleLarge)
            Text("DO Code: ${admin.doCode}")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { showDatePicker = true }) {
                val dateText = selectedStartDate?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Set Appraisal Start Date"
                Text(dateText)
            }
            if (hasChanges) {
                Button(
                    onClick = { selectedStartDate?.let { onSaveClick(admin.uid, it) } },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedStartDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedStartDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentCard(
    user: User,
    onApproveClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onSaveClick: (String, Long) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedStartDate by remember { mutableStateOf(user.startDate) }
    val hasChanges = selectedStartDate != user.startDate

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!user.isApproved) {
                    Button(onClick = { onApproveClick(user.uid) }) { Text("Approve") }
                } else {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete Advisor", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (user.isApproved) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Advisor's Code: ${user.agencyCode.ifEmpty { "Not set" }}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { showDatePicker = true }) {
                    val dateText = selectedStartDate?.let {
                        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Set Start Date"
                    Text(dateText)
                }
                if (hasChanges) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { selectedStartDate?.let { onSaveClick(user.uid, it) } },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Advisor") },
            text = { Text("Are you sure you want to delete advisor ${user.email}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick(user.uid)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedStartDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedStartDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDateAnalysisTab(adminViewModel: AdminViewModel) {
    var analysisMode by remember { mutableStateOf("single") } // "single", "compare", or "period"

    // Default: today's date pre-selected so stats show immediately on tab open
    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var selectedDate1 by remember { mutableStateOf<Long?>(todayMidnight) }
    var selectedDate2 by remember { mutableStateOf<Long?>(null) }
    var periodStartDate1 by remember { mutableStateOf<Long?>(null) }
    var periodEndDate1 by remember { mutableStateOf<Long?>(null) }
    var periodStartDate2 by remember { mutableStateOf<Long?>(null) }
    var periodEndDate2 by remember { mutableStateOf<Long?>(null) }
    var showDatePicker1 by remember { mutableStateOf(false) }
    var showDatePicker2 by remember { mutableStateOf(false) }
    var showPeriodPicker1 by remember { mutableStateOf(false) }
    var showPeriodPicker2 by remember { mutableStateOf(false) }
    var showPeriod1StartPicker by remember { mutableStateOf(false) }
    var showPeriod1EndPicker by remember { mutableStateOf(false) }
    var showPeriod2StartPicker by remember { mutableStateOf(false) }
    var showPeriod2EndPicker by remember { mutableStateOf(false) }

    // Pre-compute current month boundaries for the default month comparison card
    val (currentMonthStart, currentMonthEnd) = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        start to cal.timeInMillis
    }
    val (lastYearMonthStart, lastYearMonthEnd) = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        start to cal.timeInMillis
    }

    val proposalsState by adminViewModel.proposalsUiState.collectAsState()
    val proposalsUiState = proposalsState as? AdminViewModel.ProposalsUiState
    val agentListState by adminViewModel.agentListState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selection
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = analysisMode == "single",
                    onClick = { analysisMode = "single" },
                    label = { Text("Single Day") }
                )
                FilterChip(
                    selected = analysisMode == "compare",
                    onClick = { analysisMode = "compare" },
                    label = { Text("Compare Dates") }
                )
                FilterChip(
                    selected = analysisMode == "period",
                    onClick = { analysisMode = "period" },
                    label = { Text("Compare Periods") }
                )
            }
        }

        // Date Selection
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Date Selection", style = MaterialTheme.typography.titleMedium)
                    
                    when (analysisMode) {
                        "single" -> {
                            OutlinedButton(
                                onClick = { showDatePicker1 = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DateRange, "Select Date", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(selectedDate1?.let { formatDate(it) } ?: "Select Date")
                            }
                        }
                        "compare" -> {
                            OutlinedButton(
                                onClick = { showDatePicker1 = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DateRange, "Select Date", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(selectedDate1?.let { formatDate(it) } ?: "Select Date 1")
                            }
                            OutlinedButton(
                                onClick = { showDatePicker2 = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DateRange, "Select Date 2", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(selectedDate2?.let { formatDate(it) } ?: "Select Date 2")
                            }
                        }
                        "period" -> {
                            Text("Period 1", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = { showPeriodPicker1 = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DateRange, "Period 1", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (periodStartDate1 != null && periodEndDate1 != null) {
                                        "${formatDate(periodStartDate1!!)} - ${formatDate(periodEndDate1!!)}"
                                    } else "Select Period 1"
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            Text("Period 2", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = { showPeriodPicker2 = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DateRange, "Period 2", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (periodStartDate2 != null && periodEndDate2 != null) {
                                        "${formatDate(periodStartDate2!!)} - ${formatDate(periodEndDate2!!)}"
                                    } else "Select Period 2"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Statistics Display
        when (analysisMode) {
            "single" -> {
                if (selectedDate1 != null && proposalsUiState != null) {
                    val allPolicies = proposalsUiState.allPolicies
                    val agents = when (val state = agentListState) {
                        is AdminViewModel.AgentListUiState.Success -> state.agents
                        else -> emptyList()
                    }
                    item {
                        AgentBreakdownCard(
                            title = "Agent Performance on ${formatDate(selectedDate1!!)}",
                            policies = allPolicies,
                            agents = agents,
                            dateMillis = selectedDate1!!
                        )
                    }
                    // Default month comparison: always show this month vs same month last year
                    item {
                        val currentMonthLabel = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            .format(java.util.Date(currentMonthStart))
                        val lastYearMonthLabel = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            .format(java.util.Date(lastYearMonthStart))
                        AgentPeriodComparisonCard(
                            period1Start = currentMonthLabel,
                            period1End = "",
                            period2Start = lastYearMonthLabel,
                            period2End = "",
                            policies = allPolicies,
                            agents = agents,
                            period1StartMillis = currentMonthStart,
                            period1EndMillis = currentMonthEnd,
                            period2StartMillis = lastYearMonthStart,
                            period2EndMillis = lastYearMonthEnd
                        )
                    }
                } else {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Select a date to view agent statistics",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            "compare" -> {
                if (selectedDate1 != null && selectedDate2 != null && proposalsUiState != null) {
                    val allPolicies = proposalsUiState.allPolicies
                    val agents = when (val state = agentListState) {
                        is AdminViewModel.AgentListUiState.Success -> state.agents
                        else -> emptyList()
                    }
                    item {
                        AgentComparisonCard(
                            date1 = formatDate(selectedDate1!!),
                            date2 = formatDate(selectedDate2!!),
                            policies = allPolicies,
                            agents = agents,
                            date1Millis = selectedDate1!!,
                            date2Millis = selectedDate2!!
                        )
                    }
                } else {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Select two dates to compare",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            "period" -> {
                if (periodStartDate1 != null && periodEndDate1 != null && 
                    periodStartDate2 != null && periodEndDate2 != null && proposalsUiState != null) {
                    val allPolicies = proposalsUiState.allPolicies
                    val agents = when (val state = agentListState) {
                        is AdminViewModel.AgentListUiState.Success -> state.agents
                        else -> emptyList()
                    }
                    item {
                        AgentPeriodComparisonCard(
                            period1Start = formatDate(periodStartDate1!!),
                            period1End = formatDate(periodEndDate1!!),
                            period2Start = formatDate(periodStartDate2!!),
                            period2End = formatDate(periodEndDate2!!),
                            policies = allPolicies,
                            agents = agents,
                            period1StartMillis = periodStartDate1!!,
                            period1EndMillis = periodEndDate1!!,
                            period2StartMillis = periodStartDate2!!,
                            period2EndMillis = periodEndDate2!!
                        )
                    }
                } else {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Select two periods to compare",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Date Pickers
    if (showDatePicker1) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker1 = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate1 = datePickerState.selectedDateMillis
                    showDatePicker1 = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker1 = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDatePicker2) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker2 = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate2 = datePickerState.selectedDateMillis
                    showDatePicker2 = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker2 = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Date Range Picker 1 (Period 1) - Using sequential dialogs
    if (showPeriodPicker1) {
        showPeriod1StartPicker = true
        showPeriodPicker1 = false
    }
    
    if (showPeriod1StartPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPeriod1StartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    periodStartDate1 = datePickerState.selectedDateMillis
                    showPeriod1StartPicker = false
                    showPeriod1EndPicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showPeriod1StartPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showPeriod1EndPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPeriod1EndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    periodEndDate1 = datePickerState.selectedDateMillis
                    showPeriod1EndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPeriod1EndPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Date Range Picker 2 (Period 2) - Using sequential dialogs
    if (showPeriodPicker2) {
        showPeriod2StartPicker = true
        showPeriodPicker2 = false
    }
    
    if (showPeriod2StartPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPeriod2StartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    periodStartDate2 = datePickerState.selectedDateMillis
                    showPeriod2StartPicker = false
                    showPeriod2EndPicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showPeriod2StartPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showPeriod2EndPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPeriod2EndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    periodEndDate2 = datePickerState.selectedDateMillis
                    showPeriod2EndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPeriod2EndPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun AgentBreakdownCard(
    title: String,
    policies: List<Policy>,
    agents: List<User>,
    dateMillis: Long
) {
    // First, filter policies for the selected date
    val policiesOnDate = policies.filter { policy ->
        val policyCal = Calendar.getInstance().apply { timeInMillis = policy.dateOfCompletion }
        val targetCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        policyCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
        policyCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH) &&
        policyCal.get(Calendar.DAY_OF_MONTH) == targetCal.get(Calendar.DAY_OF_MONTH)
    }

    // Group policies by agent code
    val agentStats = policiesOnDate.groupBy { it.agentCode }
        .map { (agentCode, agentPolicies) ->
            // Priority: 1) Registered user name, 2) agentName from policy, 3) agent code
            val agentName = agents.find { it.agencyCode == agentCode }?.name 
                ?: agentPolicies.firstOrNull()?.agentName?.takeIf { it.isNotBlank() } 
                ?: agentCode
            val stats = DateStats(
                policyCount = agentPolicies.size,
                totalPremium = agentPolicies.sumOf { it.premium.toLong() },
                anandaCount = agentPolicies.count { it.isAnanda }
            )
            Triple(agentName, agentCode, stats)
        }
        .sortedByDescending { it.third.policyCount } // Sort by policy count

    val totalStats = calculateDateStats(policies, dateMillis)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            if (agentStats.isEmpty()) {
                Text(
                    "No policies found on this date",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Header
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Agent", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Policies", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Premium", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                HorizontalDivider()

                // Agent rows
                agentStats.forEach { (name, code, stats) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.weight(2f)) {
                            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(code, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${stats.policyCount}", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text("₹${formatPremium(stats.totalPremium)}", modifier = Modifier.weight(1.5f), fontSize = 13.sp)
                    }
                }

                HorizontalDivider()

                // Total row
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("TOTAL", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${totalStats.policyCount}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("₹${formatPremium(totalStats.totalPremium)}", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AgentComparisonCard(
    date1: String,
    date2: String,
    policies: List<Policy>,
    agents: List<User>,
    date1Millis: Long,
    date2Millis: Long
) {
    // Get policies for both dates
    val policiesDate1 = policies.filter { policy ->
        val policyCal = Calendar.getInstance().apply { timeInMillis = policy.dateOfCompletion }
        val targetCal = Calendar.getInstance().apply { timeInMillis = date1Millis }
        policyCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
        policyCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH) &&
        policyCal.get(Calendar.DAY_OF_MONTH) == targetCal.get(Calendar.DAY_OF_MONTH)
    }
    
    val policiesDate2 = policies.filter { policy ->
        val policyCal = Calendar.getInstance().apply { timeInMillis = policy.dateOfCompletion }
        val targetCal = Calendar.getInstance().apply { timeInMillis = date2Millis }
        policyCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
        policyCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH) &&
        policyCal.get(Calendar.DAY_OF_MONTH) == targetCal.get(Calendar.DAY_OF_MONTH)
    }

    // Get all unique agent codes from both dates
    val allAgentCodes = (policiesDate1.map { it.agentCode } + policiesDate2.map { it.agentCode }).distinct()

    val agentComparisons = allAgentCodes.map { agentCode ->
        // Priority: 1) Registered user name, 2) agentName from policy, 3) agent code
        val agentName = agents.find { it.agencyCode == agentCode }?.name
            ?: (policiesDate1 + policiesDate2).find { it.agentCode == agentCode }?.agentName?.takeIf { it.isNotBlank() }
            ?: agentCode
        val stats1 = DateStats(
            policyCount = policiesDate1.count { it.agentCode == agentCode },
            totalPremium = policiesDate1.filter { it.agentCode == agentCode }.sumOf { it.premium.toLong() },
            anandaCount = policiesDate1.count { it.agentCode == agentCode && it.isAnanda }
        )
        val stats2 = DateStats(
            policyCount = policiesDate2.count { it.agentCode == agentCode },
            totalPremium = policiesDate2.filter { it.agentCode == agentCode }.sumOf { it.premium.toLong() },
            anandaCount = policiesDate2.count { it.agentCode == agentCode && it.isAnanda }
        )
        Triple(agentName, stats1, stats2)
    }.sortedByDescending { it.second.policyCount + it.third.policyCount }

    val totalStats1 = calculateDateStats(policies, date1Millis)
    val totalStats2 = calculateDateStats(policies, date2Millis)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Agent Comparison: $date1 vs $date2", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            if (agentComparisons.isEmpty()) {
                Text(
                    "No policies found on these dates",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Header
                Row(modifier = Modifier.fillMaxWidth().padding(vertical =4.dp)) {
                    Text("Agent", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(date1.substring(0, 6), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(date2.substring(0, 6), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Δ", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                HorizontalDivider()

                // Agent comparison rows
                agentComparisons.forEach { (name, stats1, stats2) ->
                    val diff = stats1.policyCount - stats2.policyCount
                    val diffColor = when {
                        diff > 0 -> Color(0xFF4CAF50)
                        diff < 0 -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, modifier = Modifier.weight(2f), fontSize = 12.sp)
                        Text("${stats1.policyCount}", modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text("${stats2.policyCount}", modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text(
                            when {
                                diff > 0 -> "+$diff↑"
                                diff < 0 -> "$diff↓"
                                else -> "—"
                            },
                            modifier = Modifier.weight(0.7f),
                            color = diffColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider()

                // Total comparison row
                val totalDiff = totalStats1.policyCount - totalStats2.policyCount
                val totalDiffColor = when {
                    totalDiff > 0 -> Color(0xFF4CAF50)
                    totalDiff < 0 -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${totalStats1.policyCount}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${totalStats2.policyCount}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        when {
                            totalDiff > 0 -> "+$totalDiff↑"
                            totalDiff < 0 -> "$totalDiff↓"
                            else -> "—"
                        },
                        modifier = Modifier.weight(0.7f),
                        color = totalDiffColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable  
fun AgentPeriodComparisonCard(
    period1Start: String,
    period1End: String,
    period2Start: String,
    period2End: String,
    policies: List<Policy>,
    agents: List<User>,
    period1StartMillis: Long,
    period1EndMillis: Long,
    period2StartMillis: Long,
    period2EndMillis: Long
) {
    // Get policies for both periods
    val policiesPeriod1 = policies.filter { policy ->
        policy.dateOfCompletion >= period1StartMillis && policy.dateOfCompletion <= period1EndMillis
    }
    
    val policiesPeriod2 = policies.filter { policy ->
        policy.dateOfCompletion >= period2StartMillis && policy.dateOfCompletion <= period2EndMillis
    }

    // Get all unique agent codes from both periods
    val allAgentCodes = (policiesPeriod1.map { it.agentCode } + policiesPeriod2.map { it.agentCode }).distinct()

    val agentComparisons = allAgentCodes.map { agentCode ->
        val agentName = agents.find { it.agencyCode == agentCode }?.name
            ?: (policiesPeriod1 + policiesPeriod2).find { it.agentCode == agentCode }?.agentName?.takeIf { it.isNotBlank() }
            ?: agentCode
        val stats1 = DateStats(
            policyCount = policiesPeriod1.count { it.agentCode == agentCode },
            totalPremium = policiesPeriod1.filter { it.agentCode == agentCode }.sumOf { it.premium.toLong() },
            anandaCount = policiesPeriod1.count { it.agentCode == agentCode && it.isAnanda }
        )
        val stats2 = DateStats(
            policyCount = policiesPeriod2.count { it.agentCode == agentCode },
            totalPremium = policiesPeriod2.filter { it.agentCode == agentCode }.sumOf { it.premium.toLong() },
            anandaCount = policiesPeriod2.count {  it.agentCode == agentCode && it.isAnanda }
        )
        Triple(agentName, stats1, stats2)
    }.sortedByDescending { it.second.policyCount + it.third.policyCount }

    val totalStats1 = DateStats(
        policyCount = policiesPeriod1.size,
        totalPremium = policiesPeriod1.sumOf { it.premium.toLong() },
        anandaCount = policiesPeriod1.count { it.isAnanda }
    )
    val totalStats2 = DateStats(
        policyCount = policiesPeriod2.size,
        totalPremium = policiesPeriod2.sumOf { it.premium.toLong() },
        anandaCount = policiesPeriod2.count { it.isAnanda }
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Period Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$period1Start - $period1End vs $period2Start - $period2End", style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()

            if (agentComparisons.isEmpty()) {
                Text(
                    "No policies found in these periods",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Header
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Agent", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Period 1", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Period 2", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Δ", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                HorizontalDivider()

                // Total row first
                val totalPolicyDiff = totalStats1.policyCount - totalStats2.policyCount
                val totalDiffColor = when {
                    totalPolicyDiff > 0 -> Color(0xFF4CAF50)
                    totalPolicyDiff < 0 -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("TOTAL", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        "${totalStats1.policyCount} / ₹${formatPremium(totalStats1.totalPremium)}",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        "${totalStats2.policyCount} / ₹${formatPremium(totalStats2.totalPremium)}",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        when {
                            totalPolicyDiff > 0 -> "+$totalPolicyDiff↑"
                            totalPolicyDiff < 0 -> "$totalPolicyDiff↓"
                            else -> "—"
                        },
                        modifier = Modifier.weight(0.7f),
                        color = totalDiffColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider()

                // Agent rows
                agentComparisons.forEach { (agentName, stats1, stats2) ->
                    val policyDiff = stats1.policyCount - stats2.policyCount
                    val policyDiffColor = when {
                        policyDiff > 0 -> Color(0xFF4CAF50)
                        policyDiff < 0 -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(agentName, modifier = Modifier.weight(1.5f), fontSize = 10.sp, maxLines = 1)
                        Text("${stats1.policyCount} / ₹${formatPremium(stats1.totalPremium)}", modifier = Modifier.weight(1f), fontSize = 9.sp)
                        Text("${stats2.policyCount} / ₹${formatPremium(stats2.totalPremium)}", modifier = Modifier.weight(1f), fontSize = 9.sp)
                        Text(
                            when {
                                policyDiff > 0 -> "+$policyDiff↑"
                                policyDiff < 0 -> "$policyDiff↓"
                                else -> "—"
                            },
                            modifier = Modifier.weight(0.7f),
                            color = policyDiffColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
