package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.features.originacion.polizas.CopiaDePolizas
import co.solventa.movil.features.originacion.polizas.EstadosDePoliza
import co.solventa.movil.features.originacion.polizas.Poliza
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import co.solventa.movil.features.originacion.polizas.PolizasRepositorio as Billetera

/** Una póliza que el asegurado puede elegir en el paso 1. */
data class PolizaElegible(
    val id: String,
    /** Lo que se lee en el selector: «Autos · AU-2025-118204 · KJT-482». */
    val etiqueta: String,
)

interface PolizasRepositorio {
    /** Solo las vigentes: sobre las demás no se radica. */
    suspend fun activas(): List<PolizaElegible>
}

/**
 * Las pólizas de la billetera (HU-M1.2-01) sobre las que se puede radicar.
 *
 * Solo las activas del núcleo de pólizas: claims lee la cobertura del
 * siniestro en el núcleo (HU-W2.2-03), así que un siniestro sobre una póliza
 * que el núcleo no conoce llega al analista sin cobertura y no se puede
 * aprobar. Las de vida que emite originación no cubren ningún tipo de
 * incidente que se radique aquí.
 *
 * Lee la copia local, que sirve sin conexión; si todavía está vacía (la
 * billetera nunca se abrió en esta sesión) la sincroniza una vez.
 */
@Singleton
class PolizasDeLaBilletera @Inject constructor(
    private val billetera: Billetera,
) : PolizasRepositorio {

    override suspend fun activas(): List<PolizaElegible> {
        var copia: CopiaDePolizas = billetera.observar().first()
        if (copia.polizas.isEmpty()) {
            billetera.sincronizar()
            copia = billetera.observar().first()
        }
        return copia.polizas
            .filter { it.origen == ORIGEN_NUCLEO && it.estado == EstadosDePoliza.ACTIVA }
            .map { PolizaElegible(id = it.id, etiqueta = it.etiqueta()) }
    }

    private fun Poliza.etiqueta(): String =
        listOfNotNull(producto ?: linea, id, objetoAsegurado).joinToString(" · ")

    private companion object {
        /** El `origin` del contrato para las pólizas del núcleo. */
        const val ORIGEN_NUCLEO = "policy_core"
    }
}

/**
 * Las pólizas de la demostración: el dato del frame M11a, que coincide con el
 * del portal. Sirve a las vistas previas y a las pruebas que no levantan la
 * billetera; la aplicación usa [PolizasDeLaBilletera].
 */
@Singleton
class PolizasDeDemostracion @Inject constructor() : PolizasRepositorio {

    override suspend fun activas(): List<PolizaElegible> = listOf(
        PolizaElegible(id = "AU-2025-118204", etiqueta = "Autos · AU-2025-118204 · KJT-482"),
    )
}
