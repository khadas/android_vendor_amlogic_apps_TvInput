package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.R.integer;
import android.amlogic.Tv;
import android.app.AlertDialog;

import com.droidlogic.app.DroidLogicTvUtils;
import com.droidlogic.app.DroidLogicTvUtils.TvClient;
import com.droidlogic.tvinput.R;

public class OptionUiManager implements OnClickListener, OnFocusChangeListener {
    public static final String TAG = "OptionUiManager";

    private static TvClient client = TvClient.getTvClient();
    private Tv tv = DroidLogicTvUtils.TvClient.getTvInstance();

    private static final int OPTION_PICTURE_MODE                   = 100;
    private static final int OPTION_BRIGHTNESS                     = 101;
    private static final int OPTION_CONTRAST                       = 102;
    private static final int OPTION_COLOR                          = 103;
    private static final int OPTION_SHARPNESS                      = 104;
    private static final int OPTION_BACKLIGHT                      = 105;
    private static final int OPTION_COLOR_TEMPERATURE              = 106;
    private static final int OPTION_ASPECT_RATIO                   = 107;
    private static final int OPTION_DNR                            = 108;
    private static final int OPTION_3D_SETTINGS                    = 109;

    private static final int OPTION_SOUND_MODE                     = 200;
    private static final int OPTION_TREBLE                         = 201;
    private static final int OPTION_BASS                           = 202;
    private static final int OPTION_BALANCE                        = 203;
    private static final int OPTION_SPDIF                          = 204;
    private static final int OPTION_DIALOG_CLARITY                 = 205;
    private static final int OPTION_BASS_BOOST                     = 206;
    private static final int OPTION_SURROUND                       = 207;

    private static final int OPTION_CURRENT_CHANNEL                = 300;
    private static final int OPTION_FREQUENCY                      = 301;
    private static final int OPTION_COLOR_SYSTEM                   = 302;
    private static final int OPTION_SOUND_SYSTEM                   = 303;
    private static final int OPTION_VOLUME_COMPENSATE              = 304;
    private static final int OPTION_FINE_TUNE                      = 305;
    private static final int OPTION_MANUAL_SEARCH                  = 306;
    private static final int OPTION_AUTO_SEARCH                    = 307;
    private static final int OPTION_CHANNEL_EDIT                   = 308;
    private static final int OPTION_SWITCH_CHANNEL                 = 309;

    private static final int OPTION_SLEEP_TIMER                    = 400;
    private static final int OPTION_MENU_TIME                      = 401;
    private static final int OPTION_STARTUP_SETTING                = 402;
    private static final int OPTION_DYNAMIC_BACKLIGHT              = 403;
    private static final int OPTION_RESTORE_FACTORY                = 404;

    public static final int ALPHA_NO_FOCUS                         = 230;
    public static final int ALPHA_FOCUSED                          = 255;

    private Context mContext;
    private SettingsManager mSettingsManager;
    private int optionTag = OPTION_PICTURE_MODE;
    private String optionKey = null;

    public OptionUiManager (Context context){
        mContext = context;
        mSettingsManager = ((TvSettingsActivity)mContext).getSettingsManager();
    }

    public void setOptionTag (int position) {
        String item_name = ((TvSettingsActivity)mContext).getCurrentFragment().getContentList()
            .get(position).get(ContentFragment.ITEM_NAME).toString();
        Log.d(TAG, "@@@@@@@@@ item_name=" + item_name);
        //Picture
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
        //Sound
        else if (item_name.equals(mContext.getResources().getString(R.string.sound_mode))){
            optionTag = OPTION_SOUND_MODE;
            optionKey = SettingsManager.KEY_SOUND_MODE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.treble))){
            optionTag = OPTION_TREBLE;
            optionKey = SettingsManager.KEY_TREBLE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.bass))){
            optionTag = OPTION_BASS;
            optionKey = SettingsManager.KEY_BASS;
        } else if (item_name.equals(mContext.getResources().getString(R.string.balance))){
            optionTag = OPTION_BALANCE;
            optionKey = SettingsManager.KEY_BALANCE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.spdif))){
            optionTag = OPTION_SPDIF;
            optionKey = SettingsManager.KEY_SPDIF;
        } else if (item_name.equals(mContext.getResources().getString(R.string.dialog_clarity))){
            optionTag = OPTION_DIALOG_CLARITY;
            optionKey = SettingsManager.KEY_DIALOG_CLARITY;
        } else if (item_name.equals(mContext.getResources().getString(R.string.bass_boost))){
            optionTag = OPTION_BASS_BOOST;
            optionKey = SettingsManager.KEY_BASS_BOOST;
        } else if (item_name.equals(mContext.getResources().getString(R.string.surround))){
            optionTag = OPTION_SURROUND;
            optionKey = SettingsManager.KEY_SURROUND;
        }
        //Channel
        else if (item_name.equals(mContext.getResources().getString(R.string.current_channel))){
            optionTag = OPTION_CURRENT_CHANNEL;
            optionKey = SettingsManager.KEY_CURRENT_CHANNEL;
        }else if (item_name.equals(mContext.getResources().getString(R.string.frequency))){
            optionTag = OPTION_FREQUENCY;
            optionKey = SettingsManager.KEY_FREQUNCY;
        }else if (item_name.equals(mContext.getResources().getString(R.string.color_system))){
            optionTag = OPTION_COLOR_SYSTEM;
            optionKey = SettingsManager.KEY_COLOR_SYSTEM;
        }else if (item_name.equals(mContext.getResources().getString(R.string.sound_system))){
            optionTag = OPTION_SOUND_SYSTEM;
            optionKey = SettingsManager.KEY_SOUND_SYSTEM;
        }else if (item_name.equals(mContext.getResources().getString(R.string.volume_compensate))){
            optionTag = OPTION_VOLUME_COMPENSATE;
            optionKey = SettingsManager.KEY_VOLUME_COMPENSATE;
        }else if (item_name.equals(mContext.getResources().getString(R.string.fine_tune))){
            optionTag = OPTION_FINE_TUNE;
            optionKey = SettingsManager.KEY_FINE_TUNE;
        }else if (item_name.equals(mContext.getResources().getString(R.string.manual_search))){
            optionTag = OPTION_MANUAL_SEARCH;
            optionKey = SettingsManager.KEY_MANUAL_SEARCH;
        }else if (item_name.equals(mContext.getResources().getString(R.string.auto_search))){
            optionTag = OPTION_AUTO_SEARCH;
            optionKey = SettingsManager.KEY_AUTO_SEARCH;
        }else if (item_name.equals(mContext.getResources().getString(R.string.channel_edit))){
            optionTag = OPTION_CHANNEL_EDIT;
            optionKey = SettingsManager.KEY_CHANNEL_EDIT;
        }else if (item_name.equals(mContext.getResources().getString(R.string.switch_channel))){
            optionTag = OPTION_SWITCH_CHANNEL;
            optionKey = SettingsManager.KEY_SWITCH_CHANNEL;
        }
        //Settings
        else if (item_name.equals(mContext.getResources().getString(R.string.sleep_timer))){
            optionTag = OPTION_SLEEP_TIMER;
            optionKey = SettingsManager.KEY_SLEEP_TIMER;
        }else if (item_name.equals(mContext.getResources().getString(R.string.menu_time))){
            optionTag = OPTION_MENU_TIME;
            optionKey = SettingsManager.KEY_MENU_TIME;
        }else if (item_name.equals(mContext.getResources().getString(R.string.startup_setting))){
            optionTag = OPTION_STARTUP_SETTING;
            optionKey = SettingsManager.KEY_STARTUP_SETTING;
        }else if (item_name.equals(mContext.getResources().getString(R.string.dynamic_backlight))){
            optionTag = OPTION_DYNAMIC_BACKLIGHT;
            optionKey = SettingsManager.KEY_DYNAMIC_BACKLIGHT;
        }else if (item_name.equals(mContext.getResources().getString(R.string.restore_factory))){
            optionTag = OPTION_RESTORE_FACTORY;
            optionKey = SettingsManager.KEY_RESTORE_FACTORY;
        }
    }

    public int getLayoutId() {
        switch (optionTag) {
            //picture
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
            //sound
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
            //channel
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
                return R.layout.layout_channel_manual_search;
            case OPTION_AUTO_SEARCH:
                return R.layout.layout_channel_auto_search;
            //settings
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

    public void setOptionListener (View view) {
        for (int i = 0; i < ((ViewGroup)view).getChildCount(); i++) {
            View child = ((ViewGroup)view).getChildAt(i);
            if (child != null && child.hasFocusable()) {
                child.setOnClickListener(this);
                child.setOnFocusChangeListener(this);
            }
        }
    }

    @Override
    public void onClick(View view) {
        Resources res = mContext.getResources();
        switch (view.getId()) {
            //====Picture====
            //picture mode
            case R.id.picture_mode_standard:
                tv.SetPQMode(Tv.Pq_Mode.PQ_MODE_STANDARD, client.curSource, 1);
                mSettingsManager.setPictureMode(SettingsManager.STATUS_STANDARD);
                break;
            case R.id.picture_mode_vivid:
                tv.SetPQMode(Tv.Pq_Mode.PQ_MODE_BRIGHT, client.curSource, 1);
                mSettingsManager.setPictureMode(SettingsManager.STATUS_VIVID);
                break;
            case R.id.picture_mode_soft:
                tv.SetPQMode(Tv.Pq_Mode.PQ_MODE_SOFTNESS, client.curSource, 1);
                mSettingsManager.setPictureMode(SettingsManager.STATUS_SOFT);
                break;
            case R.id.picture_mode_user:
                tv.SetPQMode(Tv.Pq_Mode.PQ_MODE_USER, client.curSource, 1);
                mSettingsManager.setPictureMode(SettingsManager.STATUS_USER);
                break;
            //brightness
            case R.id.brightness_increase:
                tv.SetBrightness(tv.GetBrightness(client.curSource) + 1, client.curSource, 1);
                mSettingsManager.setBrightness(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.brightness_decrease:
                tv.SetBrightness(tv.GetBrightness(client.curSource) - 1, client.curSource, 1);
                mSettingsManager.setBrightness(SettingsManager.PERCENT_DECREASE);
                break;
            //contrast
            case R.id.contrast_increase:
                tv.SetContrast(tv.GetContrast(client.curSource) + 1, client.curSource, 1);
                mSettingsManager.setContrast(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.contrast_decrease:
                tv.SetContrast(tv.GetContrast(client.curSource) - 1, client.curSource, 1);
                mSettingsManager.setContrast(SettingsManager.PERCENT_DECREASE);
                break;
            //color
            case R.id.color_increase:
                tv.SetSaturation(tv.GetSaturation(client.curSource) + 1, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
                mSettingsManager.setColor(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.color_decrease:
                tv.SetSaturation(tv.GetSaturation(client.curSource) - 1, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
                mSettingsManager.setColor(SettingsManager.PERCENT_DECREASE);
                break;
            //sharpness
            case R.id.sharpness_increase:
                tv.SetSharpness(tv.GetSharpness(client.curSource) + 1, client.curSource, 1, 0, 1);
                mSettingsManager.setSharpness(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.sharpness_decrease:
                tv.SetSharpness(tv.GetSharpness(client.curSource) - 1, client.curSource, 1, 0, 1);
                mSettingsManager.setSharpness(SettingsManager.PERCENT_DECREASE);
                break;
            //backlight
            case R.id.backlight_increase:
                tv.SetBacklight(tv.GetBacklight(client.curSource) + 1, client.curSource, 1);
                mSettingsManager.setBacklight(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.backlight_decrease:
                tv.SetBacklight(tv.GetBacklight(client.curSource) - 1, client.curSource, 1);
                mSettingsManager.setBacklight(SettingsManager.PERCENT_DECREASE);
                break;
            //color temperature
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
            //aspect ratio
            case R.id.apect_ratio_auto:
                if (tv.Get3DMode() == 0) {
                    tv.SetDisplayMode(Tv.Display_Mode.DISPLAY_MODE_NORMAL, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
                }
                mSettingsManager.setAspectRatio(SettingsManager.STATUS_AUTO);
                break;
            case R.id.apect_ratio_four2three:
                if (tv.Get3DMode() == 0) {
                    tv.SetDisplayMode(Tv.Display_Mode.DISPLAY_MODE_MODE43, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
                }
                mSettingsManager.setAspectRatio(SettingsManager.STATUS_4_TO_3);
                break;
            case R.id.apect_ratio_panorama:
                if (tv.Get3DMode() == 0) {
                    tv.SetDisplayMode(Tv.Display_Mode.DISPLAY_MODE_FULL, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
                }
                mSettingsManager.setAspectRatio(SettingsManager.STATUS_PANORAMA);
                break;
            case R.id.apect_ratio_full_screen:
                if (tv.Get3DMode() == 0) {
                    tv.SetDisplayMode(Tv.Display_Mode.DISPLAY_MODE_169, client.curSource, tv.GetCurrentSignalInfo().fmt, 1);
                }
                mSettingsManager.setAspectRatio(SettingsManager.STATUS_FULL_SCREEN);
                break;
            //dnr
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
            //3d settings
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
                //tv.Set3DTo2DMode(Tv.Mode_3D_2D.values()[position], Tv.Tvin_3d_Status.values()[tv.Get3DMode()]);
                mSettingsManager.set3dSettings(SettingsManager.STATUS_3D_TO_2D);
                break;
            //====Sound====
            //sound mode
            case R.id.sound_mode_standard:
                tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_STD);
                tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_STD.toInt());
                break;
            case R.id.sound_mode_music:
                tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_MUSIC);
                tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_MUSIC.toInt());
                break;
            case R.id.sound_mode_news:
                tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_NEWS);
                tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_NEWS.toInt());
                break;
            case R.id.sound_mode_movie:
                tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_THEATER);
                tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_THEATER.toInt());
                break;
            case R.id.sound_mode_user:
                tv.SetAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_USER);
                tv.SaveCurAudioSoundMode(Tv.Sound_Mode.SOUND_MODE_USER.toInt());
                break;
            //Treble
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
            //Bass
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
            //Balance
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
            //SPDIF
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
            //Surround
            case R.id.surround_on:
                tv.SetAudioSrsSurround(1);
                tv.SaveCurAudioSrsSurround(1);
                break;
            case R.id.surround_off:
                tv.SetAudioSrsSurround(0);
                tv.SaveCurAudioSrsSurround(0);
                break;
            //Dialog Clarity
            case R.id.dialog_clarity_on:
                tv.SetAudioSrsDialogClarity(1);
                tv.SaveCurAudioSrsDialogClarity(1);
                break;
            case R.id.dialog_clarity_off:
                tv.SetAudioSrsDialogClarity(0);
                tv.SaveCurAudioSrsDialogClarity(0);
                break;
            //Bass Boost
            case R.id.bass_boost_on:
                tv.SetAudioSrsTruBass(1);
                tv.SaveCurAudioSrsTruBass(1);
                break;
            case R.id.bass_boost_off:
                tv.SetAudioSrsTruBass(0);
                tv.SaveCurAudioSrsTruBass(0);
                break;
            //====Channel====
            //color system
            case R.id.color_system_auto:
                break;
            case R.id.color_system_pal:
                break;
            case R.id.color_system_ntsc:
                break;
            //sound system
            case R.id.sound_system_dk:
                break;
            case R.id.sound_system_i:
                break;
            case R.id.sound_system_bg:
                break;
            case R.id.sound_system_m:
                break;
            //volume compensate
            case R.id.volume_compensate_increase:
                break;
            case R.id.volume_compensate_decrease:
                break;
            //fine tune
            case R.id.fine_tune_increase:
                break;
            case R.id.fine_tune_decrease:
                break;
            //manual search
            //auto search
            case R.id.auto_search_start:
                break;
            //====Settins====
            //Sleep Timer
            case R.id.sleep_timer_off:
                SystemProperties.set("tv.sleep_timer", "0");
                break;
            case R.id.sleep_timer_15min:
                SystemProperties.set("tv.sleep_timer", "900000");
                break;
            case R.id.sleep_timer_30min:
                SystemProperties.set("tv.sleep_timer", "1800000");
                break;
            case R.id.sleep_timer_45min:
                SystemProperties.set("tv.sleep_timer", "2700000");
                break;
            case R.id.sleep_timer_60min:
                SystemProperties.set("tv.sleep_timer", "3600000");
                break;
            case R.id.sleep_timer_90min:
                SystemProperties.set("tv.sleep_timer", "5400000");
                break;
            case R.id.sleep_timer_120min:
                SystemProperties.set("tv.sleep_timer", "7200000");
                break;
            //Dynamic Backlight
            case R.id.dynamic_backlight_on:
                tv.startAutoBacklight();
                break;
            case R.id.dynamic_backlight_off:
                tv.stopAutoBacklight();
                break;
            //Restore Factory Settings
            case R.id.restore_factory:
                new AlertDialog.Builder(mContext).setTitle(R.string.warning).setMessage(R.string.prompt_def_set)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
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
        ((TvSettingsActivity)mContext).getCurrentFragment().refreshList();
    }

    @Override
    public void onFocusChange (View v, boolean hasFocus) {
        if (v instanceof TextView) {
            if (hasFocus) {
                ((TextView)v).setTextColor(mContext.getResources().getColor(R.color.color_text_focused));
            } else
                ((TextView)v).setTextColor(mContext.getResources().getColor(R.color.color_text_item));
        }
    }

    public void setProgressStatus() {
        int progress;

        switch (optionTag) {
            case OPTION_FINE_TUNE:
                progress = mSettingsManager.getFineTuneProgress();
                setProgress(progress);
                setFineTuneFrequency(progress);
                break;
            case OPTION_AUTO_SEARCH:
                progress = mSettingsManager.getAutoSearchProgress();
                setProgress(progress);
                setAutoSearchFrequency(progress);
                break;
            default:
                progress = getIntegerFromString(mSettingsManager.getStatus(optionKey));
                if (progress >= 0)
                    setProgress(progress);
                break;
        }
    }

    public void setProgress (int progress) {
        RelativeLayout progressLayout = null;
        ViewGroup optionView = (ViewGroup)((TvSettingsActivity)mContext).mOptionLayout.getChildAt(0);

        for (int i = 0; i < optionView.getChildCount(); i++) {
            View view = optionView.getChildAt(i);
            if (view instanceof RelativeLayout) {
                progressLayout = (RelativeLayout)view;
                break;
            }
        }

        if (progressLayout != null) {
            for (int i = 0; i < progressLayout.getChildCount(); i++) {
                View child = progressLayout.getChildAt(i);
                if ( child instanceof TextView) {
                    ((TextView)child).setText(Integer.toString(progress) + "%");
                } else if (child instanceof ImageView) {
                    ((ImageView)child).setImageResource(getProgressResourceId(progress));
                }
            }
        }
    }

    public int getProgressResourceId (int progress) {
        if (progress == 0)
            ;

        switch (progress / 20) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
            case 8:
                break;
            case 9:
                break;
            case 10:
                break;
            case 11:
                break;
            case 12:
                break;
            case 13:
                break;
            case 14:
                break;
            case 15:
                break;
            case 16:
                break;
            case 17:
                break;
            case 18:
                break;
            case 19:
                break;
            case 20:
                break;
            default:
                break;
        }
        return R.drawable.progress_75;
    }

    private void setFineTuneFrequency (int progress) {
        ViewGroup optionView = (ViewGroup)((TvSettingsActivity)mContext).mOptionLayout.getChildAt(0);

        TextView frequency = (TextView)optionView.findViewById(R.id.fine_tune_frequency);
        TextView frequency_band = (TextView)optionView.findViewById(R.id.fine_tune_frequency_band);

        if (frequency != null && frequency_band != null) {
            frequency.setText("525.25MHZ");
            frequency_band.setText("UHF");
        }
    }

    private void setAutoSearchFrequency (int progress) {
        ViewGroup optionView = (ViewGroup)((TvSettingsActivity)mContext).mOptionLayout.getChildAt(0);

        TextView frequency = (TextView)optionView.findViewById(R.id.auto_search_frequency);
        TextView frequency_band = (TextView)optionView.findViewById(R.id.auto_search_frequency_band);
        TextView searched_number = (TextView)optionView.findViewById(R.id.auto_search_searched_number);
        if (frequency != null && frequency_band != null && searched_number != null) {
            frequency.setText("525.25MHZ");
            frequency_band.setText("UHF");
            searched_number.setText(mContext.getResources().getString(R.string.searched_number) + ": "
                + mSettingsManager.getAutoSearchSearchedNumber());
        }
    }

    private int getIntegerFromString (String str) {
        if (str!= null && str.contains("%"))
            return Integer.valueOf(str.replaceAll("%", ""));
        else
            return -1;

        /*String regex = "[0-9\\.]+";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        if (m.find())
            return Integer.valueOf(m.group());
        else
            return -1;*/
    }
}
