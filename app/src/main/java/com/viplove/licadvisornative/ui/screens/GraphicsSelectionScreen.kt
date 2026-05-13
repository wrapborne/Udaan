// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/GraphicsSelectionScreen.kt
package com.viplove.licadvisornative.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.viplove.licadvisornative.model.GraphicTemplate
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

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.templates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No graphic templates are available yet.", modifier = Modifier.padding(16.dp))
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.templates) { template ->
                GraphicTemplateGridItem(
                    template = template,
                    onClick = {
                        val encodedUrl = URLEncoder.encode(template.imageUrl, StandardCharsets.UTF_8.toString())
                        // UPDATED NAVIGATION
                        navController.navigate("graphics_footer_selection/${template.id}?imageUrl=$encodedUrl")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicTemplateGridItem(
    template: GraphicTemplate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(model = template.imageUrl),
                contentDescription = template.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
