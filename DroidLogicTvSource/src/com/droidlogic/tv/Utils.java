package com.droidlogic.tv;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.util.Log;

public class Utils {
    private static final boolean DEBUG = true;

    public static TvInputManager mTvInputManager;
    public static final int SOURCE_TYPE_ATV          = 0;
    public static final int SOURCE_TYPE_DTV          = 1;
    public static final int SOURCE_TYPE_COMPOSITE    = 2;
    public static final int SOURCE_TYPE_SVIDEO       = 3;
    public static final int SOURCE_TYPE_SCART        = 4;
    public static final int SOURCE_TYPE_COMPONENT    = 5;
    public static final int SOURCE_TYPE_VGA          = 6;
    public static final int SOURCE_TYPE_DVI          = 7;
    public static final int SOURCE_TYPE_HDMI1        = 8;
    public static final int SOURCE_TYPE_HDMI2        = 9;
    public static final int SOURCE_TYPE_HDMI3        = 10;
    public static final int SOURCE_TYPE_OTHER        = 11;

    public static final int port_hdmi1 = 1;
    public static final int port_hdmi2 = 2;
    public static final int port_hdmi3 = 3;

    public static void logd(String tag, String msg) {
        if (DEBUG)
            Log.d(tag, msg);
    }

    public static void loge(String tag, String msg) {
        if (DEBUG)
            Log.e(tag, msg);
    }

    public static int getSourceType(TvInputInfo info) {
        int info_type = info.getType();
        int source_type = SOURCE_TYPE_ATV;
        switch (info_type) {
            case TvInputInfo.TYPE_HDMI:
                source_type = getSourceType(info.getId());
                break;
            case TvInputInfo.TYPE_COMPONENT:
                source_type = SOURCE_TYPE_COMPONENT;
                break;
            default:
                source_type = SOURCE_TYPE_ATV;
                break;
        }
        return source_type;
    }

    public static int getSourceType(TvInputHardwareInfo info) {
        int info_type = info.getType();
        int source_type = SOURCE_TYPE_ATV;
        switch (info_type) {
            case TvInputHardwareInfo.TV_INPUT_TYPE_HDMI:
                if (info.getHdmiPortId() == port_hdmi1) {
                    source_type = SOURCE_TYPE_HDMI1;
                } else if (info.getHdmiPortId() == port_hdmi2) {
                    source_type =  SOURCE_TYPE_HDMI2;
                } else {
                    source_type = SOURCE_TYPE_HDMI3;
                }
                break;
            case TvInputHardwareInfo.TV_INPUT_TYPE_COMPONENT:
                source_type = SOURCE_TYPE_COMPONENT;
                break;
            default:
                source_type = SOURCE_TYPE_ATV;
                break;
        }
        return source_type;
    }

    public static int getSourceType(String input_id) {
        int source_type = SOURCE_TYPE_ATV;
        String[] temp = input_id.split("/");
        int device_id = 0;
        if (temp.length == 3) {
            logd("Utils", "=====temp[2] =" + temp[2]);
            device_id = Integer.parseInt(temp[2].substring(2));
            switch (device_id) {
                case 5:
                    source_type = SOURCE_TYPE_HDMI1;
                    break;
                case 6:
                    source_type = SOURCE_TYPE_HDMI2;
                    break;
                case 7:
                    source_type = SOURCE_TYPE_HDMI3;
                    break;
                default:
                    break;
            }
        }
        return source_type;
    }

    public static String getInputId(int source_type) {
        List<TvInputInfo> list = mTvInputManager.getTvInputList();
        for (TvInputInfo info:list) {
            if (getSourceType(info) == source_type)
                return info.getId();
        }
        return null;
    }

    public static CharSequence getInputLabel(Context context, String input_id) {
        TvInputManager tim = (TvInputManager)context.getSystemService(Context.TV_INPUT_SERVICE);
        TvInputInfo info = tim.getTvInputInfo(input_id);
        return info == null ? null : info.loadCustomLabel(context);
    }

    public static Drawable getInputIcon(Context context, String input_id) {
        TvInputManager tim = (TvInputManager)context.getSystemService(Context.TV_INPUT_SERVICE);
        TvInputInfo info = tim.getTvInputInfo(input_id);
        return info == null ? null : info.loadIcon(context);
    }

}
