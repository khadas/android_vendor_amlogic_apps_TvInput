package com.droidlogic.tv;


import java.util.Timer;
import java.util.TimerTask;

import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.DroidLogicTvUtils;
import com.droidlogic.ui.SourceButton;

import android.app.Activity;
import android.content.Context;
import android.media.tv.TvContract;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DroidLogicTv extends Activity implements OnClickListener, Callback {
    private static final String TAG = "DroidLogicTv";

    private Context mContext;

    private TvView mSourceView;
    private TvInputManager mTvInputManager;

    private LinearLayout mSourceMenuLayout;
    private LinearLayout mSourceInfoLayout;
    private int mCurrentSourceType;
    private String mSourceInfo;
    private boolean isNoSignal;
    private boolean isNoSignalShowing;
    private boolean isSourceMenuShowing;
    private boolean isSourceInfoShowing;

    private Timer delayTimer = null;
    private int delayCounter = 0;

    private Handler mHandler;
    private static final int MSG_INFO_DELAY = 0;
    private static final int MSG_SOURCE_DELAY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
    }

    private void init() {
        mContext = getApplicationContext();
        mHandler = new Handler(this);

        mSourceView = (TvView) findViewById(R.id.source_view);
        mSourceView.setCallback(new DroidLogicInputCallback());

        mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);
        Utils.mTvInputManager = mTvInputManager;

        SourceButton atv = (SourceButton) findViewById(R.id.atv);
        atv.setOnClickListener(this);
        //atv.setTvInputInfo(Utils.getInputInfo(atv.getSourceType()));
        SourceButton dtv = (SourceButton) findViewById(R.id.dtv);
        dtv.setOnClickListener(this);
        //dtv.setTvInputInfo(Utils.getInputInfo(dtv.getSourceType()));
        SourceButton av = (SourceButton) findViewById(R.id.av);
        av.setOnClickListener(this);
        av.setTvInputInfo(Utils.getInputInfo(av.getSourceType()));
        SourceButton hdmi1 = (SourceButton) findViewById(R.id.hdmi1);
        hdmi1.setOnClickListener(this);
        hdmi1.setTvInputInfo(Utils.getInputInfo(hdmi1.getSourceType()));
        SourceButton hdmi2 = (SourceButton) findViewById(R.id.hdmi2);
        hdmi2.setOnClickListener(this);
        hdmi2.setTvInputInfo(Utils.getInputInfo(hdmi2.getSourceType()));
        SourceButton hdmi3 = (SourceButton) findViewById(R.id.hdmi3);
        hdmi3.setOnClickListener(this);
        hdmi3.setTvInputInfo(Utils.getInputInfo(hdmi3.getSourceType()));
        mSourceMenuLayout = (LinearLayout)findViewById(R.id.menu_layout);
        mSourceInfoLayout = (LinearLayout)findViewById(R.id.info_layout);

        atv.requestFocus();

        switchToSourceInput(getDefaultSource());

        popupSourceInfo(Utils.SHOW_VIEW);
    }

    /**
     * get the default source input at first time.
     */
    private int getDefaultSource(){
        //get default source, now set hdmi1 as default

        mCurrentSourceType = Utils.SOURCE_TYPE_HDMI1;
        return mCurrentSourceType;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Utils.logd(TAG, "====keycode =" + keyCode);

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
            createDelayTimer(MSG_SOURCE_DELAY, 5);
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
        }
    }

    private void popupSourceInfo(boolean show_or_hide) {//true:show
        if (!show_or_hide) {
            destroyDelayTimer();
            isSourceInfoShowing = false;
            mSourceInfoLayout.setVisibility(View.INVISIBLE);
        } else {
            switch (mCurrentSourceType) {
                case Utils.SOURCE_TYPE_ATV:
                    initATVInfo();
                    break;
                case Utils.SOURCE_TYPE_DTV:
                    initDTVInfo();
                    break;
                case Utils.SOURCE_TYPE_AV:
                    initAVInfo("AV");
                    break;
                case Utils.SOURCE_TYPE_HDMI1:
                    initHmdiInfo("HDMI1");
                    break;
                case Utils.SOURCE_TYPE_HDMI2:
                    initHmdiInfo("HDMI2");
                    break;
                case Utils.SOURCE_TYPE_HDMI3:
                    initHmdiInfo("HDMI3");
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
            createDelayTimer(MSG_INFO_DELAY, 5);//hide it automatically after 5s
        }
    }

    private void initATVInfo() {
        //TODO
    }

    private void initDTVInfo() {
        //TODO
    }

    /**
     * @param type Current AV channel name, AV1 or AV2. if here is just only one, type is AV.
     */
    private void initAVInfo(String type) {
        TextView tv_type;
        TextView tv_rel;
        if (mSourceInfoLayout.getChildCount() == 0) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            mSourceInfoLayout.addView(inflate.inflate(R.layout.hdmi_av_info, mSourceInfoLayout, false));
        }
        tv_type = (TextView) findViewById(R.id.ha_info_type);
        tv_rel = (TextView) findViewById(R.id.ha_info_value);
        tv_type.setText(type);
        if (!TextUtils.isEmpty(mSourceInfo))
            tv_rel.setText(mSourceInfo);
        else
            tv_rel.setText("");
    }

    /**
     * @param type Current HDMI channel name, HDMI1, HDMI2 or HDMI3.
     */
    private void initHmdiInfo(String type) {
        TextView tv_type;
        TextView tv_rel;
        if (mSourceInfoLayout.getChildCount() == 0) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            mSourceInfoLayout.addView(inflate.inflate(R.layout.hdmi_av_info, mSourceInfoLayout, false));
        }
        tv_type = (TextView) findViewById(R.id.ha_info_type);
        tv_rel = (TextView) findViewById(R.id.ha_info_value);
        tv_type.setText(type);
        if (!TextUtils.isEmpty(mSourceInfo))
            tv_rel.setText(mSourceInfo);
        else
            tv_rel.setText("");
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.atv:
                break;
            case R.id.dtv:
                break;
            case R.id.av:
                switchToSourceInput(Utils.SOURCE_TYPE_AV);
                break;
            case R.id.hdmi1:
                switchToSourceInput(Utils.SOURCE_TYPE_HDMI1);
                break;
            case R.id.hdmi2:
                switchToSourceInput(Utils.SOURCE_TYPE_HDMI2);
                break;
            case R.id.hdmi3:
                switchToSourceInput(Utils.SOURCE_TYPE_HDMI3);
                break;
            default:
                break;
        }
    }

    private void switchToSourceInput(int source_type) {
        Utils.logd(TAG, "======switchToSourceInput=======");
        String input_id = Utils.getInputId(source_type);
        Utils.logd(TAG, "Input id switching to is " + input_id);
        Uri channelUri = TvContract.buildChannelUriForPassthroughInput(input_id);
        Utils.logd(TAG, "channelUri switching to is " + channelUri);
        mSourceView.tune(input_id, channelUri);
        mCurrentSourceType = source_type;
    }

    private void processSessionEvent(String inputId, String eventType, Bundle eventArgs) {
        if (eventType.equals(DroidLogicTvUtils.SIG_INFO_EVENT)) {//sig_info
            String type = eventArgs.getString(DroidLogicTvUtils.SIG_INFO_TYPE);
            String args = eventArgs.getString(DroidLogicTvUtils.SIG_INFO_ARGS);
            if (type.equals(DroidLogicTvUtils.SIG_INFO_TYPE_HDMI)) {
                String[] temp = args.split("_");
                mSourceInfo = temp[0] + "_" + temp[1];
            } else if (type.equals(DroidLogicTvUtils.SIG_INFO_TYPE_AV)) {
                mSourceInfo = args;
            }
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
            mCurrentSourceType = Utils.getSourceType(inputId);
            processSessionEvent(inputId, eventType, eventArgs);
        }

        @Override
        public void onVideoAvailable(String inputId) {
            Utils.logd(TAG, "====onVideoAvailable==inputId =" + inputId);

            isNoSignal = false;
            mCurrentSourceType = Utils.getSourceType(inputId);
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            Utils.logd(TAG, "====onVideoUnavailable==inputId =" + inputId +", ===reason ="+ reason);

            if (reason == TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN) {
                isNoSignal = true;
            }
            mCurrentSourceType = Utils.getSourceType(inputId);
            mSourceInfo = null;
            popupSourceInfo(Utils.SHOW_VIEW);
        }
    }

}
