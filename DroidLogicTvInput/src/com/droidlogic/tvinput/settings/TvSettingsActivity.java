package com.droidlogic.tvinput.settings;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;
import android.media.tv.TvInputInfo;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.tvinput.R;
import com.droidlogic.app.tv.TvControlManager;

public class TvSettingsActivity extends Activity implements OnClickListener, OnFocusChangeListener {
    private static final String TAG = "MainActivity";

    private ContentFragment fragmentImage;
    private ContentFragment fragmentSound;
    private ContentFragment fragmentChannel;
    private ContentFragment fragmentSettings;
    public RelativeLayout mOptionLayout = null;

    private ContentFragment currentFragment;
    private SettingsManager mSettingsManager;
    private OptionUiManager mOptionUiManager;

    private ImageButton tabPicture;
    private ImageButton tabSound;
    private ImageButton tabChannel;
    private ImageButton tabSettings;
    private TvControlManager mTvControlManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.layout_main);

        mSettingsManager = new SettingsManager(this, getIntent());
        mTvControlManager = mSettingsManager.getTvControlManager();

        tabPicture= (ImageButton) findViewById(R.id.button_picture);
        tabSound= (ImageButton) findViewById(R.id.button_sound);
        tabChannel= (ImageButton) findViewById(R.id.button_channel);
        tabSettings= (ImageButton) findViewById(R.id.button_settings);
        tabPicture.setOnClickListener(this);
        tabPicture.setOnFocusChangeListener(this);
        tabSound.setOnClickListener(this);
        tabSound.setOnFocusChangeListener(this);
        tabChannel.setOnClickListener(this);
        tabChannel.setOnFocusChangeListener(this);
        tabSettings.setOnClickListener(this);
        tabSettings.setOnFocusChangeListener(this);

        if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_HDMI ||
            mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_AV) {
            tabChannel.setVisibility(View.GONE);
            findViewById(R.id.title_channel).setVisibility(View.GONE);
        }

        if (savedInstanceState == null)
            setDefaultFragment();

        mOptionUiManager = new OptionUiManager(this);
        startShowActivityTimer();
    }

    private void setDefaultFragment() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        currentFragment = new ContentFragment(R.xml.list_picture, tabPicture);
        transaction.replace(R.id.settings_list, currentFragment);
        transaction.commit();
    }

    public boolean dispatchKeyEvent (KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (mOptionUiManager.isSearching())
                        return true;
                    startShowActivityTimer();
                    break;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Log.d(TAG, "==== focus =" + getCurrentFocus() + ", keycode =" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mOptionUiManager.isSearching()) {
                    mTvControlManager.DtvStopScan();
                    return true;
                }
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
       /* FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        switch (v.getId()) {
            case R.id.button_picture:
                if (fragmentImage == null) {
                    fragmentImage = new ContentFragment(R.xml.list_picture);
                }
                transaction.replace(R.id.settings_list, fragmentImage);
                break;
            case R.id.button_sound:
                if (fragmentSound == null) {
                    fragmentSound = new ContentFragment(R.xml.list_sound);
                }
                transaction.replace(R.id.settings_list, fragmentSound);
                break;
            case R.id.button_channel:
                if (fragmentChannel== null) {
                    fragmentChannel= new ContentFragment(R.xml.list_channel);
                }
                transaction.replace(R.id.settings_list, fragmentChannel);
                break;
            case R.id.button_settings:
                if (fragmentSettings== null) {
                    fragmentSettings= new ContentFragment(R.xml.list_settings);
                }
                transaction.replace(R.id.settings_list, fragmentSettings);
                break;
        }
        // transaction.addToBackStack();
        transaction.commit();*/
    }

    @Override
    public void onFocusChange (View v, boolean hasFocus) {
        if (hasFocus) {
            RelativeLayout main_view = (RelativeLayout)findViewById(R.id.main);
            if (mOptionLayout != null)
                main_view.removeView(mOptionLayout);

            FragmentManager fm = getFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();

            switch (v.getId()) {
            case R.id.button_picture:
                currentFragment = new ContentFragment(R.xml.list_picture, v);
                transaction.replace(R.id.settings_list, currentFragment);
                break;
            case R.id.button_sound:
                currentFragment = new ContentFragment(R.xml.list_sound, v);
                transaction.replace(R.id.settings_list, currentFragment);
                break;
            case R.id.button_channel:
                if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV)
                    currentFragment = new ContentFragment(R.xml.list_channel_atv, v);
                else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV)
                    currentFragment = new ContentFragment(R.xml.list_channel_dtv, v);
                transaction.replace(R.id.settings_list, currentFragment);
                break;
            case R.id.button_settings:
                currentFragment = new ContentFragment(R.xml.list_settings, v);
                transaction.replace(R.id.settings_list, currentFragment);
                break;
            }
            // transaction.addToBackStack();
            transaction.commit();
        }
    }

    public ContentFragment getCurrentFragment () {
        return currentFragment;
    }

    public SettingsManager getSettingsManager () {
        return mSettingsManager;
    }

    public OptionUiManager getOptionUiManager () {
        return mOptionUiManager;
    }

    @Override
    public void finish() {
        setResult(mSettingsManager.getActivityResult());
        super.finish();
    }

    public void onStop()
    {
        super.onStop();
    }

    public void startShowActivityTimer () {
        handler.removeMessages(0);

        int seconds = Settings.System.getInt(getContentResolver(), SettingsManager.KEY_MENU_TIME, SettingsManager.DEFUALT_MENU_TIME);
        handler.sendEmptyMessageDelayed(0, seconds * 1000);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (!mOptionUiManager.isSearching() && !imm. isAcceptingText())
                finish();
            else  {
                int seconds = Settings.System.getInt(getContentResolver(), SettingsManager.KEY_MENU_TIME, SettingsManager.DEFUALT_MENU_TIME);
                sendEmptyMessageDelayed(0, seconds * 1000);
            }
        }
    };
}
