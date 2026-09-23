package com.viplove.licadvisornative.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.viplove.licadvisornative.R
import com.viplove.licadvisornative.ui.components.BrandLogo
import com.viplove.licadvisornative.ui.components.LabeledTextField
import com.viplove.licadvisornative.ui.components.PillChip
import com.viplove.licadvisornative.ui.components.SectionCard
import com.viplove.licadvisornative.ui.theme.Dimens
import com.viplove.licadvisornative.ui.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    navController: NavController,
    registrationViewModel: RegistrationViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var userCode by remember { mutableStateOf("") }
    var adminDoCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val roleAdvisor = stringResource(R.string.role_advisor)
    val roleDo = stringResource(R.string.role_do)
    var selectedRole by remember { mutableStateOf(roleAdvisor) }

    val registrationState by registrationViewModel.registrationState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(registrationState) {
        when (val state = registrationState) {
            is RegistrationViewModel.RegistrationState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                navController.navigate("login") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is RegistrationViewModel.RegistrationState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.GutterLg),
            contentAlignment = Alignment.TopCenter
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
                        text = stringResource(R.string.create_account_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.registration_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SectionCard(title = stringResource(R.string.registration_details_title)) {
                    LabeledTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = stringResource(R.string.full_name_label),
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    LabeledTextField(
                        value = phone,
                        onValueChange = { newValue ->
                            if (newValue.length <= 10) {
                                phone = newValue.filter { it.isDigit() }
                            }
                        },
                        label = stringResource(R.string.phone_number_label),
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    LabeledTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = stringResource(R.string.email_address_label),
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }

                SectionCard(title = stringResource(R.string.registration_role_title)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GutterSm)
                    ) {
                        PillChip(
                            label = stringResource(R.string.role_advisor_short),
                            selected = selectedRole == roleAdvisor,
                            onClick = { selectedRole = roleAdvisor },
                            modifier = Modifier.weight(1f)
                        )
                        PillChip(
                            label = stringResource(R.string.role_admin_do_short),
                            selected = selectedRole == roleDo,
                            onClick = { selectedRole = roleDo },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LabeledTextField(
                        value = userCode,
                        onValueChange = { userCode = it },
                        label = if (selectedRole == roleAdvisor) stringResource(R.string.advisor_code_label) else stringResource(R.string.do_code_label),
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    if (selectedRole == roleAdvisor) {
                        LabeledTextField(
                            value = adminDoCode,
                            onValueChange = { adminDoCode = it },
                            label = stringResource(R.string.do_code_label),
                            helper = stringResource(R.string.advisor_do_code_helper),
                            imeAction = ImeAction.Next,
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                    }
                }

                SectionCard(title = stringResource(R.string.registration_password_title)) {
                    LabeledTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.password_label),
                        imeAction = ImeAction.Next,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (passwordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    LabeledTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = stringResource(R.string.confirm_password_label),
                        imeAction = ImeAction.Done,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (confirmPasswordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        registrationViewModel.registerUser(
                            name = name,
                            phone = phone,
                            email = email,
                            userCode = userCode,
                            adminDoCode = adminDoCode,
                            roleDisplayName = selectedRole,
                            password = password,
                            confirmPass = confirmPass,
                            advisorRoleName = roleAdvisor
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeight),
                    enabled = registrationState !is RegistrationViewModel.RegistrationState.Loading,
                    shape = MaterialTheme.shapes.small
                ) {
                    if (registrationState is RegistrationViewModel.RegistrationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.register_button))
                    }
                }
                TextButton(onClick = { navController.navigate("login") }) {
                    Text(stringResource(R.string.login_prompt_from_register))
                }
                Spacer(modifier = Modifier.height(Dimens.GutterLg))
            }
        }
    }
}
