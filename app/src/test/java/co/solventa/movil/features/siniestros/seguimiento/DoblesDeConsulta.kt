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

/** El pago paramétrico del prototipo (HU-M2.4-01, criterio 3): SIN-2026-01702, AV-8421 con 215 minutos de retraso. */
internal val RESOLUCION = ResolucionAutomatica(
    resueltaEn = AHORA - 2 * HORA,
    version = "parametric-rules-v1",
    disparador = Disparador("flight_delay", "AV-8421", 180.0, 215.0, "minutes", AHORA - 3 * HORA),
)

internal fun parametrico(estado: EstadoDeSiniestro = EstadoDeSiniestro.PAGADO) = siniestro(
    id = "6f1c2a7e-3b4d-4e5f-8a9b-0c1d2e3f4a5b",
    estado = estado,
).copy(
    radicado = "SIN-2026-01702",
    poliza = "VJ-2026-000931",
    linea = "travel",
    tipoDeIncidente = "flight_delay",
    resolucionAutomatica = RESOLUCION,
)

/** La copia local de las pruebas: guarda en memoria lo que se le da. */
internal class CopiaFalsa : CopiaLocalDeSiniestros {
    var lista: CopiaGuardada<List<Siniestro>>? = null
    val detalles = mutableMapOf<String, CopiaGuardada<DetalleDeSiniestro>>()

    override suspend fun guardarLista(lista: List<Siniestro>, guardadoEn: Long) {
        this.lista = CopiaGuardada(lista, guardadoEn)
    }

    override suspend fun lista(): CopiaGuardada<List<Siniestro>>? = lista

    override suspend fun guardarDetalle(detalle: DetalleDeSiniestro, guardadoEn: Long) {
        detalles[detalle.siniestro.id] = CopiaGuardada(detalle, guardadoEn)
    }

    override suspend fun detalle(id: String): CopiaGuardada<DetalleDeSiniestro>? = detalles[id]
}
