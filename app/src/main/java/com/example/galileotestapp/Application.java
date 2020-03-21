package com.example.galileotestapp;

import com.example.galileotestapp.galileo.utils.NotificationHelper;

public class Application extends android.app.Application {
    private static Application mApp;

    public static Application get() {
        return mApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
        NotificationHelper.getInstance(this).init();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mApp = null;
    }
}
