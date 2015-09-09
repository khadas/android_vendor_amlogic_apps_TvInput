package com.droidlogic.tv;


import android.util.Log;

public class Utils {
    private static final boolean DEBUG = true;

    public static final boolean SHOW_VIEW = true;
    public static final boolean HIDE_VIEW = false;

    public static void logd(String tag, String msg) {
        if (DEBUG)
            Log.d(tag, msg);
    }

    public static void loge(String tag, String msg) {
        if (DEBUG)
            Log.e(tag, msg);
    }

}
