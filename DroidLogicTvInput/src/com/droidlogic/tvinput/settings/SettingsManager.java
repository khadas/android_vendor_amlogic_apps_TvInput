package com.droidlogic.tvinput.settings;

import android.app.ActivityManager;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.IPackageDataObserver;
import android.content.res.Resources;
import android.provider.Settings;
import android.os.SystemProperties;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import android.R.integer;
import android.amlogic.Tv;

import android.media.tv.TvInputInfo;
import com.droidlogic.app.SystemControlManager;
import android.media.tv.TvContract.Channels;
import com.droidlogic.utils.tunerinput.data.ChannelInfo;
import com.droidlogic.utils.tunerinput.tvutil.TvContractUtils;
import com.droidlogic.tvclient.TvClient;
import com.droidlogic.tvinput.R;

public class SettingsManager {
    public static final String TAG = "SettingsManager";

    private TvClient client;
    private Tv tv;

    public static final String KEY_PICTURE                          = "picture";
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

    public static final String KEY_SOUND                            = "sound";
    public static final String KEY_SOUND_MODE                       = "sound_mode";
    public static final String KEY_TREBLE                           = "treble";
    public static final String KEY_BASS                             = "bass";
    public static final String KEY_BALANCE                          = "balance";
    public static final String KEY_SPDIF                            = "spdif";
    public static final String KEY_SURROUND                         = "surround";
    public static final String KEY_DIALOG_CLARITY                   = "dialog_clarity";
    public static final String KEY_BASS_BOOST                       = "bass_boost";

    public static final String KEY_CHANNEL                          = "channel";
    public static final String KEY_CURRENT_CHANNEL                  = "current_channel";
    public static final String KEY_FREQUNCY                         = "frequency";
    public static final String KEY_AUIDO_TRACK                         = "audio_track";
    public static final String KEY_SOUND_CHANNEL                        = "sound_channel";
    public static final String KEY_CHANNEL_INFO                         = "channel_info";
    public static final String KEY_COLOR_SYSTEM                     = "color_system";
    public static final String KEY_SOUND_SYSTEM                     = "sound_system";
    public static final String KEY_VOLUME_COMPENSATE                = "volume_compensate";
    public static final String KEY_FINE_TUNE                        = "fine_tune";
    public static final String KEY_MANUAL_SEARCH                    = "manual_search";
    public static final String KEY_AUTO_SEARCH                      = "auto_search";
    public static final String KEY_CHANNEL_EDIT                     = "channel_edit";
    public static final String KEY_SWITCH_CHANNEL                   = "switch_channel";

    public static final String KEY_SETTINGS                         = "settings";
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
    public static final String STATUS_MUSIC                          = "music";
    public static final String STATUS_NEWS                          = "news";
    public static final String STATUS_MOVIE                          = "movie";
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
    public static final double STATUS_DEFAUT_FREQUENCY              = 44250000;
    public static final int PERCENT_INCREASE                        = 1;
    public static final int PERCENT_DECREASE                        = -1;
    public static final int DEFAULT_SLEEP_TIMER                     = 0;
    public static final int DEFUALT_MENU_TIME                       = 10;
    public static final String LAUNCHER_NAME                         = "com.android.launcher";
    public static final String LAUNCHER_ACTIVITY                    = "com.android.launcher2.Launcher";
    public static final String TV_NAME                                = "com.droidlogic.tv";
    public static final String TV_ACTIVITY                           = "com.droidlogic.tv.DroidLogicTv";

    public static final String STRING_ICON               = "icon";
    public static final String STRING_NAME               = "name";
    public static final String STRING_STATUS              = "status";
    public static String currentTag = null;

    private Context mContext;
    private Resources mResources;
    private String mInputId;

    public SettingsManager (Context context, Intent intent) {
        mContext = context;
        mInputId = intent.getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        client = new TvClient(intent.getStringExtra(TvInputInfo.EXTRA_LABEL));
        tv = client.getTvInstance();
        mResources = mContext.getResources();
        Log.d(TAG, " @@@@@@@@@@@@@ client.curSource = " + client.curSource);
    }

    public void setTag (String tag) {
        currentTag = tag;
    }

    public String getTag () {
        return currentTag;
    }

    public TvClient getTvClient () {
        return client;
    }

    public Tv getTvInstance () {
        return tv;
    }

    public String getStatus (String key) {
        Log.d(TAG, " current screen is :" + currentTag + ", item is :" + key);

        //picture
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
        //sound
        else if (key.equals(KEY_SOUND_MODE)) {
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
        //channel
        else if (key.equals(KEY_CURRENT_CHANNEL)) {
            return getCurrentChannelStatus();
        } else if (key.equals(KEY_FREQUNCY)) {
            return getFrequencyStatus();
        } else if (key.equals(KEY_AUIDO_TRACK)) {
            return getAudioTrackStatus();
        } else if (key.equals(KEY_SOUND_CHANNEL)) {
            return getSoundChannelStatus();
        } else if (key.equals(KEY_COLOR_SYSTEM)) {
            return getColorSystemStatus();
        } else if (key.equals(KEY_SOUND_SYSTEM)) {
            return getSoundSystemStatus();
        } else if (key.equals(KEY_VOLUME_COMPENSATE)) {
            return getVolumeCompensateStatus();
        } else if (key.equals(KEY_SWITCH_CHANNEL)) {
            return getSwitchChannelStatus();
        }
        //settings
        else if (key.equals(KEY_SLEEP_TIMER)) {
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

    public  String getPictureModeStatus () {
        int pictureModeIndex = tv.GetPQMode(client.curSource);
        switch (pictureModeIndex) {
            case 0:
                return mResources.getString(R.string.standard);
            case 1:
                return mResources.getString(R.string.vivid);
            case 2:
                return mResources.getString(R.string.soft);
            case 3:
                return mResources.getString(R.string.user);
            default:
                return mResources.getString(R.string.standard);
        }
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

    public String getAspectRatioStatus () {
        int itemPosition = tv.GetDisplayMode(client.curSource);
        if (itemPosition == Tv.Display_Mode.DISPLAY_MODE_169.toInt())
            return mResources.getString(R.string.full_screen);
        else if (itemPosition == Tv.Display_Mode.DISPLAY_MODE_MODE43.toInt())
            return mResources.getString(R.string.four2three);
        else if (itemPosition == Tv.Display_Mode.DISPLAY_MODE_FULL.toInt())
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

    public  String get3dSettingsStatus () {
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

    public  String getSoundModeStatus () {
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

    private String getCurrentChannelStatus () {
        return null;
    }

    private String getFrequencyStatus () {
        return null;
    }

    private String getAudioTrackStatus () {
        if (client.curChannel.audioLangs == null)
            return "";
        else
            return client.curChannel.audioLangs[client.curChannel.audioTrackIndex];
    }

    private String getSoundChannelStatus () {
        switch (tv.DtvGetAudioChannleMod()) {
            case 0:
                return mResources.getString(R.string.stereo);
            case 1:
                return mResources.getString(R.string.left_channel);
            case 2:
                return mResources.getString(R.string.right_channel);
            default:
                return mResources.getString(R.string.stereo);
        }
    }

    public ArrayList<HashMap<String,Object>> getChannelInfo () {
        ArrayList<HashMap<String,Object>> list =  new ArrayList<HashMap<String,Object>>();

        HashMap<String,Object> item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.channel_l));
        item.put(STRING_STATUS, "2");
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.frequency_l));
        item.put(STRING_STATUS, "586.0MHz");
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.quality));
        item.put(STRING_STATUS, "18dB");
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.strength));
        item.put(STRING_STATUS, "0%");
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.type));
        item.put(STRING_STATUS, "1");
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.service_id));
        item.put(STRING_STATUS, "83");
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.pcr_id));
        item.put(STRING_STATUS, "831");
        list.add(item);

        return list;
    }

    public ArrayList<HashMap<String,Object>> getAudioTrackList () {
        ArrayList<HashMap<String,Object>> list =  new ArrayList<HashMap<String,Object>>();

        if (client.curChannel.audioLangs != null)
            for (int i=0;i<client.curChannel.audioLangs.length;i++) {
                HashMap<String,Object> item = new HashMap<String,Object>();
                item.put(STRING_NAME, client.curChannel.audioLangs[i]);
                list.add(item);
            }

        return list;
    }

    public ArrayList<HashMap<String,Object>> getSoundChannelList () {
        ArrayList<HashMap<String,Object>> list =  new ArrayList<HashMap<String,Object>>();

        HashMap<String,Object> item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.stereo));
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.left_channel));
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(STRING_NAME, mResources.getString(R.string.right_channel));
        list.add(item);

        return list;
    }

    private String getColorSystemStatus () {
        switch (client.curChannel.videoStd)
        {
            case 0:
                return mResources.getString(R.string.auto);
            case 1:
                return mResources.getString(R.string.pal);
            case 2:
                return mResources.getString(R.string.ntsc);
            default:
                return mResources.getString(R.string.auto);
        }
    }

    private String getSoundSystemStatus () {
        switch (client.curChannel.audioStd)
        {
            case 0:
                return mResources.getString(R.string.sound_system_dk);
            case 1:
                return mResources.getString(R.string.sound_system_i);
            case 2:
                return mResources.getString(R.string.sound_system_bg);
            case 3:
                return mResources.getString(R.string.sound_system_m);
            default:
                return mResources.getString(R.string.sound_system_dk);
        }
    }

    private String getVolumeCompensateStatus () {
        return client.curChannel.audioCompensation + "";
    }

    public int getFineTuneProgress () {
        return 50;
    }

    private int mSearchProgress;
    private int mSearchedNumber;

    public String getInputId() {
        return mInputId;
    }

    public int setManualSearchProgress (int progress) {
        mSearchProgress = progress;
        return progress;
    }

    public int setManualSearchSearchedNumber (int number) {
        mSearchedNumber = number;
        return number;
    }

    public int setAutoSearchProgress (int progress) {
        mSearchProgress = progress;
        return progress;
    }

    public int setAutoSearchSearchedNumber (int number) {
        mSearchedNumber = number;
        return number;
    }

    public int getManualSearchProgress () {
        return mSearchProgress;
    }

    public int getManualSearchSearchedNumber () {
        return mSearchedNumber;
    }

    public int getAutoSearchProgress () {
        return mSearchProgress;
    }

    public int getAutoSearchSearchedNumber () {
        return mSearchedNumber;
    }

    public ArrayList<HashMap<String,Object>> getChannelList (int type) {
        ArrayList<HashMap<String,Object>> list =  new ArrayList<HashMap<String,Object>>();
        ArrayList<ChannelInfo> channelList = getChannelInfoList(type);

        if (channelList.size() > 0) {
            for (int i = 0 ; i < channelList.size(); i++) {
                ChannelInfo info = channelList.get(i);
                HashMap<String,Object> item = new HashMap<String,Object>();
                if (info.skip == 0)
                    item.put(STRING_ICON, R.drawable.skip);
                else if (info.fav == 1)
                    item.put(STRING_ICON, R.drawable.favourite);
                else
                    item.put(STRING_ICON, 0);

                item.put(STRING_NAME, info.name);
                list.add(item);
            }
        } else {
            HashMap<String,Object> item = new HashMap<String,Object>();
            item.put(STRING_ICON, 0);
            item.put(STRING_NAME, mResources.getString(R.string.error_no_channel));
            list.add(item);
        }

        return list;
    }

    private ArrayList<ChannelInfo> getChannelInfoList (int type) {
        ArrayList<ChannelInfo> channelList = null;
        switch (type) {
            case ChannelEdit.TYPE_ATV:
                channelList = TvContractUtils.getAtvChannelList(mContext, mInputId);
                break;
            case ChannelEdit.TYPE_DTV_TV:
                channelList = TvContractUtils.getDtvChannelList(mContext, mInputId, 1);
                break;
            case ChannelEdit.TYPE_DTV_RADIO:
                channelList = TvContractUtils.getDtvChannelList(mContext, mInputId, 2);
                break;
        }
        return channelList;
    }

    private ChannelInfo getChannelByNumber (int type, int channelNumber) {
        ChannelInfo channel = getChannelInfoList(type).get(channelNumber);

        return channel;
    }

    private String getSwitchChannelStatus () {
        if (tv.SSMReadBlackoutEnalbe() == 0)
            return mResources.getString(R.string.static_frame);
        else
            return mResources.getString(R.string.black_frame);
    }

    public String getSleepTimerStatus () {
        String ret = "";
        int time = Settings.System.getInt(mContext.getContentResolver(), KEY_SLEEP_TIMER, DEFAULT_SLEEP_TIMER);
        switch (time)
        {
            case 0://off
                ret = mResources.getString(R.string.off);
                break;
            case 15://15min
                ret = mResources.getString(R.string.time_15min);
                break;
            case 30://30min
                ret = mResources.getString(R.string.time_30min);
                break;
            case 45://45min
                ret = mResources.getString(R.string.time_45min);
                break;
            case 60://60min
                ret = mResources.getString(R.string.time_60min);
                break;
            case 90://90min
                ret = mResources.getString(R.string.time_90min);
                break;
            case 120://120min
                ret = mResources.getString(R.string.time_120min);
                break;
            default:
                ret = mResources.getString(R.string.off);
                break;
        }
        return ret;
    }

    private String getMenuTimeStatus () {
        int seconds = Settings.System.getInt(mContext.getContentResolver(), KEY_MENU_TIME, DEFUALT_MENU_TIME);
        switch (seconds) {
            case 10:
                return mResources.getString(R.string.time_10s);
            case 20:
                return mResources.getString(R.string.time_20s);
            case 40:
                return mResources.getString(R.string.time_40s);
            case 60:
                return mResources.getString(R.string.time_60s);
            default:
                return mResources.getString(R.string.time_10s);
        }
    }

    private String getStartupSettingStatus () {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);

        if (res.activityInfo.packageName.equals(LAUNCHER_NAME))
            return mResources.getString(R.string.launcher);
        else
            return mResources.getString(R.string.tv);
    }

    private String getDynamicBacklightStatus () {
        int itemPosition = tv.isAutoBackLighting();
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else
            return mResources.getString(R.string.on);
    }

    public void setPictureMode (String mode) {
        if (mode.equals(STATUS_STANDARD)) {
            tv.SetPQMode(Tv.Pq_Mode.PQ_MODE_STANDARD, client.curSource, 1);
        } else if (mode.equals(STATUS_VIVID)) {
            tv.SetPQMode(Tv.Pq_Mode.PQ_MODE_BRIGHT, client.curSource, 1);
        } else if (mode.equals(STATUS_SOFT)) {
            tv.SetPQMode(Tv.Pq_Mode.PQ_MODE_SOFTNESS, client.curSource, 1);
        } else if (mode.equals(STATUS_USER)) {
            tv.SetPQMode(Tv.Pq_Mode.PQ_MODE_USER, client.curSource, 1);
        }
    }

    public void setBrightness (int step) {
        if (step == PERCENT_INCREASE)
            tv.SetBrightness(tv.GetBrightness(client.curSource) + 1, client.curSource, 1);
        else
            tv.SetBrightness(tv.GetBrightness(client.curSource) - 1, client.curSource, 1);
    }

    public void setContrast (int step) {
        if (step == PERCENT_INCREASE)
            tv.SetContrast(tv.GetContrast(client.curSource) + 1, client.curSource, 1);
        else
            tv.SetContrast(tv.GetContrast(client.curSource) - 1, client.curSource, 1);
    }

    public void setColor (int step) {
        if (step == PERCENT_INCREASE)
            tv.SetSaturation(tv.GetSaturation(client.curSource) + 1, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
        else
            tv.SetSaturation(tv.GetSaturation(client.curSource) - 1, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
    }

    public void setSharpness (int step) {
        if (step == PERCENT_INCREASE)
            tv.SetSharpness(tv.GetSharpness(client.curSource) + 1, client.curSource, 1, 0, 1);
        else
            tv.SetSharpness(tv.GetSharpness(client.curSource) - 1, client.curSource, 1, 0, 1);
    }

    public void setBacklight (int step) {
        if (step == PERCENT_INCREASE)
            tv.SetBacklight(tv.GetBacklight(client.curSource) + 1, client.curSource, 1);
        else
            tv.SetBacklight(tv.GetBacklight(client.curSource) - 1, client.curSource, 1);
    }

    public void setColorTemperature(String mode) {
        if (mode.equals(STATUS_STANDARD))
            tv.SetColorTemperature(Tv.color_temperature.COLOR_TEMP_STANDARD, client.curSource, 1);
        else if (mode.equals(STATUS_WARM))
            tv.SetColorTemperature(Tv.color_temperature.COLOR_TEMP_WARM, client.curSource, 1);
        else if (mode.equals(STATUS_COOL))
            tv.SetColorTemperature(Tv.color_temperature.COLOR_TEMP_COLD, client.curSource, 1);
    }

    public void setAspectRatio(String mode) {
        if (tv.Get3DMode() == 0) {
            if (mode.equals(STATUS_AUTO)) {
                tv.SetDisplayMode(Tv.Display_Mode.DISPLAY_MODE_NORMAL, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
            } else if (mode.equals(STATUS_4_TO_3)) {
                tv.SetDisplayMode(Tv.Display_Mode.DISPLAY_MODE_MODE43, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
            } else if (mode.equals(STATUS_PANORAMA)) {
                tv.SetDisplayMode(Tv.Display_Mode.DISPLAY_MODE_FULL, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
            } else if (mode.equals(STATUS_FULL_SCREEN)) {
                tv.SetDisplayMode(Tv.Display_Mode.DISPLAY_MODE_169, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
            }
        }
    }

    public void setDnr (String mode) {
        if (mode.equals(STATUS_OFF))
            tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCE_NOISE_CLOSE, client.curSource, 1);
        else if (mode.equals(STATUS_AUTO))
            tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCTION_MODE_AUTO, client.curSource, 1);
        else if (mode.equals(STATUS_MEDIUM))
            tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCE_NOISE_MID, client.curSource, 1);
        else if (mode.equals(STATUS_HIGH))
            tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCE_NOISE_STRONG, client.curSource, 1);
        else if (mode.equals(STATUS_LOW))
            tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCE_NOISE_WEAK, client.curSource, 1);
    }

    public void set3dSettings (String mode) {
        if (mode.equals(STATUS_OFF))
            tv.Set3DMode(Tv.Mode_3D.MODE_3D_CLOSE, Tv.Tvin_3d_Status.STATUS3D_DISABLE);
        else if (mode.equals(STATUS_AUTO))
            tv.Set3DMode(Tv.Mode_3D.MODE_3D_AUTO, Tv.Tvin_3d_Status.STATUS3D_AUTO);
        else if (mode.equals(STATUS_3D_LR_MODE))
            tv.Set3DLRSwith(0, Tv.Tvin_3d_Status.STATUS3D_LR);
        else if (mode.equals(STATUS_3D_RL_MODE))
            tv.Set3DLRSwith(1, Tv.Tvin_3d_Status.STATUS3D_LR);
        else if (mode.equals(STATUS_3D_UD_MODE))
            tv.Set3DLRSwith(0, Tv.Tvin_3d_Status.STATUS3D_BT);
        else if (mode.equals(STATUS_3D_DU_MODE))
            tv.Set3DLRSwith(1, Tv.Tvin_3d_Status.STATUS3D_BT);
        else if (mode.equals(STATUS_3D_TO_2D))
            ;// tv.Set3DTo2DMode(Tv.Mode_3D_2D.values()[position], Tv.Tvin_3d_Status.values()[tv.Get3DMode()]);
    }

    public void setSoundMode (String mode) {
        if (mode.equals(STATUS_STANDARD)) {
            tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_STD);
            tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_STD.toInt());
        } else if (mode.equals(STATUS_MUSIC)) {
            tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_MUSIC);
            tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_MUSIC.toInt());
        } else if (mode.equals(STATUS_NEWS)) {
            tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_NEWS);
            tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_NEWS.toInt());
        } else if (mode.equals(STATUS_MOVIE)) {
            tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_THEATER);
            tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_THEATER.toInt());
        } else if (mode.equals(STATUS_USER)) {
            tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_USER);
            tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_USER.toInt());
        }
    }

    public void setAudioTrack (int position) {
        client.curChannel.audioTrackIndex = position;
        TvContractUtils.updateChannelInfo(mContext, client.curChannel);
    }

    public void setSoundChannel (int position) {
        switch (position) {
            case 0:
                tv.DtvSetAudioChannleMod(0);
                break;
            case 1:
                tv.DtvSetAudioChannleMod(1);
                break;
            case 2:
                tv.DtvSetAudioChannleMod(2);
                break;
        }
    }

    public void setChannelName (int type, int channelNumber, String targetName) {
        ChannelInfo channel = getChannelByNumber(type, channelNumber);
        if (channel != null) {
            channel.name = targetName;
            TvContractUtils.updateChannelInfo(mContext, channel);
        }
    }

    public  void swapChannelPosition (int type, int channelNumber, int targetNumber) {
        if (channelNumber == targetNumber)
            return;

        ChannelInfo sourceChannel = getChannelByNumber(type, channelNumber);
        ChannelInfo targetChannel = getChannelByNumber(type, targetNumber);
        if (sourceChannel != null && targetChannel != null) {
            String tmp = sourceChannel.number;
            sourceChannel.number = targetChannel.number;
            targetChannel.number = tmp;

            TvContractUtils.updateChannelInfo(mContext, sourceChannel);
            TvContractUtils.updateChannelInfo(mContext, targetChannel);
        }
    }

    public  void moveChannelPosition (int type, int channelNumber, int targetNumber) {
        if (channelNumber == targetNumber)
            return;

        ArrayList<ChannelInfo> channelList = getChannelInfoList(type);

        channelList.get(channelNumber).number =  Integer.toString(targetNumber);
        if (targetNumber < channelNumber) {
            for (int i = targetNumber; i < channelNumber; i++)
                channelList.get(i).number = Integer.toString(i + 1);

            for (int i = targetNumber; i <= channelNumber; i++)
                TvContractUtils.updateChannelInfo(mContext, channelList.get(i));

        } else if (targetNumber > channelNumber) {
            for (int i = channelNumber + 1; i <= targetNumber; i++)
                channelList.get(i).number = Integer.toString(i - 1);

            for (int i = channelNumber; i <= targetNumber; i++)
                TvContractUtils.updateChannelInfo(mContext, channelList.get(i));
        }
    }

    public  void skipChannel (int type, int channelNumber) {
        ChannelInfo channel = getChannelByNumber(type, channelNumber);

        if (channel != null) {
            if (channel.skip == 0)
                channel.skip = 1;
            else {
                channel.skip = 0;
                channel.fav = 0;
            }
            TvContractUtils.updateChannelInfo(mContext, channel);
        }
    }

    public  void deleteChannel (int type, int channelNumber) {
        ChannelInfo channel = getChannelByNumber(type, channelNumber);

        if (channel != null)
            TvContractUtils.deleteChannel(mContext, channel);
    }

    public  void setFavouriteChannel (int type, int channelNumber) {
        ChannelInfo channel = getChannelByNumber(type, channelNumber);

        if (channel != null) {
            if (channel.fav == 0) {
                channel.fav = 1;
                channel.skip = 1;
            } else
                channel.fav = 0;
            TvContractUtils.updateChannelInfo(mContext, channel);
        }
    }

    public void setSleepTimer (int mins) {
        Settings.System.putInt(mContext.getContentResolver(), KEY_SLEEP_TIMER, mins);
    }

    public void setMenuTime (int seconds) {
        Settings.System.putInt(mContext.getContentResolver(), KEY_MENU_TIME, seconds);
        ((TvSettingsActivity)mContext).startShowActivityTimer();
    }

    public void setStartupSetting (int type) {
        IntentFilter  mHomeFilter = new IntentFilter(Intent.ACTION_MAIN);
        mHomeFilter.addCategory(Intent.CATEGORY_HOME);
        mHomeFilter.addCategory(Intent.CATEGORY_DEFAULT);

        PackageManager packageManager = mContext.getPackageManager();
        ComponentName component_launcher= new ComponentName(LAUNCHER_NAME, LAUNCHER_ACTIVITY);
        ComponentName component_tv = new ComponentName(TV_NAME, TV_ACTIVITY);
        ComponentName[] components = new ComponentName[] {component_launcher, component_tv};

        if (type == 0) {
            packageManager.replacePreferredActivity(mHomeFilter, IntentFilter.MATCH_CATEGORY_EMPTY,
                components, component_launcher);
        } else {
            packageManager.replacePreferredActivity(mHomeFilter, IntentFilter.MATCH_CATEGORY_EMPTY,
                components, component_tv);
        }
    }

    public void doFactoryReset() {
        setSleepTimer(DEFAULT_SLEEP_TIMER);
        setMenuTime(DEFUALT_MENU_TIME);
        setStartupSetting(0);
       // SystemControlManager mSystemControlManager = new SystemControlManager(mContext);
       // mSystemControlManager.setBootenv("ubootenv.var.upgrade_step", "1");

        for (int i = 0; i < tvPackages.length; i++) {
            ClearPackageData(tvPackages[i]);
        }
        tv.stopAutoBacklight();
        tv.SSMInitDevice();
        tv.FactoryCleanAllTableForProgram();
    }

    private String[] tvPackages = {
        "com.android.providers.tv",
    };

    private  void ClearPackageData(String packageName) {
        Log.d(TAG, "ClearPackageData:" + packageName);
        //clear data
        ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ClearUserDataObserver mClearDataObserver = new ClearUserDataObserver();
        boolean res = am.clearApplicationUserData(packageName, mClearDataObserver);
        if (!res) {
            Log.i(TAG, " clear " + packageName + " data failed");
        } else {
            Log.i(TAG, " clear " + packageName + " data succeed");
        }

        //clear cache
        PackageManager packageManager = mContext.getPackageManager();
        ClearUserDataObserver mClearCacheObserver = new ClearUserDataObserver();
        packageManager.deleteApplicationCacheFiles(packageName, mClearCacheObserver);

        //clear default
        packageManager.clearPackagePreferredActivities(packageName);
    }

    private class ClearUserDataObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
        }
    }
}
