package ar.edu.um.prog2.fzapata.cliente.viewmodel.validation

import ar.edu.um.prog2.fzapata.cliente.viewmodel.RegisterForm

// Los límites de POST /api/register (docs/contrato.md del catálogo). El patrón del login lo valida el servidor.
// Devuelve el mensaje de cada campo con error, con el mismo nombre de campo que usa el servicio.
class RegisterValidator {

    fun validate(form: RegisterForm): Map<String, String> = buildMap {
        val login = form.login.trim()
        val email = form.email.trim()

        if (login.length !in 3..50) put("login", "El login debe tener entre 3 y 50 caracteres")
        if (form.password.length !in 4..100) put("password", "La contraseña debe tener entre 4 y 100 caracteres")
        if (email.length !in 5..254 || !EMAIL.matches(email)) put("email", "El email no es válido")
        if (form.firstName.trim().length > 50) put("firstName", "El nombre admite hasta 50 caracteres")
        if (form.lastName.trim().length > 50) put("lastName", "El apellido admite hasta 50 caracteres")
    }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+$")
    }
}
