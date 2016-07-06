package com.droidlogic.tvinput.services;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import com.droidlogic.tvinput.Utils;

import com.droidlogic.app.tv.DroidLogicTvInputService;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvInputBaseSession;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputManager.Hardware;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Surface;

public class SPDIFInputService extends DroidLogicTvInputService {
    private static final String TAG = SPDIFInputService.class.getSimpleName();;
    private SPDIFInputSession mCurrentSession;
    private int number = 0;
    private int currentNumber = 0;

    @Override
    public Session onCreateSession(String inputId) {
        super.onCreateSession(inputId);

        mCurrentSession = new SPDIFInputSession(getApplicationContext(), inputId, getHardwareDeviceId(inputId));
        registerInputSession(mCurrentSession);
        mCurrentSession.setNumber(number);
        number++;

        return mCurrentSession;
    }

    public class SPDIFInputSession extends TvInputBaseSession {
        public SPDIFInputSession(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);
            Utils.logd(TAG, "=====new SPDIFInputSession=====");
        }

        public TvStreamConfig[] getConfigs() {
            return mConfigs;
        }

        public Hardware getHardware() {
            return mHardware;
        }

        public int getCurrentSessionNumber() {
            return currentNumber;
        }

        public void setCurrentSession() {
           currentNumber = getNumber();
           mCurrentSession = this;
           registerInputSession(mCurrentSession);
        }

        @Override
        public void doRelease() {
            super.doRelease();
        }

        @Override
        public void doAppPrivateCmd(String action, Bundle bundle) {
            super.doAppPrivateCmd(action, bundle);
            if (TextUtils.equals(DroidLogicTvUtils.ACTION_STOP_TV, action)) {
                stopTv();
            }
        }

        @Override
        public void doSetSurface(Surface surface) {
            super.doSetSurface(surface);
        }
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getDeviceId() != DroidLogicTvUtils.DEVICE_ID_SPDIF
            || hasInfoExisted(hardwareInfo))
            return null;

        Utils.logd(TAG, "=====onHardwareAdded=====" + hardwareInfo.getDeviceId());

        TvInputInfo info = null;
        ResolveInfo rInfo = getResolveInfo(SPDIFInputService.class.getName());
        if (rInfo != null) {
            try {
                info = TvInputInfo.createTvInputInfo(
                           getApplicationContext(),
                           rInfo,
                           hardwareInfo,
                           getTvInputInfoLabel(hardwareInfo.getDeviceId()),
                           null);
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
        if (hardwareInfo.getDeviceId() != DroidLogicTvUtils.DEVICE_ID_SPDIF
            || !hasInfoExisted(hardwareInfo))
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
