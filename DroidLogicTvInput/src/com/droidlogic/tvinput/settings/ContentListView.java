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
import com.droidlogic.tvclient.TvClient;
import android.amlogic.Tv;

public class ContentListView extends ListView implements OnItemSelectedListener {
    private static final String TAG = "ContentListView";
    private Context mContext;
    private int selectedPosition = 0;

    private TvClient client = TvClient.getTvClient();

    public ContentListView (Context context){
        super(context);
    }
    public ContentListView (Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        setOnItemSelectedListener(this);
    }

    public ContentListView (Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    public boolean dispatchKeyEvent (KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            OptionUiManager oum = ((TvSettingsActivity)mContext).getOptionUiManager();
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                String currentTag = ((TvSettingsActivity)mContext).getSettingsManager().getTag();
                if (selectedPosition == 0
                    || (client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_TV
                        && currentTag.equals(SettingsManager.KEY_CHANNEL) && selectedPosition == 2))
                    return true;

                oum.stopAllAction();
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (selectedPosition == getChildCount() -1)
                    return true;
                oum.stopAllAction();
            }else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                selectedPosition = 0;
                oum.stopAllAction();
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                setMenuAlpha(false);
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
            createOptionView(position);
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
            setMenuAlpha(true);
            setItemTextColor(getChildAt(selectedPosition), true);
            createOptionView(selectedPosition);
        } else {
            setItemTextColor(getChildAt(selectedPosition), false);
        }
    }

    private void createOptionView (int position) {
            RelativeLayout main_view = (RelativeLayout)((TvSettingsActivity)mContext).findViewById(R.id.main);
            View item_view = getChildAt(position);

            if (((TvSettingsActivity)mContext).mOptionLayout != null)
                main_view.removeView(((TvSettingsActivity)mContext).mOptionLayout);

            ((TvSettingsActivity)mContext).mOptionLayout = new RelativeLayout(mContext);

            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            Rect rect = new Rect();
            item_view.getGlobalVisibleRect(rect);
            lp.leftMargin=rect.right - dipToPx(mContext, 30f);

            //set optionview positon, 100f means make option up, 466f means option view backgroud png's height.
            if (rect.top - dipToPx(mContext, 100f) + dipToPx(mContext, 526f)
                <= mContext.getResources().getDisplayMetrics().heightPixels) {
                lp.topMargin = rect.top - dipToPx(mContext, 100f);
            } else {
                lp.topMargin = mContext.getResources().getDisplayMetrics().heightPixels - dipToPx(mContext, 526f);
            }
            ((TvSettingsActivity)mContext).mOptionLayout.setLayoutParams(lp);
            ((TvSettingsActivity)mContext).mOptionLayout.setBackgroundResource(R.drawable.background_option);

            main_view.addView(((TvSettingsActivity)mContext).mOptionLayout);
            createOptionChildView(((TvSettingsActivity)mContext).mOptionLayout, position);
    }

    private void createOptionChildView (View option_view, int position) {
        OptionUiManager oum = ((TvSettingsActivity)mContext).getOptionUiManager();
        LayoutInflater inflater =(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        oum.setOptionTag(position);
        int layout_option_child = oum.getLayoutId();
        if (layout_option_child > 0) {
            View view = inflater.inflate(layout_option_child, null);
            ((RelativeLayout)option_view).addView(view);
            oum.setProgressStatus();
            oum.setOptionListener(view);

            if (oum.getOptionTag() == OptionUiManager.OPTION_CHANNEL_INFO
                || oum.getOptionTag() == OptionUiManager.OPTION_AUDIO_TRACK
                || oum.getOptionTag() == OptionUiManager.OPTION_SOUND_CHANNEL) {
                OptionListLayout optionListLayout = new OptionListLayout(mContext, view, oum.getOptionTag());
            } else if (oum.getOptionTag() == OptionUiManager.OPTION_CHANNEL_EDIT) {
                ChannelEdit channelEdit = new ChannelEdit(mContext, view);
            }

            //set options view's focus
            if (oum.getOptionTag() == oum.OPTION_MANUAL_SEARCH) {
                oum.setManualSearchEditStyle(view);
            }
            View firstFocusableChild = null;
            View lastFocusableChild = null;
            for (int i = 0; i < ((ViewGroup)view).getChildCount(); i++) {
                View child = ((ViewGroup)view).getChildAt(i);
                if (child != null && child.hasFocusable()) {
                    if (firstFocusableChild == null) {
                        firstFocusableChild = child;
                    }
                    child.setNextFocusLeftId(R.id.content_list);
                    lastFocusableChild = child;
                }
            }
            if (firstFocusableChild != null && lastFocusableChild != null) {
                firstFocusableChild.setNextFocusUpId(firstFocusableChild.getId());
                lastFocusableChild.setNextFocusDownId(lastFocusableChild.getId());
            }
        }
    }

    public void setInitialSelection () {
        String currentTag = ((TvSettingsActivity)mContext).getSettingsManager().getTag();
        if ((client.curSource == Tv.SourceInput_Type.SOURCE_TYPE_TV
            && currentTag.equals(SettingsManager.KEY_CHANNEL))) {
            setSelection(2);
        }
    }

    public void setContentFocus (View relatedButton) {
        setNextFocusLeftId(relatedButton.getId());
        setNextFocusUpId(getId());
        setNextFocusDownId(getId());
    }

    private void setItemTextColor (View view, boolean focused) {
        TextView item_name = (TextView)view.findViewById(R.id.item_name);
        TextView item_status = (TextView)view.findViewById(R.id.item_status);
        if (focused) {
            int color_text_focused = mContext.getResources().getColor(R.color.color_text_focused);
            item_name.setTextColor(color_text_focused);
            item_status.setTextColor(color_text_focused);
        } else {
            int color_text_item = mContext.getResources().getColor(R.color.color_text_item);
            item_name.setTextColor(color_text_item);
            item_status.setTextColor(color_text_item);
        }
    }

    private void setMenuAlpha (boolean focused) {
        View view = ((TvSettingsActivity)mContext).findViewById(R.id.menu_and_content);
        if (focused) {
            view.getBackground().setAlpha(OptionUiManager.ALPHA_FOCUSED);
        } else {
            view.getBackground().setAlpha(OptionUiManager.ALPHA_NO_FOCUS);
        }
    }

    public static int pxToDip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int dipToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
