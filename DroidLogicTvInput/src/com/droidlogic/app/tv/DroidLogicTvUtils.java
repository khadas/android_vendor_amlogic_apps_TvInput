package com.droidlogic.app.tv;


public class DroidLogicTvUtils {

    /**
     * final parameters for {@link TvInpuptService.Session.notifySessionEvent}
     */
    public static final String SIG_INFO_EVENT = "sig_info_event";
    public static final String SIG_INFO_TYPE  = "sig_info_type";
    public static final String SIG_INFO_LABEL  = "sig_info_label";
    public static final String SIG_INFO_ARGS  = "sig_info_args";

    public static final int SIG_INFO_TYPE_ATV    = 0;
    public static final int SIG_INFO_TYPE_DTV    = 1;
    public static final int SIG_INFO_TYPE_HDMI   = 2;
    public static final int SIG_INFO_TYPE_AV     = 3;
    public static final int SIG_INFO_TYPE_OTHER  = 4;

    /**
     * source input type need to switch
     */
    private static final int SOURCE_TYPE_START  = 0;
    private static final int SOURCE_TYPE_END    = 7;

    public static final int SOURCE_TYPE_ATV     = SOURCE_TYPE_START;
    public static final int SOURCE_TYPE_DTV     = SOURCE_TYPE_START + 1;
    public static final int SOURCE_TYPE_AV1     = SOURCE_TYPE_START + 2;
    public static final int SOURCE_TYPE_AV2     = SOURCE_TYPE_START + 3;
    public static final int SOURCE_TYPE_HDMI1   = SOURCE_TYPE_START + 4;
    public static final int SOURCE_TYPE_HDMI2   = SOURCE_TYPE_START + 5;
    public static final int SOURCE_TYPE_HDMI3   = SOURCE_TYPE_START + 6;
    public static final int SOURCE_TYPE_OTHER   = SOURCE_TYPE_END;

    /**
     * source input id sync with {@link CTvin.h}
     */
    public static final int DEVICE_ID_ATV        = 0;
    public static final int DEVICE_ID_AV1        = 1;
    public static final int DEVICE_ID_AV2        = 2;
    public static final int DEVICE_ID_HDMI1      = 5;
    public static final int DEVICE_ID_HDMI2      = 6;
    public static final int DEVICE_ID_HDMI3      = 7;
    public static final int DEVICE_ID_DTV        = 10;

    public static final int RESULT_OK = 1;
    public static final int RESULT_UPDATE = 2;
    public static final int RESULT_FAILED = 3;
}
