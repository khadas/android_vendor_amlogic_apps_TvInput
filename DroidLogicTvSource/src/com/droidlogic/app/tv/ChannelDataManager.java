package com.droidlogic.app.tv;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.IContentObserver;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class ChannelDataManager {
    private static final String TAG = "ChannelDataManager";
    private static final boolean DEBUG = true;

    private final Context mContext;
    private static List<ChannelTuner> mChannelTuners = new ArrayList<ChannelTuner>();
    private ChannelObserver mChannelObserver;
    
    private static final UriMatcher sUriMatcher;
    private static final int MATCH_CHANNEL = 1;
    private static final int MATCH_CHANNEL_ID = 2;
    private static final int MATCH_CHANNEL_ID_LOGO = 3;
    private static final int MATCH_PASSTHROUGH_ID = 4;
    private static final int MATCH_PROGRAM = 5;
    private static final int MATCH_PROGRAM_ID = 6;
    private static final int MATCH_WATCHED_PROGRAM = 7;
    private static final int MATCH_WATCHED_PROGRAM_ID = 8;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(TvContract.AUTHORITY, "channel", MATCH_CHANNEL);
        sUriMatcher.addURI(TvContract.AUTHORITY, "channel/#", MATCH_CHANNEL_ID);
        sUriMatcher.addURI(TvContract.AUTHORITY, "channel/#/logo", MATCH_CHANNEL_ID_LOGO);
        sUriMatcher.addURI(TvContract.AUTHORITY, "passthrough/*", MATCH_PASSTHROUGH_ID);
        sUriMatcher.addURI(TvContract.AUTHORITY, "program", MATCH_PROGRAM);
        sUriMatcher.addURI(TvContract.AUTHORITY, "program/#", MATCH_PROGRAM_ID);
        sUriMatcher.addURI(TvContract.AUTHORITY, "watched_program", MATCH_WATCHED_PROGRAM);
        sUriMatcher.addURI(TvContract.AUTHORITY, "watched_program/#", MATCH_WATCHED_PROGRAM_ID);
    }

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

    private void updateChannelList(Uri uri) {
        for (ChannelTuner ct : mChannelTuners) {
            ct.updateChannelList(uri);
        }
    }

    private void processChangedUri(Uri uri) {
        if (DEBUG)
            Log.d(TAG, "==== uri =" + uri);
        switch (sUriMatcher.match(uri)) {
            case MATCH_CHANNEL:
                break;
            case MATCH_CHANNEL_ID:
                updateChannelList(uri);
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
