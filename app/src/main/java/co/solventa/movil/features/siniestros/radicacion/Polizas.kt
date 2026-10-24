package co.solventa.movil.features.siniestros.radicacion

import javax.inject.Inject
import javax.inject.Singleton

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
 * Las pólizas de la demostración.
 *
 * **Qué la reemplaza:** el cliente de `GET /v1/policies` cuando la billetera
 * de pólizas (HU-M1.2-01) esté en `develop`. El resto del flujo no cambia:
 * la interfaz es la misma y el asistente solo guarda el identificador.
 *
 * El dato es el del frame M11a y coincide con el del portal, que es la regla
 * de `docs/pantallas.md`: no se inventan datos nuevos para las pruebas.
 */
@Singleton
class PolizasDeDemostracion @Inject constructor() : PolizasRepositorio {

    override suspend fun activas(): List<PolizaElegible> = listOf(
        PolizaElegible(id = "AU-2025-118204", etiqueta = "Autos · AU-2025-118204 · KJT-482"),
    )
}
