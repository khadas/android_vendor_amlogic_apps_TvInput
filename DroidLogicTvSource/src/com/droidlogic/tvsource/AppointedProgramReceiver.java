package com.droidlogic.tvsource;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.text.TextUtils;
import android.util.Log;

import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.Program;
import com.droidlogic.app.tv.TvDataBaseManager;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;

public class AppointedProgramReceiver extends BroadcastReceiver implements OnClickListener, OnFocusChangeListener{
    private static final String TAG = "AppointedProgramReceiver";
    private static final String PACKAGE_LAUNCHER = "com.droidlogic.mboxlauncher";
    private static final String PACKAGE_TV_APP = "com.droidlogic.tvsource";
    private static final String CLASS_TV_APP = "com.droidlogic.tvsource.DroidLogicTv";
    private static final int MSG_COUNT_DOWN = 0;
    private int countdown = 60;

    private Context mContext;
    private TextView tx_title;
    private Timer timer;
    private int channelIndex = -1;
    private boolean isRadio = false;
    private AlertDialog mAlertDialog = null;

	@Override
	public void onReceive(Context context, Intent intent) {
        mContext = context;
        long programId = intent.getLongExtra(DroidLogicTvUtils.EXTRA_PROGRAM_ID, -1L);
        TvDataBaseManager tbm = new TvDataBaseManager(mContext);
        List<Program> programList = tbm.getPrograms(TvContract.buildProgramUri(programId));

        if (programList.size() > 0) {
            Program program = tbm.getPrograms(TvContract.buildProgramUri(programId)).get(0);
            program.setIsAppointed(false);
            tbm.updateProgram(program);

            ChannelInfo channel = tbm.getChannelInfo(TvContract.buildChannelUri(program.getChannelId()));
            ArrayList<ChannelInfo> channelList;

            if (ChannelInfo.isRadioChannel(channel)) {
                isRadio = true;
                channelList = tbm.getChannelList(channel.getInputId(), Channels.SERVICE_TYPE_AUDIO, true);
            } else {
                isRadio = false;
                channelList = tbm.getChannelList(channel.getInputId(), Channels.SERVICE_TYPE_AUDIO_VIDEO, true);
            }
            for (int i = 0; i < channelList.size(); i++) {
                if (ChannelInfo.isSameChannel(channel, channelList.get(i))) {
                    channelIndex = i;
                    break;
                }
            }
            Log.d(TAG, "receive appointed channel:" + channel.getDisplayName() + " program: " + program.getTitle());

            LayoutInflater inflater =(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.layout_dialog, null);

            if (mAlertDialog == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                mAlertDialog = builder.create();
                mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            }
            mAlertDialog.show();
            mAlertDialog.getWindow().setContentView(view);

            tx_title = (TextView)view.findViewById(R.id.dialog_title);
            TextView tx_content = (TextView)view.findViewById(R.id.dialog_content);
            tx_content.setText(mContext.getResources().getString(R.string.watch_program) + " " + program.getTitle());

            TextView button_cancel = (TextView)view.findViewById(R.id.dialog_cancel);
            button_cancel.setOnClickListener(this);
            button_cancel.setOnFocusChangeListener(this);

            TextView button_ok = (TextView)view.findViewById(R.id.dialog_ok);
            button_ok.setOnClickListener(this);
            button_ok.setOnFocusChangeListener(this);

            timer = new Timer(true);
            timer.schedule(task, 0, 1000);
        }else {
            Log.d(TAG, "the appointed program is not exist" + programId);
        }
	}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dialog_cancel:
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }
                break;
            case R.id.dialog_ok:
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }
                startTvApp();
                break;
        }
        timer.cancel();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v instanceof TextView) {
            if (hasFocus) {
                ((TextView) v).setTextColor(mContext.getResources().getColor(R.color.color_text_focused));
            } else {
                ((TextView) v).setTextColor(mContext.getResources().getColor(R.color.color_text_item));
            }
        }
    }

    public void startTvApp() {
        Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_CURRENT_DEVICE_ID, DroidLogicTvUtils.DEVICE_ID_DTV);
        Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_DTV_CHANNEL_INDEX, channelIndex);
        Settings.System.putInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_CURRENT_CHANNEL_IS_RADIO, isRadio? 1 : 0);

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PACKAGE_TV_APP, CLASS_TV_APP));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

        intent = new Intent();
        intent.setAction(DroidLogicTvUtils.ACTION_SWITCH_CHANNEL);
        intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, channelIndex);
        intent.putExtra(DroidLogicTvUtils.EXTRA_IS_RADIO_CHANNEL, isRadio);
        intent.putExtra("force_dtv", true);
        mContext.sendBroadcast(intent);

        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        am.forceStopPackage(PACKAGE_LAUNCHER);
    }

    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(MSG_COUNT_DOWN);
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_COUNT_DOWN:
                    tx_title.setText(Integer.toString(countdown) + " " + mContext.getResources().getString(R.string.switch_program_propmt));
                    if (countdown == 0) {
                        if (mAlertDialog != null) {
                            mAlertDialog.dismiss();
                        }
                        startTvApp();
                        timer.cancel();
                    }
                    countdown--;
                    break;
                default:
                    break;
            }
        }
    };
}
