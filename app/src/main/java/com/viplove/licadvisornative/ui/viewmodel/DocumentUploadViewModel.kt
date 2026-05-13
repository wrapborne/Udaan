// File: app/src/main/java/com/viplove/licadvisornative/ui/viewmodel/DocumentUploadViewModel.kt
package com.viplove.licadvisornative.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.viplove.licadvisornative.model.ClientDataSheet
import com.viplove.licadvisornative.model.PersonDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

class DocumentUploadViewModel : ViewModel() {

    data class DocumentSlot(
        val label: String,
        val isMandatory: Boolean,
        val personIdentifier: String,
        val documentType: String,
        val uploadedUri: String? = null
    )

    data class UiState(
        val documentSlots: List<DocumentSlot> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun generateDocumentSlots(dataSheet: ClientDataSheet) {
        val slots = mutableListOf<DocumentSlot>()
        val proposer = dataSheet.proposerDetails
        val lifeAssured = dataSheet.lifeAssuredDetails
        val initialQuestions = dataSheet.initialQuestions

        slots.addAll(addPersonSlots("Proposer", proposer, dataSheet, isProposer = true))

        if (!initialQuestions.proposerIsLA) {
            val prefix = if (initialQuestions.lifeAssuredIs == "Spouse") "Life Assured (Spouse)" else "Life Assured (Child)"
            slots.addAll(addPersonSlots(prefix, lifeAssured, dataSheet, isProposer = false))
        }

        _uiState.update { it.copy(documentSlots = slots) }
    }

    fun updateDocumentUri(dataSheet: ClientDataSheet, slot: DocumentSlot, uri: Uri): ClientDataSheet {
        val newUriString = uri.toString()
        var updatedProposer = dataSheet.proposerDetails
        var updatedLifeAssured = dataSheet.lifeAssuredDetails

        val personToUpdate = if (slot.personIdentifier == "proposer") updatedProposer else updatedLifeAssured

        // --- REFACTORED: Handles new document types for Aadhaar Front/Back ---
        val updatedPerson = when (slot.documentType) {
            "aadhaarFront" -> personToUpdate.copy(aadhaarFrontUri = newUriString)
            "aadhaarBack" -> personToUpdate.copy(aadhaarBackUri = newUriString)
            "pan" -> personToUpdate.copy(panCardUri = newUriString)
            "photo" -> personToUpdate.copy(passportPhotoUri = newUriString)
            "bank" -> personToUpdate.copy(bankProofUri = newUriString)
            "nach_bank" -> personToUpdate.copy(nachBankProofUri = newUriString)
            "income" -> personToUpdate.copy(incomeProofUri = newUriString)
            "birth_cert" -> personToUpdate.copy(birthCertificateUri = newUriString)
            else -> personToUpdate
        }

        if (slot.personIdentifier == "proposer") {
            updatedProposer = updatedPerson
            if (dataSheet.initialQuestions.proposerIsLA) updatedLifeAssured = updatedPerson
        } else {
            updatedLifeAssured = updatedPerson
        }

        return dataSheet.copy(proposerDetails = updatedProposer, lifeAssuredDetails = updatedLifeAssured)
    }

    private fun addPersonSlots(
        prefix: String,
        person: PersonDetails,
        dataSheet: ClientDataSheet,
        isProposer: Boolean
    ): List<DocumentSlot> {
        val slots = mutableListOf<DocumentSlot>()
        val initialQuestions = dataSheet.initialQuestions
        val personId = if(isProposer) "proposer" else "lifeAssured"

        // --- REFACTORED: Split Aadhaar into two separate slots ---
        slots.add(DocumentSlot("$prefix: Aadhaar (Front)", true, personId, "aadhaarFront", person.aadhaarFrontUri))
        slots.add(DocumentSlot("$prefix: Aadhaar (Back)", true, personId, "aadhaarBack", person.aadhaarBackUri))

        slots.add(DocumentSlot("$prefix: PAN Card", false, personId, "pan", person.panCardUri))
        slots.add(DocumentSlot("$prefix: Passport Photo", true, personId, "photo", person.passportPhotoUri))

        val isSpouse = !isProposer && initialQuestions.lifeAssuredIs == "Spouse"
        val isNachMandatory = dataSheet.isNachMandatory == "Yes"
        val bankProofMandatory = isProposer || isSpouse

        if (bankProofMandatory) {
            slots.add(DocumentSlot("$prefix: Bank Proof", true, personId, "bank", person.bankProofUri))
        } else if (isNachMandatory && isProposer) {
            slots.add(DocumentSlot("$prefix: Bank Proof", true, "proposer", "bank", person.bankProofUri))
        }

        val nachBankDifferent = dataSheet.proposerDetails.clientNachBankDetails.accountNo.isNotBlank() &&
                dataSheet.proposerDetails.clientNachBankDetails.accountNo != dataSheet.proposerDetails.clientBankDetails.accountNo
        if (isNachMandatory && nachBankDifferent) {
            slots.add(DocumentSlot("$prefix: NACH Bank Proof", true, "proposer", "nach_bank", person.nachBankProofUri))
        }

        slots.add(DocumentSlot("$prefix: Income Proof", false, personId, "income", person.incomeProofUri))

        if (!isProposer && initialQuestions.lifeAssuredIs == "Child") {
            val childAge = person.dateOfBirth?.let { dob ->
                val today = Calendar.getInstance()
                val birthDate = Calendar.getInstance().apply { timeInMillis = dob }
                var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) age--
                age
            } ?: 0

            if (childAge >= 12) {
                // Aadhaar is already split, so this logic is simpler
            } else {
                slots.add(DocumentSlot("$prefix: Birth Certificate", true, "lifeAssured", "birth_cert", person.birthCertificateUri))
                // Make Aadhaar optional for child under 12
                val frontIndex = slots.indexOfFirst { it.documentType == "aadhaarFront" && it.personIdentifier == "lifeAssured" }
                val backIndex = slots.indexOfFirst { it.documentType == "aadhaarBack" && it.personIdentifier == "lifeAssured" }
                if (frontIndex != -1) slots[frontIndex] = slots[frontIndex].copy(isMandatory = false)
                if (backIndex != -1) slots[backIndex] = slots[backIndex].copy(isMandatory = false)
            }
        }
        return slots
    }
}
