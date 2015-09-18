package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.droidlogic.tvinput.R;

public class SettingsManager {
    public static final String TAG = "SettingsManager";

    public static final String KEY_CONTENT_TITLE                            = "content_title";

    public static final String KEY_PICTURE                          = "Picture";
    public static final String KEY_PICTURE_MODE                     = "picture_mode";
    public static final String KEY_BRIGHTNESS                       = "brightness";
    public static final String KEY_CONTRAST                         = "contrast";
    public static final String KEY_COLOR                            = "color";
    public static final String KEY_SHARPNESS                        = "sharpness";
    public static final String KEY_BACKLIGHT                        = "backlight";
    public static final String KEY_COLOR_TEMPERATURE                = "color_temperature";
    public static final String KEY_ASPECT_RATIO                     = "aspect_ratio";
    public static final String KEY_DNR                              = "dnr";
    public static final String KEY_3D_SETTINGS                      = "settings_3d";

    public static final String KEY_SOUND                            = "Sound";
    public static final String KEY_SOUND_MODE                       = "sound_mode";
    public static final String KEY_TREBLE                           = "treble";
    public static final String KEY_BASS                             = "bass";
    public static final String KEY_BALANCE                          = "balance";
    public static final String KEY_SPDIF                            = "spdif";
    public static final String KEY_SURROUND                         = "surround";
    public static final String KEY_DIALOG_CLARITY                   = "dialog_clarity";
    public static final String KEY_BASS_BOOST                       = "bass_boost";

    public static final String KEY_CHANNEL                          = "Channel";
    public static final String KEY_CURRENT_CHANNEL                  = "current_channel";
    public static final String KEY_FREQUNCY                         = "frequency";
    public static final String KEY_COLOR_SYSTEM                     = "color_system";
    public static final String KEY_SOUND_SYSTEM                     = "sound_system";
    public static final String KEY_VOLUME_COMPENSATE                = "volume_compensate";
    public static final String KEY_FINE_TUNE                        = "fine_tune";
    public static final String KEY_MANUAL_SEARCH                    = "manual_search";
    public static final String KEY_AUTO_SEARCH                      = "auto_search";
    public static final String KEY_CHANNEL_EDIT                     = "channel_edit";
    public static final String KEY_SWITCH_CHANNEL                   = "switch_channel";

    public static final String KEY_SETTINGS                         = "Settings";
    public static final String KEY_SLEEP_TIMER                      = "sleep_timer";
    public static final String KEY_MENU_TIME                        = "menu_time";
    public static final String KEY_STARTUP_SETTING                  = "startup_setting";
    public static final String KEY_DYNAMIC_BACKLIGHT                = "dynamic_backlight";
    public static final String KEY_RESTORE_FACTORY                  = "restore_factory";

    public static final String STATUS_STANDARD                      = "standard";
    public static final String STATUS_VIVID                         = "vivid";
    public static final String STATUS_SOFT                          = "soft";
    public static final String STATUS_USER                          = "user";
    public static final String STATUS_WARM                          = "warm";
    public static final String STATUS_COOL                          = "cool";
    public static final String STATUS_ON                            = "on";
    public static final String STATUS_OFF                           = "off";
    public static final String STATUS_AUTO                          = "auto";
    public static final String STATUS_4_TO_3                        = "4:3";
    public static final String STATUS_PANORAMA                      = "panorama";
    public static final String STATUS_FULL_SCREEN                   = "full_screen";
    public static final String STATUS_MEDIUM                        = "medium";
    public static final String STATUS_HIGH                          = "high";
    public static final String STATUS_LOW                           = "low";
    public static final String STATUS_3D_LR_MODE                    = "left right mode";
    public static final String STATUS_3D_RL_MODE                    = "right left mode";
    public static final String STATUS_3D_UD_MODE                    = "up down mode";
    public static final String STATUS_3D_DU_MODE                    = "down up mode";
    public static final String STATUS_3D_TO_2D                      = "3D to 2D";

    public static final String STATUS_DEFAULT_PERCENT               = "50%";
    public static final int PERCENT_INCREASE                        = 1;
    public static final int PERCENT_DECREASE                        = -1;

    public static String currentTag = null;

    private Context mContext;
    private Resources mResources;

    public SettingsManager (Context context) {
        mContext = context;
        mResources = mContext.getResources();
    }

    public void setTag(String tag) {
        currentTag = tag;
    }

    public String getStatus(String key) {
        Log.d(TAG, " current screen is :" + currentTag + ", item is :" + key);
        if (currentTag.equals(KEY_PICTURE)) {
            return getPictureStatus(key);
        } else if (currentTag.equals(KEY_SOUND)) {
            return getSoundStatus(key);
        } else if (currentTag.equals(KEY_CHANNEL)) {
            return getChannelStatus(key);
        } else if (currentTag.equals(KEY_SETTINGS)) {
            return getSettingsStatus(key);
        }

        return null;
    }

    //picture
    private String getPictureStatus(String key) {
        if (key.equals(KEY_PICTURE_MODE)) {
            return getPictureModeStatus();
        } else if (key.equals(KEY_BRIGHTNESS)) {
            return getBrightnessStatus();
        } else if (key.equals(KEY_CONTRAST)) {
            return getContrastStatus();
        } else if (key.equals(KEY_COLOR)) {
            return getColorStatus();
        } else if (key.equals(KEY_SHARPNESS)) {
            return getSharpnessStatus();
        } else if (key.equals(KEY_BACKLIGHT)) {
            return getBacklightStatus();
        } else if (key.equals(KEY_COLOR_TEMPERATURE)) {
            return getColorTemperatureStatus();
        } else if (key.equals(KEY_ASPECT_RATIO)) {
            return getAspectRatioStatus();
        } else if (key.equals(KEY_DNR)) {
            return getDnrStatus();
        } else if (key.equals(KEY_3D_SETTINGS)) {
            return get3dSettingsStatus();
        }
        return null;
    }

    private String getPictureModeStatus () {
        return STATUS_STANDARD;
    }

    private String getBrightnessStatus () {
        return STATUS_DEFAULT_PERCENT;
    }

    private String getContrastStatus () {
        return STATUS_DEFAULT_PERCENT;
    }

    private String getColorStatus () {
        return STATUS_DEFAULT_PERCENT;
    }

    private String getSharpnessStatus () {
        return STATUS_DEFAULT_PERCENT;
    }

    private String getBacklightStatus () {
        return STATUS_DEFAULT_PERCENT;
    }

    private String getColorTemperatureStatus () {
        return mResources.getString(R.string.standard);
    }

    private String getAspectRatioStatus () {
        return mResources.getString(R.string.standard);
    }

    private String getDnrStatus () {
        return mResources.getString(R.string.auto);
    }

    private String get3dSettingsStatus () {
        return mResources.getString(R.string.off);
    }

    //Sound
    private String getSoundStatus (String key) {
        if (key.equals(KEY_SOUND_MODE)) {
            return getSoundModeStatus();
        } else if (key.equals(KEY_TREBLE)) {
            return getTrebleStatus();
        } else if (key.equals(KEY_BASS)) {
            return getBassStatus();
        } else if (key.equals(KEY_BALANCE)) {
            return getBalanceStatus();
        } else if (key.equals(KEY_SPDIF)) {
            return getSpdifStatus();
        } else if (key.equals(KEY_SURROUND)) {
            return getSurroundStatus();
        } else if (key.equals(KEY_DIALOG_CLARITY)) {
            return getDialogClarityStatus();
        } else if (key.equals(KEY_BASS_BOOST)) {
            return getBassBoostStatus();
        }
        return null;
    }


    private String getSoundModeStatus () {
        return mResources.getString(R.string.standard);
    }

    private String getTrebleStatus () {
        return STATUS_DEFAULT_PERCENT;
    }

    private String getBassStatus () {
        return STATUS_DEFAULT_PERCENT;
    }

    private String getBalanceStatus () {
        return STATUS_DEFAULT_PERCENT;
    }

    private String getSpdifStatus () {
        return mResources.getString(R.string.auto);
    }

    private String getSurroundStatus () {
        return mResources.getString(R.string.off);
    }

    private String getDialogClarityStatus () {
        return mResources.getString(R.string.off);
    }

    private String getBassBoostStatus () {
        return mResources.getString(R.string.off);
    }

    //Channel
    private String getChannelStatus (String key) {
        if (key.equals(KEY_CURRENT_CHANNEL)) {
            return getCurrentChannelStatus();
        } else if (key.equals(KEY_FREQUNCY)) {
            return getFrequencyStatus();
        } else if (key.equals(KEY_COLOR_SYSTEM)) {
            return getColorSystemStatus();
        } else if (key.equals(KEY_SOUND_SYSTEM)) {
            return getSoundSystemStatus();
        } else if (key.equals(KEY_VOLUME_COMPENSATE)) {
            return getVolumeCompensateStatus();
        } else if (key.equals(KEY_SWITCH_CHANNEL)) {
            return getSwitchChannelStatus();
        }
        return null;
    }

    private String getCurrentChannelStatus () {
        return null;
    }

    private String getFrequencyStatus () {
        return null;
    }

    private String getColorSystemStatus () {
        return mResources.getString(R.string.auto);
    }

    private String getSoundSystemStatus () {
        return mResources.getString(R.string.sound_system_dk);
    }

    private String getVolumeCompensateStatus () {
        return "0";
    }

    private String getSwitchChannelStatus () {
        return mResources.getString(R.string.static_frame);
    }

    //Settings
    private String getSettingsStatus (String key) {
        if (key.equals(KEY_SLEEP_TIMER)) {
            return getSleepTimerStatus();
        } else if (key.equals(KEY_MENU_TIME)) {
            return getMenuTimeStatus();
        } else if (key.equals(KEY_STARTUP_SETTING)) {
            return getStartupSettingStatus();
        } else if (key.equals(KEY_DYNAMIC_BACKLIGHT)) {
            return getDynamicBacklightStatus();
        }
        return null;
    }

    private String getSleepTimerStatus () {
        return mResources.getString(R.string.off);
    }

    private String getMenuTimeStatus () {
        return mResources.getString(R.string.time_10s);
    }

    private String getStartupSettingStatus () {
        return mResources.getString(R.string.launcher);
    }

    private String getDynamicBacklightStatus () {
        return mResources.getString(R.string.off);
    }

    public void setPictureMode (String mode) {

    }

    public void setBrightness (int step) {

    }

    public void setContrast (int step) {

    }

    public void setColor (int step) {

    }

    public void setSharpness (int step) {

    }

    public void setBacklight (int step) {

    }

    public void setColorTemperature(String mode) {

    }

    public void setAspectRatio(String mode) {

    }

    public void setDnr (String mode) {

    }

    public void set3dSettings (String mode) {

    }

}
