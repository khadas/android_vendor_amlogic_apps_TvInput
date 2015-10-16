package com.droidlogic.tvinput.shortcut;

import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.tvclient.TvClient;

import android.app.Activity;
import android.os.Bundle;

import android.amlogic.Tv;

public class ShortCutActivity extends Activity {
    private Tv tv = Tv.open();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        switch (getIntent().getIntExtra(DroidLogicTvUtils.EXTRA_KEY_CODE, 0)) {
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE:
                //tv.SetDisplayMode
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_3DMODE:
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE:
                //tv.SetPQMode
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE:
                //tv.SetAudioSoundMode
                //tv.SaveCurAudioSoundMode
                break;
            case DroidLogicKeyEvent.KEYCODE_TV_SLEEP:
                //ui
                break;
            case DroidLogicKeyEvent.KEYCODE_MEDIA_AUDIO_CONTROL:
                //TvContractUtils.updateChannelInfo
                break;
            case DroidLogicKeyEvent.KEYCODE_GUIDE:
                break;
            default:
                break;
        }
    }
}
