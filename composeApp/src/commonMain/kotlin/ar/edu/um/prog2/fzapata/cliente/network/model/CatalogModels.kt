package ar.edu.um.prog2.fzapata.cliente.network.model

import kotlinx.serialization.Serializable

// Solo los campos que la app usa; el resto de la respuesta se ignora
@Serializable
data class Professional(
    val id: Long,
    val categoryName: String,
    val firstName: String,
    val lastName: String,
    val enabled: Boolean,
    val weeklySchedules: List<WeeklySchedule> = emptyList()
)

// dayOfWeek llega como texto (MONDAY...): el contrato puede sumar valores
@Serializable
data class WeeklySchedule(
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String
)

@Serializable
data class ProfessionalCategory(
    val id: Long,
    val name: String
)

// Una página de resultados y el total de elementos que hay en el servicio (X-Total-Count)
data class Page<T>(
    val items: List<T>,
    val total: Int
)

// Los filtros de GET /api/professionals que la app ofrece
data class ProfessionalFilters(
    val categoryId: Long? = null,
    val name: String = "",
    val onlyEnabled: Boolean = true,
    val onlyAvailable: Boolean = false
)
