package com.example.galileotestapp.galileo.utils;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    private static NotificationHelper instance;

    private Context mContext;

    private boolean mInitialized = false;

    public static synchronized NotificationHelper getInstance(Context mContext) {
        if (instance == null) {
            instance = new NotificationHelper(mContext);
        }
        return instance;
    }

    private NotificationHelper(Context mContext) {
        this.mContext = mContext;
    }

    public enum Channel {
        /**
         * Default notification channel.
         */
        DEFAULT,
    }

    private final Map<Channel, String> mDefinedNotificationChannels = new HashMap<Channel, String>() {
        {
            final String DEFAULT_CHANNEL_TAG = "default-notification-channel";
            put(Channel.DEFAULT, DEFAULT_CHANNEL_TAG);
        }
    };

    public void init() {
        if (mInitialized) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannnels();
        }

        mInitialized = true;
    }

    private void initNotificationChannnels() {
        final NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        for (Channel channel : mDefinedNotificationChannels.keySet()) {
            createChannel(notificationManager, channel);
        }
    }

    @SuppressWarnings("fallthrough")
    @TargetApi(26)
    private void createChannel(final NotificationManager manager, Channel definedChannel) {
        NotificationChannel channel =
                manager.getNotificationChannel(mDefinedNotificationChannels.get(definedChannel));

        if (channel == null) {
            switch (definedChannel) {
                case DEFAULT:
                default: {
                    channel = new NotificationChannel(mDefinedNotificationChannels.get(definedChannel),
                            "Default Notifications",
                            NotificationManager.IMPORTANCE_HIGH);
                }
                break;
            }
            manager.createNotificationChannel(channel);
        }
    }

    @TargetApi(26)
    public NotificationChannel getNotificationChannel(Channel definedChannel) {
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager.getNotificationChannel(mDefinedNotificationChannels.get(definedChannel));
    }
}
