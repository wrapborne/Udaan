// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/AgentPerformanceScreen.kt
package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viplove.licadvisornative.model.Policy
import com.viplove.licadvisornative.ui.components.PillChip
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.components.StatTile
import com.viplove.licadvisornative.ui.theme.BrandDanger
import com.viplove.licadvisornative.ui.theme.BrandGold
import com.viplove.licadvisornative.ui.theme.BrandSuccess
import com.viplove.licadvisornative.ui.theme.Dimens


import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class to hold agent performance metrics
 */
data class AgentPerformanceData(
    val agentCode: String,
    val agentName: String,
    val currentPeriodPolicies: Int,
    val currentPeriodPremium: Long,
    val previousPeriodPolicies: Int,
    val previousPeriodPremium: Long,
    val anandaCount: Int,
    val ulipCount: Int
) {
    val policyGrowth: Int get() = currentPeriodPolicies - previousPeriodPolicies
    val premiumGrowth: Long get() = currentPeriodPremium - previousPeriodPremium
    val policyGrowthPercent: Double get() = if (previousPeriodPolicies > 0)
        ((currentPeriodPolicies - previousPeriodPolicies).toDouble() / previousPeriodPolicies) * 100 else 0.0
    val premiumGrowthPercent: Double get() = if (previousPeriodPremium > 0)
        ((currentPeriodPremium - previousPeriodPremium).toDouble() / previousPeriodPremium) * 100 else 0.0
}

/**
 * Agent Performance Report Tab for Admin Dashboard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentPerformanceTab(
    policies: List<Policy>
) {
    var selectedPeriod by remember { mutableStateOf("This Month") }
    val periods = listOf("This Month", "This Quarter", "This Year")
    
    val performanceData = remember(policies, selectedPeriod) {
        calculatePerformanceData(policies, selectedPeriod)
    }
    
    // Calculate totals
    val totalCurrentPolicies = performanceData.sumOf { it.currentPeriodPolicies }
    val totalCurrentPremium = performanceData.sumOf { it.currentPeriodPremium }
    val totalPreviousPolicies = performanceData.sumOf { it.previousPeriodPolicies }
    val totalPreviousPremium = performanceData.sumOf { it.previousPeriodPremium }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenHPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
    ) {
        // Period Selector
        item {
            SectionCard {
                Text(
                    "Agent Performance Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
                ) {
                    periods.forEach { period ->
                        PillChip(
                            label = period,
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Policies",
                    currentValue = totalCurrentPolicies.toString(),
                    previousValue = totalPreviousPolicies.toString(),
                    isPositiveGrowth = totalCurrentPolicies >= totalPreviousPolicies
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Premium",
                    currentValue = "₹${formatPremium(totalCurrentPremium)}",
                    previousValue = "₹${formatPremium(totalPreviousPremium)}",
                    isPositiveGrowth = totalCurrentPremium >= totalPreviousPremium
                )
            }
        }
        
        // Agent Rankings Header
        item {
            Text(
                "Agent Rankings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Dimens.GutterSm)
            )
        }
        
        // Top Performers by Policies
        item {
            PerformanceRankingCard(
                title = "Top Performers - Policies",
                data = performanceData.sortedByDescending { it.currentPeriodPolicies }.take(5),
                valueSelector = { "${it.currentPeriodPolicies} policies" },
                growthSelector = { it.policyGrowth }
            )
        }
        
        // Top Performers by Premium
        item {
            PerformanceRankingCard(
                title = "Top Performers - Premium",
                data = performanceData.sortedByDescending { it.currentPeriodPremium }.take(5),
                valueSelector = { "₹${formatPremium(it.currentPeriodPremium)}" },
                growthSelector = { it.premiumGrowth.toInt() }
            )
        }
        
        // Growth Leaders
        item {
            PerformanceRankingCard(
                title = "Growth Leaders",
                data = performanceData.sortedByDescending { it.policyGrowth }.take(5),
                valueSelector = { "${it.currentPeriodPolicies} policies" },
                growthSelector = { it.policyGrowth }
            )
        }
        
        // All Agents Detailed View
        item {
            Text(
                "All Agents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Dimens.GutterSm)
            )
        }
        
        items(performanceData.sortedByDescending { it.currentPeriodPremium }) { data ->
            AgentPerformanceCard(data)
        }
    }
}

@Composable
fun SummaryStatCard(
    modifier: Modifier = Modifier,
    title: String,
    currentValue: String,
    previousValue: String,
    isPositiveGrowth: Boolean
) {
    StatTile(
        label = title,
        value = currentValue,
        modifier = modifier,
        delta = "vs $previousValue",
        deltaUp = isPositiveGrowth,
        accent = if (isPositiveGrowth) BrandSuccess else BrandDanger
    )
}

@Composable
fun PerformanceRankingCard(
    title: String,
    data: List<AgentPerformanceData>,
    valueSelector: (AgentPerformanceData) -> String,
    growthSelector: (AgentPerformanceData) -> Int
) {
    SectionCard(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
            data.forEachIndexed { index, agentData ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.GutterXs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "#${index + 1}",
                            modifier = Modifier.width(34.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (index < 3) BrandGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            agentData.agentName.ifEmpty { agentData.agentCode },
                            fontWeight = if (index < 3) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(valueSelector(agentData), style = MaterialTheme.typography.bodyMedium)
                        val growth = growthSelector(agentData)
                        if (growth != 0) {
                            Text(
                                text = if (growth > 0) " +$growth" else " $growth",
                                color = if (growth > 0) BrandSuccess else BrandDanger,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentPerformanceCard(data: AgentPerformanceData) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        data.agentName.ifEmpty { data.agentCode },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (data.agentName.isNotEmpty()) {
                        Text(
                            data.agentCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Growth indicator
                val growth = data.policyGrowthPercent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (growth >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (growth >= 0) BrandSuccess else BrandDanger,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "${String.format("%.1f", kotlin.math.abs(growth))}%",
                        color = if (growth >= 0) BrandSuccess else BrandDanger,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem("Policies", data.currentPeriodPolicies.toString(), data.policyGrowth)
                MetricItem("Premium", "₹${formatPremium(data.currentPeriodPremium)}", null)
                MetricItem("ANANDA", data.anandaCount.toString(), null)
                MetricItem("ULIP", data.ulipCount.toString(), null)
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, growth: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        if (growth != null) {
            val color = when {
                growth > 0 -> BrandSuccess
                growth < 0 -> BrandDanger
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = when {
                    growth > 0 -> "+$growth"
                    growth < 0 -> "$growth"
                    else -> "—"
                },
                fontSize = 10.sp,
                color = color
            )
        }
    }
}

/**
 * Calculate performance data for all agents
 */
private fun calculatePerformanceData(
    policies: List<Policy>,
    period: String
): List<AgentPerformanceData> {
    val calendar = Calendar.getInstance()
    val now = calendar.timeInMillis

    // Calculate period boundaries
    val (currentStart, currentEnd) = when (period) {
        "This Month" -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val end = calendar.timeInMillis
            start to end
        }
        "This Quarter" -> {
            val currentMonth = calendar.get(Calendar.MONTH)
            val quarterStartMonth = (currentMonth / 3) * 3
            calendar.set(Calendar.MONTH, quarterStartMonth)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 3)
            val end = calendar.timeInMillis
            start to end
        }
        "This Year" -> {
            // Financial year: April 1 to March 31
            val freshCal = Calendar.getInstance()
            freshCal.set(Calendar.MONTH, Calendar.APRIL)
            freshCal.set(Calendar.DAY_OF_MONTH, 1)
            freshCal.set(Calendar.HOUR_OF_DAY, 0)
            freshCal.set(Calendar.MINUTE, 0)
            freshCal.set(Calendar.SECOND, 0)
            freshCal.set(Calendar.MILLISECOND, 0)
            if (Calendar.getInstance().get(Calendar.MONTH) < Calendar.APRIL) {
                freshCal.add(Calendar.YEAR, -1)
            }
            val start = freshCal.timeInMillis
            freshCal.add(Calendar.YEAR, 1)
            val end = freshCal.timeInMillis
            start to end
        }
        else -> 0L to now
    }

    // Previous period has the same length, immediately before current period
    val periodLength = currentEnd - currentStart
    val previousStart = currentStart - periodLength
    val previousEnd = currentStart

    // Group ALL policies by agentCode — no Firestore agent matching needed
    return policies
        .groupBy { it.agentCode }
        .map { (agentCode, agentPolicies) ->
            val agentName = agentPolicies.firstOrNull { it.agentName.isNotBlank() }?.agentName ?: ""
            val currentPolicies = agentPolicies.filter { it.doc in currentStart until currentEnd }
            val previousPolicies = agentPolicies.filter { it.doc in previousStart until previousEnd }
            AgentPerformanceData(
                agentCode = agentCode,
                agentName = agentName,
                currentPeriodPolicies = currentPolicies.size,
                currentPeriodPremium = currentPolicies.sumOf { it.premium.toLong() },
                previousPeriodPolicies = previousPolicies.size,
                previousPeriodPremium = previousPolicies.sumOf { it.premium.toLong() },
                anandaCount = currentPolicies.count { it.isAnanda },
                ulipCount = currentPolicies.count { it.isUlip }
            )
        }
}
