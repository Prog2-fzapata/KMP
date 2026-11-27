package ar.edu.um.prog2.fzapata.cliente.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    // El login avisa de la sesión vencida solo si no fue el usuario quien la cerró
    @Test
    fun onlyAnExpiredSessionIsMarkedAsExpired() {
        val store = FakeSessionStore("jwt")
        val session = SessionManager(store)

        session.logout()
        assertNull(store.load())
        assertNull(session.tokenFlow.value)
        assertFalse(session.expired)

        session.save("jwt")
        session.expire()
        assertTrue(session.expired)

        session.save("otro")
        assertFalse(session.expired)
    }
}
