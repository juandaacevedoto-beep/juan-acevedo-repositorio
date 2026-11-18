package co.edu.ecci.vai

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class VM : ViewModel() {
    var user by mutableStateOf(Session.loadUser())
    var tasks by mutableStateOf(listOf<Task>())
    var items by mutableStateOf(listOf<Item>())
    var swaps by mutableStateOf(listOf<Swap>())
    var convs by mutableStateOf(listOf<Conv>())

    private fun ok(r: JSONObject): Boolean {
        if (!r.optBoolean("ok") && r.optString("error") == "Sesión expirada") localLogout()
        return r.optBoolean("ok")
    }
    private fun err(r: JSONObject) = r.optString("error", "Error desconocido")

    private fun localLogout() {
        Session.clear(); user = null
        tasks = emptyList(); items = emptyList(); swaps = emptyList(); convs = emptyList()
    }

    suspend fun logout(ctx: Context) {
        tasks.forEach { Reminders.cancel(ctx, it.id) }
        Api.call("logout")
        localLogout()
    }

    // ---- Auth ----
    suspend fun login(email: String, pass: String): String? {
        val r = Api.call("login", JSONObject().put("email", email).put("password", pass))
        if (!r.optBoolean("ok")) return err(r)
        val u = r.getJSONObject("user")
        Session.token = u.str("token"); Session.saveUser(u); user = u.toUser()
        return null
    }

    suspend fun register(name: String, email: String, pass: String, semester: Int): String? {
        val r = Api.call("register", JSONObject().put("name", name).put("email", email).put("password", pass).put("semester", semester))
        return if (r.optBoolean("ok")) null else err(r)
    }

    suspend fun updateProfile(name: String, semester: Int): String? {
        val r = Api.call("update_profile", JSONObject().put("name", name).put("semester", semester))
        if (!ok(r)) return err(r)
        val u = r.getJSONObject("user"); Session.saveUser(u); user = u.toUser()
        return null
    }

    // ---- Carga ----
    suspend fun refreshAll(ctx: Context) { loadTasks(ctx); loadItems(); loadSwaps(); loadConvs() }

    suspend fun loadTasks(ctx: Context) {
        val r = Api.call("tasks_list")
        if (ok(r)) { tasks = r.arr("tasks").map { it.toTask() }; tasks.forEach { Reminders.schedule(ctx, it) } }
    }
    suspend fun loadItems() { val r = Api.call("items_list"); if (ok(r)) items = r.arr("items").map { it.toItem() } }
    suspend fun loadSwaps() { val r = Api.call("swaps_list"); if (ok(r)) swaps = r.arr("swaps").map { it.toSwap() } }
    suspend fun loadConvs() { val r = Api.call("inbox"); if (ok(r)) convs = r.arr("convs").map { it.toConv() } }

    // ---- Tareas ----
    suspend fun saveTask(ctx: Context, t: Task): String? {
        val r = Api.call(
            "task_save", JSONObject().put("id", t.id).put("title", t.title).put("date", t.date).put("time", t.time)
                .put("category", t.category).put("priority", t.priority).put("reminder", t.reminder).put("done", t.done)
        )
        if (!ok(r)) return err(r)
        loadTasks(ctx); return null
    }
    suspend fun deleteTask(ctx: Context, t: Task): String? {
        Reminders.cancel(ctx, t.id)
        val r = Api.call("task_delete", JSONObject().put("id", t.id))
        if (!ok(r)) return err(r)
        loadTasks(ctx); return null
    }

    // ---- Marketplace ----
    suspend fun createItem(title: String, desc: String, cat: String, price: Int, cond: String, imgB64: String): String? {
        val r = Api.call(
            "item_create", JSONObject().put("title", title).put("description", desc).put("category", cat)
                .put("price", price).put("cond", cond).put("image", imgB64)
        )
        if (!ok(r)) return err(r)
        loadItems(); return null
    }
    suspend fun markSold(id: Int): String? {
        val r = Api.call("item_sold", JSONObject().put("id", id)); if (!ok(r)) return err(r)
        loadItems(); return null
    }
    suspend fun deleteItem(id: Int): String? {
        val r = Api.call("item_delete", JSONObject().put("id", id)); if (!ok(r)) return err(r)
        loadItems(); return null
    }

    // ---- Intercambios ----
    suspend fun createSwap(title: String, desc: String, cat: String, wanted: String, imgB64: String): String? {
        val r = Api.call(
            "swap_create", JSONObject().put("title", title).put("description", desc).put("category", cat)
                .put("wanted", wanted).put("image", imgB64)
        )
        if (!ok(r)) return err(r)
        loadSwaps(); return null
    }
    suspend fun deleteSwap(id: Int): String? {
        val r = Api.call("swap_delete", JSONObject().put("id", id)); if (!ok(r)) return err(r)
        loadSwaps(); return null
    }

    // ---- Mensajes ----
    suspend fun chatGet(with: Int): List<Msg> {
        val r = Api.call("chat_get", JSONObject().put("with", with))
        return if (ok(r)) r.arr("messages").map { it.toMsg() } else emptyList()
    }
    suspend fun chatSend(to: Int, subject: String, text: String): Boolean =
        ok(Api.call("chat_send", JSONObject().put("to", to).put("subject", subject).put("text", text)))
    suspend fun roomGet(room: String): List<Msg> {
        val r = Api.call("room_get", JSONObject().put("room", room))
        return if (ok(r)) r.arr("messages").map { it.toMsg() } else emptyList()
    }
    suspend fun roomSend(room: String, text: String): Boolean =
        ok(Api.call("room_send", JSONObject().put("room", room).put("text", text)))
}
