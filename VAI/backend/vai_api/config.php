<?php
// ====== CONFIGURACIÓN (XAMPP por defecto: root sin contraseña) ======
date_default_timezone_set('America/Bogota');
const DB_HOST = 'localhost';
const DB_NAME = 'vai_db';
const DB_USER = 'root';
const DB_PASS = '';

// Correo (Gmail): activa verificación en 2 pasos y crea una "Contraseña de aplicación".
// Si dejas SMTP_USER vacío, los correos NO se envían: se anotan en mail_log.txt.
const SMTP_HOST = 'smtp.gmail.com';
const SMTP_PORT = 587;
const SMTP_USER = '';   // ej: tucorreo@gmail.com
const SMTP_PASS = '';   // contraseña de aplicación de 16 letras
const MAIL_FROM_NAME = 'VAI';

// Si quieres obligar el correo institucional escribe 'ecci.edu.co'. Vacío = cualquier correo.
const ALLOWED_DOMAIN = '';

try {
    $pdo = new PDO('mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4', DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    $pdo->exec("SET time_zone='-05:00'"); // Bogotá
} catch (Exception $e) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['ok' => false, 'error' => 'No hay conexión con MySQL. ¿Está iniciado XAMPP y creaste vai_db?']);
    exit;
}
