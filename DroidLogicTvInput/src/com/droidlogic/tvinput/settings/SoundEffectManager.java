/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.os.SystemClock;
import android.media.AudioManager;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.widget.Toast;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.media.audiofx.AudioEffect;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.*;
import com.droidlogic.app.AudioOutputManager;
import com.droidlogic.app.tv.AudioEffectManager;

public class SoundEffectManager {

    public static final String TAG = "SoundEffectManager";

    private static final UUID EFFECT_TYPE_TRUSURROUND = UUID.fromString("1424f5a0-2457-11e6-9fe0-0002a5d5c51b");
    private static final UUID EFFECT_TYPE_BALANCE = UUID.fromString("7cb34dc0-242e-11e6-bb63-0002a5d5c51b");
    private static final UUID EFFECT_TYPE_TREBLE_BASS = UUID.fromString("7e282240-242e-11e6-bb63-0002a5d5c51b");
    private static final UUID EFFECT_TYPE_DAP = UUID.fromString("3337b21d-c8e6-4bbd-8f24-698ade8491b9");
    private static final UUID EFFECT_TYPE_EQ = UUID.fromString("ce2c14af-84df-4c36-acf5-87e428ed05fc");
    private static final UUID EFFECT_TYPE_AGC = UUID.fromString("4a959f5c-e33a-4df2-8c3f-3066f9275edf");
    private static final UUID EFFECT_TYPE_VIRTUAL_SURROUND = UUID.fromString("c656ec6f-d6be-4e7f-854b-1218077f3915");
    //SoundMode mode.  Parameter ID
    public static final int PARAM_SOUND_MODE            = 0;
    public static final int PARAM_DIALOG_CLARITY        = 1;
    public static final int PARAM_SURROUND              = 2;
    public static final int PARAM_TRUVOLUME             = 3;
    //Balance level.  Parameter ID
    public static final int PARAM_BALANCE_LEVEL         = 0;
    //Tone level.  Parameter ID for
    public static final int PARAM_BASS_LEVEL            = 0;
    public static final int PARAM_TREBLE_LEVEL          = 1;
    public static final int PARAM_BAND_ENABLE           = 2;
    public static final int PARAM_BAND1                 = 3;
    public static final int PARAM_BAND2                 = 4;
    public static final int PARAM_BAND3                 = 5;
    public static final int PARAM_BAND4                 = 6;
    public static final int PARAM_BAND5                 = 7;
    public static final int PARAM_BAND_COUNT            = 8;
    //dap AudioEffect
    public static final int PARAM_EQ_ENABLE             = 0;
    public static final int PARAM_EQ_EFFECT             = 1;
    public static final int PARAM_EQ_CUSTOM             = 2;
    //agc effect define
    public static final int PARAM_AGC_ENABLE            = 0;
    public static final int PARAM_AGC_MAX_LEVEL         = 1;
    public static final int PARAM_AGC_ATTRACK_TIME      = 4;
    public static final int PARAM_AGC_RELEASE_TIME      = 5;
    public static final int PARAM_AGC_SOURCE_ID         = 6;

    public static final boolean DEFAULT_AGC_ENABLE      = true; //enable 1, disable 0
    public static final int DEFAULT_AGC_MAX_LEVEL       = -18;  //db
    public static final int DEFAULT_AGC_ATTRACK_TIME    = 10;   //ms
    public static final int DEFAULT_AGC_RELEASE_TIME    = 2;    //s
    public static final int DEFAULT_AGC_SOURCE_ID       = 3;
    //virtual surround
    public static final int PARAM_VIRTUALSURROUND       = 0;
    /* Modes of dialog clarity */
    public static final int DIALOG_CLARITY_OFF          = 0;
    public static final int DIALOG_CLARITY_LOW          = 1;
    public static final int DIALOG_CLARITY_HIGH         = 2;
    //definition off and on
    private static final int PARAMETERS_SWITCH_OFF      = 1;
    private static final int PARAMETERS_SWITCH_ON       = 0;

    private static final int UI_SWITCH_OFF              = 0;
    private static final int UI_SWITCH_ON               = 1;

    private static final int PARAMETERS_DAP_ENABLE      = 1;
    private static final int PARAMETERS_DAP_DISABLE     = 0;
    //amlogic 5 bands effect parameters
    private static final int EFFECT_SOUND_BANDS_NUM     = 5;
    //band 1, band 2, band 3, band 4, band 5  need transfer 0~100 to -10~10
    private static final int[][] EFFECT_SOUND_BAND = {  {3, 0, 0, 0, 3},        //standard
                                                        {8, 5, -3, 5, 6},       //music
                                                        {12, -6, 7, 12, 10},    //news
                                                        {6, -2, -2, 6, -3},     //theater
                                                        {8, -8, 12, -1, -4},    //game
                                                        {50, 50, 50, 50, 50}};  //user

    private static final int EFFECT_SOUND_TYPE_NUM = 6;
    //all sound mode, order is bass treble balance dialogclarity surround bassboost
    private static final int[][] EFFECT_SOUND_MODE = {  {50, 50, 50, 1, 1, 0},  //standard parameters
                                                        {74, 74, 50, 1, 0, 0},  //music parameters
                                                        {50, 26, 50, 2, 1, 0},  //movie parameters
                                                        {34, 34, 50, 2, 0, 0},  //news parameters
                                                        {34, 34, 50, 2, 0, 0},  //game parameters
                                                        {74, 50, 50, 1, 1, 0}}; //user parameters

    private int mSoundModule = AudioEffectManager.DAP_MODULE;
    // Prefix to append to audio preferences file
    private Context mContext;
    private AudioManager mAudioManager;

    //sound effects
    private AudioEffect mTruSurround;
    private AudioEffect mBalance;
    private AudioEffect mTrebleBass;
    private AudioEffect mSoundMode;
    private AudioEffect mAgc;
    private AudioEffect mVirtualSurround;

    public SoundEffectManager (Context context) {
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(context.AUDIO_SERVICE);
        creatAudioEffects();
    }

    static public boolean CanDebug() {
        return true;
    }

    private void creatAudioEffects() {
        if (CanDebug()) Log.d(TAG, "Create Audio Effects");
        creatTruSurroundAudioEffects();
        creatBalanceAudioEffects();
        creatTrebleBassAudioEffects();
        creatSoundModeAudioEffects();
        creatVirtualSurroundAudioEffects();
    }

    private boolean creatTruSurroundAudioEffects() {
        try {
            if (mTruSurround == null) {
                if (CanDebug()) Log.d(TAG, "creatTruSurroundAudioEffects");
                mTruSurround = new AudioEffect(EFFECT_TYPE_TRUSURROUND, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create mTruSurround audio effect", e);
            return false;
        }
    }

    private boolean creatBalanceAudioEffects() {
        try {
            if (mBalance == null) {
                if (CanDebug()) Log.d(TAG, "creatBalanceAudioEffects");
                mBalance = new AudioEffect(EFFECT_TYPE_BALANCE, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create mBalance audio effect", e);
            return false;
        }
    }

    private boolean creatTrebleBassAudioEffects() {
        try {
            if (mTrebleBass == null) {
                if (CanDebug()) Log.d(TAG, "creatTrebleBassAudioEffects");
                mTrebleBass = new AudioEffect(EFFECT_TYPE_TREBLE_BASS, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create mTrebleBass audio effect", e);
            return false;
        }
    }

    private boolean creatSoundModeAudioEffects() {
        //first try dap
        if (creatDapAudioEffects()) {
            return true;
        } else {
            return creatEqAudioEffects();
        }
    }

    private boolean creatEqAudioEffects() {
        try {
            if (mSoundMode == null) {
                if (CanDebug()) Log.d(TAG, "creatEqAudioEffects");
                mSoundMode = new AudioEffect(EFFECT_TYPE_EQ, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
                int result = mSoundMode.setEnabled(true);
                if (result == AudioEffect.SUCCESS) {
                    if (CanDebug()) Log.d(TAG, "creatEqAudioEffects enable eq");
                    mSoundMode.setParameter(PARAM_EQ_ENABLE, PARAMETERS_DAP_ENABLE);
                    mSoundModule = AudioEffectManager.EQ_MODULE;
                    Settings.Global.putString(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE, AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE_EQ);
                }
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create Eq audio effect", e);
            return false;
        }
    }

    private boolean creatDapAudioEffects() {
        try {
            if (mSoundMode == null) {
                if (CanDebug()) Log.d(TAG, "creatDapAudioEffects");
                mSoundMode = new AudioEffect(EFFECT_TYPE_DAP, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
                int result = mSoundMode.setEnabled(true);
                if (result == AudioEffect.SUCCESS) {
                    if (CanDebug()) Log.d(TAG, "creatDapAudioEffects enable dap");
                    mSoundMode.setParameter(PARAM_EQ_ENABLE, PARAMETERS_DAP_ENABLE);
                    mSoundModule = AudioEffectManager.DAP_MODULE;
                    Settings.Global.putString(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE, AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE_DAP);
                }
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create Dap audio effect", e);
            return false;
        }
    }

    private boolean creatAgcAudioEffects() {
        try {
            if (mAgc == null) {
                if (CanDebug()) Log.d(TAG, "creatAgcAudioEffects");
                mAgc = new AudioEffect(EFFECT_TYPE_AGC, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create Agc audio effect", e);
            return false;
        }
    }

    private boolean creatVirtualSurroundAudioEffects() {
        try {
            if (mVirtualSurround == null) {
                if (CanDebug()) Log.d(TAG, "creatVirtualSurroundAudioEffects");
                mVirtualSurround = new AudioEffect(EFFECT_TYPE_VIRTUAL_SURROUND, AudioEffect.EFFECT_TYPE_NULL, 0, 0);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create VirtualSurround audio effect", e);
            return false;
        }
    }

    public int getSoundModeStatus () {
        Log.e(TAG, "yhc 2AudioEffectManager yhc getSoundModule");
        int saveresult = -1;
        if (!creatSoundModeAudioEffects()) {
            Log.e(TAG, "getSoundModeStatus creat fail");
            return AudioEffectManager.MODE_STANDARD;
        }
        int[] value = new int[1];
        mSoundMode.getParameter(PARAM_EQ_EFFECT, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_SOUND_MODE);
        if (saveresult != value[0]) {
            Log.e(TAG, "getSoundModeStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getSoundModeStatus = " + saveresult);
        }
        return saveresult;
    }

    //return current is eq or dap
    public int getSoundModule() {
        return mSoundModule;
    }

    public int getTrebleStatus () {
        int saveresult = -1;
        if (!creatTrebleBassAudioEffects()) {
            Log.e(TAG, "getTrebleStatus mTrebleBass creat fail");
            return EFFECT_SOUND_MODE[AudioEffectManager.MODE_STANDARD][AudioEffectManager.SET_TREBLE];
        }
        int[] value = new int[1];
        mTrebleBass.getParameter(PARAM_TREBLE_LEVEL, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_TREBLE);
        if (saveresult != value[0]) {
            Log.e(TAG, "getTrebleStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getTrebleStatus = " + saveresult);
        }
        return saveresult;
    }

    public int getBassStatus () {
        int saveresult = -1;
        if (!creatTrebleBassAudioEffects()) {
            Log.e(TAG, "getBassStatus mTrebleBass creat fail");
            return EFFECT_SOUND_MODE[AudioEffectManager.MODE_STANDARD][AudioEffectManager.SET_BASS];
        }
        int[] value = new int[1];
        mTrebleBass.getParameter(PARAM_BASS_LEVEL, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_BASS);
        if (saveresult != value[0]) {
            Log.e(TAG, "getBassStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getBassStatus = " + saveresult);
        }
        return saveresult;
    }

    public int getBalanceStatus () {
        int saveresult = -1;
        if (!creatBalanceAudioEffects()) {
            Log.e(TAG, "getBalanceStatus mBalance creat fail");
            return EFFECT_SOUND_MODE[AudioEffectManager.MODE_STANDARD][AudioEffectManager.SET_BALANCE];
        }
        int[] value = new int[1];
        mBalance.getParameter(PARAM_BALANCE_LEVEL, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_BALANCE);
        if (saveresult != value[0]) {
            Log.e(TAG, "getBalanceStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getBalanceStatus = " + saveresult);
        }
        return saveresult;
    }

    // 0 1 ~ off on
    public int getSurroundStatus () {
        int saveresult = -1;
        int getresult = -1;
        if (!creatTruSurroundAudioEffects()) {
            Log.e(TAG, "getSurroundStatus mTruSurround creat fail");
            return UI_SWITCH_OFF;
        }
        int[] value = new int[1];
        mTruSurround.getParameter(PARAM_SURROUND, value);
        getresult = (value[0] == PARAMETERS_SWITCH_ON ? UI_SWITCH_ON : UI_SWITCH_OFF);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_SURROUND);
        if (saveresult != getresult) {
            Log.e(TAG, "getSurroundStatus erro get: " + getresult + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getSurroundStatus = " + saveresult);
        }
        return saveresult;
    }

    // 0 1 ~ off on
    public int getDialogClarityStatus () {
        int saveresult = -1;
        if (!creatTruSurroundAudioEffects()) {
            Log.e(TAG, "getDialogClarityStatus mBalance creat fail");
            return UI_SWITCH_OFF;
        }
        int[] value = new int[1];
        mTruSurround.getParameter(PARAM_DIALOG_CLARITY, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_DIALOG_CLARITY);
        if (saveresult != value[0]) {
            Log.e(TAG, "getDialogClarityStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getDialogClarityStatus = " + saveresult);
        }
        return saveresult;
    }

    public int getBassBoostStatus () {
        int saveresult = -1;
        int getresult = -1;
        if (!creatTruSurroundAudioEffects()) {
            Log.e(TAG, "getBassBoostStatus mAndroidBassBoost creat fail");
            return UI_SWITCH_OFF;
        }
        int[] value = new int[1];
        mTruSurround.getParameter(PARAM_TRUVOLUME, value);
        getresult = (value[0] == PARAMETERS_SWITCH_ON ? UI_SWITCH_ON : UI_SWITCH_OFF);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_BASS_BOOST);
        if (saveresult != getresult) {
            Log.e(TAG, "getSurroundStatus erro get: " + getresult + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getBassBoostStatus = " + saveresult);
        }
        return saveresult;
    }

    public boolean getAgcEnableStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcEnableStatus mAgc creat fail");
            return DEFAULT_AGC_ENABLE;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_ENABLE, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_ENABLE);
        if (saveresult != value[0]) {
            Log.e(TAG, "getAgcEnableStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcEnableStatus = " + saveresult);
        }
        return saveresult == 1;
    }

    public int getAgcMaxLevelStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcEnableStatus mAgc creat fail");
            return DEFAULT_AGC_MAX_LEVEL;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_MAX_LEVEL, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_MAX_LEVEL);
        if (saveresult != value[0]) {
            Log.e(TAG, "getAgcMaxLevelStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcMaxLevelStatus = " + saveresult);
        }
        return value[0];
    }

    public int getAgcAttrackTimeStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcAttrackTimeStatus mAgc creat fail");
            return DEFAULT_AGC_ATTRACK_TIME;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_ATTRACK_TIME, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_ATTRACK_TIME);
        if (saveresult != value[0] / 48) {
            Log.e(TAG, "getAgcAttrackTimeStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcAttrackTimeStatus = " + saveresult);
        }
        //value may be changed realtime
        return value[0] / 48;
    }

    public int getAgcReleaseTimeStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcReleaseTimeStatus mAgc creat fail");
            return DEFAULT_AGC_RELEASE_TIME;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_RELEASE_TIME, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_RELEASE_TIME);
        if (saveresult != value[0]) {
            Log.e(TAG, "getAgcReleaseTimeStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcReleaseTimeStatus = " + saveresult);
        }
        //value may be changed realtime
        return value[0];
    }

    public int getAgcSourceIdStatus () {
        int saveresult = -1;
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "getAgcSourceIdStatus mAgc creat fail");
            return DEFAULT_AGC_RELEASE_TIME;
        }
        int[] value = new int[1];
        mAgc.getParameter(PARAM_AGC_SOURCE_ID, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_AGC_SOURCE_ID);
        if (saveresult != value[0]) {
            Log.e(TAG, "getAgcSourceIdStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getAgcSourceIdStatus = " + saveresult);
        }
        //value may be changed realtime
        return value[0];
    }

    // 0 1 ~ off on
    public int getVirtualSurroundStatus() {
        int saveresult = -1;
        if (!creatVirtualSurroundAudioEffects()) {
            Log.e(TAG, "getVirtualSurroundStatus mVirtualSurround creat fail");
            return AudioOutputManager.VIRTUAL_SURROUND_OFF;
        }
        int[] value = new int[1];
        mVirtualSurround.getParameter(PARAM_VIRTUALSURROUND, value);
        saveresult = getSavedAudioParameters(AudioEffectManager.SET_VIRTUAL_URROUND);
        if (saveresult != value[0]) {
            Log.e(TAG, "getVirtualSurroundStatus erro get: " + value[0] + ", saved: " + saveresult);
        } else if (CanDebug()) {
            Log.d(TAG, "getVirtualSurroundStatus = " + saveresult);
        }
        return saveresult;
    }

    //set sound mode except customed one
    public void setSoundMode (int mode) {
        //need to set sound mode by observer listener
        saveAudioParameters(AudioEffectManager.SET_SOUND_MODE, mode);
    }

    public void setSoundModeByObserver (int mode) {
        if (!creatSoundModeAudioEffects()) {
            Log.e(TAG, "setSoundMode creat fail");
            return;
        }
        int result = mSoundMode.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setSoundMode = " + mode);
            if ((mSoundModule == AudioEffectManager.DAP_MODULE && mode == AudioEffectManager.MODE_CUSTOM) ||
                    (mSoundModule == AudioEffectManager.EQ_MODULE && mode == AudioEffectManager.EXTEND_MODE_CUSTOM)) {
                //for (int i = AudioEffectManager.SET_EFFECT_BAND1; i <= AudioEffectManager.SET_EFFECT_BAND5; i++) {
                    //set one band, at the same time the others will be set
                    setDifferentBandEffects(AudioEffectManager.SET_EFFECT_BAND1, getSavedAudioParameters(AudioEffectManager.SET_EFFECT_BAND1), false);
                //}
            } else {
                mSoundMode.setParameter(PARAM_EQ_EFFECT, mode);
            }
            //need to set sound mode by observer listener
            //saveAudioParameters(AudioEffectManager.SET_SOUND_MODE, mode);
        }
    }

    public void setDifferentBandEffects(int bandnum, int value, boolean needsave) {
        if (!creatSoundModeAudioEffects()) {
            Log.e(TAG, "setDifferentBandEffects creat fail");
            return;
        }
        int result = mSoundMode.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setDifferentBandEffects: NO." + bandnum + " = " + value);
            byte[] fiveband = new byte[5];
            for (int i = AudioEffectManager.SET_EFFECT_BAND1; i <= AudioEffectManager.SET_EFFECT_BAND5; i++) {
                if (bandnum == i) {
                    fiveband[i - AudioEffectManager.SET_EFFECT_BAND1] = (byte)MappingLine(value, true);
                    continue;
                }
                fiveband[i - AudioEffectManager.SET_EFFECT_BAND1] = (byte)MappingLine(getParameters(i), true);
            }
            mSoundMode.setParameter(PARAM_EQ_CUSTOM, fiveband);
            if (needsave) {
                saveAudioParameters(bandnum, value);
            }
        }
    }

    //convert 0~100 to -10~10 controled by need or not
    private int MappingLine(int mapval, boolean need) {
        if (!need) {
            return mapval;
        }
        final int MIN_UI_VAL = 0;
        final int MAX_UI_VAL = 100;
        final int MIN_VAL = -10;
        final int MAX_VAL = 10;
        if (MIN_VAL < 0) {
            return (mapval - (MAX_UI_VAL + MIN_UI_VAL) / 2) * (MAX_VAL - MIN_VAL)
                   / (MAX_UI_VAL - MIN_UI_VAL);
        } else {
            return (mapval - MIN_UI_VAL) * (MAX_VAL - MIN_VAL) / (MAX_UI_VAL - MIN_UI_VAL);
        }
    }

    public void setTreble (int step) {
        if (!creatTrebleBassAudioEffects()) {
            Log.e(TAG, "setTreble mTrebleBass creat fail");
            return;
        }
        int result = mTrebleBass.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setTreble = " + step);
            mTrebleBass.setParameter(PARAM_TREBLE_LEVEL, step);
            saveAudioParameters(AudioEffectManager.SET_TREBLE, step);
        }
    }

    public void setBass (int step) {
        if (!creatTrebleBassAudioEffects()) {
            Log.e(TAG, "setBass mTrebleBass creat fail");
            return;
        }
        int result = mTrebleBass.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setBass = " + step);
            mTrebleBass.setParameter(PARAM_BASS_LEVEL, step);
            saveAudioParameters(AudioEffectManager.SET_BASS, step);
        }
    }

    public void setBalance (int step) {
        if (!creatBalanceAudioEffects()) {
            Log.e(TAG, "setBalance mBalance creat fail");
            return;
        }
        int result = mBalance.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setBalance = " + step);
            mBalance.setParameter(PARAM_BALANCE_LEVEL, step);
            saveAudioParameters(AudioEffectManager.SET_BALANCE, step);
        }
    }

    public void setSurround (int mode) {
        if (!creatTruSurroundAudioEffects()) {
            Log.e(TAG, "setSurround mTruSurround creat fail");
            return;
        }
        int result = mTruSurround.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setSurround = " + mode);
            mTruSurround.setParameter(PARAM_SURROUND, mode == UI_SWITCH_ON ? PARAMETERS_SWITCH_ON : PARAMETERS_SWITCH_OFF);
            saveAudioParameters(AudioEffectManager.SET_SURROUND, mode);
        }
    }

    public void setDialogClarity (int mode) {
        if (!creatTruSurroundAudioEffects()) {
            Log.e(TAG, "setDialogClarity mTruSurround creat fail");
            return;
        }
        int result = mTruSurround.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setDialogClarity = " + mode);
            mTruSurround.setParameter(PARAM_DIALOG_CLARITY, mode);
            saveAudioParameters(AudioEffectManager.SET_DIALOG_CLARITY, mode);
        }
    }

    public void setBassBoost (int mode) {
        if (!creatTruSurroundAudioEffects()) {
            Log.e(TAG, "setBassBoost mTruSurround creat fail");
            return;
        }
        int result = mTruSurround.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setBassBoost = " + mode);
            mTruSurround.setParameter(PARAM_DIALOG_CLARITY, mode == UI_SWITCH_ON ? PARAMETERS_SWITCH_ON : PARAMETERS_SWITCH_OFF);
            saveAudioParameters(AudioEffectManager.SET_BASS_BOOST, mode);
        }
    }

    public void setAgsEnable (int mode) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setAgsEnable mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setAgsEnable = " + mode);
            mAgc.setParameter(PARAM_AGC_ENABLE, mode);
            saveAudioParameters(AudioEffectManager.SET_AGC_ENABLE, mode);
        }
    }

    public void setAgsMaxLevel (int step) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setAgsMaxLevel mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setAgsMaxLevel = " + step);
            mAgc.setParameter(PARAM_AGC_MAX_LEVEL, step);
            saveAudioParameters(AudioEffectManager.SET_AGC_MAX_LEVEL, step);
        }
    }

    public void setAgsAttrackTime (int step) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setAgsAttrackTime mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setAgsAttrackTime = " + step);
            mAgc.setParameter(PARAM_AGC_ATTRACK_TIME, step * 48);
            saveAudioParameters(AudioEffectManager.SET_AGC_ATTRACK_TIME, step);
        }
    }

    public void setAgsReleaseTime (int step) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setAgsReleaseTime mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setAgsReleaseTime = " + step);
            mAgc.setParameter(PARAM_AGC_RELEASE_TIME, step);
            saveAudioParameters(AudioEffectManager.SET_AGC_RELEASE_TIME, step);
        }
    }

    public void setSourceIdForAvl (int step) {
        if (!creatAgcAudioEffects()) {
            Log.e(TAG, "setSourceIdForAvl mAgc creat fail");
            return;
        }
        int result = mAgc.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setSourceIdForAvl = " + step);
            mAgc.setParameter(PARAM_AGC_SOURCE_ID, step);
            saveAudioParameters(AudioEffectManager.SET_AGC_SOURCE_ID, step);
        }
    }

    public void setVirtualSurround (int mode) {
        if (!creatVirtualSurroundAudioEffects()) {
            Log.e(TAG, "setVirtualSurround mVirtualSurround creat fail");
            return;
        }
        int result = mVirtualSurround.setEnabled(true);
        if (result == AudioEffect.SUCCESS) {
            if (CanDebug()) Log.d(TAG, "setVirtualSurround = " + mode);
            mVirtualSurround.setParameter(PARAM_VIRTUALSURROUND, mode);
            saveAudioParameters(AudioEffectManager.SET_VIRTUAL_URROUND, mode);
        }
    }

    public void setParameters(int order, int value) {
        switch (order) {
            case AudioEffectManager.SET_BASS:
                setBass(value);
                break;
            case AudioEffectManager.SET_TREBLE:
                setTreble(value);
                break;
            case AudioEffectManager.SET_BALANCE:
                setBalance(value);
                break;
            case AudioEffectManager.SET_DIALOG_CLARITY:
                setDialogClarity(value);
                break;
            case AudioEffectManager.SET_SURROUND:
                setSurround(value);
                break;
            case AudioEffectManager.SET_BASS_BOOST:
                setBassBoost(value);
                break;
            case AudioEffectManager.SET_SOUND_MODE:
                setSoundMode(value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND1:
            case AudioEffectManager.SET_EFFECT_BAND2:
            case AudioEffectManager.SET_EFFECT_BAND3:
            case AudioEffectManager.SET_EFFECT_BAND4:
            case AudioEffectManager.SET_EFFECT_BAND5:
                setDifferentBandEffects(order, value, true);
                break;
            case AudioEffectManager.SET_AGC_ENABLE:
                setAgsEnable(value);
                break;
            case AudioEffectManager.SET_AGC_MAX_LEVEL:
                setAgsMaxLevel(value);
                break;
            case AudioEffectManager.SET_AGC_ATTRACK_TIME:
                setAgsAttrackTime(value);
                break;
            case AudioEffectManager.SET_AGC_RELEASE_TIME:
                setAgsReleaseTime(value);
                break;
            case AudioEffectManager.SET_AGC_SOURCE_ID:
                setSourceIdForAvl(value);
                break;
            case AudioEffectManager.SET_VIRTUAL_URROUND:
                setVirtualSurround(value);
                break;
            default:
                break;
        }
    }

    public int getParameters(int order) {
        int value = -1;
        if (order < AudioEffectManager.SET_BASS || order > AudioEffectManager.SET_VIRTUAL_URROUND) {
            Log.e(TAG, "getParameters order erro");
            return value;
        }
        value = getSavedAudioParameters(order);
        return value;
    }

    private void saveAudioParameters(int id, int value) {
        switch (id) {
            case AudioEffectManager.SET_BASS:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BASS, value);
                break;
            case AudioEffectManager.SET_TREBLE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_TREBLE, value);
                break;
            case AudioEffectManager.SET_BALANCE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BALANCE, value);
                break;
            case AudioEffectManager.SET_DIALOG_CLARITY:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_DIALOG_CLARITY, value);
                break;
            case AudioEffectManager.SET_SURROUND:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SURROUND, value);
                break;
            case AudioEffectManager.SET_BASS_BOOST:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BASS_BOOST, value);
                break;
            case AudioEffectManager.SET_SOUND_MODE:
                String soundmodetype = Settings.Global.getString(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE);
                if (soundmodetype == null || AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE_EQ.equals(soundmodetype)) {
                    Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_EQ_VALUE, value);
                } else if ((AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE_DAP.equals(soundmodetype))) {
                    Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_DAP_VALUE, value);
                } else {
                    Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE, value);
                }
                break;
            case AudioEffectManager.SET_EFFECT_BAND1:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND1, value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND2:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND2, value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND3:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND3, value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND4:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND4, value);
                break;
            case AudioEffectManager.SET_EFFECT_BAND5:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND5, value);
                break;
            case AudioEffectManager.SET_AGC_ENABLE:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_ENABLE, value);
                break;
            case AudioEffectManager.SET_AGC_MAX_LEVEL:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_MAX_LEVEL, value);
                break;
            case AudioEffectManager.SET_AGC_ATTRACK_TIME:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_ATTRACK_TIME, value);
                break;
            case AudioEffectManager.SET_AGC_RELEASE_TIME:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_RELEASE_TIME, value);
                break;
            case AudioEffectManager.SET_AGC_SOURCE_ID:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_SOURCE_ID, value);
                break;
            case AudioEffectManager.SET_VIRTUAL_URROUND:
                Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.VIRTUAL_SURROUND, value);
                break;
            default:
                break;
        }
    }

    private int getSavedAudioParameters(int id) {
        int result = -1;
        switch (id) {
            case AudioEffectManager.SET_BASS:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BASS, EFFECT_SOUND_MODE[AudioEffectManager.MODE_STANDARD][AudioEffectManager.SET_BASS]);
                break;
            case AudioEffectManager.SET_TREBLE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_TREBLE, EFFECT_SOUND_MODE[AudioEffectManager.MODE_STANDARD][AudioEffectManager.SET_TREBLE]);
                break;
            case AudioEffectManager.SET_BALANCE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BALANCE, EFFECT_SOUND_MODE[AudioEffectManager.MODE_STANDARD][AudioEffectManager.SET_BALANCE]);
                break;
            case AudioEffectManager.SET_DIALOG_CLARITY:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_DIALOG_CLARITY, DIALOG_CLARITY_OFF);
                break;
            case AudioEffectManager.SET_SURROUND:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SURROUND, UI_SWITCH_OFF);
                break;
            case AudioEffectManager.SET_BASS_BOOST:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BASS_BOOST, UI_SWITCH_OFF);
                break;
            case AudioEffectManager.SET_SOUND_MODE:
                String soundmodetype = Settings.Global.getString(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE);
                if (soundmodetype == null || AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE_EQ.equals(soundmodetype)) {
                    result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_EQ_VALUE, AudioEffectManager.MODE_STANDARD);
                } else if ((AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE_DAP.equals(soundmodetype))) {
                    result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_DAP_VALUE, AudioEffectManager.MODE_STANDARD);
                } else {
                    result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE, AudioEffectManager.MODE_STANDARD);
                }
                Log.d(TAG, "getSavedAudioParameters SET_SOUND_MODE = " + result);
                break;
            case AudioEffectManager.SET_EFFECT_BAND1:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND1, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND1 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_EFFECT_BAND2:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND2, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND2 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_EFFECT_BAND3:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND3, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND3 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_EFFECT_BAND4:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND4, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND4 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_EFFECT_BAND5:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND5, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND5 - PARAM_BAND1]);
                break;
            case AudioEffectManager.SET_AGC_ENABLE:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_ENABLE, DEFAULT_AGC_ENABLE ? 1 : 0);
                break;
            case AudioEffectManager.SET_AGC_MAX_LEVEL:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_MAX_LEVEL, DEFAULT_AGC_MAX_LEVEL);
                break;
            case AudioEffectManager.SET_AGC_ATTRACK_TIME:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_ATTRACK_TIME, DEFAULT_AGC_ATTRACK_TIME);
                break;
            case AudioEffectManager.SET_AGC_RELEASE_TIME:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_RELEASE_TIME, DEFAULT_AGC_RELEASE_TIME);
                break;
            case AudioEffectManager.SET_AGC_SOURCE_ID:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_SOURCE_ID, DEFAULT_AGC_SOURCE_ID);
                break;
            case AudioEffectManager.SET_VIRTUAL_URROUND:
                result = Settings.Global.getInt(mContext.getContentResolver(), AudioOutputManager.VIRTUAL_SURROUND, AudioOutputManager.VIRTUAL_SURROUND_OFF);
                break;
            default:
                break;
        }
        return result;
    }

    public void cleanupAudioEffects() {
        if (mBalance!= null) {
            mBalance.setEnabled(false);
            mBalance.release();
            mBalance = null;
        }
        if (mTruSurround!= null) {
            mTruSurround.setEnabled(false);
            mTruSurround.release();
            mTruSurround = null;
        }
        if (mTrebleBass!= null) {
            mTrebleBass.setEnabled(false);
            mTrebleBass.release();
            mTrebleBass = null;
        }
        if (mSoundMode!= null) {
            mSoundMode.setEnabled(false);
            mSoundMode.release();
            mSoundMode = null;
        }
        if (mAgc!= null) {
            mAgc.setEnabled(false);
            mAgc.release();
            mAgc = null;
        }
        if (mVirtualSurround != null) {
            mVirtualSurround.setEnabled(false);
            mVirtualSurround.release();
            mVirtualSurround = null;
        }
    }

    public void initSoundEffectSettings() {
        if (Settings.Global.getInt(mContext.getContentResolver(), "set_five_band", 0) == 0) {
            for (int i = AudioEffectManager.SET_EFFECT_BAND1; i <= AudioEffectManager.SET_EFFECT_BAND5; i++) {
                saveAudioParameters(i, EFFECT_SOUND_BAND[AudioEffectManager.MODE_CUSTOM][i - AudioEffectManager.SET_EFFECT_BAND1]);
            }
            Settings.Global.putInt(mContext.getContentResolver(), "set_five_band", 1);
        }
        for (int i = 0; i < AudioEffectManager.SET_EFFECT_BAND1; i++) {
            int value = getSavedAudioParameters(i);
            setParameters(i, value);
            Log.d(TAG, "initSoundEffectSettings NO." + i + "=" + value);
        }
        for (int i = AudioEffectManager.SET_AGC_ENABLE; i < AudioEffectManager.SET_VIRTUAL_URROUND + 1; i++) {
            int value = getSavedAudioParameters(i);
            setParameters(i, value);
            Log.d(TAG, "initSoundEffectSettings NO." + i + "=" + value);
        }
        //init sound parameter at the same time
        /*SoundParameterSettingManager soundparameter = new SoundParameterSettingManager(mContext);
        if (soundparameter != null) {
            soundparameter.initParameterAfterBoot();
        }
        soundparameter = null;*/
        applyAudioEffectByPlayEmptyTrack();
    }

    public void resetSoundEffectSettings() {
        Log.d(TAG, "resetSoundEffectSettings");
        cleanupAudioEffects();
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BASS, EFFECT_SOUND_MODE[AudioEffectManager.MODE_STANDARD][AudioEffectManager.SET_BASS]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_TREBLE, EFFECT_SOUND_MODE[AudioEffectManager.MODE_STANDARD][AudioEffectManager.SET_TREBLE]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BALANCE, EFFECT_SOUND_MODE[AudioEffectManager.MODE_STANDARD][AudioEffectManager.SET_BALANCE]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_DIALOG_CLARITY, DIALOG_CLARITY_OFF);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SURROUND, UI_SWITCH_OFF);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BASS_BOOST, UI_SWITCH_OFF);
        Settings.Global.putString(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE, AudioOutputManager.SOUND_EFFECT_SOUND_MODE_TYPE_EQ);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE, AudioEffectManager.MODE_STANDARD);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_DAP_VALUE, AudioEffectManager.MODE_STANDARD);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_SOUND_MODE_EQ_VALUE, AudioEffectManager.MODE_STANDARD);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND1, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND1 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND2, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND2 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND3, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND3 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND4, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND4 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_BAND5, EFFECT_SOUND_BAND[AudioEffectManager.MODE_STANDARD][PARAM_BAND5 - PARAM_BAND1]);
        Settings.Global.putInt(mContext.getContentResolver(), "set_five_band", 0);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_ENABLE, DEFAULT_AGC_ENABLE ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_MAX_LEVEL, DEFAULT_AGC_MAX_LEVEL);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_ATTRACK_TIME, DEFAULT_AGC_ATTRACK_TIME);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_RELEASE_TIME, DEFAULT_AGC_RELEASE_TIME);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.SOUND_EFFECT_AGC_SOURCE_ID, DEFAULT_AGC_SOURCE_ID);
        Settings.Global.putInt(mContext.getContentResolver(), AudioOutputManager.VIRTUAL_SURROUND, AudioOutputManager.VIRTUAL_SURROUND_OFF);
        initSoundEffectSettings();
    }

    private void applyAudioEffectByPlayEmptyTrack() {
        int bufsize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        byte data[] = new byte[bufsize];
        AudioTrack trackplayer = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufsize, AudioTrack.MODE_STREAM);
        trackplayer.play();
        trackplayer.write(data, 0, data.length);
        trackplayer.stop();
        trackplayer.release();
    }
}

