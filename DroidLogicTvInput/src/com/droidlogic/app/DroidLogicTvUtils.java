package com.droidlogic.app;

import android.amlogic.Tv;
import android.amlogic.Tv.SourceInput_Type;

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


    public static class TvClient {
        private static Tv tv;

        static private TvClient mTvClientInstance = null;
        public SourceInput_Type curSource = Tv.SourceInput_Type.SOURCE_TYPE_TV;

        public static TvClient getTvClient()
        {
            if (mTvClientInstance == null)
                mTvClientInstance = new TvClient();
            return mTvClientInstance;
        }

        public static Tv getTvInstance() {
            if (tv == null) {
                tv = Tv.open();
            }
            return tv;
        }
    }

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
}
