package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.model.*
import com.viplove.licadvisornative.ui.viewmodel.DataCollectionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCollectionScreen(
    navController: NavController,
    dataViewModel: DataCollectionViewModel = viewModel()
) {
    val uiState by dataViewModel.uiState.collectAsState()

    // Translate the title from the ViewModel
    val stepTitle = when (uiState.currentStepTitle) {
        "Initial Questions" -> stringResource(R.string.initial_questions_title)
        "Plan Details" -> stringResource(R.string.plan_details_title)
        "Personal Details" -> stringResource(R.string.personal_details_title)
        // Add other dynamic titles here if needed
        else -> uiState.currentStepTitle // Fallback for dynamic titles
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stepTitle) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStepIndex > 0) dataViewModel.previousStep() else navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else if (!uiState.isNewDraft) {
                        Text("Draft Saved", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentStep = uiState.currentStepIndex,
                totalSteps = uiState.totalSteps,
                onPrevious = { dataViewModel.previousStep() },
                onNext = { dataViewModel.nextStep() },
                onSubmit = {
                    dataViewModel.submitDataSheet()
                    navController.popBackStack()
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            DataCollectionWizard(
                uiState = uiState,
                onDataChange = { dataViewModel.onDataChange(it) },
                viewModel = dataViewModel
            )
        }
    }
}

@Composable
fun DataCollectionWizard(
    uiState: DataCollectionViewModel.UiState,
    onDataChange: (ClientDataSheet) -> Unit,
    viewModel: DataCollectionViewModel
) {
    val dataSheet = uiState.dataSheet

    when (uiState.currentStepTitle) {
        "Initial Questions" -> Step0_InitialQuestions(
            initialQuestions = dataSheet.initialQuestions,
            onQuestionsChange = { onDataChange(dataSheet.copy(initialQuestions = it)) }
        )
        "Plan Details" -> Step_PlanDetails(uiState = uiState, onDataChange = onDataChange)

        "Life Assured: Personal Details",
        "Proposer: Personal Details",
        "Life Assured (Spouse): Personal Details" -> {
            val person = if (uiState.currentStepTitle.contains("Proposer")) dataSheet.proposerDetails else dataSheet.lifeAssuredDetails
            Step_PersonalDetails(personDetails = person, onDetailsChange = { updatedPerson ->
                val updatedSheet = if (uiState.currentStepTitle.contains("Proposer")) {
                    dataSheet.copy(proposerDetails = updatedPerson)
                } else {
                    if (dataSheet.initialQuestions.proposerIsLA) {
                        dataSheet.copy(proposerDetails = updatedPerson, lifeAssuredDetails = updatedPerson)
                    } else {
                        dataSheet.copy(lifeAssuredDetails = updatedPerson)
                    }
                }
                onDataChange(updatedSheet)
            })
        }

        "Life Assured: Occupation Details",
        "Proposer: Occupation Details",
        "Life Assured (Spouse): Occupation Details" -> {
            val person = if (uiState.currentStepTitle.contains("Proposer")) dataSheet.proposerDetails else dataSheet.lifeAssuredDetails
            Step_OccupationDetails(personDetails = person, onDetailsChange = { updatedPerson ->
                val updatedSheet = if (uiState.currentStepTitle.contains("Proposer")) {
                    dataSheet.copy(proposerDetails = updatedPerson)
                } else {
                    if (dataSheet.initialQuestions.proposerIsLA) {
                        dataSheet.copy(proposerDetails = updatedPerson, lifeAssuredDetails = updatedPerson)
                    } else {
                        dataSheet.copy(lifeAssuredDetails = updatedPerson)
                    }
                }
                onDataChange(updatedSheet)
            })
        }

        "Life Assured: Family & Health",
        "Proposer: Family & Health",
        "Life Assured (Spouse): Family & Health" -> {
            val person = if (uiState.currentStepTitle.contains("Proposer")) dataSheet.proposerDetails else dataSheet.lifeAssuredDetails
            val isForProposer = !uiState.currentStepTitle.contains("Life Assured")
            Step_FamilyHealth(personDetails = person, onDetailsChange = { updatedPerson ->
                val updatedSheet = if (isForProposer) {
                    if (dataSheet.initialQuestions.proposerIsLA) {
                        dataSheet.copy(proposerDetails = updatedPerson, lifeAssuredDetails = updatedPerson)
                    } else {
                        dataSheet.copy(proposerDetails = updatedPerson)
                    }
                } else {
                    dataSheet.copy(lifeAssuredDetails = updatedPerson)
                }
                onDataChange(updatedSheet)
            }, viewModel = viewModel, isForProposer = isForProposer)
        }

        "Life Assured: Nominee & Bank",
        "Proposer: Nominee & Bank" -> {
            Step_NomineeBank(
                personDetails = dataSheet.proposerDetails,
                isNachMandatory = dataSheet.isNachMandatory == "Yes",
                onDetailsChange = { updatedPerson ->
                    onDataChange(dataSheet.copy(proposerDetails = updatedPerson, lifeAssuredDetails = if(dataSheet.initialQuestions.proposerIsLA) updatedPerson else dataSheet.lifeAssuredDetails))
                },
                viewModel = viewModel
            )
        }

        "Female Insured Info" -> {
            val personForFemaleInfo = if (dataSheet.initialQuestions.proposerIsLA) dataSheet.proposerDetails else dataSheet.lifeAssuredDetails
            Step_FemaleInsuredInfo(personDetails = personForFemaleInfo, onDetailsChange = { updatedDetails ->
                val updatedSheet = if (dataSheet.initialQuestions.proposerIsLA) {
                    dataSheet.copy(proposerDetails = updatedDetails, lifeAssuredDetails = updatedDetails)
                } else {
                    dataSheet.copy(lifeAssuredDetails = updatedDetails)
                }
                onDataChange(updatedSheet)
            })
        }

        "Life Assured (Child) Details" -> Step_LifeAssuredChild(personDetails = dataSheet.lifeAssuredDetails, onDetailsChange = { onDataChange(dataSheet.copy(lifeAssuredDetails = it)) })
        "Document Uploads" -> DocumentUploadStep(dataSheet = dataSheet, onDataChange = onDataChange)
        "Review & Submit" -> ReviewAndSubmitStep(dataSheet = dataSheet)

        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Step not implemented: ${uiState.currentStepTitle}")
        }
    }
}


// --- INDIVIDUAL STEP COMPOSABLES ---

@Composable
fun Step_PersonalDetails(personDetails: PersonDetails, onDetailsChange: (PersonDetails) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PersonalDetailsForm(personDetails = personDetails, onDetailsChange = onDetailsChange)
        }
    }
}

@Composable
fun Step_OccupationDetails(personDetails: PersonDetails, onDetailsChange: (PersonDetails) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OccupationDetailsForm(personDetails = personDetails, onDetailsChange = onDetailsChange)
        }
    }
}

@Composable
fun Step_FamilyHealth(personDetails: PersonDetails, onDetailsChange: (PersonDetails) -> Unit, viewModel: DataCollectionViewModel, isForProposer: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FamilyAndHealthForm(personDetails = personDetails, onDetailsChange = onDetailsChange, viewModel = viewModel, isForProposer = isForProposer)
        }
    }
}

@Composable
fun Step_NomineeBank(personDetails: PersonDetails, isNachMandatory: Boolean, onDetailsChange: (PersonDetails) -> Unit, viewModel: DataCollectionViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NomineeAndBankForm(
                personDetails = personDetails,
                isNachMandatory = isNachMandatory,
                onDetailsChange = onDetailsChange,
                viewModel = viewModel
            )
        }
    }
}

// --- CORE STEP COMPOSABLES ---

@Composable
fun Step0_InitialQuestions(initialQuestions: InitialQuestions, onQuestionsChange: (InitialQuestions) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(stringResource(R.string.initial_questions_title), style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider()

        Text(stringResource(R.string.gender_of_proposer_q), style = MaterialTheme.typography.titleMedium)
        Row {
            RadioButtonWithText(text = stringResource(R.string.male), selected = initialQuestions.proposerGender == "Male", onClick = { onQuestionsChange(initialQuestions.copy(proposerGender = "Male")) })
            Spacer(Modifier.width(16.dp))
            RadioButtonWithText(text = stringResource(R.string.female), selected = initialQuestions.proposerGender == "Female", onClick = { onQuestionsChange(initialQuestions.copy(proposerGender = "Female", isHousewife = false)) })
        }

        if (initialQuestions.proposerGender == "Female") {
            Text(stringResource(R.string.housewife_working_q), style = MaterialTheme.typography.titleMedium)
            Row {
                RadioButtonWithText(
                    text = stringResource(R.string.housewife),
                    selected = initialQuestions.isHousewife,
                    onClick = { onQuestionsChange(initialQuestions.copy(isHousewife = true, proposerIsLA = true)) }
                )
                Spacer(Modifier.width(16.dp))
                RadioButtonWithText(
                    text = stringResource(R.string.working),
                    selected = !initialQuestions.isHousewife,
                    onClick = { onQuestionsChange(initialQuestions.copy(isHousewife = false)) }
                )
            }
        }

        val isSameAsLaEnabled = !(initialQuestions.proposerGender == "Female" && initialQuestions.isHousewife)
        Text(stringResource(R.string.proposer_same_as_la_q), style = MaterialTheme.typography.titleMedium)
        Row {
            RadioButtonWithText(text = stringResource(R.string.yes), selected = initialQuestions.proposerIsLA, onClick = { if(isSameAsLaEnabled) onQuestionsChange(initialQuestions.copy(proposerIsLA = true)) })
            Spacer(Modifier.width(16.dp))
            RadioButtonWithText(text = stringResource(R.string.no), selected = !initialQuestions.proposerIsLA, onClick = { if(isSameAsLaEnabled) onQuestionsChange(initialQuestions.copy(proposerIsLA = false)) })
        }
        if (!isSameAsLaEnabled) {
            Text(
                stringResource(R.string.housewife_propose_self),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!initialQuestions.proposerIsLA) {
            val lifeAssuredOptions = when {
                initialQuestions.proposerGender == "Male" -> listOf("Spouse", "Child")
                initialQuestions.proposerGender == "Female" && !initialQuestions.isHousewife -> listOf("Child")
                else -> emptyList()
            }
            if (lifeAssuredOptions.isNotEmpty()) {
                FilterDropdown(
                    label = stringResource(R.string.who_is_la_q),
                    options = lifeAssuredOptions,
                    selectedOption = initialQuestions.lifeAssuredIs,
                    onOptionSelected = { onQuestionsChange(initialQuestions.copy(lifeAssuredIs = it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step_PlanDetails(uiState: DataCollectionViewModel.UiState, onDataChange: (ClientDataSheet) -> Unit) {
    var showDocPicker by remember { mutableStateOf(false) }
    val dataSheet = uiState.dataSheet
    // --- UPDATED: Get planDetails from dataSheet ---
    val planDetails = dataSheet.planDetails
    val focusManager = LocalFocusManager.current

    LaunchedEffect(dataSheet.dateOfCommencement, dataSheet.isNachMandatory) {
        if (dataSheet.isNachMandatory == "Yes" && dataSheet.dateOfCommencement != null) {
            val docCalendar = Calendar.getInstance().apply { timeInMillis = dataSheet.dateOfCommencement!! }
            val dayOfMonth = docCalendar.get(Calendar.DAY_OF_MONTH)

            val nachDay = when {
                dayOfMonth in 1..7 -> 7
                dayOfMonth in 8..15 -> 15
                dayOfMonth in 16..22 -> 22
                else -> 28
            }

            val nachCalendar = Calendar.getInstance().apply {
                timeInMillis = dataSheet.dateOfCommencement!!
                set(Calendar.DAY_OF_MONTH, nachDay)
            }

            val newNachDate = nachCalendar.timeInMillis
            if (newNachDate != dataSheet.nachDate) {
                onDataChange(dataSheet.copy(nachDate = newNachDate))
            }
        } else {
            if (dataSheet.nachDate != null) {
                onDataChange(dataSheet.copy(nachDate = null))
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text(stringResource(R.string.plan_premium_details_title), style = MaterialTheme.typography.headlineSmall) }

        // --- UPDATED: This field binds to the top-level sumAssured, which is correct based on the model ---
        item {
            OutlinedTextField(
                value = dataSheet.sumAssured,
                onValueChange = { onDataChange(dataSheet.copy(sumAssured = it.filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.sum_assured_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.sumAssuredInWords.isNotEmpty()) {
                Text(
                    text = uiState.sumAssuredInWords,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 4.dp)
                )
            }
        }

        // --- FIX: Replaced 'planAndTerm' with 'planDetails' fields ---
        item {
            OutlinedTextField(
                value = planDetails.planName,
                onValueChange = {
                    val newDetails = planDetails.copy(planName = it)
                    onDataChange(dataSheet.copy(planDetails = newDetails))
                },
                label = { Text("Plan Name") }, // You may want to add this to strings.xml
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = planDetails.planNumber,
                onValueChange = {
                    val newDetails = planDetails.copy(planNumber = it.filter { c -> c.isDigit() })
                    onDataChange(dataSheet.copy(planDetails = newDetails))
                },
                label = { Text("Plan Number") }, // You may want to add this to strings.xml
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = planDetails.policyTerm,
                    onValueChange = {
                        val newDetails = planDetails.copy(policyTerm = it.filter { c -> c.isDigit() })
                        onDataChange(dataSheet.copy(planDetails = newDetails))
                    },
                    label = { Text("Policy Term (Yrs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) }),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = planDetails.ppt,
                    onValueChange = {
                        val newDetails = planDetails.copy(ppt = it.filter { c -> c.isDigit() })
                        onDataChange(dataSheet.copy(planDetails = newDetails))
                    },
                    label = { Text("Premium Term (Yrs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- UPDATED: This field binds to the top-level mode, which is correct based on the model ---
        item {
            FilterDropdown(
                label = stringResource(R.string.mode_label),
                options = listOf("MLY", "QLY", "HLY", "YLY", "Single"),
                selectedOption = dataSheet.mode,
                onOptionSelected = { onDataChange(dataSheet.copy(mode = it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedButton(onClick = { showDocPicker = true }, modifier = Modifier.fillMaxWidth()) {
                val dateText = dataSheet.dateOfCommencement?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: stringResource(R.string.select_doc_button)
                Text(dateText)
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = dataSheet.isNachMandatory == "Yes",
                    onCheckedChange = { isChecked ->
                        onDataChange(dataSheet.copy(isNachMandatory = if (isChecked) "Yes" else "No"))
                    }
                )
                Text(stringResource(R.string.nach_required_checkbox))
            }
        }

        if (dataSheet.isNachMandatory == "Yes" && dataSheet.nachDate != null) {
            item {
                OutlinedTextField(
                    value = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(dataSheet.nachDate!!)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("eNACH Date") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!dataSheet.initialQuestions.proposerIsLA && dataSheet.initialQuestions.lifeAssuredIs == "Child") {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = dataSheet.pwbRequired,
                        onCheckedChange = { onDataChange(dataSheet.copy(pwbRequired = it)) }
                    )
                    Text(stringResource(R.string.pwb_rider_q))
                }
            }
        }
    }

    if (showDocPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDocPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDataChange(dataSheet.copy(dateOfCommencement = datePickerState.selectedDateMillis))
                    showDocPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDocPicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun Step_LifeAssuredChild(personDetails: PersonDetails, onDetailsChange: (PersonDetails) -> Unit) {
    val focusManager = LocalFocusManager.current
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Life Assured (Child) Details", style = MaterialTheme.typography.headlineSmall) }
        item {
            OutlinedTextField(
                value = personDetails.name,
                onValueChange = { onDetailsChange(personDetails.copy(name = it)) },
                label = { Text(stringResource(R.string.child_full_name)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
        }
        item {
            OutlinedTextField(
                value = personDetails.educationDetails,
                onValueChange = { onDetailsChange(personDetails.copy(educationDetails = it)) },
                label = { Text(stringResource(R.string.child_education)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = personDetails.heightCm,
                    onValueChange = { onDetailsChange(personDetails.copy(heightCm = it.filter { c -> c.isDigit() })) },
                    label = { Text(stringResource(R.string.height_cm_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) }),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = personDetails.weightKg,
                    onValueChange = { onDetailsChange(personDetails.copy(weightKg = it.filter { c -> c.isDigit() })) },
                    label = { Text(stringResource(R.string.weight_kg_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = personDetails.isPermanentAddressSame,
                    onCheckedChange = { onDetailsChange(personDetails.copy(isPermanentAddressSame = it)) }
                )
                Text(stringResource(R.string.address_same_as_proposer))
            }
        }
        if (!personDetails.isPermanentAddressSame) {
            item { AddressFields(address = personDetails.communicationAddress, onAddressChange = { onDetailsChange(personDetails.copy(communicationAddress = it)) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step_FemaleInsuredInfo(personDetails: PersonDetails, onDetailsChange: (PersonDetails) -> Unit) {
    var showDeliveryDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text(stringResource(R.string.female_insured_info_title), style = MaterialTheme.typography.headlineSmall) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = personDetails.isPregnant,
                    onCheckedChange = { onDetailsChange(personDetails.copy(isPregnant = it)) }
                )
                Text(stringResource(R.string.are_you_pregnant_q))
            }
        }

        item {
            OutlinedButton(onClick = { showDeliveryDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                val dateText = personDetails.dateOfLastDelivery?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: stringResource(R.string.select_last_delivery_date)
                Text(dateText)
            }
        }

        item {
            OutlinedTextField(
                value = personDetails.miscarriageDetails,
                onValueChange = { onDetailsChange(personDetails.copy(miscarriageDetails = it)) },
                label = { Text(stringResource(R.string.miscarriage_details)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }
    }

    if (showDeliveryDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDeliveryDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDetailsChange(personDetails.copy(dateOfLastDelivery = datePickerState.selectedDateMillis))
                    showDeliveryDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDeliveryDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


// --- FORM COMPOSABLES ---

@Composable
fun PersonalDetailsForm(personDetails: PersonDetails, onDetailsChange: (PersonDetails) -> Unit)  {
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.personal_details_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = personDetails.name,
            onValueChange = { onDetailsChange(personDetails.copy(name = it)) },
            label = { Text(stringResource(R.string.full_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        OutlinedTextField(
            value = personDetails.contactNo,
            onValueChange = { onDetailsChange(personDetails.copy(contactNo = it.filter { c -> c.isDigit() })) },
            label = { Text(stringResource(R.string.contact_no_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = personDetails.emailId,
            onValueChange = { onDetailsChange(personDetails.copy(emailId = it)) },
            label = { Text(stringResource(R.string.email_id_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = personDetails.adharCardNo,
            onValueChange = { onDetailsChange(personDetails.copy(adharCardNo = it.filter { c -> c.isDigit() })) },
            label = { Text(stringResource(R.string.aadhar_card_no_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth()
        )

        FilterDropdown(
            label = stringResource(R.string.marital_status_label),
            options = listOf("Single", "Married", "Divorced", "Widow"),
            selectedOption = personDetails.maritalStatus,
            onOptionSelected = { onDetailsChange(personDetails.copy(maritalStatus = it)) },
            modifier = Modifier.fillMaxWidth()
        )

        if (personDetails.maritalStatus == "Married") {
            OutlinedTextField(
                value = personDetails.spouseName,
                onValueChange = { onDetailsChange(personDetails.copy(spouseName = it)) },
                label = { Text(stringResource(R.string.spouse_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
        }

        OutlinedTextField(
            value = personDetails.fatherName,
            onValueChange = { onDetailsChange(personDetails.copy(fatherName = it)) },
            label = { Text(stringResource(R.string.father_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        OutlinedTextField(
            value = personDetails.motherName,
            onValueChange = { onDetailsChange(personDetails.copy(motherName = it)) },
            label = { Text(stringResource(R.string.mother_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        FilterDropdown(
            label = stringResource(R.string.it_assessee_q),
            options = listOf("Yes", "No"),
            selectedOption = personDetails.taxAssessee,
            onOptionSelected = { onDetailsChange(personDetails.copy(taxAssessee = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = personDetails.panCardNo,
            onValueChange = { onDetailsChange(personDetails.copy(panCardNo = it.uppercase())) },
            label = { Text(stringResource(R.string.pan_card_no_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        Text(stringResource(R.string.comm_address), style = MaterialTheme.typography.titleMedium)
        AddressFields(address = personDetails.communicationAddress, onAddressChange = { onDetailsChange(personDetails.copy(communicationAddress = it)) })

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = personDetails.isPermanentAddressSame,
                onCheckedChange = { isChecked ->
                    val updatedDetails = personDetails.copy(isPermanentAddressSame = isChecked)
                    if (isChecked) {
                        onDetailsChange(updatedDetails.copy(permanentAddress = updatedDetails.communicationAddress.copy()))
                    } else {
                        onDetailsChange(updatedDetails)
                    }
                }
            )
            Text(stringResource(R.string.perm_address_same))
        }

        if (!personDetails.isPermanentAddressSame) {
            Text(stringResource(R.string.perm_address), style = MaterialTheme.typography.titleMedium)
            AddressFields(address = personDetails.permanentAddress, onAddressChange = { onDetailsChange(personDetails.copy(permanentAddress = it)) })
        }
    }
}

@Composable
fun OccupationDetailsForm(personDetails: PersonDetails, onDetailsChange: (PersonDetails) -> Unit) {
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.occupation_details_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = personDetails.occupation,
            onValueChange = { onDetailsChange(personDetails.copy(occupation = it)) },
            label = { Text(stringResource(R.string.occupation_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        OutlinedTextField(
            value = personDetails.natureOfDuties,
            onValueChange = { onDetailsChange(personDetails.copy(natureOfDuties = it)) },
            label = { Text(stringResource(R.string.nature_of_duties_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        OutlinedTextField(
            value = personDetails.lengthOfService,
            onValueChange = { onDetailsChange(personDetails.copy(lengthOfService = it)) },
            label = { Text(stringResource(R.string.length_of_service_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        OutlinedTextField(
            value = personDetails.employerName,
            onValueChange = { onDetailsChange(personDetails.copy(employerName = it)) },
            label = { Text(stringResource(R.string.name_of_employer_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        OutlinedTextField(
            value = personDetails.annualIncome,
            onValueChange = { onDetailsChange(personDetails.copy(annualIncome = it.filter { c -> c.isDigit() })) },
            label = { Text(stringResource(R.string.annual_income_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )
        FilterDropdown(
            label = stringResource(R.string.education_label),
            options = listOf("Professional", "Post Graduate or Graduate", "HSC/ 12th or Diploma", "SSC/10th", "9th Std. Non SSC", "Literate up to 8th", "Illiterate"),
            selectedOption = personDetails.education,
            onOptionSelected = { onDetailsChange(personDetails.copy(education = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        FilterDropdown(
            label = stringResource(R.string.armed_forces_q),
            options = listOf("Yes", "No"),
            selectedOption = personDetails.armedForces,
            onOptionSelected = { onDetailsChange(personDetails.copy(armedForces = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        if (personDetails.isHousewife) {
            Text(stringResource(R.string.husbands_details), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = personDetails.husbandOccupation,
                onValueChange = { onDetailsChange(personDetails.copy(husbandOccupation = it)) },
                label = { Text(stringResource(R.string.husbands_occupation)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
            OutlinedTextField(
                value = personDetails.husbandAnnualIncome,
                onValueChange = { onDetailsChange(personDetails.copy(husbandAnnualIncome = it.filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.husbands_annual_income)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = personDetails.husbandPolicyDetails,
                onValueChange = { onDetailsChange(personDetails.copy(husbandPolicyDetails = it)) },
                label = { Text(stringResource(R.string.husbands_policy_details)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }
    }
}

@Composable
fun FamilyAndHealthForm(personDetails: PersonDetails, onDetailsChange: (PersonDetails) -> Unit, viewModel: DataCollectionViewModel, isForProposer: Boolean) {
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.family_health_history_title), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = personDetails.heightCm,
                onValueChange = { onDetailsChange(personDetails.copy(heightCm = it.filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.height_cm_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) }),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = personDetails.weightKg,
                onValueChange = { onDetailsChange(personDetails.copy(weightKg = it.filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.weight_kg_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = personDetails.stateOfHealth,
            onValueChange = { onDetailsChange(personDetails.copy(stateOfHealth = it)) },
            label = { Text(stringResource(R.string.state_of_health_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.family_history_title), style = MaterialTheme.typography.titleMedium)

        personDetails.familyHistory.forEach { member ->
            FamilyMemberCard(
                member = member,
                onMemberChange = { updatedMember ->
                    val index = personDetails.familyHistory.indexOf(member)
                    if (index != -1) {
                        val newList = personDetails.familyHistory.toMutableList()
                        newList[index] = updatedMember
                        onDetailsChange(personDetails.copy(familyHistory = newList))
                    }
                },
                onRemove = { viewModel.removeFamilyMember(member, isForProposer) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.add_optional_family_members), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { viewModel.addFamilyMember("Spouse", isForProposer) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.add_spouse_button)) }
            Button(onClick = { viewModel.addFamilyMember("Brother", isForProposer) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.add_brother_button)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { viewModel.addFamilyMember("Sister", isForProposer) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.add_sister_button)) }
            Button(onClick = { viewModel.addFamilyMember("Son", isForProposer) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.add_son_button)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { viewModel.addFamilyMember("Daughter", isForProposer) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.add_daughter_button)) }
        }
    }
}

@Composable
fun FamilyMemberCard(member: FamilyMember, onMemberChange: (FamilyMember) -> Unit, onRemove: () -> Unit) {
    val focusManager = LocalFocusManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(member.relation, style = MaterialTheme.typography.titleMedium)
                if (member.relation !in listOf("Father", "Mother")) {
                    IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, stringResource(R.string.remove)) }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButtonWithText(text = stringResource(R.string.alive), selected = member.isAlive, onClick = { onMemberChange(member.copy(isAlive = true)) })
                Spacer(Modifier.width(16.dp))
                RadioButtonWithText(text = stringResource(R.string.dead), selected = !member.isAlive, onClick = { onMemberChange(member.copy(isAlive = false)) })
            }

            if (member.isAlive) {
                OutlinedTextField(
                    value = member.age,
                    onValueChange = { onMemberChange(member.copy(age = it.filter { c -> c.isDigit() })) },
                    label = { Text(stringResource(R.string.age)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = member.ageAtDeath,
                    onValueChange = { onMemberChange(member.copy(ageAtDeath = it.filter { c -> c.isDigit() })) },
                    label = { Text(stringResource(R.string.age_at_death)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = member.causeOfDeath,
                    onValueChange = { onMemberChange(member.copy(causeOfDeath = it)) },
                    label = { Text(stringResource(R.string.cause_of_death)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun NomineeAndBankForm(
    personDetails: PersonDetails,
    isNachMandatory: Boolean,
    onDetailsChange: (PersonDetails) -> Unit,
    viewModel: DataCollectionViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.nominee_bank_details_title), style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(stringResource(R.string.client_bank_details_title), style = MaterialTheme.typography.titleMedium)
        BankDetailsCard(bankDetails = personDetails.clientBankDetails, onDetailsChange = { onDetailsChange(personDetails.copy(clientBankDetails = it)) })
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (isNachMandatory) {
            Text(stringResource(R.string.nach_bank_details), style = MaterialTheme.typography.titleMedium)
            BankDetailsCard(
                bankDetails = personDetails.clientNachBankDetails,
                onDetailsChange = { onDetailsChange(personDetails.copy(clientNachBankDetails = it)) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        Text(stringResource(R.string.nominee_details_title), style = MaterialTheme.typography.titleMedium)
        personDetails.nominees.forEach { nominee ->
            NomineeCard(
                nominee = nominee,
                onNomineeChange = { updatedNominee ->
                    val index = personDetails.nominees.indexOf(nominee)
                    if (index != -1) {
                        val newList = personDetails.nominees.toMutableList()
                        newList[index] = updatedNominee
                        onDetailsChange(personDetails.copy(nominees = newList))
                    }
                },
                onRemove = { viewModel.removeNominee(nominee) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = { viewModel.addNominee() }) { Text(stringResource(R.string.add_nominee_button)) }

        if (personDetails.nominees.any { (it.age.toIntOrNull() ?: 99) < 18 }) {
            Text(stringResource(R.string.appointee_details_minor), style = MaterialTheme.typography.titleLarge)
            val appointee = personDetails.appointee ?: Appointee()
            AppointeeCard(appointee = appointee, onAppointeeChange = { onDetailsChange(personDetails.copy(appointee = it)) })
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun AddressFields(address: Address, onAddressChange: (Address) -> Unit) {
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = address.line1,
            onValueChange = { onAddressChange(address.copy(line1 = it)) },
            label = { Text(stringResource(R.string.address_line_1)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        OutlinedTextField(
            value = address.line2,
            onValueChange = { onAddressChange(address.copy(line2 = it)) },
            label = { Text(stringResource(R.string.address_line_2)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = address.city,
                onValueChange = { onAddressChange(address.copy(city = it)) },
                label = { Text(stringResource(R.string.city)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) })
            )
            OutlinedTextField(
                value = address.pincode,
                onValueChange = { onAddressChange(address.copy(pincode = it.take(6).filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.pincode)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = address.district,
                onValueChange = { onAddressChange(address.copy(district = it)) },
                label = { Text(stringResource(R.string.district)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) })
            )
            OutlinedTextField(
                value = address.state,
                onValueChange = { onAddressChange(address.copy(state = it)) },
                label = { Text(stringResource(R.string.state)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }
    }
}

@Composable
fun BankDetailsCard(bankDetails: BankDetails, onDetailsChange: (BankDetails) -> Unit) {
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = bankDetails.bankName,
            onValueChange = { onDetailsChange(bankDetails.copy(bankName = it)) },
            label = { Text(stringResource(R.string.bank_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        OutlinedTextField(
            value = bankDetails.ifscCode,
            onValueChange = { onDetailsChange(bankDetails.copy(ifscCode = it.uppercase())) },
            label = { Text(stringResource(R.string.ifsc_code_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )
        OutlinedTextField(
            value = bankDetails.accountNo,
            onValueChange = { onDetailsChange(bankDetails.copy(accountNo = it.filter { c -> c.isDigit() })) },
            label = { Text(stringResource(R.string.account_no_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )
        FilterDropdown(
            label = stringResource(R.string.account_type_label),
            options = listOf("Savings", "Current"),
            selectedOption = bankDetails.accountType,
            onOptionSelected = { onDetailsChange(bankDetails.copy(accountType = it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NomineeCard(nominee: Nominee, onNomineeChange: (Nominee) -> Unit, onRemove: () -> Unit) {
    val focusManager = LocalFocusManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.nominee), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, stringResource(R.string.remove)) }
            }
            OutlinedTextField(
                value = nominee.name,
                onValueChange = { onNomineeChange(nominee.copy(name = it)) },
                label = { Text(stringResource(R.string.nominee_name)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
            OutlinedTextField(
                value = nominee.age,
                onValueChange = { onNomineeChange(nominee.copy(age = it.filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.age)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nominee.relation,
                onValueChange = { onNomineeChange(nominee.copy(relation = it)) },
                label = { Text(stringResource(R.string.relation)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
            OutlinedTextField(
                value = nominee.percentageOfShare,
                onValueChange = { onNomineeChange(nominee.copy(percentageOfShare = it.filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.percentage_share)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AppointeeCard(appointee: Appointee, onAppointeeChange: (Appointee) -> Unit) {
    val focusManager = LocalFocusManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.appointee), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = appointee.name,
                onValueChange = { onAppointeeChange(appointee.copy(name = it)) },
                label = { Text(stringResource(R.string.appointee_name)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
            OutlinedTextField(
                value = appointee.age,
                onValueChange = { onAppointeeChange(appointee.copy(age = it.filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.age)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = appointee.relation,
                onValueChange = { onAppointeeChange(appointee.copy(relation = it)) },
                label = { Text(stringResource(R.string.relation)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }
    }
}


@Composable
fun DocumentUploadStep(dataSheet: ClientDataSheet, onDataChange: (ClientDataSheet) -> Unit) {
    // This function's body is intentionally left empty
    // so that the imported 'DocumentUploadStep' from
    // 'com.viplove.licadvisornative.ui.screens.DocumentUploadScreen.kt' is used.
}


@Composable
fun RadioButtonWithText(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .selectable(selected = selected, onClick = onClick, role = androidx.compose.ui.semantics.Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = text, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun BottomNavigationBar(
    currentStep: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    BottomAppBar {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isFirstStep = currentStep == 0
            val isLastStep = currentStep == totalSteps

            Button(
                onClick = onPrevious,
                enabled = !isFirstStep
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Previous")
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.back_button))
            }

            Text(
                stringResource(R.string.step_of, currentStep + 1, totalSteps + 1),
                fontWeight = FontWeight.Bold
            )

            if (isLastStep) {
                Button(
                    onClick = onSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.submit_button))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Icon(Icons.Filled.Check, contentDescription = "Submit")
                }
            } else {
                Button(onClick = onNext) {
                    Text(stringResource(R.string.next_button))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        }
    }
}
