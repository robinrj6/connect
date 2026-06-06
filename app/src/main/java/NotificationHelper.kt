import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "connect_notifications"
    private const val GROUP_KEY = "com.example.connect.EMOJI_GROUP"

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)

        // 1. Create Channel (Required for API 26+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Connect Interactions",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        // 2. Build the individual notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info) // Replace with your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setGroup(GROUP_KEY) // This is the magic for batching
            .setAutoCancel(true)
            .build()

        // 3. Build the Summary (The "Batch" cover)
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("New interactions received"))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true) // Marks this as the header for the batch
            .build()

        // Send individual
        notificationManager.notify(notificationId, notification)
        // Send/Update summary
        notificationManager.notify(0, summaryNotification)
    }
}