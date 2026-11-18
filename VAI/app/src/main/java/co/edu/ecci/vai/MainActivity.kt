package co.edu.ecci.vai

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Nav {
    data class TaskForm(val task: Task?, val date: String?) : Nav()
    data class ItemDetail(val id: Int) : Nav()
    object NewItem : Nav()
    object NewSwap : Nav()
    data class SwapDetail(val id: Int) : Nav()
    object Room : Nav()
    data class Chat(val toId: Int, val toName: String, val subject: String) : Nav()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Session.init(this)
        Reminders.channel(this)
        setContent { VaiTheme { Surface(Modifier.fillMaxSize(), color = Bg) { App() } } }
    }
}

@Composable
fun App(vm: VM = viewModel()) {
    var registering by remember { mutableStateOf(false) }
    var prefill by remember { mutableStateOf("") }
    when {
        vm.user != null -> MainShell(vm)
        registering -> RegisterScreen(vm, onDone = { prefill = it; registering = false }, onBack = { registering = false })
        else -> LoginScreen(vm, prefill) { registering = true }
    }
}

private val tabs: List<Pair<String, ImageVector>> = listOf(
    "Casa" to Icons.Outlined.Home,
    "Calendario" to Icons.Outlined.CalendarMonth,
    "Intercambio" to Icons.Outlined.SwapHoriz,
    "Ventas" to Icons.Outlined.ShoppingBag,
    "Perfil" to Icons.Outlined.Person
)

@Composable
fun MainShell(vm: VM) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var stack by remember { mutableStateOf(listOf<Nav>()) }
    fun push(n: Nav) { stack = stack + n }
    fun pop() { stack = stack.dropLast(1) }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) perm.launch(Manifest.permission.POST_NOTIFICATIONS)
        vm.refreshAll(ctx)
    }
    // Refresca la sección al entrar o al volver de una pantalla secundaria
    LaunchedEffect(tab, stack.size) {
        when (tab) {
            1 -> vm.loadTasks(ctx)
            2 -> vm.loadSwaps()
            3 -> vm.loadItems()
            4 -> { vm.loadConvs(); vm.loadItems() }
            else -> vm.loadTasks(ctx)
        }
    }
    BackHandler(enabled = stack.isNotEmpty() || tab != 0) { if (stack.isNotEmpty()) pop() else tab = 0 }

    val top = stack.lastOrNull()
    if (top != null) {
        when (top) {
            is Nav.TaskForm -> TaskFormScreen(vm, top.task, top.date, ::pop)
            is Nav.ItemDetail -> ItemDetailScreen(vm, top.id, ::push, ::pop)
            is Nav.NewItem -> NewItemScreen(vm, ::pop)
            is Nav.NewSwap -> NewSwapScreen(vm, ::pop)
            is Nav.SwapDetail -> SwapDetailScreen(vm, top.id, ::push, ::pop)
            is Nav.Room -> ChatScreen(vm, "Sala de socialización", "Semestre ${vm.user?.semester ?: 1}", "Semestre ${vm.user?.semester ?: 1}", 0, ::pop)
            is Nav.Chat -> ChatScreen(vm, top.toName, top.subject, null, top.toId, ::pop)
        }
    } else {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    tabs.forEachIndexed { i, (label, icon) ->
                        NavigationBarItem(
                            selected = tab == i, onClick = { tab = i },
                            icon = { Icon(icon, label) }, label = { T(label, 10, FontWeight.Medium, if (tab == i) Indigo else Muted) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Indigo, unselectedIconColor = Muted, indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    0 -> HomeScreen(vm, { tab = it }, ::push)
                    1 -> CalendarScreen(vm, ::push)
                    2 -> SwapScreen(vm, ::push)
                    3 -> MarketScreen(vm, ::push)
                    else -> ProfileScreen(vm, ::push)
                }
            }
        }
    }
}
