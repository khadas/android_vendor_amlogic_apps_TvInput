package com.droidlogic.utils.tunerinput.tvutil;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputService;

import java.util.List;

public class TVMisc {

	public static ResolveInfo getTvIntputResolveInfo(Context context, String className)
	{
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> services = pm.queryIntentServices(
				new Intent(TvInputService.SERVICE_INTERFACE),
				PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);

		for (ResolveInfo ri: services) {
			ServiceInfo si = ri.serviceInfo;
            if (android.Manifest.permission.BIND_TV_INPUT.equals(si.permission)
					&& className.equals(si.name)) {
                return ri;
            }
		}
		return null;
	}

}
