package ar.edu.um.prog2.fzapata.cliente.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import ar.edu.um.prog2.fzapata.cliente.screens.components.Centered
import ar.edu.um.prog2.fzapata.cliente.screens.components.LoadMoreWhenNearEnd
import ar.edu.um.prog2.fzapata.cliente.screens.components.ReservationCard
import ar.edu.um.prog2.fzapata.cliente.screens.components.RetryMessage
import ar.edu.um.prog2.fzapata.cliente.viewmodel.MyReservationsViewModel
import ar.edu.um.prog2.fzapata.cliente.viewmodel.ReservationStep
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

class MyReservationsScreen : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 1u, title = "Mis reservas", icon = rememberVectorPainter(Icons.Default.DateRange))

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        // Dentro de una pestaña, LocalNavigator es el de las pestañas; el de afuera (parent) es el que apila por encima de la barra
        val navigator = checkNotNull(LocalNavigator.currentOrThrow.parent)
        val viewModel = viewModel(key = key) { MyReservationsViewModel(container.turnosApi, container.reservationStream, container.errorMessages) }
        val state by viewModel.state.collectAsState()

        // Escucha los avisos mientras la lista se ve: al taparla otra pantalla o cambiar de pestaña, la conexión se cierra
        LaunchedEffect(viewModel) { viewModel.watch() }

        val listState = rememberLazyListState()
        LoadMoreWhenNearEnd(listState, state.reservations.size, viewModel::loadMore)

        Scaffold(topBar = { TopAppBar(title = { Text("Mis reservas") }) }) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FILTERS) { (status, label) ->
                            FilterChip(selected = state.status == status, onClick = { viewModel.onStatusSelected(status) }, label = { Text(label) })
                        }
                    }
                }

                if (state.loading) {
                    item { Centered { CircularProgressIndicator() } }
                } else if (state.reservations.isEmpty() && state.error == null) {
                    item { Centered { Text(if (state.status == null) "Todavía no tenés reservas." else "No hay reservas con ese estado.") } }
                } else {
                    val sections = state.sections
                    if (sections.upcoming.isNotEmpty()) {
                        item { SectionTitle("Próximas") }
                        items(sections.upcoming, key = { it.id }) { reservation -> ReservationCard(reservation, onClick = { navigator.push(screenFor(reservation)) }) }
                    }
                    if (sections.past.isNotEmpty()) {
                        item { SectionTitle("Anteriores") }
                        items(sections.past, key = { it.id }) { reservation -> ReservationCard(reservation, onClick = { navigator.push(screenFor(reservation)) }) }
                    }
                }

                if (state.loadingMore) {
                    item { Centered { CircularProgressIndicator() } }
                }
                state.error?.let { message ->
                    item { RetryMessage(message, viewModel::retry) }
                }
            }
        }
    }

    // Una confirmada o cancelada se ve en su detalle; una que falta confirmar sigue donde estaba, para completar el teléfono
    private fun screenFor(reservation: Reservation): Screen = when (ReservationStep.of(reservation)) {
        ReservationStep.CONFIRMED, ReservationStep.OVER -> ReservationDetailScreen(reservation.id)
        else -> ReservationScreen(reservation.id, fromList = true)
    }

    private companion object {
        // Un estado por chip: el filtro del servicio admite uno solo, así que no hay uno para «en curso»
        val FILTERS = listOf<Pair<String?, String>>(null to "Todas", "CONFIRMED" to "Confirmadas", "CANCELLED" to "Canceladas")
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
