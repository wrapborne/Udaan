package com.viplove.licadvisornative.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.model.NavData
import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.model.UlipProjectionResult
import com.viplove.licadvisornative.ui.viewmodel.UlipViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UlipScreen(
    policies: List<Policy>,
    ulipViewModel: UlipViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Policy Status", "NAV", "Fund Value Calculator", "Maturity Calculator")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 16.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> UlipExistingPoliciesTab(policies = policies, ulipViewModel = ulipViewModel)
            1 -> NavTab(ulipViewModel = ulipViewModel)
            2 -> UlipFundValueCalculatorTab()
            3 -> UlipMaturityCalculatorTab(ulipViewModel = ulipViewModel)
        }
    }
}

// --- THIS TAB IS UPDATED ---
@Composable
fun NavTab(ulipViewModel: UlipViewModel) {
    val uiState by ulipViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // --- MODIFIED: Call initializeCache ---
    // This triggers the "fetch-once-a-day" logic
    LaunchedEffect(key1 = true) {
        ulipViewModel.initializeCache(context)
    }

    // --- State for the filter ---
    val allPlanNames = remember(uiState.allNavs) {
        uiState.allNavs.map { it.planName }.distinct().sorted()
    }

    val selectedPlans = uiState.selectedPlans
    var showFilterDialog by remember { mutableStateOf(false) }

    // --- Group NAVs by Plan Name ---
    val groupedNavs = remember(uiState.allNavs) {
        uiState.allNavs.groupBy { it.planName }
    }

    // --- Apply the filter ---
    val filteredNavs = remember(groupedNavs, selectedPlans) {
        groupedNavs.filterKeys { it in selectedPlans }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- MODIFIED: Show loader if cache is not initialized ---
        if (!uiState.cacheInitialized && uiState.isFetchingAllNavs) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.navListError != null) {
            Text(
                text = uiState.navListError!!,
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Live Fund NAVs", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                // Show "Refreshing..." if it's fetching, otherwise show "Cached"
                                if (uiState.isFetchingAllNavs && uiState.cacheInitialized) "Refreshing data in background..."
                                else "Data cached for today.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter Plans")
                        }
                    }
                }

                items(filteredNavs.entries.toList(), key = { it.key }) { (planName, funds) ->
                    CollapsibleNavGroupCard(planName = planName, funds = funds)
                }
            }
        }
    }

    if (showFilterDialog) {
        NavFilterDialog(
            allPlanNames = allPlanNames,
            selectedPlans = selectedPlans,
            onDismiss = { showFilterDialog = false },
            onSelectionChanged = { newSelectedSet ->
                ulipViewModel.onSelectedPlansChanged(context, newSelectedSet)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavFilterDialog(
    allPlanNames: List<String>,
    selectedPlans: Set<String>,
    onDismiss: () -> Unit,
    onSelectionChanged: (Set<String>) -> Unit
) {
    var tempSelection by remember { mutableStateOf(selectedPlans) }
    val isAllSelected = tempSelection.size == allPlanNames.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Plans") },
        text = {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = isAllSelected,
                            onValueChange = {
                                tempSelection = if (isAllSelected) emptySet() else allPlanNames.toSet()
                            },
                            role = Role.Checkbox
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isAllSelected, onCheckedChange = null)
                    Text(text = if (isAllSelected) "Select None" else "Select All", modifier = Modifier.padding(start = 16.dp))
                }
                HorizontalDivider()

                LazyColumn {
                    items(allPlanNames) { planName ->
                        val isSelected = planName in tempSelection
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = isSelected,
                                    onValueChange = {
                                        tempSelection = if (isSelected) {
                                            tempSelection - planName
                                        } else {
                                            tempSelection + planName
                                        }
                                    },
                                    role = Role.Checkbox
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Text(text = planName, modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelectionChanged(tempSelection)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CollapsibleNavGroupCard(planName: String, funds: List<NavData>) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = planName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "ArrowRotation")
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    funds.forEach { navData ->
                        FundDataRow(navData = navData)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun FundDataRow(navData: NavData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = navData.fundName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = navData.sfin,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Launch Date: ${navData.launchDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "₹${navData.nav}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
    }
}


@Composable
fun UlipFundValueCalculatorTab() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Coming Soon!", style = MaterialTheme.typography.headlineMedium)
        Text(
            "This feature will be enabled once the method for retrieving the number of units for an existing policy is finalized.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}


// --- THIS TAB IS UPDATED ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UlipMaturityCalculatorTab(ulipViewModel: UlipViewModel) {
    val uiState by ulipViewModel.uiState.collectAsState()
    val context = LocalContext.current // <-- Get context

    // --- ADD: Initialize cache here as well ---
    // This ensures data is loaded if user opens this tab first
    LaunchedEffect(key1 = true) {
        ulipViewModel.initializeCache(context)
    }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val numberFormat = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 } }

    var expandedPlan by remember { mutableStateOf(false) }
    var expandedFund by remember { mutableStateOf(false) }
    var expandedMode by remember { mutableStateOf(false) }
    var expandedRate by remember { mutableStateOf(false) }
    var expandedSa by remember { mutableStateOf(false) }

    // Logic for SA multiplier dropdown
    val age = uiState.projectionAge.toIntOrNull() ?: 0
    val saOptions = if (age <= 50) listOf(7, 10) else listOf(7)
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("New Policy Maturity Projection", style = MaterialTheme.typography.headlineSmall) }

        item {
            ExposedDropdownMenuBox(expanded = expandedPlan, onExpandedChange = { expandedPlan = !expandedPlan }) {
                OutlinedTextField(
                    value = uiState.selectedPlan?.planName ?: "Select a Plan",
                    onValueChange = {}, readOnly = true, label = { Text("Plan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPlan) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expandedPlan, onDismissRequest = { expandedPlan = false }) {
                    uiState.availablePlans.forEach { plan ->
                        DropdownMenuItem(text = { Text(plan.planName) }, onClick = {
                            ulipViewModel.onSelectedPlanChanged(plan)
                            expandedPlan = false
                        })
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.projectionAge,
                    onValueChange = { ulipViewModel.onProjectionAgeChanged(it) },
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) }),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.projectionTerm,
                    onValueChange = { ulipViewModel.onProjectionTermChanged(it) },
                    label = { Text("Term (Yrs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expandedMode, onExpandedChange = { expandedMode = !expandedMode }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.projectionMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMode) },
                        modifier = Modifier.menuAnchor(),
                        enabled = uiState.isModeSelectionEnabled // <-- This will disable the dropdown for Single Premium
                    )
                    ExposedDropdownMenu(expanded = expandedMode, onDismissRequest = { expandedMode = false }) {
                        listOf("YLY", "HLY", "QLY", "MLY").forEach { mode ->
                            DropdownMenuItem(text = { Text(mode) }, onClick = {
                                ulipViewModel.onProjectionModeChanged(mode)
                                expandedMode = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = uiState.projectionPremium,
                    onValueChange = { ulipViewModel.onProjectionPremiumChanged(it) },
                    label = { Text(if (uiState.isModeSelectionEnabled) "Premium" else "Single Premium") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expandedSa, onExpandedChange = { expandedSa = !expandedSa }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = "${uiState.saMultiplier}x Premium", onValueChange = {}, readOnly = true, label = { Text("Sum Assured") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSa) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = expandedSa, onDismissRequest = { expandedSa = false }) {
                        saOptions.forEach { sa -> DropdownMenuItem(text = { Text("${sa}x Annual Premium") }, onClick = { ulipViewModel.onSaMultiplierChanged(sa); expandedSa = false }) }
                    }
                }
                OutlinedTextField(value = numberFormat.format(uiState.sumAssured), onValueChange = { /* no-op */}, label = { Text("Calculated SA") }, readOnly = true, enabled = false, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expandedFund, onExpandedChange = { expandedFund = !expandedFund },  modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = uiState.selectedFundName, onValueChange = {}, readOnly = true, label = { Text("Fund Choice") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFund) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = expandedFund, onDismissRequest = { expandedFund = false }) {
                        uiState.selectedPlan?.fundOptions?.forEach { fund -> DropdownMenuItem(text = { Text(fund.name) }, onClick = { ulipViewModel.onSelectedFundChanged(fund.name); expandedFund = false }) }
                    }
                }

                // --- MODIFIED: This dropdown is now dynamic ---
                ExposedDropdownMenuBox(expanded = expandedRate, onExpandedChange = { expandedRate = !expandedRate },  modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.selectedCagrLabel, // Use the label
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("CAGR") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRate) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedRate, onDismissRequest = { expandedRate = false }) {
                        // Read from the dynamic list in the UiState
                        uiState.cagrOptions.forEach { (label, rateValue) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    ulipViewModel.onProjectionRateChanged(label, rateValue) // Pass both
                                    expandedRate = false
                                }
                            )
                        }
                    }
                }
                // --- END OF MODIFICATION ---
            }
        }

        item {
            Button(onClick = { ulipViewModel.calculateMaturityProjection() }, modifier = Modifier.fillMaxWidth()) { Text("Calculate Projection") }
        }

        if (uiState.projectionResult != null) {
            item {
                Text("Projected Maturity: ${currencyFormat.format(uiState.finalMaturityValue)}", style = MaterialTheme.typography.titleLarge)
                Text("Net Yield: ${numberFormat.format(uiState.finalNetYield)}%", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(Modifier.fillMaxWidth()) {
                    ProjectionTableCell(text = "Yr", weight = 0.1f, isHeader = true)
                    ProjectionTableCell(text = "Premium", weight = 0.25f, isHeader = true)
                    ProjectionTableCell(text = "Charges", weight = 0.25f, isHeader = true)
                    ProjectionTableCell(text = "Fund Value", weight = 0.4f, isHeader = true)
                }
            }
            items(uiState.projectionResult!!) { result ->
                ProjectionResultRow(result = result)
            }
        }
    }
}

@Composable
fun ProjectionResultRow(result: UlipProjectionResult) {
    val numberFormat = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 0 } }
    Row(Modifier.fillMaxWidth()) {
        ProjectionTableCell(text = result.year.toString(), weight = 0.1f)
        ProjectionTableCell(text = numberFormat.format(result.annualPremium), weight = 0.25f)
        // Sum of all charges
        ProjectionTableCell(text = numberFormat.format(result.premiumAllocationCharge + result.mortalityCharge + result.adminCharge + result.fundManagementCharge), weight = 0.25f)
        ProjectionTableCell(text = numberFormat.format(result.closingFundValue), weight = 0.4f)
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UlipExistingPoliciesTab(
    policies: List<Policy>,
    ulipViewModel: UlipViewModel
) {
    val uiState by ulipViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val filteredPolicies = remember(policies, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            policies
        } else {
            policies.filter {
                it.shortName.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.policyNumber.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { ulipViewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            label = { Text("Search by Name or Policy No") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
        if (filteredPolicies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No ULIP policies found.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPolicies) { policy ->
                    DetailedPolicyCard(policy = policy) // This composable is in CommonUi.kt
                }
            }
        }
    }
}

@Composable
fun RowScope.ProjectionTableCell(text: String, weight: Float, isHeader: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.weight(weight).padding(4.dp),
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        fontSize = 12.sp
    )
}
