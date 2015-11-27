package com.droidlogic.tvsource;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvDataBaseManager;

public class ChannelTuner {
    private static final String TAG = "ChannelTuner";
    private static final boolean DEBUG = true;
    private final Context mContext;
    private TvDataBaseManager mTvDataBaseManager;
    private final TvInputInfo mInputInfo;
    private final String mInputId;
    private int mSourceType;

    private ArrayList<ChannelInfo> mVideoChannels = null;
    private ArrayList<ChannelInfo> mRadioChannels = null;

    private ChannelInfo mCurrentChannel = null;
    private int mCurrentChannelIndex;
    private int DEFAULT_INDEX = 0;
    private String mSelection = Channels.COLUMN_BROWSABLE + "=1";

    public ChannelTuner(Context context, TvInputInfo input_info) {
        mContext = context;
        mTvDataBaseManager = new TvDataBaseManager(mContext);
        mInputInfo = input_info;
        mInputId = mInputInfo.getId();
    }

    private boolean isPassthrough() {
        return mInputInfo.isPassthroughInput();
    }

    private boolean isDTVChannel() {
        return mSourceType == DroidLogicTvUtils.SOURCE_TYPE_DTV;
    }

    private boolean isVideoChannel(ChannelInfo channel) {
        if (channel != null)
            return TextUtils.equals(channel.getServiceType(), Channels.SERVICE_TYPE_AUDIO_VIDEO);

        return false;
    }

    private boolean isRadioChannel(ChannelInfo channel) {
        if (channel != null)
            return TextUtils.equals(channel.getServiceType(), Channels.SERVICE_TYPE_AUDIO);

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
            if (mVideoChannels == null)
                mVideoChannels = new ArrayList<ChannelInfo>();
            mVideoChannels.add(mCurrentChannel);
        } else {
            Uri uri = TvContract.buildChannelsUriForInput(mInputId);
            mVideoChannels = getUnskippedChannel(mTvDataBaseManager.getChannelList(mInputId, Channels.SERVICE_TYPE_AUDIO_VIDEO));
            if (isDTVChannel())
                mRadioChannels = getUnskippedChannel(mTvDataBaseManager.getChannelList(mInputId, Channels.SERVICE_TYPE_AUDIO));

            if (mVideoChannels.size() > 0) {
                mCurrentChannel = mVideoChannels.get(DEFAULT_INDEX);
            }
        }
        if (DEBUG)
            printList();
    }

    private ArrayList<ChannelInfo> getUnskippedChannel (ArrayList<ChannelInfo> channelList) {
        ArrayList<ChannelInfo> unskippedList = new ArrayList<ChannelInfo>();
        if (channelList != null) {
            for (int i = 0; i < channelList.size(); i++) {
                ChannelInfo info = (ChannelInfo)channelList.get(i);
                if (info != null && info.isBrowsable())
                    unskippedList.add(info);
            }
        }

        return unskippedList;
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
            cursor = resolver.query(uri, ChannelInfo.COMMON_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                channel = ChannelInfo.fromCommonCursor(cursor);
                if (!TextUtils.equals(mInputId, channel.getInputId()))
                    return;
                if (DEBUG)
                    Log.d(TAG, "==== changeRowChannel name=" + channel.getDisplayName());

                if (!channel.isBrowsable()) {
                    if (isDTVChannel() && isRadioChannel(channel)) {
                        mRadioChannels = updateSkippedChannel(mRadioChannels, channel);
                    } else {
                        mVideoChannels = updateSkippedChannel(mVideoChannels, channel);
                    }
                } else {
                    if (isDTVChannel() && isRadioChannel(channel))
                        mRadioChannels = updateChannelList(mRadioChannels, channel);
                    else
                        mVideoChannels = updateChannelList(mVideoChannels, channel);
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (mCurrentChannel == null && mVideoChannels.size() > 0) {
            updateCurrentScreen(mVideoChannels.get(DEFAULT_INDEX));
        }
        if (DEBUG)
            printList();
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

        if (mRadioChannels != null)
            mRadioChannels.clear();
        if (mVideoChannels != null)
            mVideoChannels.clear();

        mVideoChannels = getUnskippedChannel(mTvDataBaseManager.getChannelList(mInputId, Channels.SERVICE_TYPE_AUDIO_VIDEO));
        if (isDTVChannel())
            mRadioChannels = getUnskippedChannel(mTvDataBaseManager.getChannelList(mInputId, Channels.SERVICE_TYPE_AUDIO));


        int index = getChannelIndex(mCurrentChannel);
        boolean isRadio = isRadioChannel(mCurrentChannel);
        if (DEBUG)
            Log.d(TAG, "==== changeChannels uri=" + uri + " index=" + index);
        if (index == -1) {
            mCurrentChannel = null;
            if (isRadio && mRadioChannels.size() > 0)
                updateCurrentScreen(mRadioChannels.get(DEFAULT_INDEX));
            else if (mVideoChannels.size() > 0)
                updateCurrentScreen(mVideoChannels.get(DEFAULT_INDEX));
            else
                TvControlManager.open().StopPlayProgram();
        } else {
            if (isRadioChannel(mCurrentChannel))
                mCurrentChannel = mRadioChannels.get(index);
            else
                mCurrentChannel = mVideoChannels.get(index);
        }
        if (DEBUG)
            printList();
    }

    private ArrayList<ChannelInfo> updateSkippedChannel (ArrayList<ChannelInfo> channelList, ChannelInfo channel) {
        int index = getChannelIndex(channel);
        if (index != -1) {
            channelList.remove(index);
            if (ChannelInfo.isSameChannel(mCurrentChannel, channel) && channelList.size() > 0)
                updateCurrentScreen(channelList.get(DEFAULT_INDEX));
            else if (channelList.size() == 0) {
                mCurrentChannel = null;
                TvControlManager.open().StopPlayProgram();
            }
        }

        return channelList;
    }

    private ArrayList<ChannelInfo> updateChannelList (ArrayList<ChannelInfo> channelList, ChannelInfo channel) {
        int index = getChannelIndex(channel);
        if (index != -1) {
            channelList.remove(index);
            int newIndex = createNewChannelIndex(channelList, channel);
            if (newIndex < channelList.size())
                channelList.add(newIndex, channel);
            else
                channelList.add(channel);

            if (ChannelInfo.isSameChannel(mCurrentChannel, channel))
                mCurrentChannel = channel;
        } else
            channelList.add(createNewChannelIndex(channelList, channel), channel);

        return channelList;
    }

    private void updateCurrentScreen(ChannelInfo info) {
        mCurrentChannel = info;
        Intent intent = new Intent(DroidLogicTvUtils.ACTION_UPDATE_TV_PLAY);
        intent.putExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, Integer.toString(getChannelIndex(mCurrentChannel)));
        mContext.sendBroadcast(intent);
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

        mCurrentChannel = isRadio ? mRadioChannels.get(index) : mVideoChannels.get(index);
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

        mCurrentChannel = isRadioChannel() ? mRadioChannels.get(index) : mVideoChannels.get(index);
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
            currentIndex = getCurrentChannelIndex();
        else
            currentIndex = 0;

        currentIndex += offset;
        if (currentIndex < 0) {
            currentIndex = total_size + currentIndex;
        }else if (currentIndex >= total_size) {
            currentIndex = currentIndex - currentIndex;
        }
        mCurrentChannel = isRadioChannel() ? mRadioChannels.get(currentIndex) : mVideoChannels.get(currentIndex);
        return true;
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

    private int createNewChannelIndex (ArrayList<ChannelInfo> channelList, ChannelInfo channel) {
        if (channelList != null && channel != null) {
            for (int i = 0; i < channelList.size(); i++) {
                if (channel.getDisplayNumber() < channelList.get(i).getDisplayNumber())
                    return i;
            }
            return channelList.size();
        }
        return -1;
    }

    private int getChannelIndex(ChannelInfo channel) {
        if (isDTVChannel() && isRadioChannel(channel)) {
            for (int i = 0; i < mRadioChannels.size(); i++) {
                if (ChannelInfo.isSameChannel(channel, mRadioChannels.get(i)))
                    return i;
            }
            return -1;
        } else {
            for (int i = 0; i < mVideoChannels.size(); i++) {
                if (ChannelInfo.isSameChannel(channel, mVideoChannels.get(i)))
                    return i;
            }
            return -1;
        }
    }

    public int getCurrentChannelIndex() {
        if (mCurrentChannel == null)
            return -1;

        return getChannelIndex(mCurrentChannel);
    }

    public String getChannelType() {
        if (mCurrentChannel == null)
            return "";
        String colorSystemString = "";
        String soundSystemString = "";
        if (mCurrentChannel.getVideoStd() == 1)
            colorSystemString = "PAL";
        else if (mCurrentChannel.getVideoStd() == 2)
            colorSystemString = "NTSC";
        else
            colorSystemString = "PAL";
        if (mCurrentChannel.getAudioStd() == 0)
            soundSystemString = "D/K";
        else if (mCurrentChannel.getAudioStd() == 1)
            soundSystemString = "I";
        else if (mCurrentChannel.getAudioStd() == 2)
            soundSystemString = "B/G";
        else if (mCurrentChannel.getAudioStd() == 3)
            soundSystemString = "M";
        else
            soundSystemString = "D/K";
        return colorSystemString + "_" + soundSystemString;
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
        return mCurrentChannel.getVideoFormat();
    }

    public void setChannelType(String type) {
        if (mCurrentChannel == null || TextUtils.isEmpty(type))
            return;
        mCurrentChannel.setType(type);
    }

    public void setChannelVideoFormat(String format) {
        if (mCurrentChannel == null || TextUtils.isEmpty(format))
            return;
        mCurrentChannel.setVideoFormat(format);
    }

    public SparseArray<ChannelInfo> getChannelVideoList() {
        SparseArray<ChannelInfo> list = new SparseArray<>();

        for (int i = 0; i < mVideoChannels.size(); i++) {
            ChannelInfo info = mVideoChannels.get(i);
            list.put(i, info);
        }

        return list;
    }

    public SparseArray<ChannelInfo> getChannelRadioList() {
        SparseArray<ChannelInfo> list = new SparseArray<>();

        for (int i = 0; i < mRadioChannels.size(); i++) {
            ChannelInfo info = mRadioChannels.get(i);
            list.put(i, info);
        }

        return list;
    }

    private void printList() {
        if (mVideoChannels != null) {
            for (int i = 0; i < mVideoChannels.size(); i++) {
                ChannelInfo info = mVideoChannels.get(i);
                Log.d(TAG, "==== video: number=" + info.getDisplayNumber() + " name=" + info.getDisplayName());
            }
        }

        if (mRadioChannels!= null) {
            for (int i = 0; i < mRadioChannels.size(); i++) {
                ChannelInfo info = mRadioChannels.get(i);
                Log.d(TAG, "==== radio: number=" + info.getDisplayNumber() + " name=" + info.getDisplayName());
            }
        }
        if (mCurrentChannel != null)
            Log.d(TAG, "==== current channel: number" + mCurrentChannel.getDisplayNumber() + " name=" + mCurrentChannel.getDisplayName());
    }
}
