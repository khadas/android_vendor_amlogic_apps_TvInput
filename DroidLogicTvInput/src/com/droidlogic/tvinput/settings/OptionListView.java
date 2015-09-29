package com.droidlogic.tvinput.settings;

import android.app.Activity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.util.AttributeSet;
import android.util.Log;

import com.droidlogic.tvinput.R;

public class OptionListView extends ListView implements OnItemSelectedListener {
    private static final String TAG = "ContentListView";
    private Context mContext;
    private int selectedPosition = 0;

    public OptionListView (Context context){
        super(context);
    }
    public OptionListView (Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        setOnItemSelectedListener(this);
    }

    public boolean dispatchKeyEvent (KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                selectedPosition = 0;
            }

            View selectedView = getSelectedView();
            if ( selectedView != null
                && !(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                    || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                setItemTextColor(selectedView, false);
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedPosition = position;
        if (hasFocus()) {
            setItemTextColor(view, true);
        } else {
            setItemTextColor(view, false);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
	}

    @Override
    protected void onFocusChanged (boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus) {
            setItemTextColor(getChildAt(selectedPosition), true);
        } else {
            setItemTextColor(getChildAt(selectedPosition), false);
        }
    }

    private void setItemTextColor (View view, boolean focused) {
        TextView item_name = (TextView)((ViewGroup)view).getChildAt(0);

        if (focused) {
            int color_text_focused = mContext.getResources().getColor(R.color.color_text_focused);
            item_name.setTextColor(color_text_focused);
        } else {
            int color_text_item = mContext.getResources().getColor(R.color.color_text_item);
            item_name.setTextColor(color_text_item);
        }
    }
}

