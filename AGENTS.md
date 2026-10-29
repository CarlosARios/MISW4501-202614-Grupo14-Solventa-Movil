# Guía para agentes de IA — Solventa Móvil

Reglas de operación, arquitectura y calidad que **todo agente de IA** —Claude Code, OpenAI Codex, Gemini CLI, Cursor, Copilot, Windsurf— debe seguir al trabajar en este repositorio.

Es el equivalente del [`AGENTS.md` del repositorio de servicios](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/blob/develop/AGENTS.md). **Está escrito en español, igual que el resto de la documentación de este repositorio**, y eso es deliberado: aquí el código, los paquetes y las claves de texto también están en español, así que un archivo de reglas en inglés obligaría a traducir cada ejemplo. La convención de idioma es **por repositorio**: Go en inglés, Angular y Kotlin en español.

---

## 1. Contexto del proyecto y arquitectura

Solventa es una plataforma *insurtech* basada en Open Finance. Este repositorio es el **cliente móvil**: la aplicación Android del asegurado, en Kotlin con Jetpack Compose.

En el [modelo de componentes](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Arquitectura_final) vive en **LAYER 4 · Clientes** y solo habla con **LAYER 3 · Gateway** por HTTPS. La identidad la emite Amazon Cognito (OIDC).

La documentación de diseño vive en la wiki del proyecto; este repositorio documenta lo que cambia junto con el código. **Leer [`docs/arquitectura.md`](docs/arquitectura.md) y el `README.md` de la capa antes de modificarla.**

Servicios que la aplicación consume, cada uno con su cliente en `core/api/`: **originación** (pólizas y renovación) · **siniestros** (radicación, consulta, asistencias) · **notificaciones** (token del dispositivo y preferencias) · **auditoría** (consentimientos del titular). **No consume socios B2B**: es un dominio del portal.

---

## 2. Reglas de arquitectura no negociables

1. **Dirección de las dependencias.** `features` → `shared` y `core`. **`core` nunca importa de `features`** y **un dominio de `features` nunca importa a otro**. Lo que dos dominios comparten sube a `shared`.
   - Consecuencia práctica ya aplicada: el `NavHost` vive en el paquete raíz y no en `core/navegacion`, porque conoce todos los dominios. En `core/navegacion` queda solo el contrato `Ruta`.
2. **Una sola salida HTTP.** Toda llamada sale por el cliente del microservicio en `core/api/`. **Ninguna pantalla ni ningún `ViewModel` construye una URL** ni conoce direcciones de microservicios, bases de datos ni mensajería.
3. **La lógica vive en el `ViewModel`, no en el composable.** Un `@Composable` dibuja y delega. Además de ser la regla de diseño, la capa de interfaz está excluida de la medición de cobertura: **lógica dentro de un composable es lógica sin probar**.
4. **Las variantes del prototipo no son rutas.** M1f es un estado de M1a; M2b y M2c son estados de M2a. Los estados —vacío, error, sin conexión, degradado, confirmación— se resuelven dentro del mismo composable.
5. **Lo que debe sobrevivir sin conexión** se escribe en Room y se encola en WorkManager. No se pierde al cerrar la aplicación.

---

## 3. Convenciones de lenguaje y estilo

- **Todo en español, sin tildes en identificadores**: paquetes, clases, funciones, claves de texto, nombres de prueba, comentarios y documentación. `features.siniestros.seguimiento`, no `features.claims.tracking`.
- **La única excepción son los nombres que impone una biblioteca o el sistema operativo** (`onCreate`, `MainActivity`, `@Composable`) y los identificadores de Android (`values-en`, `strings.xml`).
- **El registro de la documentación es impersonal o de usted, nunca tuteando** al lector. El texto de producto que ve el usuario sí tutea, porque así está el prototipo.
- Nomenclatura completa en la sección *Convenciones* del [`README.md`](README.md). Lo esencial:

| Elemento | Convención |
|---|---|
| Pantalla enrutada | sufijo `Pantalla` |
| `ViewModel` | sufijo `ViewModel`, con `@HiltViewModel` |
| Estado de pantalla | `data class` con sufijo `UiState`, expuesto como `StateFlow` |
| Repositorio | interfaz `…Repositorio` + implementación `…RepositorioImpl` |
| Cliente HTTP | sufijo `Api`, una interfaz de Retrofit por microservicio |
| DTO de red | sufijo `Dto`, con `@Serializable` y **`@SerialName` explícito en cada campo** |
| Entidad de Room | sufijo `Entidad` |
| Clave de texto | `<dominio>_<pantalla>_<texto>` |

- **Ningún texto literal en el código de Compose.** Toda cadena visible entra en `res/values/strings.xml` **y** en `res/values-en/strings.xml`. Una clave que exista en uno y no en el otro es un error.
- **Nunca escribir credenciales, claves de API ni secretos en el código.** Los valores por ambiente se leen de `BuildConfig`. `local.properties` y cualquier *keystore* no se versionan.

---

## 4. Contratos

El contrato del API todavía no está publicado y **ningún servicio ha sido definido como dueño del endpoint de registro**. Mientras tanto:

- Los repositorios se definen como interfaz y se implementan con un doble claramente marcado (fue el caso de `RegistroKycRepositorioSimulado`, que reemplazó `AnalizadorDeNitidez`). El doble y sus pruebas se borran juntos cuando llega la implementación real.
- **`@SerialName` explícito en todos los campos de los DTO.** El nombre que viaja por la red lo fija el contrato, no la propiedad en Kotlin: así un cambio de contrato es una línea y no una búsqueda por todo el proyecto.

---

## 5. Definition of Done

Antes de dar una tarea por terminada, el agente **debe ejecutar y verificar localmente**:

```bash
./gradlew lintDebug                              # 1. análisis estático
./gradlew testDebugUnitTest koverVerifyDebug     # 2. pruebas y cobertura ≥ 70 %
./gradlew assembleDebug                          # 3. build
```

Si alguna falla, se corrige antes de terminar. **Nunca reportar como verificado algo que no se ejecutó**: hay que decir qué comandos no se pudieron correr y por qué.

Las instrumentadas (`./gradlew :app:connectedDebugAndroidTest`) necesitan un emulador encendido y corren cada noche en el pipeline. Si la tarea toca una pantalla, hay que correrlas.

El *Definition of Done* funcional completo —estados del prototipo, textos en los dos idiomas, accesibilidad, Room y WorkManager— está en [`docs/flujo-de-trabajo.md`](docs/flujo-de-trabajo.md).

> **La cobertura no tiene excepción en este repositorio.** El de servicios la tiene suspendida mientras arranca; aquí la compuerta está activa desde el primer día y la cobertura actual es 87,7 %. No bajar el umbral para que pase un cambio.

---

## 6. Documentación viva

- Si una pantalla cambia de paquete o de ruta, se actualiza su fila en [`docs/pantallas.md`](docs/pantallas.md) **en el mismo pull request**. Esa tabla es el contrato entre el prototipo y el código.
- Si se agrega, renombra o divide un paquete, se actualiza la tabla del `README.md` de esa capa.
- **Las decisiones técnicas y de arquitectura las toma el equipo, no el agente.** Hay que preguntarle a la persona antes de decidir, y no crear registros de decisión por iniciativa propia.
- **Verificar la versión de toda dependencia nueva contra el repositorio real** —Google Maven para `androidx.*`, Maven Central para el resto— en lugar de suponerla. Las versiones de AGP y Kotlin de este proyecto son posteriores al conocimiento de la mayoría de los modelos, y suponer una versión produce un build roto con un error que no apunta a la causa.

---

## 7. Flujo de git y commits

- **Nunca confirmar ni empujar a `main` ni a `develop`.** Esto no se negocia con nadie: el *ruleset* del repositorio lo impide y la regla existe aunque el *ruleset* se caiga.
- **Quién confirma y empuja en una rama de trabajo se acuerda con cada persona, no se asume.** Hay quien prefiere revisar el *diff* y confirmar a mano, y quien prefiere que el agente lo haga. El agente **pregunta antes de su primera confirmación en la sesión** y respeta la respuesta durante el resto.
  - **Mientras no haya respuesta, el comportamiento por omisión es no confirmar**: prepara los archivos, verifica el *Definition of Done* y reporta qué quedó listo y con qué comando subirlo.
  - Una autorización vale para esa sesión y ese trabajo; no se extiende a la siguiente.
- **Fusionar un *pull request* nunca lo hace el agente**, lo haya autorizado quien sea.
- Ramas: `feature/<ID>-<descripcion-corta>`, `fix/<ID>-<descripcion-corta>`, `chore/<descripcion-corta>`. Ejemplo: `feature/HU-M1.1-01-registro-kyc`.
- **Conventional Commits** con el dominio de negocio como alcance, en español, sin punto final:
  - `feat(acceso): captura del documento de identidad`
  - `fix(siniestros): …` · `test(notificaciones): …` · `docs: …` · `chore: …`
  - El alcance es el paquete de `features`, no la capa: `feat(acceso)`, nunca `feat(viewmodel)`.

### 7.1 Pull requests y estrategia de fusión

```
feature/* · fix/* · chore/*  ──(PR, squash)──▶  develop  ──(PR, merge commit)──▶  main
```

| De → A | Estrategia | Por qué |
|---|---|---|
| `feature/*`, `fix/*`, `chore/*` → `develop` | **Squash** | Un commit limpio por historia; los intermedios no llegan a `develop` |
| `develop` → `main` | **Merge commit** | Conserva la historia de `develop` y evita que las dos ramas diverjan |
| Hotfix urgente | Desde `main` → PR a `main` con **squash**, y después se devuelve a `develop` | Caso excepcional |

1. Todo pull request de trabajo apunta a **`develop`**. Solo los de entrega apuntan a `main`.
2. **Un pull request = una historia o tarea.**
3. **El título del pull request es el mensaje del commit** tras el *squash*: Conventional Commits con el ID de la historia. Ejemplo: `feat(acceso): registro con verificación de identidad (HU-M1.1-01)`.
4. Antes de abrirlo: pasar el *Definition of Done* y actualizar la documentación afectada.
5. Mantener la rama al día con `develop`. Nunca reescribir la historia de una rama compartida.
6. Para fusionar: `CI · Cliente móvil` en verde y la revisión de un compañero.
7. Al fusionar se borra la rama de la historia. `develop` y `main` no se borran: el *ruleset* lo impide.
8. Usar la plantilla de [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md).

**Para agentes:** abrir un *pull request* solo si la persona lo pide, y nunca fusionarlo.

---

## 8. Guardarraíles y antipatrones prohibidos

- **Sin ampliación de alcance.** Modificar solo el dominio de la tarea asignada. No tocar otros dominios salvo que la tarea sea explícitamente sobre `shared` o `core`.
- **Sin simulaciones en código de producción**, salvo los dobles marcados como tales mientras no exista el contrato (sección 4). Los dobles de prueba viven en `src/test/`.
- **No bajar el umbral de cobertura ni excluir una clase** para que pase un cambio. Las exclusiones de Kover están justificadas una por una en el `README.md`; agregar una es una decisión, no un atajo.
- **No meter lógica en un `@Composable`** para esquivar la medición de cobertura.
- **No inventar datos de demostración.** Los del prototipo son los mismos que los de la web: los cuatro siniestros, la renovación de AU-2025-118204 y el pago paramétrico de $ 180.000 COP. Están en `docs/pantallas.md`.
- **Preservar código y contexto.** No borrar comentarios, anotaciones ni código ajeno al cambio.
- **Mantener vivas estas reglas.** Si se detecta un patrón de error repetido o una alucinación común, se agrega aquí la restricción correspondiente.
