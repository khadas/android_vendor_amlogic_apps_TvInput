package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.R.integer;
import android.amlogic.Tv;
import android.amlogic.Tv.FreqList;
import android.app.AlertDialog;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.tvclient.TvClient;
import com.droidlogic.tvinput.R;
import com.droidlogic.utils.tunerinput.tvutil.TVChannelParams;
import com.droidlogic.utils.tunerinput.tvutil.TvContractUtils;
import com.droidlogic.utils.tunerinput.tvutil.TVMultilingualText;
import com.droidlogic.utils.tunerinput.tvutil.MapUtil;
import com.droidlogic.utils.tunerinput.data.ChannelInfo;

public class OptionUiManager implements OnClickListener, OnFocusChangeListener, OnKeyListener, Tv.ScannerEventListener {
    public static final String TAG = "OptionUiManager";

    private TvClient client;
    private Tv tv;

    public static final int OPTION_PICTURE_MODE = 100;
    public static final int OPTION_BRIGHTNESS = 101;
    public static final int OPTION_CONTRAST = 102;
    public static final int OPTION_COLOR = 103;
    public static final int OPTION_SHARPNESS = 104;
    public static final int OPTION_BACKLIGHT = 105;
    public static final int OPTION_COLOR_TEMPERATURE = 106;
    public static final int OPTION_ASPECT_RATIO = 107;
    public static final int OPTION_DNR = 108;
    public static final int OPTION_3D_SETTINGS = 109;

    public static final int OPTION_SOUND_MODE = 200;
    public static final int OPTION_TREBLE = 201;
    public static final int OPTION_BASS = 202;
    public static final int OPTION_BALANCE = 203;
    public static final int OPTION_SPDIF = 204;
    public static final int OPTION_DIALOG_CLARITY = 205;
    public static final int OPTION_BASS_BOOST = 206;
    public static final int OPTION_SURROUND = 207;

    public static final int OPTION_CURRENT_CHANNEL = 300;
    public static final int OPTION_FREQUENCY = 301;
    public static final int OPTION_AUDIO_TRACK = 302;
    public static final int OPTION_SOUND_CHANNEL = 303;
    public static final int OPTION_CHANNEL_INFO = 304;
    public static final int OPTION_COLOR_SYSTEM = 305;
    public static final int OPTION_SOUND_SYSTEM = 306;
    public static final int OPTION_VOLUME_COMPENSATE = 307;
    public static final int OPTION_FINE_TUNE = 308;
    public static final int OPTION_MANUAL_SEARCH = 309;
    public static final int OPTION_AUTO_SEARCH = 310;
    public static final int OPTION_CHANNEL_EDIT = 311;
    public static final int OPTION_SWITCH_CHANNEL = 312;

    public static final int OPTION_SLEEP_TIMER = 400;
    public static final int OPTION_MENU_TIME = 401;
    public static final int OPTION_STARTUP_SETTING = 402;
    public static final int OPTION_DYNAMIC_BACKLIGHT = 403;
    public static final int OPTION_RESTORE_FACTORY = 404;

    public static final int ALPHA_NO_FOCUS = 230;
    public static final int ALPHA_FOCUSED = 255;

    private Context mContext;
    private SettingsManager mSettingsManager;
    private int optionTag = OPTION_PICTURE_MODE;
    private String optionKey = null;
    private int searchedChannelNum = 0;
    private int channelNumber = 0;//for db store TV channel's channelNumber
    private int radioNumber = 0;//for db store Radio channel's channelNumber
    List<ChannelInfo> mChannels = new ArrayList<ChannelInfo>();
    private int finish_result = DroidLogicTvUtils.RESULT_OK;
    private boolean isSearching = false;

    public int getFinishResult() {
        return finish_result;
    }

    public boolean isSearching() {
        return isSearching;
    }

    public OptionUiManager(Context context) {
        mContext = context;
        mSettingsManager = ((TvSettingsActivity) mContext).getSettingsManager();
        client = mSettingsManager.getTvClient();
        tv = mSettingsManager.getTvInstance();
        tv.setScannerListener(this);
    }

    public void setOptionTag(int position) {
        String item_name = ((TvSettingsActivity) mContext).getCurrentFragment().getContentList().get(position).get(ContentFragment.ITEM_NAME)
                .toString();
        // Picture
        if (item_name.equals(mContext.getResources().getString(R.string.picture_mode))) {
            optionTag = OPTION_PICTURE_MODE;
            optionKey = SettingsManager.KEY_PICTURE_MODE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.brightness))) {
            optionTag = OPTION_BRIGHTNESS;
            optionKey = SettingsManager.KEY_BRIGHTNESS;
        } else if (item_name.equals(mContext.getResources().getString(R.string.contrast))) {
            optionTag = OPTION_CONTRAST;
            optionKey = SettingsManager.KEY_CONTRAST;
        } else if (item_name.equals(mContext.getResources().getString(R.string.color))) {
            optionTag = OPTION_COLOR;
            optionKey = SettingsManager.KEY_COLOR;
        } else if (item_name.equals(mContext.getResources().getString(R.string.sharpness))) {
            optionTag = OPTION_SHARPNESS;
            optionKey = SettingsManager.KEY_SHARPNESS;
        } else if (item_name.equals(mContext.getResources().getString(R.string.backlight))) {
            optionTag = OPTION_BACKLIGHT;
            optionKey = SettingsManager.KEY_BACKLIGHT;
        } else if (item_name.equals(mContext.getResources().getString(R.string.color_temperature))) {
            optionTag = OPTION_COLOR_TEMPERATURE;
            optionKey = SettingsManager.KEY_COLOR_TEMPERATURE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.aspect_ratio))) {
            optionTag = OPTION_ASPECT_RATIO;
            optionKey = SettingsManager.KEY_ASPECT_RATIO;
        } else if (item_name.equals(mContext.getResources().getString(R.string.dnr))) {
            optionTag = OPTION_DNR;
            optionKey = SettingsManager.KEY_DNR;
        } else if (item_name.equals(mContext.getResources().getString(R.string.settings_3d))) {
            optionTag = OPTION_3D_SETTINGS;
            optionKey = SettingsManager.KEY_3D_SETTINGS;
        }
        // Sound
        else if (item_name.equals(mContext.getResources().getString(R.string.sound_mode))) {
            optionTag = OPTION_SOUND_MODE;
            optionKey = SettingsManager.KEY_SOUND_MODE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.treble))) {
            optionTag = OPTION_TREBLE;
            optionKey = SettingsManager.KEY_TREBLE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.bass))) {
            optionTag = OPTION_BASS;
            optionKey = SettingsManager.KEY_BASS;
        } else if (item_name.equals(mContext.getResources().getString(R.string.balance))) {
            optionTag = OPTION_BALANCE;
            optionKey = SettingsManager.KEY_BALANCE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.spdif))) {
            optionTag = OPTION_SPDIF;
            optionKey = SettingsManager.KEY_SPDIF;
        } else if (item_name.equals(mContext.getResources().getString(R.string.dialog_clarity))) {
            optionTag = OPTION_DIALOG_CLARITY;
            optionKey = SettingsManager.KEY_DIALOG_CLARITY;
        } else if (item_name.equals(mContext.getResources().getString(R.string.bass_boost))) {
            optionTag = OPTION_BASS_BOOST;
            optionKey = SettingsManager.KEY_BASS_BOOST;
        } else if (item_name.equals(mContext.getResources().getString(R.string.surround))) {
            optionTag = OPTION_SURROUND;
            optionKey = SettingsManager.KEY_SURROUND;
        }
        // Channel
        else if (item_name.equals(mContext.getResources().getString(R.string.current_channel))) {
            optionTag = OPTION_CURRENT_CHANNEL;
            optionKey = SettingsManager.KEY_CURRENT_CHANNEL;
        } else if (item_name.equals(mContext.getResources().getString(R.string.frequency))) {
            optionTag = OPTION_FREQUENCY;
            optionKey = SettingsManager.KEY_FREQUNCY;
        } else if (item_name.equals(mContext.getResources().getString(R.string.audio_track))) {
            optionTag = OPTION_AUDIO_TRACK;
            optionKey = SettingsManager.KEY_AUIDO_TRACK;
        } else if (item_name.equals(mContext.getResources().getString(R.string.sound_channel))) {
            optionTag = OPTION_SOUND_CHANNEL;
            optionKey = SettingsManager.KEY_SOUND_CHANNEL;
        } else if (item_name.equals(mContext.getResources().getString(R.string.channel_info))) {
            optionTag = OPTION_CHANNEL_INFO;
            optionKey = SettingsManager.KEY_CHANNEL_INFO;
        } else if (item_name.equals(mContext.getResources().getString(R.string.color_system))) {
            optionTag = OPTION_COLOR_SYSTEM;
            optionKey = SettingsManager.KEY_COLOR_SYSTEM;
        } else if (item_name.equals(mContext.getResources().getString(R.string.sound_system))) {
            optionTag = OPTION_SOUND_SYSTEM;
            optionKey = SettingsManager.KEY_SOUND_SYSTEM;
        } else if (item_name.equals(mContext.getResources().getString(R.string.volume_compensate))) {
            optionTag = OPTION_VOLUME_COMPENSATE;
            optionKey = SettingsManager.KEY_VOLUME_COMPENSATE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.fine_tune))) {
            optionTag = OPTION_FINE_TUNE;
            optionKey = SettingsManager.KEY_FINE_TUNE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.manual_search))) {
            optionTag = OPTION_MANUAL_SEARCH;
            optionKey = SettingsManager.KEY_MANUAL_SEARCH;
        } else if (item_name.equals(mContext.getResources().getString(R.string.auto_search))) {
            optionTag = OPTION_AUTO_SEARCH;
            optionKey = SettingsManager.KEY_AUTO_SEARCH;
        } else if (item_name.equals(mContext.getResources().getString(R.string.channel_edit))) {
            optionTag = OPTION_CHANNEL_EDIT;
            optionKey = SettingsManager.KEY_CHANNEL_EDIT;
        } else if (item_name.equals(mContext.getResources().getString(R.string.switch_channel))) {
            optionTag = OPTION_SWITCH_CHANNEL;
            optionKey = SettingsManager.KEY_SWITCH_CHANNEL;
        }
        // Settings
        else if (item_name.equals(mContext.getResources().getString(R.string.sleep_timer))) {
            optionTag = OPTION_SLEEP_TIMER;
            optionKey = SettingsManager.KEY_SLEEP_TIMER;
        } else if (item_name.equals(mContext.getResources().getString(R.string.menu_time))) {
            optionTag = OPTION_MENU_TIME;
            optionKey = SettingsManager.KEY_MENU_TIME;
        } else if (item_name.equals(mContext.getResources().getString(R.string.startup_setting))) {
            optionTag = OPTION_STARTUP_SETTING;
            optionKey = SettingsManager.KEY_STARTUP_SETTING;
        } else if (item_name.equals(mContext.getResources().getString(R.string.dynamic_backlight))) {
            optionTag = OPTION_DYNAMIC_BACKLIGHT;
            optionKey = SettingsManager.KEY_DYNAMIC_BACKLIGHT;
        } else if (item_name.equals(mContext.getResources().getString(R.string.restore_factory))) {
            optionTag = OPTION_RESTORE_FACTORY;
            optionKey = SettingsManager.KEY_RESTORE_FACTORY;
        }
    }

    public int getOptionTag() {
        return optionTag;
    }

    public int getLayoutId() {
        switch (optionTag) {
        // picture
            case OPTION_PICTURE_MODE:
                return R.layout.layout_picture_picture_mode;
            case OPTION_BRIGHTNESS:
                return R.layout.layout_picture_brightness;
            case OPTION_CONTRAST:
                return R.layout.layout_picture_contrast;
            case OPTION_COLOR:
                return R.layout.layout_picture_color;
            case OPTION_SHARPNESS:
                return R.layout.layout_picture_sharpness;
            case OPTION_BACKLIGHT:
                return R.layout.layout_picture_backlight;
            case OPTION_COLOR_TEMPERATURE:
                return R.layout.layout_picture_color_temperature;
            case OPTION_ASPECT_RATIO:
                return R.layout.layout_picture_aspect_ratio;
            case OPTION_DNR:
                return R.layout.layout_picture_dnr;
            case OPTION_3D_SETTINGS:
                return R.layout.layout_picture_3d_settings;
                // sound
            case OPTION_SOUND_MODE:
                return R.layout.layout_sound_sound_mode;
            case OPTION_TREBLE:
                return R.layout.layout_sound_treble;
            case OPTION_BASS:
                return R.layout.layout_sound_bass;
            case OPTION_BALANCE:
                return R.layout.layout_sound_balance;
            case OPTION_SPDIF:
                return R.layout.layout_sound_spdif;
            case OPTION_SURROUND:
                return R.layout.layout_sound_surround;
            case OPTION_DIALOG_CLARITY:
                return R.layout.layout_sound_dialog_clarity;
            case OPTION_BASS_BOOST:
                return R.layout.layout_sound_bass_boost;
                // channel
            case OPTION_CHANNEL_INFO:
            case OPTION_AUDIO_TRACK:
            case OPTION_SOUND_CHANNEL:
                return R.layout.layout_option_list;
            case OPTION_COLOR_SYSTEM:
                return R.layout.layout_channel_color_system;
            case OPTION_SOUND_SYSTEM:
                return R.layout.layout_channel_sound_system;
            case OPTION_SWITCH_CHANNEL:
                return R.layout.layout_channel_switch_channel;
            case OPTION_VOLUME_COMPENSATE:
                return R.layout.layout_channel_volume_compensate;
            case OPTION_FINE_TUNE:
                return R.layout.layout_channel_fine_tune;
            case OPTION_MANUAL_SEARCH:
                if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_TV)
                    return R.layout.layout_channel_manual_search_atv;
                else if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_DTV)
                    return R.layout.layout_channel_manual_search_dtv;
            case OPTION_AUTO_SEARCH:
                if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_TV)
                    return R.layout.layout_channel_auto_search_atv;
                else if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_DTV)
                    return R.layout.layout_channel_auto_search_dtv;
            case OPTION_CHANNEL_EDIT:
                return R.layout.layout_channel_channel_edit;
                // settings
            case OPTION_SLEEP_TIMER:
                return R.layout.layout_settings_sleep_timer;
            case OPTION_MENU_TIME:
                return R.layout.layout_settings_menu_time;
            case OPTION_STARTUP_SETTING:
                return R.layout.layout_settings_startup_setting;
            case OPTION_DYNAMIC_BACKLIGHT:
                return R.layout.layout_settings_dynamic_backlight;
            case OPTION_RESTORE_FACTORY:
                return R.layout.layout_settings_restore_factory;
            default:
                break;
        }
        return 0;
    }

    public void setOptionListener(View view) {
        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);
            if (child != null && child.hasFocusable() && child instanceof TextView) {
                //child.setOnClickListener(this);
                child.setOnFocusChangeListener(this);
                child.setOnKeyListener(this);
            }
        }
    }

    @Override
    public void onClick(View view) {
        Resources res = mContext.getResources();
        switch (view.getId()) {
        // ====Picture====
        // picture mode
            case R.id.picture_mode_standard:
                mSettingsManager.setPictureMode(SettingsManager.STATUS_STANDARD);
                break;
            case R.id.picture_mode_vivid:
                mSettingsManager.setPictureMode(SettingsManager.STATUS_VIVID);
                break;
            case R.id.picture_mode_soft:
                mSettingsManager.setPictureMode(SettingsManager.STATUS_SOFT);
                break;
            case R.id.picture_mode_user:
                mSettingsManager.setPictureMode(SettingsManager.STATUS_USER);
                break;
            // brightness
            case R.id.brightness_increase:
                tv.SetBrightness(tv.GetBrightness(client.curSource) + 1, client.curSource, 1);
                mSettingsManager.setBrightness(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.brightness_decrease:
                tv.SetBrightness(tv.GetBrightness(client.curSource) - 1, client.curSource, 1);
                mSettingsManager.setBrightness(SettingsManager.PERCENT_DECREASE);
                break;
            // contrast
            case R.id.contrast_increase:
                tv.SetContrast(tv.GetContrast(client.curSource) + 1, client.curSource, 1);
                mSettingsManager.setContrast(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.contrast_decrease:
                tv.SetContrast(tv.GetContrast(client.curSource) - 1, client.curSource, 1);
                mSettingsManager.setContrast(SettingsManager.PERCENT_DECREASE);
                break;
            // color
            case R.id.color_increase:
                tv.SetSaturation(tv.GetSaturation(client.curSource) + 1, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
                mSettingsManager.setColor(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.color_decrease:
                tv.SetSaturation(tv.GetSaturation(client.curSource) - 1, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
                mSettingsManager.setColor(SettingsManager.PERCENT_DECREASE);
                break;
            // sharpness
            case R.id.sharpness_increase:
                tv.SetSharpness(tv.GetSharpness(client.curSource) + 1, client.curSource, 1, 0, 1);
                mSettingsManager.setSharpness(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.sharpness_decrease:
                tv.SetSharpness(tv.GetSharpness(client.curSource) - 1, client.curSource, 1, 0, 1);
                mSettingsManager.setSharpness(SettingsManager.PERCENT_DECREASE);
                break;
            // backlight
            case R.id.backlight_increase:
                tv.SetBacklight(tv.GetBacklight(client.curSource) + 1, client.curSource, 1);
                mSettingsManager.setBacklight(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.backlight_decrease:
                tv.SetBacklight(tv.GetBacklight(client.curSource) - 1, client.curSource, 1);
                mSettingsManager.setBacklight(SettingsManager.PERCENT_DECREASE);
                break;
            // color temperature
            case R.id.color_temperature_standard:
                tv.SetColorTemperature(Tv.color_temperature.COLOR_TEMP_STANDARD, client.curSource, 1);
                mSettingsManager.setColorTemperature(SettingsManager.STATUS_STANDARD);
                break;
            case R.id.color_temperature_warm:
                tv.SetColorTemperature(Tv.color_temperature.COLOR_TEMP_WARM, client.curSource, 1);
                mSettingsManager.setColorTemperature(SettingsManager.STATUS_WARM);
                break;
            case R.id.color_temperature_cool:
                tv.SetColorTemperature(Tv.color_temperature.COLOR_TEMP_COLD, client.curSource, 1);
                mSettingsManager.setColorTemperature(SettingsManager.STATUS_COOL);
                break;
            // aspect ratio
            case R.id.apect_ratio_auto:
                mSettingsManager.setAspectRatio(SettingsManager.STATUS_AUTO);
                break;
            case R.id.apect_ratio_four2three:
                mSettingsManager.setAspectRatio(SettingsManager.STATUS_4_TO_3);
                break;
            case R.id.apect_ratio_panorama:
                mSettingsManager.setAspectRatio(SettingsManager.STATUS_PANORAMA);
                break;
            case R.id.apect_ratio_full_screen:
                mSettingsManager.setAspectRatio(SettingsManager.STATUS_FULL_SCREEN);
                break;
            // dnr
            case R.id.dnr_off:
                tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCE_NOISE_CLOSE, client.curSource, 1);
                mSettingsManager.setDnr(SettingsManager.STATUS_OFF);
                break;
            case R.id.dnr_auto:
                tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCTION_MODE_AUTO, client.curSource, 1);
                mSettingsManager.setDnr(SettingsManager.STATUS_AUTO);
                break;
            case R.id.dnr_medium:
                tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCE_NOISE_MID, client.curSource, 1);
                mSettingsManager.setDnr(SettingsManager.STATUS_MEDIUM);
                break;
            case R.id.dnr_high:
                tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCE_NOISE_STRONG, client.curSource, 1);
                mSettingsManager.setDnr(SettingsManager.STATUS_HIGH);
                break;
            case R.id.dnr_low:
                tv.SetNoiseReductionMode(Tv.Noise_Reduction_Mode.REDUCE_NOISE_WEAK, client.curSource, 1);
                mSettingsManager.setDnr(SettingsManager.STATUS_LOW);
                break;
            // 3d settings
            case R.id.settings_3d_off:
                tv.Set3DMode(Tv.Mode_3D.MODE_3D_CLOSE, Tv.Tvin_3d_Status.STATUS3D_DISABLE);
                mSettingsManager.set3dSettings(SettingsManager.STATUS_OFF);
                break;
            case R.id.settings_3d_auto:
                tv.Set3DMode(Tv.Mode_3D.MODE_3D_AUTO, Tv.Tvin_3d_Status.STATUS3D_AUTO);
                mSettingsManager.set3dSettings(SettingsManager.STATUS_AUTO);
                break;
            case R.id.settings_3d_lr_mode:
                tv.Set3DLRSwith(0, Tv.Tvin_3d_Status.STATUS3D_LR);
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_LR_MODE);
                break;
            case R.id.settings_3d_rl_mode:
                tv.Set3DLRSwith(1, Tv.Tvin_3d_Status.STATUS3D_LR);
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_RL_MODE);
                break;
            case R.id.settings_3d_ud_mode:
                tv.Set3DLRSwith(0, Tv.Tvin_3d_Status.STATUS3D_BT);
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_UD_MODE);
                break;
            case R.id.settings_3d_du_mode:
                tv.Set3DLRSwith(1, Tv.Tvin_3d_Status.STATUS3D_BT);
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_DU_MODE);
                break;
            case R.id.settings_3d_3d_to_2d:
                // tv.Set3DTo2DMode(Tv.Mode_3D_2D.values()[position], Tv.Tvin_3d_Status.values()[tv.Get3DMode()]);
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_TO_2D);
                break;
            // ====Sound====
            // sound mode
            case R.id.sound_mode_standard:
                mSettingsManager.setSoundMode(SettingsManager.STATUS_STANDARD);
                break;
            case R.id.sound_mode_music:
                mSettingsManager.setSoundMode(SettingsManager.STATUS_MUSIC);
                break;
            case R.id.sound_mode_news:
                mSettingsManager.setSoundMode(SettingsManager.STATUS_NEWS);
                break;
            case R.id.sound_mode_movie:
                mSettingsManager.setSoundMode(SettingsManager.STATUS_MOVIE);
                break;
            case R.id.sound_mode_user:
                mSettingsManager.setSoundMode(SettingsManager.STATUS_USER);
                break;
            // Treble
            case R.id.treble_increase:
                int treble_value_increase = tv.GetCurAudioTrebleVolume() + 1;
                tv.SetAudioTrebleVolume(treble_value_increase);
                tv.SaveCurAudioTrebleVolume(treble_value_increase);
                break;
            case R.id.treble_decrease:
                int treble_value_decrease = tv.GetCurAudioTrebleVolume() - 1;
                tv.SetAudioTrebleVolume(treble_value_decrease);
                tv.SaveCurAudioTrebleVolume(treble_value_decrease);
                break;
            // Bass
            case R.id.bass_increase:
                int bass_value_increase = tv.GetCurAudioBassVolume() + 1;
                tv.SetAudioBassVolume(bass_value_increase);
                tv.SaveCurAudioBassVolume(bass_value_increase);
                break;
            case R.id.bass_decrease:
                int bass_value_decrease = tv.GetCurAudioBassVolume() - 1;
                tv.SetAudioBassVolume(bass_value_decrease);
                tv.SaveCurAudioBassVolume(bass_value_decrease);
                break;
            // Balance
            case R.id.balance_increase:
                int balance_value_increase = tv.GetCurAudioBalance() + 1;
                tv.SetAudioBalance(balance_value_increase);
                tv.SaveCurAudioBalance(balance_value_increase);
                break;
            case R.id.balance_decrease:
                int balance_value_decrease = tv.GetCurAudioBalance() - 1;
                tv.SetAudioBalance(balance_value_decrease);
                tv.SaveCurAudioBalance(balance_value_decrease);
                break;
            // SPDIF
            case R.id.spdif_off:
                tv.SetAudioSPDIFSwitch(0);
                tv.SaveCurAudioSPDIFSwitch(0);
                break;
            case R.id.spdif_auto:
                tv.SetAudioSPDIFMode(0);
                tv.SaveCurAudioSPDIFMode(0);
                break;
            case R.id.spdif_pcm:
                tv.SetAudioSPDIFMode(1);
                tv.SaveCurAudioSPDIFMode(1);
                break;
            // Surround
            case R.id.surround_on:
                tv.SetAudioSrsSurround(1);
                tv.SaveCurAudioSrsSurround(1);
                break;
            case R.id.surround_off:
                tv.SetAudioSrsSurround(0);
                tv.SaveCurAudioSrsSurround(0);
                break;
            // Dialog Clarity
            case R.id.dialog_clarity_on:
                tv.SetAudioSrsDialogClarity(1);
                tv.SaveCurAudioSrsDialogClarity(1);
                break;
            case R.id.dialog_clarity_off:
                tv.SetAudioSrsDialogClarity(0);
                tv.SaveCurAudioSrsDialogClarity(0);
                break;
            // Bass Boost
            case R.id.bass_boost_on:
                tv.SetAudioSrsTruBass(1);
                tv.SaveCurAudioSrsTruBass(1);
                break;
            case R.id.bass_boost_off:
                tv.SetAudioSrsTruBass(0);
                tv.SaveCurAudioSrsTruBass(0);
                break;
            // ====Channel====
            // color system
            case R.id.color_system_auto:
                client.curChannel.videoStd = Tv.tvin_color_system_e.COLOR_SYSTEM_AUTO.toInt();
                TvContractUtils.updateChannelInfo(mContext, client.curChannel);
                tv.SetFrontendParms(Tv.tv_fe_type_e.TV_FE_ANALOG, client.curChannel.frequency, client.curChannel.videoStd, client.curChannel.audioStd, 0, 0);
                break;
            case R.id.color_system_pal:
                client.curChannel.videoStd = Tv.tvin_color_system_e.COLOR_SYSTEM_PAL.toInt();
                TvContractUtils.updateChannelInfo(mContext, client.curChannel);
                tv.SetFrontendParms(Tv.tv_fe_type_e.TV_FE_ANALOG, client.curChannel.frequency, client.curChannel.videoStd, client.curChannel.audioStd, 0, 0);
                break;
            case R.id.color_system_ntsc:
                client.curChannel.videoStd = Tv.tvin_color_system_e.COLOR_SYSTEM_NTSC.toInt();
                TvContractUtils.updateChannelInfo(mContext, client.curChannel);
                tv.SetFrontendParms(Tv.tv_fe_type_e.TV_FE_ANALOG, client.curChannel.frequency, client.curChannel.videoStd, client.curChannel.audioStd, 0, 0);
                break;
            // sound system
            case R.id.sound_system_dk:
                client.curChannel.audioStd = Tv.atv_audio_std_e.ATV_AUDIO_STD_DK.toInt();
                TvContractUtils.updateChannelInfo(mContext, client.curChannel);
                tv.SetFrontendParms(Tv.tv_fe_type_e.TV_FE_ANALOG, client.curChannel.frequency, client.curChannel.videoStd, client.curChannel.audioStd, 0, 0);
                break;
            case R.id.sound_system_i:
                client.curChannel.audioStd = Tv.atv_audio_std_e.ATV_AUDIO_STD_I.toInt();
                TvContractUtils.updateChannelInfo(mContext, client.curChannel);
                tv.SetFrontendParms(Tv.tv_fe_type_e.TV_FE_ANALOG, client.curChannel.frequency, client.curChannel.videoStd, client.curChannel.audioStd, 0, 0);
                break;
            case R.id.sound_system_bg:
                client.curChannel.audioStd = Tv.atv_audio_std_e.ATV_AUDIO_STD_BG.toInt();
                TvContractUtils.updateChannelInfo(mContext, client.curChannel);
                tv.SetFrontendParms(Tv.tv_fe_type_e.TV_FE_ANALOG, client.curChannel.frequency, client.curChannel.videoStd, client.curChannel.audioStd, 0, 0);
                break;
            case R.id.sound_system_m:
                client.curChannel.audioStd = Tv.atv_audio_std_e.ATV_AUDIO_STD_M.toInt();
                TvContractUtils.updateChannelInfo(mContext, client.curChannel);
                tv.SetFrontendParms(Tv.tv_fe_type_e.TV_FE_ANALOG, client.curChannel.frequency, client.curChannel.videoStd, client.curChannel.audioStd, 0, 0);
                break;
            // volume compensate
            case R.id.volume_compensate_increase:
                break;
            case R.id.volume_compensate_decrease:
                break;
            // fine tune
            case R.id.fine_tune_increase:
                break;
            case R.id.fine_tune_decrease:
                break;
            // manual search
            case R.id.manual_search_start:
            case R.id.manual_search_start_dtv:
                startManualSearch();
                isSearching = true;
                searchedChannelNum = 0;
                finish_result = DroidLogicTvUtils.RESULT_UPDATE;
                break;
            // auto search
            case R.id.auto_search_start_atv:
            case R.id.auto_search_start_dtv:
                TvContractUtils.deleteChannels(mContext, mSettingsManager.getInputId());
                if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_TV)
                    tv.AtvAutoScan(Tv.atv_video_std_e.ATV_VIDEO_STD_PAL, Tv.atv_audio_std_e.ATV_AUDIO_STD_I, 0);
                else if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_DTV)
                    tv.DtvAutoScan();
                isSearching = true;
                searchedChannelNum = 0;
                finish_result = DroidLogicTvUtils.RESULT_UPDATE;
                break;
            // ====Settings====
            // Sleep Timer
            case R.id.sleep_timer_off:
                mSettingsManager.setSleepTime(0);
                break;
            case R.id.sleep_timer_15min:
                mSettingsManager.setSleepTime(15);
                break;
            case R.id.sleep_timer_30min:
                mSettingsManager.setSleepTime(30);
                break;
            case R.id.sleep_timer_45min:
                mSettingsManager.setSleepTime(45);
                break;
            case R.id.sleep_timer_60min:
                mSettingsManager.setSleepTime(60);
                break;
            case R.id.sleep_timer_90min:
                mSettingsManager.setSleepTime(90);
                break;
            case R.id.sleep_timer_120min:
                mSettingsManager.setSleepTime(120);
                break;
            //menu time
            case R.id.menu_time_10s:
                mSettingsManager.setMenuTime(10);
                break;
            case R.id.menu_time_20s:
                mSettingsManager.setMenuTime(20);
                break;
            case R.id.menu_time_40s:
                mSettingsManager.setMenuTime(40);
                break;
            case R.id.menu_time_60s:
                mSettingsManager.setMenuTime(60);
                break;
            // Dynamic Backlight
            case R.id.dynamic_backlight_on:
                tv.startAutoBacklight();
                break;
            case R.id.dynamic_backlight_off:
                tv.stopAutoBacklight();
                break;
            // Switch Channel
            case R.id.switch_channel_static_frame:
                tv.setBlackoutEnable(0);
                break;
            case R.id.switch_channel_black_frame:
                tv.setBlackoutEnable(1);
                break;
            // Restore Factory Settings
            case R.id.restore_factory:
                new AlertDialog.Builder(mContext).setTitle(R.string.warning).setMessage(R.string.prompt_def_set)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                tv.stopAutoBacklight();
                                SystemProperties.set("tv.sleep_timer", "0");
                                tv.SSMInitDevice();
                                tv.FactoryCleanAllTableForProgram();
                                ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE)).reboot(null);
                            }
                        }).setNegativeButton(R.string.cancel, null).show();
                break;
            default:
                break;
        }
        setProgressStatus();
        ((TvSettingsActivity) mContext).getCurrentFragment().refreshList();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v instanceof TextView) {
            if (hasFocus) {
                ((TextView) v).setTextColor(mContext.getResources().getColor(R.color.color_text_focused));
            } else
                ((TextView) v).setTextColor(mContext.getResources().getColor(R.color.color_text_item));
        }
    }


    @Override
    public  boolean onKey (View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    onClick(v);
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
        }
        return false;
    }


    public void setProgressStatus() {
        int progress;

        switch (optionTag) {
            case OPTION_FINE_TUNE:
                progress = mSettingsManager.getFineTuneProgress();
                setProgress(progress);
                setFineTuneFrequency(progress);
                break;
            case OPTION_MANUAL_SEARCH:
                progress = mSettingsManager.getManualSearchProgress();
                setProgress(progress);
                setManualSearchInfo(null);
                break;
            case OPTION_AUTO_SEARCH:
                setProgress(0);
                break;
            default:
                progress = getIntegerFromString(mSettingsManager.getStatus(optionKey));
                if (progress >= 0)
                    setProgress(progress);
                break;
        }
    }

    public void setProgress(int progress) {
        RelativeLayout progressLayout = null;
        ViewGroup optionView = (ViewGroup) ((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);

        for (int i = 0; i < optionView.getChildCount(); i++) {
            View view = optionView.getChildAt(i);
            if (view instanceof RelativeLayout) {
                progressLayout = (RelativeLayout) view;
                break;
            }
        }

        if (progressLayout != null) {
            for (int i = 0; i < progressLayout.getChildCount(); i++) {
                View child = progressLayout.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setText(Integer.toString(progress) + "%");
                } else if (child instanceof ImageView) {
                    ((ImageView) child).setImageResource(getProgressResourceId(progress));
                }
            }
        }
    }

    public int getProgressResourceId(int progress) {
        if (progress == 0)
            return R.drawable.progress_0;

        switch (progress / 5) {
            case 0:
                return R.drawable.progress_1;
            case 1:
                return R.drawable.progress_2;
            case 2:
                return R.drawable.progress_3;
            case 3:
                return R.drawable.progress_4;
            case 4:
                return R.drawable.progress_5;
            case 5:
                return R.drawable.progress_6;
            case 6:
                return R.drawable.progress_7;
            case 7:
                return R.drawable.progress_8;
            case 8:
                return R.drawable.progress_9;
            case 9:
                return R.drawable.progress_10;
            case 10:
                return R.drawable.progress_11;
            case 11:
                return R.drawable.progress_12;
            case 12:
                return R.drawable.progress_13;
            case 13:
                return R.drawable.progress_14;
            case 14:
                return R.drawable.progress_15;
            case 15:
                return R.drawable.progress_16;
            case 16:
                return R.drawable.progress_17;
            case 17:
                return R.drawable.progress_18;
            case 18:
                return R.drawable.progress_19;
            case 19:
                return R.drawable.progress_20;
            case 20:
                return R.drawable.progress_21;
            default:
                break;
        }
        return R.drawable.progress_10;
    }

    private void setFineTuneFrequency(int progress) {
        ViewGroup optionView = (ViewGroup) ((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);

        TextView frequency = (TextView) optionView.findViewById(R.id.fine_tune_frequency);
        TextView frequency_band = (TextView) optionView.findViewById(R.id.fine_tune_frequency_band);

        if (frequency != null && frequency_band != null) {
            frequency.setText("535.25MHZ");
            frequency_band.setText("UHF");
        }
    }

    private void startManualSearch() {
        ViewGroup parent = (ViewGroup) ((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);
        if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_TV) {
            EditText begin = (EditText) parent.findViewById(R.id.manual_search_edit_from);
            EditText end = (EditText) parent.findViewById(R.id.manual_search_edit_to);

            String beginHZ = begin.getText().toString();
            if (beginHZ == null || beginHZ.length() == 0)
                beginHZ = (String) begin.getHint();

            String endHZ = end.getText().toString();
            if (endHZ == null || endHZ.length() == 0)
                endHZ = (String) end.getHint();

            mSettingsManager.setManualSearchProgress(0);
            mSettingsManager.setManualSearchSearchedNumber(0);

            tv.AtvManualScan(Integer.valueOf(beginHZ) * 1000, Integer.valueOf(endHZ) * 1000,
                Tv.atv_video_std_e.ATV_VIDEO_STD_PAL, Tv.atv_audio_std_e.ATV_AUDIO_STD_DK);
        } else if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_DTV) {
            EditText edit = (EditText) parent.findViewById(R.id.manual_search_dtv_channel);
            String channel = edit.getText().toString();
            if (channel == null || channel.length() == 0)
                channel = (String)edit.getHint();
            tv.DtvManualScan(getDvbFrequencyByPd(Integer.valueOf(channel)));
        }
    }

    public void setManualSearchEditStyle(View view) {
        if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_TV) {
            EditText edit_from = (EditText) view.findViewById(R.id.manual_search_edit_from);
            EditText edit_to = (EditText) view.findViewById(R.id.manual_search_edit_to);
            edit_from.setNextFocusLeftId(R.id.content_list);
            edit_from.setNextFocusRightId(edit_from.getId());
            edit_from.setNextFocusUpId(edit_from.getId());
            edit_to.setNextFocusLeftId(R.id.content_list);
            edit_to.setNextFocusRightId(edit_to.getId());

            TextWatcher textWatcher = new TextWatcher() {
                private Toast toast = null;

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int pos = s.length() - 1;
                        char c = s.charAt(pos);
                        if (c < '0' || c > '9') {
                            s.delete(pos, pos + 1);
                            if (toast == null)
                                toast = Toast.makeText(mContext, mContext.getResources().getString(R.string.error_not_number), Toast.LENGTH_SHORT);

                            toast.show();
                        }
                    }
                }
            };
            edit_from.addTextChangedListener(textWatcher);
            edit_to.addTextChangedListener(textWatcher);
        } else if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_DTV) {
            final EditText edit = (EditText) view.findViewById(R.id.manual_search_dtv_channel);
            edit.setNextFocusLeftId(R.id.content_list);
            edit.setNextFocusRightId(edit.getId());
            edit.setNextFocusUpId(edit.getId());
            final TextView start_frequency = (TextView)view.findViewById(R.id.manual_search_dtv_start_frequency);

            TextWatcher textWatcher = new TextWatcher() {
                private Toast toast = null;

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int pos = s.length() - 1;
                        char c = s.charAt(pos);
                        if (c < '0' || c > '9') {
                            s.delete(pos, pos + 1);
                            if (toast == null)
                                toast = Toast.makeText(mContext, mContext.getResources().getString(R.string.error_not_number), Toast.LENGTH_SHORT);

                            toast.show();
                        } else{
                            String number = edit.getText().toString();
                            if (number == null || number.length() == 0)
                                number = (String)edit.getHint();
                            start_frequency.setText(parseChannelFrequency(getDvbFrequencyByPd(Integer.valueOf(number))));
                        }
                    }
                }
            };
            edit.addTextChangedListener(textWatcher);
        }
    }

    private String parseChannelFrequency(double freq) {
        String frequency = mContext.getResources().getString(R.string.start_frequency);
        frequency += Double.toString(freq / (1000 * 1000)) + mContext.getResources().getString(R.string.mhz);
        return frequency;
    }

    private int getDvbFrequencyByPd(int pd_number) {// KHz
        int the_freq = 706000000;
        ArrayList<FreqList> m_fList = tv.DTVGetScanFreqList();
        int size = m_fList.size();
        for (int i = 0; i < size; i++) {
            if (pd_number == m_fList.get(i).ID) {
                the_freq = m_fList.get(i).freq;
                break;
            }
        }
        Log.d("fuhao", "the_freq = " + the_freq);
        return the_freq;
    }

    private void setManualSearchInfo(Tv.ScannerEvent event) {
        ViewGroup optionView = (ViewGroup)((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);
        if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_TV) {
            EditText begin = (EditText)optionView.findViewById(R.id.manual_search_edit_from);
            TextView frequency = (TextView)optionView.findViewById(R.id.manual_search_frequency);
            TextView frequency_band = (TextView)optionView.findViewById(R.id.manual_search_frequency_band);
            TextView searched_number = (TextView)optionView.findViewById(R.id.manual_search_searched_number);
            double freq = 0.0;

            if (event == null) {
                freq = Double.valueOf((String)begin.getHint()).doubleValue();
                freq /= 1000;// KHZ to MHZ
            } else {
                freq = event.freq;
                freq /= 1000 * 1000;// HZ to MHZ
            }

            if (frequency != null && frequency_band != null && searched_number != null) {
                frequency.setText(Double.toString(freq) + mContext.getResources().getString(R.string.mhz));
                frequency_band.setText(parseFrequencyBand(freq));
                searched_number.setText(mContext.getResources().getString(R.string.searched_number) + ": " + channelNumber);
            }
        } else if (event != null && client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_DTV) {
            OptionListView listView = (OptionListView)optionView.findViewById(R.id.manual_search_dtv_info);
             ArrayList<HashMap<String,Object>> dataList = getSearchedDtvInfo(event);
             SimpleAdapter adapter = new SimpleAdapter(mContext, dataList,
                    R.layout.layout_option_double_text,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
             listView.setAdapter(adapter);
        }
    }

    public ArrayList<HashMap<String,Object>> getSearchedDtvInfo (Tv.ScannerEvent event) {
        ArrayList<HashMap<String,Object>> list =  new ArrayList<HashMap<String,Object>>();

        HashMap<String,Object> item = new HashMap<String,Object>();
        item.put(SettingsManager.STRING_NAME, mContext.getResources().getString(R.string.frequency_l) + ":");
        item.put(SettingsManager.STRING_STATUS, Double.toString(event.freq/(1000 * 1000)) +
            mContext.getResources().getString(R.string.mhz));
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(SettingsManager.STRING_NAME, mContext.getResources().getString(R.string.quality) + ":");
        item.put(SettingsManager.STRING_STATUS, event.quality + mContext.getResources().getString(R.string.db));
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(SettingsManager.STRING_NAME, mContext.getResources().getString(R.string.strength) + ":");
        item.put(SettingsManager.STRING_STATUS, "%" + event.strength);
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(SettingsManager.STRING_NAME, mContext.getResources().getString(R.string.tv_channel) + ":");
        item.put(SettingsManager.STRING_STATUS, channelNumber);
        list.add(item);

        item = new HashMap<String,Object>();
        item.put(SettingsManager.STRING_NAME, mContext.getResources().getString(R.string.radio_channel) + ":");
        item.put(SettingsManager.STRING_STATUS, radioNumber);
        list.add(item);

        return list;
    }

    private void setAutoSearchFrequency(Tv.ScannerEvent event) {
        ViewGroup optionView = (ViewGroup) ((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);
        if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_TV) {
            TextView frequency = (TextView) optionView.findViewById(R.id.auto_search_frequency_atv);
            TextView frequency_band = (TextView) optionView.findViewById(R.id.auto_search_frequency_band_atv);
            TextView searched_number = (TextView) optionView.findViewById(R.id.auto_search_searched_number_atv);
            if (frequency != null && frequency_band != null && searched_number != null) {
                double freq = event.freq/(1000 * 1000);
                frequency.setText(Double.toString(freq) + mContext.getResources().getString(R.string.mhz));
                frequency_band.setText(parseFrequencyBand(freq));
                searched_number.setText(mContext.getResources().getString(R.string.searched_number) + ": " + channelNumber);
            }
        } else if (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_DTV) {
            OptionListView listView = (OptionListView)optionView.findViewById(R.id.auto_search_dtv_info);
             ArrayList<HashMap<String,Object>> dataList = getSearchedDtvInfo(event);
             SimpleAdapter adapter = new SimpleAdapter(mContext, dataList,
                    R.layout.layout_option_double_text,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
             listView.setAdapter(adapter);
        }
    }

    private String parseFrequencyBand(double freq) {
        String band = "";
        if (freq > 44.25 && freq < 143.25) {
            band = "VHFL";
        } else if (freq >= 143.25 && freq < 426.25) {
            band = "VHFH";
        } else if (freq >= 426.25 && freq < 868.25) {
            band = "UHF";
        }
        return band;
    }

    private int getIntegerFromString(String str) {
        if (str != null && str.contains("%"))
            return Integer.valueOf(str.replaceAll("%", ""));
        else
            return -1;
    }

    @Override
    public void onEvent(Tv.ScannerEvent event) {
        Log.d(TAG, "searching---precent = " + event.precent);
        ChannelInfo channel = null;
        String name = null;
        if (event.lock == 1) {
            // get a channel
            searchedChannelNum++;
        }
        switch (event.type) {
            case Tv.EVENT_DTV_PROG_DATA:
                try {
                    String composedName = new String(event.programName);
                    name = TVMultilingualText.getText(composedName);
                    if (name == null || name.isEmpty()) {
                        name = TVMultilingualText.getText(composedName, "first");
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    name = "????";
                }

                channel = new ChannelInfo(String.valueOf(channelNumber), name, null, event.orig_net_id, event.ts_id, mSettingsManager.getInputId(), event.serviceID, 0, 0,
                        event.mode, event.srvType, event.freq, event.bandwidth, event.vid, event.vfmt, event.aids, event.afmts, event.alangs, event.pcr, 0, 0, 0, 0);

                if (optionTag == OPTION_MANUAL_SEARCH)
                    TvContractUtils.updateOrinsertDtvChannel(mContext, channel);
                else {
                    if (event.srvType == 1) {
                        TvContractUtils.insertDtvChannel(mContext, channel, channelNumber);
                        channelNumber++;
                    }
                    else {
                        TvContractUtils.insertDtvChannel(mContext, channel, radioNumber);
                        radioNumber++;
                    }
                }
                Log.d(TAG, "STORE_SERVICE: " + channel.toString());
                break;
            case Tv.EVENT_SCAN_PROGRESS:
                if (event.programName != null) {
                    try {
                        String composedName = new String(event.programName);
                        name = TVMultilingualText.getText(composedName);
                        if (name == null || name.isEmpty()) {
                            name = TVMultilingualText.getText(composedName, "first");
                        }
                        Log.d(TAG, "New Program : " + name + ", type " + event.srvType);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                setProgress(event.precent);
                if (optionTag == OPTION_MANUAL_SEARCH)
                    setManualSearchInfo(event);
                else
                    setAutoSearchFrequency(event);
                break;
            case Tv.EVENT_ATV_PROG_DATA:
                channelNumber++;
                channel = new ChannelInfo("A " + String.valueOf(searchedChannelNum), event.programName, null, 0, 0, mSettingsManager.getInputId(), 0, 0, 0, 3,
                        event.srvType, event.freq, 0,// bandwidth
                        0,// videoPID
                        0,// videoFormat,
                        null,// audioPIDs[],
                        null,// audioFormats[],
                        null,// audioLangs[],
                        0,// pcrPID,
                        event.videoStd, event.audioStd, event.isAutoStd, 0);
                if (optionTag == OPTION_MANUAL_SEARCH)
                    TvContractUtils.updateOrinsertAtvChannel(mContext, channel);
                else
                    TvContractUtils.insertAtvChannel(mContext, channel, channelNumber);
                break;
            case Tv.EVENT_STORE_END:
                Log.d(TAG, "Store end");
                ((TvSettingsActivity) mContext).finish();
                break;
            case Tv.EVENT_SCAN_END:
                Log.d(TAG, "Scan end");
                tv.DtvStopScan();
                break;
            case Tv.EVENT_SCAN_EXIT:
                Log.d(TAG, "Scan exit.");
                isSearching = false;
                if (searchedChannelNum == 0) {
                    ((TvSettingsActivity) mContext).finish();
                }
                break;
            default:
                break;
        }
    }

    public void stopAllAction(){
    }
}
