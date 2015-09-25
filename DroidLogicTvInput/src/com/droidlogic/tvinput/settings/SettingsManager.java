package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;


import android.R.integer;
import android.amlogic.Tv;

import com.droidlogic.tvclient.TvClient;
import com.droidlogic.tvinput.R;

public class SettingsManager {
    public static final String TAG = "SettingsManager";

    private static TvClient client = TvClient.getTvClient();
    private Tv tv = TvClient.getTvInstance();

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

    public void setTag (String tag) {
        currentTag = tag;
    }

    public String getTag () {
        return currentTag;
    }

    public String getStatus (String key) {
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
    private String getPictureStatus (String key) {
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
        int pictureModeIndex = tv.GetPQMode(client.curSource);
        if (pictureModeIndex == 0)
            return STATUS_STANDARD;
        else if (pictureModeIndex == 1)
            return STATUS_VIVID;
        else if (pictureModeIndex == 2)
            return STATUS_SOFT;
        else
            return STATUS_USER;
    }

    private String getBrightnessStatus () {
        return tv.GetBrightness(client.curSource) + "%";
    }

    private String getContrastStatus () {
        return tv.GetContrast(client.curSource) + "%";
    }

    private String getColorStatus () {
        return tv.GetSaturation(client.curSource) + "%";
    }

    private String getSharpnessStatus () {
        return tv.GetSharpness(client.curSource) + "%";
    }

    private String getBacklightStatus () {
        return tv.GetBacklight(client.curSource) + "%";
    }

    private String getColorTemperatureStatus () {
        int itemPosition = tv.GetColorTemperature(client.curSource);
        if (itemPosition == 0)
            return mResources.getString(R.string.standard);
        else if (itemPosition == 1)
            return mResources.getString(R.string.warm);
        else
            return mResources.getString(R.string.cool);
    }

    private String getAspectRatioStatus () {
        int itemPosition = tv.GetDisplayMode(client.curSource);
        if (itemPosition == 0)
            return mResources.getString(R.string.full_screen);
        else if (itemPosition == 1)
            return mResources.getString(R.string.four2three);
        else if (itemPosition == 2)
            return mResources.getString(R.string.panorama);
        else
            return mResources.getString(R.string.auto);
    }

    private String getDnrStatus () {
        int itemPosition = tv.GetNoiseReductionMode(client.curSource);
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else if (itemPosition == 1)
            return mResources.getString(R.string.low);
        else if (itemPosition == 2)
            return mResources.getString(R.string.medium);
        else if (itemPosition == 3)
            return mResources.getString(R.string.high);
        else
            return mResources.getString(R.string.auto);
    }

    private String get3dSettingsStatus () {
        if (tv.Get3DTo2DMode() != 0)
            return mResources.getString(R.string.mode_3d_to_2d);
        int threeD_mode = tv.Get3DMode();
        if (threeD_mode == Tv.Mode_3D.MODE_3D_CLOSE.toInt()) {
            return mResources.getString(R.string.off);
        } else if (threeD_mode == Tv.Mode_3D.MODE_3D_AUTO.toInt()) {
            return mResources.getString(R.string.auto);
        } else if (threeD_mode == Tv.Mode_3D.MODE_3D_LEFT_RIGHT.toInt()) {
            if (tv.Get3DLRSwith() == 0)
                return mResources.getString(R.string.mode_lr);
            else
                return mResources.getString(R.string.mode_rl);
        } else if (threeD_mode == Tv.Mode_3D.MODE_3D_UP_DOWN.toInt()) {
            if (tv.Get3DLRSwith() == 0)
                return mResources.getString(R.string.mode_ud);
            else
                return mResources.getString(R.string.mode_du);
        } else {
            return null;
        }
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
        int itemPosition = tv.GetCurAudioSoundMode();
        if (itemPosition == 0)
            return mResources.getString(R.string.standard);
        else if (itemPosition == 1)
            return mResources.getString(R.string.music);
        else if (itemPosition == 2)
            return mResources.getString(R.string.news);
        else if (itemPosition == 3)
            return mResources.getString(R.string.movie);
        else if (itemPosition == 4)
            return mResources.getString(R.string.user);
        else
            return null;
    }

    private String getTrebleStatus () {
        return tv.GetCurAudioTrebleVolume() + "%";
    }

    private String getBassStatus () {
        return tv.GetCurAudioBassVolume() + "%";
    }

    private String getBalanceStatus () {
        return tv.GetCurAudioBalance() + "%";
    }

    private String getSpdifStatus () {
        if (tv.GetCurAudioSPDIFSwitch() == 0)
            return mResources.getString(R.string.off);
        int itemPosition = tv.GetCurAudioSPDIFMode();
        if (itemPosition == 0)
            return mResources.getString(R.string.auto);
        else if (itemPosition == 1)
            return mResources.getString(R.string.pcm);
        else
            return null;
    }

    private String getSurroundStatus () {
        int itemPosition = tv.GetCurAudioSrsSurround();
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else
            return mResources.getString(R.string.on);
    }

    private String getDialogClarityStatus () {
        int itemPosition = tv.GetCurAudioSrsDialogClarity();
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else
            return mResources.getString(R.string.on);
    }

    private String getBassBoostStatus () {
        int itemPosition = tv.GetCurAudioSrsTruBass();
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else
            return mResources.getString(R.string.on);
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
        return "0%";
    }

    public int getFineTuneProgress () {
        return 50;
    }

    public int getManualSearchProgress () {
        return 0;
    }

    public int getManualSearchSearchedNumber () {
        return 0;
    }

    public int getAutoSearchProgress () {
        return 0;
    }

    public int getAutoSearchSearchedNumber () {
        return 0;
    }

    public ArrayList<HashMap<String,Object>> geChannelEditList () {
        ArrayList<HashMap<String,Object>> list =  new ArrayList<HashMap<String,Object>>();

        HashMap<String,Object> item = new HashMap<String,Object>();
        item.put("channel_name", "channel 1");
        list.add(item);

        item = new HashMap<String,Object>();
        item.put("channel_name", "channel 2");
        list.add(item);

        return list;
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
        String ret = "";
        int time = SystemProperties.getInt("tv.sleep_timer", 0);
        switch (time)
        {
            case 0://off
                ret = mResources.getString(R.string.off);
                break;
            case 900000://15min
                ret = mResources.getString(R.string.time_15min);
                break;
            case 1800000://30min
                ret = mResources.getString(R.string.time_30min);
                break;
            case 2700000://45min
                ret = mResources.getString(R.string.time_45min);
                break;
            case 3600000://60min
                ret = mResources.getString(R.string.time_60min);
                break;
            case 5400000://90min
                ret = mResources.getString(R.string.time_90min);
                break;
            case 7200000://120min
                ret = mResources.getString(R.string.time_120min);
                break;
            default:
                ret = mResources.getString(R.string.off);
                break;
        }
        return ret;
    }

    private String getMenuTimeStatus () {
        return mResources.getString(R.string.time_10s);
    }

    private String getStartupSettingStatus () {
        return mResources.getString(R.string.launcher);
    }

    private String getDynamicBacklightStatus () {
        int itemPosition = tv.isAutoBackLighting();
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else
            return mResources.getString(R.string.on);
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

    public void setChannelName (int channer_number, String targetName) {

    }

    public  void swapChannelPosition (int channer_number, int targetNumber) {

    }

    public  void moveChannelPosition (int channer_number, int targetNumber) {

    }

    public  void skipChannel (int channelNumber) {

    }

    public  void deleteChannel (int channelNumber) {

    }

    public  void setFavouriteChannel (int channelNumber) {

    }
}
