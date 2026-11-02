package co.solventa.movil.core.di

import co.solventa.movil.core.api.notificaciones.DispositivoDto
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificacionesModuleTest {

    @Test
    fun `el registro del dispositivo envia la plataforma aunque sea el valor por defecto`() {
        val cuerpo = JSON_DE_NOTIFICACIONES.encodeToString(
            DispositivoDto.serializer(),
            DispositivoDto(pushHabilitado = false, idioma = "es"),
        )

        val campos = JSON_DE_NOTIFICACIONES.parseToJsonElement(cuerpo).jsonObject
        assertEquals("android", campos.getValue("platform").jsonPrimitive.content)
        assertEquals("es", campos.getValue("locale").jsonPrimitive.content)
    }
}
