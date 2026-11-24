package ar.edu.um.prog2.fzapata.cliente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ar.edu.um.prog2.fzapata.cliente.config.AppConfig

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = App(AppConfig(BuildConfig.CATALOG_URL, BuildConfig.TURNOS_URL))

        setContent {
            app.Content()
        }
    }
}
