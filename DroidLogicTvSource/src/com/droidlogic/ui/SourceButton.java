package com.droidlogic.ui;

import com.droidlogic.tv.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.tv.TvInputInfo;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class SourceButton extends Button {

    private Context mContext;
    private TvInputInfo mInputInfo;
    private int mSourceId;

    public SourceButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SOURCE_BUTTON);
        mSourceId = array.getInteger(R.styleable.SOURCE_BUTTON_source_id, 0);
        array.recycle();
    }

    public SourceButton(Context context, TvInputInfo info) {
        super(context);
        mContext = context;
        init();
    }

    private void init() {
        ensureValidField(mInputInfo);
        setText(mInputInfo.loadLabel(mContext));
//        if(mInputInfo.isHidden(mContext)) {
//            if (getResources().getBoolean(R.bool.source_need_gone)) {
//                setVisibility(View.GONE);
//            }else {
//                setSelected(false);
//                setFocusable(false);
//                setClickable(false);
//            }
//        }
    }

    private void ensureValidField(TvInputInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("wrong argument info...");
        }else {
            mInputInfo = info;
        }
    }

    public int getSourceType() {
        return mSourceId;
    }

    public void setTvInputInfo(TvInputInfo info) {
        mInputInfo = info;
        init();
    }
}
