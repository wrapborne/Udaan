package com.viplove.licadvisornative.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.viplove.licadvisornative.ui.theme.Dimens

data class SortChipState(
    val label: String,
    val selected: Boolean,
    val ascending: Boolean? = null,
    val onClick: () -> Unit
)

@Composable
fun SortChipsRow(
    chips: List<SortChipState>,
    modifier: Modifier = Modifier,
    label: String = "Sort by:"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.GutterXs),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = Dimens.GutterXs)
        )
        chips.forEach { chip ->
            PillChip(
                label = chip.label,
                selected = chip.selected,
                onClick = chip.onClick,
                leadingIcon = when (chip.ascending) {
                    true -> Icons.Filled.ArrowUpward
                    false -> Icons.Filled.ArrowDownward
                    null -> null
                },
                modifier = Modifier.padding(start = Dimens.GutterXs)
            )
        }
    }
}
