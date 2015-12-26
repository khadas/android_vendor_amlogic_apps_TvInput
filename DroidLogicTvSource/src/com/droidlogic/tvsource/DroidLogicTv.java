package com.droidlogic.tvsource;


import java.util.ArrayList;
import java.util.List;

import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.tv.DroidLogicTvUtils;

import com.droidlogic.tvsource.ui.ChannelListLayout;
import com.droidlogic.tvsource.ui.ChannelListLayout.OnChannelSelectListener;
import com.droidlogic.tvsource.ui.SourceButton;
import com.droidlogic.tvsource.ui.SourceInputListLayout;
import com.droidlogic.tvsource.ui.SourceInputListLayout.onSourceInputClickListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources.NotFoundException;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvView;
import android.media.tv.TvContract.Channels;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Handler.Callback;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View.OnAttachStateChangeListener;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DroidLogicTv extends Activity implements Callback, onSourceInputClickListener, OnChannelSelectListener {
    private static final String TAG = "DroidLogicTv";
    private static final String SHARE_NAME = "tv_app";

    private Context mContext;
    private TvInputManager mTvInputManager;
    private TvInputChangeCallback mTvInputChangeCallback;
    private ChannelDataManager mChannelDataManager;

    private TvView mSourceView;
    private SourceButton mSourceInput;

    private RelativeLayout mMainView;
    private SourceInputListLayout mSourceMenuLayout;
    private LinearLayout mSourceInfoLayout;
    private ChannelListLayout mChannelListLayout;

    private int mSigType;
    private boolean isMenuShowing;

    private volatile int mNoSignalShutdownCount = -1;
    private TextView mTimePromptText = null;

    //handler & message
    private Handler mHandler;
    private static final int MSG_UI_TIMEOUT              = 0;
    private static final int MSG_CHANNEL_NUM_SWITCH     = 1;
    private static final int DEFAULT_TIMEOUT             =5000;

    private int mUiType = Utils.UI_TYPE_ALL_HIDE;

    private static final int SIGNAL_GOT = 0;
    private static final int SIGNAL_NOT_GOT = 1;
    private static final int SIGNAL_SCRAMBLED = 2;
    private int mSignalState = SIGNAL_GOT;

    private static final int START_SETUP = 0;
    private static final int START_SETTING = 1;
    private boolean needUpdateSource = true;
    //if activity has been stopped, source input must be switched again.
    private boolean hasStopped = true;

    //info
    private TextView mInfoLabel;
    private TextView mInfoName;
    private TextView mInfoNumber;
    private int mPreSigType = -1;
    private boolean isNumberSwitching = false;
    private String keyInputNumber = "";

    //thread
    private HandlerThread mHandlerThread;
    private static final String mThreadName = TAG;
    private Handler mThreadHandler;
    private static final int MSG_SAVE_CHANNEL_INFO = 1;
    private boolean isPlayingRadio = false;

    private int mCurrentKeyType;
    private static final int IS_KEY_EXIT   = 0;
    private static final int IS_KEY_HOME   = 1;
    private static final int IS_KEY_OTHER  = 2;
    private boolean mSourceHasReleased = false;
    PowerManager.WakeLock mScreenLock = null;
    private AudioManager mAudioManager = null;

    private Toast toast = null;//use to show audio&subtitle track
    private boolean isToastShow = false;//use to show prompt

    BroadcastReceiver mReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DroidLogicTvUtils.ACTION_TIMEOUT_SUSPEND))
                reset_shutdown_time();
            else if (action.equals(DroidLogicTvUtils.ACTION_UPDATE_TV_PLAY)) {
                String channelNumber = intent.getStringExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER);
                if (channelNumber != null) {
                    Utils.logd(TAG, "delete or skipped current channel, switch to: name=" + mSourceInput.getChannelName()
                        + " uri=" + mSourceInput.getUri());
                    if (Integer.parseInt(channelNumber) >= 0)
                        processDeleteCurrentChannel(Integer.parseInt(channelNumber));
                    else
                        switchToSourceInput();
                } else {
                    String operation = intent.getStringExtra("tv_play_extra");
                    Utils.logd(TAG, "recevie intent : operation is " + operation);

                    if (!TextUtils.isEmpty(operation)) {
                        if (operation.equals("search_channel")) {
                            mMainView.setBackground(null);
                            isPlayingRadio = false;
                        } else if (operation.equals("mute"))
                            showMuteIcon(true);
                        else if (operation.equals("unmute"))
                            showMuteIcon(false);
                    }
                }
            } else if (action.equals(DroidLogicTvUtils.ACTION_SUBTITLE_SWITCH)) {
                int switchVal = intent.getIntExtra(DroidLogicTvUtils.EXTRA_SUBTITLE_SWITCH_VALUE, 0);
                if (switchVal == 1) {//on
                    List<TvTrackInfo> sTrackList = mSourceView.getTracks(TvTrackInfo.TYPE_SUBTITLE);
                    if (sTrackList.size() > 0) {
                        String def_lan = Settings.System.getString(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_DEFAULT_LANGUAGE);
                        for (TvTrackInfo track : sTrackList) {
                            if (track.getLanguage().equals(def_lan)) {
                                mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, track.getId());
                                return;
                            }
                        }
                        mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, sTrackList.get(0).getId());
                    }
                } else {
                    mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, null);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.logd(TAG, "==== onCreate ====");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
        initTimeSuspend(this);
        mScreenLock = ((PowerManager)this.getSystemService (Context.POWER_SERVICE)).newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        IntentFilter intentFilter = new IntentFilter(DroidLogicTvUtils.ACTION_TIMEOUT_SUSPEND);
        intentFilter.addAction(DroidLogicTvUtils.ACTION_UPDATE_TV_PLAY);
        intentFilter.addAction(DroidLogicTvUtils.ACTION_SUBTITLE_SWITCH);
        registerReceiver(mReceiver, intentFilter);
    }

    private void init() {
        mContext = getApplicationContext();
        mHandler = new Handler(this);

        initThread(mThreadName);
        mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);
        mTvInputChangeCallback = new TvInputChangeCallback();
        mTvInputManager.registerCallback(mTvInputChangeCallback, new Handler());
        mChannelDataManager = new ChannelDataManager(mContext);

        mTimePromptText = (TextView) findViewById(R.id.textView_time_prompt);
        mSourceView = (TvView) findViewById(R.id.source_view);
        mSourceView.setCallback(new TvViewInputCallback());

        mMainView = (RelativeLayout)findViewById(R.id.main_view);

        mSourceMenuLayout = (SourceInputListLayout)findViewById(R.id.menu_layout);
        mSourceMenuLayout.setOnSourceInputClickListener(this);
        mSourceInfoLayout = (LinearLayout)findViewById(R.id.info_layout);
        mChannelListLayout = (ChannelListLayout)findViewById(R.id.channel_list);
        mChannelListLayout.setOnChannelSelectListener(this);

        initSourceMenuLayout();
        setStartUpInfo();
    }

    private void setStartUpInfo() {
        final ContentResolver resolver = getContentResolver();
        String app_name = Settings.System.getString(resolver, "tv_start_up_app_name");
        if (TextUtils.isEmpty(app_name)) {
            Settings.System.putString(resolver, "tv_start_up_app_name", getComponentName().flattenToString());
        }
    }

    private void initThread(String name) {
        mHandlerThread = new HandlerThread(name);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_SAVE_CHANNEL_INFO:
                        saveDefaultChannelInfo();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private void releaseThread() {
        mHandlerThread.quit();
        mHandlerThread = null;
        mThreadHandler = null;
    }

    /**
     * must be invoked after {@link SourceButton.moveToChannel}.
     * set the background between {@link Channels#SERVICE_TYPE_AUDIO_VIDEO} and
     * {@link Channels#SERVICE_TYPE_AUDIO}.
     */
    private void initMainView() {
        if (mSourceInput == null)
            return;
        boolean is_audio = mSourceInput.isRadioChannel();
        Utils.logd(TAG, "==== isPlayingRadio =" + isPlayingRadio + ", is_audio=" + is_audio);
        if (!isPlayingRadio && is_audio) {
            try {
                mMainView.setBackground(getResources().getDrawable(R.drawable.bg_radio, null));
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            isPlayingRadio = true;
        } else if (isPlayingRadio && !is_audio) {
            mMainView.setBackground(null);
            isPlayingRadio = false;
        }
    }

    private void initSourceMenuLayout() {
        setDefaultChannelInfo();
        mSourceMenuLayout.refresh();
        mSourceInput = mSourceMenuLayout.getCurSourceInput();
    }

    /**
     * source may not be released when stopping tvapp, release it ahead of time.
     * release it when KEY_EXIT or KEY_HOME in {@link this#onPause()}
     */
    private void prepareForSourceRelease() {
        mCurrentKeyType = IS_KEY_HOME;
        mSourceHasReleased = false;
    }

    private void startPlay() {
        if (mSourceMenuLayout.getSourceCount() == 0)
            return;
        initMainView();
        if (hasStopped || needUpdateSource) {
            switchToSourceInput();
        }
        showUi(Utils.UI_TYPE_SOURCE_INFO, true);
    }

    @Override
    protected void onResume() {
        Utils.logd(TAG, "== onResume ====");
        closeTouchSound();
        closeScreenOffTimeout();

        mSourceView.setVisibility(View.VISIBLE);
        showUi(Utils.UI_TYPE_ALL_HIDE, false);
        startPlay();
        prepareForSourceRelease();
        if (hasStopped)
            reset_shutdown_time();
        hasStopped = false;
        isMenuShowing = false;
        needUpdateSource = true;
        if (mSignalState == SIGNAL_NOT_GOT)
            reset_nosignal_time();

        if (mAudioManager.isMasterMute())
            showMuteIcon(true);
        else
            showMuteIcon(false);

        super.onResume();
    }

    /**
     * get the default source input after {@code this.onCreate}.
     */
    private void setDefaultChannelInfo() {
        int device_id, atv_channel, dtv_channel;
        boolean is_radio;
        SharedPreferences sp = getSharedPreferences(SHARE_NAME, Context.MODE_PRIVATE);
        device_id = sp.getInt("device_id", 0);
        atv_channel = sp.getInt("atv_channel", -1);
        dtv_channel = sp.getInt("dtv_channel", -1);
        is_radio = sp.getBoolean("is_radio", false);
        mSourceMenuLayout.setDefaultSourceInfo(device_id, atv_channel, dtv_channel, is_radio);
    }

    /**
     * must be invoked after {@link SourceButton.moveToChannel}.
     * if there is nothing about channel switching in the tv.db, the {@code channel_uri}
     * used to tune must be wrong.
     */
    private void switchToSourceInput() {
        if (mSourceInput == null)
            return;
        mThreadHandler.obtainMessage(MSG_SAVE_CHANNEL_INFO).sendToTarget();
        mPreSigType = mSigType;
        mSigType = mSourceInput.getSigType();
        Uri channel_uri = mSourceInput.getUri();
        Utils.logd(TAG, "channelUri switching to is " + channel_uri);
        mSignalState = SIGNAL_GOT;
        mSourceView.tune(mSourceInput.getInputId(), channel_uri);
        showUi(Utils.UI_TYPE_SOURCE_INFO, true);
    }

    private void startSetupActivity () {
        if (mSourceInput == null)
            return;
        TvInputInfo info = mSourceInput.geTvInputInfo();
        Intent intent = info.createSetupIntent();
        if (intent != null) {
            intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_DEVICE_ID, mSourceInput.getDeviceId());
            intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, mSourceInput.getChannelNumber());
            intent.putExtra(DroidLogicTvUtils.EXTRA_IS_RADIO_CHANNEL, mSourceInput.isRadioChannel());
            startActivityForResult(intent, START_SETUP);
        }
    }

    private void startSettingActivity (int keycode) {
        if (mSourceInput == null)
            return;
        TvInputInfo info = mSourceInput.geTvInputInfo();
        Intent intent = info.createSettingsIntent();
        if (intent != null) {
            intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_DEVICE_ID, mSourceInput.getDeviceId());
            intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, mSourceInput.getChannelNumber());
            intent.putExtra(DroidLogicTvUtils.EXTRA_IS_RADIO_CHANNEL, mSourceInput.isRadioChannel());
            intent.putExtra(DroidLogicTvUtils.EXTRA_KEY_CODE, keycode);
            startActivityForResult(intent, START_SETTING);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Utils.logd(TAG, "====onActivityResult, requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == START_SETTING) {
            needUpdateSource = false;
            return;
        }
        if (resultCode == DroidLogicTvUtils.RESULT_OK) {
            needUpdateSource = false;
        } else if (resultCode == DroidLogicTvUtils.RESULT_UPDATE
                || resultCode == DroidLogicTvUtils.RESULT_FAILED) {
            needUpdateSource = true;
        }
    }

    /**
     * save channel number and clear something about pass through input.
     */
    private void preSwitchSourceInput() {
        if (mSourceInput == null)
            return;
        switch (mSigType) {
            case DroidLogicTvUtils.SIG_INFO_TYPE_HDMI:
                mSourceInput.setChannelVideoFormat("");
                break;
            case DroidLogicTvUtils.SIG_INFO_TYPE_AV:
                mSourceInput.setAVType("");
                break;
            default:
                break;
        }
    }

    @Override
    public void onSelect(int channelIndex, boolean isRadio) {
        if (mSourceInput.moveToChannel(channelIndex, isRadio)) {
            initMainView();
            switchToSourceInput();
        }
    }

    @Override
    public void onSourceInputClick() {
        Utils.logd(TAG, "==== onSourceInputClick ====");
        if (mSourceInput.getSourceType() == mSourceMenuLayout.getCurSourceInput().getSourceType()) {
            showUi(Utils.UI_TYPE_SOURCE_INFO, true);
            return;
        }
        preSwitchSourceInput();
        mSourceInput = mSourceMenuLayout.getCurSourceInput();
        initMainView();
        switchToSourceInput();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean ret = processKeyEvent(event.getKeyCode(), event);
        return ret ? ret : super.dispatchKeyEvent(event);
    }

    private boolean processKeyEvent(int keyCode, KeyEvent event) {
        Utils.logd(TAG, "====keycode =" + keyCode);

        if (mSignalState == SIGNAL_NOT_GOT)
            reset_nosignal_time();

        switch (mUiType ) {
            case Utils.UI_TYPE_SOURCE_LIST:
            case Utils.UI_TYPE_ATV_CHANNEL_LIST:
            case Utils.UI_TYPE_DTV_CHANNEL_LIST:
            case Utils.UI_TYPE_ATV_FAV_LIST:
            case Utils.UI_TYPE_DTV_FAV_LIST:
            mHandler.removeMessages(MSG_UI_TIMEOUT);
            mHandler.sendEmptyMessageDelayed(MSG_UI_TIMEOUT, DEFAULT_TIMEOUT);
            break;
        }

        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        switch (keyCode) {
            case KeyEvent.KEYCODE_TV_INPUT:
                if (!down)
                    return true;

                showUi(Utils.UI_TYPE_SOURCE_LIST, false);
                return true;
            case KeyEvent.KEYCODE_MENU://show setup activity
                if (!down)
                    return true;

                showUi(Utils.UI_TYPE_ALL_HIDE, false);
                mCurrentKeyType = IS_KEY_OTHER;
                isMenuShowing = true;
                startSetupActivity();
                return true;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE:
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_3DMODE:
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE:
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE:
            case DroidLogicKeyEvent.KEYCODE_TV_SLEEP:
            case DroidLogicKeyEvent.KEYCODE_GUIDE:
                if (!down)
                    return true;

                showUi(Utils.UI_TYPE_ALL_HIDE, false);
                mCurrentKeyType = IS_KEY_OTHER;
                startSettingActivity(keyCode);
                return true;
            case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK:
                if (!down)
                    return true;

                if (mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_DTV) {
                    doTrackKey(TvTrackInfo.TYPE_AUDIO);
                }
                return true;
            case KeyEvent.KEYCODE_CAPTIONS:
                if (!down)
                    return true;

                if (mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_DTV) {
                    doTrackKey(TvTrackInfo.TYPE_SUBTITLE);
                }
                return true;
            case DroidLogicKeyEvent.KEYCODE_FAV:
                if (!down)
                    return true;

                if (mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_ATV)
                    showUi(Utils.UI_TYPE_ATV_FAV_LIST, false);
                else if (mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_DTV)
                    showUi(Utils.UI_TYPE_DTV_FAV_LIST, false);
                return true;
            case DroidLogicKeyEvent.KEYCODE_LIST:
                if (!down)
                    return true;

                if (mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_ATV)
                    showUi(Utils.UI_TYPE_ATV_CHANNEL_LIST, false);
                else if (mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_DTV)
                    showUi(Utils.UI_TYPE_DTV_CHANNEL_LIST, false);
                return true;
            case KeyEvent.KEYCODE_INFO:
                if (!down)
                    return true;

                showUi(Utils.UI_TYPE_SOURCE_INFO, false);
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (!down)
                    return true;

                switch (mUiType ) {
                    case Utils.UI_TYPE_SOURCE_LIST:
                    case Utils.UI_TYPE_ATV_CHANNEL_LIST:
                    case Utils.UI_TYPE_DTV_CHANNEL_LIST:
                    case Utils.UI_TYPE_ATV_FAV_LIST:
                    case Utils.UI_TYPE_DTV_FAV_LIST:
                    showUi(Utils.UI_TYPE_SOURCE_INFO, true);
                    break;
                }
                return true;
            case KeyEvent.KEYCODE_CHANNEL_UP:
                if (!down)
                    return true;

                if (event.getRepeatCount() == 0) {
                    processKeyInputChannel(1);
                } else {
                    processkeyLongPressChannel(event.getRepeatCount() + 1);
                }
                return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                if (!down)
                    return true;

                if (event.getRepeatCount() == 0) {
                    processKeyInputChannel(-1);
                } else {
                    processkeyLongPressChannel(-(event.getRepeatCount() + 1));
                }
                return true;
            case KeyEvent.KEYCODE_LAST_CHANNEL:
                if (!down)
                    return true;

                processKeyLookBack();
                return true;
            case KeyEvent.KEYCODE_0:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
                if (!down)
                    return true;

                processNumberInputChannel(keyCode);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (!down)
                    return true;

                if (mAudioManager.isMasterMute()) {
                    mAudioManager.setMasterMute(false, AudioManager.FLAG_PLAY_SOUND);
                    showMuteIcon(false);
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                if (!down)
                    return true;

                if (mAudioManager.isMasterMute()) {
                    mAudioManager.setMasterMute(false, AudioManager.FLAG_PLAY_SOUND);
                    showMuteIcon(false);
                } else {
                    mAudioManager.setMasterMute(true, AudioManager.FLAG_PLAY_SOUND);
                    showMuteIcon(true);
                }
                return true;
            default:
                break;
        }
        return false;
    }

    private void doTrackKey(int type) {
        if (type == TvTrackInfo.TYPE_SUBTITLE) {
            List<TvTrackInfo> sTrackList = mSourceView.getTracks(TvTrackInfo.TYPE_SUBTITLE);
            if (sTrackList != null && sTrackList.size() != 0) {
                String subtitleTrackId = mSourceView.getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE);
                if (!isToastShow && subtitleTrackId == null) {
                    showCustomToast(getResources().getString(R.string.subtitle), getResources().getString(R.string.off));
                    return;
                }

                int sTrackIndex = 0;
                for (sTrackIndex = 0;sTrackIndex < sTrackList.size();sTrackIndex++) {
                    if (sTrackList.get(sTrackIndex).getId().equals(subtitleTrackId)) {
                        break;
                    }
                }

                if (isToastShow) {
                    sTrackIndex ++;
                    if (subtitleTrackId == null)
                        sTrackIndex = 0;
                    if (sTrackIndex == 0) {
                        mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, sTrackList.get(sTrackIndex).getId());
                        showCustomToast(getResources().getString(R.string.subtitle), sTrackList.get(sTrackIndex).getLanguage());
                        return;
                    }
                    if (sTrackIndex == sTrackList.size()) {
                        mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, null);
                        showCustomToast(getResources().getString(R.string.subtitle), getResources().getString(R.string.off));
                        return;
                    }
                    mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, sTrackList.get(sTrackIndex).getId());
                    showCustomToast(getResources().getString(R.string.subtitle), sTrackList.get(sTrackIndex).getLanguage());
                } else {
                    showCustomToast(getResources().getString(R.string.subtitle), sTrackList.get(sTrackIndex).getLanguage());
                }
            }
            else
                showCustomToast(getResources().getString(R.string.subtitle), getResources().getString(R.string.no));
        } else if (type == TvTrackInfo.TYPE_AUDIO) {
            String audioTrackId = mSourceView.getSelectedTrack(TvTrackInfo.TYPE_AUDIO);
            if (audioTrackId != null) {
                List<TvTrackInfo> aTrackList = mSourceView.getTracks(TvTrackInfo.TYPE_AUDIO);
                int aTrackIndex = 0;
                for (aTrackIndex = 0;aTrackIndex < aTrackList.size();aTrackIndex++) {
                    if (aTrackList.get(aTrackIndex).getId().equals(audioTrackId)) {
                        break;
                    }
                }
                if (isToastShow) {
                    aTrackIndex = (aTrackIndex + 1) % aTrackList.size();
                    mSourceView.selectTrack(TvTrackInfo.TYPE_AUDIO, aTrackList.get(aTrackIndex).getId());
                }
                showCustomToast(getResources().getString(R.string.audio_track), aTrackList.get(aTrackIndex).getLanguage());
            }
            else
                showCustomToast(getResources().getString(R.string.audio_track), getResources().getString(R.string.no));
        }
    }

    public void processKeyInputChannel(int offset) {
        if (mSourceInput.isPassthrough())
            return;

        if (mSourceInput.moveToOffset(offset))
            switchToSourceInput();
    }

    private void processkeyLongPressChannel(int offset) {
        if (mSourceInput.isPassthrough())
            return;

        int index = mSourceInput.getChannelIndex();
        int size = 0;
        mHandler.removeMessages(MSG_CHANNEL_NUM_SWITCH);
        isNumberSwitching = true;
        if (mSourceInput.isRadioChannel()) {
            size = mSourceInput.getChannelRadioList().size();
            if (size == 0)
                return;
        } else {
             size = mSourceInput.getChannelVideoList().size();
            if (size == 0)
                return;
        }

        if (offset > 0)
            keyInputNumber = Integer.toString((index + offset) % size);
        else
            keyInputNumber = Integer.toString((size + (index + offset) % size) % size);

        showUi(Utils.UI_TYPE_SOURCE_INFO, true);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CHANNEL_NUM_SWITCH), 300);
    }

    private void processNumberInputChannel(int keyCode) {
        if (mSourceInput.isPassthrough())
            return;

        mHandler.removeMessages(MSG_CHANNEL_NUM_SWITCH);
        isNumberSwitching = true;
        int val = keyCode - DroidLogicKeyEvent.KEYCODE_0;
        if (keyInputNumber.length() <= 8)
            keyInputNumber = keyInputNumber + val;
        showUi(Utils.UI_TYPE_SOURCE_INFO, true);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CHANNEL_NUM_SWITCH), 2000);
    }

    public void processKeyLookBack() {
        if (mSourceInput.moveToRecentChannel())
            switchToSourceInput();
    }

    public void processDeleteCurrentChannel(int number) {
        if (mSourceInput.moveToIndex(number))
            switchToSourceInput();
    }

    private void inflateCurrentInfoLayout() {
        if (!(mSourceInfoLayout.getChildCount() == 0 || mPreSigType != mSigType))
            return;
        mSourceInfoLayout.removeAllViews();
        mInfoNumber = null;
        LayoutInflater inflate = LayoutInflater.from(mContext);
        switch (mSigType) {
            case DroidLogicTvUtils.SIG_INFO_TYPE_ATV:
            case DroidLogicTvUtils.SIG_INFO_TYPE_DTV:
                mSourceInfoLayout.addView(inflate.inflate(R.layout.atv_dtv_info,
                        mSourceInfoLayout, false));
                mInfoLabel = (TextView) findViewById(R.id.ad_info_name);
                mInfoNumber = (TextView) findViewById(R.id.ad_info_number);
                mInfoName = (TextView) findViewById(R.id.ad_info_value);
                break;
            case DroidLogicTvUtils.SIG_INFO_TYPE_AV:
            case DroidLogicTvUtils.SIG_INFO_TYPE_HDMI:
                mSourceInfoLayout.addView(inflate.inflate(R.layout.hdmi_av_info,
                        mSourceInfoLayout, false));
                mInfoLabel = (TextView) findViewById(R.id.ha_info_name);
                mInfoName = (TextView) findViewById(R.id.ha_info_value);
                break;
            default:
                mSourceInfoLayout.addView(inflate.inflate(R.layout.atv_dtv_info,
                        mSourceInfoLayout, false));
                mInfoLabel = (TextView) findViewById(R.id.ad_info_name);
                mInfoNumber = (TextView) findViewById(R.id.ad_info_number);
                mInfoName = (TextView) findViewById(R.id.ad_info_value);
                break;
        }
    }

    private void initOtherInfo() {
        if (mSourceInput == null)
            return;
        inflateCurrentInfoLayout();
        mInfoLabel.setText(mSourceInput.getSourceLabel());
        mInfoNumber.setText(mSourceInput.getChannelNumber());
        mInfoName.setText(mSourceInput.getChannelName());
    }

    private void initATVInfo() {
        if (mSourceInput == null)
            return;
        inflateCurrentInfoLayout();
        mInfoLabel.setText(mSourceInput.getSourceLabel());
        if (isNumberSwitching) {
            mInfoNumber.setText(keyInputNumber);
            mInfoName.setText("");
        } else {
            int index = mSourceInput.getChannelIndex();
            mInfoNumber.setText(index != -1 ? Integer.toString(index) : "");
            mInfoName.setText(mSourceInput.getChannelType());
        }
    }

    private void initDTVInfo() {
        if (mSourceInput == null)
            return;
        inflateCurrentInfoLayout();
        mInfoLabel.setText(mSourceInput.getSourceLabel());
        if (isNumberSwitching) {
            mInfoNumber.setText(keyInputNumber);
            mInfoName.setText("");
        } else {
            int index = mSourceInput.getChannelIndex();
            mInfoNumber.setText(index != -1 ? Integer.toString(index) : "");
            mInfoName.setText(mSourceInput.getChannelName());
        }
    }

    private void initAVInfo() {
        if (mSourceInput == null)
            return;
        inflateCurrentInfoLayout();
        mInfoLabel.setText(mSourceInput.getSourceLabel());
        if (mSignalState == SIGNAL_NOT_GOT) {
            mInfoName.setText("");
        } else {
            mInfoName.setText(mSourceInput.getAVType());
        }
    }

    private void initHmdiInfo() {
        if (mSourceInput == null)
            return;
        inflateCurrentInfoLayout();
        mInfoLabel.setText(mSourceInput.getSourceLabel());
        if (mSignalState == SIGNAL_NOT_GOT) {
            mInfoName.setText("");
        } else {
            mInfoName.setText(mSourceInput.getChannelVideoFormat());
        }
    }

    private void showUi (int type, boolean forceShow) {
        TextView prompt_no_signal = (TextView)findViewById(R.id.no_signal);

        switch (type) {
            case Utils.UI_TYPE_SOURCE_INFO:
                mHandler.removeMessages(MSG_UI_TIMEOUT);
                if (forceShow || mSourceInfoLayout.getVisibility() != View.VISIBLE) {
                    switch (mSigType) {
                        case DroidLogicTvUtils.SIG_INFO_TYPE_ATV:
                            initATVInfo();
                            break;
                        case DroidLogicTvUtils.SIG_INFO_TYPE_DTV:
                            initDTVInfo();
                            break;
                        case DroidLogicTvUtils.SIG_INFO_TYPE_AV:
                            initAVInfo();
                            break;
                        case DroidLogicTvUtils.SIG_INFO_TYPE_HDMI:
                            initHmdiInfo();
                            break;
                        default:
                            initOtherInfo();
                            break;
                    }
                    mSourceMenuLayout.setVisibility(View.INVISIBLE);
                    prompt_no_signal.setVisibility(View.INVISIBLE);
                    mChannelListLayout.setVisibility(View.INVISIBLE);

                    mSourceInfoLayout.setVisibility(View.VISIBLE);
                    mSourceInfoLayout.requestLayout();
                    mUiType = type;

                    mHandler.sendEmptyMessageDelayed(MSG_UI_TIMEOUT, DEFAULT_TIMEOUT);
                }else {
                    mHandler.sendEmptyMessage(MSG_UI_TIMEOUT);
                }
                break;
            case Utils.UI_TYPE_SOURCE_LIST:
                mHandler.removeMessages(MSG_UI_TIMEOUT);
                if (forceShow || mSourceMenuLayout.getVisibility() != View.VISIBLE) {
                    mSourceInfoLayout.setVisibility(View.INVISIBLE);
                    prompt_no_signal.setVisibility(View.INVISIBLE);
                    mChannelListLayout.setVisibility(View.INVISIBLE);

                    mSourceMenuLayout.setVisibility(View.VISIBLE);
                    mSourceMenuLayout.requestLayout();
                    mSourceInput.requestFocus();
                    mUiType = type;

                    mHandler.sendEmptyMessageDelayed(MSG_UI_TIMEOUT, DEFAULT_TIMEOUT);
                } else {
                    mHandler.sendEmptyMessage(MSG_UI_TIMEOUT);
                }
                break;
            case Utils.UI_TYPE_ATV_CHANNEL_LIST:
            case Utils.UI_TYPE_DTV_CHANNEL_LIST:
            case Utils.UI_TYPE_ATV_FAV_LIST:
            case Utils.UI_TYPE_DTV_FAV_LIST:
                mHandler.removeMessages(MSG_UI_TIMEOUT);
                if (forceShow || type != mChannelListLayout.getType()
                        || mChannelListLayout.getVisibility() != View.VISIBLE) {
                    switch (type) {
                        case Utils.UI_TYPE_ATV_CHANNEL_LIST:
                        case Utils.UI_TYPE_ATV_FAV_LIST:
                            mChannelListLayout.initView(type, mSourceInput.getChannelVideoList());
                            break;
                        case Utils.UI_TYPE_DTV_CHANNEL_LIST:
                        case Utils.UI_TYPE_DTV_FAV_LIST:
                            mChannelListLayout.initView(type, mSourceInput.getChannelVideoList(),
                                    mSourceInput.getChannelRadioList());
                            break;
                        default:
                            break;
                    }
                    mSourceMenuLayout.setVisibility(View.INVISIBLE);
                    mSourceInfoLayout.setVisibility(View.INVISIBLE);
                    prompt_no_signal.setVisibility(View.INVISIBLE);

                    mChannelListLayout.setVisibility(View.VISIBLE);
                    mUiType = type;
                    mHandler.sendEmptyMessageDelayed(MSG_UI_TIMEOUT, DEFAULT_TIMEOUT);
                } else {
                    mHandler.sendEmptyMessage(MSG_UI_TIMEOUT);
                }
                break;
            case Utils.UI_TYPE_NO_SINAL:
                if (mSignalState == SIGNAL_SCRAMBLED)
                    prompt_no_signal.setText(mContext.getResources().getString(R.string.av_scambled));
                else if (mSignalState == SIGNAL_NOT_GOT)
                    prompt_no_signal.setText(mContext.getResources().getString(R.string.no_signal));
                else {
                    prompt_no_signal.setVisibility(View.INVISIBLE);
                    return;
                }
                if (prompt_no_signal.getVisibility() != View.VISIBLE
                    && mSourceMenuLayout.getVisibility() != View.VISIBLE
                    && mSourceInfoLayout.getVisibility() != View.VISIBLE
                    && mChannelListLayout.getVisibility() != View.VISIBLE
                    && !isToastShow && !isMenuShowing) {
                    prompt_no_signal.setVisibility(View.VISIBLE);
                    prompt_no_signal.requestLayout();
                    mUiType = type;
                }
                break;
            case Utils.UI_TYPE_ALL_HIDE:
                mHandler.removeMessages(MSG_UI_TIMEOUT);
                mChannelListLayout.setVisibility(View.INVISIBLE);
                mSourceMenuLayout.setVisibility(View.INVISIBLE);
                mSourceInfoLayout.setVisibility(View.INVISIBLE);
                prompt_no_signal.setVisibility(View.INVISIBLE);
                mUiType = type;
                break;
        }
    }

    private void showMuteIcon (boolean isShow) {
        View icon_mute = findViewById(R.id.image_mute);
        if (isShow)
            icon_mute.setVisibility(View.VISIBLE);
        else
            icon_mute.setVisibility(View.GONE);
    }

    private void processSessionEvent(String inputId, String eventType, Bundle eventArgs) {
        if (eventType.equals(DroidLogicTvUtils.SIG_INFO_EVENT)) {//sig_info
            mSigType = mSourceInput.getSigType();
            String args = "";

            switch (mSigType) {
                case DroidLogicTvUtils.SIG_INFO_TYPE_HDMI:
                    args = eventArgs.getString(DroidLogicTvUtils.SIG_INFO_ARGS);
                    String[] temp = args.split("_");
                    mSourceInput.setChannelVideoFormat(temp[0] + "_" + temp[1]);
                    break;
                case DroidLogicTvUtils.SIG_INFO_TYPE_AV:
                    args = eventArgs.getString(DroidLogicTvUtils.SIG_INFO_ARGS);
                    mSourceInput.setAVType(args);
                    break;
                default:
                    break;
            }
            showUi(Utils.UI_TYPE_SOURCE_INFO, true);
        } else if (eventType.equals(DroidLogicTvUtils.AV_SIG_SCRAMBLED)) {
            mSignalState = SIGNAL_SCRAMBLED;
            showUi(Utils.UI_TYPE_NO_SINAL, false);
        }
    }

    /**
     * release something for next resume or destroy. e.g, if exit with home key, clear info which
     * is unknown when next resume for pass through.
     * clear info for pass through, and release session.
     */
    private void releaseBeforeExit() {
        preSwitchSourceInput();
        mSourceHasReleased = true;
    }

    @Override
    protected void onPause() {
        Utils.logd(TAG, "==== onPause ====" + mCurrentKeyType);
        if (mCurrentKeyType == IS_KEY_EXIT || mCurrentKeyType == IS_KEY_HOME) {
            releaseBeforeExit();
        }
        // search is longer then 5min
        remove_nosignal_time();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Utils.logd(TAG, "==== onStop ====");
        if (toast != null)
            toast.cancel();
        hasStopped = true;
        if (!mSourceHasReleased) {
            releaseBeforeExit();
        }
        mSourceView.setVisibility(View.GONE);
        restoreTouchSound();
        openScreenOffTimeout();
        remove_shutdown_time();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Utils.logd(TAG, "==== onDestroy ====");
        mTvInputManager.unregisterCallback(mTvInputChangeCallback);
        releaseThread();
        unregisterReceiver(mReceiver);
        mChannelDataManager.release();
        super.onDestroy();
    }

    private void saveDefaultChannelInfo() {
        if (mSourceInput.getSourceType() == DroidLogicTvUtils.SOURCE_TYPE_OTHER)
            return;
        SharedPreferences sp = getSharedPreferences(SHARE_NAME, Context.MODE_PRIVATE);
        Editor edit = sp.edit();
        edit.putInt("device_id", mSourceInput.getDeviceId());

        int index = mSourceInput.getChannelIndex();
        if (index < 0) {
            edit.commit();
            return;
        }
        int type = mSourceInput.getSourceType();
        boolean is_radio = mSourceInput.isRadioChannel();
        if (type == DroidLogicTvUtils.SOURCE_TYPE_ATV) {
            edit.putInt("atv_channel", index);
        } else if (type == DroidLogicTvUtils.SOURCE_TYPE_DTV) {
            edit.putInt("dtv_channel", index);
            edit.putBoolean("is_radio", is_radio);
        }
        edit.commit();
    }

    @Override
    public boolean handleMessage(Message msg) {
        int max_counter;
        switch (msg.what) {
            case MSG_UI_TIMEOUT:
                switch (mUiType) {
                    case Utils.UI_TYPE_SOURCE_INFO:
                        mSourceInfoLayout.setVisibility(View.INVISIBLE);
                        showUi(Utils.UI_TYPE_NO_SINAL, false);
                        break;
                    case Utils.UI_TYPE_SOURCE_LIST:
                        showUi(Utils.UI_TYPE_SOURCE_INFO, true);
                        break;
                    case Utils.UI_TYPE_ATV_CHANNEL_LIST:
                    case Utils.UI_TYPE_DTV_CHANNEL_LIST:
                    case Utils.UI_TYPE_ATV_FAV_LIST:
                    case Utils.UI_TYPE_DTV_FAV_LIST:
                        showUi(Utils.UI_TYPE_SOURCE_INFO, true);
                        break;
                    case Utils.UI_TYPE_NO_SINAL:
                        break;
                }
                break;
            case MSG_CHANNEL_NUM_SWITCH:
                if (mSourceInput.moveToIndex(Integer.parseInt(keyInputNumber))) {
                    switchToSourceInput();
                }
                isNumberSwitching = false;
                keyInputNumber = "";
                showUi(Utils.UI_TYPE_SOURCE_INFO, true);
                break;
            default:
                break;
        }
        return false;
    }

    private final class TvInputChangeCallback extends TvInputManager.TvInputCallback {

        @Override
        public void onInputAdded(String inputId) {
            Utils.logd(TAG, "==== onInputAdded, inputId=" + inputId);
            int input_need_reset = mSourceMenuLayout.add(inputId);
            Utils.logd(TAG, "==== input_need_reset=" + input_need_reset);
            if (input_need_reset == SourceInputListLayout.ACTION_FAILED)
                return;

            if (mSourceMenuLayout.getVisibility() == View.VISIBLE) {
                showUi(Utils.UI_TYPE_SOURCE_LIST, true);
            }
            if (input_need_reset == SourceInputListLayout.INPUT_NEED_RESET) {
                preSwitchSourceInput();
                mSourceInput = mSourceMenuLayout.getCurSourceInput();
                startPlay();
            }
        }

        @Override
        public void onInputRemoved(String inputId) {
            Utils.logd(TAG, "==== onInputRemoved, inputId=" + inputId);
            int input_need_reset = mSourceMenuLayout.remove(inputId);
            Utils.logd(TAG, "==== input_need_reset=" + input_need_reset);
            if (input_need_reset == SourceInputListLayout.ACTION_FAILED)
                return;

            if (mSourceMenuLayout.getVisibility() == View.VISIBLE) {
                showUi(Utils.UI_TYPE_SOURCE_LIST, true);
            }
            if (input_need_reset == SourceInputListLayout.INPUT_NEED_RESET) {
                preSwitchSourceInput();
                mSourceInput = mSourceMenuLayout.getCurSourceInput();
                startPlay();
            }
        }

        @Override
        public void onInputStateChanged(String inputId, int state) {
            Utils.logd(TAG, "==== onInputStateChanged, inputId=" + inputId + ", state=" + state);
            int input_need_reset =  mSourceMenuLayout.stateChange(inputId, state);
            Utils.logd(TAG, "==== input_need_reset=" + input_need_reset);
            if (input_need_reset == SourceInputListLayout.ACTION_FAILED)
                return;

            if (mSourceMenuLayout.getVisibility() == View.VISIBLE) {
                showUi(Utils.UI_TYPE_SOURCE_LIST, true);
            }
            if (input_need_reset == SourceInputListLayout.INPUT_NEED_RESET) {
                preSwitchSourceInput();
                mSourceInput = mSourceMenuLayout.getCurSourceInput();
                startPlay();
            }
        }

        @Override
        public void onInputUpdated(String inputId) {
            Utils.logd(TAG, "==== onInputUpdated, inputId=" + inputId);
            int input_need_reset =  mSourceMenuLayout.update(inputId);
            Utils.logd(TAG, "==== input_need_reset=" + input_need_reset);
            if (input_need_reset == SourceInputListLayout.ACTION_FAILED)
                return;

            if (mSourceMenuLayout.getVisibility() == View.VISIBLE) {
                showUi(Utils.UI_TYPE_SOURCE_LIST, true);
            }
            if (input_need_reset == SourceInputListLayout.INPUT_NEED_RESET) {
                preSwitchSourceInput();
                mSourceInput = mSourceMenuLayout.getCurSourceInput();
                startPlay();
            }
        }
    }

    private final class TvViewInputCallback extends TvView.TvInputCallback {

        @Override
        public void onEvent(String inputId, String eventType, Bundle eventArgs) {
            Utils.logd(TAG, "====onEvent==inputId =" + inputId +", ===eventType ="+ eventType);
            processSessionEvent(inputId, eventType, eventArgs);
        }

        @Override
        public void onVideoAvailable(String inputId) {
            Utils.logd(TAG, "====onVideoAvailable==inputId =" + inputId);

            mSignalState = SIGNAL_GOT;
            showUi(Utils.UI_TYPE_NO_SINAL, false);
            remove_nosignal_time();
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            Utils.logd(TAG, "====onVideoUnavailable==inputId =" + inputId +", ===reason ="+ reason);
            switch (reason) {
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN:
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING:
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING:
                    if (mSignalState != SIGNAL_NOT_GOT)
                        reset_nosignal_time();
                    mSignalState = SIGNAL_NOT_GOT;
                    showUi(Utils.UI_TYPE_NO_SINAL, false);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onTracksChanged(String inputId, List<TvTrackInfo> tracks) {
            if (tracks == null)
                return;
            List<TvTrackInfo> subTracks = new ArrayList<TvTrackInfo>();
            for (TvTrackInfo track : tracks) {
                if (track.getType() == TvTrackInfo.TYPE_SUBTITLE)
                    subTracks.add(track);
            }
            int switchVal = Settings.System.getInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_SUBTITLE_SWITCH, 0);
            String def_lan = Settings.System.getString(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_DEFAULT_LANGUAGE);
            if (switchVal == 0)
                mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, null);
            else {
                for (TvTrackInfo track : subTracks) {
                    if (track.getLanguage().equals(def_lan)) {
                        mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, track.getId());
                        return;
                    }
                }
                mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, (subTracks.size() == 0 ? null : subTracks.get(0).getId()));
            }
        }
    }

    private Handler no_signal_handler = new Handler();
    private Runnable no_signal_runnable = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mNoSignalShutdownCount--;
                        if (mNoSignalShutdownCount == 0) {
                            long now = SystemClock.uptimeMillis();
                            KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, DroidLogicKeyEvent.KEYCODE_POWER, 0);
                            KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, DroidLogicKeyEvent.KEYCODE_POWER, 0);
                            InputManager.getInstance().injectInputEvent(down, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
                            InputManager.getInstance().injectInputEvent(up, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
                        }
                        else {
                            if (mNoSignalShutdownCount < 60) {
                                String str = mNoSignalShutdownCount + " " + getResources().getString(R.string.auto_shutdown_info);
                                mTimePromptText.setText(str);
                                if (mTimePromptText.getVisibility() != View.VISIBLE)// if sleep time,no show view
                                    mTimePromptText.setVisibility(View.VISIBLE);
                            } else {
                                if (mTimePromptText.getVisibility() == View.VISIBLE)
                                    mTimePromptText.setVisibility(View.GONE);
                            }
                            no_signal_handler.postDelayed(no_signal_runnable, 1000);
                        }
                    }
                    catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    };

    private void reset_nosignal_time() {
        mNoSignalShutdownCount = 300;//5min
        no_signal_handler.removeCallbacks(no_signal_runnable);
        no_signal_handler.postDelayed(no_signal_runnable, 0);
    }

    private void remove_nosignal_time() {
        if (mTimePromptText.getVisibility() == View.VISIBLE)
            mTimePromptText.setVisibility(View.GONE);
        no_signal_handler.removeCallbacks(no_signal_runnable);
    }

    /* time suspend dialog */
    private AlertDialog sdialog;//use to dismiss
    private AlertDialog.Builder suspendDialog;
    private View suspendDialogView;
    private TextView countDownView;
    private int mSuspendCount = 0;

    private void initTimeSuspend(Context context) {
        suspendDialog = new AlertDialog.Builder(context);
        suspendDialog.setTitle(getString(R.string.suspend_dialog_title));
        suspendDialog.setPositiveButton(getString(R.string.cancel_suspend), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                remove_shutdown_time();
            }
        });
        suspendDialog.setOnKeyListener(new OnKeyListener()
        {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
            {
                // TODO Auto-generated method stub
                if (keyCode == DroidLogicKeyEvent.KEYCODE_DPAD_CENTER)
                    return false;
                else
                    return true;
            }
        });
    }

    private Handler timeSuspend_handler = new Handler();
    private Runnable timeSuspend_runnable = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mSuspendCount--;
                        if (mSuspendCount == 0) {
                            remove_shutdown_time();
                            long now = SystemClock.uptimeMillis();
                            KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, DroidLogicKeyEvent.KEYCODE_POWER, 0);
                            KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, DroidLogicKeyEvent.KEYCODE_POWER, 0);
                            InputManager.getInstance().injectInputEvent(down, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
                            InputManager.getInstance().injectInputEvent(up, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
                        }
                        else {
                            if (mSuspendCount == 60) {
                                String str = mSuspendCount + " " + getResources().getString(R.string.countdown_tips);
                                suspendDialogView = View.inflate(getApplicationContext(), R.layout.timesuspend_dialog, null);
                                countDownView = (TextView) suspendDialogView.findViewById(R.id.tv_dialog);
                                countDownView.setText(str);
                                suspendDialog.setView(suspendDialogView);
                                sdialog = suspendDialog.show();
                            } else if (mSuspendCount < 60){
                                String str = mSuspendCount + " " + getResources().getString(R.string.countdown_tips);
                                countDownView.setText(str);
                            }
                            timeSuspend_handler.postDelayed(timeSuspend_runnable, 1000);
                        }
                    }
                    catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private void reset_shutdown_time() {
        int sleepTime = SystemProperties.getInt("tv.sleep_timer", 0);
        mSuspendCount = sleepTime * 60;
        if (mSuspendCount > 0) {
            timeSuspend_handler.removeCallbacks(timeSuspend_runnable);
            timeSuspend_handler.postDelayed(timeSuspend_runnable, 0);
        }
    }

    private void remove_shutdown_time() {
        if (sdialog != null)
            sdialog.dismiss();
        SystemProperties.set("tv.sleep_timer", "0");
        timeSuspend_handler.removeCallbacks(timeSuspend_runnable);
    }

    private int save_system_sound = -1;
    private void closeTouchSound() {
        save_system_sound = Settings.System.getInt(getApplicationContext().getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
        Settings.System.putInt(getApplicationContext().getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
        mAudioManager.unloadSoundEffects();
    }

    private void restoreTouchSound() {
        Settings.System.putInt(getApplicationContext().getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, save_system_sound);
        if (save_system_sound != 0) {
            mAudioManager.loadSoundEffects();
        } else {
            mAudioManager.unloadSoundEffects();
        }
    }

    protected void closeScreenOffTimeout() {
        if (mScreenLock.isHeld() == false) {
            mScreenLock.acquire();
        }
    }

    protected void openScreenOffTimeout() {
        if (mScreenLock.isHeld() == true) {
            mScreenLock.release();
        }
    }

    private void showCustomToast(String titleStr, String statusStr) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.layout_hotkey, null);

        TextView title =(TextView)layout.findViewById(R.id.toast_title);
        TextView status =(TextView)layout.findViewById(R.id.toast_status);

        title.setText(titleStr);
        status.setText(statusStr);

        if (toast == null) {
            toast = new Toast(this);
            toast.setDuration(3000);
            toast.setGravity(Gravity.CENTER_VERTICAL, 400, 300);
        }
        toast.setView(layout);
        View view = toast.getView();
        view.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            @Override
            public void onViewDetachedFromWindow(View v) {
                showUi(Utils.UI_TYPE_NO_SINAL, false);
                isToastShow = false;
            }

            @Override
            public void onViewAttachedToWindow(View v) {
                showUi(Utils.UI_TYPE_ALL_HIDE, false);
                isToastShow = true;
            }
        });
        toast.show();
    }
}
