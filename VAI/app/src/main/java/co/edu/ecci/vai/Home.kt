package co.edu.ecci.vai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun fmtTime(t: String): String = try {
    LocalTime.parse(t).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
} catch (e: Exception) { t }

@Composable
fun TaskRow(t: Task, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
        .border(1.dp, Line, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(14.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    T(fmtTime(t.time), 11, FontWeight.Medium, Muted, Modifier.width(58.dp))
    Column(Modifier.weight(1f)) {
        T(t.title, 14, FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(4.dp)); Tag(t.category)
    }
    Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
}

@Composable
fun QuickBtn(icon: ImageVector, label: String, mod: Modifier, onClick: () -> Unit) = Column(
    mod.clip(RoundedCornerShape(14.dp)).background(Color.White).border(1.dp, Line, RoundedCornerShape(14.dp))
        .clickable(onClick = onClick).padding(12.dp)
) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Soft), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Indigo, modifier = Modifier.size(18.dp))
    }
    Spacer(Modifier.height(8.dp))
    T(label, 11, FontWeight.Bold, maxLines = 2)
}

@Composable
fun HomeScreen(vm: VM, goTab: (Int) -> Unit, push: (Nav) -> Unit) {
    val u = vm.user ?: return
    val todayStr = today().toString()
    val list = vm.tasks.filter { it.date == todayStr && !it.done }.sortedBy { it.time }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T("Bienvenido", 12, color = Muted)
                    T(u.name, 21, FontWeight.ExtraBold, maxLines = 1)
                }
                Avatar(u.name, 46)
            }
        }
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Indigo).padding(20.dp)) {
                Column {
                    T("SEMESTRE ACTIVO-S", 10, FontWeight.Medium, Color(0xFFD5D7FF))
                    T("${u.semester}", 38, FontWeight.ExtraBold, Color.White)
                    T("Semana actual", 12, color = Color(0xFFD5D7FF))
                }
                Icon(Icons.Outlined.TrendingUp, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                T("Agenda de hoy", 18, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                T("View All", 12, FontWeight.Bold, Indigo, Modifier.clickable { goTab(1) })
            }
        }
        if (list.isEmpty()) item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(20.dp)) {
                T("Hoy no tienes pendientes 🎉  Toca \"View All\" para agendar algo.", 13, color = Muted)
            }
        } else items(list, key = { it.id }) { t -> TaskRow(t) { push(Nav.TaskForm(t, null)) } }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickBtn(Icons.Outlined.Groups, "Sala de socialización", Modifier.weight(1f)) { push(Nav.Room) }
                QuickBtn(Icons.Outlined.SwapHoriz, "Intercambios", Modifier.weight(1f)) { goTab(2) }
                QuickBtn(Icons.Outlined.ShoppingBag, "Marketplace", Modifier.weight(1f)) { goTab(3) }
            }
        }
    }
}
