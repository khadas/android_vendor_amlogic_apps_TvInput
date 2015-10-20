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

public class OptionListLayout implements OnItemClickListener{
    private static final String TAG = "OptionListLayout";

    private Context mContext;
    private SettingsManager mSettingsManager;
    private int mTag = -1;
    private View optionView = null;

    public OptionListLayout (Context context, View view, int tag) {
        mContext = context;
        mSettingsManager = ((TvSettingsActivity)mContext).getSettingsManager();
        optionView = view;
        mTag = tag;

        initOptionListView();
    }

    private void initOptionListView () {
        SimpleAdapter optionAdapter = null;
        ArrayList<HashMap<String,Object>> optionListData = null;
        TextView title = (TextView)optionView.findViewById(R.id.option_title);
        OptionListView optionListView = (OptionListView)optionView.findViewById(R.id.option_list);

        switch (mTag) {
            case OptionUiManager.OPTION_CHANNEL_INFO:
                title.setText(mContext.getResources().getString(R.string.channel_info));
                optionListData = ((TvSettingsActivity)mContext).getSettingsManager().getChannelInfo();
                optionAdapter = new SimpleAdapter(mContext, optionListData,
                    R.layout.layout_option_double_text,
                    new String[] {SettingsManager.STRING_NAME, SettingsManager.STRING_STATUS},
                    new int[] {R.id.text_name, R.id.text_status});
                break;
            case OptionUiManager.OPTION_AUDIO_TRACK:
                title.setText(mContext.getResources().getString(R.string.audio_track));
                optionListData = ((TvSettingsActivity)mContext).getSettingsManager().getAudioTrackList();
                optionAdapter = new SimpleAdapter(mContext, optionListData,
                    R.layout.layout_option_single_text,
                    new String[]{SettingsManager.STRING_NAME}, new int[]{R.id.text_name});
                break;
            case OptionUiManager.OPTION_SOUND_CHANNEL:
                title.setText(mContext.getResources().getString(R.string.sound_channel));
                optionListData = ((TvSettingsActivity)mContext).getSettingsManager().getSoundChannelList();
                optionAdapter = new SimpleAdapter(mContext, optionListData,
                    R.layout.layout_option_single_text,
                    new String[]{SettingsManager.STRING_NAME}, new int[]{R.id.text_name});
                break;
        }
        if (optionAdapter != null) {
            optionListView.setAdapter(optionAdapter);
            optionListView.setOnItemClickListener(this);
        }
    }

    public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
        switch (mTag) {
            case OptionUiManager.OPTION_AUDIO_TRACK:
                break;
            case OptionUiManager.OPTION_SOUND_CHANNEL:
                mSettingsManager.setSoundChannel(position);
                break;
        }
    }
}
