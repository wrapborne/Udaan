package com.viplove.licadvisornative.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.viplove.licadvisornative.ui.theme.BrandSuccess
import com.viplove.licadvisornative.ui.theme.Dimens

@Composable
fun BottomActionBar(
    onBack: () -> Unit,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryLabel: String = "Next",
    isLastStep: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = Dimens.GutterXs,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(Dimens.GutterMd)
                .fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.GutterSm)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onPrimary,
                modifier = Modifier.weight(1f),
                colors = if (isLastStep) {
                    ButtonDefaults.buttonColors(containerColor = BrandSuccess)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(primaryLabel)
            }
        }
    }
}
