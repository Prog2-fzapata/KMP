package ar.edu.um.prog2.fzapata.cliente.screens.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

// Cuántos elementos antes del final hay que estar para pedir más
private const val LOAD_MORE_MARGIN = 3

// Pide más al acercarse al final de la lista. itemCount hace que se vuelva a evaluar al llegar cada tanda:
// si la lista sigue corta, pide la siguiente
@Composable
fun LoadMoreWhenNearEnd(listState: LazyListState, itemCount: Int, onLoadMore: () -> Unit) {
    // Hace nearTheEnd true si el último visible está a 3 o menos del final
    val nearTheEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - LOAD_MORE_MARGIN
        }
    }
    LaunchedEffect(nearTheEnd, itemCount) {
        if (nearTheEnd)
            onLoadMore()
    }
}
