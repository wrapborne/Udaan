package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.ui.viewmodel.UlipPlanViewModel

@Composable
fun UlipPlanManagementScreen(
    viewModel: UlipPlanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newPlanNumber by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val onAdd = {
        viewModel.addPlanNumber(newPlanNumber)
        newPlanNumber = ""
        focusManager.clearFocus()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Manage ULIP Plan Numbers", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Add or remove plan numbers to control how policies are sorted during upload.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newPlanNumber,
                onValueChange = { newPlanNumber = it.filter { char -> char.isDigit() } },
                label = { Text("New Plan Number (e.g., 849)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onAdd, enabled = newPlanNumber.isNotBlank()) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.planNumbers) { plan ->
                UlipPlanCard(
                    planNumber = plan,
                    onRemove = { viewModel.removePlanNumber(plan) }
                )
            }
        }
    }
}

@Composable
fun UlipPlanCard(planNumber: String, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Plan: $planNumber",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRemove, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Plan")
            }
        }
    }
}
