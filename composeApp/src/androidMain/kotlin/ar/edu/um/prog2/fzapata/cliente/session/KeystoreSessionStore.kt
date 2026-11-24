package ar.edu.um.prog2.fzapata.cliente.session

import android.content.Context // necesario para acceso al sistema, aceder al token
import android.security.keystore.KeyGenParameterSpec // receta para crear una clave
import android.security.keystore.KeyProperties // constantes para la receta
import android.util.Base64 // encoder y decoder
import java.security.KeyStore // acceso a la caja fuerte
import javax.crypto.Cipher // cifra y descifra
import javax.crypto.KeyGenerator // fabrica la clave
import javax.crypto.SecretKey // tipo de clave
import javax.crypto.spec.GCMParameterSpec // Vector de inicialización + etiqueta

// Vector (IV): número aleatorio. Para que un mismo texto no de dos veces el mismo token
//  Se guarda en los primeros 12 bytes del token para que junto a la clave se pueda descifrar

// La etiqueta es un sello calculado a partir de todo lo cifrado para recalcular y comparar
//  Va al final del token, 16 bytes. Entonces -> 47 bytes en total tiene el token 12 + 19 + 16

// Android Keystore es una caja fuerte del sistema operativo
// Guarda el JWT cifrado con AES-GCM; la clave vive ahí y nunca sale de él
class KeystoreSessionStore(context: Context) : SessionStore {

    // Abre o crea el archivo privado de preferencias, pares clave valor
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Carga el token
    override fun load(): String? {
        val stored = prefs.getString(KEY_TOKEN, null) ?: return null
        // Si la clave ya no sirve (por ejemplo, tras restaurar el teléfono) el token es irrecuperable: no hay sesión
        return runCatching { decrypt(stored) }.getOrElse {
            clear()
            null
        }
    }

    override fun save(token: String) {
        prefs.edit().putString(KEY_TOKEN, encrypt(token)).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        // El Keystore elige un vector de inicialización nuevo en cada cifrado: se guarda pegado al texto cifrado
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val data = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, data.copyOfRange(0, IV_BYTES)))
        return String(cipher.doFinal(data.copyOfRange(IV_BYTES, data.size)), Charsets.UTF_8)
    }

    // Busca la clave en el keystore, si no existe, la crea. Para cifrar y descifrar.
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        // Se genera con los parámetros de abajo
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "session"
        private const val KEY_TOKEN = "token"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "session-key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BITS = 256
        private const val TAG_BITS = 128
        private const val IV_BYTES = 12
    }
}
