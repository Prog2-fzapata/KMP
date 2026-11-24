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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.screens.components.FormField
import ar.edu.um.prog2.fzapata.cliente.screens.components.LabeledCheckbox
import ar.edu.um.prog2.fzapata.cliente.viewmodel.LoginViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        val navigator = LocalNavigator.currentOrThrow
        // Con la clave de esta pantalla, un login nuevo (tras cerrar sesión) arranca con el estado vacío
        val viewModel = viewModel(key = key) { LoginViewModel(container.authApi, container.session, container.errorMessages) }
        val state by viewModel.state.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Iniciar sesión", style = MaterialTheme.typography.headlineMedium)

            FormField("Usuario o email", state.username, viewModel::onUsernameChange, enabled = !state.loading)
            FormField("Contraseña", state.password, viewModel::onPasswordChange, isPassword = true, enabled = !state.loading)

            LabeledCheckbox("Mantener la sesión abierta", state.rememberMe, viewModel::onRememberMeChange, enabled = !state.loading)

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (state.loading) {
                CircularProgressIndicator()
            } else {
                Button(onClick = viewModel::submit) { Text("Ingresar") }
                TextButton(onClick = { navigator.push(RegisterScreen()) }) { Text("Crear una cuenta") }
            }
        }
    }
}
