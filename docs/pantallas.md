# Catálogo de pantallas del cliente móvil

Prototipo: [`Solventa — Prototipo S7 — Pantallas`](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas), página **`02 · Móvil`**. Punto de inicio del recorrido: **M0 · Bienvenida**. Segundo punto de inicio: **M15b · Push en pantalla bloqueada**.

Esta tabla es el contrato entre el prototipo y el código: **el paquete de la columna «Paquete» es donde va la pantalla**, y el nombre de la rama es el ID de la columna «Historia». Si una pantalla cambia de paquete, se actualiza esta fila en el mismo *pull request*.

> La columna «Frame» de la sección de identidad enlaza al frame exacto. Las demás secciones todavía usan solo el identificador del mapa de navegación; falta copiar sus `node-id` desde Figma.

## Identidad y acceso

Las ocho pantallas de esta sección cubren los seis criterios de aceptación de HU-M1.1-01 y los cinco de HU-M1.1-02.

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| [M0](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=13-177) | Bienvenida | `features/acceso/bienvenida` | — | — | Completa. *Crear mi cuenta* → M1a · *Ya tengo cuenta* → M2a · el selector de idioma abre una hoja inferior que el prototipo no dibuja (`shared/ui/HojaDeIdioma` desde HU-M1.3-01, porque también la abre M19) |
| [M1a](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=18-227) | Captura del documento (frente) | `features/acceso/registro` | **HU-M1.1-01** | **Must** | Completa. Criterio 1, con la cámara del dispositivo |
| [M1b](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=18-324) | Captura del reverso | `features/acceso/registro` | **HU-M1.1-01** | **Must** | Completa. Criterio 1, con la cámara del dispositivo |
| [M1c](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=18-412) | Confirmación de los datos | `features/acceso/registro` | **HU-M1.1-01** | **Must** | **Lleva un campo que el frame no tiene: la contraseña.** Criterio 1 |
| [M1d](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=22-373) | Verificación exitosa · cuenta activa | `features/acceso/registro` | **HU-M1.1-01** | **Must** | Completa. Criterio 2. Sale a M3 · Inicio |
| [M1e](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=22-440) | Verificación no superada | `features/acceso/registro` | **HU-M1.1-01** | **Must** | Completa. Criterio 3: no revela el motivo técnico y ofrece soporte |
| [M1f](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=22-520) | Calidad insuficiente durante la captura | `features/acceso/registro` | **HU-M1.1-01** | **Must** | Completa. Criterio 4. La poca luz la mide el propio sensor; la borrosidad la juzga el proveedor |
| [M1g](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=1242-3278) | Verificando identidad | `features/acceso/registro` | **HU-M1.1-01** | **Must** | **Creada el 09/10/2026.** Criterio 6, primera mitad: estado en proceso sin bloquear. Avanza sola a los 3 s |
| [M1h](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=1242-3304) | La verificación tarda · aviso por notificación | `features/acceso/registro` | **HU-M1.1-01** | **Must** | **Creada el 09/10/2026.** Criterio 6, segunda mitad (> 30 s) y criterio 5: la cuenta no queda activa |
| [M2a](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=13-250) | Inicio de sesión con biometría | `features/acceso/ingreso` | **HU-M1.1-02** | **Must** | Completa |
| [M2b](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=23-489) | Inicio de sesión con contraseña | `features/acceso/ingreso` | **HU-M1.1-02** | **Must** | Completa. Criterio 4: sin biometría configurada |
| [M2c](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=23-571) | Biometría no reconocida · tres intentos | `features/acceso/ingreso` | **HU-M1.1-02** | **Must** | Completa. Criterio 3 |
| [M2d](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=23-631) | Sesión expirada a los cinco minutos | `features/acceso/ingreso` | HU-M1.1-02 · HA-S01 | Must | Completa. Criterio 5 |

Recorrido del registro: **M0 → M1a → M1b → M1c → M1g → M1d → M3**. Desvíos: M1a o M1b → M1f (calidad) · M1g → M1e (el servicio no respondió o rechazó la foto del documento) · M1g → M1h (tarda) → M0. Un dato rechazado o un documento ya registrado no sacan de M1c.

El idioma de M0 se elige en la aplicación y el cambio se ve **sin reiniciarla**: la jerarquía se recompone con el idioma elegido, y la hoja de selección se dibuja en su propia ventana, así que vuelve a aplicarlo dentro. Sin elección previa manda el del dispositivo. Cada idioma se lee en el suyo: «Español (Colombia)» y «English».

M1a y M1b usan la cámara de verdad (CameraX). El permiso se pide en caliente y la pantalla tiene estado propio para cada caso: sin permiso ofrece concederlo, negado manda a los ajustes —Android ya no deja volver a preguntar— y sin cámara lo dice y deja el marcador. **La poca luz se mide en el dispositivo**, promediando la luminancia del plano Y de la vista previa, y el aviso sale antes de tomar la foto, que es lo que pide el criterio 4. La foto va a la caché de la aplicación, no a la galería. Los datos que M1c muestra **los genera la aplicación** y el usuario los corrige en pantalla: el documento no se lee de la foto. Leerlo en el propio teléfono con reconocimiento de texto se descartó el 17/10/2026, y lo que la historia tiene que demostrar es que el dato diligenciado llega a la base de datos y que con esas credenciales se puede entrar.


## Inicio y pólizas

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| M3 | Inicio | `features/inicio` | — | — | Completa |
| M4 | Mis pólizas | `features/originacion/polizas` | — | — | Completa. La tarjeta AU-2024-093377 · No pago entra a M17b |
| M5 | Detalle de la póliza | `features/originacion/polizas` | — | — | Completa |

## Siniestros · consulta y seguimiento

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| M9 | Mis siniestros | `features/siniestros/seguimiento` | M2.4-01 | Should | Completa. Lista con estados, borrador y «actualizado hoy 14:30 · disponible sin conexión» |
| M9b | Mis siniestros · sin siniestros | `features/siniestros/seguimiento` | M2.4-02 | Could | Completa (estado vacío) |
| M9c | Mis siniestros · servicio degradado (caché) | `features/siniestros/seguimiento` | M2.4-02 · HA-L02 | Could | Completa. Es la evidencia visible de la caché de lectura |
| M10 | Detalle del siniestro · línea de tiempo | `features/siniestros/seguimiento` | M2.4-01 | Should | Completa. Misma línea de tiempo que la web (A8) |
| M10b | Siniestro paramétrico resuelto automáticamente | `features/siniestros/seguimiento` | M3.1-01 | **Must** | Completa. Parámetro, umbral, valor observado y clave de idempotencia — HA-D02 |
| M10c | Detalle del siniestro sin conexión | `features/siniestros/seguimiento` | M2.4-02 | Could | Completa |

## Radicación del siniestro

Las cuatro etapas del asistente guiado, su confirmación y los dos estados que el prototipo dibuja aparte.

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| [M11a](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-1925) | Paso 1 · Identificación | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | Completa |
| [M11b](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2072) | Paso 2 · Hechos | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | Completa. Las preguntas del tipo elegido salen aquí |
| [M11c](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2208) | Paso 3 · Evidencias | `features/siniestros/radicacion` | HU-M2.2-01 · **HU-M2.1-01** | **Must** | Completa. La etapa es del asistente; lo que se captura es de HU-M2.1-01 |
| [M11d](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2397) | Paso 4 · Declaración | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | Completa |
| [M11d2](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=701-13532) | Declaración confirmada | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | **Es un estado de M11d**, con la casilla marcada |
| [M11e](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2510) | Radicado generado | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | Completa. El código único y el resumen del caso |
| [M11f](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2568) | Avanzar deshabilitado por faltantes | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | **Es un estado del paso 3**: el mínimo de fotos lo pone HU-M2.1-01 |
| [M13b](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=51-2519) | Continuar borrador al reabrir | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | La **hoja inferior** es de esta historia; la lista del fondo es de M2.4-01 |
| [M12](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=48-2222) | Cámara · captura de evidencia | `features/siniestros/radicacion` | HU-M2.1-01 | **Must** | Completa |
| [M12b](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=48-2293) | Permisos de cámara y ubicación | `features/siniestros/radicacion` | HU-M2.1-01 | **Must** | Completa |
| [M12c](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=48-2413) | Precisión del GPS insuficiente | `features/siniestros/radicacion` | HU-M2.1-01 | **Must** | Completa |

Recorrido: **M3 → M11a → M11b → M11c → M11d → M11e → M3**. Desvíos: al reabrir con un reporte sin terminar, la hoja
de M13b ofrece retomarlo en su etapa; un dato que el servicio rechaza devuelve a la etapa donde se corrige.

Tres diferencias con el prototipo, todas por historias que aún no existen:

- **La hoja «Tienes un reporte sin terminar» aparece al abrir el asistente**, no sobre *Mis siniestros*: esa lista es
  de HU-M2.4-01. El contenido es el del frame.
- **El paso 3 no exige evidencia todavía.** La cuadrícula se muestra sin acción y con un aviso; el mínimo por tipo y
  el estado de M11f se activan con HU-M2.1-01.
- **El selector de pólizas lee una lista local** con la póliza del prototipo, hasta que `GET /v1/policies` esté en
  `develop` (HU-M1.2-01).

Y dos textos que el prototipo no define y hubo que escribir: las preguntas de **hurto**, **daños por terceros** y
**otro** —M11b solo dibuja las de colisión— y las etiquetas de los campos del paso 2, que viven dentro de instancias
del componente `Campo` y no se leen desde fuera.

## Asistencia en sitio

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| M14 | Solicitar asistencia · ubicación enviada y llamada | `features/siniestros/asistencias` | M2.3-01 | Should | Completa. Coordenadas, precisión, línea de atención y estado de la solicitud |
| M14b | Ubicación o permisos desactivados | `features/siniestros/asistencias` | M2.3-01 | Should | Completa (excepción) |
| M14c | Sin señal de satélite · la llamada se realiza igual | `features/siniestros/asistencias` | M2.3-01 | Should | Completa (excepción) |

## Notificaciones y renovación

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| M15 | Centro de notificaciones | `features/notificaciones/centro` | M3.1-01 · M3.2-01 · M3.3-01 | Must + Should | Completa. Los tres tipos de notificación con su acción |
| M15b | Push del sistema (pantalla bloqueada) | `features/notificaciones/centro` | M3.1-01 | **Must** | Completa. Segundo punto de inicio del recorrido |
| M17 | Renovación desde la alerta (datos precargados) | `features/originacion/renovaciones` | M3.2-02 | Could | Completa. Mismas cifras que la web (A4) |
| M17b | Póliza cancelada por no pago · opciones | `features/originacion/renovaciones` | M3.2-02 · W1.6-02 | Could | Completa. *Solicitar recotización* sale al flujo web (E1) |
| M17c | Renovación confirmada | `features/originacion/renovaciones` | M3.2-02 | Could | Completa |
| M18 | Adjuntar soporte pendiente | `features/notificaciones/centro` | M3.3-02 | Could | Completa |
| M18b | Adjuntar soporte · soportes completos | `features/notificaciones/centro` | M3.3-02 | Could | Completa |
| M18c | Soportes enviados · radicación reactivada | `features/notificaciones/centro` | M3.3-02 | Could | Completa |

## Perfil, privacidad e idioma

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| M19 | Perfil | `features/perfil` | **HU-M1.3-01** | **Must** | **Se construye la variante [`1376:3396`](https://www.figma.com/design/PvaIbF3OlV5AwjdnI4Cm1n?node-id=1376-3396) de la copia de trabajo (D-7 e)** con la barra superior de [`55:3017`](https://www.figma.com/design/PvaIbF3OlV5AwjdnI4Cm1n?node-id=55-3017) (es pestaña: sin flecha). Activas: huella o rostro, cierre automático de 5 min, Privacidad y consentimientos con «N activos», Idioma (abre la hoja de M0) y Cerrar sesión. **En espera con «Próximamente»**, atenuadas y sin toque: Datos personales, Medios de pago, Cambiar contraseña, Descargar mis datos, Notificaciones y Sincronización. Sin documento, «Identidad verificada (KYC)», ícono de ajustes ni términos (sin datos detrás) |
| M19b | Idioma · English (US) | `features/perfil/idioma` | OBJ 12 | — | Completa. Espejo de M10 con formatos de EE. UU. **No se construye en HU-M1.3-01:** la fila Idioma de M19 abre la hoja de M0 |
| [M8](https://www.figma.com/design/PvaIbF3OlV5AwjdnI4Cm1n?node-id=37-1233) | Privacidad y consentimientos | `features/auditoria/consentimientos` | **HU-M1.3-01** · RN-02 | **Must** | Criterios 1, 3, 5 y 6: activos y revocados o vencidos con fecha, vigencia, insignia y comprobante; aviso sin conexión del frame [`38:1503`](https://www.figma.com/design/PvaIbF3OlV5AwjdnI4Cm1n?node-id=38-1503) con la fecha y hora de la copia; cargando, vacío, error y cuenta sin vincular. Quedan para HU-M1.3-02 la segunda frase del texto, la tarjeta «Revocación efectiva en segundos» y la cola de envío; «Última consulta» no tiene criterio ni datos |
| [M8b](https://www.figma.com/design/PvaIbF3OlV5AwjdnI4Cm1n?node-id=37-1418) | Detalle del consentimiento | `features/auditoria/consentimientos` | **HU-M1.3-01** · RN-02 | **Must** | Criterios 2 a 4: identificador, entidad, alcance, finalidad, otorgamiento con canal, vigencia o fin con comprobante e historial de estados. «Revocar consentimiento» visible y **deshabilitado** solo en activos hasta HU-M1.3-02 (D-8 b). Sin «Última consulta», comprobante PDF ni dispositivo del otorgamiento (sin datos) |
| — | Preferencias de notificación | `features/notificaciones/preferencias` | M3.3-01 | Should | Entra desde M19 |

## Convenciones del prototipo que el código debe respetar

- Lienzo de referencia **390 × 844**, `minSdk 26`.
- Los datos de la demostración coinciden con el portal: los cuatro siniestros (SIN-2026-01877, -01655, -01702, -01410), la renovación de AU-2025-118204 y el pago paramétrico de $ 180.000 COP son los mismos en A7/A8/A4 y en M9/M10/M17. **No inventar datos nuevos** en las pruebas de interfaz.
- En el prototipo, los íconos de filtro y de compartir se usan como atajo para alternar los estados de excepción (M9 → M9b → M9c, M10 → M10c). Eso es un recurso de la demostración: **en el código esos estados dependen de la respuesta del servicio, no de un ícono**.
- La barra inferior es Inicio (M3) · Pólizas (M4) · Siniestros (M9) · Perfil (M19) y vive en `core/navegacion/BarraInferior.kt` (HU-M1.3-01, D-14 b). Se muestra en Inicio, Perfil y Privacidad y consentimientos (que deja Perfil seleccionada). **Pólizas y Siniestros quedan en espera** —visibles, atenuadas y sin navegar, con la descripción «…, próximamente»— hasta que existan M4 (HU-M1.2-01) y M9 (HU-M2.4-01); la radicación sigue entrando por el botón de Inicio.
