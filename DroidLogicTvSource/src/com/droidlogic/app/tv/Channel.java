package com.droidlogic.app.tv;

import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.net.Uri;
import android.text.TextUtils;

public class Channel {
    public static final String[] PROJECTION = {
        Channels._ID,
        Channels.COLUMN_INPUT_ID,
        Channels.COLUMN_TYPE,
        Channels.COLUMN_SERVICE_TYPE,
        Channels.COLUMN_DISPLAY_NUMBER,
        Channels.COLUMN_DISPLAY_NAME,
        Channels.COLUMN_VIDEO_FORMAT,
        Channels.COLUMN_BROWSABLE,
        Channels.COLUMN_LOCKED};

    private long mId;
    private String mInputId;
    private String mType;
    private String mServiceType;
    private String mDisplayNumber;
    private String mDisplayName;
    private String mVideoFormat;
    private boolean mBrowsable;
    private boolean mLocked;
    private boolean mIsPassthrough;

    private Channel() {}

    public static Channel createPassthroughChannel(Uri paramUri) {
        if (!TvContract.isChannelUriForPassthroughInput(paramUri))
            throw new IllegalArgumentException("URI is not a passthrough channel URI");
        return createPassthroughChannel((String)paramUri.getPathSegments().get(1));
    }

    public static Channel createPassthroughChannel(String input_id) {
        return new Builder().setInputId(input_id).setPassthrough(true).build();
    }

    public static Channel fromCursor(Cursor cursor) {
        Builder builder = new Builder();

        int index = cursor.getColumnIndex(Channels._ID);
        if (index >= 0)
            builder.setId(cursor.getLong(index));
        index = cursor.getColumnIndex(Channels.COLUMN_INPUT_ID);
        if (index >= 0)
            builder.setInputId(cursor.getString(index));
        index = cursor.getColumnIndex(Channels.COLUMN_TYPE);
        if (index >= 0)
            builder.setType(cursor.getString(index));
        index = cursor.getColumnIndex(Channels.COLUMN_SERVICE_TYPE);
        if (index >= 0)
            builder.setServiceType(cursor.getString(index));
        index = cursor.getColumnIndex(Channels.COLUMN_DISPLAY_NUMBER);
        if (index >= 0)
            builder.setDisplayNumber(cursor.getString(index));
        index = cursor.getColumnIndex(Channels.COLUMN_DISPLAY_NAME);
        if (index >= 0)
            builder.setDisplayName(cursor.getString(index));
        index = cursor.getColumnIndex(Channels.COLUMN_VIDEO_FORMAT);
        if (index >= 0)
            builder.setVideoFormat(cursor.getString(index));
        index = cursor.getColumnIndex(Channels.COLUMN_BROWSABLE);
        if (index >= 0)
            builder.setBrowsable(cursor.getInt(index)==1 ? true : false);
        index = cursor.getColumnIndex(Channels.COLUMN_LOCKED);
        if (index >= 0)
            builder.setLocked(cursor.getInt(index)==1 ? true : false);

        builder.setPassthrough(false);
        return builder.build();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Channel))
            return false;

        boolean ret = false;
        Channel channel = (Channel)obj;

        if (TextUtils.equals(this.mInputId, channel.getInputId())
                && this.mIsPassthrough
                && channel.isPassthrough()) {
            ret = true;
        }
        return ret;
    }

    public long getId() {
        return this.mId;
    }

    public String getInputId() {
        return this.mInputId;
    }

    public String getType() {
        return this.mType;
    }

    public String getServiceType() {
        return this.mServiceType;
    }

    public String getDisplayNumber() {
        return this.mDisplayNumber;
    }

    public String getDisplayName() {
        return this.mDisplayName;
    }

    public String getVideoFormat() {
        return this.mVideoFormat;
    }

    public boolean isPassthrough() {
        return this.mIsPassthrough;
    }

    public boolean isBrowsable() {
        return this.mBrowsable;
    }

    public boolean isLocked() {
        return this.mLocked;
    }

    public Uri getUri() {
        if (isPassthrough()) {
            return TvContract.buildChannelUriForPassthroughInput(this.mInputId);
        }
        return TvContract.buildChannelUri(this.mId);
    }

    public void setType(String type) {
        this.mType = type;
    }

    public void setVideoFormat(String format) {
        this.mVideoFormat = format;
    }

    public void setDisplayName(String name) {
        this.mDisplayName = name;
    }

    public static final class Builder {
        private final Channel mChannel = new Channel();

        public Builder() {
            mChannel.mId = -1L;
            mChannel.mInputId = "";
            mChannel.mType = "";
            mChannel.mDisplayNumber = "0";
            mChannel.mDisplayName = "";
            mChannel.mVideoFormat = "";
            mChannel.mBrowsable = false;
            mChannel.mLocked = false;
            mChannel.mIsPassthrough = false;
        }

        public Builder setId(long id) {
            mChannel.mId = id;
            return this;
        }

        public Builder setInputId(String input_id) {
            mChannel.mInputId = input_id;
            return this;
        }

        public Builder setType(String type) {
            mChannel.mType = type;
            return this;
        }

        public Builder setServiceType(String type) {
            mChannel.mServiceType = type;
            return this;
        }

        public Builder setDisplayNumber(String number) {
            mChannel.mDisplayNumber = number;
            return this;
        }

        public Builder setDisplayName(String name) {
            mChannel.mDisplayName = name;
            return this;
        }

        public Builder setVideoFormat(String format) {
            mChannel.mVideoFormat = format;
            return this;
        }

        public Builder setBrowsable(boolean flag) {
            mChannel.mBrowsable = flag;
            return this;
        }

        public Builder setPassthrough(boolean flag) {
            mChannel.mIsPassthrough = flag;
            return this;
        }

        public Builder setLocked(boolean flag) {
            mChannel.mLocked = flag;
            return this;
        }

        public Channel build() {
            return mChannel;
        }
    }
}