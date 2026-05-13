// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/ReviewAndSubmitScreen.kt
package com.viplove.licadvisornative.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "ANANDA - DATA SHEET (As Per 2.0)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            InfoRow("NAME OF THE PROPOSER:", proposer.name)
            InfoRow("Name Of Life Assured:", if (dataSheet.initialQuestions.proposerIsLA) proposer.name else lifeAssured.name)
            val dob = proposer.dateOfBirth?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: ""
            InfoRow("Date Of Birth:", dob)
            InfoRow("Gender:", proposer.gender)
            InfoRow("Contact No:", proposer.contactNo)
            InfoRow("Email ID:", proposer.emailId)
        }

        item {
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

        item {
            InfoRow("ADHAR CARD NO:", proposer.adharCardNo)
            InfoRow("FATHER NAME:", proposer.fatherName)
            InfoRow("MOTHER NAME:", proposer.motherName)
            InfoRow("MARITAL STATUS:", proposer.maritalStatus)
            InfoRow("TAX ASSESSEE:", proposer.taxAssessee)
            InfoRow("PAN CARD NO:", proposer.panCardNo)
            InfoRow("PAN NAME:", proposer.panName)
            InfoRow("ADDRESS (BOND DELIVERY):", "${proposer.communicationAddress.line1}, ${proposer.communicationAddress.city}")
        }

        if (proposer.gender == "Female" || lifeAssured.gender == "Female") {
            item {
                SectionTitle("FEMALE INSURED INFORMATION")
                val female = if (proposer.gender == "Female") proposer else lifeAssured
                InfoRow("ARE YOU PREGNANT NOW:", if (female.isPregnant) "Yes" else "No")
                val lastDelivery = female.dateOfLastDelivery?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: ""
                InfoRow("DATE OF LAST DELIVERY:", lastDelivery)
                InfoRow("DETAILS OF MISCARRIAGE/CESAREAN:", female.miscarriageDetails)
            }
        }

        item {
            InfoRow("OCCUPATION:", proposer.occupation)
            InfoRow("NATURE OF DUTIES:", proposer.natureOfDuties)
            InfoRow("LENGTH OF SERVICE:", proposer.lengthOfService)
            InfoRow("NAME OF EMPLOYER:", proposer.employerName)
            InfoRow("ANNUAL INCOME:", proposer.annualIncome)
            InfoRow("EDUCATION:", proposer.education)
        }

        item {
            SectionTitle("FAMILY HISTORY")
            FamilyHistoryTable(proposer.familyHistory)
        }

        item {
            SectionTitle("BANK DETAILS")
            BankDetailsTable(listOf(proposer.clientBankDetails))
        }

        item {
            ThreeColumnRow(
                col1 = "HEIGHT (CM): ${proposer.heightCm}",
                col2 = "WEIGHT (KG): ${proposer.weightKg}",
                col3 = "STATE OF HEALTH: ${proposer.stateOfHealth}"
            )
        }

        item {
            SectionTitle("NOMINEE DETAILS")
            NomineeDetailsTable(proposer.nominees)
        }

        if (dataSheet.isNachMandatory == "Yes") {
            item {
                SectionTitle("NOMINEE BANK DETAILS (DIVISION CASE MANDATORY)")
                BankDetailsTable(listOf(proposer.clientNachBankDetails))
            }
        }

        proposer.appointee?.let {
            if (it.name.isNotBlank()) {
                item {
                    SectionTitle("APPOINTEE DETAILS")
                    AppointeeTable(it)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(
                text = label,
                modifier = Modifier.weight(0.4f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = value,
                modifier = Modifier.weight(0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ThreeColumnRow(col1: String, col2: String, col3: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(col1, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(col2, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(col3, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color.Black, thickness = 1.dp)
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
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Column(modifier = Modifier.border(BorderStroke(1.dp, Color.Black))) {
            // 3. Create a clean header specifically for living relatives.
            Row(Modifier.fillMaxWidth()) {
                TableCell(text = "RELATION", weight = 0.5f, isHeader = true)
                TableCell(text = "AGE", weight = 0.5f, isHeader = true)
            }
            HorizontalDivider(color = Color.Black, thickness = 1.dp)

            // 4. Loop *only* through the living relatives that were entered.
            livingRelatives.forEach { member ->
                Row(Modifier.fillMaxWidth()) {
                    TableCell(text = member.relation.uppercase(), weight = 0.5f)
                    TableCell(text = member.age, weight = 0.5f)
                }
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
            }
        }
    }

    // 5. Only display the 'Deceased Relatives' table if there are any.
    if (deceasedRelatives.isNotEmpty()) {
        Text(
            "Deceased Relatives",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Column(modifier = Modifier.border(BorderStroke(1.dp, Color.Black))) {
            // 6. Create a different header for deceased relatives.
            Row(Modifier.fillMaxWidth()) {
                TableCell(text = "RELATION", weight = 0.3f, isHeader = true)
                TableCell(text = "AGE AT DEATH", weight = 0.3f, isHeader = true)
                TableCell(text = "CAUSE OF DEATH", weight = 0.4f, isHeader = true)
            }
            HorizontalDivider(color = Color.Black, thickness = 1.dp)

            // 7. Loop *only* through the deceased relatives.
            deceasedRelatives.forEach { member ->
                Row(Modifier.fillMaxWidth()) {
                    TableCell(text = member.relation.uppercase(), weight = 0.3f)
                    TableCell(text = member.ageAtDeath, weight = 0.3f)
                    TableCell(text = member.causeOfDeath, weight = 0.4f)
                }
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
            }
        }
    }
}

@Composable
fun BankDetailsTable(bankDetailsList: List<com.viplove.licadvisornative.model.BankDetails>) {
    Column(modifier = Modifier.border(BorderStroke(1.dp, Color.Black))) {
        Row(Modifier.fillMaxWidth()) {
            TableCell(text = "BANK NAME", weight = 0.25f, isHeader = true)
            TableCell(text = "IFSC CODE", weight = 0.25f, isHeader = true)
            TableCell(text = "ACCOUNT NO", weight = 0.25f, isHeader = true)
            TableCell(text = "ACCOUNT TYPE", weight = 0.25f, isHeader = true)
        }
        HorizontalDivider(color = Color.Black, thickness = 1.dp)
        bankDetailsList.forEach {
            Row(Modifier.fillMaxWidth()) {
                TableCell(text = it.bankName, weight = 0.25f)
                TableCell(text = it.ifscCode, weight = 0.25f)
                TableCell(text = it.accountNo, weight = 0.25f)
                TableCell(text = it.accountType, weight = 0.25f)
            }
            HorizontalDivider(color = Color.Black, thickness = 1.dp)
        }
    }
}

@Composable
fun NomineeDetailsTable(nominees: List<Nominee>) {
    Column(modifier = Modifier.border(BorderStroke(1.dp, Color.Black))) {
        Row(Modifier.fillMaxWidth()) {
            TableCell(text = "NOMINEE NAME", weight = 0.25f, isHeader = true)
            TableCell(text = "AGE", weight = 0.15f, isHeader = true)
            TableCell(text = "RELATION", weight = 0.2f, isHeader = true)
            TableCell(text = "PERCENTAGE OF SHARE", weight = 0.2f, isHeader = true)
            TableCell(text = "CONTACT NO", weight = 0.2f, isHeader = true)
        }
        HorizontalDivider(color = Color.Black, thickness = 1.dp)
        nominees.forEach {
            Row(Modifier.fillMaxWidth()) {
                TableCell(text = it.name, weight = 0.25f)
                TableCell(text = it.age, weight = 0.15f)
                TableCell(text = it.relation, weight = 0.2f)
                TableCell(text = it.percentageOfShare, weight = 0.2f)
                TableCell(text = it.contactNo, weight = 0.2f)
            }
            HorizontalDivider(color = Color.Black, thickness = 1.dp)
        }
    }
}

@Composable
fun AppointeeTable(appointee: com.viplove.licadvisornative.model.Appointee) {
    Column(modifier = Modifier.border(BorderStroke(1.dp, Color.Black))) {
        Row(Modifier.fillMaxWidth()) {
            TableCell(text = "APPOINTEE NAME", weight = 0.33f, isHeader = true)
            TableCell(text = "AGE", weight = 0.33f, isHeader = true)
            TableCell(text = "RELATION", weight = 0.34f, isHeader = true)
        }
        HorizontalDivider(color = Color.Black, thickness = 1.dp)
        Row(Modifier.fillMaxWidth()) {
            TableCell(text = appointee.name, weight = 0.33f)
            TableCell(text = appointee.age, weight = 0.33f)
            TableCell(text = appointee.relation, weight = 0.34f)
        }
        HorizontalDivider(color = Color.Black, thickness = 1.dp)
    }
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
            .border(BorderStroke(0.5.dp, Color.Gray))
            .weight(weight)
            .padding(8.dp),
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
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
            Text("Review Your Details", style = MaterialTheme.typography.headlineMedium)
            Text("Please confirm all details are correct before submitting.", style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
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
        Text("Occupation Details", style = MaterialTheme.typography.titleMedium)
        // ... (Existing Occupation InfoRows remain the same)
        InfoRow("Occupation", person.occupation)
        InfoRow("Nature of Duties", person.natureOfDuties)
        InfoRow("Length of Service", person.lengthOfService)
        InfoRow("Employer Name", person.employerName)
        InfoRow("Annual Income", person.annualIncome)
        InfoRow("Education", person.education)
        InfoRow("Armed", person.armedForces)


        Spacer(modifier = Modifier.height(12.dp))
        Text("Family & Health", style = MaterialTheme.typography.titleMedium)
        InfoRow("Height (cm)", person.heightCm)
        InfoRow("Weight (kg)", person.weightKg)
        InfoRow("State of Health", person.stateOfHealth)
        // ADDED: The detailed family history table
        if (person.familyHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FamilyHistoryTable(family = person.familyHistory)
        }


        Spacer(modifier = Modifier.height(12.dp))
        Text("Bank & Nominee Details", style = MaterialTheme.typography.titleMedium)
        // ADDED: The bank details table
        Spacer(modifier = Modifier.height(8.dp))
        Text("Bank Account", style = MaterialTheme.typography.titleSmall)
        BankDetailsTable(bankDetailsList = listOf(person.clientBankDetails))

        // ADDED: The nominee details table
        if (person.nominees.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Nominees", style = MaterialTheme.typography.titleSmall)
            NomineeDetailsTable(nominees = person.nominees)
        }

        // ADDED: The appointee details if a nominee is a minor
        person.appointee?.let {
            if (it.name.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Appointee (for Minor Nominee)", style = MaterialTheme.typography.titleSmall)
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
            Text("Uploaded Documents", style = MaterialTheme.typography.titleMedium)
            documents.forEach { (label, uri) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(model = Uri.parse(uri)),
                        contentDescription = label,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                }
            }
        }
    }
}
