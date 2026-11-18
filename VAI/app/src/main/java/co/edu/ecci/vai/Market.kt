package co.edu.ecci.vai

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val ITEM_CATS = listOf("Electrónica", "Vestir", "Laboratorio", "Suministros", "Libros", "Otros")
val CONDITIONS = listOf("Como nuevo", "Excelente estado", "Buen estado", "Usado")

@Composable
fun ItemCard(it: Item, mine: Boolean, onOpen: () -> Unit, onChat: () -> Unit) = Column(
    Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Line, RoundedCornerShape(16.dp))
        .clickable(onClick = onOpen).padding(8.dp)
) {
    Box {
        Pic(it.image, it.category, Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)))
        Box(Modifier.padding(6.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal = 7.dp, vertical = 3.dp)) {
            T(money(it.price) + "cop", 10, FontWeight.Bold, Indigo)
        }
    }
    Spacer(Modifier.height(6.dp))
    T(it.category.uppercase(), 9, FontWeight.Medium, Muted)
    T(it.title, 13, FontWeight.Bold, maxLines = 1)
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(it.sellerName, 20)
        Spacer(Modifier.width(5.dp))
        T(it.sellerName, 10, color = Muted, modifier = Modifier.weight(1f), maxLines = 1)
        if (!mine) Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(Soft).clickable(onClick = onChat),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.ChatBubbleOutline, "Mensaje", tint = Indigo, modifier = Modifier.size(15.dp)) }
    }
}

@Composable
fun MarketScreen(vm: VM, push: (Nav) -> Unit) {
    val me = vm.user?.id ?: 0
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Todo") }
    val list = vm.items.filter {
        !it.sold && (cat == "Todo" || it.category == cat) &&
            (q.isBlank() || it.title.contains(q, true) || it.description.contains(q, true))
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            T("Campus Market", 22, FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
            Field(q, { q = it }, "Busca calculadoras, sudaderas con capucha, tecnología...", leading = { Icon(Icons.Outlined.Search, null, tint = Muted) })
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("Todo") + ITEM_CATS).forEach { Chip(it, cat == it) { cat = it } }
            }
            if (list.isEmpty()) T("No hay artículos para mostrar. ¡Publica el primero con el botón +!", 13, color = Muted, modifier = Modifier.padding(top = 24.dp))
            LazyVerticalGrid(
                GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
            ) {
                items(list, key = { it.id }) { it ->
                    ItemCard(it, it.sellerId == me, { push(Nav.ItemDetail(it.id)) }, { push(Nav.Chat(it.sellerId, it.sellerName, it.title)) })
                }
            }
        }
        FloatingActionButton(
            onClick = { push(Nav.NewItem) }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Indigo, contentColor = Color.White, shape = CircleShape
        ) { Icon(Icons.Outlined.Add, "Vender artículo") }
    }
}

@Composable
fun ItemDetailScreen(vm: VM, id: Int, push: (Nav) -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val item = vm.items.find { it.id == id }
    if (item == null) { LaunchedEffect(Unit) { back() }; return }
    val mine = item.sellerId == vm.user?.id
    var fav by remember { mutableStateOf(id in Session.favs) }
    val count = vm.items.count { it.sellerId == item.sellerId && !it.sold }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            TopBar("Detalles del artículo", back) {
                IconButton({
                    fav = !fav
                    Session.favs = if (fav) Session.favs + id else Session.favs - id
                }) { Icon(if (fav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Favorito", tint = if (fav) Color(0xFFE11D48) else Ink) }
            }
            Pic(item.image, item.category, Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp)))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Soft).padding(horizontal = 10.dp, vertical = 6.dp)) { T(item.category, 11, FontWeight.Bold, Indigo) }
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFDCFCE7)).padding(horizontal = 10.dp, vertical = 6.dp)) { T(item.cond, 11, FontWeight.Bold, Color(0xFF15803D)) }
            }
            Spacer(Modifier.height(12.dp))
            T(item.title, 20, FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            T(money(item.price), 22, FontWeight.ExtraBold, Indigo)
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Line)
            T("Descripción", 15, FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            T(item.description.ifBlank { "Sin descripción." }, 13, color = Color(0xFF4B5563))
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).border(1.dp, Line, RoundedCornerShape(14.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(item.sellerName, 42)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    T(item.sellerName, 14, FontWeight.Bold)
                    T("$count artículo(s) publicado(s)", 11, color = Muted)
                }
                Icon(Icons.Outlined.VerifiedUser, null, tint = Color(0xFF15803D))
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.background(Color.White).padding(16.dp)) {
            if (mine) {
                BigButton("Marcar como vendido", {
                    scope.launch { val e = vm.markSold(id); if (e != null) toast(ctx, e) else { toast(ctx, "¡Marcado como vendido! 🎉"); back() } }
                })
                TextButton({
                    scope.launch { val e = vm.deleteItem(id); if (e != null) toast(ctx, e) else { toast(ctx, "Publicación eliminada"); back() } }
                }, Modifier.fillMaxWidth()) { T("Eliminar publicación", 13, FontWeight.Bold, Color(0xFFE11D48)) }
            } else {
                Button(
                    { push(Nav.Chat(item.sellerId, item.sellerName, item.title)) }, Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    T("Enviar mensaje al vendedor", 15, FontWeight.Bold, Color.White)
                }
            }
        }
    }
}

@Composable
fun NewItemScreen(vm: VM, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Electrónica") }
    var cond by remember { mutableStateOf("Buen estado") }
    var price by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(20.dp)) {
        TopBar("Vender artículo", back)
        PhotoBox(photo) { photo = it }
        Label("Nombre del artículo")
        Field(title, { title = it }, "Calculadora gráfica TI-84")
        Label("Descripción")
        Field(desc, { desc = it }, "Estado, accesorios incluidos...", singleLine = false, minLines = 3)
        Label("Categoría")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ITEM_CATS.forEach { Chip(it, cat == it) { cat = it } }
        }
        Label("Estado")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CONDITIONS.forEach { Chip(it, cond == it) { cond = it } }
        }
        Label("Precio (COP)")
        Field(price, { price = it.filter { c -> c.isDigit() } }, "20000", keyboard = KeyboardType.Number)
        Spacer(Modifier.height(24.dp))
        BigButton("Publicar artículo", {
            val p = price.toIntOrNull() ?: 0
            when {
                photo == null -> toast(ctx, "Toma una foto del artículo con la cámara 📷")
                title.isBlank() -> toast(ctx, "Escribe el nombre del artículo")
                p <= 0 -> toast(ctx, "Escribe un precio válido")
                else -> scope.launch {
                    busy = true
                    val e = vm.createItem(title.trim(), desc.trim(), cat, p, cond, photo!!.toB64())
                    busy = false
                    if (e != null) toast(ctx, e) else { toast(ctx, "Artículo publicado ✅"); back() }
                }
            }
        }, loading = busy)
    }
}
