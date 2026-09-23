package com.viplove.licadvisornative.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.model.Circular
import com.viplove.licadvisornative.model.DocumentCategories
import com.viplove.licadvisornative.ui.components.AppSearchBar
import com.viplove.licadvisornative.ui.components.EmptyState as AppEmptyState
import com.viplove.licadvisornative.ui.components.PillChip
import com.viplove.licadvisornative.ui.components.UploadCircularDialog
import com.viplove.licadvisornative.ui.theme.BrandDanger
import com.viplove.licadvisornative.ui.theme.BrandDangerBg
import com.viplove.licadvisornative.ui.theme.BrandNavyContainer
import com.viplove.licadvisornative.ui.theme.ChartBlue
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.CircularsViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

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
    val canUpload = userRole == "admin" || userRole == "superadmin"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.circulars_tab), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    if (canUpload) {
                        IconButton(onClick = { showUploadDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = stringResource(R.string.upload_circular),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenHPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
        ) {
            AppSearchBar(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = stringResource(R.string.search_circulars)
            )
            CircularCategoryPillsRow(
                selectedCategory = selectedCategory,
                categories = DocumentCategories.ALL_CIRCULAR_CATEGORIES,
                onCategorySelected = { viewModel.updateCategoryFilter(it) }
            )
            Text(
                text = "${circulars.size} circulars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.GutterXs)
            )
            if (circulars.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Filled.Article,
                    title = stringResource(R.string.empty_circulars_title),
                    message = stringResource(R.string.empty_circulars_message),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = Dimens.GutterSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
                ) {
                    items(circulars) { circular ->
                        CircularCard(
                            circular = circular,
                            onPreview = { previewCircular(context, circular, viewModel) },
                            onDownload = { downloadCircular(context, circular, viewModel) },
                            canDelete = canUpload,
                            onDelete = { viewModel.deleteCircular(circular.circularId, circular.fileExtension) }
                        )
                    }
                }
            }
        }
    }

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
private fun CircularCategoryPillsRow(
    selectedCategory: String?,
    categories: List<String>,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
        contentPadding = PaddingValues(vertical = Dimens.GutterXs)
    ) {
        item {
            PillChip(
                label = stringResource(R.string.all_categories),
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        items(categories.distinct()) { category ->
            PillChip(
                label = category,
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.GutterMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
        ) {
            CircularDocumentTypeIcon(fileType = circular.fileType.ifBlank { circular.fileExtension })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circular.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${circular.category} · ${formatDateTime(circular.uploadedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (circular.description.isNotBlank()) {
                    Text(
                        text = circular.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${circularLanguageLabel(circular.language)} · ${formatFileSize(circular.fileSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GutterXs)) {
                IconButton(onClick = onPreview) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = stringResource(R.string.preview),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.download),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_document),
                            tint = BrandDanger
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularDocumentTypeIcon(fileType: String) {
    val normalized = fileType.lowercase()
    val icon: ImageVector
    val background: Color
    val tint: Color
    when {
        normalized.contains("pdf") -> {
            icon = Icons.Filled.PictureAsPdf
            background = BrandDangerBg
            tint = BrandDanger
        }
        normalized.contains("doc") -> {
            icon = Icons.Filled.Description
            background = BrandNavyContainer
            tint = ChartBlue
        }
        else -> {
            icon = Icons.Filled.InsertDriveFile
            background = MaterialTheme.colorScheme.surfaceVariant
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = background,
        contentColor = tint,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Document type",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun circularLanguageLabel(language: String): String {
    return when (language) {
        "hindi" -> stringResource(R.string.language_hindi_detected)
        "english" -> stringResource(R.string.language_english_detected)
        "both" -> stringResource(R.string.language_both)
        else -> stringResource(R.string.language_unknown)
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
    MainScope().launch {
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
    MainScope().launch {
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
