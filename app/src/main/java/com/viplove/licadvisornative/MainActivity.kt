// File: app/src/main/java/com/viplove/licadvisornative/MainActivity.kt
package com.viplove.licadvisornative

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons // <-- NEW IMPORT
import androidx.compose.material.icons.filled.Visibility // <-- NEW IMPORT
import androidx.compose.material.icons.filled.VisibilityOff // <-- NEW IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation // <-- IMPORT
import androidx.compose.ui.text.input.VisualTransformation // <-- NEW IMPORT
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.ui.components.BrandLogo
import com.viplove.licadvisornative.ui.components.LabeledTextField
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.BrandGoldSoft
import com.viplove.licadvisornative.ui.theme.BrandNavy
import com.viplove.licadvisornative.ui.theme.BrandNavyContainer
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.screens.*
import com.viplove.licadvisornative.ui.theme.LICAdvisorNativeTheme
import com.viplove.licadvisornative.ui.viewmodel.LoginViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private const val TAG = "LIC_ADVISOR_DEBUG"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        Log.d(TAG, "MainActivity onCreate")
        setContent {
            LICAdvisorNativeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigator()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = Unit) {
        Log.d(TAG, "SplashScreen: LaunchedEffect started.")

        val safeNavigate: (String) -> Unit = { destination ->
            if (navController.currentBackStackEntry?.destination?.route == "splash") {
                Log.d(TAG, "SplashScreen: Safely navigating to '$destination'")
                navController.navigate(destination) {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                Log.w(TAG, "SplashScreen: Navigation blocked. Not on splash screen anymore.")
            }
        }

        if (!TokenManager.isLoggedIn()) {
            Log.d(TAG, "SplashScreen: No token found.")
            safeNavigate("login")
        } else {
            var role = TokenManager.getUserRole()
            if (role.isNullOrBlank()) {
                Log.d(TAG, "SplashScreen: Cached role missing. Refreshing from Firebase profile.")
                val meResponse = runCatching { ApiClient.api.me() }.getOrNull()
                if (meResponse?.isSuccessful == true) {
                    val user = meResponse.body()
                    if (user != null) {
                        TokenManager.saveUserId(user.id)
                        TokenManager.saveUserRole(user.role)
                        role = user.role
                        Log.d(TAG, "SplashScreen: Refreshed role from profile as '$role'")
                    }
                }
            }
            Log.d(TAG, "SplashScreen: Token found. Role: '$role'")
            val destination = when (role) {
                "admin" -> "admin_dashboard"
                "superadmin" -> "superadmin_dashboard"
                "advisor" -> "agent_dashboard"
                else -> {
                    Log.w(TAG, "SplashScreen: Unable to resolve role. Clearing session and returning to login.")
                    TokenManager.clearAll()
                    "login"
                }
            }
            safeNavigate(destination)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrandGoldSoft, BrandNavyContainer)))
            .padding(Dimens.GutterXl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.GutterMd)
        ) {
            BrandLogo(size = 120.dp)
            Text(
                text = "LIC UDAAN",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = BrandNavy,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Loading your session...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = BrandNavy,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}


@Composable
fun AppNavigator() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController = navController) }
        composable("register") { RegistrationScreen(navController = navController) }
        composable("forgot_password") { ForgotPasswordScreen(navController = navController) }
        composable("agent_dashboard") { AgentDashboardScreen(navController = navController) }
        composable("admin_dashboard") { AdminDashboardScreen(navController = navController) }
        composable("superadmin_dashboard") { SuperadminDashboardScreen(navController = navController) }

        composable(
            route = "data_collection/{draftId}?readOnly={readOnly}",
            arguments = listOf(
                navArgument("draftId") { type = NavType.StringType },
                navArgument("readOnly") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            DataCollectionScreen(navController = navController)
        }


        composable(
            route = "review_screen/{formId}",
            arguments = listOf(navArgument("formId") { type = NavType.StringType })
        ) { backStackEntry ->
            ReviewScreen(navController = navController)
        }

        composable(
            route = "graphics_footer_selection/{templateId}?imageUrl={imageUrl}",
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType },
                navArgument("imageUrl") {
                    type = NavType.StringType
                    defaultValue = "" // Provide a default value
                }
            )
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
            val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
            Log.d(TAG, "AppNavigator: Navigated to 'graphics_footer_selection'. TemplateId: $templateId, Raw imageUrl: $imageUrl")

            val decodedUrl = try {
                URLDecoder.decode(imageUrl, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                Log.e(TAG, "AppNavigator: URL decoding failed for imageUrl: '$imageUrl'", e)
                ""
            }


            Log.d(TAG, "AppNavigator: Decoded URL is '$decodedUrl'")
            FooterSelectionScreen(
                navController = navController,
                templateId = templateId,
                mainImageUrl = decodedUrl
            )
        }

        composable("graphics_management") {
            GraphicsManagementScreen()
        }

        composable(
            route = "graphics_editor/{templateId}?mainImageUrl={mainImageUrl}&footerImageUrl={footerImageUrl}",
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType },
                navArgument("mainImageUrl") {
                    type = NavType.StringType
                    defaultValue = "" // Use defaultValue for optional args
                },
                navArgument("footerImageUrl") {
                    type = NavType.StringType
                    defaultValue = "" // Use defaultValue for optional args
                }
            )
        ) { backStackEntry ->
            val mainImageUrl = backStackEntry.arguments?.getString("mainImageUrl") ?: ""
            val footerImageUrl = backStackEntry.arguments?.getString("footerImageUrl") ?: ""
            GraphicsEditorScreen(
                navController = navController,
                mainImageUrl = mainImageUrl,
                footerImageUrl = footerImageUrl
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    loginViewModel: LoginViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) } // <-- NEW STATE

    val uiState by loginViewModel.loginUiState.collectAsState()
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = uiState) {
        val state = uiState
        Log.d(TAG, "LoginScreen: UI state changed to ${state::class.java.simpleName}")

        when (state) {
            is LoginViewModel.LoginUiState.Success -> {
                val destination = when (state.userRole.lowercase()) {
                    "advisor" -> "agent_dashboard"
                    "admin" -> "admin_dashboard"
                    "superadmin" -> "superadmin_dashboard"
                    else -> null
                }
                Log.d(TAG, "LoginScreen: Login successful. Role: '${state.userRole}', Destination: '$destination'")
                if (destination != null) {
                    navController.navigate(destination) {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    Log.e(TAG, "LoginScreen: Invalid user role '${state.userRole}'. Cannot navigate.")
                    Toast.makeText(context, "Invalid user role. Please contact support.", Toast.LENGTH_LONG).show()
                    TokenManager.clearAll()
                }
            }
            is LoginViewModel.LoginUiState.Error -> {
                Log.e(TAG, "LoginScreen: Login error: ${state.message}")
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            is LoginViewModel.LoginUiState.Loaded -> {
                email = state.credentials.email
                password = state.credentials.password
                rememberMe = state.credentials.rememberMe
            }
            else -> {
                // Idle or Loading
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Dimens.GutterXl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.GutterLg)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
            ) {
                BrandLogo(size = 64.dp)
                Text(
                    text = "LIC UDAAN",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrandNavy,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = stringResource(R.string.login_subtitle_continue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(title = stringResource(R.string.login_card_title)) {
                LabeledTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(R.string.email_label),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    )
                )
                LabeledTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password_label),
                    modifier = Modifier.focusRequester(passwordFocusRequester),
                    imeAction = ImeAction.Done,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            loginViewModel.loginUser(email, password, rememberMe)
                        }
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it }
                        )
                        Text(
                            text = stringResource(R.string.remember_me),
                            modifier = Modifier.clickable { rememberMe = !rememberMe },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(onClick = { navController.navigate("forgot_password") }) {
                        Text(stringResource(R.string.forgot_password_short))
                    }
                }
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        loginViewModel.loginUser(email, password, rememberMe)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeight),
                    enabled = uiState !is LoginViewModel.LoginUiState.Loading,
                    shape = MaterialTheme.shapes.small
                ) {
                    if (uiState is LoginViewModel.LoginUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.login_button), fontSize = 16.sp)
                    }
                }
            }
            TextButton(onClick = { navController.navigate("register") }) {
                Text(stringResource(R.string.new_advisor_register))
            }
        }
    }
}
