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
| [M1c](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=18-412) | Confirmación de los datos | `features/acceso/registro` | **HU-M1.1-01** | **Must** | **Lleva un campo que el frame no tiene: la contraseña.** Criterio 1: los datos los lee el proveedor de identidad (`POST /v1/registrations/document-reads`) y el usuario los confirma; el correo y la contraseña los escribe él |
| [M1d](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=22-373) | Verificación exitosa · cuenta activa | `features/acceso/registro` | **HU-M1.1-01** | **Must** | Completa. Criterio 2. Sale a M3 · Inicio |
| [M1e](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=22-440) | Verificación no superada | `features/acceso/registro` | **HU-M1.1-01** | **Must** | Completa. Criterio 3: no revela el motivo técnico y ofrece soporte. Llega también con el `422 identity_not_verified` del proveedor |
| [M1f](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=22-520) | Calidad insuficiente durante la captura | `features/acceso/registro` | **HU-M1.1-01** | **Must** | Completa. Criterio 4. La poca luz la mide el propio sensor; la borrosidad la juzga el teléfono sobre la foto tomada (`AnalizadorDeNitidez`, varianza del laplaciano) |
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
| M4 | Mis pólizas | `features/originacion/polizas` | **HU-M1.2-01** | **Must** | Criterios 1, 3, 4 y 5. Pestaña Pólizas de la barra inferior (sin flecha de volver). Lista de la copia local cifrada: tarjeta con tipo, número y vigencia, insignia (Activa, Por vencer, Vencida, Cancelada; «· renovada»), valor asegurado y prima u objeto asegurado. Con red, «Última sincronización: … · disponible sin conexión»; sin red, el aviso «Sin conexión» con la fecha y hora de la copia y el ícono de sin red; tras sincronizar con cambios, «Sincronización completada · N cambios» (renovada, cancelada o modificada) con botón para cerrarlo; una copia de más de 30 días se advierte sin esconder nada. También cargando, vacío y error con reintento. Sin la lupa del encabezado (no tiene criterio). La tarjeta AU-2024-093377 abre M5; M17b es de HU-M3.2-02 |
| M5 | Detalle de la póliza | `features/originacion/polizas` | **HU-M1.2-01** | **Must** | Criterio 2, también sin conexión: número de póliza, tipo de seguro, bien asegurado, vigencia (del … al …), valor asegurado, prima si la hay, coberturas incluidas con su monto y asegurado principal (nombre y documento). Sin conexión, el aviso con la hora de la copia y el ícono de sin red; si la última sincronización la cambió, el aviso del cambio. Sin frame en los mockups de la historia: se arma con los componentes de M4 y M8b |

## Siniestros · consulta y seguimiento

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| M9 | Mis siniestros | `features/siniestros/seguimiento` | M2.4-01 | Should | Completa. Lista con estados, borrador y «actualizado hoy 14:30 · disponible sin conexión» |
| M9b | Mis siniestros · sin siniestros | `features/siniestros/seguimiento` | M2.4-02 | Could | Completa (estado vacío) |
| M9c | Mis siniestros · servicio degradado (caché) | `features/siniestros/seguimiento` | M2.4-02 · HA-L02 | Could | Completa. Es la evidencia visible de la caché de lectura |
| M10 | Detalle del siniestro · línea de tiempo | `features/siniestros/seguimiento` | M2.4-01 | Should | Completa. Misma línea de tiempo que la web (A8) |
| M10b | Siniestro paramétrico resuelto automáticamente | `features/notificaciones/centro` | **HU-M3.1-01** | **Must** | Criterio 4: lo abre la notificación sin pasos intermedios. Monto y moneda, evento (retraso del vuelo AV-8421), parámetro, umbral, valor observado con el aviso de que lo superó, medio de pago (Visa •••• 1111), fecha, póliza, siniestro y clave de idempotencia (HA-D02); sin conexión, desde la copia local. **Vive en `notificaciones/centro` y no en `siniestros/seguimiento`** porque sus datos son los del registro de la notificación (decisión del equipo) y un dominio no importa a otro |
| M10c | Detalle del siniestro sin conexión | `features/siniestros/seguimiento` | M2.4-02 | Could | Completa |

## Radicación del siniestro

Las cuatro etapas del asistente guiado, su confirmación y los dos estados que el prototipo dibuja aparte.

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| [M11a](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-1925) | Paso 1 · Identificación | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | Completa |
| [M11b](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2072) | Paso 2 · Hechos | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | Completa. Las preguntas del tipo elegido salen aquí, y el monto estimado opcional (reglas de la web, HU-W2.2-01: pesos enteros de 1 a 100.000.000.000, «Se enviará como …»), que alimenta el triage y la evaluación |
| [M11c](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2208) | Paso 3 · Evidencias | `features/siniestros/radicacion` | HU-M2.2-01 · **HU-M2.1-01** | **Must** | Completa. La etapa es del asistente; lo que se captura es de HU-M2.1-01 |
| [M11d](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2397) | Paso 4 · Declaración | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | Completa |
| [M11d2](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=701-13532) | Declaración confirmada | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | **Es un estado de M11d**, con la casilla marcada |
| [M11e](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2510) | Radicado generado | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | Completa. El código único y el resumen del caso |
| [M11f](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=43-2568) | Avanzar deshabilitado por faltantes | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | **Es un estado del paso 3**: se activa mientras falte el mínimo de cuatro fotos capturadas |
| [M13b](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=51-2519) | Continuar borrador al reabrir | `features/siniestros/radicacion` | **HU-M2.2-01** | **Must** | La **hoja inferior** es de esta historia; la lista del fondo es de M2.4-01 |
| [M12](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=48-2222) | Cámara · captura de evidencia | `features/siniestros/evidencia` | **HU-M2.1-01** | **Must** | Completa. Foto y video de hasta 30 s, con el sello de ubicación y hora del frame. **Sin la silueta de vehículo del frame** y con un aviso de grabación que el frame no dibuja |
| [M12b](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=48-2293) | Permisos de cámara y ubicación | `features/siniestros/evidencia` | **HU-M2.1-01** | **Must** | Completa. La explicación es de la aplicación; **las tres opciones las pone Android**, que es lo que el frame dibuja |
| [M12c](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=48-2413) | Precisión del GPS insuficiente | `features/siniestros/evidencia` | **HU-M2.1-01** | **Must** | Completa. Criterio 3: avisa antes de capturar y deja capturar igual, marcando la evidencia |

Recorrido: **M3 → M11a → M11b → M11c → M11d → M11e → M3**, con **M11c → M12** una vez por evidencia. Desvíos: al
reabrir con un reporte sin terminar, la hoja de M13b ofrece retomarlo en su etapa; un dato que el servicio rechaza
devuelve a la etapa donde se corrige, y una evidencia rechazada devuelve al paso 3.

Dos diferencias con el frame del visor, las dos a propósito: **la silueta de vehículo no se dibuja**, porque la
evidencia se captura para cuatro tipos de siniestro y una silueta de carro estorba encuadrando un vidrio roto; y
**se agrega un aviso de «Grabando»**, porque sin él no hay forma de saber que la cámara está grabando.

Dos añadidos al frame del paso 3: **tocar una casilla con foto abre lo capturado** —la fotografía a pantalla
completa, el video con su reproductor— y cada casilla lleva una **caneca** para descartarlo y volver a tomarlo.
El frame no los dibuja, pero sin ellos no hay manera de revisar una toma ni de corregir una que salió mal, y
una casilla ocupada por algo inservible deja el reporte trabado. La vista a pantalla completa lleva además **la hora UTC y la
ubicación de esa captura**, que es lo que pide el criterio 5 y en una casilla de ese tamaño no se lee.

La evidencia se sube **en segundo plano**: la cámara vuelve al paso 3 apenas la captura queda registrada, y cada
casilla muestra en qué va su envío. El archivo no pasa por el API Gateway sino que va directo al repositorio de
evidencia con una URL firmada, que es lo que permite que quepa un video de 30 segundos. *Continuar* se habilita con el mínimo
**capturado**, porque el criterio 5 pide que el envío no bloquee a quien está en el lugar; quien espera por las
subidas es **Radicar**, que viaja con las llaves y el servicio comprueba cada una.

Tres diferencias con el prototipo, todas por historias que aún no existen:

- **La hoja «Tienes un reporte sin terminar» aparece al abrir el asistente**, no sobre *Mis siniestros*: esa lista es
  de HU-M2.4-01. El contenido es el del frame.
- **El mínimo de evidencia es de cuatro fotos para todos los tipos de siniestro.** Es el único número que da el
  prototipo (M11c, «al menos 4»); un mínimo distinto por tipo no lo respalda ningún criterio ni ningún frame.
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
| M15 | Centro de notificaciones | `features/notificaciones/centro` | **HU-M3.1-01** · M3.2-01 · M3.3-01 | Must + Should | HU-M3.1-01, criterios 1, 5 y 6: «Hoy» y «Anteriores», la tarjeta del pago automático («Pago automático recibido: $ 180.000 COP», retraso del vuelo, minutos y medio de pago) con la barra y el punto de no leída, «Ver detalle» y «Marcar todas como leídas»; cargando, vacío, error y sin conexión con la hora de la copia. Se entra desde Inicio (botón provisional en lugar de la campana). Las tarjetas de renovación y de soporte pendiente son de M3.2-01 y M3.3-01; el ícono de filtros no tiene criterio |
| M15b | Push del sistema (pantalla bloqueada) | `features/notificaciones/centro` | **HU-M3.1-01** | **Must** | Criterios 2, 3, 4 y 6: la notificación del sistema con el monto, la moneda y el evento, en el idioma de la aplicación, con «Ver detalle»; una por pago en el grupo de los pagos automáticos, con el permiso de Android 13 pedido al llegar a Inicio. **Sin Firebase (decisión del equipo), no la empuja el servidor:** la aplicación revisa el centro cada 20 s con la aplicación a la vista y cada 15 min en segundo plano (WorkManager), y la muestra al encontrar un pago nuevo. «Adjuntar soporte» y «Más tarde» son de M3.3-01 |
| M17 | Renovación desde la alerta (datos precargados) | `features/originacion/renovaciones` | M3.2-02 | Could | Completa. Mismas cifras que la web (A4) |
| M17b | Póliza cancelada por no pago · opciones | `features/originacion/renovaciones` | M3.2-02 · W1.6-02 | Could | Completa. *Solicitar recotización* sale al flujo web (E1) |
| M17c | Renovación confirmada | `features/originacion/renovaciones` | M3.2-02 | Could | Completa |
| M18 | Adjuntar soporte pendiente | `features/notificaciones/centro` | M3.3-02 | Could | Completa |
| M18b | Adjuntar soporte · soportes completos | `features/notificaciones/centro` | M3.3-02 | Could | Completa |
| M18c | Soportes enviados · radicación reactivada | `features/notificaciones/centro` | M3.3-02 | Could | Completa |

## Perfil, privacidad e idioma

| Frame | Pantalla | Paquete | Historia | Prioridad | Estado del prototipo |
|---|---|---|---|---|---|
| [M19](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=55-3017) | Perfil | `features/perfil` | **HU-M1.3-01** | **Must** | Es pestaña de la barra inferior: sin flecha de volver. Activas: huella o rostro, cierre automático de 5 min, Privacidad y consentimientos con «N activos», Idioma (abre la hoja de M0) y Cerrar sesión. **En espera con «Próximamente»**, atenuadas y sin toque: Datos personales, Medios de pago, Cambiar contraseña, Descargar mis datos, Notificaciones y Sincronización. Sin documento, «Identidad verificada (KYC)», ícono de ajustes ni términos (sin datos detrás) |
| M19b | Idioma · English (US) | `features/perfil/idioma` | OBJ 12 | — | Completa. Espejo de M10 con formatos de EE. UU. **No se construye en HU-M1.3-01:** la fila Idioma de M19 abre la hoja de M0 |
| [M8](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=37-1233) | Privacidad y consentimientos | `features/auditoria/consentimientos` | **HU-M1.3-01** · RN-02 | **Must** | Criterios 1, 3, 5 y 6: activos y revocados o vencidos con fecha, vigencia, insignia y comprobante; aviso sin conexión del frame [`38:1503`](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=38-1503) con la fecha y hora de la copia y el ícono de sin red en la barra superior; cargando, vacío, error y cuenta sin vincular. La cola de envío de HU-M1.3-02 va en la fila de `38:1503`; la segunda frase del texto y la tarjeta «Revocación efectiva en segundos» no tienen criterio detrás; «Última consulta» no tiene criterio ni datos |
| [M8b](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=37-1418) | Detalle del consentimiento | `features/auditoria/consentimientos` | **HU-M1.3-01** · **HU-M1.3-02** · RN-02 | **Must** | HU-M1.3-01, criterios 2 a 4: identificador, entidad, alcance, finalidad, otorgamiento con canal, vigencia o fin con comprobante e historial de estados. HU-M1.3-02, criterio 6: «Revocar consentimiento», en rojo, **activo** solo en un activo que el servicio deja revocar; no aparece en revocados, vencidos ni con la revocación en cola. Con la revocación en cola, el detalle la avisa, lleva la insignia «Revocación pendiente de envío», termina el historial en «Revocación solicitada · pendiente de envío» y ofrece «Reintentar envío ahora», deshabilitado sin señal. En una revocación parcial avisa la entidad que no confirmó, lleva la insignia «Revocación parcial» y el enlace «Descargar comprobante de revocación (PDF)», sin botón de acción. Sin «Última consulta», comprobante de otorgamiento ni dispositivo del otorgamiento (sin datos) |
| [`37:1519`](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=37-1519) | Confirmar la revocación (hoja inferior) | `features/auditoria/consentimientos` | **HU-M1.3-02** · RN-02 | **Must** | Criterios 1 y 2: título con la entidad, efecto de la revocación, «Confirmar con biometría» y «Cancelar». Sin huella habilitada para Solventa, la misma hoja pide la contraseña de Solventa (campo «Contraseña» del inicio de sesión, mínimo 10 caracteres) y confirma con «Confirmar con contraseña», que se verifica con el proveedor de identidad. Cancelar, tocar fuera de la hoja o no confirmar dejan el detalle como estaba |
| [`38:1414`](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=38-1414) | Consentimiento revocado y comprobante | `features/auditoria/consentimientos` | **HU-M1.3-02** · RN-02 | **Must** | Criterios 3 y 4: «Efectivo en X segundos» (o «Efectivo desde» la hora si el reloj del teléfono iba adelantado), consentimiento y entidad, estado, solicitado, efectivo desde, comprobante, «Qué cambia», «Descargar comprobante (PDF)» con el selector de archivos del sistema y «Volver a mis consentimientos». Se abre solo cuando el servicio confirmó en línea; sin barra inferior |
| [`38:1503`](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas?node-id=38-1503) | Privacidad y consentimientos sin conexión | `features/auditoria/consentimientos` | **HU-M1.3-01** · **HU-M1.3-02** · RN-02 | **Must** | HU-M1.3-01: aviso sin conexión con la fecha y hora de la copia e ícono de sin red. HU-M1.3-02, criterio 5: con una revocación en cola, el aviso «Sin conexión: revocación en cola» con la entidad y la hora, la sección «Pendientes de envío (n)» con la tarjeta «Revocación solicitada…», la insignia «Revocación pendiente de envío», «Se reintenta al recuperar señal» y «Reintentar envío ahora», deshabilitado sin señal. Una revocación en cola que el servicio ya no acepta (el consentimiento venció mientras esperaba) se avisa con el componente «Aviso» |
| — | Preferencias de notificación | `features/notificaciones/preferencias` | M3.3-01 | Should | Entra desde M19 |

## Convenciones del prototipo que el código debe respetar

- Lienzo de referencia **390 × 844**, `minSdk 26`.
- Los datos de la demostración coinciden con el portal: los cuatro siniestros (SIN-2026-01877, -01655, -01702, -01410), la renovación de AU-2025-118204 y el pago paramétrico de $ 180.000 COP son los mismos en A7/A8/A4 y en M9/M10/M17. **No inventar datos nuevos** en las pruebas de interfaz.
- En el prototipo, los íconos de filtro y de compartir se usan como atajo para alternar los estados de excepción (M9 → M9b → M9c, M10 → M10c). Eso es un recurso de la demostración: **en el código esos estados dependen de la respuesta del servicio, no de un ícono**.
- La barra inferior es Inicio (M3) · Pólizas (M4) · Siniestros (M9) · Perfil (M19) y vive en `core/navegacion/BarraInferior.kt` (HU-M1.3-01, D-14 b). Se muestra en Inicio, Perfil y Privacidad y consentimientos (que deja Perfil seleccionada). **Siniestros queda en espera** —visible, atenuada y sin navegar, con la descripción «Siniestros, próximamente»— hasta que exista M9 (HU-M2.4-01); la radicación sigue entrando por el botón de Inicio. **Pólizas navega a M4 desde HU-M1.2-01** y la barra se muestra también allí, pero no en M5.
