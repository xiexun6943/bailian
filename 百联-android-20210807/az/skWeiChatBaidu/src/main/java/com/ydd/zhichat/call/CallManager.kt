package com.ydd.zhichat.call

import android.app.*
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.widget.RemoteViews
import com.ydd.zhichat.MyApplication
import com.ydd.zhichat.R

/**
 * Created by Administrator on 2018/9/10 0010.
 */
class CallManager {
    companion object {
        const val CALL = 0
        const val RECEIVE_CALL = 1
        const val NOTIFICATION_CALL = 10000

        const val TYPE_CALL_AUDIO = 0
        const val TYPE_CALL_VEDIO = 1

        const val isShowVideoNotify = true




        fun showCallNotification(context: Context,
                             myUid: String,
                             friendUid: String,
                             myName: String,
                             friendName: String,
                             channel: String,
                             appid: String,
                             token: String,
                             callOrReceive: Int,
                             content: String, bitmap: Bitmap?, isVideo: Boolean): Notification {
            val view = RemoteViews(context.packageName, R.layout.notification_call)
            view.setTextViewText(R.id.tv_nikeName, friendName)
            view.setTextViewText(R.id.tv_content, content)
            if (bitmap == null) {
                view.setImageViewResource(R.id.img_icon, R.drawable.ic_head)
            } else {
                view.setImageViewBitmap(R.id.img_icon, bitmap)
            }

            val intent = if (isVideo) {
                view.setImageViewResource(R.id.img_logo, R.drawable.bar_menu_call_video)
                Intent(MyApplication.getInstance(), ImVideoCallActivity::class.java)
            } else {
                view.setImageViewResource(R.id.img_logo, R.drawable.ic_call)
                Intent(MyApplication.getInstance(), ImVoiceCallActivity::class.java)
            }
            intent.putExtra("myUid",myUid)
            intent.putExtra("friendUid",friendUid)
            intent.putExtra("myName",myName)
            intent.putExtra("channel",channel)
            intent.putExtra("appid",appid)
            intent.putExtra("token",token)
            intent.putExtra("callOrReceive",callOrReceive)
            intent.putExtra("friendName",friendName)
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val intentPend = PendingIntent.getActivity(MyApplication.getInstance(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val channelId = "callId"
            val builder = NotificationCompat.Builder(context, channelId)
                    .setCustomContentView(view)
                    .setContentTitle(friendName)
                    .setContentText(content)
                    .setContentIntent(intentPend)
                    .setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(NotificationChannel(channelId, "VoiceOrVideoMsg", NotificationManager.IMPORTANCE_HIGH))
                builder
                        .setWhen(System.currentTimeMillis())
                        .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                        .build()
            } else {
                builder.build()
            }
            notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_ONLY_ALERT_ONCE
            notificationManager.notify(NOTIFICATION_CALL, notification)
            notificationManager.cancel(friendUid.toInt())
            return notification
        }

        fun closeCallNotification(context: Context) {
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_CALL)
        }
    }
}