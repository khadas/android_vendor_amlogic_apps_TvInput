package com.droidlogic.tvinput.settings;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.View;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;
import android.text.TextUtils;
import android.media.AudioManager;
import android.media.tv.TvInputInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.CheckBox;
import android.view.LayoutInflater;
import android.widget.Toast;
import android.widget.SimpleAdapter;
import android.widget.ArrayAdapter;
import android.media.tv.TvContract;

import java.util.List;
import java.util.ArrayList;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.tvinput.R;
import com.droidlogic.app.tv.TvControlManager;

public class ChannelSearchActivity extends Activity implements OnClickListener {
    private static final String TAG = "ChannelSearchActivity";

    private OptionUiManagerT mOptionUiManagerT;
    private PowerManager mPowerManager;

    //private TvControlManager mTvControlManager;
    private AudioManager mAudioManager = null;
    private boolean isFinished = false;

    private ProgressBar mProgressBar;
    private TextView mScanningMessage;
    private View mChannelHolder;
    private SimpleAdapter mAdapter;
    private volatile boolean mChannelListVisible;
    private Button mManulScanButton;
    private Button mAutoScanButton;
    private EditText mFrequecyFrom;
    private TextView mFrequecyToText;
    private EditText mFrequecyTo;
    private CheckBox mManualDTV;
    private CheckBox mManualATV;
    private CheckBox mAutoDTV;
    private CheckBox mAutoATV;
    private ListView channelList;
    private Spinner mManualSpinner;
    private Spinner mAutoSpinner;

    private TvInputInfo mTvInputInfo;

    public static final int MANUAL_START = 0;
    public static final int AUTO_START = 1;
    public static final int PROCCESS = 2;
    public static final int CHANNEL = 3;
    public static final int STATUS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tv_channel_scan);
        isFinished = false;

        mOptionUiManagerT = new OptionUiManagerT(this, getIntent(), true);
        mOptionUiManagerT.setHandler(mHandler);
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);

        mProgressBar = (ProgressBar) findViewById(R.id.tune_progress);
        mScanningMessage = (TextView) findViewById(R.id.tune_description);
        channelList = (ListView) findViewById(R.id.channel_list);
        channelList.setAdapter(mAdapter);
        channelList.setOnItemClickListener(null);
        ViewGroup progressHolder = (ViewGroup) findViewById(R.id.progress_holder);
        mChannelHolder = findViewById(R.id.channel_holder);
        mManulScanButton = (Button) findViewById(R.id.tune_manual);
        mManulScanButton.setOnClickListener(this);
        mAutoScanButton = (Button) findViewById(R.id.tune_auto);
        mAutoScanButton.setOnClickListener(this);
        mAutoScanButton.requestFocus();
        mFrequecyFrom = (EditText) findViewById(R.id.tune_frequecy_from);
        mFrequecyTo= (EditText) findViewById(R.id.tune_frequecy_to);
        mFrequecyToText = (TextView) findViewById(R.id.text_tune_frequency_to);
        mManualDTV = (CheckBox) findViewById(R.id.manual_tune_dtv);
        mManualATV = (CheckBox) findViewById(R.id.manual_tune_atv);
        mManualDTV.setChecked(true);
        mManualATV.setChecked(false);
        mAutoDTV = (CheckBox) findViewById(R.id.auto_tune_dtv);
        mAutoATV = (CheckBox) findViewById(R.id.auto_tune_atv);
        mAutoDTV.setChecked(true);
        mAutoATV.setChecked(false);
        if (!(mOptionUiManagerT.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV)) {
            mManualDTV.setEnabled(false);
            mManualATV.setEnabled(false);
            mAutoDTV.setEnabled(false);
            mAutoATV.setEnabled(false);
        }
        //if (!(mOptionUiManagerT.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV)) {
            mFrequecyTo.setVisibility(View.GONE);
            mFrequecyToText.setVisibility(View.GONE);
        //}
        initSpinner();

        startShowActivityTimer();

        mOptionUiManagerT.startTvPlayAndSetSourceInput();
    }

    private ArrayAdapter<String> arr_adapter;
    private List<String> data_list;

    private void initSpinner() {
        data_list = new ArrayList<String>();
        final int[] type = {R.string.dtmb, R.string.dvbc, R.string.dvbt, R.string.dvbt2, R.string.atsc_t, R.string.atsc_c, R.string.isdb_t};
        for (int i = 0; i < type.length; i++) {
            data_list.add(getString(type[i]));
        }
        mManualSpinner = (Spinner) findViewById(R.id.manual_spinner);
        mAutoSpinner = (Spinner) findViewById(R.id.auto_spinner);
        arr_adapter= new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, data_list);
        arr_adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mManualSpinner.setAdapter(arr_adapter);
        mAutoSpinner.setAdapter(arr_adapter);
        mManualSpinner.setSelection(mOptionUiManagerT.getDtvTypeStatus());
        mAutoSpinner.setSelection(mOptionUiManagerT.getDtvTypeStatus());
        mManualSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //mAutoSpinner.setSelection(mOptionUiManagerT.getDtvTypeStatus());
                if (position >= 0 && position <= 6) {
                    mOptionUiManagerT.setDtvType(position);
                    mAutoSpinner.setSelection(position, false);
                    //mAutoSpinner.postInvalidate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mAutoSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //mManualSpinner.setSelection(mOptionUiManagerT.getDtvTypeStatus());
                if (position >= 0 && position <= 6) {
                    mOptionUiManagerT.setDtvType(position);
                    mManualSpinner.setSelection(position, false);
                    //mManualSpinner.postInvalidate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.arg1) {
                case MANUAL_START:
                    if (ShowNoAtvFrequency()) {
                        initparametres(MANUAL_SEARCH);
                        mOptionUiManagerT.callManualSearch();
                    }
                    break;
                case AUTO_START:
                    initparametres(AUTO_SEARCH);
                    mOptionUiManagerT.callAutosearch();
                    break;
                case PROCCESS:
                    mProgressBar.setProgress(msg.what);
                    break;
                case CHANNEL:
                    mAdapter = (SimpleAdapter) msg.obj;
                    channelList.setAdapter(mAdapter);
                    channelList.setOnItemClickListener(null);
                    if (mChannelHolder.getVisibility() == View.GONE) {
                        mChannelHolder.setVisibility(View.VISIBLE);
                    }
                    break;
                case STATUS:
                    mScanningMessage.setText((String) msg.obj);
                    break;
                default :
                    break;
            }
        }
    };

    private static final int MANUAL_SEARCH = 0;
    private static final int AUTO_SEARCH = 1;

   private void initparametres(int type) {
       if (type == MANUAL_SEARCH) {
           mOptionUiManagerT.setSearchSys(mManualDTV.isChecked(), mManualATV.isChecked());
	 } else if (AUTO_SEARCH == type) {
            mOptionUiManagerT.setSearchSys(mAutoDTV.isChecked(), mAutoATV.isChecked());
       }
	 setFrequency();
        if (mChannelHolder.getVisibility() == View.VISIBLE) {
            mChannelHolder.setVisibility(View.GONE);
        }
   }

    private void setFrequency() {
        String atvdefaultbegain = "42250";
        String atvdefaultend = "868250";
        String dtvdefaultstart = "0";
        String from = null;
        String to = null;
        if (mFrequecyFrom.getText() != null && mFrequecyFrom.getText().length() > 0) {
            from = mFrequecyFrom.getText().toString();
        } else {
            from = null;
        }
        if (mFrequecyTo.getText() != null && mFrequecyTo.getText().length() > 0) {
            to = mFrequecyTo.getText().toString();
        } else {
            to= atvdefaultend;
        }
        if (mOptionUiManagerT.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
            mOptionUiManagerT.setFrequency(from != null ? from : atvdefaultbegain, to);
        } else {
            mOptionUiManagerT.setFrequency(from != null ? from : dtvdefaultstart, to);
        }
    }

    private boolean ShowNoAtvFrequency() {
        boolean status = false;
        if (mFrequecyFrom.getText() != null && mFrequecyFrom.getText().length() > 0) {
            status = true;
        } else {
            status = false;
            ShowToastTint(getString(R.string.set_frquency_channel));
        }
        return status;
    }

    private void ShowToastTint(String text) {
        LayoutInflater inflater = (LayoutInflater)ChannelSearchActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.layout_toast, null);

        TextView propmt = (TextView)layout.findViewById(R.id.toast_content);
        propmt.setText(text);

        Toast toast = new Toast(ChannelSearchActivity.this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void sendMessage(int type, int message, String information) {
        Message msg = new Message();
        msg.arg1 = type;
        msg.what = message;
        msg.obj = information;
        mHandler.sendMessage(msg);
    }

    //boolean isManualStarted = false;
    boolean isAutoStarted = false;

    @Override
    public void onClick(View v) {
         switch (v.getId()) {
             case R.id.tune_manual:
                 sendMessage(MANUAL_START, 0, null);
                 break;
             case R.id.tune_auto:
                 if (!isAutoStarted) {
                     isAutoStarted = true;
                     mAutoScanButton.setText(R.string.ut_stop_channel_scan);
                 } else {
                     isAutoStarted = false;
                     mAutoScanButton.setText(R.string.ut_auto_scan);
                 }
                 sendMessage(AUTO_START, 0, null);
                 break;
             default:
                 break;
         }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Log.d(TAG, "==== focus =" + getCurrentFocus() + ", keycode =" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mOptionUiManagerT.isSearching()) {
                    mOptionUiManagerT.DtvStopScan();
                } else {
                    finish();
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
                if (mOptionUiManagerT.isSearching()) {
                    mOptionUiManagerT.DtvStopScan();
                    return true;
                }

                finish();
                break;
            /*case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mAudioManager.isMasterMute()) {
                    mAudioManager.setMasterMute(false, AudioManager.FLAG_PLAY_SOUND);
                    mSettingsManager.sendBroadcastToTvapp("unmute");
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                if (mAudioManager.isMasterMute()) {
                    mAudioManager.setMasterMute(false, AudioManager.FLAG_PLAY_SOUND);
                    mSettingsManager.sendBroadcastToTvapp("unmute");
                } else {
                    mAudioManager.setMasterMute(true, AudioManager.FLAG_PLAY_SOUND);
                    mSettingsManager.sendBroadcastToTvapp("mute");
                }
                return true;*/
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                startShowActivityTimer();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    public OptionUiManagerT getOptionUiManager () {
        return mOptionUiManagerT;
    }

    @Override
    public void finish() {
        isFinished = true;

        if (!mPowerManager.isScreenOn()) {
            Log.d(TAG, "TV is going to sleep, stop tv");
            return;
        }

        setResult(mOptionUiManagerT.getActivityResult());
        super.finish();
    }

    @Override
    public void onResume() {
        if (isFinished) {
            resume();
            isFinished = false;
        }

        super.onResume();
        Log.d(TAG, "onResume");
        IntentFilter filter = new IntentFilter();
        filter.addAction(DroidLogicTvUtils.ACTION_CHANNEL_CHANGED);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, filter);

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        unregisterReceiver(mReceiver);

        if (mOptionUiManagerT.isSearching()) {
            mOptionUiManagerT.DtvStopScan();

            if (!mPowerManager.isScreenOn()) {
                mOptionUiManagerT.StopTv();
            }
        }

        if (!isFinishing()) {
            finish();
        }
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        release();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "finalized");
    }

    public void startShowActivityTimer () {
        handler.removeMessages(0);

        int seconds = Settings.System.getInt(getContentResolver(), SettingsManager.KEY_MENU_TIME, SettingsManager.DEFUALT_MENU_TIME);
        handler.sendEmptyMessageDelayed(0, 30 * 1000);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (!mOptionUiManagerT.isSearching() && !imm. isAcceptingText())
                finish();
            else  {
                int seconds = Settings.System.getInt(getContentResolver(), SettingsManager.KEY_MENU_TIME, SettingsManager.DEFUALT_MENU_TIME);
                sendEmptyMessageDelayed(0, 30 * 1000);
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            /*if (action.equals(DroidLogicTvUtils.ACTION_CHANNEL_CHANGED)) {
                mSettingsManager.setCurrentChannelData(intent);
                    mOptionUiManagerT.init(mSettingsManager);
                currentFragment.refreshList();
            } else */if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra("reason");
                if (TextUtils.equals(reason, "homekey")) {
                    mOptionUiManagerT.StopTv();
                    Log.d(TAG,"stop tv when exiting by home key");
                    finish();
                }
            }
        }
    };

    private void resume() {
        mOptionUiManagerT = new OptionUiManagerT(this,  getIntent(), true);

        startShowActivityTimer();
    }

    private void release() {
        handler.removeCallbacksAndMessages(null);
        mOptionUiManagerT.release();
        mOptionUiManagerT = null;
    }
}
