<?php
require __DIR__ . '/config.php';
require __DIR__ . '/mailer.php';
header('Content-Type: application/json; charset=utf-8');

$in = json_decode(file_get_contents('php://input'), true) ?: [];
$a = $in['action'] ?? '';

function out($d = []) { echo json_encode(['ok' => true] + $d, JSON_UNESCAPED_UNICODE); exit; }
function fail($m) { echo json_encode(['ok' => false, 'error' => $m], JSON_UNESCAPED_UNICODE); exit; }
function s($k) { global $in; return trim((string)($in[$k] ?? '')); }
function me() {
    global $pdo;
    $t = s('token');
    if ($t === '') fail('Sesión expirada');
    $q = $pdo->prepare('SELECT * FROM users WHERE token=?');
    $q->execute([$t]);
    $u = $q->fetch();
    if (!$u) fail('Sesión expirada');
    return $u;
}
function pub($u, $withToken = false) {
    $r = ['id' => (int)$u['id'], 'name' => $u['name'], 'email' => $u['email'], 'semester' => (int)$u['semester']];
    if ($withToken) $r['token'] = $u['token'];
    return $r;
}
function save_image($b64) {
    if ($b64 === '') return null;
    $bin = base64_decode($b64, true);
    if ($bin === false || strlen($bin) > 8 * 1024 * 1024) fail('Imagen inválida');
    $img = @imagecreatefromstring($bin);
    if (!$img) fail('Imagen inválida');
    if (!is_dir(__DIR__ . '/uploads')) mkdir(__DIR__ . '/uploads', 0777, true);
    $name = 'img_' . bin2hex(random_bytes(8)) . '.jpg';
    imagejpeg($img, __DIR__ . '/uploads/' . $name, 85);
    imagedestroy($img);
    return 'uploads/' . $name;
}

// ---------- AUTH ----------
if ($a === 'register') {
    $name = s('name'); $email = strtolower(s('email')); $pass = (string)($in['password'] ?? '');
    $sem = max(1, min(12, (int)($in['semester'] ?? 1)));
    if (mb_strlen($name) < 2) fail('Escribe tu nombre completo');
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) fail('Correo no válido');
    if (ALLOWED_DOMAIN !== '' && substr($email, -strlen('@' . ALLOWED_DOMAIN)) !== '@' . ALLOWED_DOMAIN)
        fail('Usa tu correo institucional @' . ALLOWED_DOMAIN);
    if (strlen($pass) < 6) fail('La contraseña debe tener mínimo 6 caracteres');
    $q = $pdo->prepare('SELECT id FROM users WHERE email=?'); $q->execute([$email]);
    if ($q->fetch()) fail('Ese correo ya tiene una cuenta');
    $pdo->prepare('INSERT INTO users(name,email,password,semester) VALUES(?,?,?,?)')
        ->execute([$name, $email, password_hash($pass, PASSWORD_DEFAULT), $sem]);
    send_mail($email, '¡Bienvenido a VAI!', mail_tpl('¡Hola ' . htmlspecialchars($name) . '!',
        '<p>Tu cuenta de VAI fue creada correctamente. Ya puedes iniciar sesión, organizar tus tareas y usar Campus Market.</p>'));
    out();
}

if ($a === 'login') {
    $email = strtolower(s('email')); $pass = (string)($in['password'] ?? '');
    $q = $pdo->prepare('SELECT * FROM users WHERE email=?'); $q->execute([$email]);
    $u = $q->fetch();
    if (!$u || !password_verify($pass, $u['password'])) fail('Correo o contraseña incorrectos');
    $u['token'] = bin2hex(random_bytes(24));
    $pdo->prepare('UPDATE users SET token=? WHERE id=?')->execute([$u['token'], $u['id']]);
    send_mail($u['email'], 'Nuevo inicio de sesión en VAI', mail_tpl('Inicio de sesión',
        '<p>Hola ' . htmlspecialchars($u['name']) . ', iniciaste sesión en VAI el <b>' . date('d/m/Y') . '</b> a las <b>' . date('h:i A')
        . '</b> (hora de Bogotá).</p><p>Si no fuiste tú, cambia tu contraseña.</p>'));
    out(['user' => pub($u, true)]);
}

if ($a === 'logout') {
    $u = me();
    $pdo->prepare('UPDATE users SET token=NULL WHERE id=?')->execute([$u['id']]);
    out();
}

if ($a === 'update_profile') {
    $u = me();
    $name = s('name'); $sem = max(1, min(12, (int)($in['semester'] ?? 1)));
    if (mb_strlen($name) < 2) fail('Escribe tu nombre');
    $pdo->prepare('UPDATE users SET name=?, semester=? WHERE id=?')->execute([$name, $sem, $u['id']]);
    $u['name'] = $name; $u['semester'] = $sem;
    out(['user' => pub($u, true)]);
}

// ---------- TAREAS ----------
if ($a === 'tasks_list') {
    $u = me();
    $q = $pdo->prepare('SELECT id,title,due_date AS date,TIME_FORMAT(due_time,"%H:%i") AS time,category,priority,reminder,done FROM tasks WHERE user_id=? ORDER BY due_date,due_time');
    $q->execute([$u['id']]);
    out(['tasks' => $q->fetchAll()]);
}
if ($a === 'task_save') {
    $u = me();
    $id = (int)($in['id'] ?? 0); $title = s('title'); $date = s('date'); $time = s('time');
    $cat = in_array(s('category'), ['Exam', 'Homework', 'Project', 'Personal', 'Class']) ? s('category') : 'Homework';
    $pri = in_array(s('priority'), ['Baja', 'Mediana', 'Alta']) ? s('priority') : 'Mediana';
    $rem = !empty($in['reminder']) ? 1 : 0; $done = !empty($in['done']) ? 1 : 0;
    if ($title === '') fail('Escribe el título de la tarea');
    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date) || !preg_match('/^\d{2}:\d{2}$/', $time)) fail('Fecha u hora inválida');
    if ($id) {
        $pdo->prepare('UPDATE tasks SET title=?,due_date=?,due_time=?,category=?,priority=?,reminder=?,done=? WHERE id=? AND user_id=?')
            ->execute([$title, $date, $time, $cat, $pri, $rem, $done, $id, $u['id']]);
    } else {
        $pdo->prepare('INSERT INTO tasks(user_id,title,due_date,due_time,category,priority,reminder,done) VALUES(?,?,?,?,?,?,?,?)')
            ->execute([$u['id'], $title, $date, $time, $cat, $pri, $rem, $done]);
    }
    out();
}
if ($a === 'task_delete') {
    $u = me();
    $pdo->prepare('DELETE FROM tasks WHERE id=? AND user_id=?')->execute([(int)($in['id'] ?? 0), $u['id']]);
    out();
}

// ---------- MARKETPLACE ----------
if ($a === 'items_list') {
    me();
    $r = $pdo->query('SELECT i.id,i.title,i.description,i.category,i.price,i.cond,i.image,i.sold,i.user_id AS seller_id,u.name AS seller_name FROM items i JOIN users u ON u.id=i.user_id ORDER BY i.sold, i.id DESC');
    out(['items' => $r->fetchAll()]);
}
if ($a === 'item_create') {
    $u = me();
    $title = s('title'); $price = (int)($in['price'] ?? 0);
    if ($title === '') fail('Escribe el nombre del artículo');
    if ($price <= 0) fail('Escribe un precio válido');
    $img = save_image((string)($in['image'] ?? ''));
    $pdo->prepare('INSERT INTO items(user_id,title,description,category,price,cond,image) VALUES(?,?,?,?,?,?,?)')
        ->execute([$u['id'], $title, s('description'), s('category') ?: 'Otros', $price, s('cond') ?: 'Buen estado', $img]);
    out();
}
if ($a === 'item_sold') {
    $u = me();
    $pdo->prepare('UPDATE items SET sold=1 WHERE id=? AND user_id=?')->execute([(int)($in['id'] ?? 0), $u['id']]);
    out();
}
if ($a === 'item_delete') {
    $u = me(); $id = (int)($in['id'] ?? 0);
    $q = $pdo->prepare('SELECT image FROM items WHERE id=? AND user_id=?'); $q->execute([$id, $u['id']]);
    if ($row = $q->fetch()) {
        if ($row['image'] && file_exists(__DIR__ . '/' . $row['image'])) @unlink(__DIR__ . '/' . $row['image']);
        $pdo->prepare('DELETE FROM items WHERE id=? AND user_id=?')->execute([$id, $u['id']]);
    }
    out();
}

// ---------- INTERCAMBIOS ----------
if ($a === 'swaps_list') {
    me();
    $r = $pdo->query('SELECT w.id,w.title,w.description,w.category,w.wanted,w.image,w.user_id,u.name AS user_name FROM swaps w JOIN users u ON u.id=w.user_id ORDER BY w.id DESC');
    out(['swaps' => $r->fetchAll()]);
}
if ($a === 'swap_create') {
    $u = me();
    if (s('title') === '') fail('Escribe qué ofreces');
    if (s('wanted') === '') fail('Escribe por qué lo quieres cambiar');
    $img = save_image((string)($in['image'] ?? ''));
    $pdo->prepare('INSERT INTO swaps(user_id,title,description,category,wanted,image) VALUES(?,?,?,?,?,?)')
        ->execute([$u['id'], s('title'), s('description'), s('category') ?: 'Otros', s('wanted'), $img]);
    out();
}
if ($a === 'swap_delete') {
    $u = me();
    $pdo->prepare('DELETE FROM swaps WHERE id=? AND user_id=?')->execute([(int)($in['id'] ?? 0), $u['id']]);
    out();
}

// ---------- MENSAJES ----------
if ($a === 'chat_get') {
    $u = me(); $w = (int)($in['with'] ?? 0);
    $q = $pdo->prepare('SELECT m.id,m.from_id,x.name AS from_name,m.body AS text,DATE_FORMAT(m.created_at,"%d/%m %H:%i") AS at FROM messages m JOIN users x ON x.id=m.from_id WHERE m.room IS NULL AND ((m.from_id=? AND m.to_id=?) OR (m.from_id=? AND m.to_id=?)) ORDER BY m.id');
    $q->execute([$u['id'], $w, $w, $u['id']]);
    out(['messages' => $q->fetchAll()]);
}
if ($a === 'chat_send') {
    $u = me(); $to = (int)($in['to'] ?? 0); $text = s('text');
    if ($text === '') fail('Escribe un mensaje');
    if ($to === (int)$u['id'] || $to <= 0) fail('Destinatario inválido');
    $pdo->prepare('INSERT INTO messages(from_id,to_id,subject,body) VALUES(?,?,?,?)')->execute([$u['id'], $to, s('subject'), $text]);
    out();
}
if ($a === 'inbox') {
    $u = me(); $me = (int)$u['id'];
    $r = $pdo->query("SELECT m.id, m.subject, m.body AS last, x.id AS other_id, x.name AS other_name FROM messages m JOIN users x ON x.id = IF(m.from_id=$me, m.to_id, m.from_id) WHERE m.room IS NULL AND (m.from_id=$me OR m.to_id=$me) AND m.id IN (SELECT MAX(id) FROM messages WHERE room IS NULL AND (from_id=$me OR to_id=$me) GROUP BY IF(from_id=$me, to_id, from_id)) ORDER BY m.id DESC");
    out(['convs' => $r->fetchAll()]);
}
if ($a === 'room_get') {
    me(); $room = s('room');
    $q = $pdo->prepare('SELECT m.id,m.from_id,x.name AS from_name,m.body AS text,DATE_FORMAT(m.created_at,"%d/%m %H:%i") AS at FROM messages m JOIN users x ON x.id=m.from_id WHERE m.room=? ORDER BY m.id DESC LIMIT 100');
    $q->execute([$room]);
    out(['messages' => array_reverse($q->fetchAll())]);
}
if ($a === 'room_send') {
    $u = me(); $text = s('text');
    if ($text === '' || s('room') === '') fail('Mensaje vacío');
    $pdo->prepare('INSERT INTO messages(from_id,room,body) VALUES(?,?,?)')->execute([$u['id'], s('room'), $text]);
    out();
}

fail('Acción desconocida');
