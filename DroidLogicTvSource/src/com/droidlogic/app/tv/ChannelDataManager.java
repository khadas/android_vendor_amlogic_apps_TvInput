package com.droidlogic.app.tv;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.ContentObserver;
import android.database.IContentObserver;
import android.media.tv.TvContract.Channels;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class ChannelDataManager {
    private static final String TAG = "ChannelDataManager";
    private final Context mContext;
    private static List<ChannelTuner> mChannelTuners = new ArrayList<ChannelTuner>();

    public ChannelDataManager(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        ContentObserver ob = new ChannelObserver();
        mContext.getContentResolver().registerContentObserver(Channels.CONTENT_URI, true, ob);
    }

    public static void addChannelTuner(ChannelTuner ct) {
        mChannelTuners.add(ct);
    }

    private void updateChannelList() {
        Log.d(TAG, "==== updateChannelList ====");
        for (ChannelTuner ct : mChannelTuners) {
            ct.updateChannelList();
        }
    }

    public void release() {
        mChannelTuners.clear();
    }

    private final class ChannelObserver extends ContentObserver {

        public ChannelObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateChannelList();
        }

        @Override
        public IContentObserver releaseContentObserver() {
            // TODO Auto-generated method stub
            return super.releaseContentObserver();
        }

    }
}
