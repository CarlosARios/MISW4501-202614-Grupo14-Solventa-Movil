package co.solventa.movil.features.acceso.registro

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Datos personales para registrarse en la demostración.
 *
 * **Por qué se generan.** El registro tiene que escribir en la base de datos
 * de AWS y crear una cuenta en el *user pool*, y las dos cosas exigen un
 * documento y un correo que nadie haya usado antes. Un dato fijo serviría una
 * sola vez: el segundo intento recibiría `409`. Leer el documento real del
 * usuario, además, metería su cédula en un ambiente de pruebas.
 *
 * Cada campo se puede corregir en M1c antes de enviarlo.
 *
 * **La contraseña es legible a propósito.** Son credenciales de prueba que
 * hay que poder teclear en M2b para comprobar el ingreso, así que la parte
 * variable son cuatro dígitos y el resto se lee de un vistazo.
 */
@Singleton
class GeneradorDeDatosDeDemostracion @Inject constructor() {

    private val aleatorio: Random = Random.Default

    fun datos(): DatosDocumento {
        val nombre = NOMBRES.random(aleatorio)
        val primerApellido = APELLIDOS.random(aleatorio)
        val segundoApellido = APELLIDOS.random(aleatorio)
        val sufijo = aleatorio.nextInt(1_000, 10_000)

        return DatosDocumento(
            nombres = nombre,
            apellidos = "$primerApellido $segundoApellido",
            tipoDocumento = "CC",
            numeroDocumento = conPuntos(cedula()),
            fechaNacimiento = nacimiento(),
            correo = "${sinTildes(nombre)}.${sinTildes(primerApellido)}.$sufijo@example.com",
            celular = celular(),
        )
    }

    /** Cumple la política del *user pool*: minúscula, mayúscula, dígito y símbolo. */
    fun contrasena(): String = "Solventa.2026.${aleatorio.nextInt(1_000, 10_000)}"

    /** Diez dígitos que empiezan por uno, como una cédula colombiana reciente. */
    private fun cedula(): String = buildString {
        append('1')
        repeat(LARGO_CEDULA - 1) { append(aleatorio.nextInt(10)) }
    }

    /** `1.020.345.678`, como lo muestra el frame M1c. */
    private fun conPuntos(cedula: String): String = cedula
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()

    /** Mayor de edad, que es lo que el contrato exige. */
    private fun nacimiento(): String {
        val hoy = LocalDate.now()
        val fecha = hoy
            .minusYears(aleatorio.nextLong(EDAD_MINIMA, EDAD_MAXIMA))
            .withDayOfMonth(aleatorio.nextInt(1, 29))
            .withMonth(aleatorio.nextInt(1, 13))
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    private fun celular(): String =
        "+57 3${aleatorio.nextInt(10)}${aleatorio.nextInt(10)} " +
            "${aleatorio.nextInt(100, 1_000)} ${aleatorio.nextInt(1_000, 10_000)}"

    /** El correo es el nombre de usuario en el *user pool*: sin tildes ni eñes. */
    private fun sinTildes(texto: String): String = Normalizer
        .normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase()

    private companion object {
        const val LARGO_CEDULA = 10
        const val EDAD_MINIMA = 20L
        const val EDAD_MAXIMA = 55L

        val NOMBRES = listOf(
            "Carolina", "Andrés", "Mariana", "Felipe", "Valentina",
            "Santiago", "Daniela", "Camilo", "Juliana", "Sebastián",
        )

        val APELLIDOS = listOf(
            "Pérez", "Ruiz", "Gómez", "Rodríguez", "Moreno",
            "Castaño", "Quintero", "Osorio", "Mejía", "Vargas",
        )
    }
}
