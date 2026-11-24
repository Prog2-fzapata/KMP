package ar.edu.um.prog2.fzapata.cliente.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.screens.components.FormField
import ar.edu.um.prog2.fzapata.cliente.viewmodel.RegisterViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class RegisterScreen : Screen {

    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel(key = key) { RegisterViewModel(container.authApi, container.session, container.errorMessages) }
        val state by viewModel.state.collectAsState()
        val form = state.form
        val errors = state.fieldErrors
        val enabled = !state.loading

        Column(
            modifier = Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Crear una cuenta", style = MaterialTheme.typography.headlineMedium)

            FormField("Login", form.login, { viewModel.onFormChange(form.copy(login = it)) }, errors["login"], enabled = enabled)
            FormField("Contraseña", form.password, { viewModel.onFormChange(form.copy(password = it)) }, errors["password"], isPassword = true, enabled = enabled)
            FormField("Email", form.email, { viewModel.onFormChange(form.copy(email = it)) }, errors["email"], KeyboardType.Email, enabled = enabled)
            FormField("Nombre (opcional)", form.firstName, { viewModel.onFormChange(form.copy(firstName = it)) }, errors["firstName"], enabled = enabled)
            FormField("Apellido (opcional)", form.lastName, { viewModel.onFormChange(form.copy(lastName = it)) }, errors["lastName"], enabled = enabled)

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (state.loading) {
                CircularProgressIndicator()
            } else {
                Button(onClick = viewModel::submit) { Text("Registrarme") }
                TextButton(onClick = { navigator.pop() }) { Text("Ya tengo cuenta") }
            }
        }
    }
}
