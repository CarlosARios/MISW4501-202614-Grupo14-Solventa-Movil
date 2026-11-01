package co.solventa.movil.features.originacion.polizas

/**
 * Las pólizas del prototipo (`docs/pantallas.md`) para las pruebas de interfaz
 * de la billetera (HU-M1.2-01): VH-2026-004512, AU-2025-118204 con placa
 * KJT-482 y AU-2024-093377 cancelada.
 */
object DatosDePantalla {

    const val SINCRONIZADO = 1_789_569_000_000L // 2026-09-16T14:30:00Z
    private const val DIA = 86_400_000L

    private val asegurado = Asegurado("Carolina Pérez Ruiz", "CC", "1020304050")

    val vida = PolizaEnLista(
        Poliza(
            id = "VH-2026-004512", origen = "policy_core", linea = LineasDePoliza.VIDA, producto = "Vida hipotecario",
            objetoAsegurado = null, estado = EstadosDePoliza.ACTIVA, asegurado = asegurado, prima = 68_500.0, moneda = "COP",
            valorAsegurado = 250_000_000.0, coberturas = listOf(Cobertura("death", 250_000_000.0)),
            inicio = SINCRONIZADO, fin = SINCRONIZADO + 7_305 * DIA, renovacionDe = null,
        ),
        Situacion.ACTIVA,
    )

    val autosPorVencer = PolizaEnLista(
        Poliza(
            id = "AU-2025-118204", origen = "policy_core", linea = LineasDePoliza.AUTOS, producto = "Autos",
            objetoAsegurado = "KJT-482", estado = EstadosDePoliza.ACTIVA, asegurado = asegurado, prima = null, moneda = "COP",
            valorAsegurado = 72_500_000.0,
            coberturas = listOf(Cobertura("collision", 72_500_000.0), Cobertura("theft", 72_500_000.0)),
            inicio = SINCRONIZADO - 349 * DIA, fin = SINCRONIZADO + 16 * DIA, renovacionDe = null,
        ),
        Situacion.POR_VENCER,
    )

    val cancelada = PolizaEnLista(
        autosPorVencer.poliza.copy(id = "AU-2024-093377", estado = EstadosDePoliza.CANCELADA, fin = SINCRONIZADO - 4 * DIA),
        Situacion.CANCELADA,
    )
}
