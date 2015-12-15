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
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.droidlogic.tvinput.Utils;

import com.droidlogic.app.tv.DroidLogicTvInputService;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.app.tv.TVChannelParams;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.TvInputBaseSession;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.droidlogic.app.tv.TvControlManager;

public class DTVInputService extends DroidLogicTvInputService {

    private static final String TAG = "DTVInputService";

    private static final String PID = "pid";
    private static final String FMT = "fmt";

    private DTVSessionImpl mSession;

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
        intentFilter
                .addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mParentalControlsBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mParentalControlsBroadcastReceiver);
    }

    @Override
    public Session onCreateSession(String inputId) {
        super.onCreateSession(inputId);
        if (mSession == null || !TextUtils.equals(inputId, mSession.getInputId())) {
            mSession = new DTVSessionImpl(this, inputId, getHardwareDeviceId(inputId));
            registerInputSession(mSession);
        }
        return mSession;
    }

    public class DTVSessionImpl extends TvInputBaseSession implements TvControlManager.AVPlaybackListener{
        private final Context mContext;
        private TvInputManager mTvInputManager;
        private TvContentRating mLastBlockedRating;
        private TvContentRating mCurrentContentRating;
        private final Set<TvContentRating> mUnblockedRatingSet = new HashSet<>();
        private ChannelInfo mCurrentChannel;

        protected DTVSessionImpl(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);

            mContext = context;
            mLastBlockedRating = null;
            mCurrentChannel = null;
        }

        @Override
        public void stopTvPlay() {
            super.stopTvPlay();
            releasePlayer();
        }

        @Override
        public void doRelease() {
            super.doRelease();
            mSession = null;
            releasePlayer();
        }

        @Override
        public void doSurfaceChanged(Uri uri) {
            super.doSurfaceChanged(uri);
            switchToSourceInput(uri);
        }

        @Override
        public void doTune(Uri uri) {
            super.doTune(uri);
            switchToSourceInput(uri);
        }

        @Override
        public void doAppPrivateCmd(String action, Bundle bundle) {
            super.doAppPrivateCmd(action, bundle);
            if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
                stopTv();
            }
        }

        @Override
        public void doUnblockContent(TvContentRating rating) {
            super.doUnblockContent(rating);
            if (rating != null) {
                unblockContent(rating);
            }
        }

        private void switchToSourceInput(Uri uri) {
            mUnblockedRatingSet.clear();

            if (Utils.getChannelId(uri) < 0) {
                TvControlManager mTvControlManager = TvControlManager.open();
                mTvControlManager.PlayDTVProgram(TVChannelParams.MODE_DTMB, 0, 0, 0, 0, 0, -1, -1, 0, 0);
                return;
            }
            TvDataBaseManager mTvDataBaseManager = new TvDataBaseManager(mContext);
            ChannelInfo ch = mTvDataBaseManager.getChannelInfo(uri);
            if (ch != null) {
                playProgram(ch);
                TvControlManager.open().SetAVPlaybackListener(this);
                notifyTracks(ch);
                mCurrentChannel = ch;
            } else {
                Log.w(TAG, "Failed to get channel info for " + uri);
                TvControlManager.open().SetAVPlaybackListener(null);
            }
        }

        private boolean playProgram(ChannelInfo info) {
            info.print();
            int mode = Utils.type2mode(info.getType());
            if (mode == TVChannelParams.MODE_DTMB) {
                TvControlManager mTvControlManager = TvControlManager.open();
                mTvControlManager.PlayDTVProgram(
                        mode,
                        info.getFrequency(),
                        info.getBandwidth(),
                        0,
                        info.getVideoPid(),
                        info.getVfmt(),
                        (info.getAudioPids() != null) ? info.getAudioPids()[info.getAudioTrackIndex()] : -1,
                        (info.getAudioFormats() != null) ? info.getAudioFormats()[info.getAudioTrackIndex()] : -1,
                        info.getPcrPid(),
                        info.getAudioCompensation());
            } else
                Log.d(TAG, "channel type[" + info.getType() + "] not supported yet.");
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
            // but TIS should do its best not to show any single frame of
            // blocked content.
            releasePlayer();

            Log.d(TAG, "notifyContentBlocked [rating:" + mCurrentContentRating + "]");
            notifyContentBlocked(mCurrentContentRating);
        }

        private void unblockContent(TvContentRating rating) {
            // TIS should unblock content only if unblock request is legitimate.
            if (rating == null
                    || mLastBlockedRating == null
                    || (mLastBlockedRating != null && rating.equals(mLastBlockedRating))) {
                mLastBlockedRating = null;
                if (rating != null) {
                    mUnblockedRatingSet.add(rating);
                }
                Log.d(TAG, "notifyContentAllowed");
                notifyContentAllowed();
            }
        }

        @Override
        public void onEvent(TvControlManager.AVPlaybackEvent ev) {
            Log.d(TAG, "AV evt:"+ev.mMsgType);
            if (ev.mMsgType == TvControlManager.EVENT_AV_SCRAMBLED)
                mSession.notifySessionEvent(DroidLogicTvUtils.AV_SIG_SCRAMBLED, null);
            else if (ev.mMsgType == TvControlManager.EVENT_AV_PLAYBACK_NODATA)
                ;
            else if (ev.mMsgType == TvControlManager.EVENT_AV_PLAYBACK_RESUME)
                notifyVideoAvailable();
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            Log.d(TAG, "onSelectTrack: [type:"+type+"] [id:"+trackId+"]");

            if (mCurrentChannel == null)
                return false;

            if (type == TvTrackInfo.TYPE_AUDIO) {

                TvControlManager mTvControlManager = TvControlManager.open();
                Map<String, String> parsedMap = ChannelInfo.stringToMap(trackId);
                mTvControlManager.DtvSwitchAudioTrack(Integer.parseInt(parsedMap.get("pid")),
                                        Integer.parseInt(parsedMap.get("fmt")),
                                        0);

                notifyTrackSelected(type, trackId);

                TvDataBaseManager mTvDataBaseManager = new TvDataBaseManager(mContext);
                mCurrentChannel.setAudioTrackIndex(Integer.parseInt(parsedMap.get("id")));
                mTvDataBaseManager.updateChannelInfo(mCurrentChannel);
                return true;
            }
            return false;
        }

        private void notifyTracks(ChannelInfo ch) {
            int[] audioPids = ch.getAudioPids();
            int AudioTracksCount = audioPids.length;
            if (AudioTracksCount != 0) {
                Log.d(TAG, "notify audio tracks:");
                String[] audioLanguages = ch.getAudioLangs();
                int[] audioFormats = ch.getAudioFormats();
                List < TvTrackInfo > tracks = new ArrayList<>();
                String SelectedId = null;
                for (int i=0; i<AudioTracksCount; i++) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("id", String.valueOf(i));
                    map.put("pid", String.valueOf(audioPids[i]));
                    map.put("fmt", String.valueOf(audioFormats[i]));
                    String Id = ChannelInfo.mapToString(map);
                    TvTrackInfo AudioTrack =
                        new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Id)
                                       .setLanguage(audioLanguages[i])
                                       .setAudioChannelCount(2)
                                       .build();
                    tracks.add(AudioTrack);
                    if (ch.getAudioTrackIndex() == i)
                        SelectedId = Id;
                    Log.d(TAG, "\t"+i+": ["+audioLanguages[i]+"] [pid:"+audioPids[i]+"] [fmt:"+audioFormats[i]+"]");
                }
                notifyTracksChanged(tracks);
                if (SelectedId != null) {
                    Log.d(TAG, "\tselected: ["+SelectedId+"]");
                    notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, SelectedId);
                }
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
        if (hardwareInfo.getDeviceId() != DroidLogicTvUtils.DEVICE_ID_DTV)
            return null;

        Log.d(TAG, "=====onHardwareAdded=====" + hardwareInfo.getDeviceId());

        TvInputInfo info = null;
        ResolveInfo rInfo = getResolveInfo(DTVInputService.class.getName());
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
