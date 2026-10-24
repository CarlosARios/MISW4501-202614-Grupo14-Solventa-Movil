package co.solventa.movil.features.acceso.registro

import co.solventa.movil.core.api.originacion.ErrorDeRegistro
import co.solventa.movil.core.api.originacion.OriginacionApi
import co.solventa.movil.core.api.originacion.RespuestaDeRegistro
import co.solventa.movil.core.api.originacion.SolicitudDeRegistro
import co.solventa.movil.core.errores.Correlacion
import co.solventa.movil.core.imagen.PreparadorDeCaptura
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El registro contra `POST /v1/registrations` del API Gateway (HU-M1.1-01).
 *
 * Traduce entre lo que la pantalla muestra y lo que el contrato recibe: el
 * documento sin puntos, el nombre completo en un solo campo y la fecha en
 * `yyyy-MM-dd`. Al revés traduce los códigos de estado a los tres caminos
 * que tiene el flujo.
 *
 * **Un cuerpo que no se puede leer se trata como falla del servicio.** Un
 * `201` sin cuerpo no permite afirmar que la cuenta quedó creada, y decirle
 * al usuario que entre con credenciales que quizá no existen es peor que
 * pedirle que reintente.
 */
@Singleton
class RegistroRepositorioHttp @Inject constructor(
    private val api: OriginacionApi,
    private val preparador: PreparadorDeCaptura,
) : RegistroRepositorio {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun registrar(
        datos: DatosDocumento,
        contrasena: String,
        frente: ImagenDocumento,
        reverso: ImagenDocumento,
    ): ResultadoRegistro {
        val correlacion = Correlacion.nuevo()
        // Una captura que no se puede leer no llega a salir: sin el documento
        // no hay nada que verificar, y el usuario tiene que volver a tomarla.
        val frenteEnBase64 = preparador.aBase64(frente.rutaArchivo)
            ?: return ResultadoRegistro.DocumentoNoLegible(correlacion)
        val reversoEnBase64 = preparador.aBase64(reverso.rutaArchivo)
            ?: return ResultadoRegistro.DocumentoNoLegible(correlacion)

        // Quién borra las capturas del teléfono es el ViewModel, que es el
        // que sabe cuándo una imagen dejó de hacer falta: aquí solo se envían.
        return try {
            interpretar(
                api.registrar(correlacion, solicitud(datos, contrasena, frenteEnBase64, reversoEnBase64)),
                correlacion,
            )
        } catch (sinRed: IOException) {
            ResultadoRegistro.NoDisponible(correlacion)
        } catch (ilegible: SerializationException) {
            ResultadoRegistro.NoDisponible(correlacion)
        }
    }

    private fun interpretar(
        respuesta: Response<RespuestaDeRegistro>,
        correlacion: String,
    ): ResultadoRegistro {
        val cuerpo = respuesta.body()
        return when {
            respuesta.isSuccessful && cuerpo != null -> ResultadoRegistro.Exitoso(cuerpo.cuenta)
            respuesta.code() == CONFLICTO -> ResultadoRegistro.YaRegistrado
            respuesta.code() == DATO_INVALIDO -> rechazo(respuesta, correlacion)
            else -> ResultadoRegistro.NoDisponible(correlacion)
        }
    }

    /**
     * Reparte el `422` entre los dos caminos que tiene: una cara rechazada no
     * se arregla corrigiendo el formulario, así que manda sobre los demás
     * campos.
     *
     * Un `422` que no trae campos reconocibles deja marcado el formulario
     * completo: algo hay que corregir y no se sabe qué.
     */
    private fun rechazo(respuesta: Response<*>, correlacion: String): ResultadoRegistro {
        val nombres = runCatching {
            json.decodeFromString<ErrorDeRegistro>(respuesta.errorBody()?.string().orEmpty())
        }.getOrNull()?.campos?.map { it.campo }?.toSet().orEmpty()

        if (nombres.any { it in CARAS_DEL_DOCUMENTO }) {
            return ResultadoRegistro.DocumentoNoLegible(correlacion)
        }
        val campos = nombres.mapNotNull { CAMPOS[it] }.toSet()
        return ResultadoRegistro.DatosInvalidos(campos.ifEmpty { CampoDeRegistro.entries.toSet() })
    }

    private fun solicitud(
        datos: DatosDocumento,
        contrasena: String,
        frenteEnBase64: String,
        reversoEnBase64: String,
    ) = SolicitudDeRegistro(
        documento = datos.numeroDocumento.filter(Char::isLetterOrDigit),
        nombreCompleto = "${datos.nombres.trim()} ${datos.apellidos.trim()}".trim(),
        correo = datos.correo.trim(),
        fechaNacimiento = enFormatoDelContrato(datos.fechaNacimiento),
        contrasena = contrasena,
        frenteDelDocumento = frenteEnBase64,
        reversoDelDocumento = reversoEnBase64,
    )

    /**
     * `dd/MM/yyyy` en la pantalla, `yyyy-MM-dd` en el contrato. Lo que no
     * tenga esa forma se envía tal cual y lo rechaza el servicio, que es
     * quien valida.
     */
    private fun enFormatoDelContrato(fecha: String): String {
        val partes = fecha.trim().split("/")
        if (partes.size != 3) return fecha.trim()
        val (dia, mes, anio) = partes
        return "$anio-${mes.padStart(2, '0')}-${dia.padStart(2, '0')}"
    }

    private companion object {
        const val CONFLICTO = 409
        const val DATO_INVALIDO = 422

        /**
         * Las dos caras del documento, como las nombra el contrato. No son
         * campos de M1c: se corrigen volviendo a capturar.
         */
        val CARAS_DEL_DOCUMENTO = setOf("document_front", "document_back")

        /** Los nombres del contrato, en el orden en que aparecen en M1c. */
        val CAMPOS = mapOf(
            "full_name" to CampoDeRegistro.NOMBRE,
            "document_id" to CampoDeRegistro.DOCUMENTO,
            "birth_date" to CampoDeRegistro.FECHA_NACIMIENTO,
            "email" to CampoDeRegistro.CORREO,
            "password" to CampoDeRegistro.CONTRASENA,
        )
    }
}
