package ar.edu.um.prog2.fzapata.cliente.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.screens.components.Centered
import ar.edu.um.prog2.fzapata.cliente.screens.components.DaySlots
import ar.edu.um.prog2.fzapata.cliente.screens.components.LoadMoreWhenNearEnd
import ar.edu.um.prog2.fzapata.cliente.screens.components.RetryMessage
import ar.edu.um.prog2.fzapata.cliente.viewmodel.AvailabilityViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

// Los horarios libres de un profesional, por día; al llegar al final de la lista pide los 30 días que siguen
class AvailabilityScreen(
    private val professionalId: Long,
    private val professionalName: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class) // TopAppBar aún es experimental
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel(key = key) { AvailabilityViewModel(professionalId, container.turnosApi, container.errorMessages) }
        val state by viewModel.state.collectAsState()
        // Los horarios llegan ordenados: agrupar por día conserva el orden de los días
        val days = remember(state.slots) { state.slots.groupBy { it.date }.entries.toList() }

        val listState = rememberLazyListState()
        LoadMoreWhenNearEnd(listState, state.slots.size, viewModel::loadMore)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(professionalName) },
                    navigationIcon = { TextButton(onClick = navigator::pop) { Text("Volver") } }
                )
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(days, key = { it.key }) { day -> DaySlots(day.key, day.value) }

                if (state.loading || state.loadingMore) {
                    item { Centered { CircularProgressIndicator() } }
                } else if (state.slots.isEmpty() && state.error == null) {
                    item { Centered { Text("No hay horarios libres en el próximo año.") } }
                } else if (!state.hasMore && state.error == null) {
                    item { Centered { Text("No hay más horarios.") } }
                }
                state.error?.let { message ->
                    item { RetryMessage(message, viewModel::retry) }
                }
            }
        }
    }
}
