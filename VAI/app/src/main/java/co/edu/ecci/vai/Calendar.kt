package co.edu.ecci.vai

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val ES = Locale("es", "CO")

fun monthName(ym: YearMonth) = ym.month.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() }

fun fmtDate(d: String): String = try {
    LocalDate.parse(d).format(DateTimeFormatter.ofPattern("MMM d, yyyy", ES)).replaceFirstChar { it.uppercase() }
} catch (e: Exception) { d }

@Composable
fun CalendarScreen(vm: VM, push: (Nav) -> Unit) {
    var ym by remember { mutableStateOf(YearMonth.from(today())) }
    var sel by remember { mutableStateOf(today()) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var cat by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    val tasks = vm.tasks.filter { (cat == null || it.category == cat) && (query.isBlank() || it.title.contains(query, true)) }
    val byDate = tasks.groupBy { it.date }
    val dayTasks = (byDate[sel.toString()] ?: emptyList()).sortedBy { it.time }
    val todayD = today()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ ym = ym.minusMonths(1) }, Modifier.size(32.dp)) { Icon(Icons.Outlined.ChevronLeft, "Mes anterior") }
                    T("${monthName(ym)} ${ym.year}", 18, FontWeight.ExtraBold)
                    IconButton({ ym = ym.plusMonths(1) }, Modifier.size(32.dp)) { Icon(Icons.Outlined.ChevronRight, "Mes siguiente") }
                    Spacer(Modifier.weight(1f))
                    IconButton({ searching = !searching; if (!searching) query = "" }) { Icon(Icons.Outlined.Search, "Buscar") }
                    Box {
                        IconButton({ menu = true }) { Icon(Icons.Outlined.Tune, "Filtrar", tint = if (cat != null) Indigo else Ink) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ T("Todas las categorías") }, { cat = null; menu = false })
                            listOf("Exam", "Homework", "Project", "Personal", "Class").forEach { c ->
                                DropdownMenuItem({ T(catStyle(c).first.lowercase().replaceFirstChar { it.uppercase() }) }, { cat = c; menu = false })
                            }
                        }
                    }
                }
                if (searching) {
                    Field(query, { query = it }, "Buscar tarea...", leading = { Icon(Icons.Outlined.Search, null) })
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFEAF0F6)).padding(8.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach {
                            T(it, 10, FontWeight.Medium, Muted, Modifier.weight(1f), align = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val off = ym.atDay(1).dayOfWeek.value % 7
                    val cells: List<LocalDate?> = List(off) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            for (i in 0 until 7) {
                                val d = week.getOrNull(i)
                                Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                                    if (d != null) {
                                        val isSel = d == sel
                                        val dots = (byDate[d.toString()] ?: emptyList()).map { catStyle(it.category).third }.distinct().take(3)
                                        Column(
                                            Modifier.size(40.dp).clip(CircleShape)
                                                .background(if (isSel) Indigo else Color.Transparent)
                                                .then(if (d == todayD && !isSel) Modifier.border(1.dp, Indigo, CircleShape) else Modifier)
                                                .clickable { sel = d },
                                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                                        ) {
                                            T("${d.dayOfMonth}", 13, if (isSel || d == todayD) FontWeight.Bold else FontWeight.Medium, if (isSel) Color.White else Ink)
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
                                                dots.forEach { c -> Box(Modifier.size(4.dp).clip(CircleShape).background(if (isSel) Color.White else c)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                T("${monthName(YearMonth.from(sel))} ${sel.dayOfMonth} Agenda", 16, FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
            }
            if (dayTasks.isEmpty()) item {
                T("Sin actividades este día. Toca + para agendar.", 13, color = Muted)
            } else items(dayTasks, key = { it.id }) { t ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                        .clickable { push(Nav.TaskForm(t, null)) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(3.dp).height(36.dp).background(Indigo))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        T(t.title, 14, FontWeight.Bold, maxLines = 1)
                        T(fmtTime(t.time) + if (t.done) "  ✓ Hecha" else "", 11, color = Muted)
                    }
                    Tag(t.category)
                }
            }
        }
        FloatingActionButton(
            onClick = { push(Nav.TaskForm(null, sel.toString())) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Indigo, contentColor = Color.White, shape = CircleShape
        ) { Icon(Icons.Outlined.Add, "Nueva tarea") }
    }
}

@Composable
fun TaskFormScreen(vm: VM, task: Task?, presetDate: String?, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(task?.title ?: "") }
    var date by remember { mutableStateOf(task?.date ?: presetDate ?: today().toString()) }
    var time by remember { mutableStateOf(task?.time ?: "23:59") }
    var cat by remember { mutableStateOf(task?.category ?: "Homework") }
    var rem by remember { mutableStateOf(task?.reminder ?: true) }
    var pri by remember { mutableStateOf(task?.priority ?: "Mediana") }
    var busy by remember { mutableStateOf(false) }

    fun save(done: Boolean) {
        if (title.isBlank()) { toast(ctx, "Escribe el título de la tarea"); return }
        scope.launch {
            busy = true
            val e = vm.saveTask(ctx, Task(task?.id ?: 0, title.trim(), date, time, cat, pri, rem, done))
            busy = false
            if (e != null) toast(ctx, e) else { toast(ctx, if (task == null) "Tarea guardada ✅" else "Tarea actualizada ✅"); back() }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(20.dp)) {
        TopBar(if (task == null) "Nueva Tarea" else "Editar Tarea", back)
        Label("Titulo Tarea")
        Field(title, { title = it }, "Informe de laboratorio 4")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Label("Date")
                PickerBox(Icons.Outlined.CalendarToday, fmtDate(date)) {
                    val d = LocalDate.parse(date)
                    android.app.DatePickerDialog(ctx, { _, y, m, day -> date = LocalDate.of(y, m + 1, day).toString() }, d.year, d.monthValue - 1, d.dayOfMonth).show()
                }
            }
            Column(Modifier.weight(1f)) {
                Label("Due Time")
                PickerBox(Icons.Outlined.Schedule, fmtTime(time)) {
                    val parts = time.split(":")
                    android.app.TimePickerDialog(ctx, { _, h, mi -> time = String.format(Locale.US, "%02d:%02d", h, mi) }, parts[0].toInt(), parts[1].toInt(), false).show()
                }
            }
        }
        Label("Category")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Exam", "Homework", "Project", "Personal", "Class").forEach { Chip(it, cat == it) { cat = it } }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF4F6FB)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Notifications, null, tint = Indigo)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                T("Recuérdamelo", 13, FontWeight.Bold)
                T("Alerta 30 mins antes", 11, color = Muted)
            }
            Switch(rem, { rem = it }, colors = SwitchDefaults.colors(checkedTrackColor = Indigo))
        }
        Label("Prioridad")
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F3F8)).padding(3.dp)) {
            listOf("Baja", "Mediana", "Alta").forEach { p ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (pri == p) Color.White else Color.Transparent)
                        .clickable { pri = p }.padding(vertical = 10.dp), contentAlignment = Alignment.Center
                ) { T(p, 12, if (pri == p) FontWeight.Bold else FontWeight.Medium, if (pri == p) Indigo else Muted) }
            }
        }
        Spacer(Modifier.height(24.dp))
        BigButton("Guardar Tarea", { save(task?.done ?: false) }, loading = busy)
        if (task != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton({ save(!task.done) }, Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp)) {
                T(if (task.done) "Marcar como pendiente" else "Marcar como hecha", 13, FontWeight.Bold, Indigo)
            }
            TextButton({
                scope.launch {
                    val e = vm.deleteTask(ctx, task)
                    if (e != null) toast(ctx, e) else { toast(ctx, "Tarea eliminada"); back() }
                }
            }, Modifier.fillMaxWidth()) { T("Eliminar tarea", 13, FontWeight.Bold, Color(0xFFE11D48)) }
        }
    }
}

@Composable
fun PickerBox(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Line, RoundedCornerShape(12.dp))
        .clickable(onClick = onClick).padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(icon, null, tint = Muted, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    T(text, 13, FontWeight.Medium, maxLines = 1)
}
