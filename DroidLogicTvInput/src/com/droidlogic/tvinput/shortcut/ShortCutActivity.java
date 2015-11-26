package com.droidlogic.tvinput.shortcut;

import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.tvinput.settings.SettingsManager;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.droidlogic.tvinput.R;

public class ShortCutActivity extends Activity {
    private static final String TAG = "ShortCutActivity";

    private static final int KEY_PICTURE_MODE = 2005;
    private static final int KEY_SOUND_MODE = 2006;
    private static final int KEY_ASPECT_RATIO = 2004;
    private static final int KEY_SLEEP_TIMER = 2009;

    private static final int TOAST_SHOW_TIME = 3000;

    private SettingsManager mSettingsManager;
    private Resources mResources;
    private Toast toast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mSettingsManager = new SettingsManager(this, getIntent());
        mResources = getResources();

        int mode = getIntent().getIntExtra(DroidLogicTvUtils.EXTRA_KEY_CODE, 0);
        setShortcutMode(mode);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEY_PICTURE_MODE:
                setShortcutMode(DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE);
                break;
            case KEY_SOUND_MODE:
                setShortcutMode(DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE);
                break;
            case KEY_ASPECT_RATIO:
                setShortcutMode(DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE);
                break;
            case KEY_SLEEP_TIMER:
                setShortcutMode(DroidLogicKeyEvent.KEYCODE_TV_SLEEP);
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setShortcutMode (int mode) {
        switch (mode) {
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE:
               String display_mode = mSettingsManager.getAspectRatioStatus();
                if (display_mode.equals(mResources.getString(R.string.auto))) {
                    mSettingsManager.setAspectRatio(SettingsManager.STATUS_4_TO_3);
                } else if (display_mode.equals(mResources.getString(R.string.four2three))) {
                    //mSettingsManager.setAspectRatio(SettingsManager.STATUS_PANORAMA);
                    mSettingsManager.setAspectRatio(SettingsManager.STATUS_FULL_SCREEN);
                } else if (display_mode.equals(mResources.getString(R.string.panorama))) {
                    mSettingsManager.setAspectRatio(SettingsManager.STATUS_FULL_SCREEN);
                } else if (display_mode.equals(mResources.getString(R.string.full_screen))) {
                    mSettingsManager.setAspectRatio(SettingsManager.STATUS_AUTO);
                }
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_3DMODE:
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE:
                String picture_mode = mSettingsManager.getPictureModeStatus();
                if (picture_mode.equals(mResources.getString(R.string.standard))) {
                    mSettingsManager.setPictureMode(SettingsManager.STATUS_VIVID);
                } else if (picture_mode.equals(mResources.getString(R.string.vivid))) {
                    mSettingsManager.setPictureMode(SettingsManager.STATUS_SOFT);
                } else if (picture_mode.equals(mResources.getString(R.string.soft))) {
                    mSettingsManager.setPictureMode(SettingsManager.STATUS_USER);
                } else if (picture_mode.equals(mResources.getString(R.string.user))) {
                    mSettingsManager.setPictureMode(SettingsManager.STATUS_STANDARD);
                }
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE:
                String sound_mode = mSettingsManager.getSoundModeStatus();
                if (sound_mode.equals(mResources.getString(R.string.standard))) {
                    mSettingsManager.setSoundMode(SettingsManager.STATUS_MUSIC);
                } else if (sound_mode.equals(mResources.getString(R.string.music))) {
                    mSettingsManager.setSoundMode(SettingsManager.STATUS_NEWS);
                } else if (sound_mode.equals(mResources.getString(R.string.news))) {
                    mSettingsManager.setSoundMode(SettingsManager.STATUS_MOVIE);
                } else if (sound_mode.equals(mResources.getString(R.string.movie))) {
                    mSettingsManager.setSoundMode(SettingsManager.STATUS_USER);
                } else if (sound_mode.equals(mResources.getString(R.string.user))) {
                    mSettingsManager.setSoundMode(SettingsManager.STATUS_STANDARD);
                }
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SLEEP:
                String sleep_time = mSettingsManager.getSleepTimerStatus();
                if (sleep_time.equals(mResources.getString(R.string.off))) {
                    mSettingsManager.setSleepTimer(15);
                } else if (sleep_time.equals(mResources.getString(R.string.time_15min))) {
                    mSettingsManager.setSleepTimer(30);
                } else if (sleep_time.equals(mResources.getString(R.string.time_30min))) {
                    mSettingsManager.setSleepTimer(45);
                } else if (sleep_time.equals(mResources.getString(R.string.time_45min))) {
                    mSettingsManager.setSleepTimer(60);
                } else if (sleep_time.equals(mResources.getString(R.string.time_60min))) {
                    mSettingsManager.setSleepTimer(90);
                } else if (sleep_time.equals(mResources.getString(R.string.time_90min))) {
                    mSettingsManager.setSleepTimer(120);
                } else if (sleep_time.equals(mResources.getString(R.string.time_120min))) {
                    mSettingsManager.setSleepTimer(0);
                }
                break;
            case DroidLogicKeyEvent.KEYCODE_MEDIA_AUDIO_CONTROL:

                break;
            case DroidLogicKeyEvent.KEYCODE_GUIDE:
                break;
            default:
                break;
        }

        showCustomToast(mode);
    }

    private void showCustomToast(int mode){
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.layout_shortcut_key, null);

        TextView title =(TextView)layout.findViewById(R.id.toast_title);
        TextView status =(TextView)layout.findViewById(R.id.toast_status);

        title.setText(getToastTitle(mode));
        status.setText(getStatusTitle(mode));

        if (toast == null) {
            toast = new Toast(this);
            toast.setDuration(TOAST_SHOW_TIME);
            toast.setGravity(Gravity.CENTER_VERTICAL, 400, 300);
        }
        toast.setView(layout);
        toast.show();
        startShowActivityTimer();
    }

    private String getToastTitle (int mode) {
        switch (mode) {
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE:
                return mResources.getString(R.string.picture_mode);
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_3DMODE:
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE:
                return mResources.getString(R.string.picture_mode);
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE:
                return mResources.getString(R.string.sound_mode);
            case DroidLogicKeyEvent.KEYCODE_TV_SLEEP:
                return mResources.getString(R.string.sleep_timer);
            case DroidLogicKeyEvent.KEYCODE_MEDIA_AUDIO_CONTROL:
                break;
            case DroidLogicKeyEvent.KEYCODE_GUIDE:
                break;
            default:
                break;
        }
        return null;
    }

    private String getStatusTitle (int mode) {
        switch (mode) {
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE:
                return mSettingsManager.getAspectRatioStatus();
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_3DMODE:
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE:
                return mSettingsManager.getPictureModeStatus();
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE:
                return mSettingsManager.getSoundModeStatus();
            case DroidLogicKeyEvent.KEYCODE_TV_SLEEP:
                return mSettingsManager.getSleepTimerStatus();
            case DroidLogicKeyEvent.KEYCODE_MEDIA_AUDIO_CONTROL:
                break;
            case DroidLogicKeyEvent.KEYCODE_GUIDE:
                break;
            default:
                break;
        }
        return null;
    }

    public void startShowActivityTimer () {
        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, TOAST_SHOW_TIME);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0)
                finish();
        }
    };
}
