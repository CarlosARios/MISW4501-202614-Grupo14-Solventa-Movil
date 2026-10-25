package co.solventa.movil.features.auditoria.consentimientos

/** Datos de prueba de las pantallas de consentimientos (HU-M1.3-01 y HU-M1.3-02). */
object DatosDePantalla {
    const val OTORGADO = 1_789_573_920_000L // 2026-09-16T15:52:00Z
    const val VENCE = 1_821_109_920_000L // 2027-09-16T15:52:00Z
    const val REVOCADO_EN = 1_790_848_800_000L // 2026-10-01T10:00:00Z
    const val SINCRONIZADO = 1_792_800_000_000L

    val activo = Consentimiento(
        id = "cns-activo",
        estado = EstadosDeConsentimiento.ACTIVO,
        vigente = true,
        entidades = listOf(Entidad("aliado-banco-1", "Banco Aliado")),
        finalidad = "risk_profiling_mortgage_life_offer",
        canal = "embedded",
        versionDeTerminos = "v1",
        alcance = listOf(Fuente("open_finance", listOf("credit_products", "balances"), "12_months")),
        otorgadoEn = OTORGADO,
        venceEn = VENCE,
        finalizadoEn = null,
        comprobante = null,
        historial = listOf(CambioDeEstado(CambiosDeEstado.OTORGADO, OTORGADO, null, "embedded", null)),
    )

    val revocado = activo.copy(
        id = "cns-revocado",
        estado = EstadosDeConsentimiento.REVOCADO,
        vigente = false,
        entidades = listOf(Entidad("cooperativa", "Cooperativa Andina")),
        finalizadoEn = REVOCADO_EN,
        comprobante = "REV-2026-0012",
        historial = listOf(
            CambioDeEstado(CambiosDeEstado.OTORGADO, OTORGADO, null, "embedded", null),
            CambioDeEstado(CambiosDeEstado.REVOCADO, REVOCADO_EN, "REV-2026-0012", "mobile", null),
        ),
    )

    val vencido = activo.copy(
        id = "cns-vencido",
        estado = EstadosDeConsentimiento.VENCIDO,
        vigente = false,
        entidades = listOf(Entidad("banco-central", "Banco Central")),
        finalizadoEn = VENCE,
        historial = listOf(
            CambioDeEstado(CambiosDeEstado.OTORGADO, OTORGADO, null, "embedded", null),
            CambioDeEstado(CambiosDeEstado.VENCIDO, VENCE, null, null, null),
        ),
    )

    // --- HU-M1.3-02 ---

    const val SOLICITADA = 1_789_593_004_000L // 2026-09-16T21:10:04Z
    const val EFECTIVA = SOLICITADA + 2_100L

    fun revocacion(estadoDelAviso: String = EstadosDelAviso.COMPLETADO, entidad: String = EstadosDeEntidad.CONFIRMADA) = Revocacion(
        comprobante = "REV-2026-0088",
        estado = if (estadoDelAviso == EstadosDelAviso.PARCIAL) EstadosDeConsentimiento.REVOCADO_PARCIAL else EstadosDeConsentimiento.REVOCADO,
        solicitadaEn = SOLICITADA,
        efectivaEn = EFECTIVA,
        canal = "mobile",
        dispositivo = "Pixel 8",
        estadoDelAviso = estadoDelAviso,
        entidades = listOf(EntidadDeRevocacion("aliado-banco-1", "Banco Aliado", entidad, null, EFECTIVA + 300_000L)),
    )

    /** El activo, revocado en linea desde el telefono. */
    val revocadoEnLinea = activo.copy(
        estado = EstadosDeConsentimiento.REVOCADO,
        vigente = false,
        revocable = false,
        finalizadoEn = EFECTIVA,
        comprobante = "REV-2026-0088",
        revocacion = revocacion(),
        historial = activo.historial + CambioDeEstado(CambiosDeEstado.REVOCADO, EFECTIVA, "REV-2026-0088", "mobile", null),
    )

    /** Revocado, pero Banco Aliado no confirmo en el plazo. */
    val parcial = revocadoEnLinea.copy(
        estado = EstadosDeConsentimiento.REVOCADO_PARCIAL,
        revocacion = revocacion(EstadosDelAviso.PARCIAL, EstadosDeEntidad.NO_CONFIRMADA),
    )

    /** La revocacion del activo, confirmada sin señal y en la cola (38:1503 y el detalle). */
    val enCola = RevocacionPendiente("cns-activo", "Banco Aliado", SOLICITADA, 0)
}
