/*
 Copyright 2013 Sebastián Katzer

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package de.appplant.cordova.plugin.background;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.cordova.PermissionHelper;
import de.appplant.cordova.plugin.background.ForegroundService.ForegroundBinder;
import android.app.PendingIntent;
import android.util.Log;
import android.Manifest;
import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;

import static android.content.Context.BIND_AUTO_CREATE;

public class BackgroundMode extends CordovaPlugin {

    AlarmManager alarmManager;

    PendingIntent pendingIntent;

    private Context context;

    private static CallbackContext callback;

    // Event types for callbacks
    private enum Event { ACTIVATE, DEACTIVATE, FAILURE }

    // Plugin namespace
    private static final String JS_NAMESPACE = "cordova.plugins.backgroundMode";

    // Flag indicates if the app is in background or foreground
    private boolean inBackground = false;

    // Flag indicates if the plugin is enabled or disabled
    private boolean isDisabled = true;

    // Flag indicates if the service is bind
    private boolean isBind = false;

    // Default settings for the notification
    private static JSONObject defaultSettings = new JSONObject();

    // Service that keeps the app awake
    private ForegroundService service;

    String [] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };

    long interval =  10 * 60 * 1000; // Converted 1 minutes to miliSeconds
    int afterLastUpdateMinutes = 2 * 60 * 1000; // Min Time when last location fetched
    int minimumDistanceChanged = 200; // In Meters

    // Used to (un)bind the service to with the activity
    private final ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected (ComponentName name, IBinder service)
        {
            ForegroundBinder binder = (ForegroundBinder) service;
            BackgroundMode.this.service = binder.getService();
        }

        @Override
        public void onServiceDisconnected (ComponentName name)
        {
//            fireEvent(Event.FAILURE, "'service disconnected'");
        }
    };

    /**
     * Executes the request.
     *
     * @param action   The action to execute.
     * @param args     The exec() arguments.
     * @param callbackContext The callback context used when
     *                 calling back into JavaScript.
     *
     * @return Returning false results in a "MethodNotFound" error.
     */
    @Override
    public boolean execute (String action, JSONArray args,
                            CallbackContext callbackContext)
    {
        this.callback = callbackContext;
        boolean validAction = true;

		switch (action)
        {
            case "enable":
                enableMode();
                break;
            case "disable":
                disableMode();
                break;
            case "startGettingBackgroundLocation":

                try {
                    interval = args.getLong(0);
                    afterLastUpdateMinutes = args.getInt(1);
                    minimumDistanceChanged = args.getInt(2);

                    // Update Values to Location service.
                    LocationManagerService locationService = new LocationManagerService();
                    locationService.updatePluginVariables(interval, afterLastUpdateMinutes, minimumDistanceChanged);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                startLocationTracking();
                break;
            default:
                validAction = false;
        }

        return validAction;
    }

    @SuppressLint("ServiceCast")
    public  void startLocationTracking()
    {

        if(hasPermisssion()){
            startService();
            processForegroundService();
        }else  {
            PermissionHelper.requestPermissions(this, 0, permissions);
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException
    {
        PluginResult result;
        //This is important if we're using Cordova without using Cordova, but we have the geolocation plugin installed
        if(context != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    return;
                }
            }
        }

        processForegroundService();
    }

    public void processForegroundService() {
        Activity context = cordova.getActivity();
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BroadCasterService.class);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 30000,
                pendingIntent);
    }

    public boolean hasPermisssion() {
        for(String p : permissions)
        {
            if(!PermissionHelper.hasPermission(this, p))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onPause(boolean multitasking)
    {
        try {
            inBackground = true;
            startService();
        } finally {
            clearKeyguardFlags(cordova.getActivity());
        }
    }

    /**
     * Called when the activity is no longer visible to the user.
     */
    @Override
    public void onStop () {
        clearKeyguardFlags(cordova.getActivity());
    }

	void clearKeyguardFlags (Activity app)
    {
        app.runOnUiThread(() -> app.getWindow().clearFlags(FLAG_DISMISS_KEYGUARD));
	}
	
    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onResume (boolean multitasking)
    {
        inBackground = false;
        stopService();
    }

    /**
     * Called when the activity will be destroyed.
     */
    @Override
    public void onDestroy()
    {
        stopService();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * Enable the background mode.
     */
    private void enableMode()
    {
        isDisabled = false;

        if (inBackground) {
            startService();
        }
    }

    /**
     * Disable the background mode.
     */
    private void disableMode()
    {
        stopService();
    }

    /**
     * Returns the settings for the new/updated notification.
     */
    static JSONObject getSettings () {
        return defaultSettings;
    }


    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void startService()
    {
        Activity context = cordova.getActivity();

        Intent intent = new Intent(context, ForegroundService.class);

        try {
            context.bindService(intent, connection, BIND_AUTO_CREATE);
            context.startService(intent);
        } catch (Exception e) {
            //
        }

        isBind = true;
    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void stopService()
    {
        Activity context = cordova.getActivity();
		Intent intent    = new Intent(context, ForegroundService.class);
		Intent broadCasterIntent    = new Intent(context, BroadCasterService.class);

        if (!isBind) return;

        context.unbindService(connection);
        context.stopService(intent);
        context.stopService(broadCasterIntent);

        isBind = false;
    }


    public void updateLocationData(JSONObject location) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, location);
        result.setKeepCallback(true);
        callback.sendPluginResult(result);
    }

}
