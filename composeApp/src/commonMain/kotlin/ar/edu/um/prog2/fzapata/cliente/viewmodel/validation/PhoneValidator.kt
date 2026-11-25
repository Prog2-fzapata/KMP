package ar.edu.um.prog2.fzapata.cliente.viewmodel.validation

// Regla de la cátedra: sin espacios, guiones, paréntesis ni puntos, el teléfono tiene de 7 a 15 dígitos y un + opcional al principio.
// No se completa el código de país: +54 y 549 son números distintos y adivinar uno sería inventar el dato.
class PhoneValidator {

    // Devuelve el mensaje del campo con error
    fun validate(input: String): Map<String, String> = buildMap {
        if (!PHONE.matches(normalize(input))) put("phoneNumber", "El teléfono debe tener entre 7 y 15 dígitos, con un + opcional al principio")
    }

    // El número como se envía: sin los separadores que la gente escribe
    fun normalize(input: String): String = input.filterNot { it in SEPARATORS }

    private companion object {
        const val SEPARATORS = " -()."
        val PHONE = Regex("\\+?[0-9]{7,15}")
    }
}
