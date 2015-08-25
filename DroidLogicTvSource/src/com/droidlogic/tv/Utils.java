package com.droidlogic.tv;

import java.util.List;

import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;

public class Utils {
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

    public static int getSourceType(TvInputInfo info) {
        int info_type = info.getType();
        int source_type = SOURCE_TYPE_ATV;
        switch (info_type) {
            case TvInputInfo.TYPE_HDMI:
                source_type = SOURCE_TYPE_HDMI1;
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
                if (info.getHdmiPortId() == 1)
                    source_type = SOURCE_TYPE_HDMI1;
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

    public static String getInputId(int source_type) {
        List<TvInputInfo> list = mTvInputManager.getTvInputList();
        for (TvInputInfo info:list) {
            if (getSourceType(info) == source_type)
                return info.getId();
        }
        return null;
    }
}
