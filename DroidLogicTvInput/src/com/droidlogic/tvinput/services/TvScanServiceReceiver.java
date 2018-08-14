package com.droidlogic.tvinput.services;

import android.os.SystemProperties;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;



public class TvScanServiceReceiver extends BroadcastReceiver {
    static final String TAG = "TvScanServiceReceiver";
    private static final String ACTION_BOOT_COMPLETED ="android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            {
                Log.d(TAG,"TvScanService Start*******************************************");
                context.startService(new Intent(context, TvScanService.class));
            }
        }
    }
}
