package com.droidlogic.utils;

import android.util.Log;

public class Utils {
    private static final boolean DEBUG = true;

    public static final int SOURCE_TV      = 0;
    public static final int SOURCE_AV1     = 1;
    public static final int SOURCE_AV2     = 2;
    public static final int SOURCE_YPBPR1  = 3;
    public static final int SOURCE_YPBPR2  = 4;
    public static final int SOURCE_HDMI1   = 5;
    public static final int SOURCE_HDMI2   = 6;
    public static final int SOURCE_HDMI3   = 7;
    public static final int SOURCE_VGA     = 8;
    public static final int SOURCE_DTV     = 9;
    public static final int SOURCE_SVIDEO  = 10;

    public static void logd(String tag, String msg) {
        if (DEBUG)
            Log.d(tag, msg);
    }

    public static void loge(String tag, String msg) {
        if (DEBUG)
            Log.e(tag, msg);
    }

}
