# Cómo este repositorio respeta la arquitectura de Solventa

Resumen de lo que los [modelos de arquitectura](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Arquitectura_final) y las [decisiones registradas](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Recopilaci%C3%B3n-Decisiones-de-Arquitectura) fijan para el cliente móvil, y cómo se materializa en la estructura del código. Es el espejo del [mismo documento del cliente web](https://github.com/nicolasjaramillocely99/MISW4501-202614-Grupo14-Solventa-FrontWeb/blob/main/docs/arquitectura.md).

## 1. Lugar del cliente móvil en el modelo de componentes

| Elemento del modelo | Valor | En este repositorio |
|---|---|---|
| Capa | **LAYER 4 · Clientes** (`App móvil (Kotlin)`) | Toda la aplicación |
| Conector de salida | `App móvil → API Gateway`: autoservicio del asegurado en movilidad | `core/api/` — único punto de salida HTTP |
| Capa que puede usar | Solo **LAYER 3 · Gateway** (API Gateway «Facade»). Cada capa usa la inmediatamente inferior y nunca al revés | Ningún código conoce direcciones de microservicios, bases de datos, Redis ni SNS/SQS |
| Servicios detrás del gateway que la app consume | Originación (pólizas, renovación) · Siniestros (radicación, consulta, asistencias) · Notificaciones (token del dispositivo y preferencias) · Auditoría (consentimientos del titular) | Un cliente por servicio en `core/api/` y un dominio por servicio en `features/` |
| Servicio que el cliente web **no** consume y la app **sí** | **Notificaciones.** Los eventos del ecosistema llegan por *push* a este canal, no al portal | `core/api/notificaciones/` y `features/notificaciones/` |
| Servicios que la app **no** consume | Socios B2B (es un dominio del portal, no del asegurado) | Sin cliente ni dominio |

## 2. Lugar del cliente móvil en el modelo de despliegue

| Elemento | Valor | Consecuencia en el código |
|---|---|---|
| Nodo de ejecución | «executionEnvironment» Android en el «device» teléfono del asegurado; una instancia por dispositivo | Aplicación nativa; el estado sobrevive al cierre, a diferencia del portal |
| Acceso de clientes | HTTPS/TLS → Amazon API Gateway (tipo REST) | `BuildConfig.API_GATEWAY_URL`, definido por tipo de compilación |
| Identidad (HA-S01) | HTTPS/TLS (OIDC) → Amazon Cognito User Pool; un *user pool* con grupos por rol; el *authorizer* del gateway valida el JWT sin salto de red | `core/auth/sesion/` guarda y refresca los tokens; el JWT viaja en cada petición (`core/auth/interceptores/`) |
| Rol humano | **asegurado**, único rol de este canal | No hay menú por rol ni *guards* por rol; la app entera es del asegurado |
| Desbloqueo local | `BiometricPrompt` del sistema operativo; desbloquea la sesión guardada sin reautenticar contra el proveedor de identidad | `core/auth/biometria/` · HU-M1.1-02 |
| Aprovisionamiento del usuario | El registro con verificación de identidad es el disparador: HA-S01 referencia HU-M1.1-01 como el momento en que el usuario entra al *user pool* | `features/acceso/registro/` |
| Modo sin conexión | El teléfono pierde la red; el navegador del portal normalmente no | Room para borradores y caché de lectura, WorkManager para la cola de envío. La caché con datos sensibles —las pólizas de HU-M1.2-01— se guarda cifrada con una clave del Keystore de Android (`core/persistencia/cifrado/`) |
| Timeout de integración del gateway | 5 s en las rutas de cotización (ADR-02) | La app no cotiza: la cotización es un flujo web (E1). Sí aplica el límite de 30 s de HU-M1.1-01 para la verificación de identidad |

## 3. Atributos de calidad que el cliente móvil debe evidenciar

| Historia de arquitectura | Qué muestra la app | Dónde |
|---|---|---|
| HA-L02 · Consulta rápida con caché de lectura | Mis siniestros con «actualizado hoy 14:30 · disponible sin conexión», y el estado degradado cuando la fuente no responde | `features/siniestros/seguimiento/` (M9, M9c, M10c) |
| HA-D02 · Cobro sin pérdida ante caída de la pasarela | Siniestro paramétrico resuelto automáticamente, con parámetro, umbral, valor observado y clave de idempotencia | `features/siniestros/seguimiento/` (M10b) |
| HA-S01 · Autenticación y autorización por roles | 401 → sesión expirada; desbloqueo biométrico; cierre automático por inactividad a los cinco minutos | `core/auth/`, `features/acceso/` (M2, M2d) |
| RN-02 · Revocación efectiva ≤ 5 min | Revocación del consentimiento desde el perfil, con reintento | `features/auditoria/consentimientos/` (M8, M8b) |
| OBJ 12 · Internacionalización | Español e inglés en caliente; moneda, fecha y número según la región | `core/i18n/`, `res/values-en/`, `features/perfil/idioma/` (M19, M19b) |
| OBJ 13 · Accesibilidad | Contraste AA/AAA, foco visible, estados no solo por color, `contentDescription` en todo elemento no textual | `ui/theme/`, `shared/ui/` |

## 4. Estilos arquitectónicos y su reflejo en la app

| Estilo (wiki) | Reflejo aquí |
|---|---|
| **Layer-N** | `features` → `shared`/`core` → API Gateway. Nunca al revés |
| **Microservicios acotados** | Un paquete por servicio; cambiar un dominio no obliga a tocar los demás |
| **Publicador/suscriptor** | **Sí aplica a este canal**: los eventos de notificaciones llegan por *push*. La app los recibe, no los consulta en un bucle |
| **API Gateway «Facade»** | Un solo origen (`BuildConfig.API_GATEWAY_URL`); los prefijos por servicio se concentran en `core/api/` |

## 5. Ambientes

Los cuatro ambientes de [frameworks y ambientes](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/frameworks). En Angular se resuelven reemplazando `environment.ts`; aquí se resuelven con **tipos de compilación** y `buildConfigField`:

| Ambiente | Tipo de compilación | `API_GATEWAY_URL` |
|---|---|---|
| Local (docker-compose del repositorio de servicios) | `debug` | `http://10.0.2.2:8080` |
| Test / CI efímero por *pull request* | el pipeline de `.github/workflows/` | — |
| Staging (experimentos, encendido por ventana) | `staging`, con sufijo `.staging` en el `applicationId` | Pendiente de Terraform |
| Producción demo | `release` | Pendiente de Terraform |

**`10.0.2.2` no es un error.** Dentro del emulador, `localhost` es el propio dispositivo virtual; `10.0.2.2` es como el emulador alcanza el `localhost` del PC, donde corre el `docker-compose`. El cliente web usa `http://localhost:8080` para lo mismo.

## 6. Estructura del código

```
app/src/main/java/co/solventa/movil/
├── core/          servicios transversales (ver core/README.md)
├── features/      un paquete por dominio (ver features/README.md)
├── shared/        componentes y tipos reutilizables
└── ui/theme/      colores, tipografía y forma del sistema de diseño
```
