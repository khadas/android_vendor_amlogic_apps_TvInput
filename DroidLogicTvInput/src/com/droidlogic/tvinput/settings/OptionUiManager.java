package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import com.droidlogic.tvinput.R;

public class OptionUiManager implements OnClickListener, OnFocusChangeListener {
    public static final String TAG = "OptionUiManager";

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

    public static final int ALPHA_NO_FOCUS                         = 160;
    public static final int ALPHA_FOCUSED                          = 255;

    private Context mContext;
    private SettingsManager mSettingsManager;
    private int optionTag = OPTION_PICTURE_MODE;

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
        } else if (item_name.equals(mContext.getResources().getString(R.string.brightness))) {
            optionTag = OPTION_BRIGHTNESS;
        } else if (item_name.equals(mContext.getResources().getString(R.string.contrast))) {
            optionTag = OPTION_CONTRAST;
        } else if (item_name.equals(mContext.getResources().getString(R.string.color))) {
            optionTag = OPTION_COLOR;
        } else if (item_name.equals(mContext.getResources().getString(R.string.sharpness))) {
            optionTag = OPTION_SHARPNESS;
        } else if (item_name.equals(mContext.getResources().getString(R.string.backlight))) {
            optionTag = OPTION_BACKLIGHT;
        } else if (item_name.equals(mContext.getResources().getString(R.string.color_temperature))) {
            optionTag = OPTION_COLOR_TEMPERATURE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.aspect_ratio))) {
            optionTag = OPTION_ASPECT_RATIO;
        } else if (item_name.equals(mContext.getResources().getString(R.string.dnr))) {
            optionTag = OPTION_DNR;
        } else if (item_name.equals(mContext.getResources().getString(R.string.settings_3d))) {
            optionTag = OPTION_3D_SETTINGS;
        }
        //Sound
        else if (item_name.equals(mContext.getResources().getString(R.string.sound_mode))){
            optionTag = OPTION_SOUND_MODE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.treble))){
            optionTag = OPTION_TREBLE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.bass))){
            optionTag = OPTION_BASS;
        } else if (item_name.equals(mContext.getResources().getString(R.string.balance))){
            optionTag = OPTION_BALANCE;
        } else if (item_name.equals(mContext.getResources().getString(R.string.spdif))){
            optionTag = OPTION_SPDIF;
        } else if (item_name.equals(mContext.getResources().getString(R.string.dialog_clarity))){
            optionTag = OPTION_DIALOG_CLARITY;
        } else if (item_name.equals(mContext.getResources().getString(R.string.bass_boost))){
            optionTag = OPTION_BASS_BOOST;
        } else if (item_name.equals(mContext.getResources().getString(R.string.surround))){
            optionTag = OPTION_SURROUND;
        }
        //Channel
        else if (item_name.equals(mContext.getResources().getString(R.string.current_channel))){
            optionTag = OPTION_CURRENT_CHANNEL;
        }else if (item_name.equals(mContext.getResources().getString(R.string.frequency))){
            optionTag = OPTION_FREQUENCY;
        }else if (item_name.equals(mContext.getResources().getString(R.string.color_system))){
            optionTag = OPTION_COLOR_SYSTEM;
        }else if (item_name.equals(mContext.getResources().getString(R.string.sound_system))){
            optionTag = OPTION_SOUND_SYSTEM;
        }else if (item_name.equals(mContext.getResources().getString(R.string.volume_compensate))){
            optionTag = OPTION_VOLUME_COMPENSATE;
        }else if (item_name.equals(mContext.getResources().getString(R.string.fine_tune))){
            optionTag = OPTION_FINE_TUNE;
        }else if (item_name.equals(mContext.getResources().getString(R.string.manual_search))){
            optionTag = OPTION_MANUAL_SEARCH;
        }else if (item_name.equals(mContext.getResources().getString(R.string.auto_search))){
            optionTag = OPTION_AUTO_SEARCH;
        }else if (item_name.equals(mContext.getResources().getString(R.string.channel_edit))){
            optionTag = OPTION_CHANNEL_EDIT;
        }else if (item_name.equals(mContext.getResources().getString(R.string.switch_channel))){
            optionTag = OPTION_SWITCH_CHANNEL;
        }
        //Settings
        else if (item_name.equals(mContext.getResources().getString(R.string.sleep_timer))){
            optionTag = OPTION_SLEEP_TIMER;
        }else if (item_name.equals(mContext.getResources().getString(R.string.menu_time))){
            optionTag = OPTION_MENU_TIME;
        }else if (item_name.equals(mContext.getResources().getString(R.string.startup_setting))){
            optionTag = OPTION_STARTUP_SETTING;
        }else if (item_name.equals(mContext.getResources().getString(R.string.dynamic_backlight))){
            optionTag = OPTION_DYNAMIC_BACKLIGHT;
        }else if (item_name.equals(mContext.getResources().getString(R.string.restore_factory))){
            optionTag = OPTION_RESTORE_FACTORY;
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
            //brightness
            case R.id.brightness_increase:
                mSettingsManager.setBrightness(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.brightness_decrease:
                mSettingsManager.setBrightness(SettingsManager.PERCENT_DECREASE);
                break;
            //contrast
            case R.id.contrast_increase:
                mSettingsManager.setContrast(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.contrast_decrease:
                mSettingsManager.setContrast(SettingsManager.PERCENT_DECREASE);
                break;
            //color
            case R.id.color_increase:
                mSettingsManager.setColor(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.color_decrease:
                mSettingsManager.setColor(SettingsManager.PERCENT_DECREASE);
                break;
            //sharpness
            case R.id.sharpness_increase:
                mSettingsManager.setSharpness(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.sharpness_decrease:
                mSettingsManager.setSharpness(SettingsManager.PERCENT_DECREASE);
                break;
            //backlight
            case R.id.backlight_increase:
                mSettingsManager.setBacklight(SettingsManager.PERCENT_INCREASE);
                break;
            case R.id.backlight_decrease:
                mSettingsManager.setBacklight(SettingsManager.PERCENT_DECREASE);
                break;
            //color temperature
            case R.id.color_temperature_standard:
                mSettingsManager.setColorTemperature(SettingsManager.STATUS_STANDARD);
                break;
            case R.id.color_temperature_warm:
                mSettingsManager.setColorTemperature(SettingsManager.STATUS_WARM);
                break;
            case R.id.color_temperature_cool:
                mSettingsManager.setColorTemperature(SettingsManager.STATUS_COOL);
                break;
            //aspect ratio
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
            //dnr
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
            //3d settings
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
            default:
                break;
        }
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
}
