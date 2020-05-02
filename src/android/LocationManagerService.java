package de.appplant.cordova.plugin.background;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class LocationManagerService extends Service implements LocationListener {
    private static final String TAG = "LocationManagerService";
    private Context context;
    boolean isGPSEnable = false;
    boolean isNetworkEnable = false;
    double latitude, longitude;
    LocationManager locationManager;
    private BackgroundMode bgModeService;
    Location location;
    private Handler mHandler = new Handler();
    private Timer mTimer = null;
    long notify_interval = 2000;

    public double track_lat = 0.0;
    public double track_lng = 0.0;
    public static String str_receiver = "servicetutorial.service.receiver";
    Intent intent;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        System.out.println("public void onCreate()");



        mTimer = new Timer();
        mTimer.schedule(new TimerTaskToGetLocation(), 1, notify_interval);
        intent = new Intent(str_receiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        this.context = this;


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy Location Service");
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    private void trackLocation() {
        System.out.println("public void trackLocation()");
        Log.e(TAG, "trackLocation");
        String TAG_TRACK_LOCATION = "trackLocation";

        stopSelf();
        mTimer.cancel();
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            fn_update(location);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /******************************/

    private void fn_getlocation() throws JSONException {

        System.out.println("public void fn_getlocation()");

        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnable && !isNetworkEnable) {
            Log.e(TAG, "CAN'T GET LOCATION");
            stopSelf();
        } else {
             if (isNetworkEnable) {
                 location = null;
                 if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                     // TODO: Consider calling
                     //    ActivityCompat#requestPermissions
                     // here to request the missing permissions, and then overriding
                     //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                     //                                          int[] grantResults)
                     // to handle the case where the user grants the permission. See the documentation
                     // for ActivityCompat#requestPermissions for more details.
                     return;
                 }
                 locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
                 if (locationManager != null) {
                     location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                     if (location != null) {

                         Log.e(TAG, "isNetworkEnable latitude" + location.getLatitude() + "\nlongitude" + location.getLongitude() + "");
                         latitude = location.getLatitude();
                         longitude = location.getLongitude();
                         track_lat = latitude;
                         track_lng = longitude;
                         fn_update(location);
                     }
                 }
             }else if(isGPSEnable) {
                location = null;
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        Log.e(TAG, "isGPSEnable latitude" + location.getLatitude() + "\nlongitude" + location.getLongitude() + "");
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        track_lat = latitude;
                        track_lng = longitude;
                        fn_update(location);
                    }
                }
            }

            Log.e(TAG, "START SERVICE");
            trackLocation();

        }
    }

    private class TimerTaskToGetLocation extends TimerTask {
        @Override
        public void run() {

            System.out.println("public void TimerTaskToGetLocation()");

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        fn_getlocation();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }

    private void fn_update(Location location) throws JSONException {

        Log.i("fn_update", "fn_update");

        intent.putExtra("latutide", location.getLatitude() + "");
        intent.putExtra("longitude", location.getLongitude() + "");
        sendBroadcast(intent);

        JSONObject loc = new JSONObject() {{
            put("latutide", location.getLatitude());
            put("longitude", location.getLongitude());
        }};

        BackgroundMode bgMode = new BackgroundMode();
        bgMode.updateLocationData(loc);
    }
}