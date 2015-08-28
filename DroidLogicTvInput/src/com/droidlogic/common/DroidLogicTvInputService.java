package com.droidlogic.common;


import java.util.List;

import com.droidlogic.tvinput.R;
import com.droidlogic.utils.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputService;
import android.text.TextUtils;
import android.util.SparseArray;

public class DroidLogicTvInputService extends TvInputService {
    private static final String TAG = DroidLogicTvInputService.class.getSimpleName();

    private static final int SOURCE_AV1 = 1;
    private static final int SOURCE_HDMI1 = 5;
    private static final int SOURCE_HDMI2 = 6;
    private static final int SOURCE_HDMI3 = 7;

    private SparseArray<TvInputInfo> mInfoList = new SparseArray<>();

    private Session mSession;

    @Override
    public Session onCreateSession(String inputId) {
        // TODO Auto-generated method stub
        return null;
    }

    protected void registerInputSession(Session session){
        mSession = session;
    }

    protected void updateInfoListIfNeededLocked(TvInputHardwareInfo hInfo,
            TvInputInfo info, boolean isRemoved) {
        if (isRemoved) {
            mInfoList.removeAt(hInfo.getDeviceId());
        }else {
            mInfoList.put(hInfo.getDeviceId(), info);
        }
        Utils.logd(TAG, "====sieze of mInfoList is " + mInfoList.size());
    }

    protected TvInputInfo getTvInputInfo(TvInputHardwareInfo hardwareInfo) {
        return mInfoList.get(hardwareInfo.getDeviceId());
    }

    protected int getHardwareDeviceId(String input_id) {
        int id = 0;
        for (int i=0; i<mInfoList.size(); i++) {
            if (input_id.equals(mInfoList.valueAt(i).getId())) {
                id = mInfoList.keyAt(i);
                break;
            }
        }
        Utils.logd(TAG, "====device id is " + id);
        return id;
    }

    protected String getTvInputInfoLabel(int device_id) {
        String label = null;
        String[] labels = getResources().getStringArray(R.array.config_label_name);
        switch (device_id) {
            case SOURCE_AV1:
                label = labels[SOURCE_AV1];
                break;
            case SOURCE_HDMI1:
                label = labels[SOURCE_HDMI1];
                break;
            case SOURCE_HDMI2:
                label = labels[SOURCE_HDMI2];
                break;
            case SOURCE_HDMI3:
                label = labels[SOURCE_HDMI3];
                break;
            default:
                break;
        }
        return label;
    }

    protected ResolveInfo getResolveInfo(String cls_name) {
        if (TextUtils.isEmpty(cls_name))
            return null;
        ResolveInfo ret_ri = null;
        Context context = getApplicationContext();

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(TvInputService.SERVICE_INTERFACE),
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);

        for (ResolveInfo ri: services) {
            ServiceInfo si = ri.serviceInfo;
            if (!android.Manifest.permission.BIND_TV_INPUT.equals(si.permission)) {
                continue;
            }
            Utils.logd(TAG, "===cls_name = " + cls_name + ", si.name = " + si.name);
            if (cls_name.equals(si.name)) {
                ret_ri = ri;
                break;
            }
        }
        return ret_ri;
    }

}
