// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/DocumentUploadScreen.kt
package com.viplove.licadvisornative.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.viplove.licadvisornative.BuildConfig
import com.viplove.licadvisornative.model.ClientDataSheet
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.BrandDangerBg
import com.viplove.licadvisornative.ui.theme.BrandNavyContainer
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.DocumentUploadViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import java.util.Locale

@Composable
fun DocumentUploadStep(
    dataSheet: ClientDataSheet,
    onDataChange: (ClientDataSheet) -> Unit,
    docViewModel: DocumentUploadViewModel = viewModel()
) {
    val uiState by docViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var currentlyScanningSlot by remember { mutableStateOf<DocumentUploadViewModel.DocumentSlot?>(null) }
    var tempPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val slotToUpdate = currentlyScanningSlot ?: return@rememberLauncherForActivityResult
                try {
                    val intent: Intent? = result.data
                    val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(intent)
                    scanningResult?.pages?.firstOrNull()?.imageUri?.let { uri ->
                        val updatedSheet = docViewModel.updateDocumentUri(dataSheet, slotToUpdate, uri)
                        onDataChange(updatedSheet)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    currentlyScanningSlot = null
                }
            } else {
                currentlyScanningSlot = null
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempPhotoUri?.let { uri ->
                    val slotToUpdate = currentlyScanningSlot ?: return@let
                    val updatedSheet = docViewModel.updateDocumentUri(dataSheet, slotToUpdate, uri)
                    onDataChange(updatedSheet)
                }
            }
            currentlyScanningSlot = null
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createImageUri(context)
                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
                currentlyScanningSlot = null
            }
        }
    )

    LaunchedEffect(dataSheet) {
        docViewModel.generateDocumentSlots(dataSheet)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenHPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
    ) {
        item {
            SectionCard(title = "Document slots") {
                Text(
                    "Upload the required documents. Camera and document scanner options are available per slot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(uiState.documentSlots) { slot ->
            DocumentSlotCard(
                slot = slot,
                onScanClick = {
                    currentlyScanningSlot = slot
                    val options = GmsDocumentScannerOptions.Builder()
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .setGalleryImportAllowed(true)
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setPageLimit(1)
                        .build()
                    val scanner = GmsDocumentScanning.getClient(options)
                    scanner.getStartScanIntent(context as Activity)
                        .addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                        .addOnFailureListener {
                            currentlyScanningSlot = null
                        }
                },
                onPhotoClick = {
                    currentlyScanningSlot = slot
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }

        item {
            Text("Virtual A4 Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 400.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                A4PreviewLayout(slots = uiState.documentSlots)
            }
        }
    }
}

@Composable
fun A4PreviewLayout(slots: List<DocumentUploadViewModel.DocumentSlot>) {
    val uploadedSlots = slots.filter { it.uploadedUri != null }

    if (uploadedSlots.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("Previews will appear here")
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        uploadedSlots.forEach { slot ->
            val modifier = when(slot.documentType) {
                "photo" -> Modifier.width(120.dp)
                "bank", "nach_bank" -> Modifier.fillMaxWidth(0.9f)
                else -> Modifier.fillMaxWidth(0.8f)
            }
            PreviewImageCard(slot = slot, modifier = modifier)
        }
    }
}


@Composable
fun PreviewImageCard(slot: DocumentUploadViewModel.DocumentSlot, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        val aspectRatio = when(slot.documentType) {
            "photo" -> 3.5f / 4.5f
            "bank", "nach_bank" -> 16f / 9f
            else -> 1.58f
        }

        Card(elevation = CardDefaults.cardElevation(2.dp)) {
            Image(
                painter = rememberAsyncImagePainter(model = Uri.parse(slot.uploadedUri)),
                contentDescription = slot.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = slot.label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

private fun createImageUri(context: Context): Uri {
    val imageDir = File(context.filesDir, "images")
    imageDir.mkdirs()

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFile = File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        imageDir
    )

    val authority = "${BuildConfig.APPLICATION_ID}.provider"

    return FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        authority,
        imageFile
    )
}

@Composable
fun DocumentSlotCard(
    slot: DocumentUploadViewModel.DocumentSlot,
    onScanClick: () -> Unit,
    onPhotoClick: () -> Unit
) {
    val uploadAction = if (slot.documentType == "photo") onPhotoClick else onScanClick
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (slot.uploadedUri == null) 1.5.dp else 0.5.dp,
            color = if (slot.uploadedUri == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        if (slot.uploadedUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.GutterMd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
            ) {
                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = Uri.parse(slot.uploadedUri)),
                        contentDescription = "${slot.label} preview",
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(slot.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (slot.isMandatory) "Mandatory · Uploaded" else "Optional · Uploaded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = uploadAction) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Replace ${slot.label}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.GutterLg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (slot.documentType == "photo") BrandDangerBg else BrandNavyContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (slot.documentType == "photo") Icons.Default.AddAPhoto else Icons.Filled.DocumentScanner,
                            contentDescription = slot.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(slot.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text(
                    text = if (slot.isMandatory) "Mandatory · Tap to upload PDF / image" else "Optional · Tap to upload PDF / image",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(onClick = uploadAction, shape = MaterialTheme.shapes.small) {
                    Icon(
                        imageVector = if (slot.documentType == "photo") Icons.Default.AddAPhoto else Icons.Filled.DocumentScanner,
                        contentDescription = if (slot.documentType == "photo") "Take photo" else "Scan document"
                    )
                    Spacer(modifier = Modifier.width(Dimens.GutterSm))
                    Text(if (slot.documentType == "photo") "Take photo" else "Scan document")
                }
            }
        }
    }
}
