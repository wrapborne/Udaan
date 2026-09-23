package com.viplove.licadvisornative.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.viplove.licadvisornative.model.GraphicTemplate
import com.viplove.licadvisornative.ui.components.EmptyState
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.GraphicsViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun GraphicsSelectionScreen(
    navController: NavController,
    graphicsViewModel: GraphicsViewModel = viewModel()
) {
    val uiState by graphicsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.templates.isEmpty() -> {
            EmptyState(
                icon = Icons.Filled.PhotoLibrary,
                title = "No templates",
                message = "No graphic templates are available yet."
            )
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.ScreenHPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd),
                horizontalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
            ) {
                items(uiState.templates) { template ->
                    GraphicTemplateGridItem(
                        template = template,
                        onClick = {
                            val encodedUrl = URLEncoder.encode(template.imageUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate("graphics_footer_selection/${template.id}?imageUrl=$encodedUrl")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GraphicTemplateGridItem(
    template: GraphicTemplate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = template.imageUrl,
                contentDescription = template.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f),
                contentScale = ContentScale.Crop
            )
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(Dimens.GutterSm)
                )
            }
        }
    }
}
