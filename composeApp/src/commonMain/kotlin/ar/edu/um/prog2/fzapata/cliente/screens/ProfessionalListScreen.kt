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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.screens.components.Centered
import ar.edu.um.prog2.fzapata.cliente.screens.components.LabeledSwitch
import ar.edu.um.prog2.fzapata.cliente.screens.components.LoadMoreWhenNearEnd
import ar.edu.um.prog2.fzapata.cliente.screens.components.ProfessionalCard
import ar.edu.um.prog2.fzapata.cliente.screens.components.RetryMessage
import ar.edu.um.prog2.fzapata.cliente.screens.components.SearchField
import ar.edu.um.prog2.fzapata.cliente.screens.components.fullName
import ar.edu.um.prog2.fzapata.cliente.viewmodel.ProfessionalListViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

class ProfessionalListScreen : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = "Profesionales", icon = rememberVectorPainter(Icons.Default.Home))

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        // Dentro de una pestaña, LocalNavigator es el de las pestañas; el de afuera (parent) es el que apila por encima de la barra
        val navigator = checkNotNull(LocalNavigator.currentOrThrow.parent)
        val viewModel = viewModel(key = key) { ProfessionalListViewModel(container.catalogApi, container.errorMessages) }
        val state by viewModel.state.collectAsState()

        val listState = rememberLazyListState() // sabe que elementos se ven y cuántos hay en total
        LoadMoreWhenNearEnd(listState, state.professionals.size, viewModel::loadMore)

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Profesionales") })
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { SearchField(state.nameInput, viewModel::onNameChange, viewModel::onSearch, "Buscar por nombre") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.filters.categoryId == null,
                                onClick = { viewModel.onCategorySelected(null) },
                                label = { Text("Todas") }
                            )
                        }
                        items(state.categories, key = { it.id }) { category ->
                            FilterChip(
                                selected = state.filters.categoryId == category.id,
                                onClick = { viewModel.onCategorySelected(category.id) },
                                label = { Text(category.name) }
                            )
                        }
                    }
                }
                item { LabeledSwitch("Solo habilitados", state.filters.onlyEnabled, viewModel::onOnlyEnabledChange) }
                item { LabeledSwitch("Solo con horarios", state.filters.onlyAvailable, viewModel::onOnlyAvailableChange) }

                if (state.loading) {
                    item { Centered { CircularProgressIndicator() } }
                } else if (state.professionals.isEmpty() && state.error == null) {
                    item { Centered { Text("No hay profesionales con esos filtros.") } }
                }

                items(state.professionals, key = { it.id }) { professional ->
                    ProfessionalCard(professional, onClick = { navigator.push(AvailabilityScreen(professional.id, professional.fullName())) })
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
}
