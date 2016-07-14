package com.droidlogic.tvsource;


import java.util.ArrayList;
import java.util.List;

import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.Program;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.app.tv.TVTime;
import com.droidlogic.app.tv.ChannelInfo;

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
import android.content.res.Resources.NotFoundException;
import android.hardware.input.InputManager;
import android.hardware.hdmi.HdmiTvClient;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiControlManager;
import android.media.AudioManager;
import android.media.AudioSystem;
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
import android.util.ArrayMap;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View.OnAttachStateChangeListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class DroidLogicTv extends Activity implements Callback, onSourceInputClickListener, OnChannelSelectListener {
    private static final String TAG = "DroidLogicTv";
    private static final String SHARE_NAME = "tv_app";

    public static final String PROP_TV_PREVIEW = "tv.is.preview.window";

    private Context mContext;
    private TvInputManager mTvInputManager;
    private TvInputChangeCallback mTvInputChangeCallback;
    private ChannelDataManager mChannelDataManager;
    private TvDataBaseManager mTvDataBaseManager;
    private HdmiTvClient mHdmiTvClient = null;

    private FrameLayout mRootView;
    private TvViewInputCallback mTvViewCallback = new TvViewInputCallback();
    private TvView mSourceView;
    private SourceButton mSourceInput;

    private RelativeLayout mMainView;
    private SourceInputListLayout mSourceMenuLayout;
    private LinearLayout mSourceInfoLayout;
    private ChannelListLayout mChannelListLayout;
    private TextView prompt_no_signal;

    private int mSigType = 0;
    private boolean isMenuShowing;
    private String mSourceInputId = null;

    private volatile int mNoSignalShutdownCount = -1;
    private TextView mTimePromptText = null;

    //handler & message
    private Handler mHandler;
    private static final int MSG_UI_TIMEOUT              = 0;
    private static final int MSG_CHANNEL_NUM_SWITCH     = 1;
    private static final int MSG_TUNE                     = 2;

    private static final int DEFAULT_TIMEOUT             = 5000;

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
    private boolean isSearchingChannel = false;

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

    private static final int IS_KEY_EXIT   = 0;
    private static final int IS_KEY_HOME   = 1;
    private static final int IS_KEY_OTHER  = 2;
    private PowerManager.WakeLock mScreenLock = null;
    private Object mLock = new Object();
    private AudioManager mAudioManager = null;
    private SystemControlManager mSystemControlManager = null;

    ArrayList<ArrayMap<String, Object>> list_programs = new ArrayList<ArrayMap<String, Object>>();
    SimpleAdapter adapter_programs = null;

    private Toast toast = null;//use to show audio&subtitle track
    private boolean isToastShow = false;//use to show prompt

    private boolean isRunning = true;

    private boolean mReceiverRegisted = false;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DroidLogicTvUtils.ACTION_DELETE_CHANNEL)) {
                if (isSearchingChannel)
                    return;
                int channelNumber = intent.getIntExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, -1);
                Utils.logd(TAG, "delete or skipped current channel, switch to: name=" + mSourceInput.getChannelName()
                           + " uri=" + mSourceInput.getUri());
                if (channelNumber >= 0) {
                    processDeleteCurrentChannel(channelNumber);
                } else {
                    sendTuneMessage();
                }
                Intent i = new Intent(DroidLogicTvUtils.ACTION_CHANNEL_CHANGED);
                i.putExtra(TvInputInfo.EXTRA_INPUT_ID, mSourceInput.getInputId());
                i.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_DEVICE_ID, mSourceInput.getDeviceId());
                i.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, (int)mSourceInput.getChannelId());
                i.putExtra(DroidLogicTvUtils.EXTRA_IS_RADIO_CHANNEL, mSourceInput.isRadioChannel());
                context.sendBroadcast(i);
            } else if (action.equals(DroidLogicTvUtils.ACTION_UPDATE_TV_PLAY)) {
                String operation = intent.getStringExtra("tv_play_extra");
                Utils.logd(TAG, "recevie intent : operation is " + operation);
                if (!TextUtils.isEmpty(operation)) {
                    if (operation.equals("search_channel")) {
                        mMainView.setBackgroundDrawable(null);
                        mSourceView.sendAppPrivateCommand(DroidLogicTvUtils.ACTION_STOP_PLAY, null);
                        isSearchingChannel = true;
                    } else if (operation.equals("mute")) {
                        showMuteIcon(true);
                    } else if (operation.equals("unmute")) {
                        showMuteIcon(false);
                    }
                }
            } else if (action.equals(DroidLogicTvUtils.ACTION_SWITCH_CHANNEL)) {
                if (isSearchingChannel)
                    return;

                int channelIndex = intent.getIntExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, -1);
                boolean isRadioChannel = intent.getBooleanExtra(DroidLogicTvUtils.EXTRA_IS_RADIO_CHANNEL, false);
                boolean force_dtv = intent.getBooleanExtra("force_dtv", false);

                Utils.logd(TAG, "recevie intent :switch channel to index=" + channelIndex + " isRadio=" + isRadioChannel
                           + " force dtv=" + force_dtv);
                if (!force_dtv) {
                    onSelect(channelIndex, isRadioChannel);
                } else {
                    SourceButton dtvSourceButton = mSourceMenuLayout.getDtvSourceButton();
                    if (mSourceInput.getSourceType() != DroidLogicTvUtils.SOURCE_TYPE_DTV) {
                        dtvSourceButton.moveToChannel(channelIndex, isRadioChannel);
                        dtvSourceButton.switchSource();
                    } else {
                        onSelect(channelIndex, isRadioChannel);
                    }
                }
            } else if (action.equals(DroidLogicTvUtils.ACTION_SUBTITLE_SWITCH)) {
                int switchVal = intent.getIntExtra(DroidLogicTvUtils.EXTRA_SUBTITLE_SWITCH_VALUE, 0);
                resetSubtitleTrack((switchVal == 1));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.logd(TAG, "==== onCreate ====");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
        mScreenLock = ((PowerManager)this.getSystemService (Context.POWER_SERVICE)).newWakeLock(
                          PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mSystemControlManager = new SystemControlManager(this);
        initThread(mThreadName);
        mSourceInputId = getIntent().getStringExtra(DroidLogicTvUtils.SOURCE_INPUT_ID);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);
        mSourceInputId = intent.getStringExtra(DroidLogicTvUtils.SOURCE_INPUT_ID);
    }

    private void init() {
        mContext = getApplicationContext();
        mHandler = new Handler(this);

        mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);
        mTvInputChangeCallback = new TvInputChangeCallback();
        mTvDataBaseManager = new TvDataBaseManager(mContext);

        mTimePromptText = (TextView) findViewById(R.id.textView_time_prompt);

        mRootView = (FrameLayout)findViewById(R.id.root_view);
        addTvView();
        mMainView = (RelativeLayout)findViewById(R.id.main_view);

        mSourceMenuLayout = (SourceInputListLayout)findViewById(R.id.menu_layout);
        mSourceMenuLayout.setOnSourceInputClickListener(this);
        mSourceInfoLayout = (LinearLayout)findViewById(R.id.info_layout);
        mChannelListLayout = (ChannelListLayout)findViewById(R.id.channel_list);
        mChannelListLayout.setOnChannelSelectListener(this);
        prompt_no_signal = (TextView)findViewById(R.id.no_signal);

        mChannelDataManager = new ChannelDataManager(mContext);

        setStartUpInfo();
    }

    private void addTvView() {
        if (mSourceView == null) {
            mSourceView = (TvView)findViewById(R.id.source_view);
            mSourceView.setCallback(mTvViewCallback);
        } else {
            mSourceView.setVisibility(View.VISIBLE);
        }
    }

    private void releaseTvView() {
        if (mSourceView != null) {
            mSourceView.setVisibility(View.GONE);
            //mSourceView.reset();
        }
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
        mThreadHandler.removeCallbacksAndMessages(null);
        mThreadHandler = null;
    }

    private void initSourceMenuLayout() {
        setDefaultChannelInfo();
        mSourceMenuLayout.refresh();
        if (mSourceInput != null) {
            mSourceMenuLayout.getCurSourceInput().setChannelVideoFormat(mSourceInput.getChannelVideoFormat());
            mSourceMenuLayout.getCurSourceInput().setAVType(mSourceInput.getAVType());
        }
        mSourceInput = mSourceMenuLayout.getCurSourceInput();
    }

    private void startPlay() {
        if (mSourceMenuLayout.getSourceCount() == 0)
            return;
        if (hasStopped || needUpdateSource) {
            sendTuneMessage();
        }
    }

    @Override
    protected void onResume() {
        Utils.logd(TAG, "== onResume ====");
        closeTouchSound();
        closeScreenOffTimeout();
        initSourceMenuLayout();

        mSystemControlManager.setProperty(PROP_TV_PREVIEW, "false");
        addTvView();
        showUi(Utils.UI_TYPE_ALL_HIDE, false);
        startPlay();
        showUi(Utils.UI_TYPE_SOURCE_INFO, false);
        hasStopped = false;
        isMenuShowing = false;
        needUpdateSource = true;
        isRunning = true;
        isSearchingChannel = false;
        if (mSignalState == SIGNAL_NOT_GOT)
            reset_nosignal_time();

        if (mAudioManager.isStreamMute(AudioSystem.STREAM_MUSIC))
            showMuteIcon(true);
        else
            showMuteIcon(false);
        mTvInputManager.registerCallback(mTvInputChangeCallback, new Handler());
        switchHdmiChannel();
        if (!mReceiverRegisted) {
            mReceiverRegisted = true;
            IntentFilter intentFilter = new IntentFilter(DroidLogicTvUtils.ACTION_TIMEOUT_SUSPEND);
            intentFilter.addAction(DroidLogicTvUtils.ACTION_UPDATE_TV_PLAY);
            intentFilter.addAction(DroidLogicTvUtils.ACTION_SUBTITLE_SWITCH);
            intentFilter.addAction(DroidLogicTvUtils.ACTION_DELETE_CHANNEL);
            intentFilter.addAction(DroidLogicTvUtils.ACTION_SWITCH_CHANNEL);
            registerReceiver(mReceiver, intentFilter);
        }
        super.onResume();
    }

    /**
     * get the default source input after {@code this.onCreate}.
     */
    private void setDefaultChannelInfo() {
        int device_id, atv_channel, dtv_channel;
        boolean is_radio;
        device_id = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_DEVICE_ID, 0);
        atv_channel = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_ATV_CHANNEL_INDEX, -1);
        dtv_channel = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_DTV_CHANNEL_INDEX, -1);
        is_radio = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_CHANNEL_IS_RADIO, 0) == 1 ? true : false;
        mSourceMenuLayout.setDefaultSourceInfo(device_id, atv_channel, dtv_channel, is_radio);
    }

    private void saveDefaultChannelInfo() {
        if (mSourceInput.getSourceType() == DroidLogicTvUtils.SOURCE_TYPE_OTHER)
            return;

        Settings.System.putInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_DEVICE_ID, mSourceInput.getDeviceId());
        int index;
        if (mSourceInput.getChannelNumber().isEmpty()) {
            index = -1;
        } else {
            index = Integer.parseInt(mSourceInput.getChannelNumber());
        }
        int type = mSourceInput.getSourceType();
        boolean is_radio = mSourceInput.isRadioChannel();
        if (type == DroidLogicTvUtils.SOURCE_TYPE_ATV) {
            Settings.System.putInt(getContentResolver(), DroidLogicTvUtils.TV_ATV_CHANNEL_INDEX, index);
            Settings.System.putInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_CHANNEL_IS_RADIO, 0);
        } else if (type == DroidLogicTvUtils.SOURCE_TYPE_DTV) {
            Settings.System.putInt(getContentResolver(), DroidLogicTvUtils.TV_DTV_CHANNEL_INDEX, index);
            Settings.System.putInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_CHANNEL_IS_RADIO, is_radio ? 1 : 0);
        }
    }

    private void sendTuneMessage() {
        mHandler.removeMessages(MSG_TUNE);
        mHandler.sendEmptyMessage(MSG_TUNE);
    }

    /**
     * must be invoked after {@link SourceButton.moveToChannel}.
     * if there is nothing about channel switching in the tv.db, the {@code channel_uri}
     * used to tune must be wrong.
     */
    private void switchToSourceInput() {
        if (mSourceInput == null)
            return;
        if (mThreadHandler == null) {
            return;
        }
        mThreadHandler.obtainMessage(MSG_SAVE_CHANNEL_INFO).sendToTarget();
        mPreSigType = mSigType;
        mSigType = mSourceInput.getSigType();
        mSignalState = SIGNAL_GOT;
        if (mSourceInput.isAvaiableSource()) {
            addTvView();
            mThreadHandler.obtainMessage(MSG_SAVE_CHANNEL_INFO).sendToTarget();
            Uri channel_uri = mSourceInput.getUri();
            Utils.logd(TAG, "channelUri switching to is " + channel_uri);
            mSourceView.tune(mSourceInput.getInputId(), channel_uri);
            if (mSourceInput.isRadioChannel()) {
                mMainView.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_radio));
            } else {
                mMainView.setBackgroundDrawable(null);
            }
        } else {
            mSignalState = SIGNAL_NOT_GOT;
            reset_nosignal_time();
            releaseTvView();
            mMainView.setBackgroundDrawable(getResources().getDrawable(R.drawable.hotplug_out));
        }
        showUi(Utils.UI_TYPE_SOURCE_INFO, false);
    }

    private void switchToDtvChannel(int channelId) {
        SourceButton dtvSourceButton = mSourceMenuLayout.getDtvSourceButton();
        if (dtvSourceButton == null) {
            return;
        }
        int index = -1;
        boolean isRadio = false;
        for (int i = 0; i < dtvSourceButton.getChannelVideoList().size(); i++) {
            if (channelId == dtvSourceButton.getChannelVideoList().get(i).getId()) {
                index = i;
                isRadio = false;
                break;
            }
        }
        if (index == -1) {
            for (int i = 0; i < dtvSourceButton.getChannelRadioList().size(); i++) {
                if (channelId == dtvSourceButton.getChannelRadioList().get(i).getId()) {
                    index = i;
                    isRadio = true;
                    break;
                }
            }
        }
        if (index != -1) {
            showUi(Utils.UI_TYPE_ALL_HIDE, true);
            if (mSourceInput.getSourceType() != DroidLogicTvUtils.SOURCE_TYPE_DTV) {
                dtvSourceButton.moveToChannel(index, isRadio);
                dtvSourceButton.switchSource();
            } else {
                onSelect(index, isRadio);
            }
        }
    }

    private void switchHdmiChannel() {
        if (mSourceInputId != null) {
            TvInputInfo info = mTvInputManager.getTvInputInfo(mSourceInputId);
            if (info != null) {
                mSourceInput = mSourceMenuLayout.getSourceInput(info);
                if (mSourceInput != null) mSourceInput.switchSource();
            }
            mSourceInputId = null;
        }
    }

    private void startSetupActivity () {
        if (mSourceInput == null || mSourceInput.getTvInputInfo() == null)
            return;

        showUi(Utils.UI_TYPE_ALL_HIDE, false);
        isMenuShowing = true;
        TvInputInfo info = mSourceInput.getTvInputInfo();
        Intent intent = info.createSetupIntent();
        if (intent != null) {
            intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_DEVICE_ID, mSourceInput.getDeviceId());
            intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, (int)mSourceInput.getChannelId());
            intent.putExtra(DroidLogicTvUtils.EXTRA_IS_RADIO_CHANNEL, mSourceInput.isRadioChannel());
            startActivityForResult(intent, START_SETUP);
        }
    }

    private void startSettingActivity (int keycode) {
        if (mSourceInput == null || mSourceInput.getTvInputInfo() == null)
            return;

        showUi(Utils.UI_TYPE_ALL_HIDE, false);
        TvInputInfo info = mSourceInput.getTvInputInfo();
        Intent intent = info.createSettingsIntent();
        if (intent != null) {
            intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_DEVICE_ID, mSourceInput.getDeviceId());
            intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, (int)mSourceInput.getChannelId());
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
    public void onSelect(int channelNum, boolean isRadio) {
        if (mSourceInput.moveToChannel(channelNum, isRadio)) {
            sendTuneMessage();
        }
    }

    @Override
    public void onSourceInputClick() {
        Utils.logd(TAG, "==== onSourceInputClick ====");
        hideTvView(mSourceMenuLayout);
        showUi(Utils.UI_TYPE_SOURCE_INFO, false);
        if (mSourceInput.getSourceType() == mSourceMenuLayout.getCurSourceInput().getSourceType()) {
            return;
        }
        preSwitchSourceInput();
        mSourceInput = mSourceMenuLayout.getCurSourceInput();
        sendTuneMessage();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!isBootvideoStopped())
            return true;
        sendKeyEventToHdmi(event.getKeyCode(), event.getAction() == KeyEvent.ACTION_DOWN);
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

                if (keyCode == DroidLogicKeyEvent.KEYCODE_GUIDE
                    && (mSourceInput.getSourceType() != DroidLogicTvUtils.SOURCE_TYPE_DTV
                        || mSignalState == SIGNAL_NOT_GOT)) {
                    return true;
                }

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
                if (mSourceInfoLayout.getVisibility() == View.VISIBLE)
                    hideTvView(mSourceInfoLayout);
                else
                    showUi(Utils.UI_TYPE_SOURCE_INFO, false);
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (!down)
                    return true;

                hideTvView(mSourceMenuLayout);
                hideTvView(prompt_no_signal);
                hideTvView(mChannelListLayout);
                showUi(Utils.UI_TYPE_NO_SINAL, false);
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
                    break;

                if (mAudioManager.isStreamMute(AudioSystem.STREAM_MUSIC)) {
                    mAudioManager.setStreamMute(AudioSystem.STREAM_MUSIC, false);
                    showMuteIcon(false);
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                if (!down)
                    return true;

                if (mAudioManager.isStreamMute(AudioSystem.STREAM_MUSIC)) {
                    mAudioManager.setStreamMute(AudioSystem.STREAM_MUSIC, false);
                    showMuteIcon(false);
                } else {
                    mAudioManager.setStreamMute(AudioSystem.STREAM_MUSIC, true);
                    showMuteIcon(true);
                }
                return true;
            default:
                break;
        }
        return false;
    }

    private void sendKeyEventToHdmi(int keyCode, boolean isPressed) {
        if (mHdmiTvClient == null) {
            HdmiControlManager hdmiManager = (HdmiControlManager) getSystemService(Context.HDMI_CONTROL_SERVICE);
            if (hdmiManager == null) return;
            mHdmiTvClient = hdmiManager.getTvClient();
        }
        if (mHdmiTvClient != null && mUiType != Utils.UI_TYPE_SOURCE_LIST) {
            mHdmiTvClient.sendKeyEvent(keyCode, isPressed);
        }
    }

    private void updateTracksInfo(int audioTrack, int subtitleTrack) {
        ChannelInfo currentChannel = mSourceInput.getChannelInfo();
        if (currentChannel != null) {
            if ((audioTrack >= 0) || (audioTrack == -2))
                currentChannel.setAudioTrackIndex(audioTrack);
            if ((subtitleTrack >= 0) || (subtitleTrack == -2))
                currentChannel.setSubtitleTrackIndex(subtitleTrack);

            mTvDataBaseManager.updateChannelInfo(currentChannel);
        }
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
                for (sTrackIndex = 0; sTrackIndex < sTrackList.size(); sTrackIndex++) {
                    if (sTrackList.get(sTrackIndex).getId().equals(subtitleTrackId)) {
                        break;
                    }
                }

                if (isToastShow) {
                    sTrackIndex ++;
                    if (sTrackIndex == sTrackList.size()) {
                        mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, null);
                        updateTracksInfo(-1, -2);
                        showCustomToast(getResources().getString(R.string.subtitle), getResources().getString(R.string.off));
                        return;
                    }
                    if (subtitleTrackId == null) {
                        sTrackIndex = 0;
                    }
                    mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, sTrackList.get(sTrackIndex).getId());
                    updateTracksInfo(-1, sTrackIndex);
                    showCustomToast(getResources().getString(R.string.subtitle), sTrackList.get(sTrackIndex).getLanguage());
                    Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_SUBTITLE_SWITCH, 1);
                } else {
                    showCustomToast(getResources().getString(R.string.subtitle), sTrackList.get(sTrackIndex).getLanguage());
                }
            } else
                showCustomToast(getResources().getString(R.string.subtitle), getResources().getString(R.string.no));
        } else if (type == TvTrackInfo.TYPE_AUDIO) {
            String audioTrackId = mSourceView.getSelectedTrack(TvTrackInfo.TYPE_AUDIO);
            if (audioTrackId != null) {
                List<TvTrackInfo> aTrackList = mSourceView.getTracks(TvTrackInfo.TYPE_AUDIO);
                int aTrackIndex = 0;
                for (aTrackIndex = 0; aTrackIndex < aTrackList.size(); aTrackIndex++) {
                    if (aTrackList.get(aTrackIndex).getId().equals(audioTrackId)) {
                        break;
                    }
                }
                if (isToastShow) {
                    aTrackIndex = (aTrackIndex + 1) % aTrackList.size();
                    mSourceView.selectTrack(TvTrackInfo.TYPE_AUDIO, aTrackList.get(aTrackIndex).getId());
                    updateTracksInfo(aTrackIndex, -1);
                }
                showCustomToast(getResources().getString(R.string.audio_track), aTrackList.get(aTrackIndex).getLanguage());
            } else
                showCustomToast(getResources().getString(R.string.audio_track), getResources().getString(R.string.no));
        }
    }

    public void processKeyInputChannel(int offset) {
        if (mSourceInput.isPassthrough())
            return;

        if (mSourceInput.moveToOffset(offset))
            sendTuneMessage();
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
            keyInputNumber = Integer.toString(mSourceInput.getChannelVideoList().valueAt((index + offset) % size).getNumber());
        else
            keyInputNumber = Integer.toString(mSourceInput.getChannelVideoList().valueAt((size + (index + offset) % size) % size).getNumber());

        showUi(Utils.UI_TYPE_SOURCE_INFO, false);
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
        showUi(Utils.UI_TYPE_SOURCE_INFO, false);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CHANNEL_NUM_SWITCH), 2000);
    }

    public void processKeyLookBack() {
        if (mSourceInput.moveToRecentChannel())
            sendTuneMessage();
    }

    public void processDeleteCurrentChannel(int number) {
        if (mSourceInput.moveToIndex(number))
            sendTuneMessage();
    }

    private boolean inflateCurrentInfoLayout() {
        boolean needFresh = false;
        if (mSourceInput == null)
            return false;

        String label = null;
        String name = null;
        String number = null;
        switch (mSigType) {
            case DroidLogicTvUtils.SIG_INFO_TYPE_ATV:
                label = mSourceInput.getSourceLabel();
                if (isNumberSwitching) {
                    number = keyInputNumber;
                    name = "";
                } else {
                    number = mSourceInput.getChannelNumber();
                    name = mSourceInput.getChannelType();
                }
                break;
            case DroidLogicTvUtils.SIG_INFO_TYPE_DTV:
                label = mSourceInput.getSourceLabel();
                if (isNumberSwitching) {
                    number = keyInputNumber;
                    name = "";
                } else {
                    number = mSourceInput.getChannelNumber();
                    name = mSourceInput.getChannelName();
                }
                break;

            case DroidLogicTvUtils.SIG_INFO_TYPE_AV:
                label = mSourceInput.getSourceLabel();
                if (mSignalState == SIGNAL_NOT_GOT) {
                    name = "";
                } else {
                    name = mSourceInput.getAVType();
                }
                break;
            case DroidLogicTvUtils.SIG_INFO_TYPE_HDMI:
                label = mSourceInput.getSourceLabel();
                if (mSignalState == SIGNAL_NOT_GOT) {
                    name = "";
                } else {
                    name = mSourceInput.getChannelVideoFormat();
                }
                break;
            default:
                label = mSourceInput.getSourceLabel();
                number = mSourceInput.getChannelNumber();
                name = mSourceInput.getChannelName();
                break;
        }

        if (mInfoLabel == null
                || (mInfoLabel != null && !TextUtils.equals((String)mInfoLabel.getText(), label))) {
            needFresh = true;
        }
        if (mInfoName == null
                || (mInfoName != null && !TextUtils.equals((String)mInfoName.getText(), name))) {
            needFresh = true;
        }
        if (mInfoNumber != null && !TextUtils.equals((String)mInfoNumber.getText(), number)) {
            needFresh = true;
        }

        if (needFresh) {
            mSourceInfoLayout.removeAllViews();
            mInfoNumber = null;
            LayoutInflater inflate = LayoutInflater.from(mContext);
            switch (mSigType) {
                case DroidLogicTvUtils.SIG_INFO_TYPE_ATV:
                case DroidLogicTvUtils.SIG_INFO_TYPE_DTV:
                    mSourceInfoLayout.addView(inflate.inflate(R.layout.atv_dtv_info, mSourceInfoLayout, false));
                    mInfoLabel = (TextView) findViewById(R.id.ad_info_name);
                    mInfoNumber = (TextView) findViewById(R.id.ad_info_number);
                    mInfoName = (TextView) findViewById(R.id.ad_info_value);
                    break;
                case DroidLogicTvUtils.SIG_INFO_TYPE_AV:
                case DroidLogicTvUtils.SIG_INFO_TYPE_HDMI:
                    mSourceInfoLayout.addView(inflate.inflate(R.layout.hdmi_av_info, mSourceInfoLayout, false));
                    mInfoLabel = (TextView) findViewById(R.id.ha_info_name);
                    mInfoName = (TextView) findViewById(R.id.ha_info_value);
                    break;
                default:
                    mSourceInfoLayout.addView(inflate.inflate(R.layout.atv_dtv_info, mSourceInfoLayout, false));
                    mInfoLabel = (TextView) findViewById(R.id.ad_info_name);
                    mInfoNumber = (TextView) findViewById(R.id.ad_info_number);
                    mInfoName = (TextView) findViewById(R.id.ad_info_value);
                    break;
            }
            mInfoLabel.setText(label);
            mInfoName.setText(name);
            if (mInfoNumber != null) {
                mInfoNumber.setText(number);
            }
            mInfoName.requestFocus();
        }

        return needFresh;
    }

    private void showUi (int type, boolean forceShow) {

        synchronized (mLock) {
            switch (type) {
                case Utils.UI_TYPE_SOURCE_INFO:
                    if (mSourceMenuLayout.getVisibility() == View.VISIBLE
                            || (!inflateCurrentInfoLayout() && mSourceInfoLayout.getVisibility() == View.VISIBLE)) {
                        return;
                    }

                    mHandler.removeMessages(MSG_UI_TIMEOUT);
                    showTvView(mSourceInfoLayout);
                    mSourceInfoLayout.requestLayout();
                    mUiType = type;
                    mHandler.sendEmptyMessageDelayed(MSG_UI_TIMEOUT, DEFAULT_TIMEOUT);
                    break;
                case Utils.UI_TYPE_SOURCE_LIST:
                    if (forceShow || mSourceMenuLayout.getVisibility() != View.VISIBLE) {
                        mHandler.removeMessages(MSG_UI_TIMEOUT);
                        showTvView(mSourceMenuLayout);
                        mSourceMenuLayout.requestLayout();
                        mSourceInput.requestFocus();
                        mUiType = type;

                        mHandler.sendEmptyMessageDelayed(MSG_UI_TIMEOUT, DEFAULT_TIMEOUT);
                    }
                    break;
                case Utils.UI_TYPE_ATV_CHANNEL_LIST:
                case Utils.UI_TYPE_DTV_CHANNEL_LIST:
                case Utils.UI_TYPE_ATV_FAV_LIST:
                case Utils.UI_TYPE_DTV_FAV_LIST:
                    if (forceShow || type != mChannelListLayout.getType()
                        || mChannelListLayout.getVisibility() != View.VISIBLE) {
                        mChannelListLayout.initView(type, mSourceInput);
                        mHandler.removeMessages(MSG_UI_TIMEOUT);
                        showTvView(mChannelListLayout);
                        mChannelListLayout.requestFocus();
                        mUiType = type;
                        mHandler.sendEmptyMessageDelayed(MSG_UI_TIMEOUT, DEFAULT_TIMEOUT);
                    }
                    break;
                case Utils.UI_TYPE_NO_SINAL:
                    if (mSignalState == SIGNAL_SCRAMBLED)
                        prompt_no_signal.setText(mContext.getResources().getString(R.string.av_scambled));
                    else if (mSignalState == SIGNAL_NOT_GOT && mSourceInput.isAvaiableSource())
                        prompt_no_signal.setText(mContext.getResources().getString(R.string.no_signal));
                    else {
                        hideTvView(prompt_no_signal);
                        return;
                    }
                    if (prompt_no_signal.getVisibility() != View.VISIBLE
                        && mSourceMenuLayout.getVisibility() != View.VISIBLE
                        && mSourceInfoLayout.getVisibility() != View.VISIBLE
                        && mChannelListLayout.getVisibility() != View.VISIBLE
                        && !isToastShow && !isMenuShowing) {
                        showTvView(prompt_no_signal);
                        prompt_no_signal.requestLayout();
                        mUiType = type;
                    }
                    break;
                case Utils.UI_TYPE_ALL_HIDE:
                    hideTvView(mChannelListLayout);
                    hideTvView(mSourceMenuLayout);
                    hideTvView(mSourceInfoLayout);
                    hideTvView(prompt_no_signal);

                    mUiType = type;
                    mHandler.removeMessages(MSG_UI_TIMEOUT);
                    break;
            }
        }
    }

    private void showMuteIcon (boolean isShow) {
        View icon_mute = findViewById(R.id.image_mute);
        if (isShow)
            showTvView(icon_mute);
        else
            hideTvView(icon_mute);
    }

    private void processSessionEvent(String inputId, String eventType, Bundle eventArgs) {
        if (eventType == null || eventArgs == null)
            return;

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
            showUi(Utils.UI_TYPE_SOURCE_INFO, false);
        } else if (eventType.equals(DroidLogicTvUtils.AV_SIG_SCRAMBLED)) {
            mSignalState = SIGNAL_SCRAMBLED;
            showUi(Utils.UI_TYPE_NO_SINAL, false);
        }
    }

    @Override
    protected void onPause() {
        Utils.logd(TAG, "==== onPause ====");
        isRunning = false;
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
        releaseTvView();
        restoreTouchSound();
        openScreenOffTimeout();
        mTvInputManager.unregisterCallback(mTvInputChangeCallback);
        unregisterReceiver(mReceiver);
        mReceiverRegisted = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Utils.logd(TAG, "==== onDestroy ====");
        releaseThread();
        mChannelDataManager.release();
        super.onDestroy();
    }

    @Override
    public boolean handleMessage(Message msg) {
        int max_counter;
        switch (msg.what) {
            case MSG_UI_TIMEOUT:
                switch (mUiType) {
                    case Utils.UI_TYPE_SOURCE_INFO:
                        hideTvView(mSourceInfoLayout);
                        showUi(Utils.UI_TYPE_NO_SINAL, false);
                        break;
                    case Utils.UI_TYPE_SOURCE_LIST:
                        hideTvView(mSourceMenuLayout);
                        showUi(Utils.UI_TYPE_SOURCE_INFO, false);
                        break;
                    case Utils.UI_TYPE_ATV_CHANNEL_LIST:
                    case Utils.UI_TYPE_DTV_CHANNEL_LIST:
                    case Utils.UI_TYPE_ATV_FAV_LIST:
                    case Utils.UI_TYPE_DTV_FAV_LIST:
                        showUi(Utils.UI_TYPE_SOURCE_INFO, false);
                        break;
                    case Utils.UI_TYPE_NO_SINAL:
                        break;
                }
                break;
            case MSG_CHANNEL_NUM_SWITCH:
                boolean isRadio = mSourceInput.isRadioChannel();
                int channelnum = Integer.parseInt(keyInputNumber);
                if (mSourceInput.moveToIndex(mSourceInput.getChannelIndex(channelnum, isRadio))) {
                    sendTuneMessage();
                }
                isNumberSwitching = false;
                keyInputNumber = "";
                showUi(Utils.UI_TYPE_SOURCE_INFO, false);
                break;
            case MSG_TUNE:
                if (isBootvideoStopped()) {
                    Utils.logd(TAG, "======== bootvideo is stopped, start tv play");
                    switchToSourceInput();
                } else {
                    Utils.logd(TAG, "======== bootvideo is not stopped, wait it");
                    mHandler.sendEmptyMessageDelayed(MSG_TUNE, 200);
                }
                break;
            default:
                break;
        }
        return false;
    }

    /**
     * when {@code TvInputChangeCallback} is invoked, must distinguish whether the {@value inputId}
     * is valid or a HDMI device.
     */
    private boolean isValidInputId(String inputId) {
        Utils.logd(TAG, "isValidInputId, inputId = " + inputId);
        if (TextUtils.isEmpty(inputId))
            return false;

        String[] temp = inputId.split(Utils.DELIMITER_INFO_IN_ID);
        if (temp.length == 3 && temp[2].startsWith(Utils.PREFIX_HDMI_DEVICE))
            return false;

        return true;
    }

    private final class TvInputChangeCallback extends TvInputManager.TvInputCallback {

        @Override
        public void onInputAdded(String inputId) {
            Utils.logd(TAG, "==== onInputAdded, inputId=" + inputId);
            if (!isValidInputId(inputId))
                return;

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
                mSourceView.reset();
                startPlay();
            }
        }

        @Override
        public void onInputRemoved(String inputId) {
            Utils.logd(TAG, "==== onInputRemoved, inputId=" + inputId);
            if (!isValidInputId(inputId))
                return;

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
            if (!isValidInputId(inputId))
                return;

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
            if (!isValidInputId(inputId))
                return;

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
            Utils.logd(TAG, "====onEvent==inputId =" + inputId + ", ===eventType =" + eventType);
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
        public void onConnectionFailed(String inputId) {
            Utils.logd(TAG, "====onConnectionFailed==inputId =" + inputId);
            new Thread( new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    startPlay();
                }
            }).start();
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            Utils.logd(TAG, "====onVideoUnavailable==inputId =" + inputId + ", ===reason =" + reason);
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
            int switchVal = Settings.System.getInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_KEY_SUBTITLE_SWITCH, 0);
            resetSubtitleTrack((switchVal == 1));
        }

        @Override
        public void onChannelRetuned(String inputId, Uri channelUri) {
            if (mSourceInput != null
                    && channelUri != null
                    && TextUtils.isEmpty(inputId)
                    && TextUtils.equals(inputId, mSourceInput.getInputId())
                    && channelUri.equals(mSourceInput.getUri())) {
                sendTuneMessage();
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
                        } else {
                            if (mNoSignalShutdownCount < 60) {
                                String str = mNoSignalShutdownCount + "";
                                mTimePromptText.setText(str);
                                if (mTimePromptText.getVisibility() != View.VISIBLE)// if sleep time,no show view
                                    showTvView(mTimePromptText);
                            } else {
                                hideTvView(mTimePromptText);
                            }
                            no_signal_handler.postDelayed(no_signal_runnable, 1000);
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    };

    private void reset_nosignal_time() {
        if ( !isRunning )
            return;
        mNoSignalShutdownCount = 300;//5min
        no_signal_handler.removeCallbacks(no_signal_runnable);
        no_signal_handler.postDelayed(no_signal_runnable, 0);
    }

    private void remove_nosignal_time() {
        if (mTimePromptText.getVisibility() == View.VISIBLE)
            hideTvView(mTimePromptText);
        no_signal_handler.removeCallbacks(no_signal_runnable);
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

        TextView title = (TextView)layout.findViewById(R.id.toast_title);
        TextView status = (TextView)layout.findViewById(R.id.toast_status);

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

    private void showTvView(View tvView) {
        if (tvView.getVisibility() == View.VISIBLE)
            return;
        if (tvView.getId() == mSourceInfoLayout.getId()) {
            hideTvView(mSourceMenuLayout);
            hideTvView(prompt_no_signal);
            hideTvView(mChannelListLayout);
        } else if (tvView.getId() == mSourceMenuLayout.getId()) {
            hideTvView(mSourceInfoLayout);
            hideTvView(prompt_no_signal);
            hideTvView(mChannelListLayout);
        } else if (tvView.getId() == mChannelListLayout.getId()) {
            hideTvView(mSourceMenuLayout);
            hideTvView(mSourceInfoLayout);
            hideTvView(prompt_no_signal);
        }
        tvView.setVisibility(View.VISIBLE);
    }

    private void hideTvView(View tvView) {
        if (tvView.getVisibility() == View.VISIBLE)
            tvView.setVisibility(View.INVISIBLE);
    }

    private boolean isBootvideoStopped() {
        return !TextUtils.equals(mSystemControlManager.getProperty("service.bootvideo"), "1")
               || TextUtils.equals(mSystemControlManager.getProperty("service.bootvideo.exit"), "1");
    }

    private void resetSubtitleTrack(boolean on) {
        if (on) {
            List<TvTrackInfo> sTrackList = mSourceView.getTracks(TvTrackInfo.TYPE_SUBTITLE);
            if (sTrackList.size() > 0) {

                int channelSpecificTrack = mSourceInput.getChannelInfo().getSubtitleTrackIndex();
                if (channelSpecificTrack >= 0) {
                    mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, sTrackList.get(channelSpecificTrack).getId());
                    return;
                }
                if (channelSpecificTrack == -2) {
                    mSourceView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, null);
                    return;
                }

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
