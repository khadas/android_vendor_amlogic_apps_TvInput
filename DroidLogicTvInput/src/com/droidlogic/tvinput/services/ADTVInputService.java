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

public class ADTVInputService extends DTVInputService {

    private static final String TAG = "ADTVInputService";

    private ADTVSessionImpl mCurrentSession;

    private Map<Integer, ADTVSessionImpl> sessionMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Session onCreateSession(String inputId) {
        registerInput(inputId);
        mCurrentSession = new ADTVSessionImpl(this, inputId, getHardwareDeviceId(inputId));
        registerInputSession(mCurrentSession);
        mCurrentSession.setSessionId(id);
        sessionMap.put(id, mCurrentSession);
        id++;

        return mCurrentSession;
    }

    @Override
    public void tvPlayStopped(int sessionId) {
        ADTVSessionImpl session = sessionMap.get(sessionId);
        if (session != null) {
            session.stopSubtitle();
            session.setMonitor(null);
        }
    }

    @Override
    public void setCurrentSessionById(int sessionId) {
        Utils.logd(TAG, "setCurrentSessionById:"+sessionId);
        ADTVSessionImpl session = sessionMap.get(sessionId);
        if (session != null) {
            mCurrentSession = session;
        }
    }

    @Override
    public void doTuneFinish(int result, Uri uri, int sessionId) {
        Log.d(TAG, "doTuneFinish,result:"+result+"sessionId:"+sessionId);
        if (result == ACTION_SUCCESS) {
            ADTVSessionImpl session = sessionMap.get(sessionId);
            if (session != null)
                session.switchToSourceInput(uri);
        }
    }

    public class ADTVSessionImpl extends DTVInputService.DTVSessionImpl implements TvControlManager.AVPlaybackListener {
        private TvContentRating mLastBlockedRating;
        private TvContentRating mCurrentContentRating;
        private final Set<TvContentRating> mUnblockedRatingSet = new HashSet<>();

        protected ADTVSessionImpl(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);
        }

        @Override
        public void doAppPrivateCmd(String action, Bundle bundle) {
           if (DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN.equals(action)
                || DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN.equals(action)) {
                Log.d(TAG, "do private cmd: DTV_XXX_SCAN");
                //TODO let scanner know adtv here?
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

        @Override
        protected boolean playProgram(ChannelInfo info) {
            info.print();

            if (info.isAnalogChannnel()) {
                mTvControlManager.PlayATVProgram(info.getFrequency() + info.getFineTune(),
                    info.getVideoStd(),
                    info.getAudioStd(),
                    0,
                    info.getAudioCompensation());
                checkContentBlockNeeded();
                return true;
            }

            TvControlManager.FEParas fe = new TvControlManager.FEParas(info.getFEParas());
            int audioTrackAuto = getAudioTrackAuto(info);
            int mixingLevel = mAudioADMixingLevel;
            if (mixingLevel < 0)
                mixingLevel = Settings.System.getInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_AD_MIX, AD_MIXING_LEVEL_DEF);

            mTvControlManager.PlayDTVProgram(
                    fe,
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

        @Override
        public void onEvent(int msgType, int programID) {
            Log.d(TAG, "AV evt:" + msgType);
            super.onEvent(msgType, programID);
        }

    }

    @Override
    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getDeviceId() != DroidLogicTvUtils.DEVICE_ID_ADTV)
            return null;

        Log.d(TAG, "=====onHardwareAdded=====" + hardwareInfo.getDeviceId());

        TvInputInfo info = null;
        ResolveInfo rInfo = getResolveInfo(ADTVInputService.class.getName());
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

    @Override
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
