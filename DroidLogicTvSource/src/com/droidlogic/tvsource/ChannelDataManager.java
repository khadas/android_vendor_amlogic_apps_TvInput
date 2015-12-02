package com.droidlogic.tvsource;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.ContentObserver;
import android.database.IContentObserver;
import android.media.tv.TvContract.Channels;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.droidlogic.app.tv.DroidLogicTvUtils;

public class ChannelDataManager {
    private static final String TAG = "ChannelDataManager";
    private static final boolean DEBUG = true;

    private final Context mContext;
    private static List<ChannelTuner> mChannelTuners = new ArrayList<ChannelTuner>();
    private ChannelObserver mChannelObserver;

    public ChannelDataManager(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        if (mChannelObserver == null)
            mChannelObserver = new ChannelObserver();
        mContext.getContentResolver().registerContentObserver(Channels.CONTENT_URI, true, mChannelObserver);
    }

    public static void addChannelTuner(ChannelTuner ct) {
        mChannelTuners.add(ct);
    }

    private void changeRowChannel(Uri uri) {
        for (ChannelTuner ct : mChannelTuners) {
            ct.changeRowChannel(uri);
        }
    }

    private void changeChannels(Uri uri) {
        for (ChannelTuner ct : mChannelTuners) {
            ct.changeChannels(uri);
        }
    }

    private void processChangedUri(Uri uri) {
        if (DEBUG)
            Log.d(TAG, "==== uri =" + uri);
        switch (DroidLogicTvUtils.matchsWhich(uri)) {
            case DroidLogicTvUtils.MATCH_CHANNEL:
                changeChannels(uri);
                break;
            case DroidLogicTvUtils.MATCH_CHANNEL_ID:
                changeRowChannel(uri);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    public void release() {
        mChannelTuners.clear();
        if (mChannelObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mChannelObserver);
            mChannelObserver = null;
        }
    }

    private final class ChannelObserver extends ContentObserver {

        public ChannelObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            processChangedUri(uri);
        }

        @Override
        public IContentObserver releaseContentObserver() {
            // TODO Auto-generated method stub
            return super.releaseContentObserver();
        }

    }
}
