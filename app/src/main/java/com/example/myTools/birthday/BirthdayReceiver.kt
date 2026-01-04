package com.example.myTools.birthday


import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myTools.MainActivity

//農曆生日

class BirthdayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "親友"
        val message = intent.getStringExtra("message") ?: "農曆生日快到了！"
        val notificationId = intent.getIntExtra("id", 0)

        // ★ 加入 Log，請在 Logcat 搜尋 "BirthdayReceiver" ★
        Log.d("BirthdayReceiver", "收到廣播！準備發送通知: $name, $message")

        showNotification(context, name, message, notificationId)
    }

    private fun showNotification(context: Context, title: String, message: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "birthday_channel"

        // 1. 創建通知頻道 (Android 8.0+ 必須)
        val channel = NotificationChannel(channelId, "生日提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "農曆生日提醒通知"
        }
        notificationManager.createNotificationChannel(channel)

        // 2. 點擊通知後打開 App
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. 構建通知
        val notification = NotificationCompat.Builder(context, channelId)
            // 確保這裡有一個有效的 icon，否則通知會崩潰
            .setSmallIcon(R.drawable.ic_menu_my_calendar) // 這裡暫時用系統圖標，你可以換成 R.drawable.ic_cake
            .setContentTitle("🎂 $title 生日提醒")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}