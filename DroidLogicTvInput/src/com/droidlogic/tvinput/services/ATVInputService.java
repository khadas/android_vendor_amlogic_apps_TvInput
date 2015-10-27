package com.droidlogic.tvinput.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputHardwareInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.droidlogic.utils.Utils;
import com.droidlogic.utils.tunerinput.tvutil.TvContractUtils;
import com.droidlogic.utils.tunerinput.data.ChannelInfo;

import com.droidlogic.app.tv.DroidLogicTvInputService;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvInputBaseSession;
import com.droidlogic.tvclient.TvClient;

import java.util.HashSet;
import java.util.Set;

import android.amlogic.Tv;

public class ATVInputService extends DroidLogicTvInputService {

    private static final String TAG = "ATVInputService";

    private static TvClient client = TvClient.getTvClient();
    private ATVSessionImpl mSession;

    private final BroadcastReceiver mParentalControlsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSession != null) {
                mSession.checkContentBlockNeeded();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mParentalControlsBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mParentalControlsBroadcastReceiver);
    }

    @Override
    public Session onCreateSession(String inputId) {
        mSession = new ATVSessionImpl(this, inputId, getHardwareDeviceId(inputId));
        mSession.setOverlayViewEnabled(true);
        registerInputSession(mSession);
        client.curSource = Tv.SourceInput_Type.SOURCE_TYPE_TV;
        return mSession;
    }

    public class ATVSessionImpl extends TvInputBaseSession {
        private final Context mContext;
        private TvInputManager mTvInputManager;
        private TvContentRating mLastBlockedRating;
        private TvContentRating mCurrentContentRating;
        private final Set<TvContentRating> mUnblockedRatingSet = new HashSet<>();
        private Tv mTv = TvClient.getTvInstance();

        protected ATVSessionImpl(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);

            mContext = context;
            mLastBlockedRating = null;
        }

        @Override
        public void doRelease() {
            super.doRelease();
            releasePlayer();
        }

        @Override
        public void doTune(Uri uri) {
            super.doTune(uri);
            switchToSourceInput(uri);
        }

        @Override
        public void doSurfaceChanged(Uri uri) {
            super.doSurfaceChanged(uri);
            switchToSourceInput(uri);
        }

        @Override
        public void doUnblockContent(TvContentRating rating) {
            super.doUnblockContent(rating);
            if (rating != null) {
                unblockContent(rating);
            }
        }

        @Override
        public void onAppPrivateCommand(String action, Bundle data) {
            if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
                stopTv();
            }
        }

        private void switchToSourceInput(Uri uri) {
            mUnblockedRatingSet.clear();

            if (Utils.getChannelId(uri) < 0)
                return;
            ChannelInfo ch = TvContractUtils.getChannelInfoATV(
                    mContext.getContentResolver(), uri);
            if (ch != null) {
                playProgram(ch);
            } else {
                Log.w(TAG, "Failed to get channel info for " + uri);
            }
        }

        private boolean playProgram(ChannelInfo info) {
            mTv.PlayATVProgram(info.frequency, info.videoStd, info.audioStd, info.fineTune, info.audioCompensation);
            client.curChannel = info;
            checkContentBlockNeeded();
            return true;
        }

        private void checkContentBlockNeeded() {
            if (mCurrentContentRating == null
                    || !mTvInputManager.isParentalControlsEnabled()
                    || !mTvInputManager.isRatingBlocked(mCurrentContentRating)
                    || mUnblockedRatingSet.contains(mCurrentContentRating)) {
                // Content rating is changed so we don't need to block anymore.
                // Unblock content here explicitly to resume playback.
                unblockContent(null);
                return;
            }

            mLastBlockedRating = mCurrentContentRating;
            // Children restricted content might be blocked by TV app as well,
            // but TIS should do its best not to show any single frame of blocked content.
            releasePlayer();

            Log.d(TAG, "notifyContentBlocked [rating:" + mCurrentContentRating + "]");
            notifyContentBlocked(mCurrentContentRating);
        }

        private void unblockContent(TvContentRating rating) {
            // TIS should unblock content only if unblock request is legitimate.
            if (rating == null
                    || mLastBlockedRating == null
                    || (mLastBlockedRating != null && rating
                            .equals(mLastBlockedRating))) {
                mLastBlockedRating = null;
                if (rating != null) {
                    mUnblockedRatingSet.add(rating);
                }
                Log.d(TAG, "notifyContentAllowed");
                notifyContentAllowed();
            }
        }
    }

    public static final class TvInput {
        public final String displayName;
        public final String name;
        public final String description;
        public final String logoThumbUrl;
        public final String logoBackgroundUrl;

        public TvInput(String displayName, String name, String description,
                String logoThumbUrl, String logoBackgroundUrl) {
            this.displayName = displayName;
            this.name = name;
            this.description = description;
            this.logoThumbUrl = logoThumbUrl;
            this.logoBackgroundUrl = logoBackgroundUrl;
        }
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getDeviceId() != DroidLogicTvUtils.DEVICE_ID_ATV)
            return null;

        Log.d(TAG, "=====onHardwareAdded=====" + hardwareInfo.toString());

        TvInputInfo info = null;
        ResolveInfo rInfo = getResolveInfo(ATVInputService.class.getName());
        if (rInfo != null) {
            try {
                info = TvInputInfo.createTvInputInfo(this, rInfo, hardwareInfo,
                        getTvInputInfoLabel(hardwareInfo.getDeviceId()), null);
            } catch (Exception e) {
            }
        }
        updateInfoListIfNeededLocked(hardwareInfo, info, false);

        return info;
    }

    public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_TUNER)
            return null;

        TvInputInfo info = getTvInputInfo(hardwareInfo);
        String id = null;
        if (info != null)
            id = info.getId();

        updateInfoListIfNeededLocked(hardwareInfo, info, true);

        Log.d(TAG, "=====onHardwareRemoved===== " + id);
        return id;
    }
}
