package co.solventa.movil.features.siniestros.seguimiento

/**
 * Los siniestros del prototipo (M9c y M10) para las pruebas de interfaz de la
 * consulta (HU-M2.4-01, HA-L02): Autos · Colisión devuelto, Hogar · Daño por
 * agua en evaluación y uno pagado.
 */
object DatosDeConsulta {

    const val AHORA = 1_789_569_000_000L // 2026-09-16T14:30:00Z
    private const val HORA = 3_600_000L

    val hogar = Siniestro(
        id = "8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90", radicado = "SIN-2026-01655",
        estado = EstadoDeSiniestro.EN_EVALUACION, codigoDeEstado = "in_evaluation",
        poliza = "HG-2025-880231", linea = "home", producto = null, objetoAsegurado = null,
        tipoDeIncidente = "water_damage", ocurrioEn = AHORA - 300 * HORA, radicadoEn = AHORA - 280 * HORA,
        lugar = "Carrera 15 # 93-40, apto 402",
    )

    val autos = hogar.copy(
        id = "2b0e7d1a-5c3f-4e8a-9b1d-6f2a4c8e0d13", radicado = "SIN-2026-01877",
        estado = EstadoDeSiniestro.DEVUELTO, codigoDeEstado = "returned",
        poliza = "AU-2025-118204", linea = "auto", tipoDeIncidente = "collision",
    )

    val pagado = hogar.copy(
        id = "c1d2e3f4-a5b6-4c7d-8e9f-0a1b2c3d4e5f", radicado = "SIN-2026-01702",
        estado = EstadoDeSiniestro.PAGADO, codigoDeEstado = "paid", poliza = "AU-2025-118204", linea = "auto",
        tipoDeIncidente = "theft",
    )

    val cambios = listOf(
        CambioDeEstado(EstadoDeSiniestro.RADICADO, AHORA - 280 * HORA),
        CambioDeEstado(EstadoDeSiniestro.EN_EVALUACION, AHORA - 260 * HORA),
    )

    /** El pago paramétrico del prototipo (M10b, HU-M2.4-01): AV-8421 con 215 minutos de retraso, umbral 180. */
    val parametrico = Siniestro(
        id = "6f1c2a7e-3b4d-4e5f-8a9b-0c1d2e3f4a5b", radicado = "SIN-2026-01702",
        estado = EstadoDeSiniestro.PAGADO, codigoDeEstado = "paid",
        poliza = "VJ-2026-000931", linea = "travel", producto = "Viaje", objetoAsegurado = null,
        tipoDeIncidente = "flight_delay", ocurrioEn = AHORA - 113 * HORA, radicadoEn = AHORA - 113 * HORA,
        lugar = "AV-8421",
        resolucionAutomatica = ResolucionAutomatica(
            resueltaEn = AHORA - 113 * HORA + 180_000L,
            version = "parametric-rules-v1",
            disparador = Disparador("flight_delay", "AV-8421", 180.0, 215.0, "minutes", AHORA - 113 * HORA),
        ),
    )

    fun detalleParametrico() = DetalleSiniestroUiState(
        cargando = false,
        siniestro = parametrico,
        etapas = LineaDeTiempo.armar(
            parametrico,
            listOf(
                CambioDeEstado(EstadoDeSiniestro.APROBADO, parametrico.radicadoEn),
                CambioDeEstado(EstadoDeSiniestro.PAGADO, parametrico.radicadoEn + 480_000L),
            ),
        ),
        progreso = LineaDeTiempo.progreso(parametrico),
        frescura = Frescura(false, AHORA),
    )

    fun detalle(desdeCache: Boolean, leidoEn: Long = AHORA) = DetalleSiniestroUiState(
        cargando = false,
        siniestro = hogar,
        etapas = LineaDeTiempo.armar(hogar.estado, cambios),
        progreso = LineaDeTiempo.progreso(hogar.estado),
        frescura = Frescura(desdeCache, leidoEn),
    )
}
