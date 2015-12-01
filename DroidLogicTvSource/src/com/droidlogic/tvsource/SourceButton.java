package com.droidlogic.tvsource;

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;

import com.droidlogic.tvsource.ChannelDataManager;
import com.droidlogic.tvsource.ChannelTuner;
import com.droidlogic.tvsource.R;
import com.droidlogic.tvsource.Utils;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SourceButton extends Button implements OnClickListener{
    private static final String TAG = "SourceButton";

    private Context mContext;
    private ChannelTuner mChannelTuner;
    private TvInputInfo mInputInfo;
    private int mHardwareDeviceId = -1;
    private int mSourceType;
    private int mIsHardware = -1;
    private String mAvType = "";
    private int recentChannelIndex = -1;

    private OnSourceClickListener mListener;

    public SourceButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SourceButton(Context context, TvInputInfo info) {
        super(context);
        mContext = context;
        mInputInfo = info;
        init();
    }

    private void init() {
        ensureValidField(mInputInfo);
        if (isHidden()) {
            this.setVisibility(View.GONE);
            return;
        }

        setText(getLabel());
        setTextAppearance(mContext, R.style.tv_source_button);
        setBackgroundResource(R.drawable.bg_source_bt);

        initDeviceId();
        initSourceType();
        initChannelTuner();
        setOnClickListener(this);
    }

    public interface OnSourceClickListener{
        void onButtonClick(SourceButton sb);
    }

    public void setOnSourceClickListener(OnSourceClickListener l) {
        mListener = l;
    }

    public TvInputInfo geTvInputInfo() {
        return mInputInfo;
    }

    public String getInputId() {
        return mInputInfo.getId();
    }

    public int getDeviceId() {
        return mHardwareDeviceId;
    }

    private CharSequence getLabel() {
        if (!TextUtils.isEmpty(mInputInfo.loadCustomLabel(mContext))) {
            return mInputInfo.loadCustomLabel(mContext);
        }
        return mInputInfo.loadLabel(mContext);
    }

    public String getSourceLabel() {
        if (isRadioChannel())
            return getResources().getString(R.string.radio_label);
        return getLabel().toString();
    }

    private boolean isHidden() {
        return mInputInfo.isHidden(mContext);
    }

    public boolean isPassthrough() {
        return mInputInfo.isPassthroughInput();
    }

    public boolean isHardware() {
        return mIsHardware == -1 ? false : true;
    }

    public int getSourceType() {
        return mSourceType;
    }

    public Uri getUri() {
        return mChannelTuner.getUri();
    }

    public long getChannelId() {
        return mChannelTuner.getChannelId();
    }

    public int getChannelIndex() {
        return mChannelTuner.getCurrentChannelIndex();
    }

    public boolean isRadioChannel() {
        if (mChannelTuner != null)
            return mChannelTuner.isRadioChannel();
        else
            return false;
    }

    public String getChannelType() {
        return mChannelTuner.getChannelType();
    }

    public String getChannelNumber() {
        return mChannelTuner.getChannelNumber();
    }

    public String getChannelName() {
        return mChannelTuner.getChannelName();
    }

    public String getChannelVideoFormat() {
        return mChannelTuner.getChannelVideoFormat();
    }

    public void setAVType(String type) {
        mAvType = type;
    }

    public String getAVType() {
        return mAvType;
    }

    public void setChannelVideoFormat(String format) {
        mChannelTuner.setChannelVideoFormat(format);
    }

    public SparseArray<ChannelInfo> getChannelVideoList() {
        return mChannelTuner.getChannelVideoList();
    }

    public SparseArray<ChannelInfo> getChannelRadioList() {
        return mChannelTuner.getChannelRadioList();
    }

    private void initChannelTuner() {
        mChannelTuner = new ChannelTuner(mContext, mInputInfo);
        mChannelTuner.initChannelList(mSourceType);
        ChannelDataManager.addChannelTuner(mChannelTuner);
    }

    private void initDeviceId() {
        String[] temp = mInputInfo.getId().split("/");
        if (temp.length == 3) {
            mHardwareDeviceId = Integer.parseInt(temp[2].substring(2));
            mIsHardware = 1;
        }
    }

    private void initSourceType() {
        mSourceType = DroidLogicTvUtils.SOURCE_TYPE_OTHER;
        if (isHardware()) {
            switch (mHardwareDeviceId) {
                case DroidLogicTvUtils.DEVICE_ID_ATV:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_ATV;
                    break;
                case DroidLogicTvUtils.DEVICE_ID_DTV:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_DTV;
                    break;
                case DroidLogicTvUtils.DEVICE_ID_AV1:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_AV1;
                    break;
                case DroidLogicTvUtils.DEVICE_ID_AV2:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_AV2;
                    break;
                case DroidLogicTvUtils.DEVICE_ID_HDMI1:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_HDMI1;
                    break;
                case DroidLogicTvUtils.DEVICE_ID_HDMI2:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_HDMI2;
                    break;
                case DroidLogicTvUtils.DEVICE_ID_HDMI3:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_HDMI3;
                    break;
                default:
                    break;
            }
        }
    }

    public int getSigType() {
        int ret = 0;
        switch (mSourceType) {
            case DroidLogicTvUtils.SOURCE_TYPE_ATV:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_ATV;
                break;
            case DroidLogicTvUtils.SOURCE_TYPE_DTV:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_DTV;
                break;
            case DroidLogicTvUtils.SOURCE_TYPE_AV1:
            case DroidLogicTvUtils.SOURCE_TYPE_AV2:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_AV;
                break;
            case DroidLogicTvUtils.SOURCE_TYPE_HDMI1:
            case DroidLogicTvUtils.SOURCE_TYPE_HDMI2:
            case DroidLogicTvUtils.SOURCE_TYPE_HDMI3:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_HDMI;
                break;
            default:
                ret = DroidLogicTvUtils.SIG_INFO_TYPE_OTHER;
                break;
        }
        return ret;
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
        init();
    }

    public boolean moveToChannel(int index, boolean isRadio) {
        setRecentChannelIndex(getChannelIndex());
        return mChannelTuner.moveToChannel(index, isRadio);
    }

    /**
     * @return {@code true} move successfully, otherwise, move failed.
     */
    public boolean moveToOffset(int offset) {
        setRecentChannelIndex(getChannelIndex());
        return isPassthrough() ? false : mChannelTuner.moveToOffset(offset);
    }

    public boolean moveToIndex(int index) {
        int saveIndex = getChannelIndex();
        if (isPassthrough() ? false : mChannelTuner.moveToIndex(index)) {
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
    public void onClick(View v) {
        Utils.logd(TAG, "Input id switching to is " + mInputInfo.getId());
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
