// File: app/src/main/java/com/viplove/licadvisornative/MainActivity.kt
package com.viplove.licadvisornative

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons // <-- NEW IMPORT
import androidx.compose.material.icons.filled.Visibility // <-- NEW IMPORT
import androidx.compose.material.icons.filled.VisibilityOff // <-- NEW IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation // <-- IMPORT
import androidx.compose.ui.text.input.VisualTransformation // <-- NEW IMPORT
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
import com.viplove.licadvisornative.network.TokenManager
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
            val role = TokenManager.getUserRole()
            Log.d(TAG, "SplashScreen: Token found. Role: '$role'")
            val destination = when (role) {
                "admin" -> "admin_dashboard"
                "superadmin" -> "superadmin_dashboard"
                else -> "agent_dashboard"
            }
            safeNavigate(destination)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.mipmap.logo_foreground),
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp)
        )

        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.login_prompt), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.tertiary)
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester),
            singleLine = true,
            // --- NEW: Toggle visibility ---
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // --- NEW: Add the icon button ---
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else
                    Icons.Filled.VisibilityOff

                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, description)
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
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
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.remember_me),
                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                )
            }

            TextButton(
                onClick = { navController.navigate("forgot_password") }
            ) {
                Text(stringResource(R.string.forgot_password))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                focusManager.clearFocus()
                loginViewModel.loginUser(email, password, rememberMe)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = uiState !is LoginViewModel.LoginUiState.Loading
        ) {
            if (uiState is LoginViewModel.LoginUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.login_button), fontSize = 16.sp)
            }
        }
        TextButton(onClick = { navController.navigate("register") }) {
            Text(stringResource(R.string.signup_prompt))
        }
    }
}