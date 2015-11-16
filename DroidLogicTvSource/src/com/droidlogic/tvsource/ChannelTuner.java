package com.droidlogic.tvsource;

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

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;

public class ChannelTuner {
    private static final String TAG = "ChannelTuner";
    private final Context mContext;
    private final TvInputInfo mInputInfo;
    private final String mInputId;
    private int mSourceType;

    private SparseArray<ChannelInfo> mVideoChannels = new SparseArray<>();
    private SparseArray<ChannelInfo> mRadioChannels = new SparseArray<>();

    private ChannelInfo mCurrentChannel;
    private int mCurrentChannelIndex;
    private int DEFAULT_INDEX = 0;
    private String mSelection = Channels.COLUMN_BROWSABLE + "=1";

    public ChannelTuner(Context context, TvInputInfo input_info) {
        mContext = context;
        mInputInfo = input_info;
        mInputId = mInputInfo.getId();
        reset();
    }

    private boolean isPassthrough() {
        return mInputInfo.isPassthroughInput();
    }

    private boolean isDTVChannel() {
        return mSourceType == DroidLogicTvUtils.SOURCE_TYPE_DTV;
    }

    private boolean isVideoChannel(ChannelInfo channel) {
        return TextUtils.equals(channel.getServiceType(), Channels.SERVICE_TYPE_AUDIO_VIDEO);
    }

    private boolean isRadioChannel(ChannelInfo channel) {
        return TextUtils.equals(channel.getServiceType(), Channels.SERVICE_TYPE_AUDIO);
    }

    private boolean isSameChannel(ChannelInfo channel_1, ChannelInfo channel_2) {
        if ((isDTVChannel() && channel_1.getServiceId() == channel_2.getServiceId())
            || (!isDTVChannel() && channel_1.getFrequency() == channel_2.getFrequency())) {
            Log.d(TAG, "======= current channel " + channel_1.getDisplayName() + " changed");
            return true;
        }
        return false;
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
            mCurrentChannel = ChannelInfo.createPassthroughChannel(mInputId);
            mVideoChannels.put(DEFAULT_INDEX, mCurrentChannel);
        } else {
            Cursor cursor = null;
            ContentResolver resolver = mContext.getContentResolver();
            Uri uri = TvContract.buildChannelsUriForInput(mInputId);
            try {
                cursor = resolver.query(uri, ChannelInfo.SIMPLE_PROJECTION, mSelection, null, null);
                if (cursor != null)
                    Log.d(TAG, "==== initChannelList, cursor count = " + cursor.getCount());
                while (cursor != null && cursor.moveToNext()) {
                    ChannelInfo channel = ChannelInfo.fromSimpleCursor(cursor);
                    if (mSourceType == DroidLogicTvUtils.SOURCE_TYPE_OTHER) {
                        mVideoChannels.put((int)channel.getId(), channel);
                    } else if (isDTVChannel() && isRadioChannel(channel)) {
                        mRadioChannels.put(channel.getDisplayNumber(), channel);
                    } else if (isVideoChannel(channel)) {
                        mVideoChannels.put(channel.getDisplayNumber(), channel);
                    }
                }
                if (mVideoChannels.size() > 0) {
                    mCurrentChannel = mVideoChannels.valueAt(DEFAULT_INDEX);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
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

    /**
     * invoked when delete/insert/update a channel.
     * if delete/insert a channel, must update mCurrentChannel}.
     * if update a channel, replace the old one.
     */
    public void changeRowChannel(Uri uri) {
        if (isPassthrough())
            return;

        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        ChannelInfo channel = null;
        try {
            cursor = resolver.query(uri, ChannelInfo.SIMPLE_PROJECTION, mSelection, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                long id = (long)Integer.parseInt(uri.getLastPathSegment());
                deleteRowChannel(id);
                mCurrentChannel = null;
                return;
            } else if (cursor != null && cursor.moveToNext()) {
                channel = ChannelInfo.fromSimpleCursor(cursor);
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
                    mRadioChannels.setValueAt(i, channel);
                    return;
                }
            }
            mRadioChannels.put(channel.getDisplayNumber(), channel);
            mCurrentChannel = channel;
            return;
        } else if (isVideoChannel(channel)) {
            for (int i=0; i<mVideoChannels.size(); i++) {
                if (channel.equals(mVideoChannels.valueAt(i))) {
                    mVideoChannels.setValueAt(i, channel);

                    if (isSameChannel(mCurrentChannel, channel))
                        mCurrentChannel = channel;
                    return;
                }
            }
            mVideoChannels.put(channel.getDisplayNumber(), channel);
            mCurrentChannel = channel;
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

        mRadioChannels.clear();
        mVideoChannels.clear();
        ChannelInfo saveCurrentChannel = mCurrentChannel;
        mCurrentChannel = null;

        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        try {
            cursor = resolver.query(uri, ChannelInfo.SIMPLE_PROJECTION, mSelection, null, null);
            while (cursor != null && cursor.moveToNext()) {
                ChannelInfo channel = ChannelInfo.fromSimpleCursor(cursor);
                if (mSourceType == DroidLogicTvUtils.SOURCE_TYPE_OTHER) {
                    mVideoChannels.put((int)channel.getId(), channel);
                } else if (isDTVChannel() && isRadioChannel(channel)) {
                    mRadioChannels.put(channel.getDisplayNumber(), channel);
                } else if (isVideoChannel(channel)) {
                    mVideoChannels.put(channel.getDisplayNumber(), channel);
                }
                if (isSameChannel(channel, saveCurrentChannel)) {
                    mCurrentChannel = channel;
                }
            }
            if (mCurrentChannel == null && mVideoChannels.size() > 0) {
                mCurrentChannel = mVideoChannels.valueAt(DEFAULT_INDEX);
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
    }

    /**
     * usually, method is used to initialize this object.
     */
    public boolean moveToChannel(int index, boolean isRadio) {
        if (isPassthrough())
            return false;

        int total_size = 0;
        total_size = isRadio ? mRadioChannels.size() : mVideoChannels.size();
        if (index < 0 || index >= total_size) {
            return false;
        }

        mCurrentChannel = getChannelByIndex(index, isRadio);
        return true;
    }

    public boolean moveToIndex(int index) {
        if (isPassthrough())
            return false;

        int total_size = 0;
        total_size = isRadioChannel() ? mRadioChannels.size() : mVideoChannels.size();
        if (index < 0 || index >= total_size) {
            return false;
        }

        mCurrentChannel = getChannelByIndex(index);
        return true;
    }

    /**
     * @param step offset from current channel index.
     * @return {@code true} indicates to get a channel successfully.
     * {@code false} indicates channel is null.
     */
    public boolean moveToOffset(int offset) {
        if (isPassthrough())
            return false;

        int total_size = isRadioChannel() ? mRadioChannels.size() : mVideoChannels.size();
        if (total_size <= 0)
            return false;

        int currentIndex;
        if (mCurrentChannel != null)
            currentIndex = mCurrentChannel.getDisplayNumber();
        else
            currentIndex = 0;

        currentIndex += offset;
        if (currentIndex < 0) {
            currentIndex = total_size + currentIndex;
        }else if (currentIndex >= total_size) {
            currentIndex = currentIndex - currentIndex;
        }
        mCurrentChannel = getChannelByIndex(currentIndex);
        return true;
    }

    private ChannelInfo getChannelByIndex (int index, boolean isRadio) {
        ChannelInfo info = null;
        if (isRadio) {
            for (int i = 0; i < mRadioChannels.size(); i++) {
                info = (ChannelInfo)mRadioChannels.valueAt(i);
                if (info.getDisplayNumber() == index)
                    return info;
            }
        } else {
            for (int i = 0; i < mVideoChannels.size(); i++) {
                info = (ChannelInfo)mVideoChannels.valueAt(i);
                if (info.getDisplayNumber() == index)
                    return info;
            }
        }
        return null;
    }

    private ChannelInfo getChannelByIndex (int index) {
        ChannelInfo info = null;
        if (isRadioChannel()) {
            for (int i = 0; i < mRadioChannels.size(); i++) {
                info = (ChannelInfo)mRadioChannels.valueAt(i);
                if (info.getDisplayNumber() == index)
                    return info;
            }
        } else {
            for (int i = 0; i < mVideoChannels.size(); i++) {
                info = (ChannelInfo)mVideoChannels.valueAt(i);
                if (info.getDisplayNumber() == index)
                    return info;
            }
        }
        return null;
    }

    public Uri getUri() {
        if (mCurrentChannel == null) {
            return TvContract.buildChannelUri(-1);
        }
        return mCurrentChannel.getUri();
    }

    public long getChannelId() {
        if (mCurrentChannel == null)
            return -1;
        return mCurrentChannel.getId();
    }

    public int getChannelIndex() {
        if (mCurrentChannel == null)
            return -1;

        return mCurrentChannel.getDisplayNumber();
    }

    public String getChannelType() {
        if (mCurrentChannel == null)
            return "";
        return "TBD";
    }

    public String getChannelNumber() {
        if (mCurrentChannel == null)
            return "";
        return Integer.toString(mCurrentChannel.getDisplayNumber());
    }

    public String getChannelName() {
        if (mCurrentChannel == null)
            return "";
        return mCurrentChannel.getDisplayName();
    }

    public String getChannelVideoFormat() {
        if (mCurrentChannel == null)
            return "";
        return "TBD";
    }

    public void setChannelType(String type) {
        if (mCurrentChannel == null || TextUtils.isEmpty(type))
            return;
        //mCurrentChannel.setType(Integer.valueOf(type));
    }

    public void setChannelVideoFormat(String format) {
        if (mCurrentChannel == null || TextUtils.isEmpty(format))
            return;
        //mCurrentChannel.setVideoFormat(Integer.valueOf(format));
    }

    public SparseArray<ChannelInfo> getChannelVideoList() {
        return mVideoChannels;
    }

    public SparseArray<ChannelInfo> getChannelRadioList() {
        return mRadioChannels;
    }
}
