package co.solventa.movil.core.auth.cognito

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * Lee las afirmaciones del *id token* sin comprobar su firma.
 *
 * **No comprobar la firma aquí no es un descuido.** Quien decide si el token
 * vale es el backend, que lo verifica contra el JWKS del *pool* en cada
 * petición. La aplicación solo lo abre para mostrar el nombre y el correo en
 * pantalla y para saber cuándo vence. Si alguien alterara el token en su
 * propio dispositivo, lo único que conseguiría es ver un nombre equivocado en
 * su teléfono y que el backend le rechace la petición.
 *
 * Usa `java.util.Base64` y no el de Android a propósito: así esta lógica se
 * prueba en la JVM, sin emulador.
 */
object TokenDeIdentidad {

    private val json = Json { ignoreUnknownKeys = true }

    /** Afirmaciones del token, o `null` si no tiene la forma de un JWT. */
    fun afirmaciones(token: String): Afirmaciones? = runCatching {
        val carga = token.split('.').getOrNull(1) ?: return null
        val objeto = json.parseToJsonElement(
            String(Base64.getUrlDecoder().decode(sinRelleno(carga)), Charsets.UTF_8),
        ).jsonObject

        fun texto(clave: String): String? = objeto[clave]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

        val correo = texto("email")
        Afirmaciones(
            correo = correo.orEmpty(),
            // El alta administrativa no siempre fija `name`; en ese caso se
            // muestra la parte local del correo, que es mejor que un vacío.
            nombre = texto("name") ?: correo?.substringBefore('@').orEmpty(),
            // `exp` viene en segundos desde época.
            venceEn = (texto("exp")?.toLongOrNull() ?: 0L) * 1_000L,
            // El identificador estable de la cuenta (HU-M1.3-01): es la
            // llave del propietario de la copia local de los consentimientos.
            sub = texto("sub").orEmpty(),
        )
    }.getOrNull()

    /**
     * Un JWT viaja en base64url sin relleno; `java.util.Base64` lo exige,
     * así que se completa antes de decodificar.
     */
    private fun sinRelleno(carga: String): String = when (carga.length % 4) {
        2 -> "$carga=="
        3 -> "$carga="
        else -> carga
    }

    data class Afirmaciones(val correo: String, val nombre: String, val venceEn: Long, val sub: String = "")
}
