package com.droidlogic.app;

import android.amlogic.Tv;

public class DroidLogicTvUtils {

    /**
     * final parameters for {@link TvInpuptService.Session.notifySessionEvent}
     */
    public static final String SIG_INFO_EVENT  = "sig_info_event";
    public static final String SIG_INFO_TYPE   = "sig_info_type";
    public static final String SIG_INFO_LABEL  = "sig_info_label";
    public static final String SIG_INFO_ARGS   = "sig_info_args";

    public static final int SIG_INFO_TYPE_ATV  = 0;
    public static final int SIG_INFO_TYPE_DTV  = 1;
    public static final int SIG_INFO_TYPE_HDMI = 2;
    public static final int SIG_INFO_TYPE_AV   = 3;


    public static class TvClient {
        private static Tv tv;
        public static Tv getTvInstance() {
            if (tv == null) {
                tv = Tv.open();
            }
            return tv;
        }
    }
}
