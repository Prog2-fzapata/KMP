package ar.edu.um.prog2.fzapata.cliente.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionManagerTest {

    @Test
    fun aSavedTokenSurvivesANewManager() {
        val store = FakeSessionStore()
        SessionManager(store).save("jwt")

        assertEquals("jwt", SessionManager(store).token())
    }

    @Test
    fun expiringClearsBothTheManagerAndTheStore() {
        val store = FakeSessionStore("jwt")
        val session = SessionManager(store)

        session.expire()

        assertNull(session.token())
        assertNull(store.load())
    }
}
