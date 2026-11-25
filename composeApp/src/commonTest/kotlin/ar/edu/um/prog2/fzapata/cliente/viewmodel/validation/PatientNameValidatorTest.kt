package ar.edu.um.prog2.fzapata.cliente.viewmodel.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PatientNameValidatorTest {

    private val validator = PatientNameValidator()

    @Test
    fun theNameAndTheLastNameNeedAtLeastTwoCharactersWithoutCountingSpaces() {
        assertTrue(validator.validate("Ana", "Gómez").isEmpty())
        assertTrue(validator.validate("Jo", "Li").isEmpty())

        // Un solo carácter lo acepta el catálogo, pero turnos lo rechaza
        assertEquals(setOf("firstName", "lastName"), validator.validate("A", "B").keys)
        assertEquals(setOf("firstName"), validator.validate("  A  ", "Gómez").keys)
        assertEquals(setOf("lastName"), validator.validate("Ana", "").keys)
    }

    @Test
    fun theNameAndTheLastNameAdmitAtMostFiftyCharacters() {
        assertTrue(validator.validate("A".repeat(50), "B".repeat(50)).isEmpty())
        assertEquals(setOf("firstName", "lastName"), validator.validate("A".repeat(51), "B".repeat(51)).keys)
    }
}
