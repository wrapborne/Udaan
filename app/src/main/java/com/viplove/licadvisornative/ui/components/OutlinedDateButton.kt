package com.viplove.licadvisornative.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.viplove.licadvisornative.ui.theme.Dimens

@Composable
fun OutlinedDateButton(
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.ButtonHeight),
        shape = MaterialTheme.shapes.small
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarToday,
            contentDescription = "Select date"
        )
        Text(label ?: "Select date")
    }
}
