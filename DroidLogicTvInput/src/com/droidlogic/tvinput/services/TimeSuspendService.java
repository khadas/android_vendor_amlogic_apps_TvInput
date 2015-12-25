package com.droidlogic.tvinput.services;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import com.droidlogic.tvinput.R;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.DroidLogicKeyEvent;


public class TimeSuspendService extends Service {
    private final String TAG = "TimeSuspendService";

    /* time suspend dialog */
    private AlertDialog mDialog;//use to dismiss
    private TextView mCountDownText;
    private int mSuspendCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public IBinder onBind ( Intent intent ) {
        return null;
    }

    @Override
    public int onStartCommand ( Intent intent, int flags, int startId ) {
        Log.d ( TAG, "onStartCommand");
        if (intent != null)
            Log.d(TAG, "intent="+intent);

        reset_shutdown_time();

        return super.onStartCommand ( intent, flags, startId );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d ( TAG, "onDestroy");
        remove_shutdown_time();
    }

    private void initTimeSuspend() {
        AlertDialog.Builder suspendDialog = new AlertDialog.Builder(this);
        View suspendDialogView = View.inflate(this, R.layout.timesuspend_dialog, null);
        Button mBn = (Button) suspendDialogView.findViewById(R.id.btn_cancel_suspend);
        mBn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
            }
        });
        mCountDownText = (TextView) suspendDialogView.findViewById(R.id.tv_dialog);
        suspendDialog.setView(suspendDialogView);
        mDialog = suspendDialog.create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    }

    private Handler timeSuspend_handler = new Handler();
    private Runnable timeSuspend_runnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mSuspendCount > 60)
                    mSuspendCount = 60;
                else
                    mSuspendCount--;
                if (mSuspendCount == 0) {
                    long now = SystemClock.uptimeMillis();
                    KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, DroidLogicKeyEvent.KEYCODE_POWER, 0);
                    KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, DroidLogicKeyEvent.KEYCODE_POWER, 0);
                    InputManager.getInstance().injectInputEvent(down, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
                    InputManager.getInstance().injectInputEvent(up, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
                    stopSelf();
                }
                else {
                    if (mSuspendCount == 60) {
                        String str = mSuspendCount + " " + getResources().getString(R.string.countdown_tips);
                        initTimeSuspend();
                        mCountDownText.setText(str);
                        mDialog.show();
                    } else if (mSuspendCount < 60){
                        String str = mSuspendCount + " " + getResources().getString(R.string.countdown_tips);
                        mCountDownText.setText(str);
                    }
                    timeSuspend_handler.postDelayed(timeSuspend_runnable, 1000);
                }
            }
            catch (RuntimeException e) {
                        e.printStackTrace();
            }

        }
    };

    private void reset_shutdown_time() {
        int sleepTime = SystemProperties.getInt("tv.sleep_timer", 0);
        mSuspendCount = sleepTime*60;
        if (mSuspendCount > 60) {
            timeSuspend_handler.removeCallbacks(timeSuspend_runnable);
            timeSuspend_handler.postDelayed(timeSuspend_runnable, (mSuspendCount-60)*1000);
        }
    }

    private void remove_shutdown_time() {
        Log.d ( TAG, "remove_shutdown_time");
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        SystemProperties.set("tv.sleep_timer", "0");
        timeSuspend_handler.removeCallbacks(timeSuspend_runnable);
    }


}
