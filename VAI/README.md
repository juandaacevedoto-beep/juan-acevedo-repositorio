# VAI – App Android (Kotlin + Jetpack Compose) con backend XAMPP / MySQL

## 1) Base de datos y API (XAMPP)
1. Abre **XAMPP Control Panel** → inicia **Apache** y **MySQL**.
2. Copia la carpeta `backend/vai_api` dentro de `C:\xampp\htdocs\` (queda `C:\xampp\htdocs\vai_api`).
3. Entra a http://localhost/phpmyadmin → pestaña **Importar** → elige `backend/vai_api/setup.sql` → **Continuar**.
   (Crea la base `vai_db`, las tablas y unos artículos demo.)
4. Prueba en el navegador: http://localhost/vai_api/ → debe decir "VAI API funcionando ✅".

## 2) Correo al iniciar sesión (Gmail)
En `vai_api/config.php` rellena `SMTP_USER` (tu Gmail) y `SMTP_PASS` (**contraseña de aplicación** de 16 letras:
Cuenta Google → Seguridad → Verificación en 2 pasos → Contraseñas de aplicaciones).
Si lo dejas vacío la app funciona igual y los correos solo se anotan en `vai_api/mail_log.txt`.
Se envía correo al **crear cuenta** y en **cada inicio de sesión** (con hora de Bogotá).

## 3) Android Studio
1. File → Open → carpeta `VAI` (la raíz de este proyecto). Espera el *Gradle Sync* (necesita internet).
2. Emulador: no cambies nada (`10.0.2.2` = tu PC). **Celular real**: en `Api.kt` cambia `HOST` por
   `http://IP_DE_TU_PC/vai_api/` (PC y celular en la misma WiFi; permite Apache en el firewall).
3. Run ▶. Crea una cuenta con **Crear** → inicia sesión.

## Qué es funcional
- Registro/login con contraseña cifrada en MySQL + correo de confirmación.
- Calendario mensual con hora de **America/Bogota**, crear/editar/eliminar/marcar tareas, recordatorio local 30 min antes.
- Ventas: publicar artículo con **cámara** (o galería), buscar, filtrar, detalle, mensaje al vendedor, marcar vendido, eliminar.
- Intercambio: publicar, buscar, filtrar, proponer intercambio por chat.
- Sala de socialización (chat grupal por semestre), mensajes en Perfil, editar perfil, cerrar sesión.
- Los botones Google/Apple del login solo muestran un aviso (requieren OAuth).
