package co.solventa.movil.features.originacion.polizas

/**
 * Compara la copia que había en el teléfono con la lista que acaba de llegar
 * y dice qué pólizas cambiaron (HU-M1.2-01, criterio 4). El servicio no guarda
 * versiones por dispositivo, así que la comparación vive aquí:
 *
 * - **Renovada:** una póliza nueva cuyo `renovacionDe` es una de la copia, o
 *   la misma póliza con un fin de vigencia posterior.
 * - **Cancelada:** la misma póliza, que pasó a `cancelled`.
 * - **Modificada:** la misma póliza con cualquier otro dato distinto.
 *
 * Sin copia anterior (la primera sincronización de la cuenta) no hay cambios
 * que avisar: todo sería «nuevo».
 */
object DetectorDeCambios {

    fun comparar(anteriores: List<Poliza>, nuevas: List<Poliza>): List<CambioDePoliza> {
        if (anteriores.isEmpty()) return emptyList()
        val porId = anteriores.associateBy { it.id }
        return nuevas.mapNotNull { nueva ->
            val anterior = porId[nueva.id]
            when {
                anterior == null ->
                    if (nueva.renovacionDe != null && nueva.renovacionDe in porId) {
                        CambioDePoliza(nueva, TipoDeCambio.RENOVADA)
                    } else {
                        null
                    }
                anterior == nueva -> null
                nueva.estado == EstadosDePoliza.CANCELADA && anterior.estado != EstadosDePoliza.CANCELADA ->
                    CambioDePoliza(nueva, TipoDeCambio.CANCELADA)
                nueva.fin > anterior.fin -> CambioDePoliza(nueva, TipoDeCambio.RENOVADA)
                else -> CambioDePoliza(nueva, TipoDeCambio.MODIFICADA)
            }
        }
    }
}
