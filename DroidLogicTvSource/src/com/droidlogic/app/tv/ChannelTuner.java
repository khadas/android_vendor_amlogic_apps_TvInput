package com.droidlogic.app.tv;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

public class ChannelTuner {
    private static final String TAG = "ChannelTuner";
    private final Context mContext;
    private final TvInputInfo mInputInfo;
    private final String mInputId;
    private int mSourceType;

    private SparseArray<Channel> mVideoChannels = new SparseArray<>();
    private SparseArray<Channel> mRadioChannels = new SparseArray<>();

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
            mCurrentChannelIndex = 0;
            mCurrentChannel = Channel.createPassthroughChannel(mInputId);
            mVideoChannels.put(mCurrentChannelIndex, mCurrentChannel);
        } else {
            Cursor cursor = null;
            ContentResolver resolver = mContext.getContentResolver();
            Uri uri = TvContract.buildChannelsUriForInput(mInputId);
            try {
                cursor = resolver.query(uri, Channel.PROJECTION, null, null, null);
                if (cursor != null)
                    Log.d(TAG, "==== initChannelList, cursor count = " + cursor.getCount());
                while (cursor != null && cursor.moveToNext()) {
                    Channel channel = Channel.fromCursor(cursor);
                    if (isDTVChannel() && isRadioChannel(channel)) {
                        mRadioChannels.put(channel.getChannelNumber(), channel);
                    } else if (isVideoChannel(channel)) {
                        mVideoChannels.put(channel.getChannelNumber(), channel);
                    }
                }
                if (mVideoChannels.size() > 0) {
                    mCurrentChannelIndex = 0;
                    mCurrentChannel = mVideoChannels.valueAt(mCurrentChannelIndex);
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
                    mCurrentChannel = mRadioChannels.valueAt(mCurrentChannelIndex);
                } else if (mCurrentChannelIndex <= mRadioChannels.size()) {
                    mCurrentChannel = mRadioChannels.valueAt(mCurrentChannelIndex);
                } else {
                    mCurrentChannelIndex = mRadioChannels.size() - 1;
                    mCurrentChannel = mRadioChannels.valueAt(mCurrentChannelIndex);
                }
            } else if (mVideoChannels.size() > 0){
                if (mCurrentChannelIndex == -1) {
                    mCurrentChannelIndex = 0;
                    mCurrentChannel = mVideoChannels.valueAt(mCurrentChannelIndex);
                } else if (mCurrentChannelIndex <= mVideoChannels.size()-1) {
                    mCurrentChannel = mVideoChannels.valueAt(mCurrentChannelIndex);
                } else {
                    mCurrentChannelIndex = mVideoChannels.size() - 1;
                    mCurrentChannel = mVideoChannels.valueAt(mCurrentChannelIndex);
                }
            } else {
                mCurrentChannel = null;
                mCurrentChannelIndex = -1;
            }
        } else if (isRadioChannel(channel)) {
            mCurrentChannelIndex = mRadioChannels.indexOfValue(channel);
            mCurrentChannel = channel;
        } else if (isVideoChannel(channel)) {
            mCurrentChannelIndex = mVideoChannels.indexOfValue(channel);
            mCurrentChannel = channel;
        } else {
            //service type is other.
        }
    }

    private void deleteRowChannel(long id) {
        if (isDTVChannel()) {
            for (int i=0; i<mRadioChannels.size(); i++) {
                if (mRadioChannels.valueAt(i).getId() == id) {
                    mRadioChannels.removeAt(i);
                    return;
                }
            }
        }
        for (int i=0; i<mVideoChannels.size(); i++) {
            if (mVideoChannels.valueAt(i).getId() == id) {
                mVideoChannels.removeAt(i);
                return;
            }
        }
    }

    public void changeRowChannel(Uri uri) {
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
        if (!TextUtils.equals(mInputId, channel.getInputId()))
            return;
        Log.d(TAG, "==== channel inputId = " + channel.getInputId()
                + ", service_id = " + channel.getServiceId());
        if (isDTVChannel() && isRadioChannel(channel)) {
            for (int i=0; i<mRadioChannels.size(); i++) {
                if (channel.equals(mRadioChannels.valueAt(i))) {
                    break;
                }
            }
            mRadioChannels.put(channel.getChannelNumber(), channel);
            updateCurrentChannel(channel);
            return;
        } else if (isVideoChannel(channel)) {
            for (int i=0; i<mRadioChannels.size(); i++) {
                if (channel.equals(mRadioChannels.valueAt(i))) {
                    break;
                }
            }
            mVideoChannels.put(channel.getChannelNumber(), channel);
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
                    mRadioChannels.put(channel.getChannelNumber(), channel);
                } else if (isVideoChannel(channel)) {
                    mVideoChannels.put(channel.getChannelNumber(), channel);
                }
            }
            if (mVideoChannels.size() > 0) {
                mCurrentChannelIndex = 0;
                mCurrentChannel = mVideoChannels.valueAt(mCurrentChannelIndex);
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
        if (isPassthrough())
            return;

        int total_size = 0;
        total_size = isRadio ? mRadioChannels.size() : mVideoChannels.size();
        if (index < 0 || index >= total_size) {
            mCurrentChannelIndex = DEFAULT_CHANNEL_IDDEX;
            mCurrentChannel = null;
        } else {
            mCurrentChannelIndex = index;
            mCurrentChannel = isRadio ? mRadioChannels.valueAt(index) : mVideoChannels.valueAt(index);
        }
    }

    /**
     * for {@link KeyEvent.KEYCODE_CHANNEL_UP} and {@link KeyEvent.KEYCODE_CHANNEL_DOWN}
     * @param flag {@code true} means {@link KeyEvent.KEYCODE_CHANNEL_UP},
     * {@code false} means {@link KeyEvent.KEYCODE_CHANNEL_DOWN}
     */
    public boolean moveToChannel(boolean flag) {
        if (isPassthrough())
            return false;

        int total_size = isRadioChannel() ? mRadioChannels.size() : mVideoChannels.size();
        int step = flag ? 1 : -1;
        mCurrentChannelIndex += step;
        if (total_size <= 0)
            return false;

        if (mCurrentChannelIndex < 0) {
            mCurrentChannelIndex = total_size - 1;
        }else if (mCurrentChannelIndex >= total_size) {
            mCurrentChannelIndex = 0;
        }
        mCurrentChannel = isRadioChannel() ? mRadioChannels.valueAt(mCurrentChannelIndex)
                : mVideoChannels.valueAt(mCurrentChannelIndex);
        return true;
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
