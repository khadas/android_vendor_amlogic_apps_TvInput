package com.droidlogic.tvsource;

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.tvsource.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ChannelListLayout extends LinearLayout implements OnItemClickListener {
    private static final String TAG = "ChannelListLayout";
    private Context mContext;
    private int mType;
    private boolean tabFlag = true;//true:first tab

    private TextView tabAtv;
    private LinearLayout tabDtv;
    private TextView tabDtvv;
    private TextView tabDtvr;
    private ListView mListView;
    private MyAdapter mAdapter;
    SparseArray<ChannelInfo> videoList;
    SparseArray<ChannelInfo> radioList;

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
        tabAtv = (TextView)findViewById(R.id.tab_atv);
        tabDtvv = (TextView)findViewById(R.id.tab_dtv_video);
        tabDtvr = (TextView)findViewById(R.id.tab_dtv_radio);
        tabDtv = (LinearLayout)findViewById(R.id.tabs_dtv);
        mListView = (ListView)findViewById(R.id.channel_video_list);
        mListView.setOnItemClickListener(this);
    }

    public void initView(int type, SparseArray<ChannelInfo> list) {
        Utils.logd(TAG, "==== init atv list, type =" + type + ", size =" + list.size());
        mType = type;
        tabFlag = true;
        initTab();

        videoList = list;
        initList();
    }

    public void initView(int type, SparseArray<ChannelInfo> video, SparseArray<ChannelInfo> radio) {
        Utils.logd(TAG, "==== init dtv list, type =" + type + ", video size =" + video.size()
                + ", radio size =" + radio.size());
        mType = type;
        tabFlag = true;
        initTab();

        videoList = video;
        radioList = radio;
        initList();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ChannelInfo channel = (ChannelInfo)mAdapter.getItem(position);
        int index = (int) mAdapter.getItemId(position);

        if (channel != null) {
            mListener.onSelect(index, !tabFlag);
        }
    }

    private SparseArray<ChannelInfo> getFavList(SparseArray<ChannelInfo> list) {
        SparseArray<ChannelInfo> favList = new SparseArray<ChannelInfo>();
        for (int i = 0; i < list.size(); i++) {
            ChannelInfo info = list.valueAt(i);
            if (info.isFavourite())
                favList.put(info.getDisplayNumber(), info);
        }
        return favList;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Utils.logd(TAG, "==== event.keycode =" + event.getKeyCode());
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!tabFlag) {
                    switchToChannelList();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (tabFlag) {
                    switchToChannelList();
                }
                break;
            default:
                break;
        }

        return super.dispatchKeyEvent(event);
    }

    private void switchToChannelList() {
        if (mType == Utils.ATV_FAV_LIST || mType == Utils.ATV_LIST)
            return;
        tabFlag = !tabFlag;
        initTab();
        initList();
    }

    private void initList() {
        if (mAdapter == null)
            mAdapter = new MyAdapter(mContext);
        switch (mType) {
            case Utils.ATV_LIST:
            case Utils.ATV_FAV_LIST:
                mAdapter.setFavorite(mType == Utils.ATV_FAV_LIST ? true : false);
                mAdapter.setList(mType == Utils.DTV_FAV_LIST ? getFavList(videoList) : videoList);
                mListView.setAdapter(mAdapter);
                break;
            case Utils.DTV_LIST:
            case Utils.DTV_FAV_LIST:
                mAdapter.setFavorite(mType == Utils.DTV_FAV_LIST ? true : false);
                if (tabFlag) {
                    mAdapter.setList(mType == Utils.DTV_FAV_LIST ? getFavList(videoList) : videoList);
                } else {
                    mAdapter.setList(mType == Utils.DTV_FAV_LIST ? getFavList(radioList) : radioList);
                }
                mListView.setAdapter(mAdapter);
                break;
            default:
                break;
        }
    }

    private void initTab() {
        switch (mType) {
            case Utils.ATV_FAV_LIST:
            case Utils.ATV_LIST:
                tabAtv.setBackgroundResource(R.drawable.atv_tab_focus);
                tabDtv.setVisibility(INVISIBLE);
                tabAtv.setVisibility(VISIBLE);
                break;
            case Utils.DTV_FAV_LIST:
            case Utils.DTV_LIST:
                tabAtv.setVisibility(INVISIBLE);
                if (tabFlag) {
                    textViewFocus(tabDtvv);
                    textViewUnfocus(tabDtvr);
                } else {
                    textViewFocus(tabDtvr);
                    textViewUnfocus(tabDtvv);
                }
                tabDtv.setVisibility(VISIBLE);
                break;
            default:
                break;
        }
    }

    private void textViewFocus(TextView view){
        ColorStateList csl;
        csl = (ColorStateList) mResources.getColorStateList(R.color.channel_list_tab_focus);
        if (csl != null) {
            view.setTextColor(csl);
        }
        view.setBackgroundResource(R.drawable.dtv_tab_focus);
    }

    private void textViewUnfocus(TextView view){
        ColorStateList csl;
        csl = (ColorStateList) mResources.getColorStateList(R.color.color_text_main);
        if (csl != null) {
            view.setTextColor(csl);
        }
        view.setBackgroundResource(R.drawable.dtv_tab_unfocus);
    }

    public boolean isShow() {
        return this.getVisibility() == VISIBLE;
    }

    public void show() {
        this.setVisibility(VISIBLE);
        this.requestLayout();
        this.requestFocus();
    }

    public void hide() {
        if (!isShow())
            return;
        tabFlag = true;
        this.setVisibility(INVISIBLE);
    }

    public interface OnChannelSelectListener {
        void onSelect(int channelIndex, boolean isRadio);
    }

    public void setOnChannelSelectListener (OnChannelSelectListener listener) {
        mListener = listener;
    }

    private class MyAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private SparseArray<ChannelInfo> mList;
        private boolean mIsFav = false;

        private MyAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        public void setFavorite(boolean isFav) {
            mIsFav = isFav;
        }

        public void setList(SparseArray<ChannelInfo> list) {
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
            Utils.logd(TAG, "==== number =" + channel.getDisplayNumber());
            Utils.logd(TAG, "==== name =" + channel.getDisplayName());
            holder.channelNum.setText(Integer.toString(position));
            holder.channelName.setText(channel.getDisplayName());
            if (mIsFav) {
                holder.favImg.setImageResource(R.drawable.list_fav);
            }
            return convertView;
        }
    }

    private class ViewHolder {
        public TextView channelNum;
        public TextView channelName;
        public ImageView favImg;

        public ViewHolder(View view) {
            channelNum = (TextView)view.findViewById(R.id.list_item_channel_num);
            channelName = (TextView)view.findViewById(R.id.list_item_channel_name);
            favImg = (ImageView)view.findViewById(R.id.list_item_channel_fav);
        }
    }
}
