package com.viplove.licadvisornative.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.viplove.licadvisornative.model.GraphicFooter
import com.viplove.licadvisornative.model.GraphicTemplate
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.GraphicUploadDraft
import com.viplove.licadvisornative.ui.viewmodel.GraphicsViewModel
import com.viplove.licadvisornative.ui.viewmodel.GraphicsViewModel.GraphicKind

private val roleOptions = listOf(
    GraphicUploadDraft.ROLE_ALL to "All users",
    GraphicUploadDraft.ROLE_ADMIN to "Admins only",
    GraphicUploadDraft.ROLE_ADVISOR to "Advisors only"
)

/** Something the superadmin asked to delete, waiting for confirmation. */
private sealed interface PendingDelete {
    val name: String
    data class Template(val template: GraphicTemplate) : PendingDelete { override val name get() = template.name }
    data class Footer(val footer: GraphicFooter) : PendingDelete { override val name get() = footer.name }
}

@Composable
fun GraphicsManagementScreen(
    graphicsViewModel: GraphicsViewModel = viewModel()
) {
    val uiState by graphicsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    LaunchedEffect(graphicsViewModel) {
        graphicsViewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    val templatePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { graphicsViewModel.onImageSelected(context, GraphicKind.TEMPLATE, it) }
    }
    val footerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { graphicsViewModel.onImageSelected(context, GraphicKind.FOOTER, it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenHPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
    ) {
        item {
            UploadForm(
                title = "Upload new template",
                namePlaceholder = "Template name",
                draft = uiState.templateDraft,
                showRole = true,
                onNameChange = { graphicsViewModel.onNameChanged(GraphicKind.TEMPLATE, it) },
                onRoleChange = graphicsViewModel::onRoleChanged,
                onPickImage = { templatePicker.launch("image/*") },
                onClearImage = { graphicsViewModel.clearImage(GraphicKind.TEMPLATE) },
                onUpload = { graphicsViewModel.upload(GraphicKind.TEMPLATE) }
            )
        }

        item {
            SectionHeader(
                title = "Templates (${uiState.templates.size})",
                onRefresh = graphicsViewModel::refresh
            )
        }
        when {
            uiState.isLoading && uiState.templates.isEmpty() -> item { LoadingRow() }
            uiState.templates.isEmpty() -> item { Text("No templates yet. Upload one above.") }
            else -> items(uiState.templates, key = { "t_${it.id}" }) { template ->
                GraphicTemplateCard(template = template, onDelete = { pendingDelete = PendingDelete.Template(template) })
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GutterSm))
            UploadForm(
                title = "Upload new footer",
                namePlaceholder = "Footer name",
                draft = uiState.footerDraft,
                showRole = false,
                onNameChange = { graphicsViewModel.onNameChanged(GraphicKind.FOOTER, it) },
                onRoleChange = {},
                onPickImage = { footerPicker.launch("image/*") },
                onClearImage = { graphicsViewModel.clearImage(GraphicKind.FOOTER) },
                onUpload = { graphicsViewModel.upload(GraphicKind.FOOTER) },
                hint = "Footers sit along the bottom of every graphic, under the advisor's photo, name and phone. " +
                    "Use a wide PNG with a light background."
            )
        }

        item { SectionHeader(title = "Footers (${uiState.footers.size})") }
        if (uiState.footers.isEmpty()) {
            item { Text("No footers yet. Upload one above.") }
        } else {
            items(uiState.footers, key = { "f_${it.id}" }) { footer ->
                GraphicFooterCard(footer = footer, onDelete = { pendingDelete = PendingDelete.Footer(footer) })
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${target.name}\"?") },
            text = { Text("It will be removed for every user. This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        when (target) {
                            is PendingDelete.Template -> graphicsViewModel.deleteGraphicTemplate(target.template)
                            is PendingDelete.Footer -> graphicsViewModel.deleteGraphicFooter(target.footer)
                        }
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun UploadForm(
    title: String,
    namePlaceholder: String,
    draft: GraphicUploadDraft,
    showRole: Boolean,
    onNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onUpload: () -> Unit,
    hint: String? = null
) {
    SectionCard(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)) {
            hint?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                value = draft.name,
                onValueChange = onNameChange,
                label = { Text(namePlaceholder) },
                singleLine = true,
                enabled = !draft.isUploading,
                modifier = Modifier.fillMaxWidth()
            )
            if (showRole) {
                Text("Visible to", style = MaterialTheme.typography.labelLarge)
                Column {
                    roleOptions.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = draft.visibleToRole == key,
                                    enabled = !draft.isUploading,
                                    onClick = { onRoleChange(key) }
                                )
                                .heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = draft.visibleToRole == key, onClick = null, enabled = !draft.isUploading)
                            Spacer(Modifier.width(Dimens.GutterSm))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            draft.image?.let { image ->
                Box {
                    AsyncImage(
                        model = image.file,
                        contentDescription = "Selected image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    if (!draft.isUploading) {
                        FilledTonalIconButton(
                            onClick = onClearImage,
                            modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.GutterXs)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove selected image")
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPickImage,
                    enabled = !draft.isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(Dimens.GutterXs))
                    Text(if (draft.image != null) "Change image" else "Select image")
                }
                Button(
                    onClick = onUpload,
                    enabled = draft.canUpload,
                    modifier = Modifier.weight(1f)
                ) {
                    if (draft.isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(Dimens.GutterSm))
                        Text("Uploading…")
                    } else {
                        Text("Upload")
                    }
                }
            }
            if (!draft.isUploading && !draft.canUpload) {
                val missing = listOfNotNull(
                    "an image".takeIf { draft.image == null },
                    "a name".takeIf { draft.name.isBlank() }
                )
                Text(
                    "Add ${missing.joinToString(" and ")} to upload.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onRefresh: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        onRefresh?.let {
            IconButton(onClick = it) { Icon(Icons.Default.Refresh, contentDescription = "Refresh templates and footers") }
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(modifier = Modifier.fillMaxWidth().padding(Dimens.GutterLg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun GraphicTemplateCard(template: GraphicTemplate, onDelete: () -> Unit) {
    GraphicListCard(
        imageUrl = template.imageUrl,
        name = template.name,
        subtitle = when (template.visibleToRole) {
            GraphicUploadDraft.ROLE_ADMIN -> "Admins only"
            GraphicUploadDraft.ROLE_ADVISOR -> "Advisors only"
            else -> "All users"
        },
        deleteDescription = "Delete template ${template.name}",
        onDelete = onDelete
    )
}

@Composable
fun GraphicFooterCard(footer: GraphicFooter, onDelete: () -> Unit) {
    GraphicListCard(
        imageUrl = footer.imageUrl,
        name = footer.name,
        subtitle = null,
        deleteDescription = "Delete footer ${footer.name}",
        onDelete = onDelete
    )
}

@Composable
private fun GraphicListCard(
    imageUrl: String,
    name: String,
    subtitle: String?,
    deleteDescription: String,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Dimens.GutterSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(Dimens.GutterLg))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = deleteDescription, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
