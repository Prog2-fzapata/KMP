package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.network.model.Page
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

internal const val TOTAL_COUNT = "X-Total-Count"

// El cuerpo de una respuesta paginada con el total de X-Total-Count.
// Sin el total no se sabe si hay más páginas: se toma lo recibido como todo lo que hay.
internal suspend inline fun <reified T> HttpResponse.toPage(page: Int, size: Int): Page<T> {
    val items = body<List<T>>()
    val total = headers[TOTAL_COUNT]?.toIntOrNull() ?: (page * size + items.size)
    return Page(items, total)
}
