package co.edu.ecci.vai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId

/**
 * ⚠️ Emulador de Android Studio: 10.0.2.2 apunta al localhost de tu PC (XAMPP).
 * Celular real: pon la IP de tu PC en la misma WiFi, ej. "http://192.168.1.20/vai_api/"
 */
const val HOST = "http://10.0.2.2/vai_api/"

val BOG: ZoneId = ZoneId.of("America/Bogota")
fun today(): LocalDate = LocalDate.now(BOG)

data class User(val id: Int, val name: String, val email: String, val semester: Int)
data class Task(
    val id: Int, val title: String, val date: String, val time: String,
    val category: String, val priority: String, val reminder: Boolean, val done: Boolean
)
data class Item(
    val id: Int, val title: String, val description: String, val category: String, val price: Int,
    val cond: String, val image: String, val sellerId: Int, val sellerName: String, val sold: Boolean
)
data class Swap(
    val id: Int, val title: String, val description: String, val category: String,
    val wanted: String, val image: String, val userId: Int, val userName: String
)
data class Msg(val id: Int, val fromId: Int, val fromName: String, val text: String, val at: String)
data class Conv(val otherId: Int, val otherName: String, val last: String, val subject: String)

fun JSONObject.str(k: String): String = if (isNull(k)) "" else optString(k)
fun JSONObject.arr(k: String): List<JSONObject> {
    val a = optJSONArray(k) ?: return emptyList()
    return List(a.length()) { a.getJSONObject(it) }
}
fun JSONObject.toUser() = User(optInt("id"), str("name"), str("email"), optInt("semester", 1))
fun JSONObject.toTask() = Task(
    optInt("id"), str("title"), str("date"), str("time").take(5),
    str("category"), str("priority"), optInt("reminder") == 1, optInt("done") == 1
)
fun JSONObject.toItem() = Item(
    optInt("id"), str("title"), str("description"), str("category"), optInt("price"),
    str("cond"), str("image"), optInt("seller_id"), str("seller_name"), optInt("sold") == 1
)
fun JSONObject.toSwap() = Swap(
    optInt("id"), str("title"), str("description"), str("category"),
    str("wanted"), str("image"), optInt("user_id"), str("user_name")
)
fun JSONObject.toMsg() = Msg(optInt("id"), optInt("from_id"), str("from_name"), str("text"), str("at"))
fun JSONObject.toConv() = Conv(optInt("other_id"), str("other_name"), str("last"), str("subject"))

object Session {
    private lateinit var p: android.content.SharedPreferences
    fun init(c: Context) { p = c.getSharedPreferences("vai", Context.MODE_PRIVATE) }

    var token: String
        get() = p.getString("token", "") ?: ""
        set(v) { p.edit().putString("token", v).apply() }

    fun saveUser(u: JSONObject) { p.edit().putString("user", u.toString()).apply() }
    fun loadUser(): User? {
        val s = p.getString("user", null) ?: return null
        if (token.isEmpty()) return null
        return try { JSONObject(s).toUser() } catch (e: Exception) { null }
    }

    var favs: Set<Int>
        get() = (p.getStringSet("favs", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
        set(v) { p.edit().putStringSet("favs", v.map { it.toString() }.toSet()).apply() }

    fun clear() { p.edit().remove("token").remove("user").apply() }
}

object Api {
    suspend fun call(action: String, params: JSONObject = JSONObject()): JSONObject = withContext(Dispatchers.IO) {
        try {
            val c = URL(HOST + "api.php").openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 10000
            c.readTimeout = 30000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            params.put("action", action)
            params.put("token", Session.token)
            c.outputStream.use { it.write(params.toString().toByteArray(Charsets.UTF_8)) }
            val stream = if (c.responseCode < 400) c.inputStream else c.errorStream
            JSONObject(stream.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            JSONObject().put("ok", false)
                .put("error", "No se pudo conectar con el servidor. ¿XAMPP está encendido? (${e.javaClass.simpleName})")
        }
    }
}
