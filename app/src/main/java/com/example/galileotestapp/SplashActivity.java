package com.example.galileotestapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.galileotestapp.galileo.GalileoListener;
import com.example.galileotestapp.galileo.GalileoRelevance;
import com.example.galileotestapp.galileo.utils.NotificationUtils;

public class SplashActivity extends AppCompatActivity implements GalileoListener {
    private static final String TAG = SplashActivity.class.getCanonicalName();

    private static final int LOCATION_PERMISSION_REQUEST = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);



        if (!checkLocationPermission()) {
            return;
        }

        GalileoRelevance.getInstance().init(this);
        GalileoRelevance.getInstance().setListener(this);
        GalileoRelevance.getInstance().verifyAvailability();
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return false;
        } else {
            return true;
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            GalileoRelevance.getInstance().init(this);
            GalileoRelevance.getInstance().setListener(this);
            GalileoRelevance.getInstance().verifyAvailability();
        }
    }

    @Override
    public void onGalileoNotAvailable() {
        NotificationUtils.postNotification(this, "Limited navigation performance, consider upgrading to Galileo for an optimal experience");
    }

    @Override
    public void onGalileoAvailable(int frequency) {
        if (frequency == 1) {
            //Single frequency
            NotificationUtils.postNotification(this, "Good navigation performance, consider upgrading to dual frequency");
        } else {
            //Dual frequency
            NotificationUtils.postNotification(this, "You have the best navigation performance");
        }

        GalileoRelevance.getInstance().stop();
    }
}
