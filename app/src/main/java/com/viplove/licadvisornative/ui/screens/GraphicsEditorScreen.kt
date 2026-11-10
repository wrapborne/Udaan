package com.viplove.licadvisornative.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.ui.components.PillChip
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.BrandGold
import com.viplove.licadvisornative.ui.theme.BrandNavyContainer
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.theme.TextPrimary
import com.viplove.licadvisornative.ui.theme.TextSecondary
import com.viplove.licadvisornative.ui.viewmodel.GraphicsEditorViewModel
import com.viplove.licadvisornative.ui.viewmodel.GraphicsEditorViewModel.EditorEvent
import com.viplove.licadvisornative.ui.viewmodel.GraphicsEditorViewModel.ExportAction
import com.viplove.licadvisornative.util.GraphicLayout
import com.viplove.licadvisornative.util.GraphicSpec
import com.viplove.licadvisornative.util.NavArgs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsEditorScreen(
    navController: NavController,
    mainImageUrl: String,
    footerImageUrl: String,
    editorViewModel: GraphicsEditorViewModel = viewModel()
) {
    val uiState by editorViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val decodedMainUrl = remember(mainImageUrl) { NavArgs.decodeUrl(mainImageUrl) }
    val decodedFooterUrl = remember(footerImageUrl) { NavArgs.decodeUrl(footerImageUrl).takeIf { it.isNotBlank() } }

    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> uri?.let { editorViewModel.uploadProfilePicture(context, it) } }
    )

    LaunchedEffect(editorViewModel) {
        editorViewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Message -> Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
                is EditorEvent.Share -> shareGraphic(context, event.uri)
            }
        }
    }

    val user = uiState.user
    val spec = GraphicSpec(
        mainImageUrl = decodedMainUrl,
        footerImageUrl = decodedFooterUrl,
        profileImage = uiState.profileImage ?: R.mipmap.logo_foreground,
        name = user?.name.takeIf { !it.isNullOrBlank() } ?: "Your Name",
        role = when (user?.role) {
            "admin" -> "Development Officer"
            "advisor" -> "Financial Advisor"
            else -> "LIC Professional"
        },
        phone = user?.phone.takeIf { !it.isNullOrBlank() } ?: "+91 12345 67890"
    )
    val canExport = user != null && uiState.exporting == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize graphic", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = Dimens.GutterXs) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(Dimens.ScreenHPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
                ) {
                    OutlinedButton(
                        onClick = { editorViewModel.export(context, spec, ExportAction.SAVE) },
                        enabled = canExport,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        ExportButtonContent(
                            busy = uiState.exporting == ExportAction.SAVE,
                            icon = { Icon(Icons.Default.Download, contentDescription = null) },
                            label = "Save"
                        )
                    }
                    Button(
                        onClick = { editorViewModel.export(context, spec, ExportAction.SHARE) },
                        enabled = canExport,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        ExportButtonContent(
                            busy = uiState.exporting == ExportAction.SHARE,
                            icon = { Icon(Icons.Default.Share, contentDescription = null) },
                            label = "Share"
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenHPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(Dimens.GutterXl), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                user == null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
                    ) {
                        Text("Could not load your profile.")
                        OutlinedButton(onClick = editorViewModel::retry) { Text("Try again") }
                    }
                }
                else -> {
                    SectionCard {
                        GraphicPreview(spec = spec)
                    }
                    Text(
                        "Your photo, name and phone number come from your profile. Share sends the image to " +
                            "WhatsApp or any app; Save puts it in your gallery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            PillChip(
                                label = if (uiState.isUploading) "Uploading photo…" else "Change photo",
                                selected = false,
                                leadingIcon = Icons.Filled.Person,
                                onClick = {
                                    if (!uiState.isUploading) profileImagePickerLauncher.launch("image/*")
                                }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                leadingIcon = { Icon(Icons.Filled.TextFields, contentDescription = "Text") },
                                label = { Text("Text") }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = "Color") },
                                label = { Text("Color") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportButtonContent(busy: Boolean, icon: @Composable () -> Unit, label: String) {
    if (busy) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
    } else {
        icon()
    }
    Spacer(Modifier.width(Dimens.GutterSm))
    Text(label)
}

private fun shareGraphic(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Share graphic"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to share the image.", Toast.LENGTH_LONG).show()
    }
}

/** On-screen preview, laid out with [GraphicLayout] so it matches the exported image. */
@Composable
private fun GraphicPreview(spec: GraphicSpec) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
    ) {
        val side = maxWidth
        val density = LocalDensity.current
        fun textSize(fraction: Float) = with(density) { (side * fraction).toSp() }

        AsyncImage(
            model = spec.mainImageUrl,
            contentDescription = "Graphic template",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        spec.footerImageUrl?.let { footer ->
            AsyncImage(
                model = footer,
                contentDescription = "Footer graphic",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                contentScale = ContentScale.FillWidth
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(side * GraphicLayout.PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = spec.profileImage,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(side * GraphicLayout.AVATAR)
                    .clip(CircleShape)
                    .background(BrandNavyContainer)
                    .border(side * GraphicLayout.AVATAR_RING, Color.White, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(side * GraphicLayout.TEXT_GAP))
            Column(verticalArrangement = Arrangement.spacedBy(side * GraphicLayout.LINE_GAP)) {
                Text(
                    text = spec.name,
                    fontSize = textSize(GraphicLayout.NAME_TEXT),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = spec.role,
                    fontSize = textSize(GraphicLayout.ROLE_TEXT),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = spec.phone,
                    fontSize = textSize(GraphicLayout.PHONE_TEXT),
                    color = BrandGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
