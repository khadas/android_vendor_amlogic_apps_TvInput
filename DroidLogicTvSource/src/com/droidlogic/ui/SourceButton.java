package com.droidlogic.ui;

import com.droidlogic.app.DroidLogicTvUtils;
import com.droidlogic.tv.R;
import com.droidlogic.tv.Utils;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SourceButton extends Button implements OnClickListener{
    private static final String TAG = "SourceButton";

    private Context mContext;
    private TvInputInfo mInputInfo;
    private int mHardwareDeviceId = -1;
    private int mSourceType;
    private int mIsHardware = -1;

    private SourceButtonListener mListener;

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
        setText(getLabel());
        setTextAppearance(mContext, R.style.tv_source_button);
        setBackgroundResource(R.drawable.bg_source_bt);
//        if(mInputInfo.isHidden(mContext)) {
//            if (getResources().getBoolean(R.bool.source_need_gone)) {
//                setVisibility(View.GONE);
//            }else {
//                setSelected(false);
//                setFocusable(false);
//                setClickable(false);
//            }
//        }
        initDeviceId();
        initSourceType();
        setOnClickListener(this);
    }

    public interface SourceButtonListener{
        void onButtonClick(SourceButton sb);
    }

    public void setSourceButttonListener(SourceButtonListener l) {
        mListener = l;
    }

    public String getInputId() {
        return mInputInfo.getId();
    }

    public int getDeviceId() {
        return mHardwareDeviceId;
    }

    public String getLabel() {
        return mInputInfo.loadLabel(mContext).toString();
    }

    public boolean isHardware() {
        return mIsHardware == -1 ? false : true;
    }

    public int getSourceType() {
        return mSourceType;
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
                case 0:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_ATV;
                    break;
                case 10:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_DTV;
                    break;
                case 1:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_AV1;
                    break;
                case 2:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_AV2;
                    break;
                case 5:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_HDMI1;
                    break;
                case 6:
                    mSourceType = DroidLogicTvUtils.SOURCE_TYPE_HDMI2;
                    break;
                case 7:
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

    @Override
    public void onClick(View v) {
        Utils.logd(TAG, "Input id switching to is " + mInputInfo.getId());
        mListener.onButtonClick(this);
    }
}
