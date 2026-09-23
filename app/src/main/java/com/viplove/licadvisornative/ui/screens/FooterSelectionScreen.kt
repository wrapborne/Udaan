package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.viplove.licadvisornative.model.GraphicFooter
import com.viplove.licadvisornative.ui.components.BottomActionBar
import com.viplove.licadvisornative.ui.components.EmptyState
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.BrandNavy
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.GraphicsViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FooterSelectionScreen(
    navController: NavController,
    templateId: String,
    mainImageUrl: String,
    graphicsViewModel: GraphicsViewModel = viewModel()
) {
    val uiState by graphicsViewModel.uiState.collectAsState()
    var selectedFooter by remember(uiState.footers) { mutableStateOf(uiState.footers.firstOrNull()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick a footer", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                onBack = { navController.popBackStack() },
                onPrimary = {
                    selectedFooter?.let { footer ->
                        val encodedMainUrl = URLEncoder.encode(mainImageUrl, StandardCharsets.UTF_8.toString())
                        val encodedFooterUrl = URLEncoder.encode(footer.imageUrl, StandardCharsets.UTF_8.toString())
                        navController.navigate("graphics_editor/$templateId?mainImageUrl=$encodedMainUrl&footerImageUrl=$encodedFooterUrl")
                    }
                },
                primaryLabel = "Continue"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenHPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
        ) {
            SectionCard(title = "Selected template") {
                AsyncImage(
                    model = mainImageUrl,
                    contentDescription = "Selected template",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = "Footers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (uiState.footers.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Image,
                    title = "No footers",
                    message = "No footer styles are available yet.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = Dimens.FabBottomInset),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
                ) {
                    items(uiState.footers) { footer ->
                        FooterGridCard(
                            footer = footer,
                            selected = selectedFooter?.id == footer.id,
                            onClick = { selectedFooter = footer }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterGridCard(
    footer: GraphicFooter,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (selected) BorderStroke(2.dp, BrandNavy) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            AsyncImage(
                model = footer.imageUrl,
                contentDescription = footer.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )
            Text(
                text = footer.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(Dimens.GutterSm)
            )
        }
    }
}
