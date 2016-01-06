package com.droidlogic.tvinput.settings;

import android.R.integer;
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
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;
import java.util.HashMap;

import android.media.tv.TvInputInfo;
import com.droidlogic.app.SystemControlManager;
import android.media.tv.TvContract.Channels;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TVMultilingualText;
import com.droidlogic.tvinput.R;
import com.droidlogic.tvinput.services.TimeSuspendService;

public class SettingsManager {
    public static final String TAG = "SettingsManager";

    public static final String KEY_PICTURE                          = "picture";
    public static final String KEY_PICTURE_MODE                     = "picture_mode";
    public static final String KEY_BRIGHTNESS                       = "brightness";
    public static final String KEY_CONTRAST                         = "contrast";
    public static final String KEY_COLOR                            = "color";
    public static final String KEY_SHARPNESS                        = "sharpness";
    public static final String KEY_BACKLIGHT                        = "backlight";
    public static final String KEY_TINT                             = "tint";
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
    public static final String KEY_DEFAULT_LANGUAGE                 = "default_language";
    public static final String KEY_SUBTITLE_SWITCH                  = "sub_switch";
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
    public static final String STATUS_PCM                            = "pcm";
    public static final String STATUS_STEREO                         = "stereo";
    public static final String STATUS_LEFT_CHANNEL                  = "left channel";
    public static final String STATUS_RIGHT_CHANNEL                 = "right channel";
    public static final String STATUS_RAW                            = "raw";

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
    private TvControlManager.SourceInput_Type mTvSource;
    private ChannelInfo currentChannel;
    private TvControlManager mTvControlManager;
    private TvDataBaseManager mTvDataBaseManager;
    private boolean isRadioChannel = false;
    private int mResult = DroidLogicTvUtils.RESULT_OK;

    public SettingsManager (Context context, Intent intent) {
        mContext = context;
        mTvDataBaseManager = new TvDataBaseManager(mContext);

        mInputId = intent.getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        isRadioChannel = intent.getBooleanExtra(DroidLogicTvUtils.EXTRA_IS_RADIO_CHANNEL, false);
        mTvSource = parseTvSourceType(intent.getIntExtra(DroidLogicTvUtils.EXTRA_CHANNEL_DEVICE_ID, -1));

        if (mTvSource == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV
            || mTvSource == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            String channelNumber = intent.getStringExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER);
            Log.d(TAG, "current channelNumber = " +  channelNumber);
            if (!TextUtils.isEmpty(channelNumber))
                currentChannel = getChannelByNumber(parseChannelEditType(), Integer.valueOf(channelNumber));
        }

        mTvControlManager = TvControlManager.open();
        mResources = mContext.getResources();
        Log.d(TAG, " curSource=" + mTvSource + " isRadio=" + isRadioChannel);
    }

    public void setTag (String tag) {
        currentTag = tag;
    }

    public String getTag () {
        return currentTag;
    }

    public TvControlManager.SourceInput_Type getCurentTvSource () {
        return mTvSource;
    }

    public TvControlManager getTvControlManager () {
        return mTvControlManager;
    }

    public TvDataBaseManager getTvDataBaseManager () {
        return mTvDataBaseManager;
    }

    private TvControlManager.SourceInput_Type parseTvSourceType (int deviceId) {
        TvControlManager.SourceInput_Type source;

        switch (deviceId) {
            case DroidLogicTvUtils.DEVICE_ID_ATV:
                source = TvControlManager.SourceInput_Type.SOURCE_TYPE_TV;
                break;
            case DroidLogicTvUtils.DEVICE_ID_AV1:
            case DroidLogicTvUtils.DEVICE_ID_AV2:
                source = TvControlManager.SourceInput_Type.SOURCE_TYPE_AV;
                break;
            case DroidLogicTvUtils.DEVICE_ID_HDMI1:
            case DroidLogicTvUtils.DEVICE_ID_HDMI2:
            case DroidLogicTvUtils.DEVICE_ID_HDMI3:
                source = TvControlManager.SourceInput_Type.SOURCE_TYPE_HDMI;
                break;
            case DroidLogicTvUtils.DEVICE_ID_DTV:
                source = TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV;
                break;
            default:
                source = TvControlManager.SourceInput_Type.SOURCE_TYPE_TV;
                break;
        }
        return source;
    }

    private int parseChannelEditType () {
        if (mTvSource == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV)
            return ChannelEdit.TYPE_ATV;
        else if (mTvSource == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            if (!isRadioChannel)
                return ChannelEdit.TYPE_DTV_TV;
            else
                return ChannelEdit.TYPE_DTV_RADIO;
        }

        return ChannelEdit.TYPE_ATV;
    }

    public void setActivityResult(int result) {
        mResult = result;
    }

    public int getActivityResult() {
        return mResult;
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
        } else if (key.equals(KEY_TINT)) {
            return getTintStatus();
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
        else if (key.equals(KEY_CURRENT_CHANNEL) || key.equals(KEY_CHANNEL_INFO)) {
            return getCurrentChannelStatus();
        } else if (key.equals(KEY_FREQUNCY)) {
            return getFrequencyStatus();
        } else if (key.equals(KEY_AUIDO_TRACK)) {
            return getAudioTrackStatus();
        } else if (key.equals(KEY_SOUND_CHANNEL)) {
            return getSoundChannelStatus();
        } else if (key.equals(KEY_DEFAULT_LANGUAGE)) {
            return getDefaultLanStatus();
        } else if (key.equals(KEY_SUBTITLE_SWITCH)) {
            return getSubtitleSwitchStatus();
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
        int pictureModeIndex = mTvControlManager.GetPQMode(mTvSource);
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
        return mTvControlManager.GetBrightness(mTvSource) + "%";
    }

    private String getContrastStatus () {
        return mTvControlManager.GetContrast(mTvSource) + "%";
    }

    private String getColorStatus () {
        return mTvControlManager.GetSaturation(mTvSource) + "%";
    }

    private String getSharpnessStatus () {
        return mTvControlManager.GetSharpness(mTvSource) + "%";
    }

    private String getBacklightStatus () {
        return mTvControlManager.GetBacklight(mTvSource) + "%";
    }

    public boolean isShowTint() {
        String colorSystem = getColorSystemStatus();
        if (mTvSource == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV
            && colorSystem != null && colorSystem.equals(mResources.getString(R.string.ntsc))) {
            TvControlManager.tvin_info_t tmpInfo = mTvControlManager.GetCurrentSignalInfo();
            if (tmpInfo.status == TvControlManager.tvin_sig_status_t.TVIN_SIG_STATUS_STABLE) {
                Log.d(TAG, "ATV NTSC mode signal is stable, show Tint");
                return true;
            }
        } else if (mTvSource == TvControlManager.SourceInput_Type.SOURCE_TYPE_AV) {
            TvControlManager.tvin_info_t tmpInfo = mTvControlManager.GetCurrentSignalInfo();
            if (tmpInfo.status == TvControlManager.tvin_sig_status_t.TVIN_SIG_STATUS_STABLE) {
                String[] strings = tmpInfo.fmt.toString().split("_");
                if (strings[4].contains("NTSC")) {
                    Log.d(TAG, "AV NTSC mode signal is stable, show Tint");
                    return true;
                }
            }
        }
        return false;
    }

    private String getTintStatus () {
        return mTvControlManager.GetHue(mTvSource) + "%";
    }

    public String getColorTemperatureStatus () {
        int itemPosition = mTvControlManager.GetColorTemperature(mTvSource);
        if (itemPosition == 0)
            return mResources.getString(R.string.standard);
        else if (itemPosition == 1)
            return mResources.getString(R.string.warm);
        else
            return mResources.getString(R.string.cool);
    }

    public String getAspectRatioStatus () {
        int itemPosition = mTvControlManager.GetDisplayMode(mTvSource);
        if (itemPosition == TvControlManager.Display_Mode.DISPLAY_MODE_169.toInt())
            return mResources.getString(R.string.full_screen);
        else if (itemPosition == TvControlManager.Display_Mode.DISPLAY_MODE_MODE43.toInt())
            return mResources.getString(R.string.four2three);
        else if (itemPosition == TvControlManager.Display_Mode.DISPLAY_MODE_FULL.toInt())
            return mResources.getString(R.string.panorama);
        else
            return mResources.getString(R.string.auto);
    }

    public String getDnrStatus () {
        int itemPosition = mTvControlManager.GetNoiseReductionMode(mTvSource);
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
        if (mTvControlManager.Get3DTo2DMode() != 0)
            return mResources.getString(R.string.mode_3d_to_2d);
        int threeD_mode = mTvControlManager.Get3DMode();
        if (threeD_mode == TvControlManager.Mode_3D.MODE_3D_CLOSE.toInt()) {
            return mResources.getString(R.string.off);
        } else if (threeD_mode == TvControlManager.Mode_3D.MODE_3D_AUTO.toInt()) {
            return mResources.getString(R.string.auto);
        } else if (threeD_mode == TvControlManager.Mode_3D.MODE_3D_LEFT_RIGHT.toInt()) {
            if (mTvControlManager.Get3DLRSwith() == 0)
                return mResources.getString(R.string.mode_lr);
            else
                return mResources.getString(R.string.mode_rl);
        } else if (threeD_mode == TvControlManager.Mode_3D.MODE_3D_UP_DOWN.toInt()) {
            if (mTvControlManager.Get3DLRSwith() == 0)
                return mResources.getString(R.string.mode_ud);
            else
                return mResources.getString(R.string.mode_du);
        } else {
            return null;
        }
    }

    public  String getSoundModeStatus () {
        int itemPosition = mTvControlManager.GetCurAudioSoundMode();
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
        return mTvControlManager.GetCurAudioTrebleVolume() + "%";
    }

    private String getBassStatus () {
        return mTvControlManager.GetCurAudioBassVolume() + "%";
    }

    private String getBalanceStatus () {
        return mTvControlManager.GetCurAudioBalance() + "%";
    }

    public String getSpdifStatus () {
        if (mTvControlManager.GetCurAudioSPDIFSwitch() == 0)
            return mResources.getString(R.string.off);
        int itemPosition = mTvControlManager.GetCurAudioSPDIFMode();
        if (itemPosition == 0)
            return mResources.getString(R.string.pcm);
        else if (itemPosition == 1)
            return mResources.getString(R.string.raw);
        else
            return null;
    }

    public String getSurroundStatus () {
        int itemPosition = mTvControlManager.GetCurAudioSrsSurround();
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else
            return mResources.getString(R.string.on);
    }

    public String getDialogClarityStatus () {
        int itemPosition = mTvControlManager.GetCurAudioSrsDialogClarity();
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else
            return mResources.getString(R.string.on);
    }

    public String getBassBoostStatus () {
        int itemPosition = mTvControlManager.GetCurAudioSrsTruBass();
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else
            return mResources.getString(R.string.on);
    }

    public ChannelInfo getCurrentChannel() {
        return currentChannel;
    }

    public int getCurrentChannelNumber() {
        if (currentChannel != null)
            return currentChannel.getNumber();

        return -1;
    }

    private String getCurrentChannelStatus () {
        if (currentChannel != null)
            return currentChannel.getDisplayName();

        return null;
    }

    private String getFrequencyStatus () {
        if (currentChannel != null)
            return Integer.toString(currentChannel.getFrequency());

        return null;
    }

    private String getAudioTrackStatus () {
        if (currentChannel != null &&  currentChannel.getAudioLangs() != null)
            return currentChannel.getAudioLangs()[currentChannel.getAudioTrackIndex()];
        else
            return null;
    }

    public String getSoundChannelStatus () {
        if (currentChannel != null) {
            switch (currentChannel.getAudioChannel()) {
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
        return null;
    }

    public ArrayList<HashMap<String,Object>> getChannelInfo () {
        ArrayList<HashMap<String,Object>> list =  new ArrayList<HashMap<String,Object>>();

        if (currentChannel != null) {
            HashMap<String,Object> item = new HashMap<String,Object>();
            item.put(STRING_NAME, mResources.getString(R.string.channel_l));
            item.put(STRING_STATUS, currentChannel.getDisplayName());
            list.add(item);

            item = new HashMap<String,Object>();
            item.put(STRING_NAME, mResources.getString(R.string.frequency_l));
            item.put(STRING_STATUS, Integer.toString(currentChannel.getFrequency()));
            list.add(item);

            /*item = new HashMap<String,Object>();
            item.put(STRING_NAME, mResources.getString(R.string.quality));
            item.put(STRING_STATUS, "18dB");
            list.add(item);

            item = new HashMap<String,Object>();
            item.put(STRING_NAME, mResources.getString(R.string.strength));
            item.put(STRING_STATUS, "0%");
            list.add(item);*/

            item = new HashMap<String,Object>();
            item.put(STRING_NAME, mResources.getString(R.string.type));
            item.put(STRING_STATUS, currentChannel.getType());
            list.add(item);

            item = new HashMap<String,Object>();
            item.put(STRING_NAME, mResources.getString(R.string.service_id));
            item.put(STRING_STATUS, Integer.toString(currentChannel.getServiceId()));
            list.add(item);

            item = new HashMap<String,Object>();
            item.put(STRING_NAME, mResources.getString(R.string.pcr_id));
            item.put(STRING_STATUS, Integer.toString(currentChannel.getPcrPid()));
            list.add(item);
        }
        return list;
    }

    public ArrayList<HashMap<String,Object>> getAudioTrackList () {
        ArrayList<HashMap<String,Object>> list =  new ArrayList<HashMap<String,Object>>();

        if (currentChannel != null && currentChannel.getAudioLangs() != null)
            for (int i=0;i<currentChannel.getAudioLangs().length;i++) {
                HashMap<String,Object> item = new HashMap<String,Object>();
                item.put(STRING_NAME, currentChannel.getAudioLangs()[i]);
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

    public ArrayList<HashMap<String,Object>> getDefaultLanguageList () {
        ArrayList<HashMap<String,Object>> list =  new ArrayList<HashMap<String,Object>>();

        String[] def_lanArray = mResources.getStringArray(R.array.def_lan);
        for (String lanNameString : def_lanArray) {
            HashMap<String,Object> item = new HashMap<String,Object>();
            item.put(STRING_NAME, lanNameString);
            list.add(item);
        }

        return list;
    }

    private String getDefaultLanStatus () {
        String ret = Settings.System.getString(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_DEFAULT_LANGUAGE);
        String[] lanArray;
        if (ret == null) {
            lanArray =  mResources.getStringArray(R.array.def_lan);
            for (String lang : lanArray) {
                if (lang.equals(Locale.getDefault().getISO3Language()))
                    return lang;
            }
            ret = lanArray[0];
        } else {
            if (ret.equals("")) {
                lanArray =  mResources.getStringArray(R.array.def_lan);
                for (String lang : lanArray) {
                    if (lang.equals(Locale.getDefault().getISO3Language()))
                        return lang;
                }
                ret = lanArray[0];
            }
        }
        return ret;
    }

    public String getSubtitleSwitchStatus () {
        int switchVal = Settings.System.getInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_SUBTITLE_SWITCH, 0);
        switch (switchVal) {
            case 0:
                return mResources.getString(R.string.off);
            case 1:
                return mResources.getString(R.string.on);
            default:
                return mResources.getString(R.string.off);
        }
    }

    public String getColorSystemStatus () {
        if (currentChannel != null) {
            switch (currentChannel.getVideoStd())
            {
                case 1:
                    return mResources.getString(R.string.pal);
                case 2:
                    return mResources.getString(R.string.ntsc);
                default:
                    return mResources.getString(R.string.pal);
            }
        }
        return null;
    }

    public String getSoundSystemStatus () {
        if (currentChannel != null) {
            switch (currentChannel.getAudioStd())
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

        return null;
    }

    private String getVolumeCompensateStatus () {
        if (currentChannel != null)
            return currentChannel.getAudioCompensation() + "";
        else
            return null;
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
                if (!info.isBrowsable())
                    item.put(STRING_ICON, R.drawable.skip);
                else if (info.isFavourite())
                    item.put(STRING_ICON, R.drawable.favourite);
                else
                    item.put(STRING_ICON, 0);

                item.put(STRING_NAME, i + ". " + info.getDisplayName());
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
            case ChannelEdit.TYPE_DTV_TV:
                channelList = mTvDataBaseManager.getChannelList(mInputId, Channels.SERVICE_TYPE_AUDIO_VIDEO);
                break;
            case ChannelEdit.TYPE_DTV_RADIO:
                channelList = mTvDataBaseManager.getChannelList(mInputId, Channels.SERVICE_TYPE_AUDIO);
                break;
        }
        return channelList;
    }

    private ChannelInfo getChannelByNumber (int type, int channelNumber) {
        ArrayList<ChannelInfo> channelList = getChannelInfoList(type);

        if (channelList != null && channelList.size() > 0) {
            for (int i = 0; i < channelList.size(); i++) {
                ChannelInfo info = channelList.get(i);
                if (info != null && info.getNumber() == channelNumber)
                    return info;
            }
        }

        return null;
    }

    public String getSwitchChannelStatus () {
        if (mTvControlManager.SSMReadBlackoutEnalbe() == 0)
            return mResources.getString(R.string.static_frame);
        else
            return mResources.getString(R.string.black_frame);
    }

    public String getSleepTimerStatus () {
        String ret = "";
        int time = SystemProperties.getInt("tv.sleep_timer", 0);
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

    public String getMenuTimeStatus () {
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

    public String getStartupSettingStatus () {
        int type = Settings.System.getInt(mContext.getContentResolver(), "tv_start_up_enter_app", 0);

        if (type == 0)
            return mResources.getString(R.string.launcher);
        else
            return mResources.getString(R.string.tv);
    }

    public String getDynamicBacklightStatus () {
        int itemPosition = mTvControlManager.isAutoBackLighting();
        if (itemPosition == 0)
            return mResources.getString(R.string.off);
        else
            return mResources.getString(R.string.on);
    }

    public void setPictureMode (String mode) {
        if (mode.equals(STATUS_STANDARD)) {
            mTvControlManager.SetPQMode(TvControlManager.Pq_Mode.PQ_MODE_STANDARD, mTvSource, 1);
        } else if (mode.equals(STATUS_VIVID)) {
            mTvControlManager.SetPQMode(TvControlManager.Pq_Mode.PQ_MODE_BRIGHT, mTvSource, 1);
        } else if (mode.equals(STATUS_SOFT)) {
            mTvControlManager.SetPQMode(TvControlManager.Pq_Mode.PQ_MODE_SOFTNESS, mTvSource, 1);
        } else if (mode.equals(STATUS_USER)) {
            mTvControlManager.SetPQMode(TvControlManager.Pq_Mode.PQ_MODE_USER, mTvSource, 1);
        }
    }

    private int setPictureUserMode(String key) {
        int brightness = mTvControlManager.GetBrightness(mTvSource);
        int contrast = mTvControlManager.GetContrast(mTvSource);
        int color = mTvControlManager.GetSaturation(mTvSource);
        int sharpness = mTvControlManager.GetSharpness(mTvSource);
        int tint = -1;
        if (isShowTint())
            tint = mTvControlManager.GetHue(mTvSource);
        int ret = -1;

        switch (mTvControlManager.GetPQMode(mTvSource)) {
            case 0:
            case 1:;
            case 2:
                setPictureMode(STATUS_USER);
                break;
        }

        //Log.d(TAG, " brightness=" + brightness + " contrast=" + contrast + " color=" + color + " sharp=" + sharpness);
        if (!key.equals(KEY_BRIGHTNESS))
            mTvControlManager.SetBrightness(brightness, mTvSource, 1);
        else
            ret = brightness;

        if (!key.equals(KEY_CONTRAST))
            mTvControlManager.SetContrast(contrast, mTvSource, 1);
        else
            ret = contrast;

        if (!key.equals(KEY_COLOR))
            mTvControlManager.SetSaturation(color, mTvSource, mTvControlManager.GetCurrentSignalInfo().fmt, 1);
        else
            ret = color;

        if (!key.equals(KEY_SHARPNESS))
            mTvControlManager.SetSharpness(sharpness, mTvSource, 1, 0, 1);
        else
            ret = sharpness;

        if (isShowTint()) {
            if (!key.equals(KEY_TINT))
                mTvControlManager.SetHue(tint, mTvSource, mTvControlManager.GetCurrentSignalInfo().fmt, 1);
            else
                ret = tint;
        }

        return ret;
    }

    public void setBrightness (int step) {
        if (mTvControlManager.GetPQMode(mTvSource) == 3)
            mTvControlManager.SetBrightness(mTvControlManager.GetBrightness(mTvSource) + step, mTvSource, 1);
        else
            mTvControlManager.SetBrightness(setPictureUserMode(KEY_BRIGHTNESS) + step, mTvSource, 1);
    }

    public void setContrast (int step) {
        if (mTvControlManager.GetPQMode(mTvSource) == 3)
            mTvControlManager.SetContrast(mTvControlManager.GetContrast(mTvSource) + step, mTvSource, 1);
        else
            mTvControlManager.SetContrast(setPictureUserMode(KEY_CONTRAST) + step, mTvSource, 1);
    }

    public void setColor (int step) {
        if (mTvControlManager.GetPQMode(mTvSource) == 3)
            mTvControlManager.SetSaturation(mTvControlManager.GetSaturation(mTvSource) + step,
                mTvSource, mTvControlManager.GetCurrentSignalInfo().fmt, 1);
        else
            mTvControlManager.SetSaturation(setPictureUserMode(KEY_COLOR) + step,
                mTvSource, mTvControlManager.GetCurrentSignalInfo().fmt, 1);
    }

    public void setSharpness (int step) {
        if (mTvControlManager.GetPQMode(mTvSource) == 3)
            mTvControlManager.SetSharpness(mTvControlManager.GetSharpness(mTvSource) + step, mTvSource, 1, 0, 1);
        else
            mTvControlManager.SetSharpness(setPictureUserMode(KEY_SHARPNESS) + step, mTvSource, 1, 0, 1);
    }

    public void setTint (int step) {
        if (isShowTint()) {
            if (mTvControlManager.GetPQMode(mTvSource) == 3)
                mTvControlManager.SetHue(mTvControlManager.GetHue(mTvSource) + step, mTvSource,
                    mTvControlManager.GetCurrentSignalInfo().fmt, 1);
            else
                mTvControlManager.SetHue(setPictureUserMode(KEY_TINT) + step, mTvSource,
                    mTvControlManager.GetCurrentSignalInfo().fmt, 1);
        }
    }

    public void setBacklight (int step) {
        int value = mTvControlManager.GetBacklight(mTvSource) + step;
        if (value >= 0 && value <= 100) {
            mTvControlManager.SetBacklight(value, mTvSource, 1);
        }
    }

    public void setColorTemperature(String mode) {
        if (mode.equals(STATUS_STANDARD))
            mTvControlManager.SetColorTemperature(TvControlManager.color_temperature.COLOR_TEMP_STANDARD, mTvSource, 1);
        else if (mode.equals(STATUS_WARM))
            mTvControlManager.SetColorTemperature(TvControlManager.color_temperature.COLOR_TEMP_WARM, mTvSource, 1);
        else if (mode.equals(STATUS_COOL))
            mTvControlManager.SetColorTemperature(TvControlManager.color_temperature.COLOR_TEMP_COLD, mTvSource, 1);
    }

    public void setAspectRatio(String mode) {
        if (mTvControlManager.Get3DMode() == 0) {
            if (mode.equals(STATUS_AUTO)) {
                mTvControlManager.SetDisplayMode(TvControlManager.Display_Mode.DISPLAY_MODE_NORMAL,
                    mTvSource, mTvControlManager.GetCurrentSignalInfo().fmt, 1);
            } else if (mode.equals(STATUS_4_TO_3)) {
                mTvControlManager.SetDisplayMode(TvControlManager.Display_Mode.DISPLAY_MODE_MODE43,
                    mTvSource, mTvControlManager.GetCurrentSignalInfo().fmt, 1);
            } else if (mode.equals(STATUS_PANORAMA)) {
                mTvControlManager.SetDisplayMode(TvControlManager.Display_Mode.DISPLAY_MODE_FULL,
                    mTvSource, mTvControlManager.GetCurrentSignalInfo().fmt, 1);
            } else if (mode.equals(STATUS_FULL_SCREEN)) {
                mTvControlManager.SetDisplayMode(TvControlManager.Display_Mode.DISPLAY_MODE_169,
                    mTvSource, mTvControlManager.GetCurrentSignalInfo().fmt, 1);
            }
        }
    }

    public void setDnr (String mode) {
        if (mode.equals(STATUS_OFF))
            mTvControlManager.SetNoiseReductionMode(TvControlManager.Noise_Reduction_Mode.REDUCE_NOISE_CLOSE, mTvSource, 1);
        else if (mode.equals(STATUS_AUTO))
            mTvControlManager.SetNoiseReductionMode(TvControlManager.Noise_Reduction_Mode.REDUCTION_MODE_AUTO, mTvSource, 1);
        else if (mode.equals(STATUS_MEDIUM))
            mTvControlManager.SetNoiseReductionMode(TvControlManager.Noise_Reduction_Mode.REDUCE_NOISE_MID, mTvSource, 1);
        else if (mode.equals(STATUS_HIGH))
            mTvControlManager.SetNoiseReductionMode(TvControlManager.Noise_Reduction_Mode.REDUCE_NOISE_STRONG, mTvSource, 1);
        else if (mode.equals(STATUS_LOW))
            mTvControlManager.SetNoiseReductionMode(TvControlManager.Noise_Reduction_Mode.REDUCE_NOISE_WEAK, mTvSource, 1);
    }

    public void set3dSettings (String mode) {
        if (mode.equals(STATUS_OFF))
            mTvControlManager.Set3DMode(TvControlManager.Mode_3D.MODE_3D_CLOSE, TvControlManager.Tvin_3d_Status.STATUS3D_DISABLE);
        else if (mode.equals(STATUS_AUTO))
            mTvControlManager.Set3DMode(TvControlManager.Mode_3D.MODE_3D_AUTO, TvControlManager.Tvin_3d_Status.STATUS3D_AUTO);
        else if (mode.equals(STATUS_3D_LR_MODE))
            mTvControlManager.Set3DLRSwith(0, TvControlManager.Tvin_3d_Status.STATUS3D_LR);
        else if (mode.equals(STATUS_3D_RL_MODE))
            mTvControlManager.Set3DLRSwith(1, TvControlManager.Tvin_3d_Status.STATUS3D_LR);
        else if (mode.equals(STATUS_3D_UD_MODE))
            mTvControlManager.Set3DLRSwith(0, TvControlManager.Tvin_3d_Status.STATUS3D_BT);
        else if (mode.equals(STATUS_3D_DU_MODE))
            mTvControlManager.Set3DLRSwith(1, TvControlManager.Tvin_3d_Status.STATUS3D_BT);
        else if (mode.equals(STATUS_3D_TO_2D))
            ;// tv.Set3DTo2DMode(Tv.Mode_3D_2D.values()[position], Tv.Tvin_3d_Status.values()[tv.Get3DMode()]);
    }

    public void setSoundMode (String mode) {
        if (mode.equals(STATUS_STANDARD)) {
            mTvControlManager.SetAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_STD);
            mTvControlManager.SaveCurAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_STD.toInt());
        } else if (mode.equals(STATUS_MUSIC)) {
            mTvControlManager.SetAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_MUSIC);
            mTvControlManager.SaveCurAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_MUSIC.toInt());
        } else if (mode.equals(STATUS_NEWS)) {
            mTvControlManager.SetAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_NEWS);
            mTvControlManager.SaveCurAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_NEWS.toInt());
        } else if (mode.equals(STATUS_MOVIE)) {
            mTvControlManager.SetAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_THEATER);
            mTvControlManager.SaveCurAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_THEATER.toInt());
        } else if (mode.equals(STATUS_USER)) {
            mTvControlManager.SetAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_USER);
            mTvControlManager.SaveCurAudioSoundMode(TvControlManager.Sound_Mode.SOUND_MODE_USER.toInt());
        }
    }

    public void setTreble (int step) {
        int treble_value = mTvControlManager.GetCurAudioTrebleVolume() + step;

        int bass_value = -1;
        if (mTvControlManager.GetCurAudioSoundMode() != 4)
            bass_value = mTvControlManager.GetCurAudioBassVolume();

        if (treble_value >= 0 && treble_value <= 100) {
            mTvControlManager.SetAudioTrebleVolume(treble_value);
            mTvControlManager.SaveCurAudioTrebleVolume(treble_value);
        }

        if (bass_value != -1) {
            mTvControlManager.SetAudioBassVolume(bass_value);
            mTvControlManager.SaveCurAudioBassVolume(bass_value);
        }
    }

    public void setBass (int step) {
        int bass_value = mTvControlManager.GetCurAudioBassVolume() + step;

        int treble_value = -1;
        if (mTvControlManager.GetCurAudioSoundMode() != 4)
            treble_value = mTvControlManager.GetCurAudioTrebleVolume();

        if (bass_value >= 0 && bass_value <= 100) {
            mTvControlManager.SetAudioBassVolume(bass_value);
            mTvControlManager.SaveCurAudioBassVolume(bass_value);
        }

        if (treble_value != -1) {
            mTvControlManager.SetAudioTrebleVolume(treble_value);
            mTvControlManager.SaveCurAudioTrebleVolume(treble_value);
        }
    }

    public void setBalance (int step) {
        int balance_value = mTvControlManager.GetCurAudioBalance() + step;
        if (balance_value >= 0 && balance_value <= 100) {
            mTvControlManager.SetAudioBalance(balance_value);
            mTvControlManager.SaveCurAudioBalance(balance_value);
        }
    }

    public void setSpdif (String mode) {
        if (mode.equals(STATUS_OFF)) {
            mTvControlManager.SetAudioSPDIFSwitch(0);
            mTvControlManager.SaveCurAudioSPDIFSwitch(0);
        } else if (mode.equals(STATUS_PCM)) {
            mTvControlManager.SetAudioSPDIFSwitch(1);
            mTvControlManager.SaveCurAudioSPDIFSwitch(1);
            mTvControlManager.SetAudioSPDIFMode(0);
            mTvControlManager.SaveCurAudioSPDIFMode(0);
        } else if (mode.equals(STATUS_RAW)) {
            mTvControlManager.SetAudioSPDIFSwitch(1);
            mTvControlManager.SaveCurAudioSPDIFSwitch(1);
            mTvControlManager.SetAudioSPDIFMode(1);
            mTvControlManager.SaveCurAudioSPDIFMode(1);
        }
    }

    public void setSurround (String mode) {
        if (mode.equals(STATUS_ON)) {
            mTvControlManager.SetAudioSrsSurround(1);
            mTvControlManager.SaveCurAudioSrsSurround(1);
        } else if (mode.equals(STATUS_OFF)) {
            setDialogClarity(STATUS_OFF);
            setBassBoost(STATUS_OFF);
            mTvControlManager.SetAudioSrsSurround(0);
            mTvControlManager.SaveCurAudioSrsSurround(0);
        }
    }

    public void setDialogClarity (String mode) {
        if (mode.equals(STATUS_ON)) {
            setSurround(STATUS_ON);
            mTvControlManager.SetAudioSrsDialogClarity(1);
            mTvControlManager.SaveCurAudioSrsDialogClarity(1);
        } else if (mode.equals(STATUS_OFF)) {
            mTvControlManager.SetAudioSrsDialogClarity(0);
            mTvControlManager.SaveCurAudioSrsDialogClarity(0);
        }
    }

    public void setBassBoost (String mode) {
        if (mode.equals(STATUS_ON)) {
            setSurround(STATUS_ON);
            mTvControlManager.SetAudioSrsTruBass(1);
            mTvControlManager.SaveCurAudioSrsTruBass(1);
        } else if (mode.equals(STATUS_OFF)) {
            mTvControlManager.SetAudioSrsTruBass(0);
            mTvControlManager.SaveCurAudioSrsTruBass(0);;
        }
    }

    public void setAudioTrack (int position) {
        if (currentChannel != null) {
            mTvControlManager.DtvSwitchAudioTrack(currentChannel.getAudioPids()[position],
                currentChannel.getAudioFormats()[position], 0);
            currentChannel.setAudioTrackIndex(position);
            mTvDataBaseManager.updateChannelInfo(currentChannel);
        }
    }

    public void setSoundChannel (int mode) {
       if (currentChannel != null) {
           currentChannel.setAudioChannel(mode);
           mTvDataBaseManager.updateChannelInfo(currentChannel);
           mTvControlManager.DtvSetAudioChannleMod(currentChannel.getAudioChannel());
       }
    }

    public void setDefLanguage (int position) {
        String[] def_lanArray = mResources.getStringArray(R.array.def_lan);
        Settings.System.putString(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_DEFAULT_LANGUAGE, def_lanArray[position]);
        ArrayList<ChannelInfo> mVideoChannels = mTvDataBaseManager.getChannelList(mInputId, Channels.SERVICE_TYPE_AUDIO_VIDEO);
        int trackIdx = 0;
        for (ChannelInfo channel : mVideoChannels) {
            if (channel.getAudioLangs() != null) {
                String[] strArray = channel.getAudioLangs();
                for (trackIdx = 0;trackIdx < strArray.length;trackIdx++) {
                    if (def_lanArray[position].equals(strArray[trackIdx]))
                        break;
                }
                trackIdx %= strArray.length;
                channel.setAudioTrackIndex(trackIdx);
                mTvDataBaseManager.updateChannelInfo(channel);
            }
        }
    }

    public void setColorSystem(int mode) {
        if (currentChannel != null) {
            currentChannel.setVideoStd(mode);
            mTvDataBaseManager.updateChannelInfo(currentChannel);
            mTvControlManager.SetFrontendParms(TvControlManager.tv_fe_type_e.TV_FE_ANALOG, currentChannel.getFrequency(),
                currentChannel.getVideoStd(), currentChannel.getAudioStd(), 0, 0);
        }
    }

     public void setSoundSystem(int mode) {
        if (currentChannel != null) {
            currentChannel.setAudioStd(mode);
            mTvDataBaseManager.updateChannelInfo(currentChannel);
            mTvControlManager.SetFrontendParms(TvControlManager.tv_fe_type_e.TV_FE_ANALOG, currentChannel.getFrequency(),
                currentChannel.getVideoStd(), currentChannel.getAudioStd(), 0, 0);
        }
    }

     public void setVolumeCompensate (int offset) {
        if (currentChannel != null) {
            if ((currentChannel.getAudioCompensation() < 20 && offset > 0)
                || (currentChannel.getAudioCompensation() > -20 && offset < 0)) {
                currentChannel.setAudioCompensation(currentChannel.getAudioCompensation() + offset);
                mTvDataBaseManager.updateChannelInfo(currentChannel);
                mTvControlManager.SetCurProgVolumeCompesition(currentChannel.getAudioCompensation());
            }
        }
     }

    public void setChannelName (int type, int channelNumber, String targetName) {
        ChannelInfo channel = getChannelByNumber(type, channelNumber);
        mTvDataBaseManager.setChannelName(channel, targetName);
    }

    public void swapChannelPosition (int type, int channelNumber, int targetNumber) {
        if (channelNumber == targetNumber)
            return;

        ChannelInfo sourceChannel = getChannelByNumber(type, channelNumber);
        ChannelInfo targetChannel = getChannelByNumber(type, targetNumber);
        mTvDataBaseManager.swapChannel(sourceChannel, targetChannel);
    }

    public void moveChannelPosition (int type, int channelNumber, int targetNumber) {
        if (channelNumber == targetNumber)
            return;

        ArrayList<ChannelInfo> channelList = getChannelInfoList(type);
        mTvDataBaseManager.moveChannel(channelList, channelNumber, targetNumber);
    }

    public void skipChannel (int type, int channelNumber) {
        ChannelInfo channel = getChannelByNumber(type, channelNumber);
        //if (ChannelInfo.isSameChannel(currentChannel, channel))
            //setActivityResult(DroidLogicTvUtils.RESULT_UPDATE);

        mTvDataBaseManager.skipChannel(channel);
    }

    public  void deleteChannel (int type, int channelNumber) {
        ArrayList<ChannelInfo> channelList = getChannelInfoList(type);
        //if (ChannelInfo.isSameChannel(currentChannel,  channelList.get(channelNumber)))
            //setActivityResult(DroidLogicTvUtils.RESULT_UPDATE);

        ChannelInfo channel = channelList.get(channelNumber);
        mTvDataBaseManager.deleteChannel(channel);
    }

    public void setFavouriteChannel (int type, int channelNumber) {
        ChannelInfo channel = getChannelByNumber(type, channelNumber);
        mTvDataBaseManager.setFavouriteChannel(channel);
    }

    public void setSleepTimer (int mins) {
        SystemProperties.set("tv.sleep_timer", mins+"");
        //Intent intent = new Intent(DroidLogicTvUtils.ACTION_TIMEOUT_SUSPEND);
        //mContext.sendBroadcast(intent);//to tvapp
        mContext.startService ( new Intent ( mContext, TimeSuspendService.class ) );
    }

    public void setMenuTime (int seconds) {
        Settings.System.putInt(mContext.getContentResolver(), KEY_MENU_TIME, seconds);
        ((TvSettingsActivity)mContext).startShowActivityTimer();
    }

    public void setStartupSetting (int type) {
        Settings.System.putInt(mContext.getContentResolver(), "tv_start_up_enter_app", type);
    }

    public void setSubtitleSwitch (int switchVal) {
        Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_SUBTITLE_SWITCH, switchVal);
    }

    public void doFactoryReset() {
        setSleepTimer(DEFAULT_SLEEP_TIMER);
        setMenuTime(DEFUALT_MENU_TIME);
        setStartupSetting(0);
        setDefLanguage(0);
        setSubtitleSwitch(0);
       // SystemControlManager mSystemControlManager = new SystemControlManager(mContext);
       // mSystemControlManager.setBootenv("ubootenv.var.upgrade_step", "1");

        for (int i = 0; i < tvPackages.length; i++) {
            ClearPackageData(tvPackages[i]);
        }
        mTvControlManager.stopAutoBacklight();
        mTvControlManager.SSMInitDevice();
        mTvControlManager.FactoryCleanAllTableForProgram();
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

    public void sendBroadcastToTvapp(String extra) {
        Intent intent = new Intent(DroidLogicTvUtils.ACTION_UPDATE_TV_PLAY);
        intent.putExtra("tv_play_extra", extra);
        mContext.sendBroadcast(intent);
    }
}
