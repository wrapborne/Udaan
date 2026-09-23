package com.viplove.licadvisornative.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.viplove.licadvisornative.ui.theme.Dimens

data class UploadAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector = Icons.Filled.CloudUpload,
    val containerColor: Color? = null,
    val contentColor: Color? = null
)

@Composable
fun UploadButtonRow(
    actions: List<UploadAction>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
    ) {
        actions.forEach { action ->
            Button(
                onClick = action.onClick,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.ButtonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = action.containerColor ?: MaterialTheme.colorScheme.primary,
                    contentColor = action.contentColor ?: MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label
                )
                Text(action.label)
            }
        }
    }
}
