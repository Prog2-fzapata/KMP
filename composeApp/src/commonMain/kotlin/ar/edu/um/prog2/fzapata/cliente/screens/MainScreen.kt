package ar.edu.um.prog2.fzapata.cliente.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.viewmodel.ResumeViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator

// La pantalla de quien tiene sesión: las secciones con la barra inferior. Los horarios y la reserva se abren
// con el Navigator de afuera, así que quedan por encima y tapan la barra.
class MainScreen : Screen {

    private val tabs = listOf<Tab>(ProfessionalListScreen(), ProfileScreen())

    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        val navigator = LocalNavigator.currentOrThrow
        val resume = viewModel(key = "$key-resume") { ResumeViewModel(container.turnosApi) }
        val resumeId by resume.reservationId.collectAsState()

        // Al abrir la app con una reserva en curso se vuelve a ella; las secciones quedan debajo
        LaunchedEffect(resumeId) {
            resumeId?.let {
                navigator.push(ReservationScreen(it))
                resume.onResumed() // al volver no se abre otra vez
            }
        }

        TabNavigator(tabs.first()) {
            val tabNavigator = LocalTabNavigator.current
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val options = tab.options
                            NavigationBarItem(
                                selected = tabNavigator.current.key == tab.key,
                                onClick = { tabNavigator.current = tab },
                                icon = { options.icon?.let { Icon(it, contentDescription = options.title) } },
                                label = { Text(options.title) }
                            )
                        }
                    }
                }
            ) { padding ->
                // Lo que ya cubre la barra no se vuelve a descontar dentro de cada sección
                Box(Modifier.padding(padding).consumeWindowInsets(padding)) { CurrentTab() }
            }
        }
    }
}
