package com.droidlogic.ui;

import com.droidlogic.app.tv.ChannelDataManager;
import com.droidlogic.app.tv.ChannelTuner;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.tv.R;
import com.droidlogic.tv.Utils;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
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

    public CharSequence getLabel() {
        if (!TextUtils.isEmpty(mInputInfo.loadCustomLabel(mContext))) {
            return mInputInfo.loadCustomLabel(mContext);
        }
        return mInputInfo.loadLabel(mContext);
    }

    private boolean isHidden() {
        return mInputInfo.isHidden(mContext);
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

    public int getChannelId() {
        return mChannelTuner.getChannelIndex();
    }

    public boolean isRadioChannel() {
        return mChannelTuner.isRadioChannel();
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

    public void setChannelType(String type) {
        mChannelTuner.setChannelType(type);
    }

    public void setChannelVideoFormat(String format) {
        mChannelTuner.setChannelVideoFormat(format);
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

    /**
     * @return {@code true} move successfully, otherwise, the input is pass through or move failed.
     */
    public boolean channelUp() {
        return mChannelTuner.moveToChannel(true);
    }

    /**
     * @return {@code true} move successfully, otherwise, the input is pass through or move failed.
     */
    public boolean channelDown() {
        return mChannelTuner.moveToChannel(false);
    }

    public void moveToChannel(int index, boolean isRadio) {
        mChannelTuner.moveToChannel(index, isRadio);
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
                + ", label=" + getLabel()
                + ", sourceType=" + getSourceType()
                + "}";
    }

}
