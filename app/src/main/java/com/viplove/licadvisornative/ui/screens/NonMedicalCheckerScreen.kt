// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/NonMedicalCheckerScreen.kt
package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.ui.viewmodel.NonMedicalCheckerViewModel
import com.viplove.licadvisornative.util.NonMedicalCheckerLogic

@Composable
fun NonMedicalCheckerScreen(
    checkerViewModel: NonMedicalCheckerViewModel = viewModel()
) {
    val uiState by checkerViewModel.uiState.collectAsState()
    val availablePlans = remember { NonMedicalCheckerLogic.getAvailablePlans() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.checker_personal_income_details), style = MaterialTheme.typography.titleLarge)
        }

        item {
            OutlinedTextField(
                value = uiState.age,
                onValueChange = { checkerViewModel.onAgeChange(it) },
                label = { Text(stringResource(R.string.age_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.income,
                onValueChange = { checkerViewModel.onIncomeChange(it) },
                label = { Text(stringResource(R.string.annual_income_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.isResidentIndian, onCheckedChange = { checkerViewModel.onIsResidentChange(it) })
                Text(stringResource(R.string.resident_indian_checkbox))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.isMinor, onCheckedChange = { checkerViewModel.onIsMinorChange(it) })
                Text(stringResource(R.string.is_minor_checkbox))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.isStudent, onCheckedChange = { checkerViewModel.onIsStudentChange(it) })
                Text(stringResource(R.string.is_major_student_checkbox))
            }
        }

        item {
            Text(stringResource(R.string.checker_qual_occupation_details), style = MaterialTheme.typography.titleLarge)
        }

        item {
            FilterDropdown(
                label = stringResource(R.string.qualification_label),
                options = listOf("Post Graduate", "Graduate", "Professional", "HSC / Plus 2", "SSC / 10th", "Others"),
                selectedOption = uiState.qualification,
                onOptionSelected = { checkerViewModel.onQualificationChange(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            FilterDropdown(
                label = stringResource(R.string.profession_label),
                options = listOf("Employed", "Business", "Professionals", "Others"),
                selectedOption = uiState.profession,
                onOptionSelected = { checkerViewModel.onProfessionChange(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(stringResource(R.string.checker_plan_details), style = MaterialTheme.typography.titleLarge)
        }

        item {
            FilterDropdown(
                label = stringResource(R.string.select_plan_number_label),
                options = availablePlans,
                selectedOption = uiState.planNumber,
                onOptionSelected = { checkerViewModel.onPlanNumberChange(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = { checkerViewModel.checkEligibility() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.check_eligibility_button))
            }
        }

        // --- Results Section ---
        if (uiState.resultCategory != null) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val category = uiState.resultCategory!!
                    if (category != "Ineligible") {
                        Text("Result", style = MaterialTheme.typography.headlineSmall)
                        Text("Eligible under: $category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Max Sum Assured: ₹${uiState.resultSaLimit} Lakhs", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text("Result", style = MaterialTheme.typography.headlineSmall)
                        Text("Not Eligible", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("The case does not fall under any non-medical category.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
