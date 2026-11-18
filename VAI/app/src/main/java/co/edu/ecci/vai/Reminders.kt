package co.edu.ecci.vai

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object Reminders {
    const val CH = "vai_tareas"

    fun channel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CH, "Recordatorios de tareas", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun pi(ctx: Context, id: Int, title: String, time: String): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).putExtra("title", title).putExtra("time", time).putExtra("id", id)
        return PendingIntent.getBroadcast(ctx, id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun cancel(ctx: Context, id: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(ctx, id, "", ""))
    }

    /** Programa la alerta 30 minutos antes de la entrega (hora de Bogotá). */
    fun schedule(ctx: Context, t: Task) {
        cancel(ctx, t.id)
        if (!t.reminder || t.done) return
        val whenMs = try {
            LocalDateTime.of(LocalDate.parse(t.date), LocalTime.parse(t.time))
                .atZone(BOG).minusMinutes(30).toInstant().toEpochMilli()
        } catch (e: Exception) { return }
        if (whenMs <= System.currentTimeMillis()) return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi(ctx, t.id, t.title, t.time))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Reminders.channel(c)
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(c, Reminders.CH)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ " + (i.getStringExtra("title") ?: "Tarea"))
            .setContentText("Vence a las " + (i.getStringExtra("time") ?: "") + " (en 30 minutos)")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(i.getIntExtra("id", 0), n)
    }
}
