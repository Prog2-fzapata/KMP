package ar.edu.um.prog2.fzapata.cliente.network

import kotlinx.serialization.json.Json

// El mismo lector de JSON, lee las respuestas de los pedidos y los avisos del stream: los servicios pueden sumar campos sin romper la app
internal val appJson = Json { ignoreUnknownKeys = true }
