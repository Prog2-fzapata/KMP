package ar.edu.um.prog2.fzapata.cliente.screens.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Centra su contenido en el ancho de la lista: el círculo de carga, el aviso de lista vacía
@Composable
fun Centered(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { content() }
}
