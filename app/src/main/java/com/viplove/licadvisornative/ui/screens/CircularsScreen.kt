package com.viplove.licadvisornative.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.model.Circular
import com.viplove.licadvisornative.model.DocumentCategories
import com.viplove.licadvisornative.ui.viewmodel.CircularsViewModel
import com.viplove.licadvisornative.ui.components.UploadCircularDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import java.text.SimpleDateFormat
import java.util.*

/**
 * Circulars screen - ADMIN ONLY
 * Admins can upload and download circulars
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularsScreen(
    userRole: String,
    userEmail: String,
    userName: String,
    viewModel: CircularsViewModel = viewModel()
) {
    val circulars by viewModel.circulars.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    var showUploadDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Admins and superadmins can upload circulars
    val canUpload = userRole == "admin" || userRole == "superadmin"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.circulars_tab)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (canUpload) {
                FloatingActionButton(
                    onClick = { showUploadDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.upload_circular))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_circulars)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Category Filter
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.updateCategoryFilter(null) },
                        label = { Text(stringResource(R.string.all_categories)) }
                    )
                }
                
                items(DocumentCategories.ALL_CIRCULAR_CATEGORIES.size) { index ->
                    val category = DocumentCategories.ALL_CIRCULAR_CATEGORIES[index]
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.updateCategoryFilter(category) },
                        label = { Text(category) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Circulars List
            if (circulars.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Article,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.empty_circulars_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.empty_circulars_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(circulars) { circular ->
                        CircularCard(
                            circular = circular,
                            onPreview = {
                                previewCircular(context, circular, viewModel)
                            },
                            onDownload = {
                                downloadCircular(context, circular, viewModel)
                            },
                            canDelete = canUpload,
                            onDelete = {
                                viewModel.deleteCircular(circular.circularId, circular.fileExtension)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Upload Dialog
    if (showUploadDialog) {
        UploadCircularDialog(
            onDismiss = { showUploadDialog = false },
            userEmail = userEmail,
            userName = userName,
            viewModel = viewModel
        )
    }
}

@Composable
fun CircularCard(
    circular: Circular,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = circular.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Category and Language
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(circular.category, style = MaterialTheme.typography.bodySmall) }
                )
                
                if (circular.language.isNotBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = { 
                            Text(
                                when(circular.language) {
                                    "hindi" -> stringResource(R.string.language_hindi_detected)
                                    "english" -> stringResource(R.string.language_english_detected)
                                    "both" -> stringResource(R.string.language_both)
                                    else -> stringResource(R.string.language_unknown)
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description if available
            if (circular.description.isNotBlank()) {
                Text(
                    text = circular.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Metadata
            Text(
                text = stringResource(R.string.uploaded_by, circular.uploadedByName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = formatDateTime(circular.uploadedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.file_size, formatFileSize(circular.fileSizeBytes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.delete_document))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                OutlinedButton(onClick = onPreview) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.preview))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.download))
                }
            }
        }
    }
}



private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

private fun downloadCircular(
    context: android.content.Context,
    circular: Circular,
    viewModel: CircularsViewModel
) {
    kotlinx.coroutines.MainScope().launch {
        val downloadUrl = viewModel.getDownloadUrl(circular.circularId)
        if (downloadUrl != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
            context.startActivity(intent)
        }
    }
}

private fun previewCircular(
    context: android.content.Context,
    circular: Circular,
    viewModel: CircularsViewModel
) {
    kotlinx.coroutines.MainScope().launch {
        val downloadUrl = viewModel.getDownloadUrl(circular.circularId)
        if (downloadUrl != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(downloadUrl), getMimeType(circular.fileType))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(Intent.createChooser(intent, "Preview with..."))
            } catch (e: Exception) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
            }
        }
    }
}

private fun getMimeType(fileType: String): String {
    return when (fileType.lowercase()) {
        "pdf" -> "application/pdf"
        "image", "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> "*/*"
    }
}

// Upload Dialog is now imported from com.viplove.licadvisornative.ui.components.UploadCircularDialog
