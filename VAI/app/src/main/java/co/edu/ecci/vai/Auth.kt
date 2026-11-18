package co.edu.ecci.vai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(vm: VM, prefill: String, onCreate: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(prefill) }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Indigo), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.MenuBook, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(10.dp))
        T("VAI", 28, FontWeight.ExtraBold)
        T("Your Academic Planner & Marketplace", 13, color = Muted)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp)) {
            T("Bienvenido", 20, FontWeight.Bold)
            T("Inicia sesión para acceder a tus clases y a la tienda.", 13, color = Muted)
            Label("Correo Institucional")
            Field(email, { email = it }, "pacho@ecci.edu.co", keyboard = KeyboardType.Email)
            Label("Contraseña")
            Field(pass, { pass = it }, "••••••••", password = !show, keyboard = KeyboardType.Password, trailing = {
                IconButton({ show = !show }) { Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Indigo) }
            })
            T(
                if (show) "Ocultar contraseña" else "Mostrar contraseña?", 12, FontWeight.Medium, Indigo,
                Modifier.align(Alignment.End).padding(vertical = 8.dp).clickable { show = !show }
            )
            Spacer(Modifier.height(4.dp))
            BigButton("Entrar", {
                if (email.isBlank() || pass.isBlank()) { toast(ctx, "Escribe tu correo y contraseña"); return@BigButton }
                scope.launch {
                    busy = true
                    val e = vm.login(email.trim(), pass)
                    busy = false
                    if (e != null) toast(ctx, e) else toast(ctx, "¡Bienvenido! Te enviamos un correo con la confirmación de tu inicio de sesión.")
                }
            }, loading = busy)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton({ toast(ctx, "El acceso con Google requiere configurar OAuth; por ahora usa tu correo.") }, Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp)) { T("Google", 13, FontWeight.Medium) }
                OutlinedButton({ toast(ctx, "El acceso con Apple solo está disponible en iOS.") }, Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp)) { T("Apple", 13, FontWeight.Medium) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row {
            T("¿No tienes una cuenta? ", 13, color = Muted)
            T("Crear", 13, FontWeight.Bold, Indigo, Modifier.clickable { onCreate() })
        }
    }
}

@Composable
fun RegisterScreen(vm: VM, onDone: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var sem by remember { mutableIntStateOf(1) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(24.dp)) {
        TopBar("Crear cuenta", onBack)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp)) {
            T("Únete a VAI", 20, FontWeight.Bold)
            T("Tus datos se guardan para que puedas iniciar sesión.", 13, color = Muted)
            Label("Nombre completo")
            Field(name, { name = it }, "Julian Acevedo")
            Label("Correo Institucional")
            Field(email, { email = it }, "pacho@ecci.edu.co", keyboard = KeyboardType.Email)
            Label("Contraseña (mínimo 6 caracteres)")
            Field(pass, { pass = it }, "••••••••", password = true, keyboard = KeyboardType.Password)
            Label("Confirmar contraseña")
            Field(pass2, { pass2 = it }, "••••••••", password = true, keyboard = KeyboardType.Password)
            Label("Semestre activo")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..10).forEach { Chip("$it", sem == it) { sem = it } }
            }
            Spacer(Modifier.height(20.dp))
            BigButton("Crear cuenta", {
                when {
                    name.isBlank() -> toast(ctx, "Escribe tu nombre")
                    pass != pass2 -> toast(ctx, "Las contraseñas no coinciden")
                    else -> scope.launch {
                        busy = true
                        val e = vm.register(name.trim(), email.trim(), pass, sem)
                        busy = false
                        if (e != null) toast(ctx, e) else { toast(ctx, "Cuenta creada ✅ Revisa tu correo e inicia sesión."); onDone(email.trim()) }
                    }
                }
            }, loading = busy)
        }
        Spacer(Modifier.height(20.dp))
        T("Volver a iniciar sesión", 13, FontWeight.Bold, Indigo, Modifier.fillMaxWidth().clickable { onBack() }, align = TextAlign.Center)
    }
}
