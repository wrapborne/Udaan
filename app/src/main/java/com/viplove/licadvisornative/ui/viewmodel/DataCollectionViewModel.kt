package com.viplove.licadvisornative.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.viplove.licadvisornative.model.*
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.network.toClientDataSheet
import com.viplove.licadvisornative.util.ErrorMapper
import com.viplove.licadvisornative.util.NumberToWords
import com.viplove.licadvisornative.util.RemoteConfigManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.floor
import kotlin.math.roundToInt

class DataCollectionViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = "DataCollectionVM"

    private var stepSequence: List<String> = emptyList()

    data class UiState(
        val currentStepIndex: Int = 0,
        val totalSteps: Int = 1,
        val chartData: ChartData? = null,
        val currentStepTitle: String = "Initial Questions",
        val dataSheet: ClientDataSheet = ClientDataSheet(),
        val isSaving: Boolean = false,
        val error: String? = null,
        val isNewDraft: Boolean = true,
        val sumAssuredInWords: String = "",
        val isReadOnly: Boolean = false,
        val heightFeet: String = "",
        val heightInches: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val api = ApiClient.api
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Any?>>() {}.type
    private val listMapType = object : TypeToken<List<Map<String, Any?>>>() {}.type
    private var saveDraftJob: Job? = null

    init {
        val draftId = savedStateHandle.get<String>("draftId")
        val isReadOnly = savedStateHandle.get<Boolean>("readOnly") ?: false
        _uiState.update { it.copy(isReadOnly = isReadOnly) }

        viewModelScope.launch {
            RemoteConfigManager.chartDataState.collect { chartData ->
                if (chartData != null) {
                    _uiState.update { it.copy(chartData = chartData) }
                }
            }
        }

        if (draftId == null || draftId == "new") {
            createNewDraft()
        } else {
            loadDraft(draftId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveDraftJob?.cancel()
    }

    private fun syncHeightFields(person: PersonDetails) {
        val cm = person.heightCm.toDoubleOrNull() ?: 0.0
        if (cm > 0) {
            val totalInches = cm / 2.54
            val feet = floor(totalInches / 12).toInt()
            val inches = (totalInches % 12).roundToInt()
            _uiState.update { it.copy(heightFeet = feet.toString(), heightInches = inches.toString()) }
        } else {
            _uiState.update { it.copy(heightFeet = "", heightInches = "") }
        }
    }

    private fun loadDraft(draftId: String) {
        viewModelScope.launch {
            try {
                val response = api.getDatasheet(draftId)
                if (response.isSuccessful) {
                    val sheet = response.body()?.toClientDataSheet()
                    if (sheet != null) {
                        val sa = sheet.sumAssured.toLongOrNull() ?: 0L
                        val sumAssuredInWords = if (sa > 0) NumberToWords.convert(sa) else ""
                        _uiState.update { it.copy(dataSheet = sheet, isNewDraft = false, sumAssuredInWords = sumAssuredInWords) }
                        buildStepSequence(sheet)
                        val personForHealth = getPersonForCurrentHealthStep(sheet, _uiState.value.currentStepTitle)
                        personForHealth?.let { syncHeightFields(it) }
                    } else {
                        createNewDraft()
                    }
                } else {
                    createNewDraft()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = ErrorMapper.map(e)) }
                createNewDraft()
            }
        }
    }

    private fun createNewDraft() {
        val advisorId = TokenManager.getUserId() ?: return

        viewModelScope.launch {
            val adminId = try {
                val response = api.me()
                if (response.isSuccessful) response.body()?.adminId ?: "" else ""
            } catch (e: Exception) {
                _uiState.update { it.copy(error = ErrorMapper.map(e)) }
                ""
            }

            val prePopulatedFamilyHistory = mutableListOf(
                FamilyMember(relation = "Father"),
                FamilyMember(relation = "Mother")
            )
            val newSheet = ClientDataSheet(
                id = UUID.randomUUID().toString(),
                createdByAdvisorId = advisorId,
                adminId = adminId,
                proposerDetails = PersonDetails(familyHistory = prePopulatedFamilyHistory),
                lifeAssuredDetails = PersonDetails(familyHistory = prePopulatedFamilyHistory),
                isArchived = false
            )

            try {
                api.createDatasheet(newSheet.toApiPayload())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create draft on server: ${e.message}")
            }

            _uiState.update { it.copy(dataSheet = newSheet, isNewDraft = false) }
            buildStepSequence(newSheet)
        }
    }

    private fun buildStepSequence(dataSheet: ClientDataSheet) {
        val questions = dataSheet.initialQuestions
        val steps = mutableListOf("Initial Questions", "Plan Details")

        fun addPersonDetailSteps(personPrefix: String, isProposer: Boolean, needsHealth: Boolean) {
            steps.add("$personPrefix: Personal Details")
            steps.add("$personPrefix: Occupation Details")
            if (needsHealth) {
                steps.add("$personPrefix: Family & Health")
            }
            if (isProposer) {
                steps.add("$personPrefix: Nominee & Bank")
            }
        }

        val lifeAssuredPerson = if (questions.proposerIsLA) dataSheet.proposerDetails else dataSheet.lifeAssuredDetails
        val shouldShowFemaleInfo = lifeAssuredPerson.gender == "Female" && lifeAssuredPerson.maritalStatus == "Married"

        if (questions.proposerIsLA) {
            addPersonDetailSteps("Life Assured", isProposer = true, needsHealth = true)
        } else {
            if (questions.lifeAssuredIs == "Spouse") {
                addPersonDetailSteps("Proposer", isProposer = true, needsHealth = false)
                addPersonDetailSteps("Life Assured (Spouse)", isProposer = false, needsHealth = true)
            } else {
                addPersonDetailSteps("Proposer", isProposer = true, needsHealth = dataSheet.pwbRequired)
                steps.add("Life Assured (Child) Details")
            }
        }

        if (shouldShowFemaleInfo) {
            steps.add("Female Insured Info")
        }

        steps.add("Document Uploads")
        steps.add("Review & Submit")

        this.stepSequence = steps

        var updatedStepTitle = ""
        _uiState.update {
            val newIndex = it.currentStepIndex.coerceAtMost(steps.size - 1)
            updatedStepTitle = steps.getOrElse(newIndex) { "" }
            it.copy(
                currentStepIndex = newIndex,
                totalSteps = steps.size - 1,
                currentStepTitle = updatedStepTitle
            )
        }

        val personForHealth = getPersonForCurrentHealthStep(dataSheet, updatedStepTitle)
        personForHealth?.let { person -> syncHeightFields(person) }
    }

    private fun getPersonForCurrentHealthStep(dataSheet: ClientDataSheet, stepTitle: String): PersonDetails? {
        return when {
            stepTitle.contains("Proposer: Family & Health") -> dataSheet.proposerDetails
            stepTitle.contains("Life Assured: Family & Health") -> dataSheet.lifeAssuredDetails
            else -> null
        }
    }

    fun nextStep() {
        if (_uiState.value.currentStepIndex < _uiState.value.totalSteps) {
            val newIndex = _uiState.value.currentStepIndex + 1
            _uiState.update { it.copy(
                currentStepIndex = newIndex,
                currentStepTitle = stepSequence.getOrElse(newIndex) { "" }
            ) }
            val person = getPersonForCurrentHealthStep(_uiState.value.dataSheet, _uiState.value.currentStepTitle)
            person?.let { syncHeightFields(it) }
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStepIndex > 0) {
            val newIndex = _uiState.value.currentStepIndex - 1
            _uiState.update { it.copy(
                currentStepIndex = newIndex,
                currentStepTitle = stepSequence.getOrElse(newIndex) { "" }
            ) }
            val person = getPersonForCurrentHealthStep(_uiState.value.dataSheet, _uiState.value.currentStepTitle)
            person?.let { syncHeightFields(it) }
        }
    }

    fun onHeightFeetChange(feet: String, isForProposer: Boolean) {
        val feetValue = feet.filter { it.isDigit() }
        _uiState.update { it.copy(heightFeet = feetValue) }
        updateHeightCm(feetValue, _uiState.value.heightInches, isForProposer)
    }

    fun onHeightInchesChange(inches: String, isForProposer: Boolean) {
        val inchesValue = inches.filter { it.isDigit() }
        _uiState.update { it.copy(heightInches = inchesValue) }
        updateHeightCm(_uiState.value.heightFeet, inchesValue, isForProposer)
    }

    private fun updateHeightCm(feetStr: String, inchesStr: String, isForProposer: Boolean) {
        val feet = feetStr.toIntOrNull() ?: 0
        val inches = inchesStr.toIntOrNull() ?: 0

        val totalInches = (feet * 12) + inches
        val cm = if (totalInches > 0) (totalInches * 2.54).roundToInt() else 0

        val currentSheet = _uiState.value.dataSheet
        val personToUpdate = if (isForProposer) currentSheet.proposerDetails else currentSheet.lifeAssuredDetails
        val updatedPerson = personToUpdate.copy(heightCm = cm.toString())

        val updatedSheet = if (isForProposer) {
            currentSheet.copy(proposerDetails = updatedPerson, lifeAssuredDetails = if (currentSheet.initialQuestions.proposerIsLA) updatedPerson else currentSheet.lifeAssuredDetails)
        } else {
            currentSheet.copy(lifeAssuredDetails = updatedPerson)
        }
        onDataChange(updatedSheet)
    }

    fun onDataChange(updatedSheet: ClientDataSheet) {
        if (_uiState.value.isReadOnly) return

        val oldSheet = _uiState.value.dataSheet
        var sheetToUpdate = updatedSheet
        var sumAssuredInWords = _uiState.value.sumAssuredInWords

        if (updatedSheet.sumAssured != oldSheet.sumAssured) {
            val sa = updatedSheet.sumAssured.toLongOrNull() ?: 0L
            sumAssuredInWords = if (sa > 0) NumberToWords.convert(sa) else ""
        }

        val questions = updatedSheet.initialQuestions
        if (questions != oldSheet.initialQuestions) {
            val proposerDetails = sheetToUpdate.proposerDetails.copy(gender = questions.proposerGender)
            val lifeAssuredDetails = if (questions.proposerIsLA) {
                proposerDetails
            } else {
                val laGender = if (questions.lifeAssuredIs == "Spouse") "Female" else sheetToUpdate.lifeAssuredDetails.gender
                sheetToUpdate.lifeAssuredDetails.copy(gender = laGender)
            }
            sheetToUpdate = sheetToUpdate.copy(
                proposerDetails = proposerDetails,
                lifeAssuredDetails = lifeAssuredDetails
            )
        }

        if (sheetToUpdate.initialQuestions != oldSheet.initialQuestions ||
            sheetToUpdate.pwbRequired != oldSheet.pwbRequired ||
            sheetToUpdate.proposerDetails.maritalStatus != oldSheet.proposerDetails.maritalStatus ||
            sheetToUpdate.lifeAssuredDetails.maritalStatus != oldSheet.lifeAssuredDetails.maritalStatus) {
            buildStepSequence(sheetToUpdate)
        }

        _uiState.update { it.copy(dataSheet = sheetToUpdate, sumAssuredInWords = sumAssuredInWords) }
        triggerSaveDraft()
    }

    fun addFamilyMember(relation: String, isForProposer: Boolean) {
        val currentSheet = _uiState.value.dataSheet
        val personToUpdate = if (isForProposer) currentSheet.proposerDetails else currentSheet.lifeAssuredDetails

        val shouldAdd = if (relation in listOf("Father", "Mother")) {
            personToUpdate.familyHistory.none { it.relation == relation }
        } else {
            true
        }

        if (shouldAdd) {
            val updatedFamilyHistory = personToUpdate.familyHistory.toMutableList().apply {
                add(FamilyMember(relation = relation))
            }
            val updatedPerson = personToUpdate.copy(familyHistory = updatedFamilyHistory)
            val updatedSheet = if (isForProposer) {
                currentSheet.copy(proposerDetails = updatedPerson, lifeAssuredDetails = if (currentSheet.initialQuestions.proposerIsLA) updatedPerson else currentSheet.lifeAssuredDetails)
            } else {
                currentSheet.copy(lifeAssuredDetails = updatedPerson)
            }
            onDataChange(updatedSheet)
        }
    }

    fun removeFamilyMember(member: FamilyMember, isForProposer: Boolean) {
        if (member.relation in listOf("Father", "Mother")) return

        val currentSheet = _uiState.value.dataSheet
        val personToUpdate = if (isForProposer) currentSheet.proposerDetails else currentSheet.lifeAssuredDetails

        val updatedFamilyHistory = personToUpdate.familyHistory.toMutableList().apply {
            remove(member)
        }
        val updatedPerson = personToUpdate.copy(familyHistory = updatedFamilyHistory)
        val updatedSheet = if (isForProposer) {
            currentSheet.copy(proposerDetails = updatedPerson, lifeAssuredDetails = if (currentSheet.initialQuestions.proposerIsLA) updatedPerson else currentSheet.lifeAssuredDetails)
        } else {
            currentSheet.copy(lifeAssuredDetails = updatedPerson)
        }
        onDataChange(updatedSheet)
    }

    fun addNominee() {
        val currentSheet = _uiState.value.dataSheet.copy()
        currentSheet.proposerDetails.nominees.add(Nominee())
        onDataChange(currentSheet)
    }

    fun removeNominee(nominee: Nominee) {
        val currentSheet = _uiState.value.dataSheet.copy()
        currentSheet.proposerDetails.nominees.remove(nominee)
        onDataChange(currentSheet)
    }

    fun addPreviousPolicy() {
        val currentSheet = _uiState.value.dataSheet.copy()
        currentSheet.previousPolicies.add(PreviousPolicy())
        onDataChange(currentSheet)
    }

    fun removePreviousPolicy(policy: PreviousPolicy) {
        val currentSheet = _uiState.value.dataSheet.copy()
        currentSheet.previousPolicies.remove(policy)
        onDataChange(currentSheet)
    }

    private fun triggerSaveDraft() {
        if (_uiState.value.isReadOnly) return
        saveDraftJob?.cancel()
        saveDraftJob = viewModelScope.launch {
            delay(2000)
            saveDatasheetToApi()
        }
    }

    private suspend fun saveDatasheetToApi() {
        _uiState.update { it.copy(isSaving = true) }
        val currentSheet = _uiState.value.dataSheet
        try {
            val response = api.updateDatasheet(currentSheet.id, currentSheet.toApiPayload())
            if (response.isSuccessful) {
                _uiState.update { it.copy(isSaving = false, isNewDraft = false) }
            } else {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save draft.") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false, error = ErrorMapper.map(e)) }
        }
    }

    fun submitDataSheet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val currentSheet = _uiState.value.dataSheet
            try {
                val payload = currentSheet.toApiPayload().toMutableMap()
                payload["is_draft"] = false
                val response = api.updateDatasheet(currentSheet.id, payload)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isSaving = false) }
                } else {
                    _uiState.update { it.copy(isSaving = false, error = "Failed to submit form.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    private fun ClientDataSheet.toApiPayload(): Map<String, Any?> {
        return mapOf(
            "created_by_advisor_id" to createdByAdvisorId,
            "admin_id" to adminId,
            "is_draft" to isDraft,
            "is_archived" to isArchived,
            "sum_assured" to sumAssured,
            "mode" to mode,
            "date_of_commencement" to dateOfCommencement,
            "nach_date" to nachDate,
            "is_nach_mandatory" to isNachMandatory,
            "pwb_required" to pwbRequired,
            "initial_questions" to gson.fromJson<Map<String, Any?>>(gson.toJson(initialQuestions), mapType),
            "plan_details" to gson.fromJson<Map<String, Any?>>(gson.toJson(planDetails), mapType),
            "proposer_details" to gson.fromJson<Map<String, Any?>>(gson.toJson(proposerDetails), mapType),
            "life_assured_details" to gson.fromJson<Map<String, Any?>>(gson.toJson(lifeAssuredDetails), mapType),
            "previous_policies" to gson.fromJson<List<Map<String, Any?>>>(gson.toJson(previousPolicies), listMapType)
        )
    }
}
