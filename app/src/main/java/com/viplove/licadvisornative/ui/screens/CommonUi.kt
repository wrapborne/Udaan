package com.viplove.licadvisornative.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.ui.components.StatTile
import com.viplove.licadvisornative.ui.theme.BrandDanger
import com.viplove.licadvisornative.ui.theme.BrandDangerBg
import com.viplove.licadvisornative.ui.theme.BrandGold
import com.viplove.licadvisornative.ui.theme.BrandGoldDark
import com.viplove.licadvisornative.ui.theme.BrandGoldSoft
import com.viplove.licadvisornative.ui.theme.BrandSuccess
import com.viplove.licadvisornative.ui.theme.BrandWarning
import com.viplove.licadvisornative.ui.theme.BrandWarningBg
import com.viplove.licadvisornative.ui.theme.BrandWhatsApp
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .heightIn(min = Dimens.FieldHeight),
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    StatTile(
        label = label,
        value = value,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PolicyCard(policy: Policy) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.GutterMd),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = policy.shortName.ifBlank { policy.policyNumber },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Policy no: ${policy.policyNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = policy.plan,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatRawDate(policy.doc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DetailedPolicyCard(policy: Policy) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isLate = isPolicyLate(policy.doc, policy.mode, policy.enachDate, policy.lastPremiumPaidDate)
    val dueDate = calculateDueDate(policy.doc, policy.mode, policy.enachDate)
    val lateDays = calculateLateDays(policy.doc, policy.mode, policy.enachDate, policy.lastPremiumPaidDate)
    val lastPaid = policy.lastPremiumPaidDate?.let { formatRawDate(it) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.GutterLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = policy.shortName.ifBlank { "Policy ${policy.policyNumber}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Policy no: ${policy.policyNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(policy.policyNumber))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(Dimens.IconBtnMin)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy policy number",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                PolicyBadge(policy = policy, isLate = isLate, lateDays = lateDays)
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GutterXs)) {
                Text(
                    text = "Plan: ${policy.plan} · Mode: ${policy.mode}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buildString {
                        append("DOC: ${formatRawDate(policy.doc)}")
                        if (lastPaid != null) append(" · Last paid: $lastPaid")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (lastPaid != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isLate) "Due: $dueDate  LATE" else "Due: $dueDate",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isLate) FontWeight.SemiBold else FontWeight.Normal
                )
                if (policy.enachDate.isNotEmpty()) {
                    Text(
                        text = "ENACH: ${policy.enachDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Premium",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%.2f", policy.premium)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { sharePolicyOnWhatsApp(context, policy, dueDate) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = BrandWhatsApp,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share policy on WhatsApp",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(PAYMENT_URL)
                            context.startActivity(intent)
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Payment link")
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicyBadge(policy: Policy, isLate: Boolean, lateDays: Int) {
    val badge = when {
        policy.isAnanda -> Triple("ANANDA", BrandGoldSoft, BrandGoldDark)
        isLate -> Triple("Late ${lateDays.coerceAtLeast(31)}d", BrandDangerBg, BrandDanger)
        policy.isUlip -> Triple("ULIP", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        else -> null
    }
    if (badge != null) {
        Surface(
            shape = RoundedCornerShape(Dimens.PillCorner),
            color = badge.second,
            contentColor = badge.third
        ) {
            Text(
                text = badge.first,
                modifier = Modifier.padding(horizontal = Dimens.GutterSm, vertical = Dimens.GutterXs),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun sharePolicyOnWhatsApp(context: android.content.Context, policy: Policy, dueDate: String) {
    val message = buildString {
        append("*Policy Details*\n\n")
        append("*Name:* ${policy.shortName}\n")
        append("*Policy No:* ${policy.policyNumber}\n")
        append("*Plan:* ${policy.plan}\n")
        append("*Premium:* ₹${policy.premium}\n")
        append("*Mode:* ${policy.mode}\n")
        append("*Due Date:* $dueDate\n")
        if (policy.enachDate.isNotEmpty()) append("*ENACH:* ${policy.enachDate}\n")
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
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(fallbackIntent, "Share via"))
    }
}

private fun calculateDueDate(docTimestamp: Long, mode: String, enachDate: String): String {
    val baseTimestamp = if (enachDate.isNotEmpty()) {
        try {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(enachDate)?.time ?: docTimestamp
        } catch (e: Exception) {
            docTimestamp
        }
    } else {
        docTimestamp
    }

    if (baseTimestamp == 0L) return "N/A"

    val dueDateCalendar = Calendar.getInstance().apply { timeInMillis = baseTimestamp }
    val now = Calendar.getInstance()
    val monthInterval = modeToMonthInterval(mode) ?: return "N/A"

    while (
        dueDateCalendar.before(now) ||
        dueDateCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        dueDateCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
        dueDateCalendar.get(Calendar.DAY_OF_MONTH) <= now.get(Calendar.DAY_OF_MONTH)
    ) {
        dueDateCalendar.add(Calendar.MONTH, monthInterval)
    }

    return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(dueDateCalendar.time)
}

private fun isPolicyLate(docTimestamp: Long, mode: String, enachDate: String, lastPremiumPaidDate: Long?): Boolean {
    val daysDifference = calculateLateDays(docTimestamp, mode, enachDate, lastPremiumPaidDate)
    return daysDifference > 30 && daysDifference <= 365
}

private fun calculateLateDays(
    docTimestamp: Long,
    mode: String,
    enachDate: String,
    lastPremiumPaidDate: Long?
): Int {
    val baseTimestamp = if (lastPremiumPaidDate != null && lastPremiumPaidDate > 0) {
        lastPremiumPaidDate
    } else if (enachDate.isNotEmpty()) {
        try {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(enachDate)?.time ?: docTimestamp
        } catch (e: Exception) {
            docTimestamp
        }
    } else {
        docTimestamp
    }

    if (baseTimestamp == 0L) return 0

    val nextDueCal = Calendar.getInstance().apply { timeInMillis = baseTimestamp }
    val monthInterval = modeToMonthInterval(mode) ?: return 0
    val now = Calendar.getInstance()

    if (lastPremiumPaidDate != null && lastPremiumPaidDate > 0) {
        nextDueCal.add(Calendar.MONTH, monthInterval)
    } else {
        while (nextDueCal.before(now)) {
            nextDueCal.add(Calendar.MONTH, monthInterval)
        }
    }

    return ((now.timeInMillis - nextDueCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
}

private fun modeToMonthInterval(mode: String): Int? {
    return when (mode.uppercase(Locale.ROOT)) {
        "YLY", "YEARLY" -> 12
        "HLY", "HALFYEARLY" -> 6
        "QLY", "QUARTERLY" -> 3
        "MLY", "MONTHLY" -> 1
        else -> null
    }
}

@Composable
fun PremiumSummaryCard(summary: AdminViewModel.AgentSummary) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(Dimens.GutterLg),
                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse premium summary" else "Expand premium summary",
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.agentName.ifEmpty { summary.agencyCode },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (summary.agentName.isNotEmpty()) {
                        Text(
                            text = summary.agencyCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Premium",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%.2f", summary.totalScheduledPremium)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded && summary.monthlyBreakdown.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.GutterLg, vertical = Dimens.GutterSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
                ) {
                    summary.monthlyBreakdown.forEach { monthlyPremium ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = monthlyPremium.month,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%.2f", monthlyPremium.premium)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
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
        if (sortOrder == AdminViewModel.SortOrder.ASC) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    } else {
        null
    }
    InputChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        shape = RoundedCornerShape(Dimens.PillCorner),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        trailingIcon = {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = "Sort order",
                    modifier = Modifier.size(16.dp)
                )
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

@Composable
fun OfflineBanner(isOnline: Boolean) {
    AnimatedVisibility(visible = !isOnline) {
        Surface(
            color = BrandGoldSoft,
            contentColor = BrandGoldDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.GutterLg, vertical = Dimens.GutterSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = "Offline",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.GutterSm))
                Text(
                    text = "You're offline · Showing cached data",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SyncStatusBar(lastSyncTime: Long?, isOnline: Boolean) {
    if (lastSyncTime != null && lastSyncTime > 0) {
        val elapsed = System.currentTimeMillis() - lastSyncTime
        val timeAgo = when {
            elapsed < 60_000 -> "Just now"
            elapsed < 3_600_000 -> "${elapsed / 60_000} min ago"
            elapsed < 86_400_000 -> "${elapsed / 3_600_000} hours ago"
            else -> formatDateTime(lastSyncTime)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.GutterLg, vertical = Dimens.GutterXs),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(Dimens.PillCorner),
                color = if (isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = "Synced: $timeAgo",
                    modifier = Modifier.padding(horizontal = Dimens.GutterSm, vertical = Dimens.GutterXs),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatRawDate(millis: Long): String {
    if (millis <= 0L) return "N/A"
    return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(millis))
}

private const val PAYMENT_URL =
    "https://ebiz.licindia.in/D2CPM/?_ga=2.79975455.94153341.1756390897-1803416620.1748157402&_gac=1.115865716.1756390897.Cj0KCQjw_L_FBhDmARIsAItqgt4JucN53yqOrSxxkmQStkBybH4GXFjuyA9YxdwOL6rGc2-qRHH-de4aAq80EALw_wcB#DirectPayp"
