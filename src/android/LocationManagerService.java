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
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

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

    long notify_interval = 1 * 60 * 1000; // Converted 10 minutes to miliSeconds
    int minTime = 1 * 60 * 1000; // Min Time when last location fetched
    int minDistance = 25;

    public double tracked_lat = 0.0;
    public double tracked_lng = 0.0;
    public static String str_receiver = "de.appplant.cordova.plugin.background";
    Intent intent;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BackgroundMode bg = new BackgroundMode();
        System.out.println("bg.interval");

        notify_interval = bg.interval * 60 * 1000;
        System.out.println(notify_interval);
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
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    private void trackLocation() {
        stopSelf();
        mTimer.cancel();
    }

    @Override
    public void onLocationChanged(Location location) {
//        try {
//            fn_update(location);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
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
        System.out.println("fn_getlocation");
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnable && !isNetworkEnable) {
            stopSelf();
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            location = null;

            if (isGPSEnable) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        fn_update(location);
                    }
                }
            }else if (isNetworkEnable) {
                 locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                 if (locationManager != null) {
                     location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                     if (location != null) {
                         fn_update(location);
                     }
                 }
             }
//            trackLocation();
        }
    }

    private class TimerTaskToGetLocation extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        BackgroundMode bg = new BackgroundMode();
                        notify_interval = bg.interval;
                        minTime = bg.afterLastUpdateMinutes;
                        minDistance = bg.minimumDistanceChanged;

                        fn_getlocation();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }

    private void fn_update(Location location) throws JSONException {

        System.out.println("private void fn_update(Location location) throws JSONException");

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        intent.putExtra("latutide", lat + "");
        intent.putExtra("longitude", lng + "");
        sendBroadcast(intent);

//        if(tracked_lat == lat && tracked_lng == lng) {
//            return;
//        }

        if(tracked_lat != 0.00 && tracked_lng != 0.00) {

            Location loc1 = new Location("");

            loc1.setLatitude(tracked_lat);
            loc1.setLongitude(tracked_lng);

            Location loc2 = new Location("");
            loc2.setLatitude(lat);
            loc2.setLongitude(lng);

            float distanceInMeters = loc1.distanceTo(loc2);

//            if(distanceInMeters <= minDistance) {
//                return;
//            }

        }

        // Set Lat Long when we get New Location
        tracked_lat = lat;
        tracked_lng = lng;

        JSONObject loc = new JSONObject() {{
            put("lat", location.getLatitude());
            put("long", location.getLongitude());
        }};

        BackgroundMode bgMode = new BackgroundMode();
        bgMode.updateLocationData(loc);
    }
}