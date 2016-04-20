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
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Surface;

public class HdmiInputService extends DroidLogicTvInputService {
    private static final String TAG = HdmiInputService.class.getSimpleName();
    private HdmiInputSession mSession;

    @Override
    public Session onCreateSession(String inputId) {
        super.onCreateSession(inputId);
        if (mSession == null || !TextUtils.equals(inputId, mSession.getInputId())) {
            mSession = new HdmiInputSession(getApplicationContext(), inputId, getHardwareDeviceId(inputId));
            registerInputSession(mSession);
        }
        return mSession;
    }

    public class HdmiInputSession extends TvInputBaseSession {
        public HdmiInputSession(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);
            Utils.logd(TAG, "=====new HdmiInputSession=====");
        }

        @Override
        public void doRelease() {
            super.doRelease();
            if (mSession != null && mSession.getDeviceId() == getDeviceId()) {
                mSession = null;
            }
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
            if (surface == null && getHardware() == null) {//device has been released
                mSession = null;
            }
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (isNavigationKey(keyCode)) {
                getHardware().dispatchKeyEventToHdmi(event);
                return true;
            }
            return false;
        }
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_HDMI
            || hasInfoExisted(hardwareInfo))
            return null;

        Utils.logd(TAG, "=====onHardwareAdded=====" + hardwareInfo.getDeviceId());

        TvInputInfo info = null;
        ResolveInfo rInfo = getResolveInfo(HdmiInputService.class.getName());
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
        if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_HDMI
            || !hasInfoExisted(hardwareInfo))
            return null;

        TvInputInfo info = getTvInputInfo(hardwareInfo);
        String id = null;
        if (info != null)
            id = info.getId();
        updateInfoListIfNeededLocked(hardwareInfo, info, true);

        if (mSession != null && hardwareInfo.getDeviceId() == mSession.getDeviceId()) {
            mSession = null;
        }

        Utils.logd(TAG, "=====onHardwareRemoved=====" + id);
        return id;
    }

}
