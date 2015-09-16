package com.droidlogic.tv;


import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.DroidLogicTvUtils;
import com.droidlogic.ui.SourceButton;
import com.droidlogic.ui.SourceButton.SourceButtonListener;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.media.tv.TvView.TvInputCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DroidLogicTv extends Activity implements Callback, SourceButtonListener {
    private static final String TAG = "DroidLogicTv";

    private Context mContext;
    private TvInputManager mTvInputManager;

    private TvView mSourceView;

    private LinearLayout mSourceMenuLayout;
    private LinearLayout mSourceInfoLayout;

    //max index of all hardware devices in mSourceMenuLayout
    private int maxHardwareIndex = 0;

    private int msigType;
    private String mSigInfo;
    private String mSigLabel;

    private boolean isNoSignal;
    private boolean isNoSignalShowing;
    private boolean isSourceMenuShowing;
    private boolean isSourceInfoShowing;

    private Timer delayTimer = null;
    private int delayCounter = 0;

    private Handler mHandler;
    private static final int MSG_INFO_DELAY = 0;
    private static final int MSG_INFO_DELAY_TIME = 5;
    private static final int MSG_SOURCE_DELAY = 1;
    private static final int MSG_SOURCE_DELAY_TIME = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initVideoView();
        init();
    }

    private void initVideoView() {
        ViewGroup root = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
        root.setFocusable(true);
        SurfaceView surfaceView = new SurfaceView(this);
        root.addView(surfaceView, 0);
        if (surfaceView != null) {
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceDestroyed(SurfaceHolder arg0) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.reset();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    try {
                        mediaPlayer.setDataSource("tvin:test");
                        mediaPlayer.setDisplay(holder);
                        mediaPlayer.prepare();
                    } catch (Exception e) {
                        Utils.loge(TAG, e.toString());
                    }
                    mediaPlayer.start();
                }

                @Override
                public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    private void init() {
        mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);

        mContext = getApplicationContext();
        mHandler = new Handler(this);

        mSourceView = (TvView) findViewById(R.id.source_view);
        mSourceView.setCallback(new DroidLogicInputCallback());

        mSourceMenuLayout = (LinearLayout)findViewById(R.id.menu_layout);
        mSourceInfoLayout = (LinearLayout)findViewById(R.id.info_layout);

        initSourceMenuLayout();
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
            sb.setSourceButttonListener(this);
        }
    }

    @Override
    protected void onResume() {
        Utils.logd(TAG, "==onResume====");

        switchToSourceInput(getDefaultSource());

        popupSourceInfo(Utils.SHOW_VIEW);
        super.onResume();
    }

    /**
     * get the default source input at first time.
     */
    private String getDefaultSource(){
        //get default source, now set hdmi1 as default

        String input_id = "com.droidlogic.tvinput/.services.HdmiInputService/HW5";
        TvInputInfo info = mTvInputManager.getTvInputInfo(input_id);
        mSigLabel = info.loadLabel(mContext).toString();
        msigType = DroidLogicTvUtils.SIG_INFO_TYPE_HDMI;

        return input_id;
    }

    private void switchToSourceInput(String inputId) {
        Uri channel = TvContract.buildChannelUriForPassthroughInput(inputId);
        Utils.logd(TAG, "channelUri switching to is " + channel);

        mSourceView.tune(inputId, channel);

        if (isSourceMenuShowing)
            popupSourceMenu(Utils.HIDE_VIEW);
    }

    @Override
    public void onButtonClick(String inputId, String sourceType) {
        if (!TextUtils.isEmpty(mSigLabel) && sourceType.equals(mSigLabel))
            return;
        mSigInfo = null;
        mSigLabel = sourceType;
        switchToSourceInput(inputId);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Utils.logd(TAG, "====keycode =" + keyCode);

        if (isSourceMenuShowing) {
            createDelayTimer(MSG_SOURCE_DELAY, MSG_SOURCE_DELAY_TIME);
        }
        switch (keyCode) {
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_SOURCE_LIST:
            case DroidLogicKeyEvent.KEYCODE_MENU:
                popupSourceMenu(
                        mSourceMenuLayout.getVisibility() == View.VISIBLE ? Utils.HIDE_VIEW : Utils.SHOW_VIEW);
                return true;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_TVINFO:
                popupSourceInfo(Utils.SHOW_VIEW);
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void popupSourceMenu(boolean show_or_hide) {//ture:show
        if (!show_or_hide) {
            destroyDelayTimer();
            isSourceMenuShowing = false;
            mSourceMenuLayout.setVisibility(View.INVISIBLE);
            popupSourceInfo(Utils.SHOW_VIEW);
        } else {
            isSourceMenuShowing = true;
            mSourceMenuLayout.setVisibility(View.VISIBLE);
            mSourceMenuLayout.requestLayout();
            if (isSourceInfoShowing)
                popupSourceInfo(Utils.HIDE_VIEW);
            if (isNoSignalShowing)
                popupNoSignal(Utils.HIDE_VIEW);
            createDelayTimer(MSG_SOURCE_DELAY, MSG_SOURCE_DELAY_TIME);
        }
    }

    private void popupNoSignal(boolean show_or_hide) {//true:show
        TextView no_signal = (TextView)findViewById(R.id.no_signal);
        if (!show_or_hide) {
            isNoSignalShowing = false;
            no_signal.setVisibility(View.INVISIBLE);
        } else {
            isNoSignalShowing = true;
            no_signal.setVisibility(View.VISIBLE);
            no_signal.requestLayout();
            if (isSourceInfoShowing)
                popupSourceInfo(Utils.HIDE_VIEW);
        }
    }

    private void popupSourceInfo(boolean show_or_hide) {//true:show
        if (!show_or_hide) {
            destroyDelayTimer();
            isSourceInfoShowing = false;
            mSourceInfoLayout.setVisibility(View.INVISIBLE);
        } else {
            switch (msigType) {
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
                    break;
            }
            if (isNoSignalShowing) {
                popupNoSignal(Utils.HIDE_VIEW);
            }
            if (isSourceMenuShowing) {
                popupSourceMenu(Utils.HIDE_VIEW);
            }
            mSourceInfoLayout.setVisibility(View.VISIBLE);
            isSourceInfoShowing = true;
            createDelayTimer(MSG_INFO_DELAY, MSG_INFO_DELAY_TIME);
        }
    }

    private void initATVInfo() {
        //TODO
    }

    private void initDTVInfo() {
        //TODO
    }

    private void initAVInfo() {
        TextView tv_type;
        TextView tv_rel;
        if (mSourceInfoLayout.getChildCount() == 0) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            mSourceInfoLayout.addView(inflate.inflate(R.layout.hdmi_av_info, mSourceInfoLayout, false));
        }
        tv_type = (TextView) findViewById(R.id.ha_info_type);
        tv_rel = (TextView) findViewById(R.id.ha_info_value);
        tv_type.setText(mSigLabel);
        if (!TextUtils.isEmpty(mSigInfo))
            tv_rel.setText(mSigInfo);
        else
            tv_rel.setText("");
    }

    private void initHmdiInfo() {
        TextView tv_type;
        TextView tv_rel;
        if (mSourceInfoLayout.getChildCount() == 0) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            mSourceInfoLayout.addView(inflate.inflate(R.layout.hdmi_av_info, mSourceInfoLayout, false));
        }
        tv_type = (TextView) findViewById(R.id.ha_info_type);
        tv_rel = (TextView) findViewById(R.id.ha_info_value);
        tv_type.setText(mSigLabel);
        if (!TextUtils.isEmpty(mSigInfo))
            tv_rel.setText(mSigInfo);
        else
            tv_rel.setText("");
    }

    private void processSessionEvent(String inputId, String eventType, Bundle eventArgs) {
        if (eventType.equals(DroidLogicTvUtils.SIG_INFO_EVENT)) {//sig_info
            msigType = eventArgs.getInt(DroidLogicTvUtils.SIG_INFO_TYPE);
            String args = eventArgs.getString(DroidLogicTvUtils.SIG_INFO_ARGS);
            if (msigType == DroidLogicTvUtils.SIG_INFO_TYPE_HDMI) {
                String[] temp = args.split("_");
                mSigInfo = temp[0] + "_" + temp[1];
            } else if (msigType == DroidLogicTvUtils.SIG_INFO_TYPE_AV) {
                mSigInfo = args;
            }
            mSigLabel = eventArgs.getString(DroidLogicTvUtils.SIG_INFO_LABEL);
            popupSourceInfo(Utils.SHOW_VIEW);
        }
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mSourceView.reset();
        super.onDestroy();
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
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            Utils.logd(TAG, "====onVideoUnavailable==inputId =" + inputId +", ===reason ="+ reason);

            if (reason == TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN) {
                isNoSignal = true;
            }
            popupNoSignal(Utils.SHOW_VIEW);
        }
    }

}
