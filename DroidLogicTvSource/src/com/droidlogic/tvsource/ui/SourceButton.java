package com.droidlogic.tvsource.ui;

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;

import com.droidlogic.tvsource.ChannelDataManager;
import com.droidlogic.tvsource.ChannelTuner;
import com.droidlogic.tvsource.R;
import com.droidlogic.tvsource.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;

public class SourceButton extends Button implements OnClickListener, OnFocusChangeListener{
    private static final String TAG = "SourceButton";

    private Context mContext;
    private Resources mResources;
    private ChannelTuner mChannelTuner = null;
    private TvInputInfo mInputInfo;
    private int mHardwareDeviceId = -1;
    private int mSourceType;
    private String mSourceLabel;
    private boolean mIsHardware = false;
    private String mAvType = "";
    private int recentChannelIndex = -1;
    private int mState = -1;

    private OnSourceClickListener mListener;

    public SourceButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * used when entering into TvApp at first time for hardware input.
     */
    public SourceButton(Context context, int deviceId) {
        super(context);
        Utils.logd(TAG, "==== deviceId = " + deviceId);
        mContext = context;
        mResources = context.getResources();
        mHardwareDeviceId = deviceId;
        init();
    }

    /**
     * used when initializing non-hardware input or updating source input.
     */
    public SourceButton(Context context, TvInputInfo info) {
        super(context);
        mContext = context;
        mResources = context.getResources();
        mInputInfo = info;
        init();
    }

    public void sourceRelease() {
        mInputInfo = null;
        ChannelDataManager.removeChannelTuner(mChannelTuner);
        mChannelTuner = null;
    }

    private void init() {
        if (isHidden()) {
            this.setVisibility(View.GONE);
            return;
        }

        setTextAppearance(mContext, R.style.tv_source_button);
        setBackgroundResource(R.drawable.bg_source_bt);
        setFocusableInTouchMode(true);

        initDeviceId();
        initSourceLabel();
        setText(getLabel());
        initTextColor();
        initSourceType();
        initChannelTuner();
        setOnClickListener(this);
        setOnFocusChangeListener(this);
    }

    public interface OnSourceClickListener{
        void onButtonClick(SourceButton sb);
    }

    public void setOnSourceClickListener(OnSourceClickListener l) {
        mListener = l;
    }

    public TvInputInfo getTvInputInfo() {
        return mInputInfo;
    }

    public String getInputId() {
        return mInputInfo == null ? "" : mInputInfo.getId();
    }

    public int getDeviceId() {
        return mHardwareDeviceId;
    }

    private CharSequence getLabel() {
        if (mInputInfo == null) {
            return mSourceLabel;
        }
        if (!TextUtils.isEmpty(mInputInfo.loadCustomLabel(mContext))) {
            return mInputInfo.loadCustomLabel(mContext);
        }
        return mInputInfo.loadLabel(mContext);
    }

    public String getSourceLabel() {
        if (mInputInfo == null) {
            return mSourceLabel;
        }
        if (isRadioChannel())
            return mResources.getString(R.string.radio_label);
        return getLabel().toString();
    }

    public boolean isAvaiableSource() {
        return mInputInfo != null;
    }

    private boolean isHidden() {
        return mInputInfo == null ? false : mInputInfo.isHidden(mContext);
    }

    public boolean isPassthrough() {
        return mInputInfo == null ? true : mInputInfo.isPassthroughInput();
    }

    public boolean isHardware() {
        return mIsHardware;
    }

    public int getSourceType() {
        return mSourceType;
    }

    public Uri getUri() {
        return  mInputInfo == null ? null : mChannelTuner.getUri();
    }

    public long getChannelId() {
        return  mInputInfo == null ? -1 : mChannelTuner.getChannelId();
    }

    public int getChannelIndex() {
        return mChannelTuner == null ? 0 : mChannelTuner.getCurrentChannelIndex();
    }

    public boolean isRadioChannel() {
        return mChannelTuner == null ? false : mChannelTuner.isRadioChannel();
    }

    public String getChannelType() {
        return mChannelTuner == null ? "" : mChannelTuner.getChannelType();
    }

    public String getChannelNumber() {
        return mChannelTuner == null ? "" : mChannelTuner.getChannelNumber();
    }

    public String getChannelName() {
        return mChannelTuner == null ? "" : mChannelTuner.getChannelName();
    }

    public String getChannelVideoFormat() {
        return mChannelTuner == null ? "" : mChannelTuner.getChannelVideoFormat();
    }

    public ChannelInfo getChannelInfo() {
        return mChannelTuner.getChannelInfo();
    }

    public void setAVType(String type) {
        mAvType = type;
    }

    public String getAVType() {
        return mAvType;
    }

    public void setChannelVideoFormat(String format) {
        if (mChannelTuner != null) {
            mChannelTuner.setChannelVideoFormat(format);
        }
    }

    public SparseArray<ChannelInfo> getChannelVideoList() {
        return mChannelTuner.getChannelVideoList();
    }

    public SparseArray<ChannelInfo> getChannelRadioList() {
        return mChannelTuner.getChannelRadioList();
    }

    private void initChannelTuner() {
        if (mInputInfo == null)
            return;
        mChannelTuner = new ChannelTuner(mContext, mInputInfo);
        mChannelTuner.initChannelList(mSourceType);
        ChannelDataManager.addChannelTuner(mChannelTuner);
    }

    private void initTextColor() {
        if (mState != TvInputManager.INPUT_STATE_CONNECTED)
            setTextColor(getResources().getColor(R.color.source_undisconnect));
        else
            setTextColor(getResources().getColor(R.color.source_unfocus));
    }

    /**
     * if {@code mInputInfo} is null, the device id must be initialized in constructor.
     */
    private void initDeviceId() {
        if (mHardwareDeviceId >= 0) {
            mIsHardware = true;
            return;
        }
        if (mInputInfo == null)
            return;

        String[] temp = mInputInfo.getId().split("/");
        if (temp.length == 3) {
            mHardwareDeviceId = Integer.parseInt(temp[2].substring(2));
            mIsHardware = true;
        }
    }


    private void initSourceLabel() {
        if (mIsHardware) {
            switch (mHardwareDeviceId) {
                case DroidLogicTvUtils.DEVICE_ID_ATV:
                    mSourceLabel = mResources.getString(R.string.source_bt_atv);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_DTV:
                    mSourceLabel = mResources.getString(R.string.source_bt_dtv);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_AV1:
                    mSourceLabel = mResources.getString(R.string.source_bt_av1);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_AV2:
                    mSourceLabel = mResources.getString(R.string.source_bt_av2);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_HDMI1:
                    mSourceLabel = mResources.getString(R.string.source_bt_hdmi1);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_HDMI2:
                    mSourceLabel = mResources.getString(R.string.source_bt_hdmi2);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_HDMI3:
                    mSourceLabel = mResources.getString(R.string.source_bt_hdmi3);
                    break;
                default:
                    break;
            }
        }
    }

    private void initSourceType() {
        mSourceType = DroidLogicTvUtils.getSourceType(mHardwareDeviceId);
    }

    public int getSigType() {
        return DroidLogicTvUtils.getSigType(mSourceType);
    }

    private void ensureValidField(TvInputInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("wrong argument info...");
        }else {
            mInputInfo = info;
        }
    }

    public void setTvInputInfo(TvInputInfo info) {
        mInputInfo = info;
        initChannelTuner();
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        Utils.logd(TAG, "==== setState, mState=" + mState + ", state=" + state);
        if (mState != state)
            stateChanged(state);
        mState = state;
    }

    private void stateChanged(int state) {
        if (hasFocus()) {
            setTextColor(getResources().getColor(R.color.source_focus));
        } else if (state != TvInputManager.INPUT_STATE_CONNECTED) {
            setTextColor(getResources().getColor(R.color.source_undisconnect));
        } else {
            setTextColor(getResources().getColor(R.color.source_unfocus));
        }
    }

    public boolean moveToChannel(int index, boolean isRadio) {
        if (mChannelTuner == null)
            return false;
        setRecentChannelIndex(getChannelIndex());
        return mChannelTuner.moveToChannel(index, isRadio);
    }

    /**
     * @return {@code true} move successfully, otherwise, move failed.
     */
    public boolean moveToOffset(int offset) {
        if (mChannelTuner == null)
            return false;
        setRecentChannelIndex(getChannelIndex());
        return isPassthrough() ? false : mChannelTuner.moveToOffset(offset);
    }

    public boolean moveToIndex(int index) {
        int saveIndex = getChannelIndex();
        if ((mChannelTuner == null || isPassthrough()) ? false : mChannelTuner.moveToIndex(index)) {
            setRecentChannelIndex(saveIndex);
            return true;
        } else
            return false;
    }

    public boolean moveToRecentChannel() {
        if (recentChannelIndex != getChannelIndex())
            return moveToIndex(recentChannelIndex);
        else
            return false;
    }

    private void setRecentChannelIndex(int index) {
        if (recentChannelIndex != index)
            recentChannelIndex = index;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus) {
            setTextColor(getResources().getColor(R.color.source_focus));
        } else if (mState != TvInputManager.INPUT_STATE_CONNECTED) {
            setTextColor(getResources().getColor(R.color.source_undisconnect));
        } else {
            setTextColor(getResources().getColor(R.color.source_unfocus));
        }
    }

    @Override
    public void onClick(View v) {
        mListener.onButtonClick(this);
    }

    @Override
    public String toString() {
        return "SourceButton {"
                + "inputId=" + getInputId()
                + ", isHardware=" + isHardware()
                + ", label=" + getSourceLabel()
                + ", sourceType=" + getSourceType()
                + "}";
    }

}
