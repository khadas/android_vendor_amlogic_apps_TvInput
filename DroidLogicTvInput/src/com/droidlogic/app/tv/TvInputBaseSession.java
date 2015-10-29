package com.droidlogic.app.tv;

import android.content.Context;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputManager.Hardware;
import android.media.tv.TvInputManager.HardwareCallback;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

public abstract class TvInputBaseSession extends TvInputService.Session implements Handler.Callback {
    private static final boolean DEBUG = true;
    private static final String TAG = "TvInputBaseSession";

    private String mInputId;
    private int mDeviceId;
    private Surface mSurface;
    private Hardware mHardware;
    private TvInputManager mTvInputManager;
    private TvStreamConfig[] mConfigs;
    private boolean isTuneNotReady = false;
    private Uri mChannelUri;
    private HandlerThread mHandlerThread;
    private Handler mSessionHandler;

    private HardwareCallback mHardwareCallback = new HardwareCallback(){
        @Override
        public void onReleased() {
            if (DEBUG)
                Log.d(TAG, "==== onReleased ===");
        }

        @Override
        public void onStreamConfigChanged(TvStreamConfig[] configs) {
            if (DEBUG)
                Log.d(TAG, "==== onStreamConfigChanged ====");
            mConfigs = configs;
        }
    };

    public TvInputBaseSession(Context context, String inputId, int deviceId) {
        super(context);
        mInputId = inputId;
        mDeviceId = deviceId;
        mTvInputManager = (TvInputManager)context.getSystemService(Context.TV_INPUT_SERVICE);
        mHardware = mTvInputManager.acquireTvInputHardware(deviceId,
                mHardwareCallback, mTvInputManager.getTvInputInfo(inputId));
        initThread(mInputId);
    }

    private void initThread(String inputId) {
        mHandlerThread = new HandlerThread(inputId);
        mHandlerThread.start();
        mSessionHandler = new Handler(mHandlerThread.getLooper(), this);
    }

    private void releaseThread() {
        mHandlerThread.quit();
        mHandlerThread = null;
        mSessionHandler = null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (DEBUG)
            Log.d(TAG, "==== handleMessage ====" + msg.what);
        switch (msg.what) {
            case DroidLogicTvUtils.SESSION_DO_RELEASE:
                doRelease();
                break;
            case DroidLogicTvUtils.SESSION_DO_SURFACE_CHANGED:
                doSurfaceChanged((Uri)msg.obj);
                break;
            case DroidLogicTvUtils.SESSION_DO_TUNE:
                doTune((Uri)msg.obj);
                break;
            case DroidLogicTvUtils.SESSION_UNBLOCK_CONTENT:
                doUnblockContent((TvContentRating)msg.obj);
                break;
            default:
                break;
        }
        return false;
    }

    public void doRelease() {
        mHardware.setSurface(null, null);
        mTvInputManager.releaseTvInputHardware(mDeviceId, mHardware);
        releaseThread();
    }

    public void doTune(Uri uri) {
        mHardware.setSurface(mSurface, mConfigs[0]);
    }

    public void doSurfaceChanged(Uri uri) {
        mHardware.setSurface(mSurface, mConfigs[0]);
    }

    public void doUnblockContent(TvContentRating rating) {}

    @Override
    public void onRelease() {
        doRelease();
    }

    @Override
    public boolean onSetSurface(Surface surface) {
        mSurface = surface;
        return false;
    }

    @Override
    public void onSurfaceChanged(int format, int width, int height) {
        if (isTuneNotReady) {
            mSessionHandler.obtainMessage(
                    DroidLogicTvUtils.SESSION_DO_SURFACE_CHANGED, mChannelUri).sendToTarget();
            isTuneNotReady = false;
        }
    }

    @Override
    public void onSetStreamVolume(float volume) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onTune(Uri channelUri) {
        if (DEBUG)
            Log.d(TAG, "==== onTune ====" + channelUri);
        mChannelUri = channelUri;
        if (mSurface == null) {//TvView is not ready
            isTuneNotReady = true;
        } else {
            mSessionHandler.obtainMessage(
                    DroidLogicTvUtils.SESSION_DO_TUNE, mChannelUri).sendToTarget();
        }
        return false;
    }

    @Override
    public void onSetCaptionEnabled(boolean enabled) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUnblockContent(TvContentRating unblockedRating) {
        if (DEBUG)
            Log.d(TAG, "==== onUnblockContent ====");
        mSessionHandler.obtainMessage(
                DroidLogicTvUtils.SESSION_UNBLOCK_CONTENT, unblockedRating).sendToTarget();
    }

}
