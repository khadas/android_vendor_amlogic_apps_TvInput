package com.droidlogic.tvinput.settings;

import android.app.Activity;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.Color;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import com.droidlogic.tvinput.R;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvControlManager;

public class ChannelEdit implements OnClickListener, OnFocusChangeListener, OnItemClickListener {
    private static final String TAG = "ChannelEdit";

    public static final int TYPE_ATV                           = 0;
    public static final int TYPE_DTV_TV                        = 1;
    public static final int TYPE_DTV_RADIO                     = 2;

    private static final int ACTION_INITIAL_STATE             = -1;
    private static final int ACTION_SHOW_LIST                 = 0;
    private static final int ACTION_SHOW_OPERATIONS          = 1;
    private static final int ACTION_OPERATIONS_EDIT          = 2;
    private static final int ACTION_OPERATIONS_SWAP          = 3;
    private static final int ACTION_OPERATIONS_MOVE          = 4;
    private static final int ACTION_OPERATIONS_SKIP          = 5;
    private static final int ACTION_OPERATIONS_DELETE        = 6;
    private static final int ACTION_OPERATIONS_FAVOURITE     = 7;

    private Context mContext;
    private SettingsManager mSettingsManager;
    private OptionUiManager mOptionUiManager;
    private View channelEditView = null;
    private TextView button_tv = null;
    private TextView button_radio = null;
    private OptionListView channelListView;
    private ViewGroup operationsView;
    private ViewGroup operationsEditView;
    SimpleAdapter ChannelAdapter;
    ArrayList<HashMap<String,Object>> ChannelListData;

    private int channelType = TYPE_ATV;
    private int currentChannelPosition = ACTION_INITIAL_STATE;
    private int needOperateChannelPosition = ACTION_INITIAL_STATE;
    private int currentOperation = ACTION_INITIAL_STATE;


    public ChannelEdit (Context context, View view) {
        mContext = context;
        mSettingsManager = ((TvSettingsActivity)mContext).getSettingsManager();
        mOptionUiManager = ((TvSettingsActivity)mContext).getOptionUiManager();
        channelEditView = view;

        initChannelEditView();

    }

    private void initChannelEditView () {
        button_tv = (TextView)channelEditView.findViewById(R.id.channel_edit_tv);
        button_radio = (TextView)channelEditView.findViewById(R.id.channel_edit_radio);
        button_tv.setOnClickListener(this);
        button_tv.setOnFocusChangeListener(this);
        button_radio.setOnClickListener(this);
        button_radio.setOnFocusChangeListener(this);

        channelListView = (OptionListView)channelEditView.findViewById(R.id.channnel_edit_list);
        operationsView = (ViewGroup)channelEditView.findViewById(R.id.channel_edit_operations);
        operationsEditView = (ViewGroup)channelEditView.findViewById(R.id.channel_edit_editname);

        if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV)
            channelType = TYPE_ATV;
        else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV)
            channelType = TYPE_DTV_TV;
        ChannelListData = ((TvSettingsActivity)mContext).getSettingsManager().getChannelList(channelType);

        ChannelAdapter = new SimpleAdapter(mContext, ChannelListData,
            R.layout.layout_option_icon_text,
            new String[]{SettingsManager.STRING_ICON, SettingsManager.STRING_NAME}, new int[]{R.id.image_icon, R.id.text_name});
        channelListView.setAdapter(ChannelAdapter);
        if (!ChannelListData.get(0).get(SettingsManager.STRING_NAME).toString()
            .equals(mContext.getResources().getString(R.string.error_no_channel)))
            channelListView.setOnItemClickListener(this);

        if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_TV) {
            button_tv.setVisibility(View.GONE);
            button_radio.setVisibility(View.GONE);
            operationsView.setVisibility(View.GONE);
            operationsEditView.setVisibility(View.GONE);
        } else if (mSettingsManager.getCurentTvSource() == TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV) {
            channelListView.setVisibility(View.GONE);
            operationsView.setVisibility(View.GONE);
            operationsEditView.setVisibility(View.GONE);
        }

        TextView edit = (TextView)channelEditView.findViewById(R.id.edit);
        TextView ensure_edit = (TextView)channelEditView.findViewById(R.id.ensure_edit);
        TextView swap = (TextView)channelEditView.findViewById(R.id.swap);
        TextView move = (TextView)channelEditView.findViewById(R.id.move);
        TextView skip = (TextView)channelEditView.findViewById(R.id.skip);
        TextView delete = (TextView)channelEditView.findViewById(R.id.delete);
        TextView favourite = (TextView)channelEditView.findViewById(R.id.favourite);
        edit.setOnClickListener(this);
        edit.setOnFocusChangeListener(this);
        ensure_edit.setOnClickListener(this);
        ensure_edit.setOnFocusChangeListener(this);
        swap.setOnClickListener(this);
        swap.setOnFocusChangeListener(this);
        move.setOnClickListener(this);
        move.setOnFocusChangeListener(this);
        skip.setOnClickListener(this);
        skip.setOnFocusChangeListener(this);
        delete.setOnClickListener(this);
        delete.setOnFocusChangeListener(this);
        favourite.setOnClickListener(this);
        favourite.setOnFocusChangeListener(this);
    }

    private void showDtvMainView () {
        button_tv.setVisibility(View.VISIBLE);
        button_tv.requestFocus();
        button_radio.setVisibility(View.VISIBLE);

        channelListView.setVisibility(View.GONE);
        operationsView.setVisibility(View.GONE);
        operationsEditView.setVisibility(View.GONE);
    }

    private void showListView () {
        if (currentOperation != ACTION_INITIAL_STATE) {
            channelListView.setSelector(R.anim.flicker_background);
            AnimationDrawable animaition = (AnimationDrawable)channelListView.getSelector();
            animaition.start();
        }

        freshChannelList();
        channelListView.setVisibility(View.VISIBLE);
        channelListView.requestFocus();
        channelListView.setNextFocusLeftId(R.id.content_list);

        button_tv.setVisibility(View.GONE);
        button_radio.setVisibility(View.GONE);
        operationsView.setVisibility(View.GONE);
        operationsEditView.setVisibility(View.GONE);
    }

    private void showOperationsView () {
        operationsView.setVisibility(View.VISIBLE);
        TextView edit = (TextView)channelEditView.findViewById(R.id.edit);
        edit.requestFocus();
        channelListView.setVisibility(View.GONE);
        setFocus(operationsView);
    }

    private void showEditView () {
        operationsEditView.setVisibility(View.VISIBLE);
        EditText edit_name = (EditText)channelEditView.findViewById(R.id.edit_name);
        edit_name.requestFocus();
        operationsView.setVisibility(View.GONE);
        setFocus(operationsEditView);
    }

    private void setFocus(View view) {
        View firstFocusableChild = null;
        View lastFocusableChild = null;
        for (int i = 0; i < ((ViewGroup)view).getChildCount(); i++) {
            View child = ((ViewGroup)view).getChildAt(i);
            if (child != null && child.hasFocusable() ) {
                if (firstFocusableChild == null) {
                    firstFocusableChild = child;
                }
                child.setNextFocusLeftId(R.id.content_list);
                lastFocusableChild = child;
            }
        }

        if (firstFocusableChild != null && lastFocusableChild != null) {
            firstFocusableChild.setNextFocusUpId(firstFocusableChild.getId());
            lastFocusableChild.setNextFocusDownId(lastFocusableChild.getId());
        }
    }

    private void setChannelName () {
        EditText edit_name = (EditText)channelEditView.findViewById(R.id.edit_name);
        mSettingsManager.setChannelName(channelType, currentChannelPosition, edit_name.getText().toString());
    }

    private void swapChannelPosition () {
        mSettingsManager.swapChannelPosition(channelType, needOperateChannelPosition, currentChannelPosition);
    }

    private void moveChannelPosition () {
        mSettingsManager.moveChannelPosition(channelType, needOperateChannelPosition, currentChannelPosition);
    }

    private void skipChannel () {
        mSettingsManager.skipChannel(channelType, currentChannelPosition);
    }

    private void deleteChannel () {
        mSettingsManager.deleteChannel(channelType, currentChannelPosition);
    }

    private void setFavouriteChannel () {
        mSettingsManager.setFavouriteChannel(channelType, currentChannelPosition);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.channel_edit_tv:
                channelType = TYPE_DTV_TV;
                showListView();
                break;
            case R.id.channel_edit_radio:
                channelType = TYPE_DTV_RADIO;
                showListView();
                break;
            case R.id.edit:
                showEditView();
                break;
            case R.id.ensure_edit:
                setChannelName();
                showListView();
                break;
            case R.id.swap:
                needOperateChannelPosition = currentChannelPosition;
                currentOperation = ACTION_OPERATIONS_SWAP;
                showListView();
                break;
            case R.id.move:
                needOperateChannelPosition = currentChannelPosition;
                currentOperation = ACTION_OPERATIONS_MOVE;
                showListView();
                break;
            case R.id.skip:
                skipChannel();
                showListView();
                break;
            case R.id.delete:
                deleteChannel();
                showListView();
                break;
            case R.id.favourite:
                setFavouriteChannel();
                showListView();
                break;
        }
    }

    @Override
    public void onFocusChange (View v, boolean hasFocus) {
        if (v instanceof TextView) {
            if (hasFocus) {
                ((TextView)v).setTextColor(mContext.getResources().getColor(R.color.color_text_focused));
            } else
                ((TextView)v).setTextColor(mContext.getResources().getColor(R.color.color_text_item));
        }
    }

    @Override
    public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
        currentChannelPosition = position;
        if (currentOperation == ACTION_INITIAL_STATE) {
            showOperationsView();
        } else {
            if (currentOperation == ACTION_OPERATIONS_SWAP)
                swapChannelPosition();
            else if (currentOperation == ACTION_OPERATIONS_MOVE)
                moveChannelPosition();
                channelListView.setSelector(R.drawable.item_background);
                freshChannelList();
            }
        recoverActionState();
    }

    private void freshChannelList () {
        ArrayList<HashMap<String,Object>> list = null;

        ChannelListData.clear();
        list = ((TvSettingsActivity)mContext).getSettingsManager().getChannelList(channelType);

        if (list != null) {
            ChannelListData.addAll(list);
            ChannelAdapter.notifyDataSetChanged();

            if (!ChannelListData.get(0).get(SettingsManager.STRING_NAME).toString()
                .equals(mContext.getResources().getString(R.string.error_no_channel))) {
                channelListView.setOnItemClickListener(this);
            }else {
                channelListView.setOnItemClickListener(null);
            }
        }
    }

    private void recoverActionState () {
        currentOperation = ACTION_INITIAL_STATE;
        needOperateChannelPosition = ACTION_INITIAL_STATE;
    }
}
