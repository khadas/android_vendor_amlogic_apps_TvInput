package com.droidlogic.tvinput;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TVChannelParams;

import android.media.tv.TvContract.Channels;
import android.net.Uri;
import android.text.TextUtils;
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

    public static String mode2type(int mode) {
        String type = "";
        switch (mode) {
            case TVChannelParams.MODE_DTMB:
                type = Channels.TYPE_DTMB;
                break;
            case TVChannelParams.MODE_QPSK:
                type = Channels.TYPE_DVB_S;
                break;
            case TVChannelParams.MODE_QAM:
                type = Channels.TYPE_DVB_C;
                break;
            case TVChannelParams.MODE_OFDM:
                type = Channels.TYPE_DVB_T;
                break;
            case TVChannelParams.MODE_ATSC:
                type = Channels.TYPE_ATSC_C;
                break;
            case TVChannelParams.MODE_ANALOG:
                type = Channels.TYPE_PAL;
                break;
            case TVChannelParams.MODE_ISDBT:
                type = Channels.TYPE_ISDB_T;
                break;
            default:
                break;
        }
        return type;
    }

    public static int type2mode(String type) {
        int mode = 5;
        if (TextUtils.equals(type, Channels.TYPE_DTMB)) {
            mode = TVChannelParams.MODE_DTMB;
        } else if (TextUtils.equals(type, Channels.TYPE_DVB_S)) {
            mode = TVChannelParams.MODE_QPSK;
        } else if (TextUtils.equals(type, Channels.TYPE_DVB_C)) {
            mode = TVChannelParams.MODE_QAM;
        } else if (TextUtils.equals(type, Channels.TYPE_DVB_T)) {
            mode = TVChannelParams.MODE_OFDM;
        } else if (TextUtils.equals(type, Channels.TYPE_ATSC_C)) {
            mode = TVChannelParams.MODE_ATSC;
        } else if (TextUtils.equals(type, Channels.TYPE_PAL)) {
            mode = TVChannelParams.MODE_ANALOG;
        } else if (TextUtils.equals(type, Channels.TYPE_ISDB_T)) {
            mode = TVChannelParams.MODE_ISDBT;
        }

        return mode;
    }
}
