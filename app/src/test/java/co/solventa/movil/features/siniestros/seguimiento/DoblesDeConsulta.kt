package co.solventa.movil.features.siniestros.seguimiento

/** El siniestro M10 del prototipo: Hogar · Daño por agua, SIN-2026-01655. */
internal const val AHORA = 1_789_569_000_000L // 2026-09-16T14:30:00Z
internal const val HORA = 3_600_000L

internal fun siniestro(
    id: String = "8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90",
    estado: EstadoDeSiniestro = EstadoDeSiniestro.EN_EVALUACION,
) = Siniestro(
    id = id,
    radicado = "SIN-2026-01655",
    estado = estado,
    codigoDeEstado = estado.codigo,
    poliza = "HG-2025-880231",
    linea = "home",
    producto = null,
    objetoAsegurado = null,
    tipoDeIncidente = "water_damage",
    ocurrioEn = AHORA - 300 * HORA,
    radicadoEn = AHORA - 280 * HORA,
)

/** La consulta de las pruebas: devuelve lo que se le dice y cuenta las llamadas. */
internal class ConsultaFalsa : ConsultaDeSiniestrosRepositorio {
    var lista: ResultadoDeConsulta<List<Siniestro>> = ResultadoDeConsulta.Exito(emptyList(), Frescura(false, AHORA))
    var detalle: ResultadoDeConsulta<DetalleDeSiniestro> = ResultadoDeConsulta.Error
    var consultas = 0
    var pedido: String? = null

    override suspend fun listar(): ResultadoDeConsulta<List<Siniestro>> {
        consultas++
        return lista
    }

    override suspend fun detalle(id: String): ResultadoDeConsulta<DetalleDeSiniestro> {
        consultas++
        pedido = id
        return detalle
    }
}
