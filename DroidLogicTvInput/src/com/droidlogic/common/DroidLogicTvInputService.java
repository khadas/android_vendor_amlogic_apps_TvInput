package com.droidlogic.common;


import com.droidlogic.tvinput.R;
import com.droidlogic.utils.InputUtils;
import com.droidlogic.utils.Utils;

import android.amlogic.Tv;
import android.amlogic.Tv.SourceInput;
import android.content.pm.ResolveInfo;
import android.media.tv.TvInputService;
import android.widget.Toast;

public class DroidLogicTvInputService extends TvInputService {
    private static final String TAG = DroidLogicTvInputService.class.getSimpleName();

    private static Tv tv;

    private Tv getTvInstance(){
        synchronized (tv) {
            if (tv == null)
                tv = Tv.open();
        }
        return tv;
    }

    protected void switchToSourceInput(int source) {
        getTvInstance();
        switch (source) {
            case Utils.SOURCE_TV:
                tv.SetSourceInput(SourceInput.TV);
                break;
            case Utils.SOURCE_AV1:
                tv.SetSourceInput(SourceInput.AV1);
                break;
            case Utils.SOURCE_HDMI1:
                tv.SetSourceInput(SourceInput.HDMI1);
                break;
            default:
                Toast.makeText(getApplicationContext(),
                        R.string.source_input_error, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public Session onCreateSession(String inputId) {
        // TODO Auto-generated method stub
        return null;
    }

    protected ResolveInfo getResolveInfo(String name) {
        InputUtils iu = new InputUtils(getApplicationContext());
        return iu.getResolveInfo(name);
    }

}
