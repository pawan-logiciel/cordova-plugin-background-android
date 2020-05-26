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
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.app.NotificationChannel;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.graphics.Color;

import org.json.JSONObject;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

/**
 * Puts the service in a foreground state, where the system considers it to be
 * something the user is actively aware of and thus not a candidate for killing
 * when low on memory.
 */
public class ForegroundService extends Service {

    public static final String COLOR = "color";

    // Fixed ID for the 'foreground' notification
    public static final int NOTIFICATION_ID = -574543954;

    // Default title of the background notification
    private static final String NOTIFICATION_TITLE =
            "App is running in background";

    // Default text of the background notification
    private static final String NOTIFICATION_TEXT =
            "JobProgress is using location in background";

    // Default icon of the background notification
    private static final String NOTIFICATION_ICON = "icon";

    // Binder given to clients
    private final IBinder binder = new ForegroundBinder();

    // Partial wake lock to prevent the app from going to sleep when locked
    private PowerManager.WakeLock wakeLock;

    /**
     * Allow clients to call on to the service.
     */
    @Override
    public IBinder onBind (Intent intent) {
        return binder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class ForegroundBinder extends Binder
    {
        ForegroundService getService()
        {
            // Return this instance of ForegroundService
            // so clients can call public methods
            return ForegroundService.this;
        }
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        keepAwake();
    }

    /**
     * No need to run headless on destroy.
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        sleepWell();
    }

    /**
     * Prevent Android from stopping the background service automatically.
     */
    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    @SuppressLint("WakelockTimeout")
    private void keepAwake()
    {

        Bundle extras = new Bundle();
        extras.putString("icon", "icon");
        JSONObject settings = BackgroundMode.getSettings();
        boolean isSilent    = settings.optBoolean("silent", false);

        if (!isSilent) {
            startForeground(NOTIFICATION_ID, makeNotification(extras));
        }

        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);

        wakeLock = pm.newWakeLock(
                PARTIAL_WAKE_LOCK, "backgroundmode:wakelock");

        wakeLock.acquire();
    }

    /**
     * Stop background mode.
     */
    private void sleepWell()
    {
        stopForeground(true);
        getNotificationManager().cancel(NOTIFICATION_ID);

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    /**
     * Create a notification as the visible part to be able to put the service
     * in a foreground state by using the default settings.
     */
    private Notification makeNotification(Bundle extras)
    {
        return makeNotification(BackgroundMode.getSettings(), extras);
    }

    /**
     * Create a notification as the visible part to be able to put the service
     * in a foreground state.
     *
     * @param settings The config settings
     */
    private Notification makeNotification (JSONObject settings, Bundle extras)
    {
        // use channelid for Oreo and higher
        String CHANNEL_ID = "cordova-plugin-background-mode-id";

        if(Build.VERSION.SDK_INT >= 26){
            // The user-visible name of the channel.
            CharSequence name = "JobPogress";
            // The user-visible description of the channel.
            String description = "App is using location in background";

            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name,importance);

            // Configure the notification channel.
            mChannel.setDescription(description);

            getNotificationManager().createNotificationChannel(mChannel);
        }
        String title    = settings.optString("title", NOTIFICATION_TITLE);
        String text     = settings.optString("text", NOTIFICATION_TEXT);
        boolean bigText = settings.optBoolean("bigText", false);



        Context context = getApplicationContext();
        String pkgName  = context.getPackageName();
        Intent intent   = context.getPackageManager()
                .getLaunchIntentForPackage(pkgName);

        intent.putExtra("title", title);
        intent.putExtra("message", text);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true);

        if(Build.VERSION.SDK_INT >= 26){
            notification.setChannelId(CHANNEL_ID);
        }

        if (settings.optBoolean("hidden", true)) {
            notification.setPriority(Notification.PRIORITY_MIN);
        }

        if (bigText || text.contains("\n")) {
//            notification.setStyle(
//                    new Notification.BigTextStyle().bigText(text));
        }

        SharedPreferences prefs = context.getSharedPreferences("de.appplant.cordova.plugin.background", Context.MODE_PRIVATE);
        String localIconColor = prefs.getString("iconColor", null);

        /*
         * Notification Icon Color
         *
         * Sets the small-icon background color of the notification.
         * To use, add the `iconColor` key to plugin android options
         *
         */
        setNotificationIconColor(extras.getString(COLOR), notification, localIconColor);

        /*
         * Notification Icon
         *
         * Sets the small-icon of the notification.
         *
         * - checks the plugin options for `icon` key
         * - if none, uses the application icon
         *
         * The icon value must be a string that maps to a drawable resource.
         * If no resource is found, falls
         *
         */
        String packageName = context.getPackageName();
        Resources resources = context.getResources();
        String localIcon = prefs.getString("icon", null);
        setNotificationSmallIcon(context, extras, packageName, resources, notification, localIcon);


        if (intent != null && settings.optBoolean("resume")) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(
                    context, NOTIFICATION_ID, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);


            notification.setContentIntent(contentIntent);
        }

        return notification.build();

    }


    private void setNotificationIconColor(String color, NotificationCompat.Builder mBuilder, String localIconColor) {
        int iconColor = 0;
        if (color != null && !"".equals(color)) {
            try {
                iconColor = Color.parseColor(color);
            } catch (IllegalArgumentException e) {
                Log.e("LOG_TAG", "couldn't parse color from android options");
            }
        } else if (localIconColor != null && !"".equals(localIconColor)) {
            try {
                iconColor = Color.parseColor(localIconColor);
            } catch (IllegalArgumentException e) {
                Log.e("LOG_TAG", "couldn't parse color from android options");
            }
        }
        if (iconColor != 0) {
            mBuilder.setColor(iconColor);
        }
    }

    private void setNotificationSmallIcon(Context context, Bundle extras, String packageName, Resources resources,
                                          NotificationCompat.Builder mBuilder, String localIcon) {
        int iconId = 0;
        String icon = extras.getString("icon");

        if (icon != null && !"".equals(icon)) {
            iconId = getImageId(resources, icon, packageName);
            Log.d("LOG_TAG", "using icon from plugin options");
        } else if (localIcon != null && !"".equals(localIcon)) {
            iconId = getImageId(resources, localIcon, packageName);
            Log.d("LOG_TAG", "using icon from plugin options");
        }
        if (iconId == 0) {
            Log.d("LOG_TAG", "no icon resource found - using application icon");
            iconId = context.getApplicationInfo().icon;
            Log.d("Icon ID Anuj", String.valueOf(iconId));
        }

        mBuilder.setSmallIcon(iconId);
    }

    
    private int getImageId(Resources resources, String icon, String packageName) {
        int iconId = resources.getIdentifier(icon, "drawable", packageName);
        if (iconId == 0) {
            iconId = resources.getIdentifier(icon, "mipmap", packageName);
        }
        return iconId;
    }

    /**
     * Set notification color if its supported by the SDK.
     *
     * @param notification A Notification.Builder instance
     * @param settings A JSON dict containing the color definition (red: FF0000)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setColor (NotificationCompat.Builder notification, JSONObject settings)
    {

        String hex = settings.optString("color", null);

        if (Build.VERSION.SDK_INT < 21 || hex == null)
            return;

        try {
            int aRGB = Integer.parseInt(hex, 16) + 0xFF000000;
            notification.setColor(aRGB);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the shared notification service manager.
     */
    private NotificationManager getNotificationManager()
    {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }


}
