package com.viplove.licadvisornative.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.model.GraphicFooter
import com.viplove.licadvisornative.model.GraphicTemplate
import com.viplove.licadvisornative.ui.viewmodel.GraphicsViewModel

@Composable
fun GraphicsManagementScreen(
    graphicsViewModel: GraphicsViewModel = viewModel()
) {
    // ✅ CONTROL THE FEATURE FROM HERE
    // Set this to 'true' to work on the feature, 'false' to show the message.
    val isFeatureEnabled = false

    if (isFeatureEnabled) {
        // --- ALL YOUR ORIGINAL CODE IS SAFE HERE ---
        val uiState by graphicsViewModel.uiState.collectAsState()
        val context = LocalContext.current

        var templateName by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf("all") }
        val roleOptions = mapOf("all" to "All Users", "admin" to "Admins Only", "advisor" to "Advisors Only")

        var footerName by remember { mutableStateOf("") }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri: Uri? ->
                Log.d("GraphicsUpload", "Image picker returned a result. URI: $uri")
                uri?.let {
                    graphicsViewModel.onImageSelected(context, it)
                }
            }
        )

        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Upload New Graphic", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isUploading
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Visible To:", style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.fillMaxWidth()) {
                    roleOptions.forEach { (key, text) ->
                        Row(
                            Modifier
                                .selectable(
                                    selected = (key == selectedRole),
                                    onClick = { selectedRole = key }
                                )
                                .padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (key == selectedRole),
                                onClick = { selectedRole = key }
                            )
                            Text(text = text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.selectedImageUri != null) {
                    Text(
                        text = "Image selected. Ready to upload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !uiState.isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Select Image")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.selectedImageUri != null) "Change Image" else "Select Image")
                    }

                    if (uiState.isUploading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                graphicsViewModel.uploadGraphicTemplate(templateName, selectedRole)
                                templateName = ""
                                selectedRole = "all"
                            },
                            enabled = uiState.selectedImageUri != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Upload as Template")
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Manage Templates", style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.isLoading && uiState.templates.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.templates.isEmpty()) {
                item {
                    Text("No graphic templates have been uploaded yet.")
                }
            } else {
                items(uiState.templates) { template ->
                    GraphicTemplateCard(
                        template = template,
                        onDelete = { graphicsViewModel.deleteGraphicTemplate(template) }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Upload New Footer", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = footerName,
                    onValueChange = { footerName = it },
                    label = { Text("Footer Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isUploading
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isUploading) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center){
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            graphicsViewModel.uploadGraphicFooter(footerName)
                            footerName = ""
                        },
                        enabled = uiState.selectedImageUri != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upload Selected Image as Footer")
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Manage Footers", style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.isLoading && uiState.footers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.footers.isEmpty()) {
                item { Text("No footers have been uploaded yet.") }
            } else {
                items(uiState.footers) { footer ->
                    GraphicFooterCard(
                        footer = footer,
                        onDelete = { graphicsViewModel.deleteGraphicFooter(footer) }
                    )
                }
            }
        }
    } else {
        // --- THIS IS THE "COMING SOON" MESSAGE ---
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.graphics_management_soon),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}

@Composable
fun GraphicTemplateCard(template: GraphicTemplate, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = template.imageUrl),
                contentDescription = template.name,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, fontWeight = FontWeight.Bold)
                val visibilityText = when(template.visibleToRole) {
                    "admin" -> "Admins Only"
                    "advisor" -> "Advisors Only"
                    else -> "All Users"
                }
                Text(visibilityText, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Template", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun GraphicFooterCard(footer: GraphicFooter, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = footer.imageUrl),
                contentDescription = footer.name,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(footer.name, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Footer", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
