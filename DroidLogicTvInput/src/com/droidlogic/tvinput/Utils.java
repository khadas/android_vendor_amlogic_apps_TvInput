package com.droidlogic.tvinput;

import com.droidlogic.app.tv.DroidLogicTvUtils;

import android.net.Uri;
import android.util.Log;

public class Utils {
    private static final boolean DEBUG = true;

    public static void logd(String tag, String msg) {
        if (DEBUG)
            Log.d(tag, msg);
    }

    public static void loge(String tag, String msg) {
        if (DEBUG)
            Log.e(tag, msg);
    }

    public static int getChannelId(Uri uri) {
        return DroidLogicTvUtils.getChannelId(uri);
    }
}
