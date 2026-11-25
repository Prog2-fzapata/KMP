package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.model.Page
import ar.edu.um.prog2.fzapata.cliente.network.model.Professional
import ar.edu.um.prog2.fzapata.cliente.network.model.ProfessionalCategory
import ar.edu.um.prog2.fzapata.cliente.network.model.ProfessionalFilters
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

// Un método por endpoint del catálogo; los errores salen como ApiException
class CatalogApi(private val client: HttpClient, private val config: AppConfig) {

    // Solo manda los filtros que están activos: lo que no se manda no filtra
    suspend fun professionals(filters: ProfessionalFilters, page: Int): Page<Professional> {
        val response = client.get("${config.catalogUrl}/api/professionals") {
            parameter("page", page)
            parameter("size", PROFESSIONAL_PAGE_SIZE)
            filters.categoryId?.let { parameter("categoryId", it) }
            filters.name.trim().ifEmpty { null }?.let { parameter("name", it) }
            if (filters.onlyEnabled) parameter("enabled", true)
            if (filters.onlyAvailable) parameter("available", true)
        }
        return response.toPage(page, PROFESSIONAL_PAGE_SIZE)
    }

    // Las habilitadas, en una sola página: se usan todas juntas, como chips
    suspend fun categories(): List<ProfessionalCategory> =
        client.get("${config.catalogUrl}/api/professional-categories") {
            parameter("enabled", true)
            parameter("size", CATEGORY_PAGE_SIZE)
        }.body()

    companion object {
        const val PROFESSIONAL_PAGE_SIZE = 20
        // Las categorías se piden de una vez: 100 es lo máximo que admite el servicio por página
        private const val CATEGORY_PAGE_SIZE = 100
    }
}
