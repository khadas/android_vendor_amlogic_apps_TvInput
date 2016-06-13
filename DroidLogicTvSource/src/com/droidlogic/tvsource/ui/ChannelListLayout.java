package com.droidlogic.tvsource.ui;

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.tvsource.R;
import com.droidlogic.tvsource.Utils;
import com.droidlogic.app.tv.DroidLogicTvUtils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.provider.Settings;

public class ChannelListLayout extends LinearLayout implements OnItemClickListener, OnFocusChangeListener, OnItemSelectedListener{
    private static final String TAG = "ChannelListLayout";
    private Context mContext;
    private SourceButton mSourceInput;
    private int mType;
    private boolean modeVideo = true;//true:first tab

    private TextView title;
    private TextView txVideo;
    private TextView txRadio;
    private ListView mListView;
    private MyAdapter mAdapter;
    SparseArray<ChannelInfo> videoList;
    SparseArray<ChannelInfo> radioList;
    private int mPosition = -1;
    private Resources mResources;

    private OnChannelSelectListener mListener;

    public ChannelListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mResources = getResources();
        init();
    }

    private void init() {
        inflate(mContext, R.layout.channel_list, this);
        title = (TextView)findViewById(R.id.tx_title);
        txVideo = (TextView)findViewById(R.id.tx_video);
        txRadio = (TextView)findViewById(R.id.tx_radio);
        txVideo.setOnFocusChangeListener(this);
        txRadio.setOnFocusChangeListener(this);
        mListView = (ListView)findViewById(R.id.channel_video_list);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemSelectedListener(this);
        mListView.setNextFocusUpId(mListView.getId());
        mListView.setNextFocusDownId(mListView.getId());
    }

    public void initView(int type, SourceButton sourceInput) {
        Utils.logd(TAG, "==== init list, type =" + type);
        mSourceInput = sourceInput;
        mType = type;
        modeVideo = true;
        initTab();

        videoList = sourceInput.getChannelVideoList();
        radioList = sourceInput.getChannelRadioList();
        initList();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ChannelInfo channel = (ChannelInfo)mAdapter.getItem(position);
        if (channel != null) {
            mListener.onSelect(channel.getNumber(), !modeVideo);
        }
    }

    private SparseArray<ChannelInfo> getFavList(SparseArray<ChannelInfo> list) {
        SparseArray<ChannelInfo> favList = new SparseArray<ChannelInfo>();
        for (int i = 0; i < list.size(); i++) {
            ChannelInfo info = list.valueAt(i);
            if (info.isFavourite())
                favList.put(info.getNumber(), info);
        }
        return favList;
    }

    private int getChannelIndex (ChannelInfo channel) {
        if (ChannelInfo.isRadioChannel(channel)) {
            for (int i = 0; i < radioList.size(); i++) {
                if (ChannelInfo.isSameChannel(channel, radioList.get(i)))
                    return i;
            }
            return -1;
        } else {
            for (int i = 0; i < videoList.size(); i++) {
                if (ChannelInfo.isSameChannel(channel, videoList.get(i)))
                    return i;
            }
            return -1;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Utils.logd(TAG, "==== event.keycode =" + event.getKeyCode());
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mListView.hasFocus() && mPosition != -1
                        && mPosition >= mListView.getAdapter().getCount() -1) {
                    mListView.setSelection(0);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mListView.hasFocus() && mPosition == 0) {
                    mListView.setSelection(mListView.getAdapter().getCount() -1);
                }
                break;
            default:
                break;
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onFocusChange (View v, boolean hasFocus) {
        if (hasFocus) {
            switch (v.getId()) {
                case R.id.tx_video:
                    switchToChannelList(true);
                    break;
                case R.id.tx_radio:
                    switchToChannelList(false);
                    break;
            }
        }
    }

    private void switchToChannelList(boolean isVideo) {
        if (mType == Utils.UI_TYPE_ATV_FAV_LIST || mType == Utils.UI_TYPE_ATV_CHANNEL_LIST)
            return;
        modeVideo = isVideo;
        initTab();
        initList();
    }

    private void initList() {
        mListView.setAdapter(null);
        switch (mType) {
            case Utils.UI_TYPE_ATV_CHANNEL_LIST:
            case Utils.UI_TYPE_ATV_FAV_LIST:
                mAdapter = new MyAdapter(mContext, mType == Utils.UI_TYPE_ATV_FAV_LIST ? getFavList(videoList) : videoList);
                mListView.setAdapter(mAdapter);
                break;
            case Utils.UI_TYPE_DTV_CHANNEL_LIST:
            case Utils.UI_TYPE_DTV_FAV_LIST:
                if (modeVideo) {
                    mAdapter = new MyAdapter(mContext, mType == Utils.UI_TYPE_DTV_FAV_LIST ? getFavList(videoList) : videoList);
                } else {
                    mAdapter = new MyAdapter(mContext, mType == Utils.UI_TYPE_DTV_FAV_LIST ? getFavList(radioList) : radioList);
                }
                mListView.setAdapter(mAdapter);
                break;
            default:
                break;
        }

        int current_index = mSourceInput.getChannelIndex();
        if (modeVideo == !mSourceInput.isRadioChannel() && current_index != -1)
            mListView.setSelection(current_index);
    }

    private void initTab() {
        switch (mType) {
            case Utils.UI_TYPE_ATV_FAV_LIST:
                title.setText(mResources.getString(R.string.fav_list));
                txRadio.setVisibility(INVISIBLE);
                break;
            case Utils.UI_TYPE_ATV_CHANNEL_LIST:
                title.setText(mResources.getString(R.string.channel_list));
                txRadio.setVisibility(INVISIBLE);
                txVideo.requestFocus();
                break;
            case Utils.UI_TYPE_DTV_FAV_LIST:
                title.setText(mResources.getString(R.string.fav_list));
                txRadio.setVisibility(VISIBLE);
                break;
            case Utils.UI_TYPE_DTV_CHANNEL_LIST:
                title.setText(mResources.getString(R.string.channel_list));
                txRadio.setVisibility(VISIBLE);
                break;
            default:
                break;
        }
    }

    public int getType() {
        return mType;
    }

    public interface OnChannelSelectListener {
        void onSelect(int channelNum, boolean isRadio);
    }

    public void setOnChannelSelectListener (OnChannelSelectListener listener) {
        mListener = listener;
    }

    private class MyAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private SparseArray<ChannelInfo> mList;

        private MyAdapter(Context context,SparseArray<ChannelInfo> list) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.valueAt(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.channel_list_item, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            ChannelInfo channel = (ChannelInfo)getItem(position);
            Utils.logd(TAG, "==== position =" + position + ", count =" + getCount());
            if (channel == null)
                return null;
            Utils.logd(TAG, "==== number =" + channel.getNumber());
            Utils.logd(TAG, "==== name =" + channel.getDisplayNameLocal());

            if (ChannelInfo.isSameChannel(channel, mSourceInput.getChannelInfo())) {
                holder.img_playing.setImageDrawable(mResources.getDrawable(R.drawable.icon_select));
            } else {
                holder.img_playing.setImageDrawable(null);
            }

            String name = channel.getDisplayNumber();

            name += "  " + channel.getDisplayNameLocal();
            holder.tx_channel.setText(name);
            return convertView;
        }
    }

    private class ViewHolder {
        public TextView tx_channel;
        public ImageView img_playing;

        public ViewHolder(View view) {
            tx_channel = (TextView)view.findViewById(R.id.tx_channel);
            img_playing = (ImageView)view.findViewById(R.id.img_playing);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mPosition = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

}
