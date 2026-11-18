<?php
// Cliente SMTP mínimo (STARTTLS + AUTH LOGIN) sin librerías externas.
function mail_log($m) { @file_put_contents(__DIR__ . '/mail_log.txt', date('c') . ' ' . $m . "\n", FILE_APPEND); }

function send_mail($to, $subject, $html) {
    if (SMTP_USER === '' || SMTP_PASS === '') { mail_log("[SIN SMTP] a $to: $subject"); return false; }
    try {
        $ctx = stream_context_create(['ssl' => ['verify_peer' => false, 'verify_peer_name' => false]]);
        $fp = @stream_socket_client('tcp://' . SMTP_HOST . ':' . SMTP_PORT, $en, $es, 15, STREAM_CLIENT_CONNECT, $ctx);
        if (!$fp) { mail_log("conexión SMTP falló: $es"); return false; }
        stream_set_timeout($fp, 15);
        $rd = function () use ($fp) { $r = ''; while (($l = fgets($fp, 515)) !== false) { $r .= $l; if (isset($l[3]) && $l[3] === ' ') break; } return $r; };
        $cmd = function ($c, $ok) use ($fp, $rd) { fwrite($fp, $c . "\r\n"); return strpos($rd(), (string)$ok) === 0; };
        $rd();
        $cmd('EHLO localhost', 250);
        if (!$cmd('STARTTLS', 220)) { mail_log('STARTTLS rechazado'); fclose($fp); return false; }
        stream_socket_enable_crypto($fp, true, STREAM_CRYPTO_METHOD_TLS_CLIENT);
        $cmd('EHLO localhost', 250);
        $cmd('AUTH LOGIN', 334);
        $cmd(base64_encode(SMTP_USER), 334);
        if (!$cmd(base64_encode(SMTP_PASS), 235)) { mail_log('Login SMTP rechazado (revisa la contraseña de aplicación)'); fclose($fp); return false; }
        $cmd('MAIL FROM:<' . SMTP_USER . '>', 250);
        if (!$cmd('RCPT TO:<' . $to . '>', 250)) { mail_log("destinatario rechazado: $to"); fclose($fp); return false; }
        $cmd('DATA', 354);
        $h = 'From: =?UTF-8?B?' . base64_encode(MAIL_FROM_NAME) . '?= <' . SMTP_USER . ">\r\n"
           . "To: <$to>\r\n"
           . 'Subject: =?UTF-8?B?' . base64_encode($subject) . "?=\r\n"
           . 'Date: ' . date('r') . "\r\nMIME-Version: 1.0\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n";
        fwrite($fp, $h . chunk_split(base64_encode($html)) . "\r\n.\r\n");
        $ok = strpos($rd(), '250') === 0;
        $cmd('QUIT', 221);
        fclose($fp);
        mail_log(($ok ? 'ENVIADO' : 'FALLÓ') . " a $to: $subject");
        return $ok;
    } catch (Throwable $e) { mail_log('excepción: ' . $e->getMessage()); return false; }
}

function mail_tpl($title, $body) {
    return '<div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;border:1px solid #e6eaf2;border-radius:16px;overflow:hidden">'
         . '<div style="background:#5b5fef;color:#fff;padding:20px;font-size:22px;font-weight:bold">VAI</div>'
         . '<div style="padding:24px;color:#0f172a"><h2 style="margin-top:0">' . $title . '</h2>' . $body
         . '<p style="color:#8793a8;font-size:12px;margin-top:24px">Academic Planner &amp; Marketplace · Universidad ECCI</p></div></div>';
}
