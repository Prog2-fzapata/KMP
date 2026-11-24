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

    // Es lo que hace volver al login: quien observa el token se entera de que se vació
    @Test
    fun observersAreToldWhenTheSessionIsSavedAndExpired() {
        val session = SessionManager(FakeSessionStore())
        assertNull(session.tokenFlow.value)

        session.save("jwt")
        assertEquals("jwt", session.tokenFlow.value)

        session.expire()
        assertNull(session.tokenFlow.value)
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
