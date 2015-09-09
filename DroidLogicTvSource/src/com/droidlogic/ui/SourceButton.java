package com.droidlogic.ui;

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
        setText(getSourceBtLabel());
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
        setOnClickListener(this);
    }

    public interface SourceButtonListener{
        void onButtonClick(String inputId, String sourceType);
    }

    public void setSourceButttonListener(SourceButtonListener l) {
        mListener = l;
    }

    public int getDeviceId() {
        return mHardwareDeviceId;
    }

    private String getSourceBtLabel() {
        return mInputInfo.loadLabel(mContext).toString();
    }

    public boolean isHardware() {
        boolean ret = false;
        String[] temp = mInputInfo.getId().split("/");
        if (temp.length == 3) {
            mHardwareDeviceId = Integer.parseInt(temp[2].substring(2));
            ret = true;
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

    @Override
    public void onClick(View v) {
        Utils.logd(TAG, "Input id switching to is " + mInputInfo.getId());
        mListener.onButtonClick(mInputInfo.getId(), getSourceBtLabel());
    }
}
