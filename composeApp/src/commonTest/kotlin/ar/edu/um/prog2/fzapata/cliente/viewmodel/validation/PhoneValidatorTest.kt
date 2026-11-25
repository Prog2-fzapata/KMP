package ar.edu.um.prog2.fzapata.cliente.viewmodel.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhoneValidatorTest {

    private val validator = PhoneValidator()

    @Test
    fun theUsualWaysOfTypingAPhoneAreAcceptedAndSentClean() {
        val typed = mapOf(
            "+54 9 261 312 4578" to "+5492613124578",
            "2613124578" to "2613124578",
            "5492613124578" to "5492613124578",
            "261 312-4578" to "2613124578",
            "(0261) 312.4578" to "02613124578"
        )

        typed.forEach { (input, sent) ->
            assertTrue(validator.validate(input).isEmpty(), input)
            assertEquals(sent, validator.normalize(input), input)
        }
    }

    @Test
    fun thePhoneNeedsBetweenSevenAndFifteenDigitsAndThePlusOnlyAtTheStart() {
        assertTrue(validator.validate("1".repeat(7)).isEmpty())
        assertTrue(validator.validate("+" + "1".repeat(15)).isEmpty())

        listOf("", "123456", "1".repeat(16), "261+3124578", "+", "261-312-45ab").forEach {
            assertEquals(setOf("phoneNumber"), validator.validate(it).keys, it)
        }
    }
}
