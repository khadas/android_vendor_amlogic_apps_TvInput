package com.droidlogic.tvsource;


import android.util.Log;

public class Utils {
    private static final boolean DEBUG = true;

    public static final boolean SHOW_VIEW = true;
    public static final boolean HIDE_VIEW = false;

    public static final int UI_TYPE_SOURCE_INFO = 0;
    public static final int UI_TYPE_SOURCE_LIST = 1;
    public static final int UI_TYPE_ATV_CHANNEL_LIST = 2;
    public static final int UI_TYPE_DTV_CHANNEL_LIST = 3;
    public static final int UI_TYPE_ATV_FAV_LIST = 4;
    public static final int UI_TYPE_DTV_FAV_LIST = 5;
    public static final int UI_TYPE_NO_SINAL = 6;
    public static final int UI_TYPE_ALL_HIDE = 7;

    public static void logd(String tag, String msg) {
        if (DEBUG)
            Log.d(tag, msg);
    }

    public static void loge(String tag, String msg) {
        if (DEBUG)
            Log.e(tag, msg);
    }

}
