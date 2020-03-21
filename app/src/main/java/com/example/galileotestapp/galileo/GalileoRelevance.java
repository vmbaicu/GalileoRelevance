package com.example.galileotestapp.galileo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.*;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.RequiresApi;
import com.example.galileotestapp.R;
import com.example.galileotestapp.galileo.model.GnssType;
import com.example.galileotestapp.galileo.model.SatelliteStatus;
import com.example.galileotestapp.galileo.utils.CarrierFreqUtils;
import com.example.galileotestapp.galileo.utils.GpsTestUtil;
import com.example.galileotestapp.galileo.utils.MathUtils;

import static com.example.galileotestapp.galileo.model.SatelliteStatus.NO_DATA;


/**
 * Galileo Relevance implementation.
 *
 * The purpose of this component is to verify if the device can receive Galileo signals.
 */
public class GalileoRelevance implements GpsListener {
    private static final String TAG = GalileoRelevance.class.getCanonicalName();

    private static GalileoRelevance instance;

    private GalileoListener listener;
    //Reference app context in order to avoid any leaks.
    private Context context;

    private SharedPreferences prefs;

    public static synchronized GalileoRelevance getInstance() {
        if (instance == null) {
            instance = new GalileoRelevance();
        }

        return instance;
    }

    public void setListener(GalileoListener listener) {
        this.listener = listener;
    }

    private int isGalileoEnabled() {
        // 0 - never checked, 1 - false, 2 - true
        return prefs.getInt("is_enabled", 0);
    }

    private void setGalileoEnabled(int enabled, int frequency) {
        prefs.edit()
                .putInt("is_enabled", enabled)
                .putInt("frequency", frequency)
                .apply();
    }

    private int getFrequency() {
        return prefs.getInt("frequency", 0);
    }

    //Allow all location updates, we want to get GALILEO signals ASAP when possible.
    private final long minTime = 1; // Min Time between location updates, in milliseconds
    private final float minDistance = 0; // Min Distance between location updates, in meters

    private LocationManager locationManager;
    private LocationProvider provider;

    private Location mLastLocation;

    /**
     * Android M (6.0.1) and below status and listener
     */
    private GpsStatus mLegacyStatus;

    private GpsStatus.Listener mLegacyStatusListener;

    /**
     * Android N (7.0) and above status and listeners
     */
    private GnssStatus mGnssStatus;

    private GnssStatus.Callback mGnssStatusListener;

    private GnssMeasurementsEvent.Callback mGnssMeasurementsListener;

    private boolean gpsStarted;

    public void init(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getProvider(LocationManager.GPS_PROVIDER);

        prefs = context.getSharedPreferences("galileo_prefs", Context.MODE_PRIVATE);
    }

    public void verifyAvailability() {
        //cached check.
        if (isGalileoEnabled() != 0) {
            if (listener != null) {
                if (isGalileoEnabled() == 1) {
                    listener.onGalileoNotAvailable();
                } else {
                    listener.onGalileoAvailable(getFrequency());
                }
            }

            return;
        }

        //In order to retrieve a Galileo signal from GNSS we have to request location updates from the GPS antenna.
        gpsStart();

        if (GpsTestUtil.isGnssStatusListenerSupported()) {
            addGnssStatusListener();
            addGnssMeasurementsListener();
        } else {
            //We have no way to check for Galileo Signals.
            gpsStop();
            setGalileoEnabled(1, 0);
            if (listener != null) {
                listener.onGalileoNotAvailable();
            }
        }
    }

    public void stop() {
        if (GpsTestUtil.isGnssStatusListenerSupported()) {
            removeGnssStatusListener();
            removeGnssMeasurementsListener();
        }

        gpsStop();
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.N)
    private void addGnssStatusListener() {
        mGnssStatusListener = new GnssStatus.Callback() {
            @Override
            public void onStarted() {
                onGnssStarted();
            }

            @Override
            public void onStopped() {
                onGnssStopped();
            }

            @Override
            public void onFirstFix(int ttffMillis) {
                onGnssFirstFix(ttffMillis);
            }

            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                mGnssStatus = status;

                GalileoRelevance.this.onSatelliteStatusChanged(mGnssStatus);
            }
        };
        locationManager.registerGnssStatusCallback(mGnssStatusListener);
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addGnssMeasurementsListener() {
        mGnssMeasurementsListener = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                GalileoRelevance.this.onGnssMeasurementsReceived(event);
            }

            @Override
            public void onStatusChanged(int status) {
                final String statusMessage;
                switch (status) {
                    case STATUS_LOCATION_DISABLED:
                        statusMessage = context.getString(R.string.gnss_measurement_status_loc_disabled);
                        break;
                    case STATUS_NOT_SUPPORTED:
                        statusMessage = context.getString(R.string.gnss_measurement_status_not_supported);
                        break;
                    case STATUS_READY:
                        statusMessage = context.getString(R.string.gnss_measurement_status_ready);
                        break;
                    default:
                        statusMessage = context.getString(R.string.gnss_status_unknown);
                }
                Log.d(TAG, "GnssMeasurementsEvent.Callback.onStatusChanged() - " + statusMessage);
            }
        };
        locationManager.registerGnssMeasurementsCallback(mGnssMeasurementsListener);
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void removeGnssStatusListener() {
        if (locationManager != null) {
            locationManager.unregisterGnssStatusCallback(mGnssStatusListener);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void removeGnssMeasurementsListener() {
        if (locationManager != null && mGnssMeasurementsListener != null) {
            locationManager.unregisterGnssMeasurementsCallback(mGnssMeasurementsListener);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void gpsStart() {
        if (gpsStarted) {
            return;
        }

        locationManager.requestLocationUpdates(provider.getName(), minTime, minDistance, this);
        gpsStarted = true;
    }

    @Override
    public void gpsStop() {
        locationManager.removeUpdates(this);
        gpsStarted = false;
    }

    @Override
    public void onGpsStatusChanged(int event, GpsStatus status) {

    }

    @Override
    public void onGnssFirstFix(int ttffMillis) {

    }

    @SuppressLint("NewApi")
    @Override
    public void onSatelliteStatusChanged(GnssStatus status) {
        updateGnssStatus(status);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private synchronized void updateGnssStatus(GnssStatus status) {
        boolean foundGalileo = false;
        boolean isDualFrequency = false;

        final int length = status.getSatelliteCount();
        int count = 0;
        while (count < length) {
            SatelliteStatus satStatus = new SatelliteStatus(status.getSvid(count), GpsTestUtil.getGnssConstellationType(status.getConstellationType(count)),
                    status.getCn0DbHz(count),
                    status.hasAlmanacData(count),
                    status.hasEphemerisData(count),
                    status.usedInFix(count),
                    status.getElevationDegrees(count),
                    status.getAzimuthDegrees(count));
            if (GpsTestUtil.isGnssCarrierFrequenciesSupported()) {
                if (status.hasCarrierFrequencyHz(count)) {
                    satStatus.setHasCarrierFrequency(true);
                    satStatus.setCarrierFrequencyHz(status.getCarrierFrequencyHz(count));
                }
            }

            if (satStatus.getGnssType() == GnssType.SBAS) {
                satStatus.setSbasType(GpsTestUtil.getSbasConstellationType(satStatus.getSvid()));
                //We can keep track of SBAS here if we wish.
            }

            count++;

            String satType = null;
            GnssType type = satStatus.getGnssType();
            switch (type) {
                case NAVSTAR:
                   satType = "NAVSTAR";
                    break;
                case GLONASS:
                    satType = "GLONASS";
                    break;
                case QZSS:
                    satType = "QZSS";
                    break;
                case BEIDOU:
                    satType = "BEIDOU";
                    break;
                case GALILEO: {
                    satType = "GALILEO";
                    foundGalileo = true;
                }
                break;
                case SBAS:
                    satType = "SBAS";
                    break;
                case UNKNOWN:
                    satType = "UNKNOWN";
                    break;
            }

            String frequencyLabel = null;
            if (GpsTestUtil.isGnssCarrierFrequenciesSupported()) {
                if (satStatus.getCarrierFrequencyHz() != NO_DATA) {
                    // Convert Hz to MHz
                    float carrierMhz = MathUtils.toMhz(satStatus.getCarrierFrequencyHz());
                    String carrierLabel = CarrierFreqUtils.getCarrierFrequencyLabel(type,
                            satStatus.getSvid(),
                            carrierMhz);
                    if (carrierLabel != null) {
                        frequencyLabel = carrierLabel;
                    }

                    //E1 is Galileo Single frequency
                    if (foundGalileo && !TextUtils.isEmpty(frequencyLabel) && !frequencyLabel.equals("E1")) {
                        isDualFrequency = true;
                    }
                }
            }
            Log.e(TAG, "Sattelite: " + satType + " - frequency=" + frequencyLabel);
        }

        if (foundGalileo) {
            setGalileoEnabled(2, isDualFrequency ? 2: 1);

            if (listener != null) {
                listener.onGalileoAvailable(isDualFrequency ? 2 : 1);
            }
        }
    }

    @Override
    public void onGnssStarted() {

    }

    @Override
    public void onGnssStopped() {

    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        Log.w(TAG, "Location: lat=" + location.getLatitude() + ", lng=" + location.getLongitude() + ", acc=" + location.getAccuracy());
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
