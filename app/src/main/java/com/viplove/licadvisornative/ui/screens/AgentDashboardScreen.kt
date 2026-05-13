package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.model.ClientDataSheet
import com.viplove.licadvisornative.model.Policy // <-- Uses the main Policy model
import com.viplove.licadvisornative.ui.viewmodel.AgentViewModel
import com.viplove.licadvisornative.util.LocaleManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDashboardScreen(
    navController: NavController,
    agentViewModel: AgentViewModel = viewModel()
) {
    val tabs = listOf(
        stringResource(R.string.my_policies_tab),
        "ULIP",
        "Data Analysis",
        stringResource(R.string.forms_tab),  // NEW: Forms tab at position 3
        stringResource(R.string.drafts_tab),
        stringResource(R.string.checker_tab),
        stringResource(R.string.graphics_tab)
    )
    var selectedTabIndex by remember { mutableStateOf(0) }
    val uiState by agentViewModel.uiState.collectAsState()
    val agentUiState = uiState as? AgentViewModel.AgentUiState

    var showProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.agent_dashboard_title)) },
                actions = {
                    IconButton(onClick = { agentViewModel.refreshData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Data")
                    }
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "My Profile")
                    }
                    IconButton(onClick = {
                        agentViewModel.logout()
                        navController.navigate("login") { popUpTo(0) }
                    }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("data_collection/new") }) {
                Icon(Icons.Default.Add, contentDescription = "Add New Client")
            }
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
            when (selectedTabIndex) {
                0 -> AgentPoliciesTab(agentViewModel, agentUiState)

                1 -> {
                    // Filter the main list to get only ULIP policies
                    val ulipPolicies = agentUiState?.filteredPolicies?.filter { it.isUlip } ?: emptyList()
                    UlipScreen(policies = ulipPolicies)
                }

                2 -> if (agentUiState != null) {
                    AgentDateAnalysisTab(agentUiState)
                }

                3 -> {
                    val currentUser = agentUiState?.currentUser
                    FormsScreen(
                        userRole = "advisor",
                        userEmail = currentUser?.email ?: "",
                        userName = currentUser?.name ?: ""
                    )
                }

                4 -> if (agentUiState != null) {
                    DraftsTab(navController, agentViewModel, agentUiState)
                }
                5 -> NonMedicalCheckerScreen()
                6 -> GraphicsSelectionScreen(navController = navController)
            }
        }
    }

    if (showProfileDialog) {
        val context = LocalContext.current
        val languages = listOf(stringResource(R.string.language_english), stringResource(R.string.language_hindi))
        val currentLocale = context.resources.configuration.locales[0]
        val initialLanguage = if (currentLocale.language == "hi") languages[1] else languages[0]
        var selectedLanguage by remember { mutableStateOf(initialLanguage) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(stringResource(R.string.my_profile_title)) },
            text = {
                agentUiState?.currentUser?.let { user ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(label = stringResource(R.string.email_label), value = user.email)
                        InfoRow(label = stringResource(R.string.agency_code_label), value = user.agencyCode)
                        val startDate = user.startDate?.let {
                            SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(it))
                        } ?: "Not set by DO"
                        InfoRow(label = stringResource(R.string.start_date_label), value = startDate)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(stringResource(R.string.select_language), fontWeight = FontWeight.Bold)

                        languages.forEach { language ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (language == selectedLanguage),
                                        onClick = {
                                            if (selectedLanguage != language) {
                                                selectedLanguage = language
                                                val langCode = if (language == languages[0]) "en" else "hi"
                                                LocaleManager.updateAppLocale(context, langCode)
                                            }
                                        }
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (language == selectedLanguage),
                                    onClick = null
                                )
                                Text(text = language, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text(stringResource(R.string.close_button)) }
            }
        )
    }
}

// --- ADDED: InfoRow composable was missing ---
@Composable
fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            modifier = Modifier.width(120.dp),
            fontWeight = FontWeight.Bold
        )
        Text(text = value)
    }
}


@Composable
fun DraftsTab(
    navController: NavController,
    agentViewModel: AgentViewModel,
    uiState: AgentViewModel.AgentUiState
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf<ClientDataSheet?>(null) }
    var showDownloadDialog by remember { mutableStateOf<ClientDataSheet?>(null) }
    var showArchiveDialog by remember { mutableStateOf<ClientDataSheet?>(null) }

    // --- ADDED: Focus Manager ---
    val focusManager = LocalFocusManager.current

    val filteredDrafts = uiState.filteredDrafts
    val filteredSubmitted = uiState.filteredSubmitted
    val filteredArchived = uiState.filteredArchived

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            OutlinedTextField(
                value = uiState.draftsSearchQuery,
                onValueChange = { agentViewModel.onDraftsSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by Lead Name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                // --- ADDED: Keyboard actions ---
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }

        item {
            Text("Saved Drafts", style = MaterialTheme.typography.headlineSmall)
            Text("Continue filling out these forms.", style = MaterialTheme.typography.bodyMedium)
        }
        if (filteredDrafts.isEmpty()) {
            item { Text("No drafts found.", modifier = Modifier.padding(vertical = 8.dp)) }
        } else {
            items(filteredDrafts) { draft ->
                DraftCard(
                    draft = draft,
                    onClick = { navController.navigate("data_collection/${draft.id}") },
                    onDeleteClick = { showDeleteDialog = draft }
                )
            }
        }

        item {
            Text("Submitted Forms", style = MaterialTheme.typography.headlineSmall)
            Text("These forms are complete and ready for review.", style = MaterialTheme.typography.bodyMedium)
        }

        if (filteredSubmitted.isEmpty()) {
            item { Text("No submitted forms found.", modifier = Modifier.padding(vertical = 8.dp)) }
        } else {
            items(filteredSubmitted) { form ->
                SubmittedFormCard(
                    form = form,
                    onViewClick = { navController.navigate("review_screen/${form.id}") },
                    onArchiveClick = { showArchiveDialog = form },
                    onDownloadClick = { showDownloadDialog = form }
                )
            }
        }

        if (filteredArchived.isNotEmpty()) {
            item {
                Text("Recently Archived", style = MaterialTheme.typography.headlineSmall)
                Text("These forms can be restored for up to 7 days.", style = MaterialTheme.typography.bodyMedium)
            }
            items(filteredArchived) { form ->
                ArchivedFormCard(
                    form = form,
                    onRestoreClick = { agentViewModel.restoreForm(form.id) }
                )
            }
        }
    }

    if (showDeleteDialog != null) {
        val draftToDelete = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Draft?") },
            text = { Text("Are you sure you want to permanently delete this draft? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        agentViewModel.deleteDraft(draftToDelete.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showDownloadDialog != null) {
        val formToDownload = showDownloadDialog!!
        DownloadOptionsDialog(
            formName = formToDownload.proposerDetails.name,
            onDismiss = { showDownloadDialog = null },
            onDownloadCombined = {
                agentViewModel.downloadFormAsCombinedPdf(formToDownload, context)
                showDownloadDialog = null
            },
            onDownloadSeparate = {
                agentViewModel.downloadFormAsSeparatePdfs(formToDownload, context)
                showDownloadDialog = null
            }
        )
    }

    if (showArchiveDialog != null) {
        val formToArchive = showArchiveDialog!!
        AlertDialog(
            onDismissRequest = { showArchiveDialog = null },
            title = { Text("Archive Form?") },
            text = { Text("Are you sure you want to archive this form? It can be restored later.") },
            confirmButton = {
                Button(onClick = {
                    agentViewModel.archiveForm(formToArchive.id)
                    showArchiveDialog = null
                }) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DownloadOptionsDialog(
    formName: String,
    onDismiss: () -> Unit,
    onDownloadCombined: () -> Unit,
    onDownloadSeparate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Options") },
        text = { Text("How would you like to download the files for $formName?") },
        confirmButton = {
            Column {
                Button(onClick = onDownloadSeparate, modifier = Modifier.fillMaxWidth()) {
                    Text("Download Separate PDFs")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDownloadCombined, modifier = Modifier.fillMaxWidth()) {
                    Text("Download Combined PDF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DraftCard(draft: ClientDataSheet, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(draft.proposerDetails.name.ifEmpty { "New Client" }, style = MaterialTheme.typography.titleLarge)
                val date = draft.lastUpdated?.let { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(it) } ?: "Not yet saved"
                Text("Last updated: $date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Draft", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SubmittedFormCard(form: ClientDataSheet, onViewClick: () -> Unit, onArchiveClick: () -> Unit, onDownloadClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onViewClick), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(form.proposerDetails.name.ifEmpty { "New Client" }, style = MaterialTheme.typography.titleLarge)
            val date = form.lastUpdated?.let { SimpleDateFormat("dd MMM yyyy", Locale.US).format(it) } ?: "Unknown"
            Text("Submitted on: $date", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDownloadClick) {
                    Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(18.dp))
                }
                OutlinedButton(onClick = onArchiveClick) {
                    Icon(Icons.Default.Archive, contentDescription = "Archive", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ArchivedFormCard(form: ClientDataSheet, onRestoreClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(form.proposerDetails.name.ifEmpty { "New Client" }, style = MaterialTheme.typography.titleMedium)
                Text("Archived", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onRestoreClick) {
                Icon(Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restore")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentPoliciesTab(
    agentViewModel: AgentViewModel,
    uiState: AgentViewModel.AgentUiState?
) {
    val showDatePicker = remember { mutableStateOf(false) }

    // --- ADDED: Focus Manager ---
    val focusManager = LocalFocusManager.current
    
    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (uiState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                agentViewModel.refreshData()
                kotlinx.coroutines.delay(1000)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { agentViewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_by_name_policy_plan)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                // --- ADDED: Keyboard actions ---
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }

        // --- NEW: Late Policy Filter Checkbox ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.showOnlyLate,
                    onCheckedChange = { agentViewModel.onLateFilterToggled(it) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Show Only Late Policies (>30 days overdue)",
                    modifier = Modifier.clickable { agentViewModel.onLateFilterToggled(!uiState.showOnlyLate) }
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown(
                    label = stringResource(R.string.plan_filter),
                    options = uiState.availablePlans,
                    selectedOption = uiState.selectedPlan,
                    onOptionSelected = { agentViewModel.onPlanSelected(it) },
                    modifier = Modifier.weight(1f)
                )
                FilterDropdown(
                    label = stringResource(R.string.mode_filter),
                    options = uiState.availableModes,
                    selectedOption = uiState.selectedMode,
                    onOptionSelected = { agentViewModel.onModeSelected(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown(
                    label = stringResource(R.string.financial_year_filter),
                    options = uiState.availableFinancialYears,
                    selectedOption = uiState.selectedFinancialYear,
                    onOptionSelected = { agentViewModel.onFinancialYearSelected(it) },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.startDate == null
                )
                FilterDropdown(
                    label = stringResource(R.string.appraisal_year_filter),
                    options = uiState.availableAppraisalYears,
                    selectedOption = uiState.selectedAppraisalYear,
                    onOptionSelected = { agentViewModel.onAppraisalYearSelected(it) },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.startDate == null
                )
            }
        }
        item {
            OutlinedButton(
                onClick = { showDatePicker.value = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedFinancialYear == "None" && uiState.selectedAppraisalYear == "None"
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "Select Date Range")
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = formatDateRange(uiState.startDate, uiState.endDate))
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatCard(stringResource(R.string.total_policies_stat), uiState.filteredPolicies.size.toString())
                StatCard(stringResource(R.string.ananda_count_stat), uiState.filteredPolicies.count { it.isAnanda }.toString())
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        if (uiState.filteredPolicies.isEmpty() && !uiState.isLoading) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No policies found for the selected filters.")
                }
            }
        } else {
            items(uiState.filteredPolicies) { policy ->
                DetailedPolicyCard(policy = policy)
            }
        }
        }
    }

    if (showDatePicker.value) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker.value = false
                    agentViewModel.onDateRangeSelected(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker.value = false }) { Text("Cancel") } }
        ) {
            DateRangePicker(state = dateRangePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDateAnalysisTab(agentUiState: AgentViewModel.AgentUiState?) {
    var analysisMode by remember { mutableStateOf("single") } // "single", "compare", or "period"

    // Default: today pre-selected so stats show immediately
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

    // Pre-compute month boundaries for auto-loaded month comparison
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

    if (agentUiState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

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
                if (selectedDate1 != null) {
                    item {
                        val stats1 = calculateDateStats(agentUiState.allPolicies, selectedDate1!!)
                        DateStatsCard("Your Performance on ${formatDate(selectedDate1!!)}", stats1)
                    }
                    // Auto month comparison: this month vs same month last year
                    item {
                        val thisMonthStats = calculatePeriodStats(agentUiState.allPolicies, currentMonthStart, currentMonthEnd)
                        val lastYearMonthStats = calculatePeriodStats(agentUiState.allPolicies, lastYearMonthStart, lastYearMonthEnd)
                        val currentMonthLabel = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(java.util.Date(currentMonthStart))
                        val lastYearMonthLabel = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(java.util.Date(lastYearMonthStart))
                        ComparisonStatsCard(
                            date1 = currentMonthLabel,
                            date2 = lastYearMonthLabel,
                            stats1 = thisMonthStats,
                            stats2 = lastYearMonthStats
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
                                    "Select a date to view statistics",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            "compare" -> {
                if (selectedDate1 != null && selectedDate2 != null) {
                    item {
                        val stats1 = calculateDateStats(agentUiState.allPolicies, selectedDate1!!)
                        val stats2 = calculateDateStats(agentUiState.allPolicies, selectedDate2!!)
                        ComparisonStatsCard(
                            date1 = formatDate(selectedDate1!!),
                            date2 = formatDate(selectedDate2!!),
                            stats1 = stats1,
                            stats2 = stats2
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
                    periodStartDate2 != null && periodEndDate2 != null) {
                    item {
                        val stats1 = calculatePeriodStats(agentUiState.allPolicies, periodStartDate1!!, periodEndDate1!!)
                        val stats2 = calculatePeriodStats(agentUiState.allPolicies, periodStartDate2!!, periodEndDate2!!)
                        ComparisonStatsCard(
                            date1 = "${formatDate(periodStartDate1!!)} - ${formatDate(periodEndDate1!!)}",
                            date2 = "${formatDate(periodStartDate2!!)} - ${formatDate(periodEndDate2!!)}",
                            stats1 = stats1,
                            stats2 = stats2
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

    // Date Picker 1
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

    // Date Picker 2
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

// Helper data class for statistics
data class DateStats(
    val policyCount: Int,
    val totalPremium: Long,
    val anandaCount: Int
)

// Calculate statistics for policies registered on a specific date
fun calculateDateStats(policies: List<Policy>, dateMillis: Long): DateStats {
    val targetCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val targetYear = targetCal.get(Calendar.YEAR)
    val targetMonth = targetCal.get(Calendar.MONTH)
    val targetDay = targetCal.get(Calendar.DAY_OF_MONTH)

    val policiesOnDate = policies.filter { policy ->
        val policyCal = Calendar.getInstance().apply { timeInMillis = policy.dateOfCompletion }
        policyCal.get(Calendar.YEAR) == targetYear &&
        policyCal.get(Calendar.MONTH) == targetMonth &&
        policyCal.get(Calendar.DAY_OF_MONTH) == targetDay
    }

    return DateStats(
        policyCount = policiesOnDate.size,
        totalPremium = policiesOnDate.sumOf { it.premium.toLong() },
        anandaCount = policiesOnDate.count { it.isAnanda }
    )
}

// Calculate statistics for policies in a date range
fun calculatePeriodStats(policies: List<Policy>, startMillis: Long, endMillis: Long): DateStats {
    val policiesInPeriod = policies.filter { policy ->
        policy.dateOfCompletion >= startMillis && policy.dateOfCompletion <= endMillis
    }

    return DateStats(
        policyCount = policiesInPeriod.size,
        totalPremium = policiesInPeriod.sumOf { it.premium.toLong() },
        anandaCount = policiesInPeriod.count { it.isAnanda }
    )
}



@Composable
fun DateStatsCard(title: String, stats: DateStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            
            StatRow("Policies:", stats.policyCount.toString())
            StatRow("Total Premium:", "₹${formatPremium(stats.totalPremium)}")
            StatRow("ANANDA Count:", stats.anandaCount.toString())
        }
    }
}

@Composable
fun ComparisonStatsCard(date1: String, date2: String, stats1: DateStats, stats2: DateStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Comparison: $date1 vs $date2", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            // Table Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                Text(date1, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(date2, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Δ", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            HorizontalDivider()

            // Policies Row
            ComparisonRow("Policies", stats1.policyCount, stats2.policyCount)
            
            // Premium Row
            ComparisonRow("Premium", stats1.totalPremium, stats2.totalPremium, isRupees = true)
            
            // ANANDA Row
            ComparisonRow("ANANDA", stats1.anandaCount, stats2.anandaCount)
        }
    }
}

@Composable
fun ComparisonRow(label: String, value1: Number, value2: Number, isRupees: Boolean = false) {
    val diff = value1.toLong() - value2.toLong()
    val diffText = when {
        diff > 0 -> "+${if (isRupees) formatPremium(diff) else diff} ↑"
        diff < 0 -> "${if (isRupees) formatPremium(diff) else diff} ↓"
        else -> "—"
    }
    val diffColor = when {
        diff > 0 -> Color(0xFF4CAF50) // Green
        diff < 0 -> Color(0xFFF44336) // Red
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1.5f))
        Text(
            if (isRupees) "₹${formatPremium(value1.toLong())}" else value1.toString(),
            modifier = Modifier.weight(1f),
            fontSize = 12.sp
        )
        Text(
            if (isRupees) "₹${formatPremium(value2.toLong())}" else value2.toString(),
            modifier = Modifier.weight(1f),
            fontSize = 12.sp
        )
        Text(
            diffText,
            modifier = Modifier.weight(0.8f),
            color = diffColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

fun formatPremium(premium: Long): String {
    return when {
        premium >= 10000000 -> "%.2f Cr".format(premium / 10000000.0)
        premium >= 100000 -> "%.2f L".format(premium / 100000.0)
        premium >= 1000 -> "%.2f K".format(premium / 1000.0)
        else -> premium.toString()
    }
}
