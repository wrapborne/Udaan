package com.viplove.licadvisornative.model

import java.util.Date
import java.util.UUID

// --- NEW: Definition for PlanDetails ---
data class PlanDetails(
    val planName: String = "",
    val planNumber: String = "",
    val sumAssured: String = "",
    val policyTerm: String = "",
    val ppt: String = "", // Premium Paying Term
    val mode: String = ""
)

// --- A reusable class to hold all details for one person ---
data class PersonDetails(
    // Personal Info
    var name: String = "",
    var dateOfBirth: Long? = null,
    var gender: String = "",
    var contactNo: String = "",
    var emailId: String = "",
    var adharCardNo: String = "",
    var fatherName: String = "",
    var motherName: String = "",
    var maritalStatus: String = "Single",
    var spouseName: String = "",
    var taxAssessee: String = "No",
    var armedForces: String ="No",
    var panCardNo: String = "",
    var panName: String = "",
    var communicationAddress: Address = Address(),
    var permanentAddress: Address = Address(),
    var isPermanentAddressSame: Boolean = true,

    // Occupation Info
    var occupation: String = "",
    var natureOfDuties: String = "",
    var lengthOfService: String = "",
    var employerName: String = "",
    var annualIncome: String = "",
    var education: String = "",
    var isHousewife: Boolean = false,
    var husbandOccupation: String = "",
    var husbandAnnualIncome: String = "",
    var husbandPolicyDetails: String = "",

    // Female Insured Info
    var isPregnant: Boolean = false,
    var dateOfLastDelivery: Long? = null,
    var miscarriageDetails: String = "",

    // Child Info
    var educationDetails: String = "", // For class/college

    var aadhaarFrontUri: String? = null,
    var aadhaarBackUri: String? = null,
    var panCardUri: String? = null,
    var passportPhotoUri: String? = null,
    var bankProofUri: String? = null,
    var nachBankProofUri: String? = null,
    var incomeProofUri: String? = null,
    var birthCertificateUri: String? = null,

    // Health & Family
    var heightCm: String = "",
    var weightKg: String = "",
    var stateOfHealth: String = "",
    var familyHistory: MutableList<FamilyMember> = mutableListOf(),

    // Bank & Nominee (Only for Proposer)
    var clientBankDetails: BankDetails = BankDetails(),
    var clientNachBankDetails: BankDetails = BankDetails(),
    var nominees: MutableList<Nominee> = mutableListOf(),
    var appointee: Appointee? = null
)


// --- The Main Data Sheet ---
data class ClientDataSheet(
    var id: String = "",
    var createdByAdvisorId: String = "",
    var adminId: String = "",

    var isDraft: Boolean = true,
    var lastUpdated: Date? = null,
    var isArchived: Boolean = false,

    // Step 0: Initial Questions
    var initialQuestions: InitialQuestions = InitialQuestions(),

    // Plan Details
    var sumAssured: String = "",
    val planDetails: PlanDetails = PlanDetails(),
    val selectedPlan: PlanData? = null,
    val selectedTerm: Int? = null,
    var mode: String = "",
    var dateOfCommencement: Long? = null,
    var nachDate: Long? = null,
    var isNachMandatory: String = "No",
    var pwbRequired: Boolean = false,

    // Details for the two main roles
    var proposerDetails: PersonDetails = PersonDetails(),
    var lifeAssuredDetails: PersonDetails = PersonDetails(),

    // Other details
    var previousPolicies: MutableList<PreviousPolicy> = mutableListOf()
){    // ADD THIS CALCULATED PROPERTY
    val selectedPpt: Int?
        get() = selectedPlan?.termsAndPpts?.get(selectedTerm)
}

// --- Data class for the initial questions ---
data class InitialQuestions(
    var proposerGender: String = "Male",
    var isHousewife: Boolean = false,
    var proposerIsLA: Boolean = true,
    var lifeAssuredIs: String = "Spouse" // Spouse or Child
)


// --- All other sub-models ---
data class Address(
    var line1: String = "",
    var line2: String = "",
    var city: String = "",
    var district: String = "",
    var state: String = "",
    var pincode: String = ""
)

data class PreviousPolicy(
    var id: String = UUID.randomUUID().toString(),
    var details: String = ""
)

data class FamilyMember(
    var relation: String = "",
    var age: String = "",
    var isAlive: Boolean = true,
    var causeOfDeath: String = "",
    var ageAtDeath: String = ""
)

data class BankDetails(
    var bankName: String = "",
    var ifscCode: String = "",
    var accountNo: String = "",
    var accountType: String = "Savings"
)

data class Nominee(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var age: String = "",
    var relation: String = "",
    var percentageOfShare: String = "",
    var contactNo: String = "",
    var bankDetails: BankDetails = BankDetails()
)

data class Appointee(
    var name: String = "",
    var age: String = "",
    var relation: String = ""
)
