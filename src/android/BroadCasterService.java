package de.appplant.cordova.plugin.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class BroadCasterService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("Service_call_"  , "You are in BroadCasterService class.");
        Intent background = new Intent(context, LocationManagerService.class);
        Log.e("BroadCasterService ","testing called broadcast called");
        context.startService(background);
    }
}