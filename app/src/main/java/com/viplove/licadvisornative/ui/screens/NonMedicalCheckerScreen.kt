package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.ui.components.LabeledTextField
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.BrandDanger
import com.viplove.licadvisornative.ui.theme.BrandDangerBg
import com.viplove.licadvisornative.ui.theme.BrandSuccess
import com.viplove.licadvisornative.ui.theme.BrandSuccessBg
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.NonMedicalCheckerViewModel
import com.viplove.licadvisornative.util.NonMedicalCheckerLogic
import androidx.compose.ui.res.stringResource

@Composable
fun NonMedicalCheckerScreen(
    checkerViewModel: NonMedicalCheckerViewModel = viewModel()
) {
    val uiState by checkerViewModel.uiState.collectAsState()
    val availablePlans = remember { NonMedicalCheckerLogic.getAvailablePlans() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenHPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
    ) {
        item {
            SectionCard(title = stringResource(R.string.checker_personal_income_details)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
                    LabeledTextField(
                        value = uiState.age,
                        onValueChange = { checkerViewModel.onAgeChange(it) },
                        label = stringResource(R.string.age_label),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        isMono = true
                    )
                    LabeledTextField(
                        value = uiState.income,
                        onValueChange = { checkerViewModel.onIncomeChange(it) },
                        label = stringResource(R.string.annual_income_label),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        isMono = true
                    )
                }
                CheckerToggleRow(
                    label = stringResource(R.string.resident_indian_checkbox),
                    checked = uiState.isResidentIndian,
                    onCheckedChange = { checkerViewModel.onIsResidentChange(it) }
                )
                CheckerToggleRow(
                    label = stringResource(R.string.is_minor_checkbox),
                    checked = uiState.isMinor,
                    onCheckedChange = { checkerViewModel.onIsMinorChange(it) }
                )
                CheckerToggleRow(
                    label = stringResource(R.string.is_major_student_checkbox),
                    checked = uiState.isStudent,
                    onCheckedChange = { checkerViewModel.onIsStudentChange(it) }
                )
            }
        }

        item {
            SectionCard(title = stringResource(R.string.checker_qual_occupation_details)) {
                FilterDropdown(
                    label = stringResource(R.string.qualification_label),
                    options = listOf("Post Graduate", "Graduate", "Professional", "HSC / Plus 2", "SSC / 10th", "Others"),
                    selectedOption = uiState.qualification,
                    onOptionSelected = { checkerViewModel.onQualificationChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                FilterDropdown(
                    label = stringResource(R.string.profession_label),
                    options = listOf("Employed", "Business", "Professionals", "Others"),
                    selectedOption = uiState.profession,
                    onOptionSelected = { checkerViewModel.onProfessionChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            SectionCard(title = stringResource(R.string.checker_plan_details)) {
                FilterDropdown(
                    label = stringResource(R.string.select_plan_number_label),
                    options = availablePlans,
                    selectedOption = uiState.planNumber,
                    onOptionSelected = { checkerViewModel.onPlanNumberChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Button(
                onClick = { checkerViewModel.checkEligibility() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(R.string.check_eligibility_button))
            }
        }

        uiState.resultCategory?.let { category ->
            item {
                CheckerResultCard(
                    category = category,
                    saLimit = uiState.resultSaLimit
                )
            }
        }
    }
}

@Composable
private fun CheckerToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CheckerResultCard(
    category: String,
    saLimit: Int?
) {
    val eligible = category != "Ineligible"
    SectionCard(title = "Result") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.GutterMd),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = if (eligible) BrandSuccess else BrandDanger,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (eligible) Icons.Filled.Check else Icons.Filled.Cancel,
                        contentDescription = if (eligible) "Eligible" else "Not eligible"
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (eligible) "Eligible · $category" else "Not eligible",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (eligible) BrandSuccess else BrandDanger,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (eligible) "Max sum assured: ₹${saLimit ?: 0} Lakhs"
                    else "The case does not fall under any non-medical category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
