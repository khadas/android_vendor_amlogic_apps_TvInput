package com.droidlogic.tvsource;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.tv.DroidLogicTvUtils;

import com.droidlogic.tvsource.ChannelListLayout.OnChannelSelectListener;
import com.droidlogic.tvsource.SourceButton.OnSourceClickListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources.NotFoundException;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.media.tv.TvContract.Channels;
import android.media.tv.TvView.TvInputCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.Handler.Callback;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DroidLogicTv extends Activity implements Callback, OnSourceClickListener, OnChannelSelectListener {
    private static final String TAG = "DroidLogicTv";
    private static final String SHARE_NAME = "tv_app";

    private Context mContext;
    private TvInputManager mTvInputManager;
    private ChannelDataManager mChannelDataManager;

    private TvView mSourceView;
    private SourceButton mSourceInput;

    private RelativeLayout mMainView;
    private LinearLayout mSourceMenuLayout;
    private LinearLayout mSourceInfoLayout;
    private ChannelListLayout mChannelListLayout;

    private MediaPlayer mPlayer;

    //max index of all hardware devices in mSourceMenuLayout
    private int maxHardwareIndex = 0;

    private int mSigType;

    private boolean isNoSignal;
//    private boolean isNoSignalShowing;
    private boolean isSourceMenuShowing;
//    private boolean isSourceInfoShowing;

    private Timer delayTimer = null;
    private int delayCounter = 0;
    private Timer channelSwitchTimer = null;
    private volatile int mNoSignalShutdownCount = -1;
    private TextView mTimePromptText = null;

    private Handler mHandler;
    private static final int MSG_INFO_DELAY             = 0;
    private static final int MSG_SOURCE_DELAY           = 1;
    private static final int MSG_CHANNEL_LIST_DELAY     = 2;
    private static final int MSG_CHANNEL_KEY_SWITCH     = 3;
    private static final int MSG_CHANNEL_NUM_SWITCH     = 4;

    private static final int TIME_INFO_DELAY = 5;
    private static final int TIME_SOURCE_DELAY = 5;
    private static final int TIME_CHANNEL_LIST_DELAY = 5;

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
    private boolean isSwitchingChannel = false;
    private boolean isNumberSwitching = false;
    private String keyInputNumber = "";

    //thread
    private HandlerThread mHandlerThread;
    private static final String mThreadName = TAG;
    private Handler mThreadHandler;
    private static final int MSG_SAVE_CHANNEL_INFO = 1;
    private boolean isPlayingRadio = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.logd(TAG, "==== onCreate ====");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
//        initVideoView();
        init();
        initTimeSuspend(this);
    }

    private void initVideoView() {
        ViewGroup root = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
        SurfaceView surfaceView = new SurfaceView(this);
        root.addView(surfaceView, 0);
        if (surfaceView != null) {
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceDestroyed(SurfaceHolder arg0) {
                    Utils.logd(TAG, "==== video view release ====");
                    if (mPlayer != null) {
                        try {
                            mPlayer.stop();
                        } catch (IllegalStateException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        mPlayer.release();
                        mPlayer = null;
                    }
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    if (mPlayer == null) {
                        mPlayer = new MediaPlayer();
                    }
                    mPlayer.reset();
                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    try {
                        mPlayer.setDataSource("tvin:test");
                        mPlayer.setDisplay(holder);
                        mPlayer.prepare();
                    } catch (Exception e) {
                        Utils.loge(TAG, e.toString());
                    }
                    mPlayer.start();
                }

                @Override
                public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    private void init() {
        mContext = getApplicationContext();
        mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);
        mChannelDataManager = new ChannelDataManager(mContext);

        mHandler = new Handler(this);

        mTimePromptText = (TextView) findViewById(R.id.textView_time_prompt);
        mSourceView = (TvView) findViewById(R.id.source_view);
        mSourceView.setCallback(new DroidLogicInputCallback());

        mMainView = (RelativeLayout)findViewById(R.id.main_view);

        mSourceMenuLayout = (LinearLayout)findViewById(R.id.menu_layout);
        mSourceInfoLayout = (LinearLayout)findViewById(R.id.info_layout);
        mChannelListLayout = (ChannelListLayout)findViewById(R.id.channel_list);
        mChannelListLayout.setOnChannelSelectListener(this);

        initThread(mThreadName);
        initSourceMenuLayout();
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

    /**
     * Add given number views, the number depends on the input services have registered.
     * The total number must be one more than the views need add, because there is a {@link TextView}
     * have been added {@link main.xml}.
     */
    private void initSourceMenuLayout() {
        List<TvInputInfo> input_list = mTvInputManager.getTvInputList();
        if (mSourceMenuLayout.getChildCount() > 1) {
            mSourceMenuLayout.removeViews(1, mSourceMenuLayout.getChildCount()-1);
        }
        for (TvInputInfo info : input_list) {
            SourceButton sb = new SourceButton(mContext, info);
            if (sb.isHardware()) {
                setDefaultChannelInfo(sb);
                if (maxHardwareIndex == 0) {
                    mSourceMenuLayout.addView(sb, 1);
                    maxHardwareIndex++;
                } else {
                    int lo = 1;
                    int hi = maxHardwareIndex;

                    while (lo <= hi) {
                        final int mid = (lo + hi) >>> 1;
                        final SourceButton temp = (SourceButton) mSourceMenuLayout.getChildAt(mid);
                        final int temp_id = temp.getDeviceId();
                        if (temp_id < sb.getDeviceId()) {
                            lo = mid + 1;
                        } else if (temp_id > sb.getDeviceId()) {
                            hi = mid - 1;
                        }
                    }
                    mSourceMenuLayout.addView(sb, lo);
                    maxHardwareIndex++;
                }
            }else {
                mSourceMenuLayout.addView(sb);
            }
            sb.setOnSourceClickListener(this);
        }
    }

    @Override
    protected void onResume() {
        Utils.logd(TAG, "== onResume ====");

        initMainView();
        if (hasStopped || needUpdateSource) {
            switchToSourceInput();
        }
        hasStopped = false;
        needUpdateSource = true;
        reset_nosignal_time();
        reset_shutdown_time();

        popupSourceInfo(Utils.SHOW_VIEW);
        super.onResume();
    }

    /**
     * get the default source input after {@code this.onCreate}.
     */
    private void setDefaultChannelInfo(SourceButton sb) {
        int device_id, atv_channel, dtv_channel;
        boolean is_radio;
        SharedPreferences sp = getSharedPreferences(SHARE_NAME, Context.MODE_PRIVATE);
        device_id = sp.getInt("device_id", 0);
        atv_channel = sp.getInt("atv_channel", -1);
        dtv_channel = sp.getInt("dtv_channel", -1);
        is_radio = sp.getBoolean("is_radio", false);
        if (sb.getSourceType() == DroidLogicTvUtils.SOURCE_TYPE_ATV && atv_channel >= 0) {
            sb.moveToChannel(atv_channel, false);
        } else if (sb.getSourceType() == DroidLogicTvUtils.SOURCE_TYPE_DTV && dtv_channel >= 0) {
            sb.moveToChannel(dtv_channel, is_radio);
        }

        if (device_id == sb.getDeviceId()) {
            mSourceInput = sb;
        }
    }

    /**
     * must be invoked after {@link SourceButton.moveToChannel}.
     * if there is nothing about channel switching in the tv.db, the {@code channel_uri}
     * used to tune must be wrong.
     */
    private void switchToSourceInput() {
        mThreadHandler.obtainMessage(MSG_SAVE_CHANNEL_INFO).sendToTarget();
        mPreSigType = mSigType;
        mSigType = mSourceInput.getSigType();
        Uri channel_uri = mSourceInput.getUri();
        Utils.logd(TAG, "channelUri switching to is " + channel_uri);

        mSourceView.tune(mSourceInput.getInputId(), channel_uri);
        popupSourceMenu(Utils.HIDE_VIEW);
        popupSourceInfo(Utils.SHOW_VIEW);
    }

    private void startSetupActivity () {
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
        switch (mSigType) {
            case DroidLogicTvUtils.SIG_INFO_TYPE_HDMI:
                mSourceInput.setChannelVideoFormat("");
                break;
            case DroidLogicTvUtils.SIG_INFO_TYPE_AV:
                mSourceInput.setChannelType("");
                break;
            default:
                break;
        }
    }

    @Override
    public void onSelect(int channelIndex, boolean isRadio) {
        if (mSourceInput.moveToChannel(channelIndex, isRadio)) {
            initMainView();
            popupChannelList(Utils.HIDE_VIEW);
            switchToSourceInput();
        }
    }

    @Override
    public void onButtonClick(SourceButton sb) {
        Utils.logd(TAG, "==== onButtonClick ====" + sb);
        if (mSourceInput.getSourceType() == sb.getSourceType()) {
            popupSourceMenu(Utils.HIDE_VIEW);
            popupSourceInfo(Utils.SHOW_VIEW);
            return;
        }
        preSwitchSourceInput();
        mSourceInput = sb;
        initMainView();
        switchToSourceInput();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Utils.logd(TAG, "====keycode =" + keyCode);
        reset_nosignal_time();
        if (isSourceMenuShowing) {
            createDelayTimer(MSG_SOURCE_DELAY, TIME_SOURCE_DELAY);
        }
        if (mChannelListLayout.isShow()) {
            createDelayTimer(MSG_CHANNEL_LIST_DELAY, TIME_CHANNEL_LIST_DELAY);
        }
        switch (keyCode) {
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_SOURCE_LIST:
                popupSourceMenu(isSourceMenuShowing ? Utils.HIDE_VIEW : Utils.SHOW_VIEW);
                return true;
            case DroidLogicKeyEvent.KEYCODE_MENU://show setup activity
                popupSourceMenu(Utils.HIDE_VIEW);
                popupSourceInfo(Utils.HIDE_VIEW);
                popupChannelList(Utils.HIDE_VIEW);
                startSetupActivity();
                return true;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE:
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_3DMODE:
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE:
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE:
            case DroidLogicKeyEvent.KEYCODE_TV_SLEEP:
            case DroidLogicKeyEvent.KEYCODE_MEDIA_AUDIO_CONTROL:
            case DroidLogicKeyEvent.KEYCODE_GUIDE:
                popupSourceMenu(Utils.HIDE_VIEW);
                popupSourceInfo(Utils.HIDE_VIEW);
                popupChannelList(Utils.HIDE_VIEW);
                startSettingActivity(keyCode);
                return true;
            case DroidLogicKeyEvent.KEYCODE_FAV:
                if (mChannelListLayout.isShow()) {
                    popupChannelList(Utils.HIDE_VIEW);
                    popupSourceInfo(Utils.SHOW_VIEW);
                } else {
                    popupChannelList(Utils.SHOW_VIEW,
                            mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_ATV ? Utils.ATV_FAV_LIST
                                    : (mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_DTV
                                    ? Utils.DTV_FAV_LIST : -1));
                }
                return true;
            case DroidLogicKeyEvent.KEYCODE_LIST:
                if (mChannelListLayout.isShow()) {
                    popupChannelList(Utils.HIDE_VIEW);
                    popupSourceInfo(Utils.SHOW_VIEW);
                } else {
                    popupChannelList(Utils.SHOW_VIEW,
                            mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_ATV ? Utils.ATV_LIST
                                    : (mSigType == DroidLogicTvUtils.SIG_INFO_TYPE_DTV
                                    ? Utils.DTV_LIST : -1));
                }
                return true;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_TVINFO:
                popupSourceInfo(Utils.SHOW_VIEW);
                return true;
            case DroidLogicKeyEvent.KEYCODE_BACK:
                if (isSourceMenuShowing) {
                    popupSourceMenu(Utils.HIDE_VIEW);
                    popupSourceInfo(Utils.SHOW_VIEW);
                    return true;
                } else if (mChannelListLayout.isShow()) {
                    popupChannelList(Utils.HIDE_VIEW);
                    popupSourceInfo(Utils.SHOW_VIEW);
                    return true;
                }
                break;
            case DroidLogicKeyEvent.KEYCODE_CHANNEL_UP:
                processKeyInputChannel(1);
                return true;
            case DroidLogicKeyEvent.KEYCODE_CHANNEL_DOWN:
                processKeyInputChannel(-1);
                return true;
            case DroidLogicKeyEvent.KEYCODE_0:
            case DroidLogicKeyEvent.KEYCODE_1:
            case DroidLogicKeyEvent.KEYCODE_2:
            case DroidLogicKeyEvent.KEYCODE_3:
            case DroidLogicKeyEvent.KEYCODE_4:
            case DroidLogicKeyEvent.KEYCODE_5:
            case DroidLogicKeyEvent.KEYCODE_6:
            case DroidLogicKeyEvent.KEYCODE_7:
            case DroidLogicKeyEvent.KEYCODE_8:
            case DroidLogicKeyEvent.KEYCODE_9:
                processNumberInputChannel(keyCode);
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void processKeyInputChannel(int offset) {
        if (mSourceInput.isPassthrough())
            return;

        destroyChannelSwitchTimer();
        if (mSourceInput.moveToOffset(offset)) {
            isSwitchingChannel = true;
            popupSourceInfo(Utils.SHOW_VIEW);
        }
        channelSwitch(MSG_CHANNEL_KEY_SWITCH, 500);
    }

    private void processNumberInputChannel(int keyCode) {
        if (mSourceInput.isPassthrough())
            return;

        destroyChannelSwitchTimer();
        isNumberSwitching = true;
        int val = keyCode - DroidLogicKeyEvent.KEYCODE_0;
        if (keyInputNumber.length() <= 8)
            keyInputNumber = keyInputNumber + val;
        popupSourceInfo(Utils.SHOW_VIEW);
        isSwitchingChannel = mSourceInput.moveToIndex(Integer.parseInt(keyInputNumber));
        channelSwitch(MSG_CHANNEL_NUM_SWITCH, 2000);
    }

    private void destroyChannelSwitchTimer() {
        if (channelSwitchTimer != null) {
            channelSwitchTimer.cancel();
            channelSwitchTimer = null;
            isSwitchingChannel = false;
        }
    }

    private void channelSwitch(final int what, final int delay) {
        destroyChannelSwitchTimer();
        channelSwitchTimer = new Timer();
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                mHandler.removeMessages(what);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(what), delay);
            }
        };
        channelSwitchTimer.schedule(task, 0);
    }

    private void popupSourceMenu(boolean show_or_hide) {//ture:show
        if (!show_or_hide) {
            if (mSourceMenuLayout.getVisibility() != View.VISIBLE)
                return;
            destroyDelayTimer();
            isSourceMenuShowing = false;
            mSourceMenuLayout.setVisibility(View.INVISIBLE);
        } else {
            isSourceMenuShowing = true;
            mSourceMenuLayout.setVisibility(View.VISIBLE);
            mSourceMenuLayout.requestLayout();
            mSourceInput.requestFocus();

            popupSourceInfo(Utils.HIDE_VIEW);
            popupNoSignal(Utils.HIDE_VIEW);
            popupChannelList(Utils.HIDE_VIEW);
            createDelayTimer(MSG_SOURCE_DELAY, TIME_SOURCE_DELAY);
        }
    }

    private void popupNoSignal(boolean show_or_hide) {//true:show
        TextView no_signal = (TextView)findViewById(R.id.no_signal);
        if (!show_or_hide) {
            if (no_signal.getVisibility() != View.VISIBLE)
                return;
            no_signal.setVisibility(View.INVISIBLE);
        } else {
            no_signal.setVisibility(View.VISIBLE);
            no_signal.requestLayout();
            popupSourceInfo(Utils.HIDE_VIEW);
        }
    }

    private void popupSourceInfo(boolean show_or_hide) {//true:show
        if (!show_or_hide) {
            if (mSourceInfoLayout.getVisibility() != View.VISIBLE)
                return;
            destroyDelayTimer();
            mSourceInfoLayout.setVisibility(View.INVISIBLE);
        } else {
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
            popupNoSignal(Utils.HIDE_VIEW);
            popupSourceMenu(Utils.HIDE_VIEW);
            popupChannelList(Utils.HIDE_VIEW);
            mSourceInfoLayout.setVisibility(View.VISIBLE);
            createDelayTimer(MSG_INFO_DELAY, TIME_INFO_DELAY);
        }
    }

    private void popupChannelList(boolean show_or_hide) {
        if (!show_or_hide) {//hide
            destroyDelayTimer();
            mChannelListLayout.hide();
        } else {
            popupChannelList(Utils.SHOW_VIEW, -1);
        }
    }

    private void popupChannelList(boolean show_or_hide, int type) {
        if (!show_or_hide) {//hide
            destroyDelayTimer();
            mChannelListLayout.hide();
        } else {
            if (type == -1)
                return;
            switch (type) {
                case Utils.ATV_FAV_LIST:
                    mChannelListLayout.initView(type, mSourceInput.getChannelVideoList());
                    break;
                case Utils.ATV_LIST:
                    mChannelListLayout.initView(type, mSourceInput.getChannelVideoList());
                    break;
                case Utils.DTV_FAV_LIST:
                    mChannelListLayout.initView(type, mSourceInput.getChannelVideoList(),
                            mSourceInput.getChannelRadioList());
                    break;
                case Utils.DTV_LIST:
                    mChannelListLayout.initView(type, mSourceInput.getChannelVideoList(),
                            mSourceInput.getChannelRadioList());
                    break;
                default:
                    break;
            }
            popupSourceMenu(Utils.HIDE_VIEW);
            popupSourceInfo(Utils.HIDE_VIEW);
            popupNoSignal(Utils.HIDE_VIEW);
            mChannelListLayout.show();
            createDelayTimer(MSG_CHANNEL_LIST_DELAY, TIME_CHANNEL_LIST_DELAY);
        }
    }

    private void inflatCurrentInfoLayout() {
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
        inflatCurrentInfoLayout();
        mInfoLabel.setText(mSourceInput.getSourceLabel());
        mInfoNumber.setText(mSourceInput.getChannelNumber());
        mInfoName.setText(mSourceInput.getChannelName());
    }

    private void initATVInfo() {
        inflatCurrentInfoLayout();
        mInfoLabel.setText(mSourceInput.getSourceLabel());
        if (isNumberSwitching) {
            mInfoNumber.setText(keyInputNumber);
            mInfoName.setText("");
        } else {
            int index = mSourceInput.getChannelIndex();
            mInfoNumber.setText(index != -1 ? Integer.toString(index) : "");
            mInfoName.setText(TextUtils.isEmpty(mSourceInput.getChannelType()) ? "" : mSourceInput
                    .getChannelType().substring(5));
        }
    }

    private void initDTVInfo() {
        inflatCurrentInfoLayout();
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
        inflatCurrentInfoLayout();
        mInfoLabel.setText(mSourceInput.getSourceLabel());
        if (isNoSignal) {
            mInfoName.setText("");
        } else {
            mInfoName.setText(mSourceInput.getChannelType());
        }
    }

    private void initHmdiInfo() {
        inflatCurrentInfoLayout();
        mInfoLabel.setText(mSourceInput.getSourceLabel());
        if (isNoSignal) {
            mInfoName.setText("");
        } else {
            mInfoName.setText(mSourceInput.getChannelVideoFormat());
        }
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
                    mSourceInput.setChannelType(args);
                    break;
                default:
                    break;
            }
            popupSourceInfo(Utils.SHOW_VIEW);
        }
    }

    /**
     * release something for next resume or destroy. e.g, if exit with home key, clear info which
     * is unknown when next resume for pass through.
     * clear info for pass through, and release session.
     */
    private void releaseBeforeExit() {
        preSwitchSourceInput();
        mSourceView.reset();
    }

    @Override
    protected void onPause() {
        Utils.logd(TAG, "==== onPause ====");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Utils.logd(TAG, "==== onStop ====");
        hasStopped = true;
        releaseBeforeExit();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Utils.logd(TAG, "==== onDestroy ====");
        releaseThread();
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

    private void createDelayTimer(final int msg_event, final int time){
        destroyDelayTimer();
        delayTimer = new Timer();
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                mHandler.obtainMessage(msg_event, time).sendToTarget();
            }
        };
        delayTimer.schedule(task, 0, 1000);
    }

    private void destroyDelayTimer(){
        if (delayTimer != null) {
            delayTimer.cancel();
            delayTimer = null;
        }
        delayCounter = 0;
    }

    @Override
    public boolean handleMessage(Message msg) {
        int max_counter;
        switch (msg.what) {
            case MSG_INFO_DELAY:
                delayCounter++;
                max_counter = (int)msg.obj;
                if (delayCounter > max_counter) {
                    popupSourceInfo(Utils.HIDE_VIEW);
                    Utils.logd(TAG, "====isNoSignal =" + isNoSignal);
                    if (isNoSignal) {
                        popupNoSignal(Utils.SHOW_VIEW);
                    }
                }
                break;
            case MSG_SOURCE_DELAY:
                delayCounter++;
                max_counter = (int)msg.obj;
                if (delayCounter > max_counter) {
                    popupSourceMenu(Utils.HIDE_VIEW);
                    popupSourceInfo(Utils.SHOW_VIEW);
                }
                break;
            case MSG_CHANNEL_KEY_SWITCH:
                if (isSwitchingChannel)
                    switchToSourceInput();
                isSwitchingChannel = false;
                break;
            case MSG_CHANNEL_NUM_SWITCH:
                if (isSwitchingChannel)
                    switchToSourceInput();
                isSwitchingChannel = false;
                isNumberSwitching = false;
                keyInputNumber = "";
                popupSourceInfo(Utils.SHOW_VIEW);
                break;
            case MSG_CHANNEL_LIST_DELAY:
                delayCounter++;
                max_counter = (int)msg.obj;
                if (delayCounter > max_counter) {
                    popupChannelList(Utils.HIDE_VIEW);
                    popupSourceInfo(Utils.SHOW_VIEW);
                }
                break;
            default:
                break;
        }
        return false;
    }

    public class DroidLogicInputCallback extends TvInputCallback {

        @Override
        public void onEvent(String inputId, String eventType, Bundle eventArgs) {
            Utils.logd(TAG, "====onEvent==inputId =" + inputId +", ===eventType ="+ eventType);
            processSessionEvent(inputId, eventType, eventArgs);
        }

        @Override
        public void onVideoAvailable(String inputId) {
            Utils.logd(TAG, "====onVideoAvailable==inputId =" + inputId);

            isNoSignal = false;
            popupNoSignal(Utils.HIDE_VIEW);
            remove_nosignal_time();
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            Utils.logd(TAG, "====onVideoUnavailable==inputId =" + inputId +", ===reason ="+ reason);
            switch (reason) {
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN:
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING:
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING:
                    isNoSignal = true;
                    popupNoSignal(Utils.SHOW_VIEW);
                    break;
                default:
                    break;
            }
            reset_nosignal_time();
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
        if (isNoSignal) {
            mNoSignalShutdownCount = 300;//5min
            no_signal_handler.removeCallbacks(no_signal_runnable);
            no_signal_handler.postDelayed(no_signal_runnable, 0);
        }
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
                            long now = SystemClock.uptimeMillis();
                            KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, DroidLogicKeyEvent.KEYCODE_POWER, 0);
                            KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, DroidLogicKeyEvent.KEYCODE_POWER, 0);
                            InputManager.getInstance().injectInputEvent(down, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
                            InputManager.getInstance().injectInputEvent(up, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
                            Log.d("fuhao", "power down ------------------ ");
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
        int sleepTime = Settings.System.getInt(mContext.getContentResolver(), "sleep_timer", 0);
        mSuspendCount = sleepTime * 60;
        if (mSuspendCount > 0) {
            timeSuspend_handler.removeCallbacks(timeSuspend_runnable);
            timeSuspend_handler.postDelayed(timeSuspend_runnable, 0);
        }
    }

    private void remove_shutdown_time() {
        Settings.System.putInt(mContext.getContentResolver(), "sleep_timer", 0);
        timeSuspend_handler.removeCallbacks(timeSuspend_runnable);
    }

}
