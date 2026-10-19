# MISW4501-202614-Grupo14-Solventa-Movil

Cliente móvil de **Solventa** (autoservicio del asegurado en movilidad: pólizas, siniestros, asistencia en sitio, notificaciones y renovación), construido en **Kotlin con Jetpack Compose**. Proyecto Final MISW4501 · Grupo 14 · Universidad de los Andes.

Este repositorio es uno de los cuatro componentes de la solución:

| Componente | Repositorio | Tecnología |
|---|---|---|
| Cliente web | [`…-FrontWeb`](https://github.com/nicolasjaramillocely99/MISW4501-202614-Grupo14-Solventa-FrontWeb) | Angular + TypeScript |
| **Cliente móvil (este)** | [`…-Movil`](https://github.com/CarlosARios/MISW4501-202614-Grupo14-Solventa-Movil) | Kotlin + Jetpack Compose |
| Microservicios de negocio |[`…-Backend`](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa) | Go sobre ECS Fargate |
| Infraestructura | `…-Infra` | Terraform sobre AWS |

En el [modelo de componentes](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Arquitectura_final#modelo-de-componentes) el cliente móvil se encuentra en **LAYER 4 · Clientes** y su comunicación es única con **LAYER 3 · Gateway** (Amazon API Gateway) por HTTPS; la identidad será administrada por **Amazon Cognito** (OIDC).

**Esta aplicación consume el servicio de notificaciones**, los eventos llegan por *push*.

---

## 1. Stack definido para el usuario móvil

Las versiones exactas se pueden validar en [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

| Necesidad | Herramienta | Versión instalada |
|---|---|---|
| Lenguaje | Kotlin | 2.2.10 |
| Interfaz | Jetpack Compose (BOM) + Material 3 | 2026.02.01 |
| Build | Android Gradle Plugin sobre Gradle | 9.4.1 / 9.6 |
| Navegación | Navigation Compose | 2.10.2 |
| Inyección de dependencias | Hilt (Dagger) con KSP | 2.60.1 |
| Cliente HTTP | Retrofit + OkHttp + kotlinx.serialization | 3.0.0 / 4.12.0 / 1.9.0 |
| Base de datos local | Room | 2.8.5 |
| Trabajo en segundo plano | WorkManager | 2.12.0 |
| Cámara (documento y evidencia) | CameraX | 1.6.2 |
| Biometría | AndroidX Biometric | 1.1.0 |
| Pruebas unitarias | JUnit4 + MockK + Turbine + coroutines-test | 4.13.2 / 1.13.13 / 1.2.0 / 1.10.2 |
| Pruebas de interfaz | Compose UI Test + Espresso | — / 3.7.0 |
| Cobertura | Kover (umbral del proyecto: ≥ 70 %) | 0.9.11 |
| Análisis estático | Android Lint | incluido en AGP |
| Integración y despliegue continuos | GitHub Actions: *pull request*, *push*, nocturna y publicación | — |
| Dependencias vulnerables | Dependabot | — |

Dispositivos soportados: **`minSdk 26`** (Android 8.0) y `targetSdk 35`. El `compileSdk` es 37. Lienzo de referencia del prototipo **390 × 844**. Idiomas: **es-CO** (predeterminado) y **en-US**.

---

## 2. How-to

### 2.1 Requisitos

| Qué | Detalle |
|---|---|
| **Android Studio** | Una versión que soporte **AGP 9.4.1**. Si al abrir el proyecto Studio propone *actualizar* el Android Gradle Plugin, **no debe aceptarse**: significa que esa instalación de Studio es anterior a la que el proyecto necesita. La salida es actualizar Studio (*Help → Check for Updates*) |
| **Git** | Para clonar el repositorio |
| **JDK** | Ninguno aparte. El proyecto usa el **JBR que viene dentro de Android Studio**. Solo quien compile desde la terminal sin Studio necesita un **JDK 21**, que es el que usa el pipeline |
| **SDK de Android** | Plataforma **API 37** y las *Android SDK Build-Tools*. No hace falta instalarlos a mano: al abrir el proyecto, Studio detecta lo que falta y ofrece descargarlo |
| **Dispositivo** | Un emulador o un teléfono con **Android 8.0 (API 26) o superior**. Para el emulador, una imagen de API 35 o 36 es la más parecida a la del pipeline |
| **Espacio en disco** | Unos 10 GB entre el SDK, el emulador y la caché de Gradle |

No hace falta instalar Gradle: el repositorio trae el *wrapper* (`gradlew`), que descarga la versión correcta la primera vez.

### 2.2 Abrir el proyecto por primera vez

```bash
git clone https://github.com/CarlosARios/MISW4501-202614-Grupo14-Solventa-Movil.git
cd MISW4501-202614-Grupo14-Solventa-Movil
```

El repositorio es público: no hace falta credencial para clonarlo. Para empujar cambios sí: acceso de colaborador y un *token* personal o una clave SSH.

1. En Android Studio: **File → Open** y seleccione la **carpeta raíz** del repositorio, la que contiene `settings.gradle.kts`. No use *Import project* ni abra la carpeta `app/`.
2. Si aparece el aviso de **Trust Project**, acéptelo: sin eso Gradle no se ejecuta.
3. Espere el *sync* de Gradle. **La primera vez tarda varios minutos**, porque descarga el *wrapper* y todas las dependencias.
4. Si Studio avisa que falta algo del SDK, acepte la descarga que propone y deje que vuelva a sincronizar.
5. Cuando el *sync* termine sin errores, seleccione un dispositivo en la barra superior y ejecute **Run** (`Mayús + F10`).

> **El aviso amarillo *«Gradle files have changed since last project sync»*.** Aparece cada vez que alguien modifica `build.gradle.kts`, `libs.versions.toml` o `gradle.properties` — por ejemplo después de un `git pull`. Se resuelve con **Sync Now**, o con **File → Sync Project with Gradle Files** (`Ctrl + Mayús + O`). Compilar sin sincronizar funciona, pero el editor subraya en rojo clases que sí existen, porque su índice quedó viejo.

> **`local.properties` no está en el repositorio** y no debe estarlo: Studio lo genera con la ruta del SDK de cada máquina, que no es la misma para todos.

### 2.3 Compilar desde la terminal

Gradle necesita saber qué JDK usar. Lo más simple es reutilizar el que ya trae Android Studio, sin instalar nada.

**La ruta no es la misma para todos.** Depende del sistema operativo y, en Windows, de cuántas veces se haya instalado Studio: un segundo instalador deja carpetas como `Android Studio1` o `Android Studio 2026.2.1`. Por eso conviene detectarla en vez de escribirla.

**Windows (PowerShell)** — detecta la carpeta, la asigna y comprueba que sirve:

```powershell
$studio = Get-ChildItem "C:\Program Files\Android", "$env:LOCALAPPDATA\Programs" -Directory -Filter "Android Studio*" -ErrorAction SilentlyContinue | Where-Object { Test-Path "$($_.FullName)\jbr\bin\java.exe" } | Select-Object -First 1
$env:JAVA_HOME = Join-Path $studio.FullName "jbr"
& "$env:JAVA_HOME\bin\java.exe" -version
```

**macOS y Linux (bash o zsh)**:

```bash
for d in "/Applications/Android Studio.app/Contents/jbr/Contents/Home" "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home" /opt/android-studio/jbr "$HOME/android-studio/jbr"; do
  [ -x "$d/bin/java" ] && export JAVA_HOME="$d" && break
done
"$JAVA_HOME/bin/java" -version
```

Si el bloque no encuentra nada, la ruta exacta está dentro de Studio: **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**. Al situar el cursor sobre la entrada, Studio muestra la ruta completa.

Esa asignación **dura solo lo que dure esa terminal**. Para dejarla fija:

- **Windows:** `setx JAVA_HOME "<la ruta>"`. Conviene conocer una trampa del sistema: `setx` escribe la variable en el registro, pero solo la heredan los procesos nuevos **cuyo proceso padre también haya arrancado después**. Cerrar y reabrir la pestaña de la terminal no basta si quien la abre es una aplicación que ya estaba en ejecución; hay que cerrar esa aplicación o cerrar la sesión de Windows.
- **macOS y Linux:** agregar la línea `export JAVA_HOME=...` a `~/.zshrc` o `~/.bashrc`.

**Alternativa sin Android Studio.** Instalar un **JDK 21** —por ejemplo [Temurin](https://adoptium.net/)— y apuntar `JAVA_HOME` ahí; es exactamente lo que hace el pipeline. En ese caso también hay que resolver el SDK de Android aparte: definir `ANDROID_HOME` o crear un `local.properties` con `sdk.dir=<ruta del SDK>`.

### 2.4 Comandos

En Windows el ejecutable es `.\gradlew.bat`; en macOS y Linux, `./gradlew`.

| Comando | Qué hace |
|---|---|
| `./gradlew assembleDebug` | Compila la variante local |
| `./gradlew installDebug` | Instala en el emulador o dispositivo conectado |
| `./gradlew lintDebug` | Android Lint |
| `./gradlew testDebugUnitTest` | Pruebas unitarias |
| `./gradlew koverLogDebug` | Imprime el porcentaje de cobertura |
| `./gradlew koverHtmlReportDebug` | Reporte navegable de cobertura |
| `./gradlew testDebugUnitTest koverVerifyDebug` | **La compuerta que corre el pipeline.** Debe pasar antes de abrir el *pull request* |
| `./gradlew assembleStaging` | Compila la variante de experimentos |
| `./gradlew connectedDebugAndroidTest` | Pruebas instrumentadas (requiere emulador encendido) |

### 2.5 A qué apunta la aplicación

El ambiente local apunta a **`http://10.0.2.2:8080`**, que es como el emulador alcanza el `localhost` del PC donde corre el `docker-compose` del repositorio de servicios. Dentro del emulador, `localhost` es el propio dispositivo virtual, no la máquina anfitriona.


### 2.6 Si algo falla

| Síntoma | Causa y salida |
|---|---|
| `ERROR: JAVA_HOME is set to an invalid directory` | La ruta no contiene `bin/java`. Conviene usar el bloque de detección de 2.3 en lugar de escribirla a mano |
| `SDK location not found` | Falta `local.properties`. Se genera al abrir el proyecto una vez en Studio, o se crea a mano con `sdk.dir=<ruta>` |
| Studio propone *Upgrade Android Gradle Plugin* | Esa instalación de Studio es anterior al AGP del proyecto. Hay que actualizar Studio, **no** el AGP |
| `Dependency requires compileSdk 38` (o superior) tras actualizar una librería | Subir `compileSdk` en `app/build.gradle.kts`. Eso no cambia `targetSdk`, que sigue en 35 por decisión del equipo |
| El editor subraya en rojo clases que sí existen | Falta sincronizar: `Ctrl + Mayús + O` |
| `Using kotlin.sourceSets DSL is not allowed with built-in Kotlin` | Falta `android.disallowKotlinSourceSets=false` en `gradle.properties`. AGP 9 trae Kotlin integrado y KSP todavía registra sus fuentes por la vía antigua |
| El build se cuelga o falla sin razón clara | `./gradlew --stop` y compilar de nuevo; si persiste, `./gradlew clean` |

---

## 3. Estructura del repositorio

```
.
├── .github/
│   ├── workflows/
│   │   ├── ci.yml                      # PR a develop/main: lint → build → unitarias; publica el APK
│   │   ├── pruebas-interfaz-nocturnas.yml # Compose UI Test sobre emulador, cada noche
│   │   └── publicar.yml                # Release con el APK al etiquetar v*.*.*
│   ├── dependabot.yml
│   └── PULL_REQUEST_TEMPLATE.md
├── AGENTS.md                           # Reglas para los agentes de IA que trabajen aquí
├── docs/
│   ├── arquitectura.md                 # Cómo este cliente respeta los modelos de arquitectura
│   ├── pantallas.md                    # Trazabilidad Figma ↔ paquete ↔ HU ↔ microservicio
│   └── flujo-de-trabajo.md             # Ramas, commits, pull requests y Definition of Done
├── app/
│   ├── schemas/                        # Esquemas de Room exportados (versionados, van al commit)
│   └── src/
│       ├── main/
│       │   ├── java/co/solventa/movil/
│       │   │   ├── core/               # Singletons: auth (Cognito y biometría), api (un cliente por
│       │   │   │                       #   microservicio), persistencia (Room), sincronización
│       │   │   │                       #   (WorkManager), i18n, navegación, errores, di (Hilt)
│       │   │   ├── features/           # Un paquete por dominio de negocio
│       │   │   │   ├── acceso/         # registro con KYC (HU-M1.1-01) e ingreso biométrico (HU-M1.1-02)
│       │   │   │   ├── inicio/         # M3
│       │   │   │   ├── originacion/    # pólizas y renovaciones
│       │   │   │   ├── siniestros/     # radicación, seguimiento y asistencias
│       │   │   │   ├── notificaciones/ # centro de notificaciones y preferencias
│       │   │   │   ├── auditoria/      # consentimientos del titular
│       │   │   │   └── perfil/         # M19 y cambio de idioma
│       │   │   ├── shared/             # Componentes de Compose, modelos y utilidades reutilizables
│       │   │   ├── ui/theme/           # Colores, tipografía y forma del sistema de diseño
│       │   │   ├── MainActivity.kt
│       │   │   └── SolventaApp.kt      # @HiltAndroidApp — contenedor raíz de dependencias
│       │   ├── res/values/             # strings.xml en es-CO (predeterminado)
│       │   ├── res/values-en/          # strings.xml en en-US
│       │   └── AndroidManifest.xml
│       ├── test/                       # Pruebas unitarias (JUnit4, MockK, Turbine)
│       └── androidTest/                # Pruebas instrumentadas (Compose UI Test, Espresso)
├── build.gradle.kts · settings.gradle.kts · gradle.properties
└── gradle/libs.versions.toml           # Catálogo único de versiones
```

Cada carpeta principal tiene un `README.md` que explica qué va en ella y cómo se nombra. Las carpetas vacías llevan un `.gitkeep` hasta que entre el primer archivo.

---

## 4. ¿Dónde ubicar la historia de usuario móvil asignada?

1. Buscar el ID de la historia (`HU-M1.1-01`, `M2.4-01`, …) en [`docs/pantallas.md`](docs/pantallas.md). La tabla indica el **paquete**, el **frame de Figma** y la prioridad.
2. Crear la rama desde `develop`: `feature/<ID>-<descripción corta>`.
3. Trabajar dentro de `features/<dominio>/<flujo>/`. El `README.md` de `features` lista sus pantallas y sus historias.
4. Las llamadas HTTP salen siempre por el cliente del microservicio en `core/api/<servicio>/`. Si la historia necesita un componente visual reutilizable, va en `shared/ui/`.
5. Los textos van en `res/values/strings.xml` y `res/values-en/strings.xml`; los colores y la tipografía salen de `ui/theme/`.
6. Antes del *pull request*: `./gradlew lintDebug assembleDebug testDebugUnitTest koverVerifyDebug` en verde, con cobertura ≥ 70 % sobre lo modificado.

Correspondencia dominio ↔ microservicio ↔ épica:

| Paquete en `features/` | Microservicio (modelo de componentes) | Épica |
|---|---|---|
| `originacion/` | Servicio de originación | Móvil 1 · Pólizas y renovación |
| `siniestros/` | Servicio de siniestros | Móvil 2 · Siniestros y asistencia |
| `notificaciones/` | Servicio de notificaciones | Móvil 3 · Notificaciones y renovación |
| `auditoria/` | Servicio de auditoría y consentimientos | Transversal · RN-02 |
| `acceso/`, `inicio/`, `perfil/` | Amazon Cognito (HA-S01) | Móvil 1 · Identidad del asegurado |

El dominio de socios B2B no existe en este canal: es del portal.

---

## 5. Convenciones

| Elemento | Convención | Ejemplo |
|---|---|---|
| Paquetes | minúsculas, en español, sin tildes ni guiones | `features.siniestros.seguimiento` |
| Pantalla (composable enrutado) | sufijo `Pantalla`, archivo con el mismo nombre | `MisSiniestrosPantalla.kt` |
| Composable no enrutado | nombre descriptivo sin sufijo | `InsigniaEstado.kt` |
| ViewModel | sufijo `ViewModel`, con `@HiltViewModel` | `MisSiniestrosViewModel` |
| Estado de pantalla | un `data class` con sufijo `UiState`, expuesto como `StateFlow` | `MisSiniestrosUiState` |
| Repositorio | interfaz `…Repositorio` y su implementación `…RepositorioImpl` | `SiniestrosRepositorio` |
| Cliente HTTP de un servicio | sufijo `Api`, una interfaz de Retrofit por microservicio | `SiniestrosApi` |
| DTO de red | sufijo `Dto`, con `@Serializable` y `@SerialName` explícito en cada campo | `SiniestroDto` |
| Entidad de Room | sufijo `Entidad` | `BorradorKycEntidad` |
| Rutas de navegación | objetos de `core/navegacion/`, nunca cadenas literales en la pantalla | `Ruta.MisSiniestros` |
| Claves de texto | `<dominio>_<pantalla>_<texto>` en `strings.xml` | `siniestros_lista_titulo` |
| Prueba unitaria | mismo nombre con sufijo `Test`, en `src/test/` | `MisSiniestrosViewModelTest.kt` |
| Ramas | `feature/`, `fix/` o `chore/` más el ID y una descripción corta, creadas desde `develop` | `feature/HU-M1.1-01-registro-kyc` |
| Commits | Conventional Commits con el dominio como alcance | `feat(siniestros): lista con estado sin conexión` |

Reglas de dependencia entre capas: `features` → `shared` y `core`; `shared` no depende de nadie; `core` no importa de `features`. Un dominio nunca importa a otro dominio: lo que comparten va a `shared`.

**`@SerialName` explícito en todos los campos de los DTO.** El nombre que viaja por la red lo fija el contrato del API, no el nombre de la propiedad en Kotlin. Escribirlo siempre convierte un cambio de contrato en una línea y no en una búsqueda por todo el proyecto.

Toda pantalla implementa los **estados** que define el prototipo (vacío, error, sin conexión, degradado, confirmación) dentro del mismo composable; las variantes `b`, `c` de Figma no son rutas distintas.

---

## 6. Ambientes

| Ambiente | Tipo de compilación | `API_GATEWAY_URL` | Uso |
|---|---|---|---|
| Local | `debug` | `http://10.0.2.2:8080` | Desarrollo diario contra el Docker Compose del repositorio de servicios |
| Test / CI | — (sin despliegue) | — | Lint, build y unitarias en cada *pull request* |
| Staging | `staging` (sufijo `.staging` en el `applicationId`) | `URL_API_GATEWAY` | Experimentos de arquitectura; se enciende solo en su ventana |
| Producción (demo) | `release` | `URL_API_GATEWAY` | Entrega evaluable |

Todos los valores se leen desde `BuildConfig`, nunca escritos en una pantalla.

### Identidad: el *user pool* permanente

El *user pool* de Cognito que usan los canales humanos es **permanente** y está configurado en el propio
`build.gradle.kts`, en las tres variantes. Lo crea y administra `infra/terraform/bootstrap/identity` del repositorio
de servicios, se aplicó una sola vez y no se destruye con las ventanas de *staging*.

| Valor | |
|---|---|
| Región | `us-east-2` |
| *User pool* | `us-east-2_EWwg8NL2C` |
| Cliente móvil | `6dri40bi3sk74cr0jb5djq43c0` |

No son secretos: un *user pool* se identifica por ellos y el cliente móvil es público, sin secreto de cliente. El
*pool* tampoco contiene datos personales; sus usuarios son cuatro cuentas de prueba `@example.com`, una por rol, y la
contraseña la custodia el equipo fuera del repositorio.

**La variante `debug` autentica contra ese mismo *pool*.** Es lo que permite comprobar en un dispositivo el ingreso
real, la renovación silenciosa y el vencimiento de la sesión sin tener que construir y firmar la versión de
producción.

### La URL del API Gateway entra al construir

A diferencia del *user pool*, la plataforma es efímera: cada ventana de *staging* levanta su propio gateway con un
dominio distinto. Por eso la URL no está escrita en el repositorio, sino que se toma de la variable de entorno
`URL_API_GATEWAY` en el momento de construir:

```powershell
$env:URL_API_GATEWAY = "https://abc123.execute-api.us-east-2.amazonaws.com"
```

```powershell
.\gradlew :app:assembleRelease
```

Sin la variable, el valor queda en `PENDIENTE` y la aplicación arranca igual: el ingreso funciona, porque Cognito es
permanente, y lo que falla son las llamadas al API, que es exactamente lo que ocurre cuando no hay ninguna ventana
abierta. Cambiar de ventana es volver a construir, sin tocar código ni abrir un *pull request*.

---

## 7. Pruebas

Según la [estrategia de pruebas v1.2](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Estrategia-de-Pruebas-v-1.2):

| Nivel | Herramienta | Dónde | Cuándo corre |
|---|---|---|---|
| Unitarias (OBJ 02, ≥ 70 %) | JUnit4 + MockK + Turbine | `app/src/test/` | Cada *pull request* |
| Base de datos local | `room-testing` sobre migraciones y DAO | `app/src/test/` | Cada *pull request* |
| Interfaz y extremo a extremo (OBJ 06) | Compose UI Test | `app/src/androidTest/` | Ejecución nocturna sobre emulador, y bajo demanda |
| Accesibilidad (OBJ 13) | Descripciones de accesibilidad verificadas con Compose UI Test | `app/src/androidTest/` | Con las instrumentadas |
| Internacionalización (OBJ 12) | Revisión de `values/` y `values-en/` y casos manuales | — | Por sprint |

Regla de integración: ningún cambio entra a `develop` ni a `main` con pruebas fallidas.

### Qué mide el 70 %

La compuerta de Kover se aplica sobre **la lógica**: `ViewModel`, repositorios, mapeadores, casos de uso y utilidades. Quedan fuera de la medición, porque no se prueban con unitarias:

- La capa de interfaz: las funciones `@Composable` y `@Preview`, los paquetes `ui/theme` y `shared/ui` completos, `MainActivity` y `SolventaApp`. Se verifica en `androidTest/` con Compose UI Test.
- Los archivos de vistas previas, que terminan en `Preview.kt`. Son herramienta de diseño, no código que corra en el dispositivo.
- Las declaraciones de rutas (`core/navegacion/Ruta`): objetos sin comportamiento.
- El código que generan Hilt, Room y Compose, y `BuildConfig`.

Se excluyen **paquetes completos** y no solo las funciones `@Composable` porque un archivo de interfaz deja además constantes, enumeraciones y lambdas sueltas que no son lógica de negocio y empujan la medición hacia abajo sin decir nada útil.

**El grafo de navegación sí se mide**, aunque hoy no tenga pruebas. Es deliberado: ahí viven decisiones de recorrido que pueden fallar, y dejarlas contando es el recordatorio de que faltan sus pruebas instrumentadas.

La consecuencia práctica: **poner lógica dentro de un `@Composable` la saca de la medición**. Es una razón más para que la decisión viva en el `ViewModel`, que es además la convención del repositorio.

Mientras no exista lógica que medir, `koverVerifyDebug` reporta `No sources` y pasa. En cuanto entre la primera clase con lógica sin pruebas, falla: está verificado.

---

## 8. Referencias

- [Wiki del proyecto](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki)
- [Modelos de arquitectura y patrones](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Arquitectura_final)
- [Decisiones de arquitectura](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Recopilaci%C3%B3n-Decisiones-de-Arquitectura)
- [Definición de frameworks y ambientes](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/frameworks)
- [Estrategia de pruebas v1.2](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Estrategia-de-Pruebas-v-1.2)
- [Sistema de diseño](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Sistema-de-Dise%C3%B1o_Solventa) y [prototipo en Figma](https://www.figma.com/design/ZBPl5l9oXTPlfiOHrzCM2M/Solventa-%E2%80%94-Prototipo-S7-%E2%80%94-Pantallas)
- [Plan de trabajo por sprints](https://github.com/leonelparrales22/MISW4501-202614-Grupo14-Solventa/wiki/Plan-de-trabajo-por-sprints)
- [Flujo de trabajo del cliente web](https://github.com/nicolasjaramillocely99/MISW4501-202614-Grupo14-Solventa-FrontWeb/blob/main/docs/flujo-de-trabajo.md), del que este repositorio copia sus reglas

## Equipo

| Nombre | GitHub |
|---|---|
| Carlos A. Ríos P. | CarlosARios |
| Cristhian S. Contreras B. | criscont17 |
| Leonel A. Parrales M. | leonelparrales22 |
| Maycol S. A. Niño | maycolan |
| Nicolás J. Jaramillo C. | nicolasjaramillocely99 |
