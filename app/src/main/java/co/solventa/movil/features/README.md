# `features` — un paquete por dominio

Cada dominio corresponde a un microservicio detrás del API Gateway, igual que en el cliente web. Dentro de cada dominio, un paquete por flujo, y dentro de cada flujo las pantallas de Compose con su `ViewModel` y su estado.

Un `features` **no importa de otro** `features`. Si dos flujos necesitan lo mismo, eso vive en `shared` o en `core`.

| Paquete | Flujos | Pantallas del prototipo | Historias |
|---|---|---|---|
| `acceso/registro/` | Registro con verificación de identidad (KYC): captura del frente y del reverso, confirmación de los datos extraídos, iluminación insuficiente, verificando identidad, resultado y aviso por notificación | M1a–M1h | HU-M1.1-01 |
| `acceso/ingreso/` | Bienvenida, inicio de sesión con biometría o contraseña, tres intentos, sesión expirada | M0, M2a–M2d | HU-M1.1-02 |
| `inicio/` | Pantalla de inicio con avisos y accesos rápidos | M3 | — |
| `originacion/polizas/` | Mis pólizas y detalle de la póliza | M4, M5 | — |
| `originacion/renovaciones/` | Renovación desde la alerta, póliza cancelada por no pago, renovación confirmada | M17, M17b, M17c | M3.2-02 |
| `siniestros/radicacion/` | Asistente guiado de la radicación: identificación, hechos, evidencias y declaración, con borrador local | M11a–M11f, M13b | HU-M2.2-01 |
| `siniestros/evidencia/` | Captura de evidencia con la cámara: fotos y video de hasta 30 s con ubicación, hora en UTC y sello de integridad, subidos en segundo plano con URL firmada | M12, M12b, M12c | HU-M2.1-01 |
| `siniestros/seguimiento/` | Mis siniestros, estados vacío y degradado, detalle con línea de tiempo, siniestro paramétrico resuelto, detalle sin conexión | M9, M9b, M9c, M10, M10b, M10c | M2.4-01, M2.4-02, M3.1-01 |
| `siniestros/asistencias/` | Solicitar asistencia en sitio, ubicación desactivada, sin señal de satélite | M14, M14b, M14c | M2.3-01 |
| `notificaciones/centro/` | Centro de notificaciones, *push* en pantalla bloqueada, adjuntar soporte pendiente y su confirmación | M15, M15b, M18, M18b, M18c | M3.1-01, M3.2-01, M3.3-01, M3.3-02 |
| `notificaciones/preferencias/` | Qué notificaciones recibe el asegurado y por qué canal | (desde M19) | M3.3-01 |
| `auditoria/consentimientos/` | Privacidad y consentimientos del titular y su detalle, con la copia sin conexión en Room y la sincronización al volver la red (**HU-M1.3-01**: `ConsentimientosRepositorio`, trabajo de WorkManager, M8 y M8b). Revocación efectiva (**HU-M1.3-02**): `ConfirmadorDeRevocacion` (huella o contraseña), `RevocacionRepositorio` (la solicitud se guarda primero en la cola de Room y se envía con clave de idempotencia; sin red queda pendiente), `EnvioDeRevocacionesWorker` con trabajo urgente que exige red, `RevocacionViewModel`, la hoja de confirmación, la pantalla de revocado, la sección «Pendientes de envío» y el comprobante en PDF con el selector del sistema | M8, M8b, `37:1519`, `38:1414`, `38:1503` | HU-M1.3-01 · HU-M1.3-02 · RN-02 |
| `perfil/` | Perfil del asegurado (**HU-M1.3-01**): identidad, biometría, cierre automático, Privacidad y consentimientos, Idioma y Cerrar sesión; las filas sin servicio quedan en espera. **HU-M1.3-02:** antes de cerrar la sesión avisa si hay revocaciones en cola (no se pierden: se envían cuando la misma cuenta vuelve a entrar) | M19 | HU-M1.3-01 · HU-M1.3-02 |
| `perfil/idioma/` | Cambio de idioma en caliente y formatos de la región | M19b | OBJ 12 |

El prototipo completo está en [`docs/pantallas.md`](../../../../../../docs/pantallas.md).
