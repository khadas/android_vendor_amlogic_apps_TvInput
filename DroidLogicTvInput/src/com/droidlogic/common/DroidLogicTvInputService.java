package com.droidlogic.common;


import com.droidlogic.utils.InputUtils;
import com.droidlogic.utils.Utils;

import android.content.pm.ResolveInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputService;
import android.util.SparseArray;

public class DroidLogicTvInputService extends TvInputService {
    private static final String TAG = DroidLogicTvInputService.class.getSimpleName();

    private SparseArray<TvInputInfo> mInfoList = new SparseArray<>();

    @Override
    public Session onCreateSession(String inputId) {
        // TODO Auto-generated method stub
        return null;
    }

    protected ResolveInfo getResolveInfo(String name) {
        InputUtils iu = new InputUtils(getApplicationContext());
        return iu.getResolveInfo(name);
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

}
