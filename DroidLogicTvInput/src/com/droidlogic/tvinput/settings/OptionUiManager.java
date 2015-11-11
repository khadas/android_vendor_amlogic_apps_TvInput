package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvControlManager.FreqList;
import android.app.AlertDialog;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TVMultilingualText;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.tvinput.R;

public class OptionUiManager implements OnClickListener, OnFocusChangeListener, OnKeyListener, TvControlManager.ScannerEventListener {
    public static final String TAG = "OptionUiManager";

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
    private TvControlManager mTvControlManager;
    private TvDataBaseManager mTvDataBaseManager;
    private int optionTag = OPTION_PICTURE_MODE;
    private String optionKey = null;
    private int channelNumber = 0;//for setting show searched tv channelNumber
    private int radioNumber = 0;//for setting show searched radio channelNumber
    private int tvDisplayNumber = 0;//for db store TV channel's channel displayNumber
    private int radioDisplayNumber = 0;//for db store Radio channel's channel displayNumber
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
        mTvControlManager = mSettingsManager.getTvControlManager();
        mTvDataBaseManager = mSettingsManager.getTvDataBaseManager();
        mTvControlManager.setScannerListener(this);
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
                if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV)
                    return R.layout.layout_channel_manual_search_atv;
                else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV)
                    return R.layout.layout_channel_manual_search_dtv;
            case OPTION_AUTO_SEARCH:
                if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV)
                    return R.layout.layout_channel_auto_search_atv;
                else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV)
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
                mSettingsManager.setBrightness(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.brightness_decrease:
                mSettingsManager.setBrightness(SettingsManager.PERCENT_DECREASE);
                break;
            // contrast
            case R.id.contrast_increase:
                mSettingsManager.setContrast(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.contrast_decrease:
                mSettingsManager.setContrast(SettingsManager.PERCENT_DECREASE);
                break;
            // color
            case R.id.color_increase:
                mSettingsManager.setColor(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.color_decrease:
                mSettingsManager.setColor(SettingsManager.PERCENT_DECREASE);
                break;
            // sharpness
            case R.id.sharpness_increase:
                mSettingsManager.setSharpness(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.sharpness_decrease:
                mSettingsManager.setSharpness(SettingsManager.PERCENT_DECREASE);
                break;
            // backlight
            case R.id.backlight_increase:
                mSettingsManager.setBacklight(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.backlight_decrease:
                mSettingsManager.setBacklight(SettingsManager.PERCENT_DECREASE);
                break;
            // color temperature
            case R.id.color_temperature_standard:
                mSettingsManager.setColorTemperature(SettingsManager.STATUS_STANDARD);
                break;
            case R.id.color_temperature_warm:
                mSettingsManager.setColorTemperature(SettingsManager.STATUS_WARM);
                break;
            case R.id.color_temperature_cool:
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
                mSettingsManager.setDnr(SettingsManager.STATUS_OFF);
                break;
            case R.id.dnr_auto:
                mSettingsManager.setDnr(SettingsManager.STATUS_AUTO);
                break;
            case R.id.dnr_medium:
                mSettingsManager.setDnr(SettingsManager.STATUS_MEDIUM);
                break;
            case R.id.dnr_high:
                mSettingsManager.setDnr(SettingsManager.STATUS_HIGH);
                break;
            case R.id.dnr_low:
                mSettingsManager.setDnr(SettingsManager.STATUS_LOW);
                break;
            // 3d settings
            case R.id.settings_3d_off:
                mSettingsManager.set3dSettings(SettingsManager.STATUS_OFF);
                break;
            case R.id.settings_3d_auto:
                mSettingsManager.set3dSettings(SettingsManager.STATUS_AUTO);
                break;
            case R.id.settings_3d_lr_mode:
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_LR_MODE);
                break;
            case R.id.settings_3d_rl_mode:
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_RL_MODE);
                break;
            case R.id.settings_3d_ud_mode:
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_UD_MODE);
                break;
            case R.id.settings_3d_du_mode:
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_DU_MODE);
                break;
            case R.id.settings_3d_3d_to_2d:
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
                int treble_value_increase = mTvControlManager.GetCurAudioTrebleVolume() + 1;
                mTvControlManager.SetAudioTrebleVolume(treble_value_increase);
                mTvControlManager.SaveCurAudioTrebleVolume(treble_value_increase);
                break;
            case R.id.treble_decrease:
                int treble_value_decrease = mTvControlManager.GetCurAudioTrebleVolume() - 1;
                mTvControlManager.SetAudioTrebleVolume(treble_value_decrease);
                mTvControlManager.SaveCurAudioTrebleVolume(treble_value_decrease);
                break;
            // Bass
            case R.id.bass_increase:
                int bass_value_increase = mTvControlManager.GetCurAudioBassVolume() + 1;
                mTvControlManager.SetAudioBassVolume(bass_value_increase);
                mTvControlManager.SaveCurAudioBassVolume(bass_value_increase);
                break;
            case R.id.bass_decrease:
                int bass_value_decrease = mTvControlManager.GetCurAudioBassVolume() - 1;
                mTvControlManager.SetAudioBassVolume(bass_value_decrease);
                mTvControlManager.SaveCurAudioBassVolume(bass_value_decrease);
                break;
            // Balance
            case R.id.balance_increase:
                int balance_value_increase = mTvControlManager.GetCurAudioBalance() + 1;
                mTvControlManager.SetAudioBalance(balance_value_increase);
                mTvControlManager.SaveCurAudioBalance(balance_value_increase);
                break;
            case R.id.balance_decrease:
                int balance_value_decrease = mTvControlManager.GetCurAudioBalance() - 1;
                mTvControlManager.SetAudioBalance(balance_value_decrease);
                mTvControlManager.SaveCurAudioBalance(balance_value_decrease);
                break;
            // SPDIF
            case R.id.spdif_off:
                mTvControlManager.SetAudioSPDIFSwitch(0);
                mTvControlManager.SaveCurAudioSPDIFSwitch(0);
                break;
            case R.id.spdif_auto:
                mTvControlManager.SetAudioSPDIFMode(0);
                mTvControlManager.SaveCurAudioSPDIFMode(0);
                break;
            case R.id.spdif_pcm:
                mTvControlManager.SetAudioSPDIFMode(1);
                mTvControlManager.SaveCurAudioSPDIFMode(1);
                break;
            // Surround
            case R.id.surround_on:
                mTvControlManager.SetAudioSrsSurround(1);
                mTvControlManager.SaveCurAudioSrsSurround(1);
                break;
            case R.id.surround_off:
                mTvControlManager.SetAudioSrsSurround(0);
                mTvControlManager.SaveCurAudioSrsSurround(0);
                break;
            // Dialog Clarity
            case R.id.dialog_clarity_on:
                mTvControlManager.SetAudioSrsDialogClarity(1);
                mTvControlManager.SaveCurAudioSrsDialogClarity(1);
                break;
            case R.id.dialog_clarity_off:
                mTvControlManager.SetAudioSrsDialogClarity(0);
                mTvControlManager.SaveCurAudioSrsDialogClarity(0);
                break;
            // Bass Boost
            case R.id.bass_boost_on:
                mTvControlManager.SetAudioSrsTruBass(1);
                mTvControlManager.SaveCurAudioSrsTruBass(1);
                break;
            case R.id.bass_boost_off:
                mTvControlManager.SetAudioSrsTruBass(0);
                mTvControlManager.SaveCurAudioSrsTruBass(0);
                break;
            // ====Channel====
            // color system
            case R.id.color_system_auto:
                mSettingsManager.setColorSystem(TvControlManager.tvin_color_system_e.COLOR_SYSTEM_AUTO.toInt());
                break;
            case R.id.color_system_pal:
                mSettingsManager.setColorSystem(TvControlManager.tvin_color_system_e.COLOR_SYSTEM_PAL.toInt());
                break;
            case R.id.color_system_ntsc:
                mSettingsManager.setColorSystem(TvControlManager.tvin_color_system_e.COLOR_SYSTEM_NTSC.toInt());
                break;
            // sound system
            case R.id.sound_system_dk:
                 mSettingsManager.setSoundSystem(TvControlManager.atv_audio_std_e.ATV_AUDIO_STD_DK.toInt());
                break;
            case R.id.sound_system_i:
                mSettingsManager.setSoundSystem(TvControlManager.atv_audio_std_e.ATV_AUDIO_STD_I.toInt());
                break;
            case R.id.sound_system_bg:
                mSettingsManager.setSoundSystem(TvControlManager.atv_audio_std_e.ATV_AUDIO_STD_BG.toInt());
                break;
            case R.id.sound_system_m:
                mSettingsManager.setSoundSystem(TvControlManager.atv_audio_std_e.ATV_AUDIO_STD_M.toInt());
                break;
            // volume compensate
            case R.id.volume_compensate_increase:
                mSettingsManager.setVolumeCompensate(1);
                break;
            case R.id.volume_compensate_decrease:
                mSettingsManager.setVolumeCompensate(-1);
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
                finish_result = DroidLogicTvUtils.RESULT_UPDATE;
                break;
            // auto search
            case R.id.auto_search_start_atv:
            case R.id.auto_search_start_dtv:
                mTvDataBaseManager.deleteChannels(mSettingsManager.getInputId());
                if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV)
                    mTvControlManager.AtvAutoScan(TvControlManager.atv_video_std_e.ATV_VIDEO_STD_PAL, TvControlManager.atv_audio_std_e.ATV_AUDIO_STD_I, 0);
                else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV)
                    mTvControlManager.DtvAutoScan();
                isSearching = true;
                finish_result = DroidLogicTvUtils.RESULT_UPDATE;
                break;
            // ====Settings====
            // Sleep Timer
            case R.id.sleep_timer_off:
                mSettingsManager.setSleepTimer(0);
                break;
            case R.id.sleep_timer_15min:
                mSettingsManager.setSleepTimer(15);
                break;
            case R.id.sleep_timer_30min:
                mSettingsManager.setSleepTimer(30);
                break;
            case R.id.sleep_timer_45min:
                mSettingsManager.setSleepTimer(45);
                break;
            case R.id.sleep_timer_60min:
                mSettingsManager.setSleepTimer(60);
                break;
            case R.id.sleep_timer_90min:
                mSettingsManager.setSleepTimer(90);
                break;
            case R.id.sleep_timer_120min:
                mSettingsManager.setSleepTimer(120);
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
                mTvControlManager.startAutoBacklight();
                break;
            case R.id.dynamic_backlight_off:
                mTvControlManager.stopAutoBacklight();
                break;
            // Switch Channel
            case R.id.switch_channel_static_frame:
                mTvControlManager.setBlackoutEnable(0);
                break;
            case R.id.switch_channel_black_frame:
                mTvControlManager.setBlackoutEnable(1);
                break;
            // Restore Factory Settings
            case R.id.startup_setting_launcher:
                mSettingsManager.setStartupSetting(0);
                break;
            case R.id.startup_setting_tv:
                mSettingsManager.setStartupSetting(1);
                break;
            case R.id.restore_factory:
                createFactoryResetUi();
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
        if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
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

            mTvControlManager.AtvManualScan(Integer.valueOf(beginHZ) * 1000, Integer.valueOf(endHZ) * 1000,
                TvControlManager.atv_video_std_e.ATV_VIDEO_STD_PAL, TvControlManager.atv_audio_std_e.ATV_AUDIO_STD_DK);
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            EditText edit = (EditText) parent.findViewById(R.id.manual_search_dtv_channel);
            String channel = edit.getText().toString();
            if (channel == null || channel.length() == 0)
                channel = (String)edit.getHint();
            mTvControlManager.DtvManualScan(getDvbFrequencyByPd(Integer.valueOf(channel)));
        }
    }

    public void setManualSearchEditStyle(View view) {
        if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
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
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
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
        ArrayList<FreqList> m_fList = mTvControlManager.DTVGetScanFreqList();
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

    private void setManualSearchInfo(TvControlManager.ScannerEvent event) {
        ViewGroup optionView = (ViewGroup)((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);
        if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
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
        } else if (event != null && mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            OptionListView listView = (OptionListView)optionView.findViewById(R.id.manual_search_dtv_info);
             ArrayList<HashMap<String,Object>> dataList = getSearchedDtvInfo(event);
             SimpleAdapter adapter = new SimpleAdapter(mContext, dataList,
                    R.layout.layout_option_double_text,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
             listView.setAdapter(adapter);
        }
    }

    public ArrayList<HashMap<String,Object>> getSearchedDtvInfo (TvControlManager.ScannerEvent event) {
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

    private void setAutoSearchFrequency(TvControlManager.ScannerEvent event) {
        ViewGroup optionView = (ViewGroup) ((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);
        if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
            TextView frequency = (TextView) optionView.findViewById(R.id.auto_search_frequency_atv);
            TextView frequency_band = (TextView) optionView.findViewById(R.id.auto_search_frequency_band_atv);
            TextView searched_number = (TextView) optionView.findViewById(R.id.auto_search_searched_number_atv);
            if (frequency != null && frequency_band != null && searched_number != null) {
                double freq = event.freq/(1000 * 1000);
                frequency.setText(Double.toString(freq) + mContext.getResources().getString(R.string.mhz));
                frequency_band.setText(parseFrequencyBand(freq));
                searched_number.setText(mContext.getResources().getString(R.string.searched_number) + ": " + channelNumber);
            }
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
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

    private ChannelInfo createDtvChannelInfo (TvControlManager.ScannerEvent event) {
        String name = null;
        String serviceType;
        int display_number;

        try {
            String composedName = new String(event.programName);
            name = TVMultilingualText.getText(composedName);
            if (name == null || name.isEmpty()) {
                name = TVMultilingualText.getText(composedName, "first");
            }
        } catch (Exception e) {
            e.printStackTrace();
            name = "????";
        }

        if (event.srvType == 1) {
            serviceType = Channels.SERVICE_TYPE_AUDIO_VIDEO;
            display_number = tvDisplayNumber;
        } else if (event.srvType == 2) {
            serviceType = Channels.SERVICE_TYPE_AUDIO;
            display_number = radioDisplayNumber;
        } else {
            serviceType= Channels.SERVICE_TYPE_OTHER;
            display_number = radioDisplayNumber;
        }

        return new ChannelInfo.Builder()
            .setInputId(mSettingsManager.getInputId())
            .setType(event.mode)
            .setServiceType(serviceType)
            .setServiceId(event.serviceID)
            .setDisplayNumber(display_number)
            .setDisplayName(name)
            .setLogoUrl(null)
            .setOriginalNetworkId(event.orig_net_id)
            .setTransportStreamId(event.ts_id)
            .setVideoPid(event.vid)
            .setVideoStd(0)
            .setVideoFormat(event.vfmt)
            .setVideoWidth(0)
            .setVideoHeight(0)
            .setAudioPids(event.aids)
            .setAudioFormats(event.afmts)
            .setAudioLangs(event.alangs)
            .setAudioStd(0)
            .setIsAutoStd(event.isAutoStd)
            .setAudioTrackIndex(0)
            .setAudioCompensation(0)
            .setPcrPid(event.pcr)
            .setFrequency(event.freq)
            .setBandwidth(event.bandwidth)
            .setFineTune(0)
            .setBrowsable(true)
            .setIsFavourite(false)
            .setPassthrough(false)
            .setLocked(false)
            .build();
    }

    private ChannelInfo createAtvChannelInfo (TvControlManager.ScannerEvent event) {
        String serviceType;
        if (event.srvType == 1)
            serviceType = Channels.SERVICE_TYPE_AUDIO_VIDEO;
        else if (event.srvType == 2)
            serviceType = Channels.SERVICE_TYPE_AUDIO;
        else
            serviceType= Channels.SERVICE_TYPE_OTHER;

        return new ChannelInfo.Builder()
            .setInputId(mSettingsManager.getInputId())
            .setType(3)
            .setServiceType(serviceType)
            .setServiceId(0)
            .setDisplayNumber(tvDisplayNumber)
            .setDisplayName(event.programName)
            .setLogoUrl(null)
            .setOriginalNetworkId(0)
            .setTransportStreamId(0)
            .setVideoPid(0)
            .setVideoStd(event.videoStd)
            .setVideoFormat(0)
            .setVideoWidth(0)
            .setVideoHeight(0)
            .setAudioPids(null)
            .setAudioFormats(null)
            .setAudioLangs(null)
            .setAudioStd(event.audioStd)
            .setIsAutoStd(event.isAutoStd)
            .setAudioTrackIndex(0)
            .setAudioCompensation(0)
            .setPcrPid(0)
            .setFrequency(event.freq)
            .setBandwidth(0)
            .setFineTune(0)
            .setBrowsable(true)
            .setIsFavourite(false)
            .setPassthrough(false)
            .setLocked(false)
            .build();
    }

    @Override
    public void onEvent(TvControlManager.ScannerEvent event) {
        //Log.d("fuhao", "searching---precent = " + event.precent + ",arg0.quality = " + event.quality + ",strength = " + event.strength);
        ChannelInfo channel = null;
        String name = null;
        if (event.lock == 1) {
            // get a channel
            if (event.srvType == 1)
                channelNumber++;
            else
                radioNumber++;
        }
        switch (event.type) {
            case TvControlManager.EVENT_DTV_PROG_DATA:
                channel = createDtvChannelInfo(event);
                if (optionTag == OPTION_MANUAL_SEARCH) {
                    mTvDataBaseManager.updateOrinsertDtvChannel(channel);
                } else {
                    if (event.srvType == 1) {//{@link Channels#SERVICE_TYPE_AUDIO_VIDEO}
                        mTvDataBaseManager.insertDtvChannel(channel, tvDisplayNumber);
                        tvDisplayNumber++;
                    } else if (event.srvType == 2){
                        mTvDataBaseManager.insertDtvChannel(channel, radioDisplayNumber);
                        radioDisplayNumber++;
                    }
                }

                channel.print();
                break;
            case TvControlManager.EVENT_SCAN_PROGRESS:
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
            case TvControlManager.EVENT_ATV_PROG_DATA:
                channel = createAtvChannelInfo(event);
                if (optionTag == OPTION_MANUAL_SEARCH)
                    mTvDataBaseManager.updateOrinsertAtvChannel(channel);
                else
                    mTvDataBaseManager.insertAtvChannel(channel, tvDisplayNumber);
                tvDisplayNumber++;
                channel.print();
                break;
            case TvControlManager.EVENT_STORE_END:
                Log.d(TAG, "Store end");
                ((TvSettingsActivity) mContext).finish();
                break;
            case TvControlManager.EVENT_SCAN_END:
                Log.d(TAG, "Scan end");
                mTvControlManager.DtvStopScan();
                break;
            case TvControlManager.EVENT_SCAN_EXIT:
                Log.d(TAG, "Scan exit.");
                isSearching = false;
                if (channelNumber == 0 && radioNumber == 0) {
                    ((TvSettingsActivity) mContext).finish();
                }
                break;
            default:
                break;
        }
    }

    private void createFactoryResetUi () {
         LayoutInflater inflater =(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         View view = inflater.inflate(R.layout.layout_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final AlertDialog mAlertDialog = builder.create();
        mAlertDialog.show();
        mAlertDialog.getWindow().setContentView(view);
        //mAlertDialog.getWindow().setLayout(150, 320);

        TextView button_cancel = (TextView)view.findViewById(R.id.dialog_cancel);
        button_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAlertDialog != null)
                    mAlertDialog.dismiss();
            }
        });
        button_cancel.setOnFocusChangeListener(this);
        TextView button_ok = (TextView)view.findViewById(R.id.dialog_ok);
        button_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mSettingsManager.doFactoryReset();
                ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE)).reboot("");
            }
        });
        button_ok.setOnFocusChangeListener(this);
    }

    public void stopAllAction(){
    }
}
