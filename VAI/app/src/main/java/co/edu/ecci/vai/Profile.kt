package co.edu.ecci.vai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(vm: VM, push: (Nav) -> Unit) {
    val u = vm.user ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    val mine = vm.items.filter { it.sellerId == u.id }
    val pending = vm.tasks.count { !it.done }
    val mySwaps = vm.swaps.count { it.userId == u.id }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(u.name, 80)
                Spacer(Modifier.height(10.dp))
                T(u.name, 20, FontWeight.ExtraBold)
                T(u.email, 12, color = Muted)
                T("Semestre ${u.semester} · Universidad ECCI", 12, FontWeight.Medium, Indigo)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("$pending", "Tareas pendientes", Modifier.weight(1f))
                Stat("${mine.size}", "Publicaciones", Modifier.weight(1f))
                Stat("$mySwaps", "Intercambios", Modifier.weight(1f))
            }
        }
        item { T("Mis mensajes", 16, FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp)) }
        if (vm.convs.isEmpty()) item { T("Aún no tienes conversaciones.", 12, color = Muted) }
        items(vm.convs, key = { "c" + it.otherId }) { c ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
                    .clickable { push(Nav.Chat(c.otherId, c.otherName, c.subject)) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(c.otherName, 38); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    T(c.otherName, 13, FontWeight.Bold, maxLines = 1)
                    T(c.last, 11, color = Muted, maxLines = 1)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
            }
        }
        item { T("Mis publicaciones", 16, FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp)) }
        if (mine.isEmpty()) item { T("Todavía no has publicado artículos. Ve a Ventas y toca +.", 12, color = Muted) }
        items(mine, key = { "i" + it.id }) { i ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
                    .clickable { push(Nav.ItemDetail(i.id)) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pic(i.image, i.category, Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    T(i.title, 13, FontWeight.Bold, maxLines = 1)
                    T(money(i.price) + if (i.sold) " · VENDIDO" else "", 11, color = if (i.sold) Color(0xFF15803D) else Muted)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton({ editing = true }, Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); T("Editar perfil", 13, FontWeight.Bold, Indigo)
            }
        }
        item {
            Button(
                { scope.launch { vm.logout(ctx); toast(ctx, "Sesión cerrada") } }, Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDE8EC))
            ) {
                Icon(Icons.Outlined.Logout, null, Modifier.size(16.dp), tint = Color(0xFFE11D48)); Spacer(Modifier.width(6.dp))
                T("Cerrar sesión", 13, FontWeight.Bold, Color(0xFFE11D48))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (editing) {
        var name by remember { mutableStateOf(u.name) }
        var sem by remember { mutableIntStateOf(u.semester) }
        AlertDialog(
            onDismissRequest = { editing = false }, containerColor = Color.White,
            title = { T("Editar perfil", 17, FontWeight.ExtraBold) },
            text = {
                Column {
                    Field(name, { name = it }, "Nombre")
                    Spacer(Modifier.height(10.dp))
                    T("Semestre activo", 12, FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..10).forEach { Chip("$it", sem == it) { sem = it } }
                    }
                }
            },
            confirmButton = {
                TextButton({
                    scope.launch {
                        val e = vm.updateProfile(name.trim(), sem)
                        if (e != null) toast(ctx, e) else { toast(ctx, "Perfil actualizado ✅"); editing = false }
                    }
                }) { T("Guardar", 14, FontWeight.Bold, Indigo) }
            },
            dismissButton = { TextButton({ editing = false }) { T("Cancelar", 14, color = Muted) } }
        )
    }
}

@Composable
fun Stat(value: String, label: String, mod: Modifier) = Column(
    mod.clip(RoundedCornerShape(14.dp)).background(Color.White).border(1.dp, Line, RoundedCornerShape(14.dp)).padding(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    T(value, 20, FontWeight.ExtraBold, Indigo)
    T(label, 10, color = Muted, align = TextAlign.Center, maxLines = 2)
}

/** Chat 1 a 1 (room == null) o sala grupal del semestre (room != null). Se actualiza cada 4 s. */
@Composable
fun ChatScreen(vm: VM, title: String, subtitle: String, room: String?, toId: Int, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msgs by remember { mutableStateOf(listOf<Msg>()) }
    var text by remember { mutableStateOf("") }
    val me = vm.user?.id ?: 0
    val state: LazyListState = rememberLazyListState()

    suspend fun load() { msgs = if (room != null) vm.roomGet(room) else vm.chatGet(toId) }
    LaunchedEffect(Unit) { while (true) { load(); delay(4000) } }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) state.animateScrollToItem(msgs.size - 1) }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(back) { Icon(Icons.Outlined.ArrowBack, "Volver") }
            Avatar(title, 36); Spacer(Modifier.width(10.dp))
            Column {
                T(title, 15, FontWeight.ExtraBold, maxLines = 1)
                T(subtitle, 11, color = Muted, maxLines = 1)
            }
        }
        LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (msgs.isEmpty()) item { T("Aún no hay mensajes. ¡Escribe el primero!", 12, color = Muted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            items(msgs, key = { it.id }) { m ->
                val mineMsg = m.fromId == me
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mineMsg) Alignment.End else Alignment.Start) {
                    Column(
                        Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (mineMsg) Indigo else Color.White).padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (!mineMsg && room != null) T(m.fromName, 10, FontWeight.Bold, Indigo)
                        T(m.text, 13, color = if (mineMsg) Color.White else Ink)
                        T(m.at, 9, color = if (mineMsg) Color(0xFFD5D7FF) else Muted)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Field(text, { text = it }, "Escribe un mensaje...", Modifier.weight(1f))
            IconButton({
                val t = text.trim()
                if (t.isEmpty()) return@IconButton
                scope.launch {
                    val ok = if (room != null) vm.roomSend(room, t) else vm.chatSend(toId, subtitle, t)
                    if (ok) { text = ""; load() } else toast(ctx, "No se pudo enviar el mensaje")
                }
            }) { Icon(Icons.Outlined.Send, "Enviar", tint = Indigo) }
        }
    }
}
