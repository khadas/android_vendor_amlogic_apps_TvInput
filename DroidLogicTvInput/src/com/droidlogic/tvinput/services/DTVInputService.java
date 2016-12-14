package com.droidlogic.tvinput.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ContentUris;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvContract;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputManager.Hardware;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.database.ContentObserver;
import android.database.IContentObserver;
import android.provider.Settings;

import com.droidlogic.tvinput.Utils;

import com.droidlogic.app.tv.DroidLogicTvInputService;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.app.tv.TVChannelParams;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.TvInputBaseSession;
import com.droidlogic.app.tv.Program;
import com.droidlogic.app.tv.TVMultilingualText;
import com.droidlogic.app.tv.TVTime;
import com.droidlogic.app.tv.TvStoreManager;
import com.droidlogic.app.SystemControlManager;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

import com.droidlogic.app.tv.TvControlManager;

import java.util.HashMap;
import java.util.Map;
import android.net.Uri;
import android.view.Surface;

public class DTVInputService extends DroidLogicTvInputService {

    private static final String TAG = "DTVInputService";

    private static final String DTV_AUTO_RESCAN_SERVICE = "tv.dtv.auto_rescan_service";
    private static final String DTV_AUTO_RESCAN_TIME = "tv.dtv.auto_rescan_time";
    private static final String DTV_AUDIO_AD_DISABLE = "tv.dtv.ad.disable";
    private static final String DTV_CHANNEL_NUMBER_START = "tv.channel.number.start";

    private static final String DTV_AUDIO_TRACK_IDX = "tv.dtv.audio_track_idx";
    private static final String DTV_AUDIO_AD_TRACK_IDX = "tv.dtv.audio_ad_track_idx";
    private static final String DTV_SUBTITLE_TRACK_IDX = "tv.dtv.subtitle_track_idx";

    private static final String DTV_SUBTITLE_AUTO_START = "tv.dtv.subtitle.autostart";

    private DTVSessionImpl mCurrentSession;
    private int id = 0;

    private Map<Integer, DTVSessionImpl> sessionMap = new HashMap<>();
    private final BroadcastReceiver mParentalControlsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCurrentSession != null) {
                String action = intent.getAction();
                if (action.equals(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED)
                    || action.equals(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED)) {
                    mCurrentSession.checkContentBlockNeeded();
                } else if (action.equals(Intent.ACTION_TIME_CHANGED)) {
                    Log.d(TAG, "SysTime changed.");
                    mCurrentSession.restartMonitorTime();
                }
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
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
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

        mCurrentSession = new DTVSessionImpl(this, inputId, getHardwareDeviceId(inputId));
        registerInputSession(mCurrentSession);
        mCurrentSession.setSessionId(id);
        sessionMap.put(id, mCurrentSession);
        id++;

        return mCurrentSession;
    }

    @Override
    public void tvPlayStopped(int sessionId) {
        DTVSessionImpl session = sessionMap.get(sessionId);
        if (session != null) {
            session.stopSubtitle();
            session.setMonitor(null);
        }
    }

    @Override
    public void setCurrentSessionById(int sessionId) {
        Utils.logd(TAG, "setCurrentSessionById:"+sessionId);
        DTVSessionImpl session = sessionMap.get(sessionId);
        if (session != null) {
            mCurrentSession = session;
        }
    }

    @Override
    public void doTuneFinish(int result, Uri uri, int sessionId) {
        Log.d(TAG, "doTuneFinish,result:"+result+"sessionId:"+sessionId);
        if (result == ACTION_SUCCESS) {
            DTVSessionImpl session = sessionMap.get(sessionId);
            if (session != null)
                session.switchToSourceInput(uri);
        }
    }

    /*set below 3 vars true to enable tracks-auto-select in this service.*/
    private static boolean subtitleAutoSave = false;
    private static boolean audioAutoSave = false;
    private static boolean subtitleAutoStart = false;

    /*associate audio*/
    private static boolean audioADAutoStart = false;

    /*only one monitor instance for all sessions*/
    private static HandlerThread mHandlerThread = null;
    private static Handler mHandler = null;
    private static DTVSessionImpl.DTVMonitor monitor = null;
    private final Object mLock = new Object();
    private static String mDtvType = TvContract.Channels.TYPE_DTMB;
    private Uri  mCurrentUri;

    public class DTVSessionImpl extends TvInputBaseSession implements TvControlManager.AVPlaybackListener {
        private final Context mContext;
        private TvInputManager mTvInputManager;
        private TvDataBaseManager mTvDataBaseManager;
        private TvControlManager mTvControlManager;
        private TvContentRating mLastBlockedRating;
        private TvContentRating mCurrentContentRating;
        private final Set<TvContentRating> mUnblockedRatingSet = new HashSet<>();
        private ChannelInfo mCurrentChannel;
        private SystemControlManager mSystemControlManager;

        private final static int AD_MIXING_LEVEL_DEF = 50;
        private int mAudioADMixingLevel = -1;

        protected DTVSessionImpl(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);

            mContext = context;
            mTvDataBaseManager = new TvDataBaseManager(mContext);
            mTvControlManager = TvControlManager.getInstance();
            mSystemControlManager = new SystemControlManager(mContext);
            mLastBlockedRating = null;
            mCurrentChannel = null;
            mCurrentUri = null;
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            return setSurfaceInService(surface,this);
        }

        @Override
        public boolean onTune(Uri channelUri) {
            return doTuneInService(channelUri, getSessionId());
        }

        @Override
        public void doRelease() {
            super.doRelease();
            stopSubtitle();
            setMonitor(null);
        }

        @Override
        public void doAppPrivateCmd(String action, Bundle bundle) {
            if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
                stopTv();
            } else if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_PLAY, action)) {
                Log.d(TAG, "do private cmd: STOP_PLAY");
                stopSubtitle();
                setMonitor(null);
                releasePlayer();
            } else if (TextUtils.equals(DroidLogicTvUtils.ACTION_DTV_AUTO_TRACKS, action)) {
                Log.d(TAG, "do private cmd: AUTO_TRACKS");
                subtitleAutoSave = true;
                audioAutoSave = true;
                subtitleAutoStart = true;
                if (mCurrentChannel != null)
                    startSubtitle(mCurrentChannel);
            } else if (DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN.equals(action)
                || DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN.equals(action)) {
                Log.d(TAG, "do private cmd: DTV_XXX_SCAN, stop play...");
                mCurrentUri = null;
                stopSubtitle();
                setMonitor(null);
                //releasePlayer();
                mTvControlManager.PlayDTVProgram(
                    new TvControlManager.TvMode(mDtvType).getMode(), 470000000, 0, 0, 0, 0, -1, -1, 0, 0, false);
                resetScanStoreListener();
            }  else if (TextUtils.equals(DroidLogicTvUtils.ACTION_DTV_SET_TYPE, action)) {
                mDtvType = bundle.getString(DroidLogicTvUtils.PARA_TYPE);
                Log.d(TAG, "do private cmd: DTV_SET_TYPE ["+mDtvType+"]");
            } else if (TextUtils.equals(DroidLogicTvUtils.ACTION_DTV_ENABLE_AUDIO_AD, action)) {
                audioADAutoStart = (bundle.getInt(DroidLogicTvUtils.PARA_ENABLE) == 0)? false : true;
                int adTrackIndex = bundle.getInt(DroidLogicTvUtils.PARA_VALUE1);
                Log.d(TAG, "do private cmd: ACTION_DTV_ENABLE_AUDIO_AD enable["+audioADAutoStart+"] track["+adTrackIndex+"]");
                if (TextUtils.equals(mSystemControlManager.getProperty(DTV_AUDIO_AD_DISABLE),"1")) {
                    audioADAutoStart = false;
                    return;
                }
                if (mCurrentChannel != null) {
                    if (audioADAutoStart) {
                        if (adTrackIndex == -1)
                            startAudioADByMain(mCurrentChannel, getAudioTrackAuto(mCurrentChannel));
                        else
                            startAudioAD(mCurrentChannel, adTrackIndex);
                        return;
                    }
                }
                stopAudioAD();
            } else if (TextUtils.equals(DroidLogicTvUtils.ACTION_AD_MIXING_LEVEL, action)) {
                mAudioADMixingLevel = bundle.getInt(DroidLogicTvUtils.PARA_VALUE1);
                Log.d(TAG, "do private cmd: ACTION_AD_MIXING_LEVEL ["+mAudioADMixingLevel+"]");
            }

            super.doAppPrivateCmd(action, bundle);
        }

        @Override
        public void doUnblockContent(TvContentRating rating) {
            super.doUnblockContent(rating);
            if (rating != null) {
                unblockContent(rating);
            }
        }

        private void switchToSourceInput(Uri uri) {
            mCurrentUri = uri;
            mUnblockedRatingSet.clear();

            subtitleAutoStart = mSystemControlManager.getPropertyBoolean(DTV_SUBTITLE_AUTO_START, false);
            subtitleAutoSave = subtitleAutoStart;

            Log.d(TAG, "switchToSourceInput  uri=" + uri + " this:"+ this);
            if (Utils.getChannelId(uri) < 0) {
                mTvControlManager.PlayDTVProgram(
                    new TvControlManager.TvMode(mDtvType).getMode(), 470000000, 0, 0, 0, 0, -1, -1, 0, 0, false);
                mCurrentChannel = null;
                return;
            }
            ChannelInfo ch = mTvDataBaseManager.getChannelInfo(uri);
            if (ch != null) {
                playProgram(ch);
                mCurrentChannel = ch;
            } else {
                Log.w(TAG, "Failed to get channel info for " + uri);
                mTvControlManager.SetAVPlaybackListener(null);
            }
        }

        private boolean playProgram(ChannelInfo info) {
            info.print();
            TvControlManager.TvMode mTvMode = new TvControlManager.TvMode(info.getType());
            int baseMode = mTvMode.getBase();
            int audioTrackAuto = getAudioTrackAuto(info);
            int para1 = 0;
            int para2 = 0;

            if (baseMode == TVChannelParams.MODE_DTMB) {
                para1 = info.getBandwidth();
            } else if (baseMode == TVChannelParams.MODE_QAM) {
                para1 = info.getSymbolRate();
                para2 = info.getModulation();
            } else if (baseMode == TVChannelParams.MODE_QPSK) {
                para1 = info.getSymbolRate();
            } else if (baseMode == TVChannelParams.MODE_ATSC) {
                para1 = info.getModulation();
            } else if (baseMode == TVChannelParams.MODE_OFDM) {
                para1 = info.getBandwidth();
            } else {
                    Log.d(TAG, "channel type[" + info.getType() + "] not supported yet.");
                    return false;
            }

            int mixingLevel = mAudioADMixingLevel;
            if (mixingLevel < 0)
                mixingLevel = Settings.System.getInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_AD_MIX, AD_MIXING_LEVEL_DEF);

            mTvControlManager.PlayDTVProgram(
                    mTvMode.getMode(),
                    info.getFrequency(),
                    para1,
                    para2,
                    info.getVideoPid(),
                    info.getVfmt(),
                    (audioTrackAuto >= 0) ? info.getAudioPids()[audioTrackAuto] : -1,
                    (audioTrackAuto >= 0) ? info.getAudioFormats()[audioTrackAuto] : -1,
                    info.getPcrPid(),
                    info.getAudioCompensation(),
                    DroidLogicTvUtils.hasAudioADTracks(info),
                    mixingLevel);
                    mTvControlManager.DtvSetAudioChannleMod(info.getAudioChannel());
                    mTvControlManager.SetAVPlaybackListener(this);
                    mSystemControlManager.setProperty(DTV_AUDIO_TRACK_IDX,
                        ((audioTrackAuto>=0)? String.valueOf(audioTrackAuto) : "-1"));

            stopSubtitle();

            notifyTracks(info);

            startSubtitle(info);

            startAudioADByMain(info, audioTrackAuto);

            setMonitor(info);

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
            stopSubtitle();
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
        public void onEvent(int msgType, int programID) {
            Log.d(TAG, "AV evt:" + msgType);
            if (msgType == TvControlManager.EVENT_AV_SCRAMBLED)
                notifySessionEvent(DroidLogicTvUtils.AV_SIG_SCRAMBLED, null);
            else if (msgType == TvControlManager.EVENT_AV_PLAYBACK_NODATA)
                ;
            else if (msgType == TvControlManager.EVENT_AV_PLAYBACK_RESUME) {
                if (mCurrentChannel != null && ChannelInfo.isRadioChannel(mCurrentChannel)) {
                    mTvControlManager.SetAudioMuteForTv(TvControlManager.AUDIO_UNMUTE_FOR_TV);
                    notifyVideoAvailable();
                }
            } else if (msgType == TvControlManager.EVENT_AV_VIDEO_AVAILABLE) {
                notifyVideoAvailable();

                String height = mSystemControlManager.readSysFs("/sys/class/video/frame_height");
                String pi = mSystemControlManager.readSysFs("/sys/class/deinterlace/di0/frame_format");
                String format = DroidLogicTvUtils.convertVideoFormat(height, pi);
                if (!TextUtils.equals(format, mCurrentChannel.getVideoFormat())) {
                    mCurrentChannel.setVideoFormat(format);
                    mTvDataBaseManager.updateChannelInfo(mCurrentChannel);
                }

                // TODO: audioinfo only for test here, should be used by app
                TvControlManager.AudioFormatInfo audioInfo = mTvControlManager.DtvGetAudioFormatInfo();
                mSystemControlManager.setProperty("tv.dtv.audio.channels",
                    String.valueOf(audioInfo.ChannelsOriginal)+"."+String.valueOf(audioInfo.LFEPresentOriginal));
            }
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            Log.d(TAG, "onSelectTrack: [type:" + type + "] [id:" + trackId + "]");

            if (mCurrentChannel == null)
                return false;

            if (type == TvTrackInfo.TYPE_AUDIO) {
                int index = -1;
                Map<String, String> parsedMap = ChannelInfo.stringToMap(trackId);
                index = Integer.parseInt(parsedMap.get("id"));

                stopAudioAD();

                mTvControlManager.DtvSwitchAudioTrack(Integer.parseInt(parsedMap.get("pid")),
                                        Integer.parseInt(parsedMap.get("fmt")),
                                        0);
                mSystemControlManager.setProperty(DTV_AUDIO_TRACK_IDX, String.valueOf(index));

                startAudioADByMain(mCurrentChannel, index);

                notifyTrackSelected(type, trackId);

                if (audioAutoSave) {
                    Log.d(TAG, "audioAutoSave: idx=" + index);
                    mCurrentChannel.setAudioTrackIndex(index);
                    mTvDataBaseManager.updateChannelInfo(mCurrentChannel);
                }

                return true;

            } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
                int index = -1;
                if (trackId == null) {
                    stopSubtitle();
                    index = -2;
                } else {
                    Map<String, String> parsedMap = ChannelInfo.stringToMap(trackId);
                    index = Integer.parseInt(parsedMap.get("id"));
                    startSubtitle(Integer.parseInt(parsedMap.get("type")),
                                  Integer.parseInt(parsedMap.get("pid")),
                                  Integer.parseInt(parsedMap.get("stype")),
                                  Integer.parseInt(parsedMap.get("uid1")),
                                  Integer.parseInt(parsedMap.get("uid2")));
                    mSystemControlManager.setProperty(DTV_SUBTITLE_TRACK_IDX, String.valueOf(index));
                }
                notifyTrackSelected(type, trackId);

                if (subtitleAutoSave) {
                    Log.d(TAG, "subtitleAutoSave: idx=" + index);
                    mCurrentChannel.setSubtitleTrackIndex(index);
                    mTvDataBaseManager.updateChannelInfo(mCurrentChannel);
                }

                return true;
            }
            return false;
        }

        @Override
        public View onCreateOverlayView() {
            Log.d(TAG, "onCreateOverlayView");
            return initSubtitleView();
        }

        @Override
        public void onOverlayViewSizeChanged(int width, int height) {
            Log.d(TAG, "onOverlayViewSizeChanged[" + width + "," + height + "]");
        }

        private void notifyTracks(ChannelInfo ch) {
            List < TvTrackInfo > tracks = null;
            String AudioSelectedId = null;
            String SubSelectedId = null;

            int[] audioPids = ch.getAudioPids();
            int AudioTracksCount = (audioPids == null) ? 0 : audioPids.length;
            if (AudioTracksCount != 0) {
                Log.d(TAG, "notify audio tracks["+AudioTracksCount+"]:");
                String[] audioLanguages = ch.getAudioLangs();
                int[] audioFormats = ch.getAudioFormats();
                int[] audioExts = ch.getAudioExts();
                int audioTrackAuto = getAudioTrackAuto(ch);

                if (tracks == null)
                    tracks = new ArrayList<>();

                for (int i = 0; i < AudioTracksCount; i++) {
                    boolean isDefault = false;
                    if (audioTrackAuto == i)
                        isDefault = true;

                    Map<String, String> map = new HashMap<String, String>();
                    map.put("id", String.valueOf(i));
                    map.put("pid", String.valueOf(audioPids[i]));
                    map.put("fmt", String.valueOf(audioFormats[i]));
                    map.put("ext", String.valueOf(audioExts[i]));
                    //if (isDefault)
                    //    map.put("default", String.valueOf(1));
                    String Id = ChannelInfo.mapToString(map);
                    TvTrackInfo AudioTrack =
                        new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Id)
                    .setLanguage(audioLanguages[i])
                    .setAudioChannelCount(2)
                    .build();
                    tracks.add(AudioTrack);
                    if (isDefault)
                        AudioSelectedId = Id;
                    Log.d(TAG, "\t" + i + ": [" + audioLanguages[i] + "] [pid:" + audioPids[i] + "] [fmt:" + audioFormats[i] + "]"
                        + " [ext:" + Integer.toHexString(audioExts[i]) + "]");
                }
            }

            int[] subPids = ch.getSubtitlePids();
            int SubTracksCount = (subPids == null) ? 0 : subPids.length;
            if (SubTracksCount != 0) {
                Log.d(TAG, "notify subtitle tracks["+SubTracksCount+"]:");
                String[] subLanguages = ch.getSubtitleLangs();
                int[] subTypes = ch.getSubtitleTypes();
                int[] subStypes = ch.getSubtitleStypes();
                int[] subId1s = ch.getSubtitleId1s();
                int[] subId2s = ch.getSubtitleId2s();
                int subTrackAuto = getSubtitleTrackAuto(ch);

                if (tracks == null)
                    tracks = new ArrayList<>();

                for (int i = 0; i < SubTracksCount; i++) {
                    boolean isDefault = false;
                    if (subTrackAuto == i)
                        isDefault = true;

                    Map<String, String> map = new HashMap<String, String>();
                    map.put("id", String.valueOf(i));
                    map.put("pid", String.valueOf(subPids[i]));
                    map.put("type", String.valueOf(subTypes[i]));
                    map.put("stype", String.valueOf(subStypes[i]));
                    map.put("uid1", String.valueOf(subId1s[i]));
                    map.put("uid2", String.valueOf(subId2s[i]));
                    //if (isDefault)
                    //    map.put("default", String.valueOf(1));
                    String Id = ChannelInfo.mapToString(map);
                    TvTrackInfo SubtitleTrack =
                        new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, Id)
                    .setLanguage(subLanguages[i])
                    .build();
                    tracks.add(SubtitleTrack);
                    if (isDefault && subtitleAutoStart)
                        SubSelectedId = Id;
                    Log.d(TAG, "\t" + i + ": [" + subLanguages[i] + "] [pid:" + subPids[i] + "] [type:" + subTypes[i] + "]");
                    Log.d(TAG, "\t" + "   [id1:" + subId1s[i] + "] [id2:" + subId2s[i] + "] [stype:" + subStypes[i] + "]");
                }

            }

            if (tracks != null)
                notifyTracksChanged(tracks);

            Log.d(TAG, "\tAuto Aud: [" + AudioSelectedId + "]");
            notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, AudioSelectedId);

            Log.d(TAG, "\tAuto Sub: [" + SubSelectedId + "]");
            notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, SubSelectedId);
        }

        /*
                Auto rule: channel specified track > default language track > system language > the 1st track > -1
            */
        private int getAudioTrackAuto(ChannelInfo info) {
            String[] trackLangArray = info.getAudioLangs();
            /*no audio tracks, get fail.*/
            if (trackLangArray == null)
                return -1;

            int index = info.getAudioTrackIndex();

            /*off by user*/
            if (index == -2)
                return -2;

            /*if valid*/
            if (index >= 0 && index < trackLangArray.length)
                return index;

            /*default language track*/
            String def_lan = Settings.System.getString(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_DEFAULT_LANGUAGE);
            if (def_lan == null)/*system language track*/
                def_lan = TVMultilingualText.getLocalLang();
            for (int trackIdx = 0; trackIdx < trackLangArray.length; trackIdx++) {
                if (trackLangArray[trackIdx].equals(def_lan))
                    return trackIdx;
            }

            /*none match, use the 1st.*/
            return 0;
        }

        private int getSubtitleTrackAuto(ChannelInfo info) {
            String[] trackLangArray = info.getSubtitleLangs();
            if (trackLangArray == null)
                return -1;

            int index = info.getSubtitleTrackIndex();

            /*off by user*/
            if (index == -2)
                return -2;

            /*if valid*/
            if (index >= 0 && index < trackLangArray.length)
                return index;

            String def_lan = Settings.System.getString(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_DEFAULT_LANGUAGE);
            if (def_lan == null)
                def_lan = TVMultilingualText.getLocalLang();
            for (int trackIdx = 0; trackIdx < trackLangArray.length; trackIdx++) {
                if (trackLangArray[trackIdx].equals(def_lan))
                    return trackIdx;
            }

            /*none match, use the 1st.*/
            return 0;
        }


        private DTVSubtitleView mSubtitleView = null;
        private static final int TYPE_DVB_SUBTITLE = 1;
        private static final int TYPE_DTV_TELETEXT = 2;
        private static final int TYPE_ATV_TELETEXT = 3;
        private static final int TYPE_DTV_CC = 4;
        private static final int TYPE_ATV_CC = 5;

        private void startSubtitle(ChannelInfo channelInfo) {

            if (!subtitleAutoStart)
                return ;

            int idx = getSubtitleTrackAuto(channelInfo);
            if (idx >= 0) {
                startSubtitle(channelInfo.getSubtitleTypes()[idx],
                              channelInfo.getSubtitlePids()[idx],
                              channelInfo.getSubtitleStypes()[idx],
                              channelInfo.getSubtitleId1s()[idx],
                              channelInfo.getSubtitleId2s()[idx]);
                mSystemControlManager.setProperty(DTV_SUBTITLE_TRACK_IDX, String.valueOf(idx));
            } else {
                stopSubtitle();
            }
        }

        private int getTeletextRegionID(String ttxRegionName) {
            final String[] supportedRegions = {"English", "Deutsch", "Svenska/Suomi/Magyar",
                                               "Italiano", "Fran?ais", "Português/Espa?ol",
                                               "Cesky/Slovencina", "Türk?e", "Ellinika", "Alarabia / English"
                                              };
            final int[] regionIDMaps = {16, 17, 18, 19, 20, 21, 14, 22, 55 , 64};

            int i;
            for (i = 0; i < supportedRegions.length; i++) {
                if (supportedRegions[i].equals(ttxRegionName))
                    break;
            }

            if (i >= supportedRegions.length) {
                Log.d(TAG, "Teletext defaut region " + ttxRegionName +
                      " not found, using 'English' as default!");
                i = 0;
            }

            Log.d(TAG, "Teletext default region id: " + regionIDMaps[i]);
            return regionIDMaps[i];
        }

        private View initSubtitleView() {
            if (mSubtitleView == null) {
                mSubtitleView = new DTVSubtitleView(mContext);
            }
            return mSubtitleView;
        }

        private void startSubtitle(int type, int pid, int stype, int id1, int id2) {
            Log.d(TAG, "start Subtitle");

            initSubtitleView();

            mSubtitleView.stop();

            if (type == TYPE_DVB_SUBTITLE) {

                DTVSubtitleView.DVBSubParams params =
                    new DTVSubtitleView.DVBSubParams(0, pid, id1, id2);
                mSubtitleView.setSubParams(params);

            } else if (type == TYPE_DTV_TELETEXT) {

                int pgno;
                pgno = (id1 == 0) ? 800 : id1 * 100;
                pgno += (id2 & 15) + ((id2 >> 4) & 15) * 10 + ((id2 >> 8) & 15) * 100;
                DTVSubtitleView.DTVTTParams params =
                    new DTVSubtitleView.DTVTTParams(0, pid, pgno, 0x3F7F, getTeletextRegionID("English"));
                mSubtitleView.setSubParams(params);

            }
            mSubtitleView.setActive(true);
            mSubtitleView.startSub(); //DVBSub/EBUSub/CC

            setOverlayViewEnabled(true);

        }

        private void stopSubtitle() {
            Log.d(TAG, "stop Subtitle");

            if (mSubtitleView != null) {
                mSubtitleView.stop();
                setOverlayViewEnabled(false);
            }

            mSystemControlManager.setProperty(DTV_SUBTITLE_TRACK_IDX, "-1");
        }

        private void startAudioADByMain(ChannelInfo channelInfo, int mainAudioTrackIndex) {

            if (!audioADAutoStart)
                return ;

            int[] idxs = DroidLogicTvUtils.getAudioADTracks(channelInfo, mainAudioTrackIndex);
            Log.d(TAG, "startAudioAD mainAudioTrackIndex["+mainAudioTrackIndex+"], AudioADTrackIndex["+Arrays.toString(idxs)+"]");

            startAudioAD(channelInfo, ((idxs == null)? -1 : idxs[0]));
        }

        private void startAudioAD(ChannelInfo channelInfo, int adAudioTrackIndex) {
            Log.d(TAG, "startAudioAD idx["+adAudioTrackIndex+"]");
            if (adAudioTrackIndex >= 0 && adAudioTrackIndex < channelInfo.getAudioPids().length) {
                mTvControlManager.DtvSetAudioAD(1,
                                channelInfo.getAudioPids()[adAudioTrackIndex],
                                channelInfo.getAudioFormats()[adAudioTrackIndex]);
                mSystemControlManager.setProperty(DTV_AUDIO_AD_TRACK_IDX, String.valueOf(adAudioTrackIndex));
            } else {
                stopAudioAD();
            }
        }

        private void stopAudioAD() {
            mTvControlManager.DtvSetAudioAD(0, 0, 0);
            mSystemControlManager.setProperty(DTV_AUDIO_AD_TRACK_IDX, "-1");
        }

        private void initThread(String name) {
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread(name);
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper());
            }
        }

        private void releaseThread() {
            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandlerThread = null;
                mHandler = null;
            }
        }

        private DTVMonitorCurrentProgramRunnable mMonitorCurrentProgramRunnable;

        private static final int MONITOR_FEND = 0;
        private static final int MONITOR_DMX = 0;
        private static final int MONITOR_MODE = DTVMonitor.MODE_UPDATE_SERVICE
                                                | DTVMonitor.MODE_UPDATE_EPG
                                                | DTVMonitor.MODE_UPDATE_TIME;
        private static final String EPG_LANGUAGE = "local eng zho chi chs first";
        private static final String DEF_CODING = "GB2312";//"standard";//force setting for auto-detect fail.

        private class DTVMonitorCurrentProgramRunnable implements Runnable {
            private final ChannelInfo mChannel;

            public DTVMonitorCurrentProgramRunnable(ChannelInfo channel) {
                mChannel = channel;
            }

            @Override
            public void run() {
                synchronized (mLock) {
                    Log.d(TAG, "monitor ch: " + mChannel.getDisplayNumber() + "-" + mChannel.getDisplayName());
                    if (monitor == null) {
                        monitor = new DTVMonitor(mContext, getInputId(), DEF_CODING, MONITOR_MODE);
                        monitor.reset(MONITOR_FEND, MONITOR_DMX,
                                  new TvControlManager.TvMode(mChannel.getType()).getBase(),
                                  EPG_LANGUAGE.replaceAll("local", TVMultilingualText.getLocalLang()));
                    }

                    monitor.enterChannel(getTVChannelParams(mChannel), false);
                    monitor.enterService(mChannel);

                    monitor.setEpgAutoReset(true);
                }
            }
        }

        private TVChannelParams getTVChannelParams(ChannelInfo channel) {
            TVChannelParams params = null;
            String type = channel.getType();
            if (TextUtils.equals(type, TvContract.Channels.TYPE_DTMB))
                params = TVChannelParams.dtmbParams(channel.getFrequency(), channel.getBandwidth());
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_C))
                params = TVChannelParams.dvbcParams(channel.getFrequency(), channel.getModulation(), channel.getSymbolRate());
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_T))
                params = TVChannelParams.dvbtParams(channel.getFrequency(), channel.getBandwidth());
            /*else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_S))
                params = TVChannelParams.dvbsParams(channel.getFrequency(), channel.getModulation(), channel.getSymbolRate());*/
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_C2))
                params = TVChannelParams.dvbcParams(channel.getFrequency(), channel.getModulation(), channel.getSymbolRate());
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_T2))
                params = TVChannelParams.dvbt2Params(channel.getFrequency(), channel.getBandwidth());
            /*else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_S2))
                params = TVChannelParams.dvbsParams(channel.getFrequency(), channel.getModulation(), channel.getSymbolRate());*/
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_T)
                ||TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_C))
                params = TVChannelParams.atscParams(channel.getFrequency(), channel.getModulation());
            else if (TextUtils.equals(type, TvContract.Channels.TYPE_ISDB_T))
                params = TVChannelParams.isdbtParams(channel.getFrequency(), channel.getBandwidth());
           return params;
        }

        private void setMonitor(ChannelInfo channel) {
            synchronized (mLock) {
                if (channel != null) {
                    Log.d(TAG, "startMonitor");

                    initThread("DTVMonitor Thread");
                    mHandler.removeCallbacks(mMonitorCurrentProgramRunnable);
                    mMonitorCurrentProgramRunnable = new DTVMonitorCurrentProgramRunnable(channel);
                    mHandler.post(mMonitorCurrentProgramRunnable);
                } else {
                    if (monitor != null) {
                        monitor.destroy();
                        monitor = null;
                    }
                    releaseThread();

                    Log.d(TAG, "stopMonitor");
                }
            }
        }

        private void restartMonitorTime() {
            synchronized (mLock) {
                Log.d(TAG, "restartMonitorTime");
                if (monitor != null)
                    monitor.restartMonitorTime();
            }
        }

        public class DTVMonitor {
            private static final String TAG = "DTVMonitor";
            private static final int MSG_MONITOR_EVENT = 1000;
            private static final int MSG_MONITOR_RESCAN_SERVICE = 2000;
            private static final int MSG_MONITOR_RESCAN_TIME = 3000;

            private static final int MODE_UPDATE_EPG = DTVEpgScanner.SCAN_EIT_ALL;
            private static final int MODE_UPDATE_SERVICE = DTVEpgScanner.SCAN_SDT;
            private static final int MODE_UPDATE_TS = DTVEpgScanner.SCAN_NIT;
            private static final int MODE_UPDATE_TIME = DTVEpgScanner.SCAN_TDT;

            private static final int AUTO_RESCAN_NONE = 0;
            private static final int AUTO_RESCAN_ONCE = 1;
            private static final int AUTO_RESCAN_CONTINUOUS = 2;
            /*
                Rescan service info periodically @AUTO_RESCAN_INTERVAL for data changing,
                due to only version-triggered at low-level
            */
            private int auto_rescan_service = AUTO_RESCAN_CONTINUOUS;

            private static final int AUTO_RESCAN_SERVICE_INTERVAL = 5000;//5s

            private int auto_rescan_time = AUTO_RESCAN_CONTINUOUS;

            private static final int AUTO_RESCAN_TIME_INTERVAL = 5000;//5s

            /*Retune current uri if necessary*/
            private boolean auto_retune_service = true;

            private HandlerThread mHandlerThread;
            private Handler mHandler;
            private Context mContext;
            private String mInputId;
            private int mMode;
            private DTVEpgScanner epgScanner;
            private TVChannelParams tvchan = null;
            private ChannelInfo tvservice = null;
            private TvDataBaseManager mTvDataBaseManager = null;
            private TVTime mTvTime = null;

            private ChannelObserver mChannelObserver;
            private ArrayList<ChannelInfo> channelMap;
            private long maxChannel_ID = 0;

            private int fend = 0;
            private int dmx = 0;
            private int src = 0;
            private String[] languages = null;

            private MonitorStoreManager mMonitorStoreManager;

            public DTVMonitor(Context context, String inputId, String coding, int mode) {
                mContext = context;
                mInputId = inputId;
                mMode = mode;
                mTvDataBaseManager = new TvDataBaseManager(mContext);
                mTvTime = new TVTime(mContext);

                int channel_number_start = mSystemControlManager.getPropertyInt(DTV_CHANNEL_NUMBER_START, 1);
                mMonitorStoreManager = new MonitorStoreManager(mInputId, channel_number_start);

                mHandlerThread = new HandlerThread(getClass().getSimpleName());
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == MSG_MONITOR_EVENT) {
                            resolveMonitorEvent((DTVEpgScanner.Event)msg.obj);
                        } else if (msg.what == MSG_MONITOR_RESCAN_SERVICE) {
                            rescanService(true);
                        } else if (msg.what == MSG_MONITOR_RESCAN_TIME) {
                            rescanTime(true);
                        }
                    }
                };

                epgScanner = new DTVEpgScanner(mode) {
                    public void onEvent(DTVEpgScanner.Event event) {
                        Log.d(TAG, "send event:" + event.type);
                        mHandler.obtainMessage(MSG_MONITOR_EVENT, event).sendToTarget();
                    }
                };
                epgScanner.setDvbTextCoding(coding);

                if ((mode & MODE_UPDATE_EPG) == MODE_UPDATE_EPG) {
                    if (mChannelObserver == null)
                        mChannelObserver = new ChannelObserver();
                    mContext.getContentResolver().registerContentObserver(TvContract.Channels.CONTENT_URI, true, mChannelObserver);
                }
            }

            private void refreshChannelMap() {
                channelMap = mTvDataBaseManager.getChannelList(mInputId, ChannelInfo.SIMPLE_PROJECTION, null, null);
                if (channelMap != null) {
                    for (ChannelInfo c : channelMap)
                        if (c.getId() > maxChannel_ID)
                            maxChannel_ID = c.getId();
                }
                Log.d(TAG, "channelMap changed. max_ID:" + maxChannel_ID);
            }

            public void reset(int fend, int dmx, int src, String textLanguages) {
                Log.d(TAG, "monitor reset all.");
                if ((mMode & MODE_UPDATE_EPG) == MODE_UPDATE_EPG)
                    refreshChannelMap();

                synchronized (this) {
                    if (epgScanner == null)
                        return;

                    epgScanner.setSource(fend, dmx, src, textLanguages);
                    languages = textLanguages.split(" ");

                    this.fend = fend;
                    this.dmx = dmx;
                    this.src = src;
                }
            }

            public void reset() {
                Log.d(TAG, "monitor reset.");

                if (epgScanner == null) {
                    Log.d(TAG, "monitor may exit, ignore.");
                    return;
                }

                reset(fend, dmx, src, EPG_LANGUAGE.replaceAll("local", TVMultilingualText.getLocalLang()));

                enterChannel(tvchan, true);
                enterService(tvservice);
            }

            public void destroy() {
                setEpgAutoReset(false);

                if (mChannelObserver != null) {
                    mContext.getContentResolver().unregisterContentObserver(mChannelObserver);
                    mChannelObserver = null;
                }

                if (mHandler != null) {/*take care of rescan befor epgScanner=null*/
                    mHandler.removeMessages(MSG_MONITOR_RESCAN_SERVICE);
                    mHandler.removeMessages(MSG_MONITOR_RESCAN_TIME);
                }
                synchronized (this) {
                    if (epgScanner != null) {
                        epgScanner.destroy();
                        epgScanner = null;
                    }
                    if (mHandler != null) {
                        mHandler.removeMessages(MSG_MONITOR_EVENT);
                        mHandler.removeMessages(MSG_MONITOR_RESCAN_SERVICE);
                        mHandler.removeMessages(MSG_MONITOR_RESCAN_TIME);
                        mHandler = null;
                    }

                if (mHandlerThread != null) {
                    mHandlerThread.quit();
                    mHandlerThread = null;
                }

                    mTvControlManager.setStorDBListener(null);
                    mTvControlManager.DtvStopScan();
                mTvDataBaseManager = null;
                mTvTime = null;
                }
            }

            public void enterChannel(TVChannelParams chan, boolean force) {
                synchronized (this) {
                    enterChannelLocked(chan, force);
                }
            }

            public void enterService(ChannelInfo channel) {
                synchronized (this) {
                    enterServiceLocked(channel);
                }
            }

            private void enterChannelLocked(TVChannelParams chan, boolean force) {
                    if (epgScanner == null)
                        return;
                    if (chan == null)
                        epgScanner.leaveChannel();
                    else if ((tvchan == null) || !tvchan.equals(chan) || force)
                        epgScanner.enterChannel();
                    tvchan = chan;
            }

            private void enterServiceLocked(ChannelInfo channel) {
                    if (epgScanner == null)
                        return;
                    if (channel == null) {
                        rescanTime(false);
                        rescanService(false);
                        epgScanner.leaveProgram();
                    } else {
                        epgScanner.enterProgram(channel);
                        rescanService(true);
                        rescanTime(true);
                    }
                    tvservice = channel;
            }

            public void rescanService(boolean on) {
                auto_rescan_service =
                    mSystemControlManager.getPropertyInt(DTV_AUTO_RESCAN_SERVICE, AUTO_RESCAN_CONTINUOUS);

                if (auto_rescan_service == AUTO_RESCAN_NONE)
                    return;

                Log.d(TAG, "rescanService:" + on);
                synchronized (this) {
                    if (epgScanner == null) {
                        Log.d(TAG, "monitor may exit, ignore.");
                        return;
                    }
                    if (mHandler != null) {
                        mHandler.removeMessages(MSG_MONITOR_RESCAN_SERVICE);
                        if (on && (auto_rescan_service == AUTO_RESCAN_CONTINUOUS)) {
                            Log.d(TAG, "rescanServiceLater");
                            mHandler.sendEmptyMessageDelayed(MSG_MONITOR_RESCAN_SERVICE, AUTO_RESCAN_SERVICE_INTERVAL);
                        }
                    }
                    if (on) {
                        if (tvservice != null) {
                            Log.d(TAG, "rescanService");
                            epgScanner.stopScan(DTVEpgScanner.SCAN_PAT|DTVEpgScanner.SCAN_PMT|DTVEpgScanner.SCAN_SDT);
                            epgScanner.startScan(DTVEpgScanner.SCAN_PAT|DTVEpgScanner.SCAN_PMT|DTVEpgScanner.SCAN_SDT);
                        }
                    }
                }
            }

            public void rescanTime(boolean on) {
                auto_rescan_time =
                    mSystemControlManager.getPropertyInt(DTV_AUTO_RESCAN_TIME, AUTO_RESCAN_CONTINUOUS);

                if (auto_rescan_time == AUTO_RESCAN_NONE)
                    return;

                Log.d(TAG, "rescanTime:" + on);
                synchronized (this) {
                    if (epgScanner == null) {
                        Log.d(TAG, "monitor may exit, ignore.");
                        return;
                    }
                    if (mHandler != null) {
                        mHandler.removeMessages(MSG_MONITOR_RESCAN_TIME);
                        if (on && (auto_rescan_time == AUTO_RESCAN_CONTINUOUS)) {
                            Log.d(TAG, "rescanTimeLater");
                            mHandler.sendEmptyMessageDelayed(MSG_MONITOR_RESCAN_TIME, AUTO_RESCAN_TIME_INTERVAL);
                        }
                    }
                    if (on)
                        epgScanner.rescanTDT();
                }
            }

            public void restartMonitorTime(){
                Log.d(TAG, "restartMonitorTime");
                synchronized (this) {
                    if (epgScanner == null) {
                        Log.d(TAG, "monitor may exit, ignore.");
                        return;
                    }
                    epgScanner.rescanTDT();
                }
            }

            private List<Program> getChannelPrograms(Uri channelUri, ChannelInfo channel,
                    DTVEpgScanner.Event event) {
                List<Program> programs = new ArrayList<>();
                for (DTVEpgScanner.Event.Evt evt : event.evts) {
                    if ((channel.getTransportStreamId() == evt.ts_id)
                        && (channel.getServiceId() == evt.srv_id)
                        && (channel.getOriginalNetworkId() == evt.net_id)) {
                        try {
                            long start = evt.start;
                            long end = evt.end;
                            Program p = new Program.Builder()
                            .setChannelId(ContentUris.parseId(channelUri))
                            .setTitle(TVMultilingualText.getText(new String(evt.name), languages))
                            .setDescription(TVMultilingualText.getText(new String(evt.desc), languages))
                            //.setContentRatings(programInfo.contentRatings)
                            //.setCanonicalGenres(programInfo.genres)
                            //.setPosterArtUri(programInfo.posterArtUri)
                            //.setInternalProviderData(TvContractUtils.convertVideoInfoToInternalProviderData(
                            //        programInfo.videoType, programInfo.videoUrl))
                            .setStartTimeUtcMillis(start * 1000)
                            .setEndTimeUtcMillis(end * 1000)
                            .build();
                            programs.add(p);
                            Log.d(TAG, "epg: sid/net/ts[" + evt.srv_id + "/" + evt.net_id + "/" + evt.ts_id + "]"
                                  + "{" + p.getTitle() + "}"
                                  + "[" + p.getStartTimeUtcMillis() / 1000
                                  + "-" + p.getEndTimeUtcMillis() / 1000
                                  + "]");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return programs;
            }

            private void resolveMonitorEvent(DTVEpgScanner.Event event) {
                Log.d(TAG, "Monitor event: " + event.type + " this:" +this);
                switch (event.type) {
                    case DTVEpgScanner.Event.EVENT_PROGRAM_EVENTS_UPDATE:
                        for (int i = 0; (channelMap != null && i < channelMap.size()); ++i) {
                            ChannelInfo channel = (ChannelInfo)channelMap.get(i);
                            Uri channelUri = TvContract.buildChannelUri(channel.getId());

                            List<Program> programs = getChannelPrograms(channelUri, channel, event);
                            if (mTvDataBaseManager != null)
                                mTvDataBaseManager.updatePrograms(channelUri, programs);
                        }
                        break;
                    case DTVEpgScanner.Event.EVENT_TDT_END:
                        Log.d(TAG, "[TDT Update]:" + event.time);
                        if (mTvTime != null)
                            mTvTime.setTime(event.time * 1000);
                        break;
                    case DTVEpgScanner.Event.EVENT_PROGRAM_AV_UPDATE: {
                        Log.d(TAG, "[AV Update]: ServiceId:" + event.channel.getServiceId()
                              + " Vid:" + event.channel.getVideoPid()
                              + " Pcr:" + event.channel.getPcrPid()
                              + " Aids:" + Arrays.toString(event.channel.getAudioPids())
                              + " Afmts:" + Arrays.toString(event.channel.getAudioFormats())
                              + " Alangs:" + Arrays.toString(event.channel.getAudioLangs())
                              + " Aexts:" + Arrays.toString(event.channel.getAudioExts())
                              + " Stypes:" + Arrays.toString(event.channel.getSubtitleTypes())
                              + " Sids:" + Arrays.toString(event.channel.getSubtitlePids())
                              + " Sstypes:" + Arrays.toString(event.channel.getSubtitleStypes())
                              + " Sid1s:" + Arrays.toString(event.channel.getSubtitleId1s())
                              + " Sid2s:" + Arrays.toString(event.channel.getSubtitleId2s())
                              + " Slangs:" + Arrays.toString(event.channel.getSubtitleLangs())
                             );
                        boolean updated = false;
                        synchronized (this) {
                            if (tvservice != null
                                && tvservice.getServiceId() == event.channel.getServiceId()) {
                                    tvservice.setVideoPid(event.channel.getVideoPid());
                                    tvservice.setVfmt(event.channel.getVfmt());
                                    tvservice.setPcrPid(event.channel.getPcrPid());
                                    tvservice.setAudioPids(event.channel.getAudioPids());
                                    tvservice.setAudioFormats(event.channel.getAudioFormats());
                                    tvservice.setAudioExts(event.channel.getAudioExts());
                                    tvservice.setAudioLangs(event.channel.getAudioLangs());
                                    tvservice.setSubtitlePids(event.channel.getSubtitlePids());
                                    tvservice.setSubtitleTypes(event.channel.getSubtitleTypes());
                                    tvservice.setSubtitleStypes(event.channel.getSubtitleStypes());
                                    tvservice.setSubtitleId1s(event.channel.getSubtitleId1s());
                                    tvservice.setSubtitleId2s(event.channel.getSubtitleId2s());
                                    tvservice.setSubtitleLangs(event.channel.getSubtitleLangs());
                                if (mTvDataBaseManager != null)
                                    mTvDataBaseManager.updateChannelInfo(tvservice);
                                updated = true;
                            }
                        }
                        if (updated) {
                            if (auto_retune_service) {
                                Log.d(TAG, "Retune Current Uri");
                                if (mCurrentUri != null)
                                    switchToSourceInput(mCurrentUri);
                            }
                        }
                    }
                    break;
                    case DTVEpgScanner.Event.EVENT_PROGRAM_NAME_UPDATE: {
                        Log.d(TAG, "[NAME Update]: current: ONID:"+event.channel.getOriginalNetworkId()
                            +" Version:"+event.channel.getSdtVersion());
                        Log.d(TAG, "\t[Service]: id:"+event.channel.getServiceId() + " name:"+event.channel.getDisplayName());
                        Log.d(TAG, "[NAME Update]: ONID:"+event.services.mNetworkId
                            +" TSID:"+event.services.mTSId
                            +" Version:"+event.services.mVersion);
                        for (DTVEpgScanner.Event.ServiceInfosFromSDT.ServiceInfoFromSDT s : event.services.mServices) {
                            Log.d(TAG, "\t[Service]: id:" + s.mId + " type:"+s.mType + " name:"+s.mName);
                            Log.d(TAG, "\t           running:"+s.mRunning + " freeCA:"+s.mFreeCA);
                        }
                        synchronized (this) {
                            if (mTvDataBaseManager == null)
                                break;
                            ArrayList<ChannelInfo> channelList =
                                mTvDataBaseManager.getChannelList(mInputId, ChannelInfo.COMMON_PROJECTION,
                                    TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID+"=? and "+
                                    TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID+"=?",
                                    new String[]{
                                        /*
                                         * If sdt timeout in scanning, onid should be -1.
                                         * Risk exsits if sdt timeout more then once in autoscan,
                                         * TSId+SId may not identify a service without ONId.
                                         * */
                                        "-1",
                                        /*String.valueOf(event.services.mNetworkId),*/
                                        String.valueOf(event.services.mTSId)
                                    });
                            int count = 0;
                            for (ChannelInfo co : channelList) {
                                for (DTVEpgScanner.Event.ServiceInfosFromSDT.ServiceInfoFromSDT sn : event.services.mServices) {
                                    if (co.getServiceId() == sn.mId) {
                                        co.setOriginalNetworkId(event.services.mNetworkId);
                                        co.setDisplayName(TVMultilingualText.getText(sn.mName));
                                        co.setDisplayNameMulti(sn.mName);
                                        co.setSdtVersion(event.services.mVersion);
                                        mTvDataBaseManager.updateChannelInfo(co);
                                        count = count + 1;
                                    }
                                }
                            }
                            Log.d(TAG, "found ["+event.services.mServices.size()+"] services in SDT.");
                            Log.d(TAG, "update ["+count+"] services in DB.");
                        }
                    }
                    break;
                    case DTVEpgScanner.Event.EVENT_CHANNEL_UPDATE:
                        Log.d(TAG, "[TS Update]: TS changed, need autoscan.");
                        synchronized (this) {
                            if (tvservice != null) {
                                Log.d(TAG, "[TS Update] Freq:" + tvservice.getFrequency());
                                int mode = new TvControlManager.TvMode(tvservice.getType()).getMode();
                                int frequency = tvservice.getFrequency();

                                enterServiceLocked(null);
                                enterChannelLocked(null, true);
                                stopSubtitle();
                                releasePlayer();

                                mTvControlManager.setStorDBListener(mMonitorStoreManager);
                                mTvControlManager.DtvManualScan(mode, frequency);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }


            private boolean epg_auto_reset = false;
            private void setEpgAutoReset(boolean enable) {
                epg_auto_reset = enable;
            }

            private final class ChannelObserver extends ContentObserver {
                public ChannelObserver() {
                    super(new Handler());
                }

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    Log.d(TAG, "channel changed: selfchange:" + selfChange + " uri:" + uri);
                    if ((DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL)//delete
                        || ((DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL_ID)
                            && (DroidLogicTvUtils.getChannelId(uri) > maxChannel_ID))) { //add
                        if (DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL) {
                            Log.d(TAG, "channel deleted");
                        } else if ((DroidLogicTvUtils.matchsWhich(uri) == DroidLogicTvUtils.MATCH_CHANNEL_ID)
                                   && (DroidLogicTvUtils.getChannelId(uri) > maxChannel_ID)) {
                            Log.d(TAG, "channel added: " + DroidLogicTvUtils.getChannelId(uri) + ">" + maxChannel_ID);
                        }
                        if (epg_auto_reset)
                            reset();
                    }
                }

                @Override
                public IContentObserver releaseContentObserver() {
                    // TODO Auto-generated method stub
                    return super.releaseContentObserver();
                }
            }

            private final class MonitorStoreManager extends TvStoreManager implements TvControlManager.StorDBEventListener {
                public MonitorStoreManager(String inputId, int initialDisplayNumber) {
                    super(mContext, inputId, initialDisplayNumber);
                }
                public void onScanEnd() {
                    mTvControlManager.DtvStopScan();
                }

                @Override
                public void StorDBonEvent(TvControlManager.ScannerEvent ev) {
                    onStoreEvent(ev);
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
        acquireHardware(info);
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
        releaseHardware();
        Log.d(TAG, "=====onHardwareRemoved===== " + id);
        return id;
    }


}
