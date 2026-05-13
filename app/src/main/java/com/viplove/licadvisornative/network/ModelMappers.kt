package com.viplove.licadvisornative.network

import com.viplove.licadvisornative.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── User mapping ──

fun ApiUser.toUser(): User {
    return User(
        uid = id,
        email = email,
        name = name,
        phone = phone,
        profilePictureUrl = profilePicturePath ?: "",
        role = role,
        isApproved = isApproved,
        adminId = adminId ?: "",
        agencyCode = agencyCode ?: "",
        doCode = doCode ?: "",
        startDate = startDate?.let { parseTimestamp(it) }
    )
}

// ── Policy mapping ──

fun ApiPolicy.toPolicy(): Policy {
    return Policy(
        policyId = id,
        policyNumber = policyNumber,
        plan = plan ?: "",
        mode = mode ?: "",
        doc = doc ?: 0L,
        isAnanda = isAnanda,
        premium = premium,
        agentCode = agentCode,
        adminId = adminId,
        shortName = shortName ?: "",
        enachDate = enachDate ?: "",
        agentName = agentName ?: "",
        lastPremiumPaidDate = lastPremiumPaidDate
    )
}

fun Policy.toPayload(): PolicyPayload {
    return PolicyPayload(
        policyNumber = policyNumber,
        plan = plan,
        mode = mode,
        doc = doc,
        premium = premium,
        agentCode = agentCode,
        shortName = shortName,
        enachDate = enachDate,
        agentName = agentName,
        isAnanda = isAnanda
    )
}

// ── Premium Summary mapping ──

fun ApiPremiumSummary.toPremiumSummary(): PremiumSummary {
    return PremiumSummary(
        summaryId = id,
        reportMonth = reportMonth,
        fpSchPrem = fpSchPrem,
        fySchPrem = fySchPrem,
        adminId = adminId,
        agencyCode = agencyCode
    )
}

fun PremiumSummary.toPayload(): SummaryPayload {
    return SummaryPayload(
        reportMonth = reportMonth,
        fpSchPrem = fpSchPrem,
        fySchPrem = fySchPrem,
        agencyCode = agencyCode
    )
}

// ── Graphics mapping ──

fun ApiGraphicsTemplate.toGraphicTemplate(): GraphicTemplate {
    return GraphicTemplate(
        id = id,
        name = name,
        imageUrl = imageUrl,
        visibleToRole = visibleToRole
    )
}

fun ApiGraphicsFooter.toGraphicFooter(): GraphicFooter {
    return GraphicFooter(
        id = id,
        name = name,
        imageUrl = imageUrl
    )
}

// ── ULIP Policy mapping ──

fun ApiUlipPolicy.toUlipPolicy(): UlipPolicy {
    return UlipPolicy(
        id = id,
        policyNumber = policyNumber,
        policyHolderName = policyHolderName,
        nav = nav,
        units = units,
        fundValue = fundValue,
        asOfDate = asOfDate ?: 0L,
        agentCode = agentCode,
        adminId = adminId
    )
}

// ── Datasheet mapping ──

fun ApiDatasheet.toClientDataSheet(): ClientDataSheet {
    return ClientDataSheet(
        id = id,
        createdByAdvisorId = createdByAdvisorId,
        adminId = adminId,
        isDraft = isDraft,
        isArchived = isArchived,
        lastUpdated = updatedAt?.let { parseTimestamp(it)?.let { ts -> Date(ts) } },
        initialQuestions = initialQuestions?.toInitialQuestions() ?: InitialQuestions(),
        planDetails = planDetails?.toPlanDetails() ?: PlanDetails(),
        sumAssured = sumAssured ?: "",
        mode = mode ?: "",
        dateOfCommencement = dateOfCommencement,
        nachDate = nachDate,
        isNachMandatory = isNachMandatory ?: "No",
        pwbRequired = pwbRequired,
        proposerDetails = proposerDetails?.toPersonDetails() ?: PersonDetails(),
        lifeAssuredDetails = lifeAssuredDetails?.toPersonDetails() ?: PersonDetails(),
        previousPolicies = previousPolicies?.map { it.toPreviousPolicy() }?.toMutableList() ?: mutableListOf()
    )
}

private fun Map<String, Any>.toInitialQuestions(): InitialQuestions {
    return InitialQuestions(
        proposerGender = (this["proposerGender"] as? String) ?: "Male",
        isHousewife = (this["isHousewife"] as? Boolean) ?: false,
        proposerIsLA = (this["proposerIsLA"] as? Boolean) ?: true,
        lifeAssuredIs = (this["lifeAssuredIs"] as? String) ?: "Spouse"
    )
}

private fun Map<String, Any>.toPlanDetails(): PlanDetails {
    return PlanDetails(
        planName = (this["planName"] as? String) ?: "",
        planNumber = (this["planNumber"] as? String) ?: "",
        sumAssured = (this["sumAssured"] as? String) ?: "",
        policyTerm = (this["policyTerm"] as? String) ?: "",
        ppt = (this["ppt"] as? String) ?: "",
        mode = (this["mode"] as? String) ?: ""
    )
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any>.toPersonDetails(): PersonDetails {
    return PersonDetails(
        name = (this["name"] as? String) ?: "",
        dateOfBirth = (this["dateOfBirth"] as? Number)?.toLong(),
        gender = (this["gender"] as? String) ?: "",
        contactNo = (this["contactNo"] as? String) ?: "",
        emailId = (this["emailId"] as? String) ?: "",
        adharCardNo = (this["adharCardNo"] as? String) ?: "",
        fatherName = (this["fatherName"] as? String) ?: "",
        motherName = (this["motherName"] as? String) ?: "",
        maritalStatus = (this["maritalStatus"] as? String) ?: "Single",
        spouseName = (this["spouseName"] as? String) ?: "",
        taxAssessee = (this["taxAssessee"] as? String) ?: "No",
        armedForces = (this["armedForces"] as? String) ?: "No",
        panCardNo = (this["panCardNo"] as? String) ?: "",
        panName = (this["panName"] as? String) ?: "",
        communicationAddress = (this["communicationAddress"] as? Map<String, Any>)?.toAddress() ?: Address(),
        permanentAddress = (this["permanentAddress"] as? Map<String, Any>)?.toAddress() ?: Address(),
        isPermanentAddressSame = (this["isPermanentAddressSame"] as? Boolean) ?: true,
        occupation = (this["occupation"] as? String) ?: "",
        natureOfDuties = (this["natureOfDuties"] as? String) ?: "",
        lengthOfService = (this["lengthOfService"] as? String) ?: "",
        employerName = (this["employerName"] as? String) ?: "",
        annualIncome = (this["annualIncome"] as? String) ?: "",
        education = (this["education"] as? String) ?: "",
        isHousewife = (this["isHousewife"] as? Boolean) ?: false,
        husbandOccupation = (this["husbandOccupation"] as? String) ?: "",
        husbandAnnualIncome = (this["husbandAnnualIncome"] as? String) ?: "",
        husbandPolicyDetails = (this["husbandPolicyDetails"] as? String) ?: "",
        isPregnant = (this["isPregnant"] as? Boolean) ?: false,
        dateOfLastDelivery = (this["dateOfLastDelivery"] as? Number)?.toLong(),
        miscarriageDetails = (this["miscarriageDetails"] as? String) ?: "",
        educationDetails = (this["educationDetails"] as? String) ?: "",
        heightCm = (this["heightCm"] as? String) ?: "",
        weightKg = (this["weightKg"] as? String) ?: "",
        stateOfHealth = (this["stateOfHealth"] as? String) ?: "",
        familyHistory = (this["familyHistory"] as? List<Map<String, Any>>)?.map { it.toFamilyMember() }?.toMutableList() ?: mutableListOf(),
        clientBankDetails = (this["clientBankDetails"] as? Map<String, Any>)?.toBankDetails() ?: BankDetails(),
        clientNachBankDetails = (this["clientNachBankDetails"] as? Map<String, Any>)?.toBankDetails() ?: BankDetails(),
        nominees = (this["nominees"] as? List<Map<String, Any>>)?.map { it.toNominee() }?.toMutableList() ?: mutableListOf(),
        appointee = (this["appointee"] as? Map<String, Any>)?.toAppointee()
    )
}

private fun Map<String, Any>.toAddress(): Address {
    return Address(
        line1 = (this["line1"] as? String) ?: "",
        line2 = (this["line2"] as? String) ?: "",
        city = (this["city"] as? String) ?: "",
        district = (this["district"] as? String) ?: "",
        state = (this["state"] as? String) ?: "",
        pincode = (this["pincode"] as? String) ?: ""
    )
}

private fun Map<String, Any>.toBankDetails(): BankDetails {
    return BankDetails(
        bankName = (this["bankName"] as? String) ?: "",
        ifscCode = (this["ifscCode"] as? String) ?: "",
        accountNo = (this["accountNo"] as? String) ?: "",
        accountType = (this["accountType"] as? String) ?: "Savings"
    )
}

private fun Map<String, Any>.toFamilyMember(): FamilyMember {
    return FamilyMember(
        relation = (this["relation"] as? String) ?: "",
        age = (this["age"] as? String) ?: "",
        isAlive = (this["isAlive"] as? Boolean) ?: true,
        causeOfDeath = (this["causeOfDeath"] as? String) ?: "",
        ageAtDeath = (this["ageAtDeath"] as? String) ?: ""
    )
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any>.toNominee(): Nominee {
    return Nominee(
        id = (this["id"] as? String) ?: java.util.UUID.randomUUID().toString(),
        name = (this["name"] as? String) ?: "",
        age = (this["age"] as? String) ?: "",
        relation = (this["relation"] as? String) ?: "",
        percentageOfShare = (this["percentageOfShare"] as? String) ?: "",
        contactNo = (this["contactNo"] as? String) ?: "",
        bankDetails = (this["bankDetails"] as? Map<String, Any>)?.toBankDetails() ?: BankDetails()
    )
}

private fun Map<String, Any>.toAppointee(): Appointee {
    return Appointee(
        name = (this["name"] as? String) ?: "",
        age = (this["age"] as? String) ?: "",
        relation = (this["relation"] as? String) ?: ""
    )
}

private fun Map<String, Any>.toPreviousPolicy(): PreviousPolicy {
    return PreviousPolicy(
        id = (this["id"] as? String) ?: java.util.UUID.randomUUID().toString(),
        details = (this["details"] as? String) ?: ""
    )
}

// ── Circulars mapping ──

fun ApiCircular.toCircular(): com.viplove.licadvisornative.model.Circular {
    return com.viplove.licadvisornative.model.Circular(
        circularId = id,
        title = title,
        category = category,
        fileName = fileName,
        fileUrl = fileUrl,
        uploadedAt = try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.US).parse(createdAt ?: "")?.time ?: System.currentTimeMillis()
        } catch (_: Exception) { System.currentTimeMillis() }
    )
}

// ── Forms mapping ──

fun ApiForm.toForm(): com.viplove.licadvisornative.model.Form {
    return com.viplove.licadvisornative.model.Form(
        formId = id,
        title = title,
        category = category,
        fileName = fileName,
        fileUrl = fileUrl,
        uploadedAt = try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.US).parse(createdAt ?: "")?.time ?: System.currentTimeMillis()
        } catch (_: Exception) { System.currentTimeMillis() }
    )
}

// ── Helpers ──

private fun parseTimestamp(dateString: String): Long? {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
        format.parse(dateString)?.time
    } catch (e: Exception) {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            format.parse(dateString)?.time
        } catch (e2: Exception) {
            null
        }
    }
}
