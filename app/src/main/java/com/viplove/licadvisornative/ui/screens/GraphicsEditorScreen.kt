package com.viplove.licadvisornative.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.ui.components.PillChip
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.BrandGold
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.GraphicsEditorViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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

    val decodedMainUrl = remember(mainImageUrl) { URLDecoder.decode(mainImageUrl, StandardCharsets.UTF_8.toString()) }
    val decodedFooterUrl = remember(footerImageUrl) { URLDecoder.decode(footerImageUrl, StandardCharsets.UTF_8.toString()) }

    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                editorViewModel.uploadProfilePicture(context, it)
            }
        }
    )

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

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
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Wire existing download graphic action when available */ }) {
                Icon(Icons.Default.Download, contentDescription = "Download Graphic")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenHPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.user == null -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Could not load user data. Please try again.")
                    }
                }
                else -> {
                    SectionCard {
                        GraphicPreview(
                            mainImageUrl = decodedMainUrl,
                            footerImageUrl = decodedFooterUrl,
                            name = uiState.user?.name.takeIf { !it.isNullOrEmpty() } ?: "Your Name",
                            role = when (uiState.user?.role) {
                                "admin" -> "Development Officer"
                                "advisor" -> "Financial Advisor"
                                else -> "LIC Professional"
                            },
                            phone = uiState.user?.phone.takeIf { !it.isNullOrEmpty() } ?: "+91 12345 67890",
                            profilePicUrl = uiState.user?.profilePictureUrl
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            PillChip(
                                label = "Profile pic",
                                selected = false,
                                leadingIcon = Icons.Filled.Person,
                                onClick = { profileImagePickerLauncher.launch("image/*") }
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
private fun GraphicPreview(
    mainImageUrl: String,
    footerImageUrl: String,
    name: String,
    role: String,
    phone: String,
    profilePicUrl: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = mainImageUrl),
            contentDescription = "Graphic template",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = rememberAsyncImagePainter(model = footerImageUrl),
            contentDescription = "Footer graphic",
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            contentScale = ContentScale.FillWidth
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(Dimens.GutterLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(3.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = profilePicUrl.takeIf { !it.isNullOrEmpty() } ?: R.mipmap.logo_foreground
                    ),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(Dimens.GutterMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = phone,
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
