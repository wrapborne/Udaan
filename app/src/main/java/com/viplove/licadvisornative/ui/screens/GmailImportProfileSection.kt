package com.viplove.licadvisornative.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.network.GmailImportHistoryItem
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.theme.BrandDanger
import com.viplove.licadvisornative.ui.theme.BrandSuccess
import com.viplove.licadvisornative.ui.viewmodel.GmailImportStatusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GmailImportProfileSection(
    statusViewModel: GmailImportStatusViewModel = viewModel()
) {
    val context = LocalContext.current
    val statusState by statusViewModel.uiState.collectAsState()
    var showHistoryDialog by remember { mutableStateOf(false) }
    var notificationsAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> notificationsAllowed = granted }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text("Gmail Import & Reminders", fontWeight = FontWeight.Bold)
                Text(
                    "Due list and commission bill updates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = {
                    if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                label = { Text(if (notificationsAllowed) "On" else "Permission off") }
            )
        }

        Text(
            "The app reminds you if due list or commission bill data is not updated for 15 days. If no new import is done, it reminds again after 2 days.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        GmailImportStatusSummary(statusState)

        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GMAIL_IMPORT_PORTAL_URL)))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text("Open Gmail Import")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
        ) {
            OutlinedButton(
                onClick = { showHistoryDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("History")
            }
            OutlinedButton(
                onClick = statusViewModel::refresh,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Refresh")
            }
        }

        if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(
                onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Enable reminders")
            }
        }
    }

    if (showHistoryDialog) {
        GmailImportHistoryDialog(
            items = statusState.items,
            isLoading = statusState.isLoading,
            error = statusState.error,
            onRefresh = statusViewModel::refresh,
            onDismiss = { showHistoryDialog = false }
        )
    }
}

private const val GMAIL_IMPORT_PORTAL_URL = "https://udaan.viplovesaini.in"
private const val IMPORT_STALE_MS = 15L * 24 * 60 * 60 * 1000

@Composable
private fun GmailImportStatusSummary(state: GmailImportStatusViewModel.UiState) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.GutterSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.GutterXs)
        ) {
            when {
                state.isLoading -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                        Text("Loading import status...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.error != null -> {
                    ImportStatusLine("Gmail import status", state.error, isStale = true)
                }
                else -> {
                    val due = state.items.latestApplied("PREMIUM_DUE_LIST")
                    val commission = state.items.latestApplied("COMMISSION_BILL")
                    ImportStatusLine("Last due list", due.shortStatus(), due.isMissingOrStale())
                    ImportStatusLine("Last commission bill", commission.shortStatus(), commission.isMissingOrStale())
                }
            }
        }
    }
}

@Composable
private fun ImportStatusLine(label: String, value: String, isStale: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isStale) Icons.Default.Error else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isStale) BrandDanger else BrandSuccess
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GmailImportHistoryDialog(
    items: List<GmailImportHistoryItem>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import history") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
            ) {
                when {
                    isLoading -> Text("Loading import history...")
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                    items.isEmpty() -> Text("No Gmail imports yet.")
                    else -> items.forEach { item -> GmailImportHistoryRow(item) }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun GmailImportHistoryRow(item: GmailImportHistoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.GutterSm),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.type.displayType(), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                AssistChip(onClick = {}, label = { Text(item.status.ifBlank { "UNKNOWN" }) })
            }
            Text(
                listOf(item.reportMonth, formatImportTime(item.updatedAt)).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                historySummary(item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.error.isNotBlank()) {
                Text(item.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun List<GmailImportHistoryItem>.latestApplied(type: String): GmailImportHistoryItem? {
    return firstOrNull { it.type == type && it.status == "APPLIED" }
}

private fun GmailImportHistoryItem?.shortStatus(): String {
    if (this == null) return "Not imported yet"
    return "${formatImportTime(updatedAt)} · ${displayRows()} rows"
}

private fun GmailImportHistoryItem?.isMissingOrStale(): Boolean {
    if (this == null || updatedAt <= 0L) return true
    return System.currentTimeMillis() - updatedAt >= IMPORT_STALE_MS
}

private fun GmailImportHistoryItem.displayRows(): Int {
    return if (importedRows > 0) importedRows else rowCount
}

private fun historySummary(item: GmailImportHistoryItem): String {
    return listOf(
        "${item.displayRows()} rows",
        "${item.updatedPolicies} policies updated",
        "${item.clearedDueItems} dues cleared",
        "${item.reconciledDueItems} reconciled",
        "${item.reversalRows} reversals"
    ).joinToString(" · ")
}

private fun String.displayType(): String {
    return when (this) {
        "PREMIUM_DUE_LIST" -> "Premium Due List"
        "COMMISSION_BILL" -> "Commission Bill"
        else -> "Unknown PDF"
    }
}

private fun formatImportTime(value: Long): String {
    if (value <= 0L) return ""
    return SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(value))
}
