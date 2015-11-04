package com.droidlogic.tvclient;

import com.droidlogic.utils.tunerinput.data.ChannelInfo;

import android.amlogic.Tv;
import android.amlogic.Tv.SourceInput_Type;
import android.text.TextUtils;

public class TvClient {
    public static final String LABEL_ATV = "ATV";
    public static final String LABEL_DTV = "DTV";
    public static final String LABEL_AV1 = "AV1";
    public static final String LABEL_AV2 = "AV2";
    public static final String LABEL_HDMI1 = "HDMI1";
    public static final String LABEL_HDMI2 = "HDMI2";
    public static final String LABEL_HDMI3 = "HDMI3";
    public SourceInput_Type curSource = Tv.SourceInput_Type.SOURCE_TYPE_TV;
    public ChannelInfo curChannel = null;

    public TvClient (String label) {
        if (!TextUtils.isEmpty(label)) {
            if (label.equals(LABEL_ATV)) {
                curSource = Tv.SourceInput_Type.SOURCE_TYPE_TV;
            } else if (label.equals(LABEL_DTV)) {
                curSource = Tv.SourceInput_Type.SOURCE_TYPE_DTV;
            } else if (label.equals(LABEL_AV1) || label.equals(LABEL_AV2)) {
                curSource = Tv.SourceInput_Type.SOURCE_TYPE_AV;
            } else {
                curSource = Tv.SourceInput_Type.SOURCE_TYPE_HDMI;
            }
        }
        curChannel = new ChannelInfo(null, null, null, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 1, 0);
    }

    public Tv getTvInstance() {
        return Tv.open();
    }
}
