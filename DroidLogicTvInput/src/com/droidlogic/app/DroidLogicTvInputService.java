package com.droidlogic.app;


import java.util.List;

import android.amlogic.Tv;
import android.amlogic.Tv.tvin_info_t;

import com.droidlogic.tvinput.R;
import com.droidlogic.tvinput.services.AVInputService.AVInputSession;
import com.droidlogic.tvinput.services.HdmiInputService.HdmiInputSession;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

public class DroidLogicTvInputService extends TvInputService implements Tv.SigInfoChangeListener {
    private static final String TAG = DroidLogicTvInputService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int SOURCE_AV1 = 1;
    private static final int SOURCE_HDMI1 = 5;
    private static final int SOURCE_HDMI2 = 6;
    private static final int SOURCE_HDMI3 = 7;

    private SparseArray<TvInputInfo> mInfoList = new SparseArray<>();

    private Session mSession;
    private String mCurrentInputId;

    @Override
    public Session onCreateSession(String inputId) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * get session has been created by {@code onCreateSession}, and input id of session.
     * @param session {@link HdmiInputSession} or {@link AVInputSession}
     * @param inputId input id of {@code session} has created by {@code onCreateSession}
     */
    protected void registerInputSession(Session session, String inputId) {
        mSession = session;
        mCurrentInputId = inputId;
        Tv tv = DroidLogicTvUtils.TvClient.getTvInstance();
        tv.SetSigInfoChangeListener(this);
    }

    /**
     * update {@code mInfoList} when hardware device is added or removed.
     * @param hInfo {@linkHardwareInfo} get from HAL.
     * @param info {@link TvInputInfo} will be added or removed.
     * @param isRemoved {@code true} if you want to remove info. {@code false} otherwise.
     */
    protected void updateInfoListIfNeededLocked(TvInputHardwareInfo hInfo,
            TvInputInfo info, boolean isRemoved) {
        if (isRemoved) {
            mInfoList.removeAt(hInfo.getDeviceId());
        }else {
            mInfoList.put(hInfo.getDeviceId(), info);
        }

        if (DEBUG)
            Log.d(TAG, "====sieze of mInfoList is " + mInfoList.size());
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

        if (DEBUG)
            Log.d(TAG, "====device id is " + id);
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

            if (DEBUG)
                Log.d(TAG, "====cls_name = " + cls_name + ", si.name = " + si.name);

            if (cls_name.equals(si.name)) {
                ret_ri = ri;
                break;
            }
        }
        return ret_ri;
    }

    private String getInfoLabel() {
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        return tim.getTvInputInfo(mCurrentInputId).loadLabel(this).toString();
    }

    @Override
    public void onSigChange(tvin_info_t signal_info) {
        Tv.tvin_sig_status_t status = signal_info.status;

        if (DEBUG)
            Log.d(TAG, "==== onSigChange ====" + status.ordinal() + status.toString());

        if (status == Tv.tvin_sig_status_t.TVIN_SIG_STATUS_NOSIG
                || status == Tv.tvin_sig_status_t.TVIN_SIG_STATUS_NULL
                || status == Tv.tvin_sig_status_t.TVIN_SIG_STATUS_NOTSUP) {
            mSession.notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
        }else if (status == Tv.tvin_sig_status_t.TVIN_SIG_STATUS_STABLE) {
            mSession.notifyVideoAvailable();
            if (mSession instanceof HdmiInputSession) {
                if (DEBUG)
                    Log.d(TAG, "signal_info.fmt.toString() for hdmi=" + signal_info.fmt.toString());

                String[] strings = signal_info.fmt.toString().split("_");
                if (strings[4].equalsIgnoreCase("1440X480I")
                        || strings[4].equalsIgnoreCase("2880X480I")
                        || strings[4].equalsIgnoreCase("720X480I")) {
                    strings[4] = "480I";
                }else if (strings[4].equalsIgnoreCase("1440X576I")
                        || strings[4].equalsIgnoreCase("2880X576I")
                        || strings[4].equalsIgnoreCase("720X576I")) {
                    strings[4] = "576I";
                }
                Bundle bundle = new Bundle();
                bundle.putInt(DroidLogicTvUtils.SIG_INFO_TYPE, DroidLogicTvUtils.SIG_INFO_TYPE_HDMI);
                bundle.putString(DroidLogicTvUtils.SIG_INFO_LABEL, getInfoLabel());
                bundle.putString(DroidLogicTvUtils.SIG_INFO_ARGS, strings[4]
                        + "_" + signal_info.reserved + "HZ");
                mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_EVENT, bundle);
            } else if (mSession instanceof AVInputSession) {
                if (DEBUG)
                    Log.d(TAG, "tmpInfo.fmt.toString() for av=" + signal_info.fmt.toString());

                String[] strings = signal_info.fmt.toString().split("_");
                Bundle bundle = new Bundle();
                bundle.putInt(DroidLogicTvUtils.SIG_INFO_TYPE, DroidLogicTvUtils.SIG_INFO_TYPE_AV);
                bundle.putString(DroidLogicTvUtils.SIG_INFO_LABEL, getInfoLabel());
                bundle.putString(DroidLogicTvUtils.SIG_INFO_ARGS, strings[4]);
                mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_EVENT, bundle);
            }
        }
    }

}
