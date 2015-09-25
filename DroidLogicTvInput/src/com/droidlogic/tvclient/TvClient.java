package com.droidlogic.tvclient;

import android.amlogic.Tv;
import android.amlogic.Tv.SourceInput_Type;

public class TvClient
{
    private static Tv tv = Tv.open();
    static private TvClient mTvClientInstance = null;
    public SourceInput_Type curSource = Tv.SourceInput_Type.SOURCE_TYPE_TV;

    public static TvClient getTvClient()
    {
        if (mTvClientInstance == null)
            mTvClientInstance = new TvClient();
        return mTvClientInstance;
    }

    public static Tv getTvInstance()
    {
        return tv;
    }
}
