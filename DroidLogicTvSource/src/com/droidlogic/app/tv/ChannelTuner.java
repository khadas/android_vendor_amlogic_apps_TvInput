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

    private List<Channel> mVideoChannels = new ArrayList<Channel>();
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

    private boolean isDTVChannel() {
        return mSourceType == DroidLogicTvUtils.SOURCE_TYPE_DTV;
    }

    private boolean isVideoChannel(Channel channel) {
        return TextUtils.equals(channel.getServiceType(), Channels.SERVICE_TYPE_AUDIO_VIDEO);
    }

    private boolean isRadioChannel(Channel channel) {
        return TextUtils.equals(channel.getServiceType(), Channels.SERVICE_TYPE_AUDIO);
    }

    public String getInputId() {
        return mInputId;
    }

    public boolean isVideoChannel() {
        if (mCurrentChannel != null) {
            return isVideoChannel(mCurrentChannel);
        }
        return false;
    }

    public boolean isRadioChannel() {
        if (mCurrentChannel != null) {
            return isRadioChannel(mCurrentChannel);
        }
        return false;
    }

    public void initChannelList(int source_type) {
        mSourceType = source_type;
        if (isPassthrough()) {
            mVideoChannels.add(Channel.createPassthroughChannel(mInputId));
            mCurrentChannel = mVideoChannels.get(0);
            mCurrentChannelIndex = 0;
        } else {
            Cursor cursor = null;
            ContentResolver resolver = mContext.getContentResolver();
            Uri uri = TvContract.buildChannelsUriForInput(mInputId);
            try {
                cursor = resolver.query(uri, Channel.PROJECTION, null, null, null);
                if (cursor != null)
                    Log.d(TAG, "====init cursor count = " + cursor.getCount());
                while (cursor != null && cursor.moveToNext()) {
                    Channel channel = Channel.fromCursor(cursor);
                    if (isDTVChannel() && isRadioChannel(channel)) {
                        mRadioChannels.add(channel);
                    } else if (isVideoChannel(channel)) {
                        mVideoChannels.add(channel);
                    }
                }
                if (mVideoChannels.size() > 0) {
                    mCurrentChannel = mVideoChannels.get(0);
                    mCurrentChannelIndex = 0;
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    /**
     * update channel lists after {@link TvProvider.notifyChange}
     * @param channel {@link Channel} has been changed. {@code null} means the channel has been delete.
     * Otherwise, the channel has been inserted or updated.
     */
    private void updateCurrentChannel(Channel channel) {
        if (channel == null) {//has delete a channel
            if (isRadioChannel() && mRadioChannels.size() > 0) {
                if (mCurrentChannelIndex == -1) {
                    mCurrentChannelIndex = 0;
                    mCurrentChannel = mRadioChannels.get(mCurrentChannelIndex);
                } else if (mCurrentChannelIndex <= mRadioChannels.size()) {
                    mCurrentChannel = mRadioChannels.get(mCurrentChannelIndex);
                } else {
                    mCurrentChannelIndex = mRadioChannels.size() - 1;
                    mCurrentChannel = mRadioChannels.get(mCurrentChannelIndex);
                }
            } else if (mVideoChannels.size() > 0){
                if (mCurrentChannelIndex == -1) {
                    mCurrentChannelIndex = 0;
                    mCurrentChannel = mVideoChannels.get(mCurrentChannelIndex);
                } else if (mCurrentChannelIndex <= mVideoChannels.size()-1) {
                    mCurrentChannel = mVideoChannels.get(mCurrentChannelIndex);
                } else {
                    mCurrentChannelIndex = mVideoChannels.size() - 1;
                    mCurrentChannel = mVideoChannels.get(mCurrentChannelIndex);
                }
            } else {
                mCurrentChannel = null;
                mCurrentChannelIndex = -1;
            }
        } else if (isRadioChannel(channel)) {
            mCurrentChannelIndex = mRadioChannels.indexOf(channel);
            mCurrentChannel = channel;
        } else if (isVideoChannel(channel)) {
            mCurrentChannelIndex = mVideoChannels.indexOf(channel);
            mCurrentChannel = channel;
        } else {
            //service type is other.
        }
    }

    private void deleteRowChannel(long id) {
        if (isDTVChannel()) {
            for (Channel c : mRadioChannels) {
                if (c.getId() == id) {
                    mRadioChannels.remove(c);
                    return;
                }
            }
        }
        for (Channel c : mVideoChannels) {
            if (c.getId() == id) {
                mVideoChannels.remove(c);
                return;
            }
        }
    }

    public void changeRowChannel(Uri uri) {
        Log.d(TAG, "==== changeRowChannel inputId = " + mInputId);
        if (isPassthrough()) 
            return;

        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        Channel channel = null;
        try {
            cursor = resolver.query(uri, Channel.PROJECTION, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                long id = (long)Integer.parseInt(uri.getLastPathSegment());
                deleteRowChannel(id);
                updateCurrentChannel(null);
                return;
            } else if (cursor != null && cursor.moveToNext()) {
                channel = Channel.fromCursor(cursor);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        Log.d(TAG, "==== channel inputId = " + channel.getInputId()
                + ", service_id = " + channel.getServiceId());
        if (!TextUtils.equals(mInputId, channel.getInputId()))
            return;
        if (isDTVChannel() && isRadioChannel(channel)) {
            for (Channel c : mRadioChannels) {
                if (c.equals(channel)) {
                    c.copyFrom(channel);
                    updateCurrentChannel(channel);
                    return;
                }
            }
            mRadioChannels.add(channel);
            updateCurrentChannel(channel);
            return;
        } else if (isVideoChannel(channel)) {
            for (Channel c : mVideoChannels) {
                if (c.equals(channel)) {
                    c.copyFrom(channel);
                    updateCurrentChannel(channel);
                    return;
                }
            }
            mVideoChannels.add(channel);
            updateCurrentChannel(channel);
        } else {
            //channel's service type is other.
        }
    }

    /**
     * insert/update/delete how many channels is unknown. So clear all lists and query
     * by the {@value uri} again.
     */
    public void changeChannels(Uri uri) {
        if (isPassthrough())
            return;

        String input_id = uri.getQueryParameter(TvContract.PARAM_INPUT);
        if (!TextUtils.equals(mInputId, input_id))
            return;
        reset();
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        try {
            cursor = resolver.query(uri, Channel.PROJECTION, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                Channel channel = Channel.fromCursor(cursor);
                if (isDTVChannel() && isRadioChannel(channel)) {
                    mRadioChannels.add(channel);
                } else if (isVideoChannel(channel)) {
                    mVideoChannels.add(channel);
                }
            }
            if (mVideoChannels.size() > 0) {
                mCurrentChannel = mVideoChannels.get(0);
                mCurrentChannelIndex = 0;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    private void reset() {
        mRadioChannels.clear();
        mVideoChannels.clear();
        mCurrentChannel = null;
        mCurrentChannelIndex = -1;
    }

    public void moveToChannel(int index, boolean isRadio) {
        if (isPassthrough() || mVideoChannels.size() <= 0)
            return;

        List<Channel> temp = new ArrayList<Channel>();
        if (isDTVChannel() && isRadio) {
            temp.addAll(mRadioChannels);
        } else {
            temp.addAll(mVideoChannels);
        }
        if (index < 0 || index >= temp.size()) {
            mCurrentChannel = null;
        } else {
            mCurrentChannelIndex = index;
            mCurrentChannel = temp.get(mCurrentChannelIndex);
        }
    }

    /**
     * for {@link KeyEvent.KEYCODE_CHANNEL_UP} and {@link KeyEvent.KEYCODE_CHANNEL_DOWN}
     * @param flag {@code true} means {@link KeyEvent.KEYCODE_CHANNEL_UP},
     * {@code false} means {@link KeyEvent.KEYCODE_CHANNEL_DOWN}
     */
    public void moveToChannel(boolean flag) {
        if (isPassthrough() || mVideoChannels.size() <= 0)
            return;

        List<Channel> temp = new ArrayList<Channel>();
        if (isDTVChannel() && isRadioChannel()) {
            temp.addAll(mRadioChannels);
        } else {
            temp.addAll(mVideoChannels);
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

}
