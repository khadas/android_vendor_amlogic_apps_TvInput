package com.droidlogic.tv;


import android.util.Log;

public class Utils {
    private static final boolean DEBUG = true;

    public static final boolean SHOW_VIEW = true;
    public static final boolean HIDE_VIEW = false;

    public static final int DTV_LIST       = 1;
    public static final int ATV_LIST       = 2;
    public static final int DTV_FAV_LIST   = 3;
    public static final int ATV_FAV_LIST   = 4;

    public static void logd(String tag, String msg) {
        if (DEBUG)
            Log.d(tag, msg);
    }

    public static void loge(String tag, String msg) {
        if (DEBUG)
            Log.e(tag, msg);
    }

}
