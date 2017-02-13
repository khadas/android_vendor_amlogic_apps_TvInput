package com.droidlogic.tvinput.services;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvStreamConfig;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.Surface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.media.tv.TvInputManager.Hardware;
import android.media.tv.TvInputManager.HardwareCallback;
import android.media.tv.TvInputHardwareInfo;
import com.droidlogic.app.SystemControlManager;

public class HdmiExtendInputService extends TvInputService {
    private static final String TAG = "HdmiExtendInputService";
    private static final boolean DEBUG = true;
    private static final int DEVICE_ID_HDMIEXTEND = 8;
    private HdmiExtendInputSession mSession;
    private String mInputId;
    private Surface mSurface;
    private Context mContext;
    public Hardware mHardware;
    public TvStreamConfig[] mConfigs;
    private TvInputManager mTvInputManager;
    private SystemControlManager mSystemControlManager = new SystemControlManager(this);
    private int mDeviceId = -1;
    private SparseArray<TvInputInfo> mInfoList = new SparseArray<TvInputInfo>();

    private HardwareCallback mHardwareCallback = new HardwareCallback(){
        @Override
        public void onReleased() {
            if (DEBUG)
                Log.d(TAG, "onReleased");

            mHardware = null;
        }

        @Override
        public void onStreamConfigChanged(TvStreamConfig[] configs) {
            if (DEBUG)
                Log.d(TAG, "onStreamConfigChanged");
            mConfigs = configs;
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mTvInputManager = (TvInputManager)this.getSystemService(Context.TV_INPUT_SERVICE);
    }

    @Override
    public Session onCreateSession(String inputId) {
        Log.d(TAG, "onCreateSession");
        if (mHardware != null && mDeviceId != -1) {
            mTvInputManager.releaseTvInputHardware(mDeviceId,mHardware);
        }
        mInputId = inputId;
        mDeviceId = getHardwareDeviceId(inputId);
        mHardware = mTvInputManager.acquireTvInputHardware(mDeviceId,mHardwareCallback,mTvInputManager.getTvInputInfo(inputId));
        mSession = new HdmiExtendInputSession(getApplicationContext());
        Log.d(TAG, "mHardware :" + mHardware);
        return mSession;
    }

    public class HdmiExtendInputSession extends TvInputService.Session{
        public HdmiExtendInputSession(Context context) {
            super(context);
            mContext = context;
            Log.d(TAG, "HdmiExtendInputSession");
        }
        @Override
        public boolean onSetSurface(Surface surface) {
            Log.d(TAG, "onSetSurface");
            mSurface = surface;
            return false;
        }
        @Override
        public boolean onTune(Uri channelUri) {
            Log.d(TAG, "onTune " );
            Log.d(TAG, "mConfigs :" + mConfigs +" mSurface :" + mSurface);
            if (mConfigs != null && mSurface != null && mHardware != null) {
                Log.d(TAG, "setSurface start!");
                mHardware.setSurface(mSurface,mConfigs[0]);
                mSession.notifyVideoAvailable();
            }
            Log.d(TAG, "onTune finish");
            return false;
        }
        @Override
        public void onRelease() {
            Log.d(TAG, "onRelease");
            mHardware.setSurface(null, null);
        }
        @Override
        public void onSetStreamVolume(float volume) {
            Log.d(TAG, "onSetStreamVolume");
          /* if ( 1.0 == volume ) {
                mSystemControlManager.openAmAudio(48000, 1, 2);
            } else {
                mSystemControlManager.closeAmAudio();
            }*/
        }
        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            Log.d(TAG, "onSetCaptionEnabled");
        }
        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (isNavigationKey(keyCode)) {
                mHardware.dispatchKeyEventToHdmi(event);
                return true;
            }
            return false;
        }
        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            return false;
        }
        @Override
        public void onSurfaceChanged(int format, int width, int height) {
            super.onSurfaceChanged(format, width, height);
        }
    }

    private int getHardwareDeviceId(String input_id) {
        int id = 0;
        for (int i = 0; i < mInfoList.size(); i++) {
            if (input_id.equals(mInfoList.valueAt(i).getId())) {
                id = mInfoList.keyAt(i);
                break;
            }
        }
        if (DEBUG)
            Log.d(TAG, "device id is " + id);
        return id;
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        Log.d(TAG, "onHardwareAdded ," + "DeviceId :" + hardwareInfo.getDeviceId());
        if (hardwareInfo.getDeviceId() != DEVICE_ID_HDMIEXTEND
                || hasInfoExisted(hardwareInfo))
            return null;
        TvInputInfo info = null;
        ResolveInfo rinfo = getResolveInfo(HdmiExtendInputService.class.getName());
        if (rinfo != null) {
            try {
              info = TvInputInfo.createTvInputInfo(getApplicationContext(), rinfo, hardwareInfo, null, null);
            } catch (XmlPullParserException e) {
                // TODO: handle exception
            } catch (IOException e) {
                // TODO: handle exception
            }
        }
        updateInfoListIfNeededLocked(hardwareInfo, info, false);
        return info;
    }

    public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
        Log.d(TAG, "onHardwareRemoved");
        if (hardwareInfo.getDeviceId() != mDeviceId)
            return null;
        TvInputInfo info = getTvInputInfo(hardwareInfo);
        String id = null;
        if (info != null)
            id = info.getId();
        updateInfoListIfNeededLocked(hardwareInfo, info, true);
        return id;
    }

    public void updateInfoListIfNeededLocked(TvInputHardwareInfo hinfo, TvInputInfo info, boolean isRemoved) {
        int Id = hinfo.getDeviceId();
        if (isRemoved) {
            mInfoList.remove(Id);
        } else {
            mInfoList.put(Id, info);
        }
        if (DEBUG)
           Log.d(TAG, "size of mInfoList is " + mInfoList.size());
    }

    protected TvInputInfo getTvInputInfo(TvInputHardwareInfo hardwareInfo) {
        return mInfoList.get(hardwareInfo.getDeviceId());
    }

    public ResolveInfo getResolveInfo(String cls_name) {
        if (TextUtils.isEmpty(cls_name))
            return null;
        ResolveInfo ret_ri = null;
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(new Intent(TvInputService.SERVICE_INTERFACE),
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (!android.Manifest.permission.BIND_TV_INPUT.equals(si.permission)) {
                continue;
            }
            if (DEBUG)
                Log.d(TAG, "cls_name = " + cls_name + ", si.name = " + si.name);
            if (cls_name.equals(si.name)) {
                ret_ri = ri;
                break;
            }
        }
        return ret_ri;
    }

    private boolean hasInfoExisted(TvInputHardwareInfo hInfo) {
        return mInfoList.get(hInfo.getDeviceId()) == null ? false : true;
    }
}
