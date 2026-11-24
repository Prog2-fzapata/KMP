package ar.edu.um.prog2.fzapata.cliente.network.model

import kotlinx.serialization.Serializable

// Cuerpo application/problem+json de los servicios; los campos que no usamos se ignoran
@Serializable
data class ProblemDetail(
    val code: String? = null,
    val fieldErrors: List<FieldError> = emptyList()
)

@Serializable
data class FieldError(
    val field: String,
    val message: String,
    val objectName: String? = null
)
