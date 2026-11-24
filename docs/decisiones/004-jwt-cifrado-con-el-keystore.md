# 004. Guardar el JWT cifrado con el Android Keystore

**Estado:** aceptada

## Contexto

El JWT da acceso a las reservas del usuario: no puede quedar en texto plano en el almacenamiento de la app ni aparecer en un log. La sesión tiene que sobrevivir a cerrar la app, porque el token dura 24 horas (30 días con `rememberMe`).

## Decisión

`KeystoreSessionStore` cifra el token con AES-GCM de 256 bits. La clave se genera dentro del Android Keystore y no sale de él; en las preferencias privadas de la app solo queda el texto cifrado junto con su vector de inicialización, en Base64. `SessionStore` es una interfaz común, así que las pruebas usan una versión en memoria. Si el token guardado no se puede descifrar, se descarta y no hay sesión. `allowBackup` está en `false` para que ese archivo no se restaure en otro teléfono, donde no existiría la clave.

## Alternativas descartadas

- `EncryptedSharedPreferences` (`security-crypto` 1.1.0): las notas de la versión marcan todas sus APIs como deprecadas a favor del uso directo del Keystore.
- Tink (1.24.0): es más completa, pero agrega una dependencia y conceptos que no hacen falta para guardar una cadena.
- Preferencias en texto plano: cualquiera con acceso al archivo vería el token.

## Consecuencias

El cifrado son unas 60 líneas propias y solo funciona en Android. No se prueba con tests unitarios porque necesita un dispositivo; se verificó en un emulador: el archivo guardado no contiene el token en claro y la sesión sobrevive a cerrar la app.
