package ar.edu.um.prog2.fzapata.cliente.config

class AppConfig(catalogUrl: String, turnosUrl: String) {

    // Sin barra final, para poder escribir "$catalogUrl/api/..." sin duplicarla
    val catalogUrl: String = catalogUrl.trimEnd('/')
    val turnosUrl: String = turnosUrl.trimEnd('/')
}
