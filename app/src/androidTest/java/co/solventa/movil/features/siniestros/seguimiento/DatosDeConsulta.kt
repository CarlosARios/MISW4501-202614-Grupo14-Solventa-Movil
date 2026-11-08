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

    fun detalle(desdeCache: Boolean, leidoEn: Long = AHORA) = DetalleSiniestroUiState(
        cargando = false,
        siniestro = hogar,
        etapas = LineaDeTiempo.armar(hogar.estado, cambios),
        progreso = LineaDeTiempo.progreso(hogar.estado),
        frescura = Frescura(desdeCache, leidoEn),
    )
}
