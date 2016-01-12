package com.droidlogic.tvinput.shortcut;

import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.Program;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.app.tv.TVTime;
import com.droidlogic.tvinput.settings.SettingsManager;
import com.droidlogic.tvinput.shortcut.GuideListView.ListItemSelectedListener;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.ArrayMap;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.droidlogic.tvinput.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

public class ShortCutActivity extends Activity implements ListItemSelectedListener, OnItemClickListener{
    private static final String TAG = "ShortCutActivity";

    private static final int MSG_FINISH = 0;
    private static final int MSG_UPDATE_PROGRAM = 1;

    private static final int TOAST_SHOW_TIME = 3000;

    private SettingsManager mSettingsManager;
    private TvDataBaseManager mTvDataBaseManager;
    private Resources mResources;
    private Toast toast = null;

    private GuideListView lv_channel;
    private GuideListView lv_week;
    private GuideListView lv_program;
    private TextView tx_date;
    private TextView tx_program_description;
    private ArrayList<ChannelInfo> channelInfoList;
    private ArrayList<ArrayMap<String,Object>> list_channels  = new ArrayList<ArrayMap<String,Object>>();
    private ArrayList<ArrayMap<String,Object>> list_date = new ArrayList<ArrayMap<String,Object>>();
    private ArrayList<ArrayMap<String,Object>> list_program = new ArrayList<ArrayMap<String,Object>>();
    ArrayList<ArrayList<ArrayList<ArrayMap<String,Object>>>> channel_data = new ArrayList<ArrayList<ArrayList<ArrayMap<String,Object>>>>();
    private SimpleAdapter channelsAdapter;
    private SimpleAdapter weekAdapter;
    private SimpleAdapter programAdapter;
    private int currentChannelIndex = -1;
    private TVTime mTvTime = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mSettingsManager = new SettingsManager(this, getIntent());
        mTvDataBaseManager = new TvDataBaseManager(this);
        mResources = getResources();

        int mode = getIntent().getIntExtra(DroidLogicTvUtils.EXTRA_KEY_CODE, 0);
        setShortcutMode(mode);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
        Log.d(TAG, "------onStop");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE:
                setShortcutMode(keyCode);
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE:
                setShortcutMode(keyCode);
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE:
                setShortcutMode(keyCode);
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SLEEP:
                setShortcutMode(keyCode);
                break;
            case DroidLogicKeyEvent.KEYCODE_GUIDE:
                if (mTvTime != null) {
                    finish();
                }
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
                showCustomToast(mode);
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
                showCustomToast(mode);
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
                showCustomToast(mode);
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
                showCustomToast(mode);
                break;
            case DroidLogicKeyEvent.KEYCODE_MEDIA_AUDIO_CONTROL:

                break;
            case DroidLogicKeyEvent.KEYCODE_GUIDE:
                setContentView(R.layout.layout_shortcut_guide);
                setGuideView();
                break;
            default:
                break;
        }
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
        handler.removeMessages(MSG_FINISH);
        handler.sendEmptyMessageDelayed(MSG_FINISH, TOAST_SHOW_TIME);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FINISH:
                    finish();
                    break;
                case MSG_UPDATE_PROGRAM:
                    setDate();
                    break;
                default:
                    break;
            }
        }
    };

    BroadcastReceiver mReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_TICK)) {
                if (tx_date != null) {
                    String[] dateAndTime = getDateAndTime(mTvTime.getTime());
                    String currentTime = dateAndTime[0] + "." + dateAndTime[1] + "." + dateAndTime[2] + "   " + dateAndTime[3] + ":" + dateAndTime[4];

                    tx_date.setText(currentTime);
                }
            }
        }
    };

    private Runnable getProgramDataRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                loadChannelData();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    };

    private void setGuideView() {
        mTvTime = new TVTime(this);

        tx_date = (TextView)findViewById(R.id.guide_date);
        String[] dateAndTime = getDateAndTime(mTvTime.getTime());
        tx_date.setText(dateAndTime[0] + "." + dateAndTime[1] + "." + dateAndTime[2] + "   " + dateAndTime[3] + ":" + dateAndTime[4]);

        tx_program_description = (TextView)findViewById(R.id.guide_details_content);

        lv_channel = (GuideListView)findViewById(R.id.list_guide_channel);
        lv_week = (GuideListView)findViewById(R.id.list_guide_week);
        lv_program = (GuideListView)findViewById(R.id.list_guide_programs);

        channelInfoList = mTvDataBaseManager.getChannelList(mSettingsManager.getInputId(), Channels.SERVICE_TYPE_AUDIO_VIDEO);
        channelInfoList.addAll(mTvDataBaseManager.getChannelList(mSettingsManager.getInputId(), Channels.SERVICE_TYPE_AUDIO));
        new Thread(getProgramDataRunnable).start();

        list_channels = getChannelList(channelInfoList);
        channelsAdapter = new SimpleAdapter(this, list_channels,
                R.layout.layout_guide_single_text,
                new String[]{GuideListView.ITEM_1}, new int[]{R.id.text_name});
        lv_channel.setAdapter(channelsAdapter);

        lv_channel.setListItemSelectedListener(this);
        lv_channel.setOnItemClickListener(this);
        lv_week.setListItemSelectedListener(this);
        lv_program.setListItemSelectedListener(this);
    }

    public ArrayList<ArrayMap<String,Object>> getChannelList (ArrayList<ChannelInfo> channelInfoList) {
        ArrayList<ArrayMap<String,Object>> list =  new ArrayList<ArrayMap<String,Object>>();

        int videoChannelIndex = 0;
        int radioChannelIndex = 0;

        if (channelInfoList.size() > 0) {
            for (int i = 0 ; i < channelInfoList.size(); i++) {
                ChannelInfo info = channelInfoList.get(i);
                if (info != null && info.isBrowsable()) {
                    ArrayMap<String,Object> item = new ArrayMap<String,Object>();
                    item.put(GuideListView.ITEM_1, i + "  " + info.getDisplayName());
                    if (ChannelInfo.isRadioChannel(info)) {
                        item.put(GuideListView.ITEM_2, radioChannelIndex);
                        radioChannelIndex++;
                        item.put(GuideListView.ITEM_3, true);
                    } else {
                        item.put(GuideListView.ITEM_2, videoChannelIndex);
                        videoChannelIndex++;
                        item.put(GuideListView.ITEM_3, false);
                    }
                    list.add(item);
                }
            }
        }

        return list;
    }

    private void loadChannelData() {
        channel_data.clear();
        for (int i = 0; i < channelInfoList.size(); i++) {
            channel_data.add(getChannleData(channelInfoList.get(i)));
        }
    }

    public ArrayList<ArrayList<ArrayMap<String,Object>>> getChannleData (ChannelInfo channel) {
        Uri uri = TvContract.buildChannelUri(channel.getId());
        List<Program> channel_programs = mTvDataBaseManager.getPrograms(uri);
        ArrayList<ArrayList<ArrayMap<String,Object>>> list_all_program = new ArrayList<ArrayList<ArrayMap<String,Object>>>();
        list_all_program.clear();

        String tmp_date = "";
        ArrayList<ArrayMap<String,Object>> list_item_program = null;

        for (int j = 0; j < channel_programs.size(); j++) {
            Program program = channel_programs.get(j);
            String[] dateAndTime = getDateAndTime(program.getStartTimeUtcMillis());
            String[] endTime = getDateAndTime(program.getEndTimeUtcMillis());
            String month_and_date = dateAndTime[1] + "." + dateAndTime[2];

            if (!tmp_date.equals(month_and_date)) {
                tmp_date = month_and_date;

                if (list_item_program != null) {
                    list_all_program.add(list_item_program);
                }

                list_item_program = new ArrayList<ArrayMap<String,Object>>();

                ArrayMap<String,Object> item_date = new ArrayMap<String,Object>();
                item_date.put(GuideListView.ITEM_1, month_and_date);
                list_item_program.add(item_date);

                ArrayMap<String,Object> item_program = new ArrayMap<String,Object>();
                if (mTvTime.getTime() >= program.getStartTimeUtcMillis() && mTvTime.getTime() <= program.getEndTimeUtcMillis()) {
                    item_program.put(GuideListView.ITEM_1, dateAndTime[3] + ":" + dateAndTime[4]
                        + "~" + endTime[3] + ":" + endTime[4]
                        + " " + mResources.getString(R.string.playing));
                } else {
                    item_program.put(GuideListView.ITEM_1, dateAndTime[3] + ":" + dateAndTime[4]);
                }
                item_program.put(GuideListView.ITEM_2, program.getTitle());
                item_program.put(GuideListView.ITEM_3, program.getDescription());
                list_item_program.add(item_program);
            } else {
                ArrayMap<String,Object> item_program = new ArrayMap<String,Object>();
                if (mTvTime.getTime() >= program.getStartTimeUtcMillis() && mTvTime.getTime() <= program.getEndTimeUtcMillis()) {
                    item_program.put(GuideListView.ITEM_1, dateAndTime[3] + ":" + dateAndTime[4]
                        + "~" + endTime[3] + ":" + endTime[4]
                        + " " + mResources.getString(R.string.playing));
                } else {
                    item_program.put(GuideListView.ITEM_1, dateAndTime[3] + ":" + dateAndTime[4]
                        + "~" + endTime[3] + ":" + endTime[4]);
                }
                item_program.put(GuideListView.ITEM_2, program.getTitle());
                item_program.put(GuideListView.ITEM_3, program.getDescription());
                list_item_program.add(item_program);
            }
        }
        if (list_item_program != null)
            list_all_program.add(list_item_program);

        return list_all_program;
    }

    public void setDate() {
        handler.removeMessages(MSG_UPDATE_PROGRAM);
        list_date.clear();

        if (channel_data.size() > currentChannelIndex) {
            ArrayList<ArrayList<ArrayMap<String,Object>>> list = channel_data.get(currentChannelIndex);
            if (list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    list_date.add(list.get(i).get(0));
                }
            } else {
                ArrayMap<String,Object> item = new ArrayMap<String,Object>();
                item.put(GuideListView.ITEM_1, mResources.getString(R.string.no_program));
                list_date.add(item);
            }
            //weekAdapter.notifyDataSetChanged();
            weekAdapter = new SimpleAdapter(this, list_date,
                    R.layout.layout_guide_single_text_center,
                    new String[]{GuideListView.ITEM_1}, new int[]{R.id.text_name});
            lv_week.setAdapter(weekAdapter);

            if (list_date.size() > 0) {
                for (int i = 0; i < list_date.size(); i++) {
                    String[] dateAndTime = getDateAndTime(mTvTime.getTime());
                    if ((dateAndTime[1] + "." + dateAndTime[2]).equals(list_date.get(i).get(GuideListView.ITEM_1).toString())) {
                        lv_week.setSelection(i);
                        return;
                    }
                }
                lv_week.setSelection(0);
            }
        }else {
            handler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRAM, 200);
        }
    }

    public void setPrograms (int position) {
        list_program.clear();

        ArrayList<ArrayList<ArrayMap<String,Object>>> list = channel_data.get(currentChannelIndex);
        if (list.size() > 0) {
            ArrayList<ArrayMap<String,Object>> programs = list.get(position);
            list_program.addAll(programs);
            list_program.remove(0);
        } else {
            ArrayMap<String,Object> item = new ArrayMap<String,Object>();
            item.put(GuideListView.ITEM_1, mResources.getString(R.string.no_program));
            list_program.add(item);
        }

        programAdapter = new SimpleAdapter(this, list_program,
                R.layout.layout_guide_double_text,
                new String[] {GuideListView.ITEM_1, GuideListView.ITEM_2},
                new int[] {R.id.text_name, R.id.text_status});
        lv_program.setAdapter(programAdapter);

        if (list_program.size() > 0) {
            for (int i = 0; i < list_program.size(); i++) {
                if (list_program.get(i).get(GuideListView.ITEM_1).toString().contains(mResources.getString(R.string.playing))) {
                    lv_program.setSelection(i);
                    return;
                }
            }
            lv_program.setSelection(0);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.list_guide_channel:
                sendSwitchChannelBroadcast(position);
                break;
        }
    }

    @Override
    public void onListItemSelected(View parent, int position) {
        switch (parent.getId()) {
            case R.id.list_guide_channel:
                lv_week.setAdapter(null);
                lv_program.setAdapter(null);
                currentChannelIndex = position;
                setDate();
                break;
            case R.id.list_guide_week:
                if (list_date.size() > 0) {
                    setPrograms(position);
                }
                break;
            case R.id.list_guide_programs:
                Object description = list_program.get(position).get(GuideListView.ITEM_3);
                if (description != null) {
                    tx_program_description.setText(description.toString());
                } else {
                    tx_program_description.setText(mResources.getString(R.string.no_information));
                }
                break;
        }
    }

    private void sendSwitchChannelBroadcast(int position) {
        int channelIndex = (int)list_channels.get(position).get(GuideListView.ITEM_2);
        boolean isRadio = (boolean)list_channels.get(position).get(GuideListView.ITEM_3);

        Intent intent = new Intent(DroidLogicTvUtils.ACTION_SWITCH_CHANNEL);
        intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, channelIndex);
        intent.putExtra(DroidLogicTvUtils.EXTRA_IS_RADIO_CHANNEL, isRadio);
        sendBroadcast(intent);
    }

    public String[] getDateAndTime(long dateTime) {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        sDateFormat.setTimeZone(TimeZone.getDefault());
        String[] dateAndTime = sDateFormat.format(new Date(dateTime + 0)).split("\\/| |:");

        return dateAndTime;
    }
}
