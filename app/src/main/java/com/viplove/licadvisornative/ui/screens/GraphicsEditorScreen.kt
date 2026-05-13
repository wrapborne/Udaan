// File: app/src/main/java/com/viplove/licadvisornative/ui/screens/GraphicsEditorScreen.kt
package com.viplove.licadvisornative.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.viplove.licadvisornative.R
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
                title = { Text("Customize Graphic") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Implement download logic */ }) {
                Icon(Icons.Default.Download, contentDescription = "Download Graphic")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.user == null) {
                Text("Could not load user data. Please try again.")
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    // Layer 1: Main Background Image
                    Image(
                        painter = rememberAsyncImagePainter(model = decodedMainUrl),
                        contentDescription = "Graphic Template",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Layer 2: Footer Image
                    Image(
                        painter = rememberAsyncImagePainter(model = decodedFooterUrl),
                        contentDescription = "Footer Graphic",
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                        contentScale = ContentScale.FillWidth
                    )

                    // Layer 3: User Details
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp) // Adjust padding to fit footer
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val profilePicUrl = uiState.user?.profilePictureUrl
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(4.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = profilePicUrl.takeIf { !it.isNullOrEmpty() } ?: R.mipmap.logo_foreground // A fallback image
                                    ),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                val titleText = when (uiState.user?.role) {
                                    "admin" -> "Development Officer"
                                    "advisor" -> "Financial Advisor"
                                    else -> "LIC Professional"
                                }

                                Text(
                                    text = uiState.user?.name.takeIf { !it.isNullOrEmpty() } ?: "Your Name",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                Text(
                                    text = titleText,
                                    fontSize = 16.sp,
                                    color = Color.DarkGray
                                )

                                Text(
                                    text = uiState.user?.phone.takeIf { !it.isNullOrEmpty() } ?: "+91 12345 67890",
                                    fontSize = 16.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
