package ar.edu.um.prog2.fzapata.cliente.viewmodel.validation

// Turnos pide al menos 2 caracteres de nombre y apellido para anotar el turno (PATIENT_NAME_REQUIRED), aunque el catálogo
// acepte uno solo; el máximo es el de la cuenta. Devuelve el mensaje de cada campo con error.
class PatientNameValidator {

    fun validate(firstName: String, lastName: String): Map<String, String> = buildMap {
        if (firstName.trim().length !in 2..50) put("firstName", "El nombre debe tener entre 2 y 50 caracteres")
        if (lastName.trim().length !in 2..50) put("lastName", "El apellido debe tener entre 2 y 50 caracteres")
    }
}
