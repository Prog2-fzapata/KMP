package ar.edu.um.prog2.fzapata.cliente.viewmodel.validation

import ar.edu.um.prog2.fzapata.cliente.viewmodel.RegisterForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegisterValidatorTest {

    private val validator = RegisterValidator()

    private val valid = RegisterForm(login = "juan.perez", password = "una-clave", email = "juan@example.com")

    @Test
    fun aFormWithOnlyTheRequiredFieldsHasNoErrors() {
        assertTrue(validator.validate(valid).isEmpty())
    }

    @Test
    fun requiredFieldsMustBeInsideTheContractLimits() {
        assertEquals(setOf("login"), validator.validate(valid.copy(login = "ab")).keys)
        assertEquals(setOf("login"), validator.validate(valid.copy(login = "a".repeat(51))).keys)
        assertTrue(validator.validate(valid.copy(login = "abc")).isEmpty())

        assertEquals(setOf("password"), validator.validate(valid.copy(password = "abc")).keys)
        assertEquals(setOf("password"), validator.validate(valid.copy(password = "a".repeat(101))).keys)
        assertTrue(validator.validate(valid.copy(password = "abcd")).isEmpty())

        assertEquals(setOf("email"), validator.validate(valid.copy(email = "a@b")).keys)
        assertEquals(setOf("email"), validator.validate(valid.copy(email = "sin-arroba.com")).keys)
        assertEquals(setOf("email"), validator.validate(valid.copy(email = "a".repeat(250) + "@b.com")).keys)
    }

    @Test
    fun optionalNamesMayBeEmptyButNotTooLong() {
        val tooLong = valid.copy(firstName = "a".repeat(51), lastName = "a".repeat(51))

        assertEquals(setOf("firstName", "lastName"), validator.validate(tooLong).keys)
        assertTrue(validator.validate(valid.copy(firstName = "a".repeat(50), lastName = "a".repeat(50))).isEmpty())
    }
}
