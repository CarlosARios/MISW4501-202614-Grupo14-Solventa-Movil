package co.solventa.movil.features.siniestros.radicacion

import androidx.annotation.StringRes
import co.solventa.movil.R

/**
 * Las preguntas de la etapa de hechos dependen del tipo de incidente
 * (criterio 2). El frame M11a lo anuncia: *«Las siguientes preguntas se
 * adaptan al tipo de incidente y a tu póliza»*.
 *
 * Una opción guarda su identificador, no su etiqueta: lo que llega al
 * servicio tiene que ser igual en español y en inglés.
 */
data class OpcionDeRespuesta(
    val valor: String,
    @StringRes val etiqueta: Int,
    /** Línea bajo la etiqueta, como «Adjuntarás el informe» en M11b. */
    @StringRes val detalle: Int? = null,
)

data class PreguntaAdaptativa(
    /** Clave con la que la respuesta viaja en `details`. */
    val id: String,
    @StringRes val etiqueta: Int,
    /** Vacío significa respuesta escrita. */
    val opciones: List<OpcionDeRespuesta> = emptyList(),
    val obligatoria: Boolean = true,
) {
    val esAbierta: Boolean get() = opciones.isEmpty()
}

private val SI_O_NO = listOf(
    OpcionDeRespuesta("si", R.string.comun_si),
    OpcionDeRespuesta("no", R.string.comun_no),
)

/** El orden es el del frame: en «¿hubo lesionados?» primero va el No. */
private val NO_O_SI = listOf(
    OpcionDeRespuesta("no", R.string.comun_no),
    OpcionDeRespuesta("si", R.string.comun_si),
)

/** Sin tipo elegido no hay preguntas que mostrar. */
fun preguntasDe(tipo: TipoDeIncidente?): List<PreguntaAdaptativa> = when (tipo) {
    null -> emptyList()

    // Las dos del frame M11b, con su mismo texto y en su mismo orden.
    TipoDeIncidente.COLISION -> listOf(
        PreguntaAdaptativa("personas_lesionadas", R.string.radicacion_pregunta_lesionados, NO_O_SI),
        PreguntaAdaptativa(
            "informe_policia",
            R.string.radicacion_pregunta_informe_policia,
            listOf(
                OpcionDeRespuesta("si", R.string.comun_si, R.string.radicacion_opcion_adjuntara_informe),
                OpcionDeRespuesta("no", R.string.comun_no),
            ),
        ),
    )

    TipoDeIncidente.HURTO -> listOf(
        PreguntaAdaptativa(
            "alcance_del_hurto",
            R.string.radicacion_pregunta_alcance_hurto,
            listOf(
                OpcionDeRespuesta("total", R.string.radicacion_opcion_total),
                OpcionDeRespuesta("parcial", R.string.radicacion_opcion_parcial),
            ),
        ),
        PreguntaAdaptativa("denuncia_presentada", R.string.radicacion_pregunta_denuncia, SI_O_NO),
    )

    TipoDeIncidente.DANOS_POR_TERCEROS -> listOf(
        PreguntaAdaptativa(
            "origen_del_dano",
            R.string.radicacion_pregunta_origen,
            listOf(
                OpcionDeRespuesta("vandalismo", R.string.radicacion_opcion_vandalismo),
                OpcionDeRespuesta("granizo", R.string.radicacion_opcion_granizo),
                OpcionDeRespuesta("inundacion", R.string.radicacion_opcion_inundacion),
                OpcionDeRespuesta("otro", R.string.radicacion_opcion_otro_origen),
            ),
        ),
        PreguntaAdaptativa("tercero_identificado", R.string.radicacion_pregunta_tercero, SI_O_NO),
    )

    // El frame lo dice en su subtítulo: «Te haremos preguntas adicionales».
    TipoDeIncidente.OTRO -> listOf(
        PreguntaAdaptativa("que_ocurrio", R.string.radicacion_pregunta_que_ocurrio),
    )
}
