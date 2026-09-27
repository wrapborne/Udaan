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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.viplove.licadvisornative.ui.theme.Dimens

@Composable
fun GmailImportProfileSection() {
    val context = LocalContext.current
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
}

private const val GMAIL_IMPORT_PORTAL_URL = "https://udaan.viplovesaini.in"
