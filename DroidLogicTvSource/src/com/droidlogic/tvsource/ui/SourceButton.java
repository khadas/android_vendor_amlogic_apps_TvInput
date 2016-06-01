package com.droidlogic.tvsource.ui;

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;

import com.droidlogic.tvsource.ChannelDataManager;
import com.droidlogic.tvsource.ChannelTuner;
import com.droidlogic.tvsource.R;
import com.droidlogic.tvsource.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SourceButton extends LinearLayout {
    private static final String TAG = "SourceButton";

    private Context mContext;
    private Resources mResources;
    private ChannelTuner mChannelTuner = null;
    private TvInputInfo mInputInfo;
    private int mHardwareDeviceId = -1;
    private int mSourceType;
    private String mSourceLabel;
    private ImageView imageSelect;
    private ImageView imageSource;
    private TextView textName;
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
        inflate(mContext, R.layout.layout_source_button, this);
        imageSelect = (ImageView)findViewById(R.id.img_select);
        imageSource = (ImageView)findViewById(R.id.img_source);
        textName = (TextView)findViewById(R.id.tx_source_name);

        initDeviceId();
        initSourceLabel();
        textName.setText(getLabel());
        initTextColor();
        initSourceType();
        initChannelTuner();
    }

    public void switchSource () {
        mListener.onButtonClick(this);
    }

    public interface OnSourceClickListener {
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
            textName.setTextColor(getResources().getColor(R.color.source_undisconnect));
        else
            textName.setTextColor(getResources().getColor(R.color.source_unfocus));
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
        Drawable icon = null;
        if (mIsHardware) {
            switch (mHardwareDeviceId) {
                case DroidLogicTvUtils.DEVICE_ID_ATV:
                    mSourceLabel = mResources.getString(R.string.source_bt_atv);
                    icon = mResources.getDrawable(R.drawable.icon_atv);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_DTV:
                    mSourceLabel = mResources.getString(R.string.source_bt_dtv);
                    icon = mResources.getDrawable(R.drawable.icon_dtv);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_AV1:
                    mSourceLabel = mResources.getString(R.string.source_bt_av1);
                    icon = mResources.getDrawable(R.drawable.icon_av);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_AV2:
                    mSourceLabel = mResources.getString(R.string.source_bt_av2);
                    icon = mResources.getDrawable(R.drawable.icon_av);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_HDMI1:
                    mSourceLabel = mResources.getString(R.string.source_bt_hdmi1);
                    icon = mResources.getDrawable(R.drawable.icon_hdmi);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_HDMI2:
                    mSourceLabel = mResources.getString(R.string.source_bt_hdmi2);
                    icon = mResources.getDrawable(R.drawable.icon_hdmi);
                    break;
                case DroidLogicTvUtils.DEVICE_ID_HDMI3:
                    mSourceLabel = mResources.getString(R.string.source_bt_hdmi3);
                    icon = mResources.getDrawable(R.drawable.icon_hdmi);
                    break;
                default:
                    break;
            }
        }
        imageSource.setImageDrawable(icon);
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
        } else {
            mInputInfo = info;
        }
    }

    public void setTvInputInfo(TvInputInfo info) {
        mInputInfo = info;
        initChannelTuner();
    }

    public void setSelected (boolean selected) {
        if (selected) {
            imageSelect.setImageDrawable(mResources.getDrawable(R.drawable.icon_select));
        } else {
            imageSelect.setImageDrawable(null);
        }
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
            textName.setTextColor(getResources().getColor(R.color.source_focus));
        } else if (state != TvInputManager.INPUT_STATE_CONNECTED) {
            textName.setTextColor(getResources().getColor(R.color.source_undisconnect));
        } else {
            textName.setTextColor(getResources().getColor(R.color.source_unfocus));
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

    public boolean dispatchKeyEvent (KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    switchSource();
                    break;
            }
        }

        return super.dispatchKeyEvent(event);
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
