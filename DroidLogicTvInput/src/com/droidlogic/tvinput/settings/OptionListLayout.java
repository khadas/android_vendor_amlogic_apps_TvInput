package com.droidlogic.tvinput.settings;

import android.app.Activity;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import com.droidlogic.tvinput.R;

public class OptionListLayout {
    private static final String TAG = "OptionListLayout";

    private Context mContext;
    private SettingsManager mSettingsManager;
    private int mTag = -1;
    private View optionView = null;
    private OptionListView optionListView;
    SimpleAdapter optionAdapter;
    ArrayList<HashMap<String,Object>> optionListData;

    public OptionListLayout (Context context, View view, int tag) {
        mContext = context;
        mSettingsManager = ((TvSettingsActivity)mContext).getSettingsManager();
        optionView = view;
        mTag = tag;

        initOptionListView();
    }

    private void initOptionListView () {
        optionListView = (OptionListView)optionView.findViewById(R.id.option_list);

        switch (mTag) {
            case OptionUiManager.OPTION_CHANNEL_INFO:
                optionListData = ((TvSettingsActivity)mContext).getSettingsManager().getChannelInfo();
                optionAdapter = new SimpleAdapter(mContext, optionListData,
                    R.layout.layout_option_double_text,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
                break;
            case OptionUiManager.OPTION_AUDIO_TRACK:
                optionListData = ((TvSettingsActivity)mContext).getSettingsManager().getAudioTrackList();
                optionAdapter = new SimpleAdapter(mContext, optionListData,
                    R.layout.layout_option_single_text,
                    new String[]{SettingsManager.STRING_NAME}, new int[]{R.id.text_name});
                break;
            case OptionUiManager.OPTION_SOUND_CHANNEL:
                optionListData = ((TvSettingsActivity)mContext).getSettingsManager().getSoundChannelList();
                optionAdapter = new SimpleAdapter(mContext, optionListData,
                    R.layout.layout_option_single_text,
                    new String[]{SettingsManager.STRING_NAME}, new int[]{R.id.text_name});
                break;
        }
        optionListView.setAdapter(optionAdapter);
        optionListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView<?> parent, View view, int position, long id) {

            }
        });
    }
}
