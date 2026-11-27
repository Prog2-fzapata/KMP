package ar.edu.um.prog2.fzapata.cliente.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.screens.components.Centered
import ar.edu.um.prog2.fzapata.cliente.screens.components.FormField
import ar.edu.um.prog2.fzapata.cliente.screens.components.RetryMessage
import ar.edu.um.prog2.fzapata.cliente.viewmodel.ProfileViewModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

class ProfileScreen : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 2u, title = "Mi perfil", icon = rememberVectorPainter(Icons.Default.Person))

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        val viewModel = viewModel(key = key) { ProfileViewModel(container.userApi, container.errorMessages) }
        val state by viewModel.state.collectAsState()
        val errors = state.fieldErrors
        val enabled = !state.saving

        Scaffold(topBar = { TopAppBar(title = { Text("Mi perfil") }) }) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    state.loading -> Centered { CircularProgressIndicator() }
                    state.loadError != null -> RetryMessage(state.loadError.orEmpty(), viewModel::retry)
                    else -> {
                        if (state.missingName) {
                            Text("Completá tu nombre y apellido para poder reservar.", color = MaterialTheme.colorScheme.error)
                        }

                        // El usuario no se puede cambiar: el servicio no lo permite
                        FormField("Usuario", state.saved?.login.orEmpty(), {}, enabled = false)
                        FormField("Nombre", state.firstName, viewModel::onFirstNameChange, errors["firstName"], enabled = enabled)
                        FormField("Apellido", state.lastName, viewModel::onLastNameChange, errors["lastName"], enabled = enabled)
                        FormField("Email", state.email, viewModel::onEmailChange, errors["email"], KeyboardType.Email, enabled = enabled)

                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (state.justSaved) Text("Datos guardados")

                        if (state.saving) {
                            Centered { CircularProgressIndicator() }
                        } else {
                            Button(onClick = viewModel::save) { Text("Guardar") }
                        }
                    }
                }

                // Fuera del when: se puede cerrar la sesión aunque la cuenta no cargue
                HorizontalDivider()
                OutlinedButton(onClick = container.session::logout) { Text("Cerrar sesión") }
            }
        }
    }
}
