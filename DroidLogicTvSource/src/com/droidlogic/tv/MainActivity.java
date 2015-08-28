package com.droidlogic.tv;


import android.app.Activity;
import android.content.Context;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.media.tv.TvView.TvInputCallback;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener{
    private static final String TAG = "DroidLogicTVSource_MainActivity";

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
        mSourceView.setCallback(new DroidLogicInputCallback());

        Button atv = (Button) findViewById(R.id.atv);
        atv.setOnClickListener(this);
        Button dtv = (Button) findViewById(R.id.dtv);
        dtv.setOnClickListener(this);
        Button av = (Button) findViewById(R.id.av);
        av.setOnClickListener(this);
        Button hdmi1 = (Button) findViewById(R.id.hdmi1);
        hdmi1.setOnClickListener(this);
        Button hdmi2 = (Button) findViewById(R.id.hdmi2);
        hdmi2.setOnClickListener(this);

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
                switchToSourceInput(Utils.SOURCE_TYPE_COMPONENT);
                break;
            case Utils.SOURCE_TYPE_HDMI1:
                switchToSourceInput(Utils.SOURCE_TYPE_HDMI1);
                break;
            case Utils.SOURCE_TYPE_HDMI2:
                switchToSourceInput(Utils.SOURCE_TYPE_HDMI2);
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
                switchToSourceInput(Utils.SOURCE_TYPE_COMPONENT);
                break;
            case R.id.hdmi1:
                switchToSourceInput(Utils.SOURCE_TYPE_HDMI1);
                break;
            case R.id.hdmi2:
                switchToSourceInput(Utils.SOURCE_TYPE_HDMI2);
                break;
            default:
                break;
        }
    }

    private void switchToSourceInput(int source_type) {
        Utils.logd(TAG, "======switchToSourceInput=======");
        String input_id = Utils.getInputId(source_type);
        Utils.logd(TAG, "Input id switching to is " + input_id);
        Uri channelUri = TvContract.buildChannelUriForPassthroughInput(input_id);
        Utils.logd(TAG, "channelUri switching to is " + channelUri);
        mSourceView.tune(input_id, channelUri);
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mSourceView.reset();
        super.onDestroy();
    }

    public class DroidLogicInputCallback extends TvInputCallback {

        @Override
        public void onEvent(String inputId, String eventType, Bundle eventArgs) {
            Utils.logd(TAG, "====onEvent==inputId =" + inputId +", ===eventType ="+ eventType);
        }

        @Override
        public void onVideoAvailable(String inputId) {
            Utils.logd(TAG, "====onVideoAvailable==inputId =" + inputId);
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            Utils.logd(TAG, "====onVideoUnavailable==inputId =" + inputId +", ===reason ="+ reason);
        }

    }

}
