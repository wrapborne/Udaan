// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/FooterSelectionScreen.kt
package com.viplove.licadvisornative.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 2: Select a Footer Style") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.footers) { footer ->
                    Card(
                        onClick = {
                            val encodedMainUrl = URLEncoder.encode(mainImageUrl, StandardCharsets.UTF_8.toString())
                            val encodedFooterUrl = URLEncoder.encode(footer.imageUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate("graphics_editor/$templateId?mainImageUrl=$encodedMainUrl&footerImageUrl=$encodedFooterUrl")
                        },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column {
                            Image(
                                painter = rememberAsyncImagePainter(model = footer.imageUrl),
                                contentDescription = footer.name,
                                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = footer.name,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
