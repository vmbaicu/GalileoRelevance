package com.example.galileotestapp.galileo.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import com.example.galileotestapp.R;
import com.example.galileotestapp.SplashActivity;

public class NotificationUtils {

    public static void postNotification(Context context, String message) {
        Notification.Builder notificationBuilder = new Notification.Builder(context);

        Intent intent = new Intent(context, SplashActivity.class);
        String channelId = null;
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = NotificationHelper.getInstance(context)
                    .getNotificationChannel(NotificationHelper.Channel.DEFAULT).getId();
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        notificationBuilder
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message)
                .setSound(defaultSoundUri);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelId != null) {
            notificationBuilder.setChannelId(channelId);
        }

        Notification notification = notificationBuilder.build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(172, notification);
    }
}
