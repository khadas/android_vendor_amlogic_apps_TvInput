package com.droidlogic.app.tv;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class ChannelTuner {
    private static final String TAG = "ChannelTuner";
    private final Context mContext;
    private final TvInputInfo mInputInfo;
    private final String mInputId;
    private int mSourceType;
    private boolean isVideoType = true;

    private List<Channel> mChannels = new ArrayList<Channel>();
    private List<Channel> mRadioChannels = new ArrayList<Channel>();

    private Channel mCurrentChannel;
    private static final int DEFAULT_CHANNEL_IDDEX = -1;
    private int mCurrentChannelIndex = DEFAULT_CHANNEL_IDDEX;

    public ChannelTuner(Context context, TvInputInfo input_info) {
        mContext = context;
        mInputInfo = input_info;
        mInputId = mInputInfo.getId();
    }

    private boolean isPassthrough() {
        return mInputInfo.isPassthroughInput();
    }

    public void initChannelList(int source_type) {
        mSourceType = source_type;
        if (isPassthrough()) {
            mChannels.add(Channel.createPassthroughChannel(mInputId));
            mCurrentChannel = mChannels.get(0);
            mCurrentChannelIndex = 0;
        } else {
            Cursor cursor = null;
            ContentResolver resolver = mContext.getContentResolver();
            Uri uri = TvContract.buildChannelsUriForInput(mInputId);
            try {
                cursor = resolver.query(uri, Channel.PROJECTION, null, null, null);
                if (cursor != null)
                    Log.d(TAG, "==== cursor count = " + cursor.getCount());
                while (cursor != null && cursor.moveToNext()) {
                    Channel channel = Channel.fromCursor(cursor);
                    if (source_type == DroidLogicTvUtils.SOURCE_TYPE_DTV
                            && TextUtils.equals(channel.getServiceType(), Channels.SERVICE_TYPE_AUDIO)) {
                        mRadioChannels.add(channel);
                    } else {
                        mChannels.add(channel);
                    }
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    public void moveToChannel(String number) {
        if (TextUtils.isEmpty(number))
            throw new IllegalArgumentException("param number is empty or null");
        List<Channel> temp = new ArrayList<Channel>();
        if (mSourceType == DroidLogicTvUtils.SOURCE_TYPE_DTV && !isVideoType) {
            temp.addAll(mRadioChannels);
        } else {
            temp.addAll(mChannels);
        }
        for (int i=0; i<temp.size(); i++) {
            Log.d(TAG, "====size ="+temp.size() +", number="+number+", disn=" +temp.get(i).getDisplayNumber());
            if (TextUtils.equals(number, temp.get(i).getDisplayNumber())) {
                mCurrentChannel = temp.get(i);
                mCurrentChannelIndex = i;
               return;
            }
        }
        mCurrentChannel = null;
    }

    public void moveToChannel(int index) {
        if (isPassthrough() || mChannels.size() <= 0)
            return;

        List<Channel> temp = new ArrayList<Channel>();
        if (mSourceType == DroidLogicTvUtils.SOURCE_TYPE_DTV && !isVideoType) {
            temp.addAll(mRadioChannels);
        } else {
            temp.addAll(mChannels);
        }
        if (index < 0 || index >= temp.size()) {
            mCurrentChannel = null;
        } else {
            mCurrentChannelIndex = index;
            mCurrentChannel = temp.get(mCurrentChannelIndex);
        }
    }

    public void moveToChannel(boolean flag) {
        if (isPassthrough() || mChannels.size() <= 0)
            return;

        List<Channel> temp = new ArrayList<Channel>();
        if (mSourceType == DroidLogicTvUtils.SOURCE_TYPE_DTV && !isVideoType) {
            temp.addAll(mRadioChannels);
        } else {
            temp.addAll(mChannels);
        }
        int step = flag ? 1 : -1;
        mCurrentChannelIndex += step;
        if (mCurrentChannelIndex < 0) {
            mCurrentChannelIndex = temp.size() - 1;
        }else if (mCurrentChannelIndex >= temp.size()) {
            mCurrentChannelIndex = 0;
        }
        mCurrentChannel = temp.get(mCurrentChannelIndex);
    }

    public Channel getChannel() {
        return mCurrentChannel;
    }

    public Channel getChannel(int index) {
        if (index < 0 || index >= mChannels.size())
            throw new IllegalArgumentException("param index is out of range");

        return mChannels.get(index);
    }

    public Channel getChannel(long _id) {
        if (mInputInfo.isPassthroughInput())
            return mChannels.get(0);

        if (_id < 0)
            throw new IllegalArgumentException("param id must be a nonnegative number");

        for (Channel channel : mChannels) {
            if (channel.getId() == _id)
                return channel;
        }
        return null;
    }

    public Channel getChannel(String number) {
        if (TextUtils.isEmpty(number))
            throw new IllegalArgumentException("param number is empty or null");

        for (Channel channel : mChannels) {
            if (TextUtils.equals(number, channel.getDisplayNumber()))
                return channel;
        }
        return null;
    }

    public Uri getUri() {
        if (mCurrentChannel == null) {
            return TvContract.buildChannelUri(DEFAULT_CHANNEL_IDDEX);
        }
        return mCurrentChannel.getUri();
    }

    public int getChannelIndex() {
        if (mCurrentChannel == null)
            return DEFAULT_CHANNEL_IDDEX;
        return mCurrentChannelIndex;
    }

    public String getChannelType() {
        if (mCurrentChannel == null)
            return "";
        return mCurrentChannel.getType();
    }

    public String getChannelNumber() {
        if (mCurrentChannel == null)
            return "";
        return mCurrentChannel.getDisplayNumber();
    }

    public String getChannelName() {
        if (mCurrentChannel == null)
            return "";
        return mCurrentChannel.getDisplayName();
    }

    public String getChannelVideoFormat() {
        if (mCurrentChannel == null)
            return "";
        return mCurrentChannel.getVideoFormat();
    }

    public void setChannelType(String type) {
        if (mCurrentChannel == null)
            return;
        mCurrentChannel.setType(type);
    }

    public void setChannelVideoFormat(String format) {
        if (mCurrentChannel == null)
            return;
        mCurrentChannel.setVideoFormat(format);
    }

    /**
     * @param flag {@code true} means {@link TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO}
     */
    public void setChannelServiceType(boolean flag) {
        isVideoType = flag;
    }
}
