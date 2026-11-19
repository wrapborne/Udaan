package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.ui.components.LabeledTextField
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.BrandDanger
import com.viplove.licadvisornative.ui.theme.BrandSuccess
import com.viplove.licadvisornative.ui.theme.BrandWarning
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.NonMedicalCheckerViewModel
import com.viplove.licadvisornative.util.MedicalUnderwriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Medical checker: non-medical or MER, special reports, riders and financial underwriting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NonMedicalCheckerScreen(
    checkerViewModel: NonMedicalCheckerViewModel = viewModel()
) {
    val s by checkerViewModel.uiState.collectAsState()
    val age = checkerViewModel.ageFor(s)
    val minor = age != null && age < 18
    val single = s.planCode in listOf("717", "749", "883", "911")
    val female = s.gender == "female"
    var showDatePicker by remember { mutableStateOf(false) }
    val planLabels = MedicalUnderwriter.PLANS.map { (code, name) -> "$name ($code)" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenHPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
    ) {
        item {
            SectionCard(title = "Proposal") {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm), verticalAlignment = Alignment.CenterVertically) {
                    LabeledTextField(
                        value = s.dob,
                        onValueChange = { value -> checkerViewModel.update { it.copy(dob = value) } },
                        label = "Date of birth",
                        helper = "DD-MM-YYYY",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        isMono = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date of birth")
                            }
                        }
                    )
                    Choice(
                        options = listOf("male" to "Male", "female" to "Female"),
                        selected = s.gender,
                        onSelect = { value -> checkerViewModel.update { it.copy(gender = value, occupation = if (value == "male" && it.occupation == "housewife") "salaried" else it.occupation) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                FilterDropdown(
                    label = "Plan",
                    options = planLabels,
                    selectedOption = planLabels[MedicalUnderwriter.PLANS.indexOfFirst { it.first == s.planCode }.coerceAtLeast(0)],
                    onOptionSelected = { label -> checkerViewModel.update { it.copy(planCode = MedicalUnderwriter.PLANS[planLabels.indexOf(label)].first) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
                    LabeledTextField(
                        value = s.sumAssured, onValueChange = { v -> checkerViewModel.update { it.copy(sumAssured = v) } },
                        label = "Sum assured", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), isMono = true
                    )
                    if (single) {
                        LabeledTextField(
                            value = s.singlePremium, onValueChange = { v -> checkerViewModel.update { it.copy(singlePremium = v) } },
                            label = "Single premium", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), isMono = true
                        )
                    } else {
                        LabeledTextField(
                            value = s.priorSumAssured, onValueChange = { v -> checkerViewModel.update { it.copy(priorSumAssured = v) } },
                            label = "SA in last 2 years", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), isMono = true
                        )
                    }
                }
                if (s.planCode == "749") {
                    Choice(listOf("1" to "Option 1", "2" to "Option 2"), s.plan749Option, { v -> checkerViewModel.update { it.copy(plan749Option = v) } })
                }
            }
        }

        if (minor) {
            item {
                SectionCard(title = "Child") {
                    CheckRow("Premium waiver (PWB) on the proposer", s.minorWithPwb) { v -> checkerViewModel.update { it.copy(minorWithPwb = v) } }
                }
            }
        } else {
            item {
                SectionCard(title = "Person") {
                    Text("Lives in", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Choice(
                        options = listOf("resident" to "India", "nriGroupV" to "NRI · Group V", "nriOther" to "NRI · other"),
                        selected = s.residency,
                        onSelect = { v -> checkerViewModel.update { it.copy(residency = v) } }
                    )
                    val occupations = MedicalUnderwriter.OCCUPATIONS.filter { female || it.id != "housewife" }
                    FilterDropdown(
                        label = "Work",
                        options = occupations.map { it.label },
                        selectedOption = occupations.firstOrNull { it.id == s.occupation }?.label ?: occupations.first().label,
                        onOptionSelected = { label -> checkerViewModel.update { it.copy(occupation = occupations.first { o -> o.label == label }.id) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
                        FilterDropdown(
                            label = "Qualification",
                            options = MedicalUnderwriter.QUALIFICATIONS.map { it.label },
                            selectedOption = MedicalUnderwriter.QUALIFICATIONS.first { it.id == s.qualification }.label,
                            onOptionSelected = { label -> checkerViewModel.update { it.copy(qualification = MedicalUnderwriter.QUALIFICATIONS.first { q -> q.label == label }.id) } },
                            modifier = Modifier.weight(1f)
                        )
                        if (s.occupation != "housewife") {
                            LabeledTextField(
                                value = s.income, onValueChange = { v -> checkerViewModel.update { it.copy(income = v) } },
                                label = "Annual income", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), isMono = true
                            )
                        }
                    }
                    if (female && s.occupation != "housewife") {
                        CheckRow("Proof of income available", s.incomeProof) { v -> checkerViewModel.update { it.copy(incomeProof = v) } }
                    }
                    if (female && s.occupation == "housewife") {
                        CheckRow("Husband has matching insurance", s.husbandInsured) { v -> checkerViewModel.update { it.copy(husbandInsured = v) } }
                    }
                    CheckRow("Smoking, alcohol or tobacco", s.adverseHabits) { v -> checkerViewModel.update { it.copy(adverseHabits = v) } }
                }
            }
            item {
                SectionCard(title = "Riders") {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
                        LabeledTextField(
                            value = s.cihr, onValueChange = { v -> checkerViewModel.update { it.copy(cihr = v) } },
                            label = "CIHR sum assured", helper = "Only 714 / 715", keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f), isMono = true
                        )
                        LabeledTextField(
                            value = s.termRider, onValueChange = { v -> checkerViewModel.update { it.copy(termRider = v) } },
                            label = "Term rider SA", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), isMono = true
                        )
                    }
                    CheckRow("Premium waiver (PWB)", s.pwb) { v -> checkerViewModel.update { it.copy(pwb = v) } }
                }
            }
        }

        item {
            Button(onClick = { checkerViewModel.check() }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                Text("Check eligibility")
            }
        }

        if (s.errors.isNotEmpty()) {
            item { Text(s.errors.joinToString(" "), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }

        s.result?.let { r ->
            item { ResultCard(r) }
            if (r.riders.isNotEmpty()) item { RidersCard(r) }
            if (r.reasons.isNotEmpty() || r.notes.isNotEmpty()) {
                item {
                    SectionCard(title = "Why, and what else is needed") {
                        (r.reasons + r.notes).forEach { line ->
                            Text("•  $line", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            r.financial?.let { f -> item { FinancialCard(f) } }
            item {
                Text(
                    "Actual premium may be higher as per underwriting. Saral Jeevan Bima and health plans are not covered yet. CIHR is not added to the SUC; the term rider is.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimens.GutterXs)
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Input)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val text = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))
                        checkerViewModel.update { it.copy(dob = text) }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (id, label) ->
            SegmentedButton(
                selected = selected == id,
                onClick = { onSelect(id) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {}
            ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun KeyValue(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(r: MedicalUnderwriter.Result) {
    val ok = r.verdict == "nonMedical"
    val title = when (r.verdict) {
        "nonMedical" -> "No medical needed"
        "vmer" -> "Video MER needed"
        "jfmr" -> "Juvenile full medical"
        else -> if (r.reports.size == 1) "MER needed" else "MER + reports"
    }
    val tone = when (r.verdict) { "nonMedical" -> BrandSuccess; "vmer" -> BrandWarning; else -> BrandDanger }
    val subtitle = buildString {
        append(MedicalUnderwriter.CATEGORY_LABELS[r.category])
        if (r.category != "MINOR" && !r.term) append(" · Group ${if (r.group == 2) "II" else "I"}${if (r.groupConfirmed) "" else " (not confirmed)"}")
        append(if (r.limit != null) " · limit ${MedicalUnderwriter.money(r.limit)} at age ${r.age}" else " · no non-medical limit at age ${r.age}")
    }
    SectionCard(title = "Result") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.GutterMd)) {
            Surface(shape = CircleShape, color = tone, contentColor = Color.White, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (ok) Icons.Filled.Check else Icons.Filled.PriorityHigh, contentDescription = if (ok) "Non-medical" else "Medical needed")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tone)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (r.reports.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                r.reports.forEach { report ->
                    Surface(shape = RoundedCornerShape(Dimens.PillCorner), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                        Text(report, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyValue("Age (nearer birthday)", r.age.toString())
            KeyValue(if (r.single) "Sum assured" else "Sum under consideration", MedicalUnderwriter.money(r.suc))
            r.limit?.let { KeyValue("Non-medical limit", MedicalUnderwriter.money(it)) }
            r.roomLeft?.let { room ->
                KeyValue(if (room >= 0) "Room left" else "Above limit by", MedicalUnderwriter.money(kotlin.math.abs(room)), if (room >= 0) BrandSuccess else MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun RidersCard(r: MedicalUnderwriter.Result) {
    SectionCard(title = "Riders") {
        r.riders.forEachIndexed { index, rider ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(MedicalUnderwriter.RIDER_LABELS[rider.id] ?: rider.id, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(
                    when (rider.status) { "no" -> "Not allowed"; "ok" -> "Allowed"; "vmer" -> "With VMER"; else -> "With MER" },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = when (rider.status) { "no" -> MaterialTheme.colorScheme.error; "ok" -> BrandSuccess; else -> BrandWarning }
                )
            }
            Text(rider.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        r.financial?.cihrMax?.let { max ->
            Text(
                "CIHR income limit: ${MedicalUnderwriter.money(max)} (income × ${r.financial.cihrMultiple})" + if (r.financial.cihrWithin == false) " · above the limit" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (r.financial.cihrWithin == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FinancialCard(f: MedicalUnderwriter.Financial) {
    SectionCard(title = "Financial underwriting") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            f.maxInsurance?.let { KeyValue("Max insurance (income × ${f.multiple})", MedicalUnderwriter.money(it)) }
            KeyValue("Total insurance (TRSA)", MedicalUnderwriter.money(f.trsa))
            if (f.withinIncomeLimit == false) KeyValue("Above the income limit", "Reduce cover", MaterialTheme.colorScheme.error)
            val proofTitle = f.proof.substringBefore(":")
            KeyValue("Proof of income", proofTitle, if (proofTitle.startsWith("Not needed")) BrandSuccess else MaterialTheme.colorScheme.onSurface)
            if (f.proof.contains(":")) Text(f.proof.substringAfter(":").trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("If yearly premium of this and earlier policies is ₹1 lakh or more: ITR or bank statement (AML).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
