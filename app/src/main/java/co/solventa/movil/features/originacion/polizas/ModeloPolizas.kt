package co.solventa.movil.features.originacion.polizas

/**
 * Una póliza de la billetera (HU-M1.2-01), con todo lo que el criterio 2 pide
 * ver sin conexión: número, tipo de seguro, vigencia, coberturas con sus
 * montos asegurados y el asegurado principal. Las fechas van en milisegundos
 * desde época y los montos en pesos.
 */
data class Poliza(
    val id: String,
    val origen: String,
    /** Uno de [LineasDePoliza]; la pantalla lo nombra en el idioma elegido. */
    val linea: String,
    /** El nombre del producto que da el núcleo («Vida hipotecario»), si lo da. */
    val producto: String?,
    val objetoAsegurado: String?,
    /** Uno de [EstadosDePoliza]. */
    val estado: String,
    val asegurado: Asegurado,
    /** Prima mensual; `null` si la fuente no la guarda. */
    val prima: Double?,
    val moneda: String,
    val valorAsegurado: Double,
    val coberturas: List<Cobertura>,
    val inicio: Long,
    val fin: Long,
    val renovacionDe: String?,
)

data class Asegurado(
    val nombre: String,
    val tipoDeDocumento: String?,
    val documento: String?,
)

data class Cobertura(
    /** El código del contrato: `collision`, `death`, `water_damage`… */
    val codigo: String,
    val monto: Double,
)

/** Los tipos de seguro que nombra el contrato. */
object LineasDePoliza {
    const val VIDA = "life"
    const val AUTOS = "auto"
    const val HOGAR = "home"
    const val VIAJE = "travel"
}

/** Los estados que nombra el contrato. */
object EstadosDePoliza {
    const val ACTIVA = "active"
    const val VENCIDA = "expired"
    const val CANCELADA = "cancelled"
}

/** Qué le pasó a una póliza entre dos sincronizaciones (criterio 4). */
enum class TipoDeCambio { RENOVADA, CANCELADA, MODIFICADA }

/**
 * Un cambio detectado al sincronizar: la póliza tal como quedó y qué le pasó.
 * Una renovación con número nuevo se reporta sobre la póliza nueva.
 */
data class CambioDePoliza(val poliza: Poliza, val tipo: TipoDeCambio)
