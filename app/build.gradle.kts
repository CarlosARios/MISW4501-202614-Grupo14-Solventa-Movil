plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
}

/**
 * Ruta del almacen de claves de publicacion, si esta disponible.
 *
 * Solo el pipeline la define, a partir del secreto del repositorio. En una
 * maquina sin almacen esto queda nulo y `assembleRelease` produce un APK sin
 * firmar, igual que antes: nadie del equipo se bloquea por no tener la clave.
 */
val rutaDelAlmacen: String? = System.getenv("RUTA_ALMACEN_DE_CLAVES")

/**
 * Identificadores del user pool permanente de Cognito (`solventa-users`, en
 * us-east-2). No son secretos: un user pool se identifica por ellos y el
 * cliente movil es publico, sin secreto de cliente. Los crea y administra
 * `infra/terraform/bootstrap/identity` del repositorio de servicios, y se
 * aplicaron una sola vez, asi que no cambian con cada ventana de staging.
 */
val regionDeCognito = "us-east-2"
val poolDeCognito = "us-east-2_EWwg8NL2C"
val clienteMovilDeCognito = "6dri40bi3sk74cr0jb5djq43c0"

/**
 * URL del API Gateway del ambiente contra el que se construye.
 *
 * A diferencia del user pool, la plataforma es efimera: cada ventana de
 * staging levanta su propio gateway con un dominio distinto. Por eso el valor
 * entra por variable de entorno al construir, y no escrito en este archivo:
 * cambiar de ventana es construir otra vez, sin commit ni pull request.
 *
 *     $env:URL_API_GATEWAY = "https://abc123.execute-api.us-east-2.amazonaws.com"
 *
 * Sin la variable queda en PENDIENTE y la aplicacion arranca igual: el
 * ingreso funciona, porque Cognito si es permanente, y lo que falla son las
 * llamadas al API, que es exactamente lo que ocurre sin una ventana abierta.
 */
val urlDelApiGateway: String = System.getenv("URL_API_GATEWAY") ?: "PENDIENTE"

/**
 * URL del servicio local, el de la variante de depuracion.
 *
 * Por omision 10.0.2.2, que es como el emulador alcanza el localhost del PC.
 * Un telefono conectado por USB no entiende esa direccion: ahi se abre el
 * puente con `adb reverse tcp:8080 tcp:8080`, que hace que el localhost del
 * telefono llegue al del PC, y se construye apuntando a el.
 *
 *     $env:URL_API_LOCAL = "http://localhost:8080"
 */
val urlDelServicioLocal: String = System.getenv("URL_API_LOCAL") ?: "http://10.0.2.2:8080"

android {
    namespace = "co.solventa.movil"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "co.solventa.movil"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Contrasena de los usuarios de prueba del user pool permanente, para
        // la unica prueba que consulta Cognito de verdad
        // (RenovacionContraElPoolTest). Entra por el entorno y no por este
        // archivo: en el repositorio no hay ninguna credencial. Sin la
        // variable, esa prueba se omite en lugar de fallar, asi que el
        // pipeline sigue en verde sin conocerla.
        System.getenv("CLAVE_POOL_SOLVENTA")?.let {
            testInstrumentationRunnerArguments["claveDePrueba"] = it
        }
    }

    signingConfigs {
        if (rutaDelAlmacen != null) {
            create("publicacion") {
                storeFile = file(rutaDelAlmacen)
                storePassword = System.getenv("CLAVE_DEL_ALMACEN")
                keyAlias = System.getenv("ALIAS_DE_LA_CLAVE")
                keyPassword = System.getenv("CLAVE_DE_LA_CLAVE")
            }
        }
    }

    buildTypes {
        // Local: desarrollo diario contra el docker-compose del repositorio de
        // servicios. 10.0.2.2 es como el emulador alcanza el localhost del PC;
        // "localhost" dentro del emulador es el propio dispositivo virtual.
        debug {
            buildConfigField("String", "AMBIENTE", "\"local\"")
            buildConfigField("String", "API_GATEWAY_URL", "\"$urlDelServicioLocal\"")
            // Depuracion autentica contra el pool real, el mismo de
            // produccion. Es lo que permite comprobar en el dispositivo el
            // ingreso, la renovacion silenciosa y el vencimiento sin tener
            // que construir y firmar la version de produccion. El pool no
            // tiene datos personales: sus cuatro usuarios son de prueba.
            buildConfigField("String", "COGNITO_REGION", "\"$regionDeCognito\"")
            buildConfigField("String", "COGNITO_USER_POOL_ID", "\"$poolDeCognito\"")
            buildConfigField("String", "COGNITO_CLIENTE_ID", "\"$clienteMovilDeCognito\"")
        }

        // Producción (demo): la entrega evaluable que despliega el pipeline.
        // Los valores reales salen del repositorio de infraestructura (Terraform).
        release {
            if (rutaDelAlmacen != null) {
                signingConfig = signingConfigs.getByName("publicacion")
            }
            buildConfigField("String", "AMBIENTE", "\"produccion\"")
            buildConfigField("String", "API_GATEWAY_URL", "\"$urlDelApiGateway\"")
            buildConfigField("String", "COGNITO_REGION", "\"$regionDeCognito\"")
            buildConfigField("String", "COGNITO_USER_POOL_ID", "\"$poolDeCognito\"")
            buildConfigField("String", "COGNITO_CLIENTE_ID", "\"$clienteMovilDeCognito\"")
            // Sin `packageScope`: el valor por omisión es `**`, o sea todo.
            //
            // El andamiaje traía `setOf("androidx.**", "kotlin.**",
            // "kotlinx.**")`, que **excluía nuestro propio código** de la
            // optimización y dejaba a R8 tocando las librerías de forma
            // inconsistente con quien las usa. El APK arrancaba y moría al
            // instante con `IllegalAccessError` en `WorkManagerInitializer`
            // —verificado en el Moto G20 el 17/10/2026—.
            //
            // Además `packageScope` es experimental y exige
            // `android.r8.gradual.support=true`, que este proyecto no
            // declara. No hay razón para apartarse del comportamiento por
            // omisión, que es el camino probado.
            optimization {
                enable = true
            }
        }

        // Staging: réplica reducida que solo se enciende durante las ventanas
        // de experimentación (k6, Toxiproxy, OWASP ZAP). El sufijo del
        // applicationId permite tenerla instalada junto a la de producción.
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            matchingFallbacks += listOf("release")
            buildConfigField("String", "AMBIENTE", "\"staging\"")
            buildConfigField("String", "API_GATEWAY_URL", "\"$urlDelApiGateway\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Robolectric (HU-M1.3-01): Room y WorkManager se prueban en la JVM con
    // los recursos de la aplicacion. Robolectric solo lee los assets fusionados
    // de la variante (no los del conjunto `test`), asi que los esquemas
    // exportados de la base entran como assets de `debug` para que
    // MigrationTestHelper compruebe cada migracion contra el esquema
    // versionado. Son dos JSON de pocos KB y no llegan a release ni a staging.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        getByName("debug") {
            assets.directories.add("$projectDir/schemas")
        }
    }
}

// Compuerta de cobertura: la misma que el pipeline del cliente web (>= 70 %).
// Se excluye el código que generan Hilt, Room y Compose, y las vistas previas,
// porque no son código que el equipo pueda probar.
kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.Hilt_*",
                    "*_HiltModules*",
                    "*_Factory",
                    "*_Factory\$*",
                    "*_MembersInjector",
                    "*_MembersInjector\$*",
                    "*_GeneratedInjector",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.*",
                    "*.ComposableSingletons*",
                    "*_Impl",
                    "*_Impl\$*",
                    // Capa de interfaz: se verifica con pruebas instrumentadas
                    // (Compose UI Test y Espresso), no con unitarias. Se
                    // excluyen los paquetes completos y no solo las funciones
                    // @Composable, porque un archivo de interfaz deja ademas
                    // constantes, enumeraciones y lambdas sueltas que no son
                    // logica de negocio y distorsionan la medicion.
                    "co.solventa.movil.ui.theme.*",
                    "co.solventa.movil.shared.ui.*",
                    "co.solventa.movil.MainActivity",
                    "co.solventa.movil.SolventaApp",
                    // Adaptador del sensor biometrico: su unica logica es
                    // traducir los codigos de BiometricPrompt, y eso solo se
                    // ejerce con un sensor real. Se verifica en dispositivo.
                    // El resto de core/auth si se prueba en la JVM.
                    // Envuelven CameraX y el permiso del sistema: no se
                    // pueden ejercer sin dispositivo. Lo que sí se prueba es
                    // la aritmética del medidor de luz, que queda fuera de
                    // esta lista a propósito.
                    // Reduce la captura con Bitmap y BitmapFactory, que no
                    // existen fuera del dispositivo. Lo que sí se prueba en la
                    // JVM es el repositorio del registro, que lo usa detrás de
                    // su interfaz, y la verificación de la imagen reducida se
                    // hace en el teléfono.
                    "co.solventa.movil.core.imagen.PreparadorDeCapturaJpeg",
                    "co.solventa.movil.core.imagen.ReductorDeFotoJpeg",
                    "co.solventa.movil.core.imagen.ReduccionJpegKt",
                    // Captura de evidencia (HU-M2.1-01): CameraX con video, el
                    // LocationManager del sistema, la subida firmada y el
                    // trabajador de WorkManager. Nada de eso existe fuera del
                    // dispositivo; lo que sí se prueba en la JVM es el
                    // repositorio y el ViewModel, que los usan detrás de sus
                    // interfaces.
                    "co.solventa.movil.features.siniestros.evidencia.ControladorDeEvidencia",
                    "co.solventa.movil.features.siniestros.evidencia.CamaraDeEvidenciaKt",
                    "co.solventa.movil.features.siniestros.evidencia.MiniaturaDeEvidenciaKt",
                    "co.solventa.movil.features.siniestros.evidencia.VistaDeEvidenciaKt",
                    "co.solventa.movil.features.siniestros.evidencia.TrabajadorDeSubida",
                    "co.solventa.movil.features.siniestros.evidencia.EnvioConWorkManager",
                    "co.solventa.movil.core.ubicacion.LocalizadorDelSistema",
                    "co.solventa.movil.core.api.siniestros.SubidorDeArchivos",
                    "co.solventa.movil.shared.ui.camara.ControladorDeCamara",
                    "co.solventa.movil.shared.ui.camara.CamaraDeDocumento",
                    "co.solventa.movil.shared.ui.camara.VistaPreviaDeCamaraKt",
                    "co.solventa.movil.core.auth.biometria.AutenticadorBiometricoAndroid",
                    "co.solventa.movil.core.auth.biometria.AutenticadorBiometricoAndroid\$*",
                    // Vistas previas: herramienta de diseno, no codigo que corra.
                    "*PreviewKt",
                    // Declaraciones de rutas: objetos sin comportamiento.
                    "co.solventa.movil.core.navegacion.Ruta",
                    "co.solventa.movil.core.navegacion.Ruta\$*",
                )
                annotatedBy(
                    "androidx.compose.ui.tooling.preview.Preview",
                    "androidx.compose.runtime.Composable",
                )
            }
        }
        verify {
            rule {
                minBound(70)
            }
        }
    }
}

// Deja el esquema de la base de datos versionado en app/schemas,
// para que las migraciones se puedan revisar en la solicitud de fusión.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navegación y estado
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Red
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Trabajo en segundo plano
    implementation(libs.androidx.work.runtime.ktx)

    // Cámara
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // Biometría
    implementation(libs.androidx.biometric)

    // Inyección de dependencias
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Base de datos local
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // La comprobacion de la renovacion contra el user pool real usa el mismo
    // cliente HTTP que la aplicacion. Sin kotlinx-coroutines-test: en el
    // dispositivo resuelve a una version mas nueva que el nucleo de corrutinas
    // que la aplicacion empaqueta, y runTest falla con NoSuchMethodError. Esa
    // prueba espera llamadas de red reales, asi que runBlocking basta.
    androidTestImplementation(libs.retrofit)
    androidTestImplementation(libs.retrofit.kotlinx.serialization)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}