package com.droidlogic.tvinput.services;


import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import com.droidlogic.common.DroidLogicTvInputService;
import com.droidlogic.utils.InputUtils;
import com.droidlogic.utils.Utils;

import android.amlogic.Tv;
import android.amlogic.Tv.SourceInput;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

public class HdmiInputService extends DroidLogicTvInputService {
    private static final String TAG = HdmiInputService.class.getSimpleName();
    private static boolean DEBUG = true;

    @Override
    public Session onCreateSession(String inputId) {
        if (DEBUG)
            Log.d(TAG, "=====onCreateSession====");
        return new HdmiInputSession(getApplicationContext());
    }

    public class HdmiInputSession extends TvInputService.Session {
        public HdmiInputSession(Context context) {
            super(context);
        }

        @Override
        public void onRelease() {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void onSetStreamVolume(float volume) {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean onTune(Uri channelUri) {
            if (DEBUG)
                Log.d(TAG, "====onTune====");
            switchToSourceInput(Utils.SOURCE_HDMI1);
            return false;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            // TODO Auto-generated method stub
        }
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        if (hardwareInfo.getType() != TvInputHardwareInfo.TV_INPUT_TYPE_HDMI)
            return null;

        if (DEBUG)
            Log.d(TAG, "=====onHardwareAdded====="+hardwareInfo.getDeviceId());

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

        return info;
    }

}
