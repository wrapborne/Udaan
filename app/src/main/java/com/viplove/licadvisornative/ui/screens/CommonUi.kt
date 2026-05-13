// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/CommonUi.kt
package com.viplove.licadvisornative.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PolicyCard(policy: Policy) {
    val formattedDate = remember(policy.doc) {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(policy.doc))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(policy.policyNumber, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(policy.plan, modifier = Modifier.weight(0.7f), fontSize = 14.sp)
        Text(formattedDate, modifier = Modifier.weight(1f), fontSize = 14.sp)
    }
    HorizontalDivider()
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailedPolicyCard(policy: Policy) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Main two-column layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // --- Left Column ---
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1.2f) // Give left column slightly more space
                ) {
                    Text(
                        policy.shortName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Policy No: ${policy.policyNumber}", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(policy.policyNumber))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Policy Number")
                        }
                    }
                    Text("Plan: ${policy.plan}   Mode: ${policy.mode}", fontSize = 14.sp)
                    Text("DOC: ${SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(policy.doc))}", fontSize = 14.sp)

                    policy.lastPremiumPaidDate?.let {
                        val formattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(it))
                        Text("Last Paid: $formattedDate", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }

                //    Text("Ananda: ${if (policy.isAnanda) "Yes" else "No"}", fontSize = 14.sp)
                    val isLate = isPolicyLate(policy.doc, policy.mode, policy.enachDate, policy.lastPremiumPaidDate)
                    val dueDate = calculateDueDate(policy.doc, policy.mode, policy.enachDate)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Due Date: $dueDate",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        if (isLate) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "⚠️ LATE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // --- Right Column ---
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Premium: ${policy.premium}", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(4.dp))

                    if (policy.enachDate.isNotEmpty()) {
                        Text("ENACH: ${policy.enachDate}", fontSize = 14.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // WhatsApp Share Button
                        IconButton(
                            onClick = {
                                val dueDate = calculateDueDate(policy.doc, policy.mode, policy.enachDate)
                                val message = buildString {
                                    append("*Policy Details*\n\n")
                                    append("📋 *Name:* ${policy.shortName}\n")
                                    append("📄 *Policy No:* ${policy.policyNumber}\n")
                                    append("📅 *Plan:* ${policy.plan}\n")
                                    append("💰 *Premium:* ₹${policy.premium}\n")
                                    append("🔄 *Mode:* ${policy.mode}\n")
                                    append("📆 *Due Date:* $dueDate\n")
                                    if (policy.enachDate.isNotEmpty()) {
                                        append("🏦 *ENACH:* ${policy.enachDate}\n")
                                    }
                                    append("\n_From LIC Advisor App_")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    setPackage("com.whatsapp")
                                    putExtra(Intent.EXTRA_TEXT, message)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback to general share if WhatsApp not installed
                                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, message)
                                    }
                                    context.startActivity(Intent.createChooser(fallbackIntent, "Share via"))
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share on WhatsApp")
                        }

                        TextButton(onClick = {
                            val url = "https://ebiz.licindia.in/D2CPM/?_ga=2.79975455.94153341.1756390897-1803416620.1748157402&_gac=1.115865716.1756390897.Cj0KCQjw_L_FBhDmARIsAItqgt4JucN53yqOrSxxkmQStkBybH4GXFjuyA9YxdwOL6rGc2-qRHH-de4aAq80EALw_wcB#DirectPayp"
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            context.startActivity(intent)
                        }) {
                            Text("Payment Link")
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDueDate(docTimestamp: Long, mode: String, enachDate: String): String {
    // Determine the base date: Use ENACH date if available, otherwise use DOC
    val baseTimestamp = if (enachDate.isNotEmpty()) {
        try {
            // Try to parse the enachDate string (format should be dd-MM-yyyy)
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            dateFormat.parse(enachDate)?.time ?: docTimestamp
        } catch (e: Exception) {
            docTimestamp // Fallback to DOC if parsing fails
        }
    } else {
        docTimestamp
    }
    
    if (baseTimestamp == 0L) return "N/A"

    val baseCalendar = Calendar.getInstance().apply { timeInMillis = baseTimestamp }
    val now = Calendar.getInstance()

    val monthInterval = when (mode.uppercase(Locale.ROOT)) {
        "YLY", "YEARLY" -> 12
        "HLY", "HALFYEARLY" -> 6
        "QLY", "QUARTERLY" -> 3
        "MLY", "MONTHLY" -> 1
        else -> return "N/A"
    }

    val dueDateCalendar = baseCalendar.clone() as Calendar
    
    // Calculate the next due date from the base date
    while (dueDateCalendar.before(now) || dueDateCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) 
           && dueDateCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)
           && dueDateCalendar.get(Calendar.DAY_OF_MONTH) <= now.get(Calendar.DAY_OF_MONTH)) {
        dueDateCalendar.add(Calendar.MONTH, monthInterval)
    }

    return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(dueDateCalendar.time)
}

/**
 * Check if a policy is late (due date is more than 1 month ago but less than 1 year)
 * Policies more than 1 year overdue are considered lapsed, not late
 */
private fun isPolicyLate(docTimestamp: Long, mode: String, enachDate: String, lastPremiumPaidDate: Long?): Boolean {
    // Determine the base date for calculating the next due date
    // Priority: 1) Last Premium Paid Date, 2) ENACH date, 3) DOC
    val baseTimestamp = if (lastPremiumPaidDate != null && lastPremiumPaidDate > 0) {
        lastPremiumPaidDate
    } else if (enachDate.isNotEmpty()) {
        try {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            dateFormat.parse(enachDate)?.time ?: docTimestamp
        } catch (e: Exception) {
            docTimestamp
        }
    } else {
        docTimestamp
    }
    
    if (baseTimestamp == 0L) return false

    val now = Calendar.getInstance()
    val nextDueCal = Calendar.getInstance().apply { timeInMillis = baseTimestamp }

    val monthInterval = when (mode.uppercase(Locale.ROOT)) {
        "YLY", "YEARLY" -> 12
        "HLY", "HALFYEARLY" -> 6
        "QLY", "QUARTERLY" -> 3
        "MLY", "MONTHLY" -> 1
        else -> return false
    }

    // If we have a last premium paid date, the next due is one interval after that
    if (lastPremiumPaidDate != null && lastPremiumPaidDate > 0) {
        nextDueCal.add(Calendar.MONTH, monthInterval)
    } else {
        // Calculate the next upcoming due date from the base date
        while (nextDueCal.before(now)) {
            nextDueCal.add(Calendar.MONTH, monthInterval)
        }
    }
    
    // Check if today is more than 30 days AFTER the next due date
    // BUT less than 365 days (1 year) - policies >1 year overdue are considered lapsed
    val daysDifference = ((now.timeInMillis - nextDueCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    return daysDifference > 30 && daysDifference <= 365
}

@Composable
fun PremiumSummaryCard(summary: AdminViewModel.AgentSummary) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header - always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (summary.agentName.isNotEmpty()) summary.agentName else summary.agencyCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (summary.agentName.isNotEmpty()) {
                            Text(
                                text = summary.agencyCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = "₹${String.format("%.2f", summary.totalScheduledPremium)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Expandable content - monthly breakdown
            if (isExpanded && summary.monthlyBreakdown.isNotEmpty()) {
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    summary.monthlyBreakdown.forEach { monthlyPremium ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = monthlyPremium.month,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format("%.2f", monthlyPremium.premium)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SortChip(
    text: String,
    isSelected: Boolean,
    sortOrder: AdminViewModel.SortOrder,
    onClick: () -> Unit
) {
    val icon = if (isSelected) {
        if (sortOrder == AdminViewModel.SortOrder.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    } else {
        null
    }
    InputChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        trailingIcon = {
            icon?.let {
                Icon(it, contentDescription = "Sort Order")
            }
        }
    )
}

fun formatDateRange(startMillis: Long?, endMillis: Long?): String {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    val start = startMillis?.let { sdf.format(Date(it)) } ?: "Start"
    val end = endMillis?.let { sdf.format(Date(it)) } ?: "End"
    return "$start - $end"

}

fun formatDate(millis: Long): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}

fun formatDateTime(millis: Long): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
}

/**
 * Banner that shows when the device is offline.
 */
@Composable
fun OfflineBanner(isOnline: Boolean) {
    androidx.compose.animation.AnimatedVisibility(visible = !isOnline) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Share, // Using available icon
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You are offline • Showing cached data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Shows last sync timestamp at the top of a screen.
 */
@Composable
fun SyncStatusBar(lastSyncTime: Long?, isOnline: Boolean) {
    if (lastSyncTime != null && lastSyncTime > 0) {
        val elapsed = System.currentTimeMillis() - lastSyncTime
        val timeAgo = when {
            elapsed < 60_000 -> "Just now"
            elapsed < 3600_000 -> "${elapsed / 60_000} min ago"
            elapsed < 86400_000 -> "${elapsed / 3600_000} hours ago"
            else -> formatDateTime(lastSyncTime)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isOnline) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Synced: $timeAgo",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
