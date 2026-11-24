package ar.edu.um.prog2.fzapata.cliente.config

import kotlin.test.Test
import kotlin.test.assertEquals

class AppConfigTest {

    @Test
    fun trailingSlashesAreRemovedFromBothUrls() {
        val config = AppConfig("http://10.0.2.2:8081/", "http://10.0.2.2:8082//")

        assertEquals("http://10.0.2.2:8081", config.catalogUrl)
        assertEquals("http://10.0.2.2:8082", config.turnosUrl)
    }

    @Test
    fun urlsWithoutTrailingSlashAreKept() {
        val config = AppConfig("http://10.0.2.2:8081", "http://10.0.2.2:8082")

        assertEquals("http://10.0.2.2:8081", config.catalogUrl)
        assertEquals("http://10.0.2.2:8082", config.turnosUrl)
    }
}
