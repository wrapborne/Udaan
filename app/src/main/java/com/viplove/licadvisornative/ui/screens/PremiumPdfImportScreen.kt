package com.viplove.licadvisornative.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.model.CommissionBillPdfResult
import com.viplove.licadvisornative.model.CommissionRowStatus
import com.viplove.licadvisornative.model.LicPremiumPdfParseResult
import com.viplove.licadvisornative.model.PremiumDuePdfResult
import com.viplove.licadvisornative.model.PremiumYearType
import com.viplove.licadvisornative.ui.components.EmptyState
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.components.StatTile
import com.viplove.licadvisornative.ui.theme.BrandDanger
import com.viplove.licadvisornative.ui.theme.BrandGold
import com.viplove.licadvisornative.ui.theme.BrandSuccess
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.PremiumPdfImportViewModel
import java.util.Locale

@Composable
fun PremiumPdfImportScreen(
    importViewModel: PremiumPdfImportViewModel = viewModel(),
    onImportApplied: () -> Unit = {}
) {
    val state by importViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { importViewModel.parsePdf(it, context.contentResolver) }
        }
    )

    LaunchedEffect(state.applyResult) {
        if (state.applyResult != null) onImportApplied()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenHPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
    ) {
        item {
            SectionCard(title = "PDF Import") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
                    Text(
                        "Import LIC premium due lists or commission bills. Review parsed rows before updating policy dues and payment history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { pdfPicker.launch("application/pdf") },
                            enabled = !state.isParsing && !state.isApplying
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("Select PDF")
                        }
                        if (state.parseResult != null) {
                            OutlinedButton(
                                onClick = { importViewModel.clear() },
                                enabled = !state.isParsing && !state.isApplying
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }

        if (state.isParsing) {
            item { LoadingCard("Reading PDF...") }
        }

        state.error?.let { message ->
            item {
                StatusCard(
                    icon = Icons.Default.Error,
                    title = "Import issue",
                    message = message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        state.applyResult?.let { result ->
            item {
                StatusCard(
                    icon = Icons.Default.CheckCircle,
                    title = "Import applied",
                    message = "Rows: ${result.importedRows}, policies updated: ${result.updatedPolicies}, dues cleared: ${result.clearedDueItems}, reversals: ${result.reversalRows}",
                    color = BrandSuccess
                )
            }
        }

        when (val result = state.parseResult) {
            is LicPremiumPdfParseResult.PremiumDueList -> {
                item {
                    PremiumDueReview(
                        result = result.result,
                        isApplying = state.isApplying,
                        onApply = importViewModel::applyCurrentImport
                    )
                }
            }
            is LicPremiumPdfParseResult.CommissionBill -> {
                item {
                    CommissionBillReview(
                        result = result.result,
                        isApplying = state.isApplying,
                        onApply = importViewModel::applyCurrentImport
                    )
                }
            }
            is LicPremiumPdfParseResult.Error, null -> {
                if (!state.isParsing && state.error == null) {
                    item {
                        EmptyState(
                            icon = Icons.Default.PictureAsPdf,
                            title = "No PDF selected",
                            message = "Select a premium due list or commission bill to preview import rows."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumDueReview(
    result: PremiumDuePdfResult,
    isApplying: Boolean,
    onApply: () -> Unit
) {
    SectionCard(title = "Premium Due List") {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)) {
            ImportHeader(result.agentName, result.agentCode, result.reportMonth)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
                StatTile("Due rows", result.rows.size.toString(), modifier = Modifier.weight(1f))
                StatTile("Lapsed", result.rows.count { it.isLapsed }.toString(), modifier = Modifier.weight(1f), accent = BrandDanger)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
                StatTile("FY", result.rows.count { it.premiumYearType == PremiumYearType.FIRST_YEAR }.toString(), modifier = Modifier.weight(1f), accent = BrandGold)
                StatTile("Renewal", result.rows.count { it.premiumYearType == PremiumYearType.RENEWAL }.toString(), modifier = Modifier.weight(1f), accent = BrandSuccess)
            }
            ReviewWarnings(result.warnings)
            HorizontalDivider()
            result.rows.take(20).forEach { row ->
                ReviewRow(
                    title = "${row.policyNumber} - ${row.policyHolderName}",
                    subtitle = "FUP ${row.fupMonth} | ${row.premiumYearType.displayName()} | ${row.mode}",
                    trailing = "${money(row.totalPremium)}${if (row.isLapsed) " | LAPSED" else ""}",
                    danger = row.isLapsed
                )
            }
            if (result.rows.size > 20) {
                Text(
                    "+ ${result.rows.size - 20} more rows will be imported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ApplyButton(
                label = "Confirm Due Import",
                isApplying = isApplying,
                enabled = result.rows.isNotEmpty(),
                onApply = onApply
            )
        }
    }
}

@Composable
private fun CommissionBillReview(
    result: CommissionBillPdfResult,
    isApplying: Boolean,
    onApply: () -> Unit
) {
    val paidRows = result.rows.filter { it.status == CommissionRowStatus.PAID }
    val reversalRows = result.rows.filter { it.status == CommissionRowStatus.COOLING_OFF_REVERSAL }
    SectionCard(title = "Commission Bill") {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)) {
            ImportHeader(result.agentName, result.agentCode, "${result.reportMonth} ${result.batch}".trim())
            if (result.processedDate.isNotBlank()) {
                Text(
                    "Processed on ${result.processedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
                StatTile("Paid", paidRows.size.toString(), modifier = Modifier.weight(1f), accent = BrandSuccess)
                StatTile("Reversals", reversalRows.size.toString(), modifier = Modifier.weight(1f), accent = BrandDanger)
            }
            ReviewWarnings(result.warnings)
            HorizontalDivider()
            result.rows.take(20).forEach { row ->
                val isReversal = row.status == CommissionRowStatus.COOLING_OFF_REVERSAL
                ReviewRow(
                    title = "${row.policyNumber} - ${row.policyHolderName}",
                    subtitle = "Due ${row.dueDate} | Adj ${row.adjustmentDate} | ${row.planTerm}",
                    trailing = "${money(row.premium)}${if (isReversal) " | REVERSAL" else ""}",
                    danger = isReversal
                )
            }
            if (result.rows.size > 20) {
                Text(
                    "+ ${result.rows.size - 20} more rows will be imported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ApplyButton(
                label = "Confirm Payment Import",
                isApplying = isApplying,
                enabled = result.rows.isNotEmpty(),
                onApply = onApply
            )
        }
    }
}

@Composable
private fun ImportHeader(agentName: String, agentCode: String, period: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(agentName.ifBlank { "Unknown advisor" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            listOf(agentCode, period).filter { it.isNotBlank() }.joinToString(" | "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReviewWarnings(warnings: List<String>) {
    if (warnings.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GutterXs)) {
        warnings.take(3).forEach { warning ->
            AssistChip(
                onClick = {},
                label = { Text(warning) },
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }
        if (warnings.size > 3) {
            Text("+ ${warnings.size - 3} more warnings", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReviewRow(title: String, subtitle: String, trailing: String, danger: Boolean) {
    Surface(
        color = if (danger) BrandDanger.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.GutterSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = if (danger) BrandDanger else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ApplyButton(label: String, isApplying: Boolean, enabled: Boolean, onApply: () -> Unit) {
    Button(
        onClick = onApply,
        enabled = enabled && !isApplying,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isApplying) {
            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
        } else {
            Text(label)
        }
    }
}

@Composable
private fun LoadingCard(message: String) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator()
            Text(message)
        }
    }
}

@Composable
private fun StatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    color: androidx.compose.ui.graphics.Color
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun PremiumYearType.displayName(): String {
    return when (this) {
        PremiumYearType.FIRST_YEAR -> "FY"
        PremiumYearType.RENEWAL -> "ST"
        PremiumYearType.UNKNOWN -> "Unknown"
    }
}

private fun money(value: Double): String {
    return "Rs. ${String.format(Locale.US, "%.2f", value)}"
}
