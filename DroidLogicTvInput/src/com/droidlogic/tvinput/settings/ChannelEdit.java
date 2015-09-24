package com.droidlogic.tvinput.settings;

import android.app.Activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
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

public class ChannelEdit {
    private static final String TAG = "ChannelEdit";
    private Context mContext;
    private View channelEditView = null;
    private ListView channelListView;
    private ViewGroup operationsView;

    private static final int ACTION_SHOW_LIST                 = 0;
    private static final int ACTION_SHOW_OPERATIONS          = 1;
    private static final int ACTION_SHOW_EDIT                 = 2;

    public ChannelEdit (Context context, View view) {
        mContext = context;
        channelEditView = view;

        initChannelEditView();

    }

    private void initChannelEditView () {
        channelListView = (ListView)channelEditView.findViewById(R.id.channnel_edit_list);
        operationsView = (ViewGroup)channelEditView.findViewById(R.id.channel_edit_operations);

        ArrayList<HashMap<String,Object>> ChannelListData
            = ((TvSettingsActivity)mContext).getSettingsManager().geChannelEditList();
        SimpleAdapter adapter = new SimpleAdapter(mContext, ChannelListData,
            R.layout.layout_channel_channel_edit_item,
            new String[]{"channel_name"}, new int[]{R.id.channel_name});
        channelListView.setAdapter(adapter);
        channelListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "@@@@@@@@@@@@@@ click");
                Message msg = new Message();
                msg.what = ACTION_SHOW_OPERATIONS;
                msg.arg1 = position;
                mHandler.sendMessage(msg);
            }
        });
    }

    private void showOperationsView (int position) {
        operationsView.setVisibility(View.VISIBLE);
        TextView edit = (TextView)channelEditView.findViewById(R.id.edit);
        edit.requestFocus();
        channelListView.setVisibility(View.GONE);
    }

 private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACTION_SHOW_LIST:
                    break;
                case ACTION_SHOW_OPERATIONS:
                    showOperationsView(msg.arg1);
                    break;
                case ACTION_SHOW_EDIT:
                    break;
                default:
                    break;
            }
        }
    };
}
