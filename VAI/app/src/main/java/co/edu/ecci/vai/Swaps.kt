package co.edu.ecci.vai

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val SWAP_CATS = listOf("Libros", "Apuntes de clase", "Equipo de laboratorio", "Calculadoras", "Otros")

@Composable
fun SwapScreen(vm: VM, push: (Nav) -> Unit) {
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Todo") }
    val list = vm.swaps.filter {
        (cat == "Todo" || it.category == cat) &&
            (q.isBlank() || it.title.contains(q, true) || it.description.contains(q, true) || it.wanted.contains(q, true))
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            T("Intercambio de Materiales", 20, FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
            Field(q, { q = it }, "Buscar libros, apuntes, calculadoras...", leading = { Icon(Icons.Outlined.Search, null, tint = Muted) })
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("Todo") + SWAP_CATS).forEach { Chip(it, cat == it) { cat = it } }
            }
            LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (list.isEmpty()) item { T("No hay intercambios. ¡Publica uno con el botón +!", 13, color = Muted) }
                items(list, key = { it.id }) { s ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                            .border(1.dp, Line, RoundedCornerShape(16.dp)).clickable { push(Nav.SwapDetail(s.id)) }.padding(10.dp)
                    ) {
                        Row {
                            Pic(s.image, s.category, Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Soft).padding(horizontal = 7.dp, vertical = 2.dp)) { T(s.category, 9, FontWeight.Bold, Indigo) }
                                Spacer(Modifier.height(4.dp))
                                T(s.title, 14, FontWeight.ExtraBold, maxLines = 2)
                                T(s.description, 11, color = Muted, maxLines = 2)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF4F6FB)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.SwapHoriz, null, tint = Indigo, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            T("Cambio por: ${s.wanted}", 11, FontWeight.Bold, maxLines = 2)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { push(Nav.NewSwap) }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Indigo, contentColor = Color.White, shape = CircleShape
        ) { Icon(Icons.Outlined.Add, "Publicar intercambio") }
    }
}

@Composable
fun SwapDetailScreen(vm: VM, id: Int, push: (Nav) -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = vm.swaps.find { it.id == id }
    if (s == null) { LaunchedEffect(Unit) { back() }; return }
    val mine = s.userId == vm.user?.id
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        TopBar("Detalle del intercambio", back)
        Pic(s.image, s.category, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Soft).padding(horizontal = 10.dp, vertical = 5.dp)) { T(s.category, 11, FontWeight.Bold, Indigo) }
        Spacer(Modifier.height(8.dp))
        T(s.title, 20, FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        T(s.description.ifBlank { "Sin descripción." }, 13, color = Color(0xFF4B5563))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.SwapHoriz, null, tint = Indigo)
            Spacer(Modifier.width(8.dp))
            Column {
                T("Cambio por", 11, color = Muted)
                T(s.wanted, 14, FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Avatar(s.userName, 36); Spacer(Modifier.width(8.dp)); T(s.userName, 13, FontWeight.Bold) }
        Spacer(Modifier.height(24.dp))
        if (mine) {
            OutlinedButton({
                scope.launch { val e = vm.deleteSwap(id); if (e != null) toast(ctx, e) else { toast(ctx, "Intercambio eliminado"); back() } }
            }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { T("Eliminar mi intercambio", 14, FontWeight.Bold, Color(0xFFE11D48)) }
        } else BigButton("Proponer intercambio", { push(Nav.Chat(s.userId, s.userName, "Intercambio: " + s.title)) })
    }
}

@Composable
fun NewSwapScreen(vm: VM, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Libros") }
    var wanted by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(20.dp)) {
        TopBar("Publicar intercambio", back)
        PhotoBox(photo) { photo = it }
        Label("¿Qué ofreces?")
        Field(title, { title = it }, "Química orgánica")
        Label("Descripción")
        Field(desc, { desc = it }, "Estado del material...", singleLine = false, minLines = 3)
        Label("Categoría")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SWAP_CATS.forEach { Chip(it, cat == it) { cat = it } }
        }
        Label("Cambio por")
        Field(wanted, { wanted = it }, "Libro de cálculo multivariable")
        Spacer(Modifier.height(24.dp))
        BigButton("Publicar intercambio", {
            scope.launch {
                busy = true
                val e = vm.createSwap(title.trim(), desc.trim(), cat, wanted.trim(), photo?.toB64() ?: "")
                busy = false
                if (e != null) toast(ctx, e) else { toast(ctx, "Intercambio publicado ✅"); back() }
            }
        }, loading = busy)
    }
}
