package com.droidlogic.utils;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputService;
import android.util.Log;

public class InputUtils {
    private static final String TAG = "InputUtils";
    private static final boolean DEBUG = true;

    private Context mContext;

    public InputUtils(Context context){
        mContext = context;
    }

    public ResolveInfo getResolveInfo(String cls_name) {
        if (cls_name == null || cls_name.isEmpty())
            return null;
        ResolveInfo ret_ri = null;

        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(TvInputService.SERVICE_INTERFACE),
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);

        for (ResolveInfo ri: services) {
            ServiceInfo si = ri.serviceInfo;
            if (!android.Manifest.permission.BIND_TV_INPUT.equals(si.permission)) {
                continue;
            }
            if (DEBUG)
                Log.d(TAG, "===cls_name = " + cls_name + ", si.name = " + si.name);
            if (cls_name.equals(si.name)) {
                ret_ri = ri;
                break;
            }
        }
        return ret_ri;
    }
}
