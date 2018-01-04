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
import android.app.ProgressDialog;

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
    private Spinner mAtscOption;
    private Spinner mAtscSearchTypeOption;

    private TvInputInfo mTvInputInfo;

    public static final int MANUAL_START = 0;
    public static final int AUTO_START = 1;
    public static final int PROCCESS = 2;
    public static final int CHANNEL = 3;
    public static final int STATUS = 4;
    public static final int NUMBER_SEARCH_START = 5;
    public static final int ATSCC_OPTION_DEFAULT = 0;

    //number search  key "numbersearch" true or false, "number" 2~135
    public static final String NUMBERSEARCH = "numbersearch";
    public static final String NUMBER = "number";
    public static final String ATSC_TV_SEARCH_SYS = "atsc_tv_search_sys";
    public static final boolean NUMBERSEARCHDEFAULT = false;
    public static final int NUMBERDEFAULT = -1;
    private ProgressDialog mProDia;
    private boolean isNumberSearchMode = false;
    private int mNumber = -1;
    private boolean hasFoundChannel = false;
    private boolean mNumberSearchDtv = true;
    private boolean  mNumberSearchAtv = true;
    private int hasFoundChannelNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Intent in = getIntent();

        mOptionUiManagerT = new OptionUiManagerT(this, in, true);
        mOptionUiManagerT.setHandler(mHandler);
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);

        if (in != null && in.getBooleanExtra(NUMBERSEARCH, NUMBERSEARCHDEFAULT)) {
            mNumber = in.getIntExtra(NUMBER, NUMBERDEFAULT);
            if (NUMBERDEFAULT == mNumber) {
                finish();
                return;
            }

            isNumberSearchMode = true;
            setContentView(R.layout.tv_number_channel_scan);
        } else {
            setContentView(R.layout.tv_channel_scan);
        }
        isFinished = false;

        if (isNumberSearchMode) {
            initNumberSearch(this);
            startShowActivityTimer();
            mOptionUiManagerT.startTvPlayAndSetSourceInput();
            return;
        }

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
        mManualATV.setOnClickListener(this);
        mAutoDTV = (CheckBox) findViewById(R.id.auto_tune_dtv);
        mAutoATV = (CheckBox) findViewById(R.id.auto_tune_atv);
        mAutoDTV.setChecked(true);
        mAutoATV.setChecked(false);
        mAutoATV.setOnClickListener(this);
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

    private void initNumberSearch(Context context) {
        mProDia= new ProgressDialog(this);
        mProDia.setTitle(R.string.number_search_title);
        mProDia.setMessage(getResources().getString(R.string.number_search_dtv));
        mProDia.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProDia.show();
        sendMessage(NUMBER_SEARCH_START, 0, null);
    }

    private void exitNumberSearch() {
        if (mProDia != null && mProDia.isShowing()) {
            mProDia.dismiss();
        }
    }

    private ArrayAdapter<String> arr_adapter;
    private List<String> data_list;
    private ArrayAdapter<String> arr_adapter_atsc_c;
    private List<String> data_list_atsc_c;
    private ArrayAdapter<String> arr_adapter_atsc_tv_search_type;
    private List<String> data_list_atsc_tv_search_type;

    private void initSpinner() {
        data_list = new ArrayList<String>();
        data_list_atsc_c = new ArrayList<String>();
        data_list_atsc_tv_search_type = new ArrayList<String>();
        final int[] type = {R.string.dtmb, R.string.dvbc, R.string.dvbt, R.string.dvbt2, R.string.atsc_t, R.string.atsc_c, R.string.isdb_t};
        final int[] type_atsc_c = {R.string.ut_tune_atsc_c_auto, R.string.ut_tune_atsc_c_standard, R.string.ut_tune_atsc_c_lrc, R.string.hrc};
        final int[] type_tv_search = {R.string.tv_search_sys_auto, R.string.tv_search_sys_pal, R.string.tv_search_sys_ntsc, R.string.tv_search_sys_secam};
        for (int i = 0; i < type.length; i++) {
            data_list.add(getString(type[i]));
        }
        for (int i = 0; i < type_atsc_c.length; i++) {
            data_list_atsc_c.add(getString(type_atsc_c[i]));
        }
        for (int i = 0; i < type_tv_search.length; i++) {
            data_list_atsc_tv_search_type.add(getString(type_tv_search[i]));
        }
        mManualSpinner = (Spinner) findViewById(R.id.manual_spinner);
        mAutoSpinner = (Spinner) findViewById(R.id.auto_spinner);
        mAtscOption = (Spinner) findViewById(R.id.atsc_c_spinner);
        mAtscSearchTypeOption = (Spinner) findViewById(R.id.atsc_tv_search_sys);
        arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, data_list);
        arr_adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mManualSpinner.setAdapter(arr_adapter);
        mAutoSpinner.setAdapter(arr_adapter);
        mManualSpinner.setSelection(mOptionUiManagerT.getDtvTypeStatus());
        mAutoSpinner.setSelection(mOptionUiManagerT.getDtvTypeStatus());
        arr_adapter_atsc_c = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, data_list_atsc_c);
        arr_adapter_atsc_c.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mAtscOption.setAdapter(arr_adapter_atsc_c);
        arr_adapter_atsc_tv_search_type = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, data_list_atsc_tv_search_type);
        arr_adapter_atsc_tv_search_type.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mAtscSearchTypeOption.setAdapter(arr_adapter_atsc_tv_search_type);
        //mAtscOption.setSelection(ATSCC_OPTION_DEFAULT);
        if (getAtsccListMode() > HRC) {
            mAtscOption.setSelection(STANDARD);//display the first item
        } else {
            mAtscOption.setSelection(getAtsccListMode() + 1);//display std lrc hrc
        }
        if (mOptionUiManagerT.getDtvTypeStatus() != OptionUiManagerT.SET_ATSC_C) {
            mAtscOption.setEnabled(false);
        }
        if (getTvSearchTypeSys() == -1) {
            mAtscSearchTypeOption.setSelection(TV_SEARCH_SYS_AUTO);
        } else {
            mAtscSearchTypeOption.setSelection(getTvSearchTypeSys());
        }
        if (mManualATV.isChecked() || mAutoATV.isChecked()) {
            mAtscSearchTypeOption.setEnabled(true);
        } else {
            mAtscSearchTypeOption.setEnabled(false);
        }

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
                    final int standard = 0;
                    if (position == OptionUiManagerT.SET_ATSC_C) {
                        mAtscOption.setEnabled(true);
                        //mAtscOption.setSelection(getAtsccListMode(), false);
                        if (getAtsccListMode() > HRC) {
                            mAtscOption.setSelection(STANDARD);//display the first item
                        } else {
                            mAtscOption.setSelection(getAtsccListMode() + 1);//display std lrc hrc
                        }
                        mOptionUiManagerT.setAtsccSearchSys(getAtsccListMode());
                    } else {
                        //mAtscOption.setSelection(getAtsccListMode(), false);
                        if (getAtsccListMode() > HRC) {
                            mAtscOption.setSelection(STANDARD);//display the first item
                        } else {
                            mAtscOption.setSelection(getAtsccListMode() + 1);//display std lrc hrc
                        }
                        mAtscOption.setEnabled(false);
                        mOptionUiManagerT.setAtsccSearchSys(getAtsccListMode());
                    }
                    //mManualSpinner.postInvalidate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mAtscOption.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position <= 3) {
                    if (position > STANDARD) {
                        mOptionUiManagerT.setAtsccSearchSys(position - 1);
                        setAtsccListMode(position - 1);
                    } else {
                        mOptionUiManagerT.setAtsccSearchSys(AUTO);
                        setAtsccListMode(AUTO);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mAtscSearchTypeOption.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position <= 3) {
                    setTvSearchTypeSys(position);
                } else {
                    setTvSearchTypeSys(TV_SEARCH_SYS_AUTO);
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
                    initparametres(MANUAL_SEARCH);
                    mOptionUiManagerT.callManualSearch();
                    break;
                case AUTO_START:
                    initparametres(AUTO_SEARCH);
                    mOptionUiManagerT.callAutosearch();
                    break;
                case PROCCESS:
                    if (!isNumberSearchMode) {
                        mProgressBar.setProgress(msg.what);
                    } else {
                        mProDia.setProgress(msg.what);
                    }
                    break;
                case CHANNEL:
                    if (!isNumberSearchMode) {
                        mAdapter = (SimpleAdapter) msg.obj;
                        channelList.setAdapter(mAdapter);
                        channelList.setOnItemClickListener(null);
                        if (mChannelHolder.getVisibility() == View.GONE) {
                            mChannelHolder.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                case STATUS:
                    if (!isNumberSearchMode) {
                        mScanningMessage.setText((String) msg.obj);
                    } else {
                        hasFoundChannelNumber = msg.what;
                        if (hasFoundChannelNumber > 0) {
                            hasFoundChannel = true;
                            Log.d(TAG, "find channel num = " + hasFoundChannelNumber);
                            if (mOptionUiManagerT.isSearching()) {
                                mOptionUiManagerT.DtvStopScan();
                            }
                            exitNumberSearch();
                            finish();
                            return;
                        }
                        if ("exit".equals((String) msg.obj)) {//scan exit
                            exitNumberSearch();
                            finish();
                        }
                    }
                    break;
                case NUMBER_SEARCH_START:
                    handler.postDelayed(TimeOutStopScanRunnable, 30000);//timeout 30s
                    initparametres(NUMBER_SEARCH_START);
                    mOptionUiManagerT.callManualSearch();
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
        } else if (NUMBER_SEARCH_START== type) {
            mOptionUiManagerT.setSearchSys(mNumberSearchDtv, mNumberSearchAtv);
            mOptionUiManagerT.setNumberSearchNeed(true);
        }
        mOptionUiManagerT.setAtsccSearchSys(getAtsccListMode());
        setFrequency();
        if (!isNumberSearchMode && mChannelHolder.getVisibility() == View.VISIBLE) {
            mChannelHolder.setVisibility(View.GONE);
        }
   }

    private void setFrequency() {
        String atvdefaultbegain = "42250";
        String atvdefaultend = "868250";
        String dtvdefaultstart = "0";
        String from = null;
        String to = null;
        if (!isNumberSearchMode && mFrequecyFrom.getText() != null && mFrequecyFrom.getText().length() > 0) {
            from = mFrequecyFrom.getText().toString();
        } else if (isNumberSearchMode) {
            from = String.valueOf(mNumber);
        } else {
            from = null;
        }
        if (!isNumberSearchMode && mFrequecyTo.getText() != null && mFrequecyTo.getText().length() > 0) {
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

    private boolean ShowNoAtvFrequencyOrChannel() {
        boolean status = false;
        if ((mFrequecyFrom.getText() != null && mFrequecyFrom.getText().length() > 0)
            && (mManualDTV.isChecked() != false || mManualATV.isChecked() != false)) {
            status = true;
        } else if ((mFrequecyFrom.getText() == null || mFrequecyFrom.getText().length() <= 0)
            && (mManualDTV.isChecked() != false || mManualATV.isChecked() != false)) {
            status = false;
            ShowToastTint(getString(R.string.set_frequency));
        } else if ((mFrequecyFrom.getText() != null && mFrequecyFrom.getText().length() > 0)
            && (mManualDTV.isChecked() == false && mManualATV.isChecked() == false)) {
            status = false;
            ShowToastTint(getString(R.string.set_channel));
        } else if ((mFrequecyFrom.getText() == null || mFrequecyFrom.getText().length() <= 0)
            && (mManualDTV.isChecked() == false && mManualATV.isChecked() == false)) {
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
        //At the moment, if dtv_type_switched is 1 in Settings, make it 0,
        //otherwise, when searching channel finished, retreat from LiveTv and lunch LiveTv again, or click Menu-->Channel,
        //it will execute resumeTvIfNeeded and send broadcast to switch channel automatically, so it should be avoid.
        resetDTVTypeSwitched();

        switch (v.getId()) {
            case R.id.tune_manual:
                if (ShowNoAtvFrequencyOrChannel()) {
                    sendMessage(MANUAL_START, 0, null);
                }
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
            case R.id.manual_tune_atv:
            case R.id.auto_tune_atv:
                if (mManualATV.isChecked() || mAutoATV.isChecked()) {
                    mAtscSearchTypeOption.setEnabled(true);
                } else {
                    mAtscSearchTypeOption.setEnabled(false);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "==== focus =" + getCurrentFocus() + ", keycode =" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mOptionUiManagerT.isSearching()) {
                    //mOptionUiManagerT.DtvStopScan();
                    handler.post(StopScanRunnable);//prevent anr
                } else {
                    finish();
                }
                return true;
            /*case KeyEvent.KEYCODE_MENU:
                if (mOptionUiManagerT.isSearching()) {
                    //mOptionUiManagerT.DtvStopScan();
                    handler.post(StopScanRunnable);//prevent anr
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

    //prevent anr when stop scan
    Runnable StopScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (mOptionUiManagerT != null && mOptionUiManagerT.isSearching()) {
                mOptionUiManagerT.DtvStopScan();
            }
        }
    };

    public void resetDTVTypeSwitched() {
        int isDtvTypeSwitched = Settings.System.getInt(this.getContentResolver(), DroidLogicTvUtils.DTV_TYPE_SWITCHED, 0);
        if (isDtvTypeSwitched == 1) {
            Settings.System.putInt(this.getContentResolver(), DroidLogicTvUtils.DTV_TYPE_SWITCHED, 0);
        }
    }

    public OptionUiManagerT getOptionUiManager () {
        return mOptionUiManagerT;
    }

    @Override
    public void finish() {
        //At the moment, if dtv_type_switched is 1 in Settings, make it 0,
        //otherwise, when switch DTV Type in ChannelSearchActivity, but don't search channel and push the EXIT key to return to LiveTv,
        //it will do resumeTvIfNeeded and the current channel will be switched to next one, so this can't happen.
        resetDTVTypeSwitched();

        isFinished = true;

        if (!mPowerManager.isScreenOn()) {
            Log.d(TAG, "TV is going to sleep, stop tv");
            return;
        }
        if (isNumberSearchMode && hasFoundChannel) {
            setResult(RESULT_OK);
        } else {
            setResult(mOptionUiManagerT.getActivityResult());
        }
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
            if ((!isNumberSearchMode && (!mOptionUiManagerT.isSearching() && !imm. isAcceptingText())) || (isNumberSearchMode && hasFoundChannel)) {
                finish();
            } else  {
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
        mHandler.removeCallbacksAndMessages(null);
        mOptionUiManagerT.release();
        exitNumberSearch();
        mOptionUiManagerT = null;
    }

    final int STANDARD = 0;
    final int LRC = 1;
    final int HRC = 2;
    final int AUTO = 3;

    final int TV_SEARCH_SYS_AUTO = 0;
    final int TV_SEARCH_SYS_PAL = 1;
    final int TV_SEARCH_SYS_NTSC = 2;
    final int TV_SEARCH_SYS_SECAM = 3;


    private void setAtsccListMode(int mode) {
        Log.d(TAG, "setAtsccListMode = " + mode);
        Settings.System.putInt(ChannelSearchActivity.this.getContentResolver(), "atsc_c_list_mode", mode);
    }

    private int getAtsccListMode() {
        Log.d(TAG, "getAtsccListMode = " + Settings.System.getInt(ChannelSearchActivity.this.getContentResolver(), "atsc_c_list_mode", STANDARD));
        return Settings.System.getInt(ChannelSearchActivity.this.getContentResolver(), "atsc_c_list_mode", STANDARD);
    }

    private void setTvSearchTypeSys(int mode) {
        Log.d(TAG, "setTvSearchTypeSys = " + mode);
        Settings.System.putInt(ChannelSearchActivity.this.getContentResolver(), ATSC_TV_SEARCH_SYS, mode);
    }

    private int getTvSearchTypeSys() {
        Log.d(TAG, "getTvSearchTypeSys = " + Settings.System.getInt(ChannelSearchActivity.this.getContentResolver(), ATSC_TV_SEARCH_SYS, TV_SEARCH_SYS_AUTO));
        return Settings.System.getInt(ChannelSearchActivity.this.getContentResolver(), ATSC_TV_SEARCH_SYS, -1);
    }

     //30s timeout, stop scan
    Runnable TimeOutStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (mOptionUiManagerT != null && mOptionUiManagerT.isSearching()) {
                mOptionUiManagerT.DtvStopScan();
            }
            exitNumberSearch();
            finish();
        }
    };
}
