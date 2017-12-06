package com.droidlogic.tvinput.settings;

import android.R.integer;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.content.Intent;
import android.media.tv.TvContract.Channels;
import android.os.PowerManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;
import android.os.Handler;
import android.os.Message;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvControlManager.FreqList;
import android.app.AlertDialog;
import com.droidlogic.app.SystemControlManager;
import android.media.tv.TvContract;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TVChannelParams;
import com.droidlogic.app.tv.TVMultilingualText;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.tvinput.R;
import com.droidlogic.tvinput.Utils;
import com.droidlogic.tvinput.settings.ContentListView;
import android.media.tv.TvInputInfo;

public class OptionUiManagerT implements  OnFocusChangeListener, TvControlManager.ScannerEventListener {
    public static final String TAG = "OptionUiManagerT";

    public static final int OPTION_PICTURE_MODE = 100;
    public static final int OPTION_BRIGHTNESS = 101;
    public static final int OPTION_CONTRAST = 102;
    public static final int OPTION_COLOR = 103;
    public static final int OPTION_SHARPNESS = 104;
    public static final int OPTION_BACKLIGHT = 105;
    public static final int OPTION_TINT = 106;
    public static final int OPTION_COLOR_TEMPERATURE = 107;
    public static final int OPTION_ASPECT_RATIO = 108;
    public static final int OPTION_DNR = 109;
    public static final int OPTION_3D_SETTINGS = 110;

    public static final int OPTION_SOUND_MODE = 200;
    public static final int OPTION_TREBLE = 201;
    public static final int OPTION_BASS = 202;
    public static final int OPTION_BALANCE = 203;
    public static final int OPTION_SPDIF = 204;
    public static final int OPTION_DIALOG_CLARITY = 205;
    public static final int OPTION_BASS_BOOST = 206;
    public static final int OPTION_SURROUND = 207;
    public static final int OPTION_VIRTUAL_SURROUND = 208;

    public static final int OPTION_AUDIO_TRACK = 300;
    public static final int OPTION_SOUND_CHANNEL = 301;
    public static final int OPTION_CHANNEL_INFO = 302;
    public static final int OPTION_COLOR_SYSTEM = 303;
    public static final int OPTION_SOUND_SYSTEM = 304;
    public static final int OPTION_VOLUME_COMPENSATE = 305;
    public static final int OPTION_FINE_TUNE = 306;
    public static final int OPTION_MANUAL_SEARCH = 307;
    public static final int OPTION_AUTO_SEARCH = 38;
    public static final int OPTION_CHANNEL_EDIT = 39;
    public static final int OPTION_SWITCH_CHANNEL = 310;
    public static final int OPTION_MTS = 311;

    public static final int OPTION_DTV_TYPE = 400;
    public static final int OPTION_SLEEP_TIMER = 401;
    public static final int OPTION_MENU_TIME = 402;
    public static final int OPTION_STARTUP_SETTING = 403;
    public static final int OPTION_DYNAMIC_BACKLIGHT = 404;
    public static final int OPTION_RESTORE_FACTORY = 405;
    public static final int OPTION_DEFAULT_LANGUAGE = 406;
    public static final int OPTION_SUBTITLE_SWITCH = 407;
    public static final int OPTION_HDMI20 = 408;
    public static final int OPTION_FBC_UPGRADE = 409;
    public static final int OPTION_AD_SWITCH = 410;
    public static final int OPTION_AD_MIX = 411;
    public static final int OPTION_AD_LIST = 412;

    public static final int ALPHA_NO_FOCUS = 230;
    public static final int ALPHA_FOCUSED = 255;

    public static final int ATV_MIN_KHZ = 42250;
    public static final int ATV_MAX_KHZ = 868250;

    private static final int PADDING_LEFT = 50;

    private Context mContext;
    private Resources mResources;
    private SettingsManager mSettingsManager;
    private TvControlManager mTvControlManager;
    private TvDataBaseManager mTvDataBaseManager;
    private int optionTag = OPTION_PICTURE_MODE;
    private String optionKey = null;
    private int channelNumber = 0;//for setting show searched tv channelNumber
    private int radioNumber = 0;//for setting show searched radio channelNumber
    private int tvDisplayNumber = 0;//for db store TV channel's channel displayNumber
    private int radioDisplayNumber = 0;//for db store Radio channel's channel displayNumber
    List<ChannelInfo> mChannels = new ArrayList<ChannelInfo>();

    public static final int SEARCH_STOPPED = 0;
    public static final int SEARCH_RUNNING = 1;
    public static final int SEARCH_PAUSED = 2;
    private int isSearching = SEARCH_STOPPED;

    public static final int AUTO_SEARCH = 0;
    public static final int MANUAL_SEARCH = 1;
    private int searchType = AUTO_SEARCH;

    private Toast toast = null;

    private Handler mHandler;
    private boolean isLiveTvScaning = false;
    private String mLiveTvFrequencyFrom = "0";
    private String mLiveTvFrequencyTo = "868250";
    private boolean mSearchDtv = false;
    private boolean mSearchAtv = false;
    private int mAtsccMode = 0;
    private boolean mLiveTvManualSearch = false;
    private boolean mLiveTvAutoSearch = false;

    public boolean isSearching() {
        return (isSearching != SEARCH_STOPPED);
    }
    public boolean isManualSearching() {
        return (isSearching() && (searchType == MANUAL_SEARCH));
    }

    /*public OptionUiManagerT(Context context) {
        mContext = context;
        mResources = mContext.getResources();
        init(((TvSettingsActivity)mContext).getSettingsManager());
    }*/

   public OptionUiManagerT(Context context, Intent intent, boolean tvscan) {
        mContext = context;
        mResources = mContext.getResources();
        isLiveTvScaning = tvscan;
	 mSettingsManager = new SettingsManager(mContext, intent);
        init();
    }

    public void init () {
        //mSettingsManager = sm;
        mTvControlManager = mSettingsManager.getTvControlManager();
        mTvDataBaseManager = mSettingsManager.getTvDataBaseManager();
        mTvControlManager.setScannerListener(this);
    }

    public int getOptionTag() {
        return optionTag;
    }

    public void setOptionListener(View view) {
        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);
            if (child != null && child.hasFocusable() && child instanceof TextView) {
                //child.setOnClickListener(this);
                child.setOnFocusChangeListener(this);
                //child.setOnKeyListener(this);
            }
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v instanceof TextView) {
            if (hasFocus) {
                ((TextView) v).setTextColor(mResources.getColor(R.color.color_text_focused));
            } else
                ((TextView) v).setTextColor(mResources.getColor(R.color.color_text_item));
        }
    }

    private void stopSearch() {
        Log.d(TAG, "stopSearch");
        doScanCmd(DroidLogicTvUtils.ACTION_STOP_SCAN, null);
    }

    private void pauseSearch() {
        Log.d(TAG, "pauseSearch");
        doScanCmd(DroidLogicTvUtils.ACTION_ATV_PAUSE_SCAN, null);
        isSearching = SEARCH_PAUSED;
    }

    private void resumeSearch() {
        Log.d(TAG, "resumeSearch");
        doScanCmd(DroidLogicTvUtils.ACTION_ATV_RESUME_SCAN, null);
        isSearching = SEARCH_RUNNING;
    }

    private static final int LRC = 2;
    private static final int HRC = 3;
    private static final int ATSC_C_LRC_SET = 1;
    private static final int ATSC_C_HRC_SET = 2;
    private static final int DTV_TO_ATV = 4;
    private static final int SET_TO_MODE = 1;

    private void startManualSearchAccordingMode() {
        Log.d(TAG, "startManualSearchAccordingMode");
        mTvControlManager.SetAudioMuteForTv(TvControlManager.AUDIO_MUTE_FOR_TV);
        String channel;
        if (!isLiveTvScaning) {
            ViewGroup parent = (ViewGroup) ((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);
            OptionEditText edit = (OptionEditText) parent.findViewById(R.id.manual_search_dtv_channel_manual);
            channel = edit.getText().toString();
            if (channel == null || channel.length() == 0) {
                channel = (String)edit.getHint();
            }
        } else {
            channel = mLiveTvFrequencyFrom;
        }

        Bundle bundle = new Bundle();
        TvControlManager.TvMode mode = new TvControlManager.TvMode(mSettingsManager.getDtvType());
        int frequency = getDvbFrequencyByPd(Integer.valueOf(channel));
        Log.d(TAG, "frequency :" + frequency);
        bundle.putInt(DroidLogicTvUtils.PARA_MANUAL_SCAN, frequency);
        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_AUTO);
        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_AUTO);
        int autoscanmode;
        if (!isLiveTvScaning) {
            autoscanmode = ((TvSettingsActivity)mContext).mManualScanEdit.checkAutoScanMode();
        } else {
            autoscanmode = checkLiveTvAutoScanMode();
        }
        switch (autoscanmode) {
        case ManualScanEdit.SCAN_ATV_DTV:
            Log.d(TAG, "MANUAL_SCAN_ATV_DTV");
            if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C)) {
                //mode.setList(1);
                if (mAtsccMode == ATSC_C_LRC_SET) {
                    mode.setList(LRC);
                } else if (mAtsccMode == ATSC_C_HRC_SET) {
                    mode.setList(HRC);
                }
            }
            mode.setExt(mode.getExt() | 1);//mixed adtv
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_MANUAL);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_FREQ);
        break;
        case ManualScanEdit.SCAN_ONLY_ATV:
            Log.d(TAG, "MANUAL_SCAN_ONLY_ATV");
            if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_T)) {
                //mode.setList(4);
            } else if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C)) {
                //mode.setList(5);
                if (mAtsccMode == ATSC_C_LRC_SET) {
                    mode.setList(LRC);
                } else if (mAtsccMode == ATSC_C_HRC_SET) {
                    mode.setList(HRC);
                }
            }
            mode.setExt(mode.getExt() | 1);//mixed adtv
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_NONE);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_FREQ);
        break;
        case ManualScanEdit.SCAN_ONLY_DTV:
            Log.d(TAG, "MANUAL_SCAN_ONLY_DTV");
            if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C)) {
                //mode.setList(1);
                if (mAtsccMode == ATSC_C_LRC_SET) {
                    mode.setList(LRC);
                } else if (mAtsccMode == ATSC_C_HRC_SET) {
                    mode.setList(HRC);
                }
            }
            mode.setExt(mode.getExt() | 1);//mixed adtv
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_MANUAL);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_NONE);
        break;
        }
        bundle.putString(TvInputInfo.EXTRA_INPUT_ID,mSettingsManager.getInputId());
        mSettingsManager.sendBroadcastToTvapp(DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN, bundle);
        doScanCmdForAtscManual(bundle);
        isSearching = SEARCH_RUNNING;
        mSettingsManager.setActivityResult(DroidLogicTvUtils.RESULT_UPDATE);
    }

    private void doScanCmdForAtscManual (Bundle bundle) {
        mTvControlManager.DtvSetTextCoding("GB2312");
        int dtvMode = (bundle == null ? TVChannelParams.MODE_DTMB
                : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_MODE, TVChannelParams.MODE_DTMB));
        TvControlManager.TvMode tvMode = TvControlManager.TvMode.fromMode(dtvMode);
        if ((tvMode.getExt() & 1) != 0) {
            int atvScanType = (bundle == null ? TvControlManager.ScanType.SCAN_ATV_NONE
                    : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_NONE));
            int dtvScanType = (bundle == null ? TvControlManager.ScanType.SCAN_DTV_NONE
                    : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_NONE));
            int atvFreq1 = (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA1, 0));
            int atvFreq2 = (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA2, 0));
            int dtvFreq = (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_MANUAL_SCAN, 0));
            int atvVideoStd = (bundle == null ? TvControlManager.ATV_VIDEO_STD_PAL
                    : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_PAL));
            int atvAudioStd = (bundle == null ? TvControlManager.ATV_AUDIO_STD_DK
                    : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_DK));
            TvControlManager.FEParas fe = new TvControlManager.FEParas();
            fe.setMode(tvMode);
            fe.setVideoStd(atvVideoStd);
            fe.setAudioStd(atvAudioStd);
            TvControlManager.ScanParas scan = new TvControlManager.ScanParas();
            if (atvScanType != TvControlManager.ScanType.SCAN_ATV_NONE) {
                atvFreq1 = dtvFreq;// - 5750000;
                atvFreq2 = dtvFreq;// + 5250000;
                scan.setAtvFrequency1(atvFreq1);
                scan.setAtvFrequency2(atvFreq2);
            }
            scan.setMode(TvControlManager.ScanParas.MODE_DTV_ATV);
            scan.setAtvMode(atvScanType);
            scan.setDtvMode(dtvScanType);
            scan.setDtvFrequency1(dtvFreq);
            scan.setDtvFrequency2(dtvFreq);
            if (checkLiveTvAutoScanMode() == ScanEdit.SCAN_ATV_DTV || checkLiveTvAutoScanMode() == ScanEdit.SCAN_ONLY_ATV) {
                scan.setAtvModifier(TvControlManager.FEParas.K_MODE, tvMode.setList(tvMode.getList() + DTV_TO_ATV).getMode());
            }
            mTvControlManager.OpenDevForScan(DroidLogicTvUtils.OPEN_DEV_FOR_SCAN_DTV);
            mTvControlManager.TvScan(fe, scan);
            }
    }

    private void startManualSearch() {
        Log.d(TAG, "startManualSearch");
        ViewGroup parent = null;;
        if (!isLiveTvScaning) {
            parent = (ViewGroup) ((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);
        }
        mTvControlManager.SetAudioMuteForTv(TvControlManager.AUDIO_MUTE_FOR_TV);
        if (mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
            Log.d(TAG, "ADTV");
            String channel;
            if (!isLiveTvScaning) {
                OptionEditText edit = (OptionEditText) parent.findViewById(R.id.manual_search_dtv_channel);
                channel = edit.getText().toString();
                if (channel == null || channel.length() == 0)
                    channel = (String)edit.getHint();
            } else {
                channel = mLiveTvFrequencyFrom;
            }

            Bundle bundle = new Bundle();
            TvControlManager.TvMode mode = new TvControlManager.TvMode(mSettingsManager.getDtvType());
            int frequency = getDvbFrequencyByPd(Integer.valueOf(channel));
            mode.setExt(mode.getExt() | 1);//mixed adtv
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
            bundle.putInt(DroidLogicTvUtils.PARA_MANUAL_SCAN, frequency);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_MANUAL);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_MANUAL);
            //bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA1, 0);
            //bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA2, 0);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_AUTO);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_AUTO);
            bundle.putString(TvInputInfo.EXTRA_INPUT_ID,mSettingsManager.getInputId());
            mSettingsManager.sendBroadcastToTvapp(DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN, bundle);
            doScanCmd(DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN, bundle);
            isSearching = SEARCH_RUNNING;
            mSettingsManager.setActivityResult(DroidLogicTvUtils.RESULT_UPDATE);
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
            String str_begin;
            String str_end;
            if (isLiveTvScaning) {
                OptionEditText edit_from = (OptionEditText) parent.findViewById(R.id.manual_search_edit_from);
                OptionEditText edit_to = (OptionEditText) parent.findViewById(R.id.manual_search_edit_to);
                str_begin = edit_from.getText().toString();
                if (str_begin == null || str_begin.length() == 0)
                    str_begin = (String) edit_from.getHint();
                str_end = edit_to.getText().toString();
                if (str_end == null || str_end.length() == 0)
                    str_end = (String) edit_to.getHint();
            } else {
                str_begin = mLiveTvFrequencyFrom;
                str_end = mLiveTvFrequencyTo;
            }

            int from = Integer.valueOf(str_begin);
            int to =  Integer.valueOf(str_end);

            if (from < ATV_MIN_KHZ || from > ATV_MAX_KHZ || to < ATV_MIN_KHZ || to > ATV_MAX_KHZ)
                showToast(mResources.getString(R.string.error_atv_error_1));
            //else if (from > to)
            //    showToast(mResources.getString(R.string.error_atv_error_2));
            else if (Math.abs(to - from) < 1000)
                showToast(mResources.getString(R.string.error_atv_error_3));
            else {
                //mSettingsManager.sendBroadcastToTvapp("search_channel");
                mSettingsManager.setManualSearchProgress(0);
                mSettingsManager.setManualSearchSearchedNumber(0);
                //int ret = mTvControlManager.AtvManualScan(from * 1000, to * 1000,
                //                                TvControlManager.ATV_VIDEO_STD_PAL, TvControlManager.ATV_AUDIO_STD_DK);
                //Log.d(TAG, "mTvControlManager.AtvManualScan return " + ret);
                //if (ret < 0) {
                //    showToast(mResources.getString(R.string.error_atv_startSearch));
                //    return;
                //}
                Bundle bundle = new Bundle();
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA1, from * 1000);
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA2, to * 1000);
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_PAL);
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_DK);
                bundle.putString(TvInputInfo.EXTRA_INPUT_ID,mSettingsManager.getInputId());
                mSettingsManager.sendBroadcastToTvapp(DroidLogicTvUtils.ACTION_ATV_MANUAL_SCAN, bundle);
                doScanCmd(DroidLogicTvUtils.ACTION_ATV_MANUAL_SCAN, bundle); //ww
                isSearching = SEARCH_RUNNING;
                mSettingsManager.setActivityResult(DroidLogicTvUtils.RESULT_UPDATE);
            }
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            //mSettingsManager.sendBroadcastToTvapp("search_channel");
            String channel;
            if (!isLiveTvScaning) {
                OptionEditText edit = (OptionEditText) parent.findViewById(R.id.manual_search_dtv_channel);
                channel = edit.getText().toString();
                if (channel == null || channel.length() == 0)
                    channel = (String)edit.getHint();
            } else {
                channel = mLiveTvFrequencyFrom;
            }

            //mTvControlManager.DtvSetTextCoding("GB2312");
            //mTvControlManager.DtvManualScan(new TvControlManager.TvMode(mSettingsManager.getDtvType()).getMode(),
            //    getDvbFrequencyByPd(Integer.valueOf(channel)));
            //Intent intent = new Intent(DroidLogicTvUtils.ACTION_SUBTITLE_SWITCH);
            //intent.putExtra(DroidLogicTvUtils.EXTRA_SWITCH_VALUE, 0);
            //mContext.sendBroadcast(intent);
            Bundle bundle = new Bundle();
            TvControlManager.TvMode mode = new TvControlManager.TvMode(mSettingsManager.getDtvType());
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
            bundle.putInt(DroidLogicTvUtils.PARA_MANUAL_SCAN, getDvbFrequencyByPd(Integer.valueOf(channel)));
            bundle.putString(TvInputInfo.EXTRA_INPUT_ID,mSettingsManager.getInputId());
            mSettingsManager.sendBroadcastToTvapp(DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN, bundle);
            doScanCmd(DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN, bundle);
            isSearching = SEARCH_RUNNING;
            mSettingsManager.setActivityResult(DroidLogicTvUtils.RESULT_UPDATE);
        }
        searchType = MANUAL_SEARCH;
    }

    public void setManualSearchEditStyle(View view) {
        if (mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
            final OptionEditText edit = (OptionEditText) view.findViewById(R.id.manual_search_dtv_channel_manual);
            edit.setNextFocusLeftId(R.id.content_list);
            edit.setNextFocusRightId(edit.getId());
            edit.setNextFocusUpId(edit.getId());
            final TextView start_frequency = (TextView)view.findViewById(R.id.manual_search_dtv_start_frequency_manual);

            TextWatcher textWatcher = new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int pos = s.length() - 1;
                        char c = s.charAt(pos);
                        if (c < '0' || c > '9') {
                            s.delete(pos, pos + 1);
                            showToast(mResources.getString(R.string.error_not_number));
                        } else {
                            String number = edit.getText().toString();
                            if (number == null || number.length() == 0)
                                number = (String)edit.getHint();

                            if (number.matches("[0-9]+"))
                                start_frequency.setText(parseChannelFrequency(getDvbFrequencyByPd(Integer.valueOf(number))));
                        }
                    }
                }
            };
            edit.addTextChangedListener(textWatcher);
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            final OptionEditText edit = (OptionEditText) view.findViewById(R.id.manual_search_dtv_channel);
            edit.setNextFocusLeftId(R.id.content_list);
            edit.setNextFocusRightId(edit.getId());
            edit.setNextFocusUpId(edit.getId());
            final TextView start_frequency = (TextView)view.findViewById(R.id.manual_search_dtv_start_frequency);

            TextWatcher textWatcher = new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int pos = s.length() - 1;
                        char c = s.charAt(pos);
                        if (c < '0' || c > '9') {
                            s.delete(pos, pos + 1);
                            showToast(mResources.getString(R.string.error_not_number));
                        } else {
                            String number = edit.getText().toString();
                            if (number == null || number.length() == 0)
                                number = (String)edit.getHint();

                            if (number.matches("[0-9]+"))
                                start_frequency.setText(parseChannelFrequency(getDvbFrequencyByPd(Integer.valueOf(number))));
                        }
                    }
                }
            };
            edit.addTextChangedListener(textWatcher);
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
            OptionEditText edit_from = (OptionEditText) view.findViewById(R.id.manual_search_edit_from);
            OptionEditText edit_to = (OptionEditText) view.findViewById(R.id.manual_search_edit_to);
            edit_from.setNextFocusLeftId(R.id.content_list);
            edit_from.setNextFocusRightId(edit_from.getId());
            edit_from.setNextFocusUpId(edit_from.getId());
            edit_to.setNextFocusLeftId(R.id.content_list);
            edit_to.setNextFocusRightId(edit_to.getId());

            TextWatcher textWatcher = new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int pos = s.length() - 1;
                        char c = s.charAt(pos);
                        if (c < '0' || c > '9') {
                            s.delete(pos, pos + 1);
                            showToast(mResources.getString(R.string.error_not_number));
                        }
                    }
                }
            };
            edit_from.addTextChangedListener(textWatcher);
            edit_to.addTextChangedListener(textWatcher);
        }
    }

    private String parseChannelFrequency(double freq) {
        String frequency = mResources.getString(R.string.start_frequency);
        frequency += Double.toString(freq / (1000 * 1000)) + mResources.getString(R.string.mhz);
        return frequency;
    }

    private int getList() {
        if (mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
            if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C)
                || mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_T)) {
                boolean isCable = mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C);
                int cableMode;
                int autoscanmode;
                if (!isLiveTvScaning) {
                    cableMode = ((TvSettingsActivity)mContext).mManualScanEdit.checkCableMode();
                    autoscanmode = ((TvSettingsActivity)mContext).mManualScanEdit.checkAutoScanMode();
                } else {
                    cableMode = mAtsccMode + SET_TO_MODE;//select atsc-c std lrc drc
                    autoscanmode = checkLiveTvAutoScanMode();
                }

                switch (autoscanmode) {
                    case ScanEdit.SCAN_ATV_DTV:
                    case ScanEdit.SCAN_ONLY_DTV:
                    if (isCable) {
                        switch (cableMode) {
                            case ScanEdit.CABLE_MODE_STANDARD: return 1;
                            case ScanEdit.CABLE_MODE_LRC:      return 2;
                            case ScanEdit.CABLE_MODE_HRC:      return 3;
                        }
                        return 1;
                    }
                    return 0;
                    case ScanEdit.SCAN_ONLY_ATV:
                    if (isCable) {
                         switch (cableMode) {
                            case ScanEdit.CABLE_MODE_STANDARD: return 5;
                            case ScanEdit.CABLE_MODE_LRC:      return 6;
                            case ScanEdit.CABLE_MODE_HRC:      return 7;
                        }
                        return 5;
                    }
                    return 4;
                }
            }
        }
        return 0;
    }

    private int getDvbFrequencyByPd(int pd_number) {
        TvControlManager.TvMode mode = new TvControlManager.TvMode(mSettingsManager.getDtvType());
        mode.setList(getList());
        Log.d(TAG, "[get freq]type:"+mode.toType()+" use list:"+mode.getList());
        return getDvbFrequencyByPd(mode.getMode(), pd_number);
    }

    private int getDvbFrequencyByPd(int tvMode, int pdNumber) {
        ArrayList<FreqList> m_fList = mTvControlManager.DTVGetScanFreqList(tvMode);
        String type = TvControlManager.TvMode.fromMode(tvMode).toType();
        int size = m_fList.size();
        int the_freq = -1;
/*
        if (type.equals(TvContract.Channels.TYPE_ATSC_T)
            || type.equals(TvContract.Channels.TYPE_ATSC_C))
            pdNumber = pdNumber - 1;
*/
        if (pdNumber < 1)
            pdNumber = 1;

        for (int i = 0; i < size; i++) {
            if (pdNumber == m_fList.get(i).channelNum) {
                the_freq = m_fList.get(i).freq;
                break;
            }
        }
        Log.d(TAG, "pdNumber: " + pdNumber + ", the_freq: " + the_freq);
        return (the_freq < 0)? m_fList.get(0).freq : the_freq;
    }

    private void setManualSearchInfo(TvControlManager.ScannerEvent event) {
        if (isLiveTvScaning) return;
        Log.d(TAG, "mSettingsManager.getCurentVirtualTvSource()="+mSettingsManager.getCurentVirtualTvSource());
        ViewGroup optionView = (ViewGroup)((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);
        if (event == null)
            return;
        if (mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
            OptionListView listView = (OptionListView)optionView.findViewById(R.id.manual_search_dtv_info_manual);
            ArrayList<HashMap<String, Object>> dataList = getSearchedDtvInfo(event);
            SimpleAdapter adapter = new SimpleAdapter(mContext, dataList,
                    R.layout.layout_option_double_text,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
            listView.setAdapter(adapter);
        }else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            OptionListView listView = (OptionListView)optionView.findViewById(R.id.manual_search_dtv_info);
            ArrayList<HashMap<String, Object>> dataList = getSearchedDtvInfo(event);
            SimpleAdapter adapter = new SimpleAdapter(mContext, dataList,
                    R.layout.layout_option_double_text,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
            listView.setAdapter(adapter);
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
            OptionEditText begin = (OptionEditText)optionView.findViewById(R.id.manual_search_edit_from);
            TextView frequency = (TextView)optionView.findViewById(R.id.manual_search_frequency);
            TextView frequency_band = (TextView)optionView.findViewById(R.id.manual_search_frequency_band);
            TextView searched_number = (TextView)optionView.findViewById(R.id.manual_search_searched_number);
            double freq = 0.0;

            if (event == null) {
                freq = Double.valueOf((String)begin.getHint()).doubleValue();
                freq /= 1000;// KHZ to MHZ
            } else {
                freq = event.freq;
                freq /= 1000 * 1000;// HZ to MHZ
            }

            if (frequency != null && frequency_band != null && searched_number != null) {
                frequency.setText(Double.toString(freq) + mResources.getString(R.string.mhz));
                frequency_band.setText(parseFrequencyBand(freq));
                searched_number.setText(mResources.getString(R.string.searched_number) + ": " + channelNumber);
            }
        }
    }

    public ArrayList<HashMap<String, Object>> getSearchedDtvInfo (TvControlManager.ScannerEvent event) {
        ArrayList<HashMap<String, Object>> list =  new ArrayList<HashMap<String, Object>>();

        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(SettingsManager.STRING_NAME, mResources.getString(R.string.frequency_l) + ":");
        item.put(SettingsManager.STRING_STATUS, Double.toString(event.freq / (1000 * 1000)) +
                 mResources.getString(R.string.mhz));
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(SettingsManager.STRING_NAME, mResources.getString(R.string.quality) + ":");
        item.put(SettingsManager.STRING_STATUS, event.quality + mResources.getString(R.string.db));
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(SettingsManager.STRING_NAME, mResources.getString(R.string.strength) + ":");
        item.put(SettingsManager.STRING_STATUS, event.strength + "%");
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(SettingsManager.STRING_NAME, mResources.getString(R.string.tv_channel) + ":");
        item.put(SettingsManager.STRING_STATUS, channelNumber);
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(SettingsManager.STRING_NAME, mResources.getString(R.string.radio_channel) + ":");
        item.put(SettingsManager.STRING_STATUS, radioNumber);
        list.add(item);

        return list;
    }

    //add by ww
    private void startAutosearch() {
        Log.d(TAG, "startAutoSearch");
        TvControlManager.TvMode mode = new TvControlManager.TvMode(mSettingsManager.getDtvType());
        mTvControlManager.SetAudioMuteForTv(TvControlManager.AUDIO_MUTE_FOR_TV);
        //mSettingsManager.sendBroadcastToTvapp("search_channel");
        if (mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
            Log.d(TAG, "ADTV");

            deleteChannels(mode, true, true);

            Bundle bundle = new Bundle();
            int[] freqPair = new int[2];
            mTvControlManager.ATVGetMinMaxFreq(freqPair);
            mode.setExt(mode.getExt() | 1);//mixed adtv
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_ALLBAND);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_AUTO);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA1, freqPair[0]);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA2, freqPair[1]);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_AUTO);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_AUTO);
            bundle.putString(TvInputInfo.EXTRA_INPUT_ID,mSettingsManager.getInputId());
            mSettingsManager.sendBroadcastToTvapp(DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN, bundle);
            doScanCmd(DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN, bundle);  //ww
            Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_DTV_CHANNEL_INDEX, -1);
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
            Log.d(TAG, "SOURCE_TYPE_TV");

            deleteChannels(mode, true, false);

            Bundle bundle = new Bundle();
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA1, 0);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA2, 0);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_PAL);
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_I);
            bundle.putString(TvInputInfo.EXTRA_INPUT_ID,mSettingsManager.getInputId());
            mSettingsManager.sendBroadcastToTvapp(DroidLogicTvUtils.ACTION_ATV_AUTO_SCAN, bundle);
            doScanCmd(DroidLogicTvUtils.ACTION_ATV_AUTO_SCAN, bundle);
            Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_ATV_CHANNEL_INDEX, -1);
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            Log.d(TAG, "SOURCE_TYPE_DTV");

            deleteChannels(mode, true, false);

            Bundle bundle = new Bundle();
            bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
            bundle.putString(TvInputInfo.EXTRA_INPUT_ID,mSettingsManager.getInputId());
            mSettingsManager.sendBroadcastToTvapp(DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN, bundle);
            doScanCmd(DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN, bundle);
            Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_DTV_CHANNEL_INDEX, -1);
        }
        isSearching = SEARCH_RUNNING;
        searchType = AUTO_SEARCH;
        mSettingsManager.setActivityResult(DroidLogicTvUtils.RESULT_UPDATE);
    }

    private void deleteChannels(TvControlManager.TvMode mode, boolean deleteAtv, boolean deleteDtv)
    {
        Log.d(TAG, " delete mode:"+mode.getBase()+" atv:"+deleteAtv+" dtv:"+deleteDtv);
        if (deleteAtv) {
            if (mode.getBase() == TVChannelParams.MODE_ATSC)
                mTvDataBaseManager.deleteChannels(mSettingsManager.getInputId(), TvContract.Channels.TYPE_NTSC);
            //else
                mTvDataBaseManager.deleteChannels(mSettingsManager.getInputId(), TvContract.Channels.TYPE_PAL);
        }
        if (deleteDtv)
            mTvDataBaseManager.deleteChannels(mSettingsManager.getInputId(), mode.toType());
    }

    private void startAutosearchAccrodingTvMode() {
        Log.d(TAG, "startAutosearchAccrodingTvMode");
        TvControlManager.TvMode mode = new TvControlManager.TvMode(mSettingsManager.getDtvType());
        //mSettingsManager.sendBroadcastToTvapp("search_channel");
        Bundle bundle = new Bundle();

        int[] freqPair = new int[2];
        mTvControlManager.ATVGetMinMaxFreq(freqPair);
        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA1, freqPair[0]);
        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA2, freqPair[1]);

        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_AUTO);
        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_AUTO);
        mTvControlManager.SetAudioMuteForTv(TvControlManager.AUDIO_MUTE_FOR_TV);
        int autoscanmode;
        int checkcablemode;
        if (!isLiveTvScaning) {
            autoscanmode = ((TvSettingsActivity)mContext).mScanEdit.checkAutoScanMode();
        } else {
            autoscanmode = checkLiveTvAutoScanMode();
        }
        Log.d(TAG, "[startAutosearchAccrodingTvMode] autoscanmode = " + autoscanmode);
        switch (autoscanmode) {
            case ScanEdit.SCAN_ATV_DTV:
                Log.d(TAG, "ADTV");

                deleteChannels(mode, true, true);
                if (!isLiveTvScaning) {
                    checkcablemode = ((TvSettingsActivity)mContext).mScanEdit.checkCableMode();
                } else {
                    checkcablemode = mAtsccMode + 1;//mAtsccMode value change from 0~2
                }
                if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C) && mAtsccMode < ATSC_C_HRC_SET + 1) {
                    if (checkcablemode == ScanEdit.CABLE_MODE_STANDARD) {
                        mode.setList(1);
                        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 5);
                    } else if (checkcablemode == ScanEdit.CABLE_MODE_LRC) {
                        mode.setList(2);
                        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 6);
                    } else if (checkcablemode == ScanEdit.CABLE_MODE_HRC) {
                        mode.setList(3);
                        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 7);
                    }// need std only , search auto by sys value
                } else if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C)) {
                    mode.setList(1);
                    bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 5);
                } else if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_T)) {
                    bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 4);
                }
                mode.setExt(mode.getExt() | 1);//mixed adtv
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_ALLBAND);
                if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_DTMB))
                    bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_AUTO);
                else
                    bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_FREQ);
            break;
            case ScanEdit.SCAN_ONLY_ATV:
                Log.d(TAG, "SOURCE_TYPE_TV");

                deleteChannels(mode, true, false);

                if (!isLiveTvScaning) {
                    checkcablemode = ((TvSettingsActivity)mContext).mScanEdit.checkCableMode();
                } else {
                    checkcablemode = mAtsccMode + 1;
                }
                if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C) && mAtsccMode < ATSC_C_HRC_SET + 1) {
                    if (checkcablemode == ScanEdit.CABLE_MODE_STANDARD) {
                        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 5);
                    } else if (checkcablemode == ScanEdit.CABLE_MODE_LRC) {
                        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 6);
                    } else if (checkcablemode == ScanEdit.CABLE_MODE_HRC) {
                        bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 7);
                    }
                } else if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C)) {
                    bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 5);
                } else if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_T)) {
                    bundle.putInt(DroidLogicTvUtils.PARA_SCAN_PARA5, 4);
                }

                mode.setExt(mode.getExt() | 1);//mixed adtv
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_NONE);
                if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_DTMB))
                    bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_AUTO);
                else
                    bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_FREQ);
            break;
            case ScanEdit.SCAN_ONLY_DTV:
                Log.d(TAG, "SOURCE_TYPE_DTV");

                deleteChannels(mode, false, true);

                if (!isLiveTvScaning) {
                    checkcablemode = ((TvSettingsActivity)mContext).mScanEdit.checkCableMode();
                } else {
                    checkcablemode = mAtsccMode + 1;
                }
                if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C) && mAtsccMode < ATSC_C_HRC_SET + 1) {
                    if (checkcablemode == ScanEdit.CABLE_MODE_STANDARD) {
                        mode.setList(1);
                    } else if (checkcablemode == ScanEdit.CABLE_MODE_LRC) {
                        mode.setList(2);
                    } else if (checkcablemode == ScanEdit.CABLE_MODE_HRC) {
                        mode.setList(3);
                    }
                } else if (mSettingsManager.getDtvType().equals(TvContract.Channels.TYPE_ATSC_C)) {
                    mode.setList(1);
                }
                mode.setExt(mode.getExt() | 1);//mixed adtv
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_MODE, mode.getMode());
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_ALLBAND);
                bundle.putInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_NONE);
            break;
        }
        bundle.putString(TvInputInfo.EXTRA_INPUT_ID,mSettingsManager.getInputId());
        mSettingsManager.sendBroadcastToTvapp(DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN, bundle);
        doScanCmd(DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN, bundle);  //ww
        Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_DTV_CHANNEL_INDEX, -1);
        isSearching = SEARCH_RUNNING;
        searchType = AUTO_SEARCH;
        mSettingsManager.setActivityResult(DroidLogicTvUtils.RESULT_UPDATE);
    }

    private void doScanCmd(String action, Bundle bundle) {
        Log.d(TAG, "doScanCmd:"+action);
        if (DroidLogicTvUtils.ACTION_ATV_AUTO_SCAN.equals(action)) {
            //ww//
            mTvControlManager.OpenDevForScan(DroidLogicTvUtils.OPEN_DEV_FOR_SCAN_ATV);
            mTvControlManager.AtvAutoScan(
                (bundle == null ? TvControlManager.ATV_VIDEO_STD_PAL
                    : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA1, TvControlManager.ATV_VIDEO_STD_PAL)),
                (bundle == null ? TvControlManager.ATV_AUDIO_STD_DK
                    : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA2, TvControlManager.ATV_AUDIO_STD_DK)),
                (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA3, 0)),
                (bundle == null ? 1 : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA4, 1)));
        } else if (DroidLogicTvUtils.ACTION_ATV_MANUAL_SCAN.equals(action)) {
            if (bundle != null) {
                //ww//
                mTvControlManager.OpenDevForScan(DroidLogicTvUtils.OPEN_DEV_FOR_SCAN_ATV);
                mTvControlManager.AtvManualScan(
                    bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA1, 0),
                    bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA2, 0),
                    bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_PAL),
                    bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_DK));
            }
        } else if (DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN.equals(action)) {
            mTvControlManager.DtvSetTextCoding("GB2312");
            int dtvMode = (bundle == null ? TVChannelParams.MODE_DTMB
                    : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_MODE, TVChannelParams.MODE_DTMB));
            TvControlManager.TvMode tvMode = TvControlManager.TvMode.fromMode(dtvMode);
            if ((tvMode.getExt() & 1) != 0) {//ADTV
                int atvScanType = (bundle == null ? TvControlManager.ScanType.SCAN_ATV_NONE
                        : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_NONE));
                int dtvScanType = (bundle == null ? TvControlManager.ScanType.SCAN_DTV_NONE
                        : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_NONE));
                int atvFreq1 = (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA1, 0));
                int atvFreq2 = (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA2, 0));
                int dtvFreq = (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_MANUAL_SCAN, 0));
                int atvVideoStd = (bundle == null ? TvControlManager.ATV_VIDEO_STD_PAL
                        : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_PAL));
                int atvAudioStd = (bundle == null ? TvControlManager.ATV_AUDIO_STD_DK
                        : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_DK));
                int atvList = (bundle == null ? tvMode.getList()
                        : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA5, tvMode.getList()));
                TvControlManager.FEParas fe = new TvControlManager.FEParas();
                fe.setMode(tvMode);
                fe.setVideoStd(atvVideoStd);
                fe.setAudioStd(atvAudioStd);
                TvControlManager.ScanParas scan = new TvControlManager.ScanParas();
                scan.setMode(TvControlManager.ScanParas.MODE_DTV_ATV);
                scan.setAtvMode(atvScanType);
                scan.setDtvMode(dtvScanType);
                scan.setAtvFrequency1(atvFreq1);
                scan.setAtvFrequency2(atvFreq2);
                scan.setDtvFrequency1(dtvFreq);
                scan.setDtvFrequency2(dtvFreq);
                scan.setAtvModifier(TvControlManager.FEParas.K_MODE,
                    TvControlManager.TvMode.fromMode(dtvMode).setList(atvList).getMode());
                //ww//
                mTvControlManager.OpenDevForScan(DroidLogicTvUtils.OPEN_DEV_FOR_SCAN_DTV);
                mTvControlManager.TvScan(fe, scan);
            } else {
                //ww//
                mTvControlManager.OpenDevForScan(DroidLogicTvUtils.OPEN_DEV_FOR_SCAN_DTV);
                mTvControlManager.DtvAutoScan(dtvMode);
            }
        } else if (DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN.equals(action)) {
            mTvControlManager.DtvSetTextCoding("GB2312");
            int dtvMode = (bundle == null ? TVChannelParams.MODE_DTMB
                    : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_MODE, TVChannelParams.MODE_DTMB));
            TvControlManager.TvMode tvMode = TvControlManager.TvMode.fromMode(dtvMode);
            if ((tvMode.getExt() & 1) != 0) {//ADTV
                int atvScanType = (bundle == null ? TvControlManager.ScanType.SCAN_ATV_NONE
                        : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_TYPE_ATV, TvControlManager.ScanType.SCAN_ATV_NONE));
                int dtvScanType = (bundle == null ? TvControlManager.ScanType.SCAN_DTV_NONE
                        : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_TYPE_DTV, TvControlManager.ScanType.SCAN_DTV_NONE));
                int atvFreq1 = (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA1, 0));
                int atvFreq2 = (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA2, 0));
                int dtvFreq = (bundle == null ? 0 : bundle.getInt(DroidLogicTvUtils.PARA_MANUAL_SCAN, 0));
                int atvVideoStd = (bundle == null ? TvControlManager.ATV_VIDEO_STD_PAL
                        : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA3, TvControlManager.ATV_VIDEO_STD_PAL));
                int atvAudioStd = (bundle == null ? TvControlManager.ATV_AUDIO_STD_DK
                        : bundle.getInt(DroidLogicTvUtils.PARA_SCAN_PARA4, TvControlManager.ATV_AUDIO_STD_DK));

                if (atvScanType != TvControlManager.ScanType.SCAN_ATV_NONE) {
                    atvFreq1 = dtvFreq - 9750000;
                    atvFreq2 = dtvFreq + 1250000;
                }
                TvControlManager.FEParas fe = new TvControlManager.FEParas();
                fe.setMode(tvMode);
                fe.setVideoStd(atvVideoStd);
                fe.setAudioStd(atvAudioStd);
                TvControlManager.ScanParas scan = new TvControlManager.ScanParas();
                scan.setMode(TvControlManager.ScanParas.MODE_DTV_ATV);
                scan.setAtvMode(atvScanType);
                scan.setDtvMode(dtvScanType);
                scan.setAtvFrequency1(atvFreq1);
                scan.setAtvFrequency2(atvFreq2);
                scan.setDtvFrequency1(dtvFreq);
                scan.setDtvFrequency2(dtvFreq);
                //scan.setDtvStandard(TvControlManager.ScanParas.DTVSTD_ATSC);
                //ww//
                mTvControlManager.OpenDevForScan(DroidLogicTvUtils.OPEN_DEV_FOR_SCAN_DTV);
                mTvControlManager.TvScan(fe, scan);
            } else {
            //ww//
            mTvControlManager.OpenDevForScan(DroidLogicTvUtils.OPEN_DEV_FOR_SCAN_DTV);
                mTvControlManager.DtvManualScan(
                    bundle.getInt(DroidLogicTvUtils.PARA_SCAN_MODE, TVChannelParams.MODE_DTMB),
                    bundle.getInt(DroidLogicTvUtils.PARA_MANUAL_SCAN, 0)
                );
            }
        } else if (DroidLogicTvUtils.ACTION_STOP_SCAN.equals(action)) {
            //ww//
            mTvControlManager.OpenDevForScan(DroidLogicTvUtils.CLOSE_DEV_FOR_SCAN);
            mTvControlManager.DtvStopScan();
        } else if (DroidLogicTvUtils.ACTION_ATV_PAUSE_SCAN.equals(action)) {
            mTvControlManager.AtvDtvPauseScan();
        } else if (DroidLogicTvUtils.ACTION_ATV_RESUME_SCAN.equals(action)) {
            mTvControlManager.AtvDtvResumeScan();
        }
    }

    private void setAutoSearchFrequency(TvControlManager.ScannerEvent event) {
        if (isLiveTvScaning) {
            return;
        }
        ViewGroup optionView = (ViewGroup) ((TvSettingsActivity) mContext).mOptionLayout.getChildAt(0);
        if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV
            || mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
            OptionListView listView = (OptionListView)optionView.findViewById(R.id.auto_search_dtv_info);
            ArrayList<HashMap<String, Object>> dataList = getSearchedDtvInfo(event);
            SimpleAdapter adapter = new SimpleAdapter(mContext, dataList,
                    R.layout.layout_option_double_text,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
            listView.setAdapter(adapter);
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
            TextView frequency = (TextView) optionView.findViewById(R.id.auto_search_frequency_atv);
            TextView frequency_band = (TextView) optionView.findViewById(R.id.auto_search_frequency_band_atv);
            TextView searched_number = (TextView) optionView.findViewById(R.id.auto_search_searched_number_atv);
            if (frequency != null && frequency_band != null && searched_number != null) {
                double freq = event.freq;
                freq /= 1000 * 1000;
                frequency.setText(Double.toString(freq) + mResources.getString(R.string.mhz));
                frequency_band.setText(parseFrequencyBand(freq));
                searched_number.setText(mResources.getString(R.string.searched_number) + ": " + channelNumber);
            }
        }
    }



    private String parseFrequencyBand(double freq) {
        String band = "";
        if (freq > 44.25 && freq < 143.25) {
            band = "VHFL";
        } else if (freq >= 143.25 && freq < 426.25) {
            band = "VHFH";
        } else if (freq >= 426.25 && freq < 868.25) {
            band = "UHF";
        }
        return band;
    }

    private int getIntegerFromString(String str) {
        if (str != null && str.contains("%"))
            return Integer.valueOf(str.replaceAll("%", ""));
        else
            return -1;
    }

    @Override
    public void onEvent(TvControlManager.ScannerEvent event) {
        ChannelInfo channel = null;
        String name = null;

        if (!isSearching())
            return;

        switch (event.type) {
            case TvControlManager.EVENT_SCAN_PROGRESS:
                int isNewProgram = 0;
                Log.d(TAG, "onEvent:"+event.precent + "%\tfreq[" + event.freq + "] lock[" + event.lock + "] strength[" + event.strength + "] quality[" + event.quality + "]");

                if ((event.mode == TVChannelParams.MODE_ANALOG) && (event.lock == 0x11)) { //trick here
                    isNewProgram = 1;
                    Log.d(TAG, "Resume Scanning");
                    if ((mTvControlManager.AtvDtvGetScanStatus() & TvControlManager.ATV_DTV_SCAN_STATUS_PAUSED)
                            == TvControlManager.ATV_DTV_SCAN_STATUS_PAUSED)
                        resumeSearch();
                } else if ((event.mode != TVChannelParams.MODE_ANALOG) && (event.programName.length() != 0)) {
                    try {
                        name = TVMultilingualText.getText(event.programName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isNewProgram = 2;
                }

                if (isNewProgram == 1) {
                    channelNumber++;
                    Log.d(TAG, "New ATV Program");
                } else if (isNewProgram == 2) {
                    if (event.srvType == 1)
                        channelNumber++;
                    else if (event.srvType == 2)
                        radioNumber++;
                    Log.d(TAG, "New DTV Program : [" + name + "] type[" + event.srvType + "]");
                }

                if (!isLiveTvScaning) {
                    //setProgress(event.precent);
                } else {
                    sendMessage(ChannelSearchActivity.PROCCESS, event.precent, null);
                    if (mLiveTvManualSearch) {
                        setLiveTvManualSearchInfo(event);
                    } else if (mLiveTvAutoSearch) {
                        setLiveTvAutoSearchFrequency(event);
                    }
                }
                if (optionTag == OPTION_MANUAL_SEARCH)
                    setManualSearchInfo(event);
                else
                    setAutoSearchFrequency(event);

                if ((event.mode == TVChannelParams.MODE_ANALOG) && ((optionTag == OPTION_MANUAL_SEARCH) || isLiveTvScaning)
                    && event.precent == 100)
                    stopSearch();
                break;

            case TvControlManager.EVENT_STORE_BEGIN:
                Log.d(TAG, "onEvent:Store begin");
                break;

            case TvControlManager.EVENT_STORE_END:
                Log.d(TAG, "onEvent:Store end");
                String prompt = mResources.getString(R.string.searched);
                int sumNumber = 0;
                if (channelNumber != 0) {
                    sumNumber += channelNumber;
                    prompt += " " + channelNumber + " " + mResources.getString(R.string.tv_channel);
                    if (radioNumber != 0) {
                        prompt += ",";
                    }
                }
                if (radioNumber != 0) {
                    sumNumber += radioNumber;
                    prompt += " " + radioNumber + " " + mResources.getString(R.string.radio_channel);
                }
                Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.ALL_CHANNELS_NUMBER, sumNumber);
                showToast(prompt);
                break;

            case TvControlManager.EVENT_SCAN_END:
                Log.d(TAG, "onEvent:Scan end");
                stopSearch();
                break;

            case TvControlManager.EVENT_SCAN_EXIT:
                Log.d(TAG, "onEvent:Scan exit.");
                SystemControlManager scm = new SystemControlManager(mContext);
                scm.setProperty("tv.channels.count", ""+(channelNumber+radioNumber));
                scm.writeSysFs(AUTO_ATSC_C_PATH, AUTO_ATSC_C_MODE_DISABLE);//disable auto select std lrc hrc
                isSearching = SEARCH_STOPPED;
                if (!isLiveTvScaning) {
                    ((TvSettingsActivity) mContext).finish();
                } else {
                    if (!mNumberSearchNeed) {
                        ((ChannelSearchActivity) mContext).finish();
                    } {
                        sendMessage(ChannelSearchActivity.STATUS, -1, "exit");
                    }
                }
                if (channelNumber == 0 && radioNumber == 0) {
                    int sumNumber1 = channelNumber + radioNumber;
                    Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.ALL_CHANNELS_NUMBER, sumNumber1);
                    showToast(mResources.getString(R.string.searched) + " 0 " + mResources.getString(R.string.channel));
                }
                break;
            default:
                break;
        }
    }

    /*private void createFactoryResetUi () {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.layout_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final AlertDialog mAlertDialog = builder.create();
        mAlertDialog.show();
        mAlertDialog.getWindow().setContentView(view);
        //mAlertDialog.getWindow().setLayout(150, 320);

        TextView button_cancel = (TextView)view.findViewById(R.id.dialog_cancel);
        button_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAlertDialog != null)
                    mAlertDialog.dismiss();
            }
        });
        button_cancel.setOnFocusChangeListener(this);
        TextView button_ok = (TextView)view.findViewById(R.id.dialog_ok);
        button_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mSettingsManager.doFactoryReset();
                ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE)).reboot("");
            }
        });
        button_ok.setOnFocusChangeListener(this);
    }*/

    private void showToast(String text) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.layout_toast, null);

        TextView propmt = (TextView)layout.findViewById(R.id.toast_content);
        propmt.setText(text);

        toast = new Toast(mContext);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    public void release() {
        mTvControlManager.setScannerListener(null);
        mSettingsManager = null;
        mTvControlManager = null;
    }

    public void setHandler (Handler handler) {
        mHandler = handler;
    }

    public void setFrequency (String value1, String value2) {
        Log.d(TAG, "[setFrequency] value1 = " + value1 + ", value2 = " + value2);
        mLiveTvFrequencyFrom = value1;
        mLiveTvFrequencyTo = value2;
    }

    public void setSearchSys (boolean value1, boolean value2) {
        Log.d(TAG, "[setSearchSys] value1 = " + value1 + ", value2 = " + value2);
        mSearchDtv = value1;
        mSearchAtv = value2;
    }

    public void setAtsccSearchSys (int value) {
        Log.d(TAG, "[setAtsccSearchSys] value = " + value);
        mAtsccMode = value;
    }

    private void sendMessage(int type, int message, Object information) {
        if (mHandler == null) {
            return;
        }
        Message msg = new Message();
        msg.arg1 = type;
        msg.what = message;
        msg.obj = information;
        mHandler.sendMessage(msg);
    }

    private static final String AUTO_ATSC_C_PATH = "/sys/module/aml_fe/parameters/auto_search_std";
    private static final String AUTO_ATSC_C_MODE_ENABLE = "1";
    private static final String AUTO_ATSC_C_MODE_DISABLE = "0";

    public void callManualSearch () {
        mLiveTvManualSearch = true;
        mLiveTvAutoSearch = false;
        SystemControlManager scm = new SystemControlManager(mContext);
        if (TvContract.Channels.TYPE_ATSC_C.equals(mSettingsManager.getDtvType()) && mAtsccMode > ATSC_C_HRC_SET) {
            scm.writeSysFs(AUTO_ATSC_C_PATH, AUTO_ATSC_C_MODE_ENABLE);
        } else {
            scm.writeSysFs(AUTO_ATSC_C_PATH, AUTO_ATSC_C_MODE_DISABLE);
        }
        if (mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
            startManualSearchAccordingMode();
        } else {
            startManualSearch();
        }
    }

    public void callAutosearch () {
        mLiveTvManualSearch = false;
        mLiveTvAutoSearch = true;
        SystemControlManager scm = new SystemControlManager(mContext);
        scm.writeSysFs(AUTO_ATSC_C_PATH, AUTO_ATSC_C_MODE_DISABLE);
        if (isSearching == SEARCH_STOPPED) {
            if (mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
                startAutosearchAccrodingTvMode();
            }else {
                startAutosearch();
            }
            //sendMessage(ChannelSearchActivity.STATUS, -1, mContext.getResources().getString(R.string.resume_search));
        } else if (isSearching == SEARCH_RUNNING) {//searching running
            pauseSearch();
            //sendMessage(ChannelSearchActivity.STATUS, -1, mContext.getResources().getString(R.string.pause_search));
             return ;
        } else {//searching paused
            resumeSearch();
            //sendMessage(ChannelSearchActivity.STATUS, -1, mContext.getResources().getString(R.string.resume_search));
            return;
        }
    }

    private int checkLiveTvAutoScanMode() {
        Log.d(TAG," TvAutoScanMode mSearchDtv = " + mSearchDtv + ", mSearchAtv = " + mSearchAtv);
        if (mSearchDtv && !mSearchAtv) {
            return ManualScanEdit.SCAN_ONLY_DTV;
        } else if (!mSearchDtv && mSearchAtv){
            return ManualScanEdit.SCAN_ONLY_ATV;
        }else if (mSearchDtv && mSearchAtv){
            return ManualScanEdit.SCAN_ATV_DTV;
        }else {
            return ManualScanEdit.SCAN_FAULT;
        }
    }

    private void setLiveTvManualSearchInfo(TvControlManager.ScannerEvent event) {
        Log.d(TAG, "[setLiveTvManualSearchInfo] mSettingsManager.getCurentVirtualTvSource()="+mSettingsManager.getCurentVirtualTvSource());
        if (event == null)
            return;
        if (mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
            ArrayList<HashMap<String, Object>> dataList = getSearchedDtvInfo(event);
            SimpleAdapter adapter = new SimpleAdapter(mContext, dataList,
                    R.layout.tv_channel_list,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
            sendMessage(ChannelSearchActivity.CHANNEL, 0, adapter);
            sendMessage(ChannelSearchActivity.STATUS, channelNumber, null);//send searched channelnumber
        }else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            ArrayList<HashMap<String, Object>> dataList = getSearchedDtvInfo(event);
            SimpleAdapter adapter = new SimpleAdapter(mContext, dataList,
                    R.layout.tv_channel_list,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
            sendMessage(ChannelSearchActivity.CHANNEL, 0, adapter);
            sendMessage(ChannelSearchActivity.STATUS, channelNumber, null);//send searched channelnumber
        }
    }

    private void setLiveTvAutoSearchFrequency(TvControlManager.ScannerEvent event) {
        if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV
            || mSettingsManager.getCurentVirtualTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
            ArrayList<HashMap<String, Object>> dataList = getSearchedDtvInfo(event);
            SimpleAdapter adapter = new SimpleAdapter(mContext, dataList,
                    R.layout.tv_channel_list,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
            sendMessage(ChannelSearchActivity.CHANNEL, 0, adapter);
        }
    }

/************************mSettingsManager interface start****************************************8*/
    public TvControlManager.SourceInput_Type getCurentVirtualTvSource () {
        return mSettingsManager.getCurentVirtualTvSource();
    }

    public TvControlManager.SourceInput_Type getCurentTvSource () {
        return mSettingsManager.getCurentTvSource();
    }

    public void startTvPlayAndSetSourceInput() {
            mSettingsManager.startTvPlayAndSetSourceInput();
    }

    public int getActivityResult() {
        if (mSettingsManager != null)
            return mSettingsManager.getActivityResult();
        else
            return -1;
    }

    public static final int SET_DTMB = 0;
    public static final int SET_DVB_C = 1;
    public static final int SET_DVB_T = 2;
    public static final int SET_DVB_T2 = 3;
    public static final int SET_ATSC_T = 4;
    public static final int SET_ATSC_C= 5;
    public static final int SET_ISDB_T = 6;

    public int getDtvTypeStatus() {
        String type = mSettingsManager.getDtvType();
        int ret = SET_ATSC_T;
        if (TextUtils.equals(type, TvContract.Channels.TYPE_DTMB)) {
                ret = SET_DTMB;
        } else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_C)) {
                ret = SET_DVB_C;
        } else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_T)) {
                ret = SET_DVB_T;
        } else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_T2)) {
                ret = SET_DVB_T2;
        } else if (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_T)) {
                ret = SET_ATSC_T;
        } else if (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_C)) {
                ret = SET_ATSC_C;
        } else if (TextUtils.equals(type, TvContract.Channels.TYPE_ISDB_T)) {
                ret = SET_ISDB_T;
        }
        return ret;
    }

    public void setDtvType(int value) {
        String type = null;
        switch (value) {
            case SET_DTMB:
                type = TvContract.Channels.TYPE_DTMB;
                break;
            case SET_DVB_C:
                type = TvContract.Channels.TYPE_DVB_C;
                break;
            case SET_DVB_T:
                type = TvContract.Channels.TYPE_DVB_T;
                break;
            case SET_DVB_T2:
                type = TvContract.Channels.TYPE_DVB_T2;
                break;
            case SET_ATSC_T:
                type = TvContract.Channels.TYPE_ATSC_T;
                break;
            case SET_ATSC_C:
                type = TvContract.Channels.TYPE_ATSC_C;
                break;
            case SET_ISDB_T:
                type = TvContract.Channels.TYPE_ISDB_T;
                break;
        }
        if (type != null) {
            mSettingsManager.setDtvType(type);
        }
    }

    private boolean mNumberSearchNeed = false;;

    public void setNumberSearchNeed(boolean need) {
        mNumberSearchNeed = need;
    }

/************************mSettingsManager interface end****************************************8*/

/************************mTvControlManager interface start****************************************8*/
    public void DtvStopScan() {
        mTvControlManager.DtvStopScan();
    }

    public void StopTv() {
        mTvControlManager.StopTv();
    }

/************************mTvControlManager interface start****************************************8*/
}
