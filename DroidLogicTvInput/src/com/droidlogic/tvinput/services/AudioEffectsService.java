/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC AudioEffectsSettingManagerService
 */

package com.droidlogic.tvinput.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.Binder;
import android.os.Handler;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.database.ContentObserver;
import android.provider.Settings;
import android.net.Uri;
import android.app.Service;
import com.droidlogic.tvinput.settings.SoundEffectManager;

/**
 * This Service modifies Audio and Picture Quality TV Settings.
 * It contains platform specific implementation of the TvTweak IOemSettings interface.
 */
public class AudioEffectsService extends PersistentService {
    private static final String TAG = AudioEffectsService.class.getSimpleName();
    private static boolean DEBUG = true;
    private SoundEffectManager mSoundEffectManager;
    private AudioEffectsService mAudioEffectsService;
    private Context mContext = null;

    // Service actions
    public static final String ACTION_STARTUP = "com.droidlogic.tvinput.services.AudioEffectsService.STARTUP";

    public static final String SOUND_EFFECT_SOUND_MODE            = "sound_effect_sound_mode";
    public static final String SOUND_EFFECT_SOUND_MODE_TYPE       = "sound_effect_sound_mode_type";
    public static final String SOUND_EFFECT_SOUND_MODE_TYPE_DAP   = "type_dap";
    public static final String SOUND_EFFECT_SOUND_MODE_TYPE_EQ    = "type_eq";
    public static final String SOUND_EFFECT_SOUND_MODE_DAP_VALUE  = "sound_effect_sound_mode_dap";
    public static final String SOUND_EFFECT_SOUND_MODE_EQ_VALUE   = "sound_effect_sound_mode_eq";
    public static final int MODE_STANDARD = 0;

    public AudioEffectsService() {
        super("AudioEffectsService");
        mAudioEffectsService = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "AudioEffectsService onCreate");
        mContext = this;
        mSoundEffectManager = new SoundEffectManager(this);
        registerCommandReceiver(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "onDestroy");
        if (mSoundEffectManager != null) {
            mSoundEffectManager.cleanupAudioEffects();
        }
        unregisterCommandReceiver(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DEBUG) Log.w(TAG, "onLowMemory");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        final String action = intent.getAction();
        if (ACTION_STARTUP.equals(action)) {
            if (DEBUG) Log.d(TAG, "processing " + ACTION_STARTUP);
            handleActionStartUp();
        } else {
            Log.w(TAG, "Unknown intent: " + action);
        }
    }

    private final IAudioEffectsService.Stub mBinder = new IAudioEffectsService.Stub(){
        public int getSoundModeStatus () {
            return mSoundEffectManager.getSoundModeStatus();
        }

        //return current is eq or dap
        public int getSoundModule() {
            return mSoundEffectManager.getSoundModule();
        }

        public int getTrebleStatus () {
            return mSoundEffectManager.getTrebleStatus();
        }

        public int getBassStatus () {
            return mSoundEffectManager.getBassStatus();
        }

        public int getBalanceStatus () {
            return mSoundEffectManager.getBalanceStatus();
        }

        public int getSurroundStatus () {
            return mSoundEffectManager.getSurroundStatus();
        }

        public int getDialogClarityStatus () {
            return mSoundEffectManager.getDialogClarityStatus();
        }

        public int getBassBoostStatus () {
            return mSoundEffectManager.getBassBoostStatus();
        }

        public boolean getAgcEnableStatus () {
            return mSoundEffectManager.getAgcEnableStatus();
        }

        public int getAgcMaxLevelStatus () {
            return mSoundEffectManager.getAgcMaxLevelStatus();
        }

        public int getAgcAttrackTimeStatus () {
            return mSoundEffectManager.getAgcAttrackTimeStatus();
        }

        public int getAgcReleaseTimeStatus () {
            return mSoundEffectManager.getAgcReleaseTimeStatus();
        }

        public int getAgcSourceIdStatus () {
            return mSoundEffectManager.getAgcSourceIdStatus();
        }

        public int getVirtualSurroundStatus() {
            return mSoundEffectManager.getVirtualSurroundStatus();
        }

        public void setSoundMode (int mode) {
            mSoundEffectManager.setSoundMode(mode);
        }

        public void setSoundModeByObserver (int mode) {
            mSoundEffectManager.setSoundModeByObserver(mode);
        }

        public void setDifferentBandEffects(int bandnum, int value, boolean needsave) {
            mSoundEffectManager.setDifferentBandEffects(bandnum, value, needsave);
        }

        public void setTreble (int step) {
            mSoundEffectManager.setTreble (step);
        }

        public void setBass (int step) {
            mSoundEffectManager.setBass (step);
        }

        public void setBalance (int step) {
            mSoundEffectManager.setBalance (step);
        }

        public void setSurround (int mode) {
            mSoundEffectManager.setSurround (mode);
        }

        public void setDialogClarity (int mode) {
            mSoundEffectManager.setDialogClarity (mode);
        }

        public void setBassBoost (int mode) {
            mSoundEffectManager.setBassBoost (mode);
        }

        public void setAgsEnable (int mode) {
            mSoundEffectManager.setAgsEnable (mode);
        }

        public void setAgsMaxLevel (int step) {
            mSoundEffectManager.setAgsMaxLevel (step);
        }

        public void setAgsAttrackTime (int step) {
            mSoundEffectManager.setAgsAttrackTime (step);
        }

        public void setAgsReleaseTime (int step) {
            mSoundEffectManager.setAgsReleaseTime (step);
        }

        public void setSourceIdForAvl (int step) {
            mSoundEffectManager.setSourceIdForAvl (step);
        }

        public void setVirtualSurround (int mode) {
            mSoundEffectManager.setVirtualSurround (mode);
        }

        public void setParameters(int order, int value) {
            mSoundEffectManager.setParameters(order, value);
        }

        public int getParameters(int order) {
            return mSoundEffectManager.getParameters(order);
        }

        public void cleanupAudioEffects() {
            mSoundEffectManager.cleanupAudioEffects();
        }

        public void initSoundEffectSettings() {
            mSoundEffectManager.initSoundEffectSettings();
        }

        public void resetSoundEffectSettings() {
            Log.d(TAG, "resetSoundEffectSettings");
            mSoundEffectManager.resetSoundEffectSettings();
        }
    };

    private void handleActionStartUp() {
        // This will apply the saved audio settings on boot
        mSoundEffectManager.initSoundEffectSettings();
    }

    private static final String RESET_ACTION = "droid.action.resetsoundeffect";
    private static final String AVL_SOURCE_ACTION = "droid.action.avlmodule";

    private void registerCommandReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RESET_ACTION);
        intentFilter.addAction(AVL_SOURCE_ACTION);
        context.registerReceiver(mSoundEffectSettingsReceiver, intentFilter);
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE), false,
                mSoundEffectParametersObserver);
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE_EQ_VALUE), false,
                mSoundEffectParametersObserver);
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE_DAP_VALUE), false,
                mSoundEffectParametersObserver);
    }

    private void unregisterCommandReceiver(Context context) {
        context.unregisterReceiver(mSoundEffectSettingsReceiver);
        context.getContentResolver().unregisterContentObserver(mSoundEffectParametersObserver);
    }

    private ContentObserver mSoundEffectParametersObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mSoundEffectManager != null && uri != null) {
                if (uri.equals(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE)) || uri.equals(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE_EQ_VALUE))
                        || uri.equals(Settings.Global.getUriFor(SOUND_EFFECT_SOUND_MODE_DAP_VALUE))) {
                    int mode = Settings.Global.getInt(mContext.getContentResolver(), uri.getLastPathSegment(), MODE_STANDARD);
                    Log.d(TAG, "onChange setSoundMode " + uri.getLastPathSegment() + ":" + mode);
                    mSoundEffectManager.setSoundModeByObserver(mode);
                }
            }
        }
    };

    private final BroadcastReceiver mSoundEffectSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "intent = " + intent);
            if (intent != null) {
                if (RESET_ACTION.equals(intent.getAction())) {
                    mSoundEffectManager.resetSoundEffectSettings();
                } else if (AVL_SOURCE_ACTION.equals(intent.getAction())) {
                    mSoundEffectManager.setSourceIdForAvl(intent.getIntExtra("source_id", SoundEffectManager.DEFAULT_AGC_SOURCE_ID));
                }
            }
        }
    };
}
