// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/ReviewAndSubmitScreen.kt
package com.viplove.licadvisornative.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.viplove.licadvisornative.model.ClientDataSheet
import com.viplove.licadvisornative.model.FamilyMember
import com.viplove.licadvisornative.model.Nominee
import com.viplove.licadvisornative.model.PersonDetails
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.BrandGold
import com.viplove.licadvisornative.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataSheetView(dataSheet: ClientDataSheet) {
    val proposer = dataSheet.proposerDetails
    val lifeAssured = dataSheet.lifeAssuredDetails

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(Dimens.ScreenHPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
    ) {
        item {
            SectionCard {
                Text(
                    "ANANDA - DATA SHEET",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    "As Per 2.0",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            ReviewSectionCard(title = "Client Details") {
                InfoRow("NAME OF THE PROPOSER:", proposer.name)
                InfoRow("Name Of Life Assured:", if (dataSheet.initialQuestions.proposerIsLA) proposer.name else lifeAssured.name)
                val dob = proposer.dateOfBirth?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: ""
                InfoRow("Date Of Birth:", dob)
                InfoRow("Gender:", proposer.gender)
                InfoRow("Contact No:", proposer.contactNo)
                InfoRow("Email ID:", proposer.emailId)
            }
        }

        item {
            ReviewSectionCard(title = "Plan Summary") {
                val planText = dataSheet.selectedPlan?.let { "${it.name} (${it.planNumber})" } ?: ""
                val termText = dataSheet.selectedTerm?.let { "T: $it" } ?: ""
                val pptText = dataSheet.selectedPpt?.let { "PPT: $it" } ?: ""
                ThreeColumnRow(
                    col1 = "SUM ASSURED: ${dataSheet.sumAssured}",
                    col2 = "PLAN: $planText",
                    col3 = "MODE: ${dataSheet.mode}"
                )
                val doc = dataSheet.dateOfCommencement?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: ""
                ThreeColumnRow(
                    col1 = "$termText / $pptText",
                    col2 = "",
                    col3 = "DATE OF COMMENCEMENT: $doc"
                )
            }
        }

        item {
            ReviewSectionCard(title = "Identity & Address") {
                InfoRow("ADHAR CARD NO:", proposer.adharCardNo)
                InfoRow("FATHER NAME:", proposer.fatherName)
                InfoRow("MOTHER NAME:", proposer.motherName)
                InfoRow("MARITAL STATUS:", proposer.maritalStatus)
                InfoRow("TAX ASSESSEE:", proposer.taxAssessee)
                InfoRow("PAN CARD NO:", proposer.panCardNo)
                InfoRow("PAN NAME:", proposer.panName)
                InfoRow("ADDRESS (BOND DELIVERY):", "${proposer.communicationAddress.line1}, ${proposer.communicationAddress.city}")
            }
        }

        if (proposer.gender == "Female" || lifeAssured.gender == "Female") {
            item {
                ReviewSectionCard(title = "Female Insured Information") {
                    val female = if (proposer.gender == "Female") proposer else lifeAssured
                    InfoRow("ARE YOU PREGNANT NOW:", if (female.isPregnant) "Yes" else "No")
                    val lastDelivery = female.dateOfLastDelivery?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: ""
                    InfoRow("DATE OF LAST DELIVERY:", lastDelivery)
                    InfoRow("DETAILS OF MISCARRIAGE/CESAREAN:", female.miscarriageDetails)
                }
            }
        }

        item {
            ReviewSectionCard(title = "Occupation Details") {
                InfoRow("OCCUPATION:", proposer.occupation)
                InfoRow("NATURE OF DUTIES:", proposer.natureOfDuties)
                InfoRow("LENGTH OF SERVICE:", proposer.lengthOfService)
                InfoRow("NAME OF EMPLOYER:", proposer.employerName)
                InfoRow("ANNUAL INCOME:", proposer.annualIncome)
                InfoRow("EDUCATION:", proposer.education)
            }
        }

        item {
            ReviewSectionCard(title = "Family History") {
                FamilyHistoryTable(proposer.familyHistory)
            }
        }

        item {
            ReviewSectionCard(title = "Bank Details") {
                BankDetailsTable(listOf(proposer.clientBankDetails))
            }
        }

        item {
            ReviewSectionCard(title = "Health Declaration") {
                ThreeColumnRow(
                    col1 = "HEIGHT (CM): ${proposer.heightCm}",
                    col2 = "WEIGHT (KG): ${proposer.weightKg}",
                    col3 = "STATE OF HEALTH: ${proposer.stateOfHealth}"
                )
            }
        }

        item {
            ReviewSectionCard(title = "Nominee Details") {
                NomineeDetailsTable(proposer.nominees)
            }
        }

        if (dataSheet.isNachMandatory == "Yes") {
            item {
                ReviewSectionCard(title = "Nominee Bank Details") {
                    Text(
                        "Division case mandatory",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BankDetailsTable(listOf(proposer.clientNachBankDetails))
                }
            }
        }

        proposer.appointee?.let {
            if (it.name.isNotBlank()) {
                item {
                    ReviewSectionCard(title = "Appointee Details") {
                        AppointeeTable(it)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.GutterSm),
        textAlign = TextAlign.Start
    )
}

@Composable
fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.GutterXs),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(0.4f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                modifier = Modifier.weight(0.6f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ThreeColumnRow(col1: String, col2: String, col3: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
            .padding(Dimens.GutterSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
    ) {
        Text(col1, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(col2, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(col3, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun FamilyHistoryTable(family: List<FamilyMember>) {
    // 1. Separate family members into two lists based on their status.
    //    We also filter out any entries where the age was left blank.
    val livingRelatives = family.filter { it.isAlive && it.age.isNotBlank() }
    val deceasedRelatives = family.filter { !it.isAlive && it.ageAtDeath.isNotBlank() }

    // 2. Only display the 'Living Relatives' table if there are any.
    if (livingRelatives.isNotEmpty()) {
        Text(
            "Living Relatives",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = Dimens.GutterSm, bottom = Dimens.GutterXs)
        )
        TableSurface {
            // 3. Create a clean header specifically for living relatives.
            Row(Modifier.fillMaxWidth()) {
                TableCell(text = "RELATION", weight = 0.5f, isHeader = true)
                TableCell(text = "AGE", weight = 0.5f, isHeader = true)
            }

            // 4. Loop *only* through the living relatives that were entered.
            livingRelatives.forEach { member ->
                Row(Modifier.fillMaxWidth()) {
                    TableCell(text = member.relation.uppercase(), weight = 0.5f)
                    TableCell(text = member.age, weight = 0.5f)
                }
            }
        }
    }

    // 5. Only display the 'Deceased Relatives' table if there are any.
    if (deceasedRelatives.isNotEmpty()) {
        Text(
            "Deceased Relatives",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = Dimens.GutterLg, bottom = Dimens.GutterXs)
        )
        TableSurface {
            // 6. Create a different header for deceased relatives.
            Row(Modifier.fillMaxWidth()) {
                TableCell(text = "RELATION", weight = 0.3f, isHeader = true)
                TableCell(text = "AGE AT DEATH", weight = 0.3f, isHeader = true)
                TableCell(text = "CAUSE OF DEATH", weight = 0.4f, isHeader = true)
            }

            // 7. Loop *only* through the deceased relatives.
            deceasedRelatives.forEach { member ->
                Row(Modifier.fillMaxWidth()) {
                    TableCell(text = member.relation.uppercase(), weight = 0.3f)
                    TableCell(text = member.ageAtDeath, weight = 0.3f)
                    TableCell(text = member.causeOfDeath, weight = 0.4f)
                }
            }
        }
    }
}

@Composable
fun BankDetailsTable(bankDetailsList: List<com.viplove.licadvisornative.model.BankDetails>) {
    TableSurface {
        Row(Modifier.fillMaxWidth()) {
            TableCell(text = "BANK NAME", weight = 0.25f, isHeader = true)
            TableCell(text = "IFSC CODE", weight = 0.25f, isHeader = true)
            TableCell(text = "ACCOUNT NO", weight = 0.25f, isHeader = true)
            TableCell(text = "ACCOUNT TYPE", weight = 0.25f, isHeader = true)
        }
        bankDetailsList.forEach {
            Row(Modifier.fillMaxWidth()) {
                TableCell(text = it.bankName, weight = 0.25f)
                TableCell(text = it.ifscCode, weight = 0.25f)
                TableCell(text = it.accountNo, weight = 0.25f)
                TableCell(text = it.accountType, weight = 0.25f)
            }
        }
    }
}

@Composable
fun NomineeDetailsTable(nominees: List<Nominee>) {
    TableSurface(minWidth = 680.dp) {
        Row(Modifier.fillMaxWidth()) {
            TableCell(text = "NOMINEE NAME", weight = 0.25f, isHeader = true)
            TableCell(text = "AGE", weight = 0.15f, isHeader = true)
            TableCell(text = "RELATION", weight = 0.2f, isHeader = true)
            TableCell(text = "PERCENTAGE OF SHARE", weight = 0.2f, isHeader = true)
            TableCell(text = "CONTACT NO", weight = 0.2f, isHeader = true)
        }
        nominees.forEach {
            Row(Modifier.fillMaxWidth()) {
                TableCell(text = it.name, weight = 0.25f)
                TableCell(text = it.age, weight = 0.15f)
                TableCell(text = it.relation, weight = 0.2f)
                TableCell(text = it.percentageOfShare, weight = 0.2f)
                TableCell(text = it.contactNo, weight = 0.2f)
            }
        }
    }
}

@Composable
fun AppointeeTable(appointee: com.viplove.licadvisornative.model.Appointee) {
    TableSurface {
        Row(Modifier.fillMaxWidth()) {
            TableCell(text = "APPOINTEE NAME", weight = 0.33f, isHeader = true)
            TableCell(text = "AGE", weight = 0.33f, isHeader = true)
            TableCell(text = "RELATION", weight = 0.34f, isHeader = true)
        }
        Row(Modifier.fillMaxWidth()) {
            TableCell(text = appointee.name, weight = 0.33f)
            TableCell(text = appointee.age, weight = 0.33f)
            TableCell(text = appointee.relation, weight = 0.34f)
        }
    }
}

@Composable
private fun TableSurface(
    minWidth: androidx.compose.ui.unit.Dp = 560.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .widthIn(min = minWidth),
        content = content
    )
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false
) {
    Text(
        text = text,
        Modifier
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant))
            .background(if (isHeader) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .weight(weight)
            .padding(Dimens.GutterSm),
        color = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = 10.sp
    )
}

@Composable
fun ReviewAndSubmitStep(dataSheet: ClientDataSheet) {
    val proposer = dataSheet.proposerDetails
    val lifeAssured = dataSheet.lifeAssuredDetails

    // Log the health details to check if they are being passed correctly
    Log.d(
        "ReviewDebug",
        "Proposer - Height=${proposer.heightCm}, Weight=${proposer.weightKg}, State=${proposer.stateOfHealth} | " +
                "Life Assured - Height=${lifeAssured.heightCm}, Weight=${lifeAssured.weightKg}, State=${lifeAssured.stateOfHealth}"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionCard {
                Text(
                    "Review Your Details",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Please confirm all details are correct before submitting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ReviewSectionCard(title = "Plan Details") {
                val planName = dataSheet.selectedPlan?.let { "${it.name} (${it.planNumber})" } ?: "Not selected"
                val term = dataSheet.selectedTerm?.let { "$it years" } ?: "Not selected"
                val ppt = dataSheet.selectedPpt?.let { "$it years" } ?: "Not selected"

                InfoRow("Plan", planName)
                InfoRow("Policy Term", term)
                InfoRow("Premium Paying Term", ppt)
                InfoRow("Sum Assured", dataSheet.sumAssured)
                InfoRow("Mode", dataSheet.mode)
                val doc = dataSheet.dateOfCommencement?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: "Not set"
                InfoRow("Date of Commencement", doc)
                if (dataSheet.isNachMandatory == "Yes") {
                    val nachDate = dataSheet.nachDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: "Not set"
                    InfoRow("NACH Date", nachDate)
                }
            }
        }

        item {
            val title = if (dataSheet.initialQuestions.proposerIsLA) "Proposer & Life Assured Details" else "Proposer Details"
            ReviewPersonCard(title = title, person = dataSheet.proposerDetails)
        }

        if (!dataSheet.initialQuestions.proposerIsLA) {
            item {
                val title = if (dataSheet.initialQuestions.lifeAssuredIs == "Spouse") "Life Assured (Spouse) Details" else "Life Assured (Child) Details"
                ReviewPersonCard(title = title, person = dataSheet.lifeAssuredDetails)
            }
        }
    }
}

@Composable
fun ReviewSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    SectionCard(
        title = title,
        trailing = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = BrandGold.copy(alpha = 0.18f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    text = "Review",
                    modifier = Modifier.padding(horizontal = Dimens.GutterSm, vertical = Dimens.GutterXs),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        content = content
    )
}

@Composable
private fun ReviewSubsection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = Dimens.GutterXs)
    )
}

@Composable
private fun DocumentThumb(label: String, uri: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), MaterialTheme.shapes.small)
            .padding(Dimens.GutterSm)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = Uri.parse(uri)),
            contentDescription = label,
            modifier = Modifier.size(44.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(Dimens.GutterSm))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ReviewPersonCard(title: String, person: PersonDetails) {
    Log.d("ReviewDebug", "Card: '$title', Family History Data: ${person.familyHistory}")
    ReviewSectionCard(title = title) {
        InfoRow("Name", person.name)
        InfoRow("Contact No", person.contactNo)
        InfoRow("Email", person.emailId)
        InfoRow("Aadhar Card No.", person.adharCardNo)
        InfoRow("PAN Card No.", person.panCardNo)
        InfoRow("Marital Status", person.maritalStatus)
        if (person.maritalStatus == "Married") {
            InfoRow("Spouse Name", person.spouseName)
        }
        InfoRow("Father Name", person.fatherName)
        InfoRow("Mother Name", person.motherName)
        InfoRow("IT Assessee", person.taxAssessee)

        Spacer(modifier = Modifier.height(12.dp))
        ReviewSubsection("Occupation Details")
        // ... (Existing Occupation InfoRows remain the same)
        InfoRow("Occupation", person.occupation)
        InfoRow("Nature of Duties", person.natureOfDuties)
        InfoRow("Length of Service", person.lengthOfService)
        InfoRow("Employer Name", person.employerName)
        InfoRow("Annual Income", person.annualIncome)
        InfoRow("Education", person.education)
        InfoRow("Armed", person.armedForces)


        Spacer(modifier = Modifier.height(12.dp))
        ReviewSubsection("Family & Health")
        InfoRow("Height (cm)", person.heightCm)
        InfoRow("Weight (kg)", person.weightKg)
        InfoRow("State of Health", person.stateOfHealth)
        // ADDED: The detailed family history table
        if (person.familyHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FamilyHistoryTable(family = person.familyHistory)
        }


        Spacer(modifier = Modifier.height(12.dp))
        ReviewSubsection("Bank & Nominee Details")
        // ADDED: The bank details table
        Spacer(modifier = Modifier.height(8.dp))
        ReviewSubsection("Bank Account")
        BankDetailsTable(bankDetailsList = listOf(person.clientBankDetails))

        // ADDED: The nominee details table
        if (person.nominees.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ReviewSubsection("Nominees")
            NomineeDetailsTable(nominees = person.nominees)
        }

        // ADDED: The appointee details if a nominee is a minor
        person.appointee?.let {
            if (it.name.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                ReviewSubsection("Appointee (for Minor Nominee)")
                AppointeeTable(appointee = it)
            }
        }

        val documents = listOfNotNull(
            person.aadhaarFrontUri?.let { "Aadhaar (Front)" to it },
            person.aadhaarBackUri?.let { "Aadhaar (Back)" to it },
            person.panCardUri?.let { "PAN Card" to it },
            person.passportPhotoUri?.let { "Passport Photo" to it },
            person.bankProofUri?.let { "Bank Proof" to it }
        )
        if (documents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ReviewSubsection("Uploaded Documents")
            documents.forEach { (label, uri) ->
                DocumentThumb(label = label, uri = uri)
            }
        }
    }
}
