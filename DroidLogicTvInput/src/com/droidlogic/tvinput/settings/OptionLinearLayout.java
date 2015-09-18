package com.droidlogic.tvinput.settings;

import android.content.Context;
import android.graphics.Rect;
import android.widget.LinearLayout;
import android.util.AttributeSet;
import android.util.Log;

public class OptionLinearLayout extends LinearLayout {
    private static final String TAG = "ChoiceLinearLayout";

    private Context mContext;

    public OptionLinearLayout (Context context, int positon){
        super(context);

        mContext = context;
    }

    public OptionLinearLayout (Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFocusChanged (boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus)
            Log.d(TAG, "@@@@@@@@@@@@@ gainFocus=" + direction);
    }
}

