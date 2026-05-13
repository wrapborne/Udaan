package com.viplove.licadvisornative.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import com.viplove.licadvisornative.model.DocumentCategories
import com.viplove.licadvisornative.model.Form
import com.viplove.licadvisornative.ui.viewmodel.FormsViewModel
import com.viplove.licadvisornative.ui.components.UploadFormDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import java.text.SimpleDateFormat
import java.util.*

/**
 * Forms screen for viewing and downloading LIC forms
 * Super Admin can upload, all users can search and download
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormsScreen(
    userRole: String,
    userEmail: String,
    userName: String,
    viewModel: FormsViewModel = viewModel()
) {
    val forms by viewModel.forms.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    var showUploadDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val canUpload = userRole == "superadmin"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forms_tab)) },
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
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.upload_form))
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
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                placeholder = stringResource(R.string.search_forms)
            )
            
            // Category Filter
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                categories = DocumentCategories.ALL_FORM_CATEGORIES,
                onCategorySelected = { viewModel.updateCategoryFilter(it) },
                onClearFilters = { viewModel.clearFilters() }
            )
            
            // Forms List
            if (forms.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_forms_title),
                    message = stringResource(R.string.empty_forms_message)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(forms) { form ->
                        FormCard(
                            form = form,
                            onPreview = {
                                // Preview form in browser/PDF viewer
                                previewForm(context, form, viewModel)
                            },
                            onDownload = {
                                // Download form
                                downloadForm(context, form, viewModel)
                            },
                            canDelete = canUpload,
                            onDelete = {
                                viewModel.deleteForm(form.formId, form.fileExtension)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Upload Dialog
    if (showUploadDialog) {
        UploadFormDialog(
            onDismiss = { showUploadDialog = false },
            userEmail = userEmail,
            userName = userName,
            viewModel = viewModel
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}

@Composable
fun CategoryFilterRow(
    selectedCategory: String?,
    categories: List<String>,
    onCategorySelected: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.all_categories)) }
            )
        }
        
        items(categories.size) { index ->
            FilterChip(
                selected = selectedCategory == categories[index],
                onClick = { onCategorySelected(categories[index]) },
                label = { Text(categories[index]) }
            )
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun FormCard(
    form: Form,
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
                text = form.title,
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
                    label = { Text(form.category, style = MaterialTheme.typography.bodySmall) }
                )
                
                if (form.language.isNotBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = { 
                            Text(
                                when(form.language) {
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
            
            // Metadata
            Text(
                text = stringResource(R.string.uploaded_by, form.uploadedByName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = formatDateTime(form.uploadedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.file_size, formatFileSize(form.fileSizeBytes)),
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

@Composable
fun EmptyState(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper Functions


private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

private fun downloadForm(
    context: android.content.Context,
    form: Form,
    viewModel: FormsViewModel
) {
    // Launch download using device's download manager
    kotlinx.coroutines.MainScope().launch {
        val downloadUrl = viewModel.getDownloadUrl(form.formId)
        if (downloadUrl != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
            context.startActivity(intent)
        }
    }
}

private fun previewForm(
    context: android.content.Context,
    form: Form,
    viewModel: FormsViewModel
) {
    // Preview form in browser/PDF viewer
    kotlinx.coroutines.MainScope().launch {
        val downloadUrl = viewModel.getDownloadUrl(form.formId)
        if (downloadUrl != null) {
            // Use ACTION_VIEW with a chooser to let user select their preferred viewer
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(downloadUrl), getMimeType(form.fileType))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(Intent.createChooser(intent, "Preview with..."))
            } catch (e: Exception) {
                // Fallback to simple ACTION_VIEW
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

// Upload Dialog is now imported from com.viplove.licadvisornative.ui.components.UploadFormDialog

