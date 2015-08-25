package com.droidlogic.tv;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements OnClickListener{
    private static final String TAG = "DroidLogicTVSource_MainActivity";
    private static final boolean DEBUG = true;

    private TvView mSourceView;
    private TvInputManager mTvInputManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
    }

    private void init(){
        mSourceView = (TvView) findViewById(R.id.source_view);

        Button atv = (Button) findViewById(R.id.atv);
        atv.setOnClickListener(this);
        Button dtv = (Button) findViewById(R.id.dtv);
        dtv.setOnClickListener(this);
        Button av = (Button) findViewById(R.id.av);
        av.setOnClickListener(this);
        Button hdmi1 = (Button) findViewById(R.id.hdmi1);
        hdmi1.setOnClickListener(this);

        mTvInputManager = (TvInputManager)getSystemService(Context.TV_INPUT_SERVICE);
        Utils.mTvInputManager = mTvInputManager;

        switchToDefaultSource();
    }

    /**
     * get the default source input at first time.
     */
    private int getDefaultSource(){
        return Utils.SOURCE_TYPE_HDMI1;
    }

    private void switchToDefaultSource(){
        int ds = getDefaultSource();
        switch (ds) {
            case Utils.SOURCE_TYPE_ATV:
                break;
            case Utils.SOURCE_TYPE_COMPONENT:
                break;
            case Utils.SOURCE_TYPE_HDMI1:
                switchToHdmi1();
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.atv:
                break;
            case R.id.dtv:
                break;
            case R.id.av:
                switchToAv();
                break;
            case R.id.hdmi1:
                switchToHdmi1();
                break;
            default:
                break;
        }
    }

    private void switchToHdmi1() {
        if (DEBUG)
            Log.d(TAG, "======switchToHdmi1=======");
        String input_id = Utils.getInputId(Utils.SOURCE_TYPE_HDMI1);
        Log.d(TAG, "Input id switching to is " + input_id);
        Uri channelUri = TvContract.buildChannelUriForPassthroughInput(input_id);
        Log.d(TAG, "channelUri switching to is " + channelUri);
        mSourceView.tune(input_id, channelUri);
    }

    private void switchToAv() {
        if (DEBUG)
            Log.d(TAG, "======switchToAv=======");
        String input_id = Utils.getInputId(Utils.SOURCE_TYPE_COMPONENT);
        Log.d(TAG, "Input id switching to is " + input_id);
        Uri channelUri = TvContract.buildChannelUriForPassthroughInput(input_id);
        Log.d(TAG, "channelUri switching to is " + channelUri);

        mSourceView.tune(input_id, channelUri);
    }
}
