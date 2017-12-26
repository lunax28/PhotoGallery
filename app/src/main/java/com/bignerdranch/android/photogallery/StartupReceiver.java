package com.bignerdranch.android.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by albus on 24-Dec-17.
 */

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"Received broadcast intent: " + intent.getAction());
        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context,isOn);
    }
}
