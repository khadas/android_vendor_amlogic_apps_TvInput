package com.droidlogic.tvclient;

import com.droidlogic.utils.tunerinput.data.ChannelInfo;

import android.amlogic.Tv;
import android.amlogic.Tv.SourceInput_Type;

public class TvClient {
    private static Tv tv = Tv.open();
    static private TvClient mTvClientInstance = null;
    public SourceInput_Type curSource = Tv.SourceInput_Type.SOURCE_TYPE_TV;
    public ChannelInfo curChannel = new ChannelInfo(null, null, null, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0);

    public static TvClient getTvClient() {
        if (mTvClientInstance == null)
            mTvClientInstance = new TvClient();
        return mTvClientInstance;
    }

    public static Tv getTvInstance() {
        return tv;
    }
}
