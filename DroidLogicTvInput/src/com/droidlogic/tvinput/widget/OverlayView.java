package com.droidlogic.tvinput.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.droidlogic.app.tv.DroidLogicOverlayView;
import com.droidlogic.tvinput.R;

public class OverlayView extends DroidLogicOverlayView {

    public OverlayView(Context context) {
        this(context, null);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void initSubView() {
        mImageView = (ImageView) findViewById(R.id.msg_image);
        mTextView = (TextView) findViewById(R.id.msg_text);
        mSubtitleView = (DTVSubtitleView) findViewById(R.id.subtitle);
        mEasTextView = (TextView) findViewById(R.id.msg_text_eas);
    }
}
