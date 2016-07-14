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
import android.hardware.hdmi.HdmiDeviceInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Surface;

public class Hdmi2InputService extends DroidLogicTvInputService {
    private static final String TAG = Hdmi2InputService.class.getSimpleName();
    private Hdmi2InputSession mCurrentSession;
    private int number = 0;
    private int currentNumber = 0;
    private final int TV_SOURCE_EXTERNAL = 0;
    private final int TV_SOURCE_INTERNAL = 1;

    @Override
    public Session onCreateSession(String inputId) {
        super.onCreateSession(inputId);

        mCurrentSession = new Hdmi2InputSession(getApplicationContext(), inputId, getHardwareDeviceId(inputId));
        registerInputSession(mCurrentSession);
        mCurrentSession.setNumber(number);
        number++;

        return mCurrentSession;
    }

    public class Hdmi2InputSession extends TvInputBaseSession {
        public Hdmi2InputSession(Context context, String inputId, int deviceId) {
            super(context, inputId, deviceId);
            Utils.logd(TAG, "=====new HdmiInputSession=====");
        }

        public TvStreamConfig[] getConfigs() {
            return mConfigs;
        }

        public Hardware getHardware() {
            return mHardware;
        }

        public void setCurrentSession() {
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
        if (hardwareInfo.getDeviceId() != DroidLogicTvUtils.DEVICE_ID_HDMI2
            || hasInfoExisted(hardwareInfo))
            return null;

        Utils.logd(TAG, "=====onHardwareAdded=====" + hardwareInfo.getDeviceId());

        TvInputInfo info = null;
        ResolveInfo rInfo = getResolveInfo(Hdmi2InputService.class.getName());
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
        if (hardwareInfo.getDeviceId() != DroidLogicTvUtils.DEVICE_ID_HDMI2
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

    int getPortIndex(int phyAddr) {
        /* TODO: consider of tuner */
        return ((phyAddr & 0xf000) >> 12) - 1;
    }

    @Override
    public TvInputInfo onHdmiDeviceAdded(HdmiDeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return null;
        }
        int phyaddr = deviceInfo.getPhysicalAddress();
        if (getTvInputInfo(phyaddr) != null) {
            Utils.logd(TAG, "onHdmiDeviceAdded, phyaddr:" + phyaddr + " already add");
            return null;
        }
        int sourceType = getPortIndex(phyaddr) + DroidLogicTvUtils.DEVICE_ID_HDMI1;

        String parentId = null;
        TvInputInfo info = null;
        TvInputInfo parent = getTvInputInfo(sourceType);
        if (parent != null) {
            parentId = parent.getId();
        } else {
            Utils.logd(TAG, "onHdmiDeviceAdded, can't found parent");
            return null;
        }
        Utils.logd(TAG, "onHdmiDeviceAdded, phyaddr:" + phyaddr +
                    ", port:" + sourceType + ", parentID:" + parentId);
        ResolveInfo rInfo = getResolveInfo(Hdmi2InputService.class.getName());
        if (rInfo != null) {
            try {
                info = TvInputInfo.createTvInputInfo(
                        getApplicationContext(),
                        rInfo,
                        deviceInfo,
                        parentId,
                        deviceInfo.getDisplayName(),
                        null);
            } catch (XmlPullParserException e) {
                // TODO: handle exception
            }catch (IOException e) {
                // TODO: handle exception
            }
        } else {
            return null;
        }
        Utils.logd(TAG, "createTvInputInfo, id:" + info.getId());
        updateInfoListIfNeededLocked(phyaddr, info, false);
        if (mCurrentSession != null) {
            mCurrentSession.selectHdmiDevice(TV_SOURCE_EXTERNAL);
        }

        return info;
    }

    @Override
    public String onHdmiDeviceRemoved(HdmiDeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return null;
        }
        int phyaddr = deviceInfo.getPhysicalAddress();
        TvInputInfo info = getTvInputInfo(phyaddr);
        if (info == null) {
            return null;
        }
        String id = info.getId();
        Utils.logd(TAG, "onHdmiDeviceRemoved, id:" + id);
        updateInfoListIfNeededLocked(phyaddr, info, true);

        return id;
    }
}
