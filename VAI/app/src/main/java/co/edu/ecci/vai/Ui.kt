package co.edu.ecci.vai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.util.Locale

val Indigo = Color(0xFF5B5FEF)
val Bg = Color(0xFFF4F8FB)
val Ink = Color(0xFF0F172A)
val Muted = Color(0xFF8793A8)
val Line = Color(0xFFE6EAF2)
val Soft = Color(0xFFE8EAFE)

@Composable
fun VaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Indigo, background = Bg, surface = Color.White, onSurface = Ink),
        content = content
    )
}

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

fun money(n: Int): String = "$" + NumberFormat.getInstance(Locale("es", "CO")).format(n)

@Composable
fun T(
    text: String, size: Int = 14, weight: FontWeight = FontWeight.Normal, color: Color = Ink,
    modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE, align: TextAlign? = null
) = Text(
    text, modifier = modifier, fontSize = size.sp, fontWeight = weight, color = color,
    maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = align
)

@Composable
fun Field(
    value: String, onChange: (String) -> Unit, placeholder: String = "",
    modifier: Modifier = Modifier.fillMaxWidth(), password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text, trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null, singleLine: Boolean = true, minLines: Int = 1
) = OutlinedTextField(
    value = value, onValueChange = onChange, modifier = modifier,
    placeholder = { T(placeholder, 14, color = Muted) },
    singleLine = singleLine, minLines = minLines, shape = RoundedCornerShape(12.dp),
    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    trailingIcon = trailing, leadingIcon = leading,
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Indigo, unfocusedBorderColor = Line,
        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
    )
)

@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) = Button(
    onClick = onClick, modifier = modifier.fillMaxWidth().height(50.dp), enabled = !loading,
    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Indigo)
) {
    if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
    else T(text, 15, FontWeight.Bold, Color.White)
}

@Composable
fun Chip(text: String, sel: Boolean, onClick: () -> Unit) = Box(
    Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) Indigo else Color.White)
        .border(1.dp, if (sel) Indigo else Line, RoundedCornerShape(20.dp))
        .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
) { T(text, 12, FontWeight.Medium, if (sel) Color.White else Ink) }

@Composable
fun Label(text: String) = T(text, 12, FontWeight.SemiBold, Ink, Modifier.padding(top = 14.dp, bottom = 6.dp))

@Composable
fun TopBar(title: String, back: () -> Unit, trailing: (@Composable () -> Unit)? = null) = Box(
    Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center
) {
    IconButton(back, Modifier.align(Alignment.CenterStart)) { Icon(Icons.Outlined.ArrowBack, "Volver", tint = Ink) }
    T(title, 17, FontWeight.Bold)
    if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
}

@Composable
fun Avatar(name: String, size: Int = 36) = Box(
    Modifier.size(size.dp).clip(CircleShape).background(Soft), contentAlignment = Alignment.Center
) { T(name.trim().take(1).uppercase().ifEmpty { "?" }, (size / 2.4).toInt(), FontWeight.Bold, Indigo) }

fun catStyle(c: String): Triple<String, Color, Color> = when (c) {
    "Exam" -> Triple("EXAMEN", Color(0xFFFDE8EC), Color(0xFFE11D48))
    "Homework" -> Triple("TAREA", Color(0xFFFEF3C7), Color(0xFFB45309))
    "Project" -> Triple("PROYECTO", Color(0xFFDCFCE7), Color(0xFF15803D))
    "Class" -> Triple("CLASE", Color(0xFFFDE8EC), Color(0xFFE11D48))
    else -> Triple("PERSONAL", Soft, Indigo)
}

@Composable
fun Tag(category: String) {
    val (label, bg, fg) = catStyle(category)
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        T(label, 9, FontWeight.Bold, fg)
    }
}

fun catEmoji(c: String) = when (c) {
    "Electrónica" -> "🧮"; "Vestir" -> "👕"; "Laboratorio" -> "🔬"; "Suministros" -> "📐"
    "Libros" -> "📚"; "Apuntes de clase" -> "📝"; "Equipo de laboratorio" -> "🔬"; "Calculadoras" -> "🧮"
    else -> "📦"
}

@Composable
fun Pic(path: String, cat: String, modifier: Modifier) {
    if (path.isNotBlank()) AsyncImage(
        model = HOST + path, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier
    ) else Box(modifier.background(Color(0xFFEAEFF7)), contentAlignment = Alignment.Center) {
        Text(catEmoji(cat), fontSize = 34.sp)
    }
}

// ---------- Cámara / galería ----------
fun loadBitmap(ctx: Context, u: Uri): Bitmap? = try {
    val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, b) }
    var s = 1
    while (b.outWidth / s > 1400 || b.outHeight / s > 1400) s *= 2
    val o = BitmapFactory.Options().apply { inSampleSize = s }
    val bmp = ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
    val ori = ctx.contentResolver.openInputStream(u)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    val deg = when (ori) { ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f; ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f }
    if (bmp != null && deg != 0f) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true) else bmp
} catch (e: Exception) { null }

fun Bitmap.toB64(): String {
    val o = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, o)
    return Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP)
}

/** Caja de foto: abre la CÁMARA al tocarla, o la galería con el segundo botón. */
@Composable
fun PhotoBox(bitmap: Bitmap?, onBitmap: (Bitmap) -> Unit) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) uri?.let { u -> loadBitmap(ctx, u)?.let(onBitmap) }
    }
    val gal = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        u?.let { x -> loadBitmap(ctx, x)?.let(onBitmap) }
    }
    fun openCamera() {
        try {
            val dir = File(ctx.cacheDir, "photos").apply { mkdirs() }
            val f = File(dir, "p_${System.currentTimeMillis()}.jpg")
            val u = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
            uri = u
            cam.launch(u)
        } catch (e: Exception) { toast(ctx, "No se pudo abrir la cámara") }
    }
    Column {
        Box(
            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFEEF1FA))
                .border(1.dp, Line, RoundedCornerShape(16.dp)).clickable { openCamera() },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.CameraAlt, null, tint = Indigo, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(6.dp)); T("Toca para tomar una foto", 13, color = Muted)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ openCamera() }, Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Outlined.CameraAlt, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); T(if (bitmap == null) "Cámara" else "Repetir foto", 12, color = Indigo)
            }
            OutlinedButton({ gal.launch("image/*") }, Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Outlined.Image, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); T("Galería", 12, color = Indigo)
            }
        }
    }
}
