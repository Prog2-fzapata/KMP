package ar.edu.um.prog2.fzapata.cliente.session

class FakeSessionStore(private var token: String? = null) : SessionStore {

    override fun load(): String? = token

    override fun save(token: String) {
        this.token = token
    }

    override fun clear() {
        token = null
    }
}
