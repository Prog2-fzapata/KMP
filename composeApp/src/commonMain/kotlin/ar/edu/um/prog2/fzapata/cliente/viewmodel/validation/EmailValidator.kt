package ar.edu.um.prog2.fzapata.cliente.viewmodel.validation

// El límite del email del servicio (5 a 254) y un formato mínimo: el servidor decide si de verdad es válido.
// Devuelve el mensaje con el mismo nombre de campo que usa el servicio.
class EmailValidator {

    fun validate(email: String): Map<String, String> = buildMap {
        val trimmed = email.trim()
        if (trimmed.length !in 5..254 || !EMAIL.matches(trimmed)) put("email", "El email no es válido")
    }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+$")
    }
}
