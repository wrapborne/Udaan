package com.viplove.licadvisornative.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.model.DocumentCategories
import com.viplove.licadvisornative.model.DocumentMetadata
import com.viplove.licadvisornative.domain.UploadProgress
import com.viplove.licadvisornative.ui.viewmodel.FormsViewModel
import com.viplove.licadvisornative.ui.viewmodel.CircularsViewModel

/**
 * Upload Dialog for Forms and Circulars
 * Two-step process: 1) File Selection + Auto-extraction, 2) Review/Edit Metadata + Upload
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadFormDialog(
    onDismiss: () -> Unit,
    userEmail: String,
    userName: String,
    viewModel: FormsViewModel = viewModel()
) {
    val extractedMetadata by viewModel.extractedMetadata.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var editableTitle by remember { mutableStateOf("") }
    var editableCategory by remember { mutableStateOf("") }
    var editableTags by remember { mutableStateOf("") }
    var editableDescription by remember { mutableStateOf("") }
    
    // File picker
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            viewModel.extractMetadataFromFile(it)
        }
    }
    
    // Update editable fields when metadata is extracted
    LaunchedEffect(extractedMetadata) {
        extractedMetadata?.let { metadata ->
            editableTitle = metadata.suggestedTitle
            editableCategory = metadata.detectedCategory
            editableTags = metadata.extractedTags.joinToString(", ")
        }
    }
    
    // Close dialog on successful upload
    LaunchedEffect(uploadProgress) {
        if (uploadProgress is UploadProgress.Success) {
            onDismiss()
            viewModel.resetUploadState()
        }
    }
    
    Dialog(
        onDismissRequest = {
            if (uploadProgress !is UploadProgress.Uploading) {
                onDismiss()
                viewModel.resetUploadState()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = uploadProgress !is UploadProgress.Uploading,
            dismissOnClickOutside = uploadProgress !is UploadProgress.Uploading,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.upload_form),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    if (uploadProgress !is UploadProgress.Uploading) {
                        IconButton(onClick = {
                            onDismiss()
                            viewModel.resetUploadState()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content based on state
                when {
                    uploadProgress is UploadProgress.Processing -> {
                        ProcessingContent()
                    }
                    uploadProgress is UploadProgress.Uploading -> {
                        UploadingContent(uploadProgress as UploadProgress.Uploading)
                    }
                    extractedMetadata != null && selectedFileUri != null -> {
                        MetadataEditorContent(
                            metadata = extractedMetadata!!,
                            fileUri = selectedFileUri!!,
                            title = editableTitle,
                            onTitleChange = { editableTitle = it },
                            category = editableCategory,
                            onCategoryChange = { editableCategory = it },
                            tags = editableTags,
                            onTagsChange = { editableTags = it },
                            description = editableDescription,
                            onDescriptionChange = { editableDescription = it },
                            categories = DocumentCategories.ALL_FORM_CATEGORIES,
                            onUpload = {
                                val tagsList = editableTags
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                
                                viewModel.uploadForm(
                                    fileUri = selectedFileUri!!,
                                    title = editableTitle,
                                    category = editableCategory,
                                    tags = tagsList,
                                    description = editableDescription,
                                    userEmail = userEmail,
                                    userName = userName
                                )
                            },
                            onCancel = {
                                selectedFileUri = null
                                viewModel.resetUploadState()
                            }
                        )
                    }
                    else -> {
                        FileSelectionContent(
                            onSelectFile = { filePicker.launch("*/*") },
                            errorMessage = errorMessage
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UploadCircularDialog(
    onDismiss: () -> Unit,
    userEmail: String,
    userName: String,
    viewModel: CircularsViewModel = viewModel()
) {
    val extractedMetadata by viewModel.extractedMetadata.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var editableTitle by remember { mutableStateOf("") }
    var editableCategory by remember { mutableStateOf("") }
    var editableTags by remember { mutableStateOf("") }
    var editableDescription by remember { mutableStateOf("") }
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            viewModel.extractMetadataFromFile(it)
        }
    }
    
    LaunchedEffect(extractedMetadata) {
        extractedMetadata?.let { metadata ->
            editableTitle = metadata.suggestedTitle
            editableCategory = metadata.detectedCategory
            editableTags = metadata.extractedTags.joinToString(", ")
        }
    }
    
    LaunchedEffect(uploadProgress) {
        if (uploadProgress is UploadProgress.Success) {
            onDismiss()
            viewModel.resetUploadState()
        }
    }
    
    Dialog(
        onDismissRequest = {
            if (uploadProgress !is UploadProgress.Uploading) {
                onDismiss()
                viewModel.resetUploadState()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = uploadProgress !is UploadProgress.Uploading,
            dismissOnClickOutside = uploadProgress !is UploadProgress.Uploading,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.upload_circular),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    if (uploadProgress !is UploadProgress.Uploading) {
                        IconButton(onClick = {
                            onDismiss()
                            viewModel.resetUploadState()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when {
                    uploadProgress is UploadProgress.Processing -> {
                        ProcessingContent()
                    }
                    uploadProgress is UploadProgress.Uploading -> {
                        UploadingContent(uploadProgress as UploadProgress.Uploading)
                    }
                    extractedMetadata != null && selectedFileUri != null -> {
                        MetadataEditorContent(
                            metadata = extractedMetadata!!,
                            fileUri = selectedFileUri!!,
                            title = editableTitle,
                            onTitleChange = { editableTitle = it },
                            category = editableCategory,
                            onCategoryChange = { editableCategory = it },
                            tags = editableTags,
                            onTagsChange = { editableTags = it },
                            description = editableDescription,
                            onDescriptionChange = { editableDescription = it },
                            categories = DocumentCategories.ALL_CIRCULAR_CATEGORIES,
                            onUpload = {
                                val tagsList = editableTags
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                
                                viewModel.uploadCircular(
                                    fileUri = selectedFileUri!!,
                                    title = editableTitle,
                                    category = editableCategory,
                                    tags = tagsList,
                                    description = editableDescription,
                                    userEmail = userEmail,
                                    userName = userName
                                )
                            },
                            onCancel = {
                                selectedFileUri = null
                                viewModel.resetUploadState()
                            }
                        )
                    }
                    else -> {
                        FileSelectionContent(
                            onSelectFile = { filePicker.launch("*/*") },
                            errorMessage = errorMessage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileSelectionContent(
    onSelectFile: () -> Unit,
    errorMessage: String?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.select_file),
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.choose_file),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSelectFile,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.choose_file))
        }
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.extracting_metadata),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun UploadingContent(progress: UploadProgress.Uploading) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = progress.progress / 100f,
            modifier = Modifier.size(80.dp),
            strokeWidth = 8.dp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.uploading, progress.progress),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataEditorContent(
    metadata: DocumentMetadata,
    fileUri: Uri,
    title: String,
    onTitleChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    categories: List<String>,
    onUpload: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Auto-detected metadata info
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auto_detected_metadata),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(
                        R.string.detected_language,
                        when (metadata.detectedLanguage) {
                            "hindi" -> stringResource(R.string.language_hindi_detected)
                            "english" -> stringResource(R.string.language_english_detected)
                            "both" -> stringResource(R.string.language_both)
                            else -> stringResource(R.string.language_unknown)
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = stringResource(R.string.confidence_score, (metadata.confidence * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall
                )
                
                if (metadata.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    metadata.warnings.forEach { warning ->
                        Text(
                            text = "⚠️ $warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Editable fields
        Text(
            text = stringResource(R.string.edit_metadata),
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.form_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Category dropdown
        var expandedCategory by remember { mutableStateOf(false) }
        
        ExposedDropdownMenuBox(
            expanded = expandedCategory,
            onExpandedChange = { expandedCategory = it }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.category)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expandedCategory,
                onDismissRequest = { expandedCategory = false }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            onCategoryChange(cat)
                            expandedCategory = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = tags,
            onValueChange = onTagsChange,
            label = { Text(stringResource(R.string.tags)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            supportingText = { Text("Comma-separated keywords") }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 4
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.cancel))
            }
            
            Button(
                onClick = onUpload,
                modifier = Modifier.weight(1f),
                enabled = title.isNotBlank() && category.isNotBlank()
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.upload))
            }
        }
    }
}
