package com.droidlogic.tvinput.services;


import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import com.droidlogic.common.DroidLogicTvInputService;
import com.droidlogic.utils.Utils;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputManager.Hardware;
import android.media.tv.TvInputManager.HardwareCallback;
import android.media.tv.TvInputService;
import android.media.tv.TvStreamConfig;
import android.net.Uri;
import android.view.Surface;

public class HdmiInputService extends DroidLogicTvInputService {
    private static final String TAG = HdmiInputService.class.getSimpleName();

    @Override
    public Session onCreateSession(String inputId) {
        Utils.logd(TAG, "=====onCreateSession====");
        return new HdmiInputSession(getApplicationContext(), inputId);
    }

    public class HdmiInputSession extends TvInputService.Session {
        private String mInputId;
        private Surface mSurface;
        private Hardware mHardware;
        private TvInputManager mTvInputManager;
        private TvStreamConfig[] mConfigs;
        private HardwareCallback mHardwareCallback = new HardwareCallback(){
            @Override
            public void onReleased() {
                Utils.logd(TAG, "====onReleased===");
            }

            @Override
            public void onStreamConfigChanged(TvStreamConfig[] configs) {
                Utils.logd(TAG, "===onStreamConfigChanged==");
                mConfigs = configs;
            }
        };

        public HdmiInputSession(Context context, String inputId) {
            super(context);
            mInputId = inputId;
            mTvInputManager = (TvInputManager)context.getSystemService(Context.TV_INPUT_SERVICE);
            mHardware = mTvInputManager.acquireTvInputHardware(getHardwareDeviceId(inputId),
                    mHardwareCallback, mTvInputManager.getTvInputInfo(inputId));
        }

        @Override
        public void onRelease() {
            mTvInputManager.releaseTvInputHardware(getHardwareDeviceId(mInputId), mHardware);
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            mSurface = surface;
            return false;
        }

        @Override
        public void onSetStreamVolume(float volume) {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean onTune(Uri channelUri) {
            Utils.logd(TAG, "====onTune====");
            switchToSourceInput();
            return false;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            // TODO Auto-generated method stub
        }

        private void switchToSourceInput(){
            mHardware.setSurface(mSurface, mConfigs[0]);
        }

    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_HDMI)
            return null;

        Utils.logd(TAG, "=====onHardwareAdded====="+hardwareInfo.getDeviceId());

        TvInputInfo info = null;
        ResolveInfo rInfo = getResolveInfo(HdmiInputService.class.getName());
        if (rInfo != null) {
            try {
                info = TvInputInfo.createTvInputInfo(getApplicationContext(), rInfo,
                        hardwareInfo, null, null);
            } catch (XmlPullParserException e) {
                // TODO: handle exception
            }catch (IOException e) {
                // TODO: handle exception
            }
        }
        updateInfoListIfNeededLocked(hardwareInfo, info, false);

        return info;
    }

    public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_HDMI)
            return null;

        TvInputInfo info = getTvInputInfo(hardwareInfo);
        String id = null;
        if (info != null)
            id = info.getId();
        updateInfoListIfNeededLocked(hardwareInfo, info, true);

        Utils.logd(TAG, "=====onHardwareRemoved=====" + id);
        return id;
    }

}
