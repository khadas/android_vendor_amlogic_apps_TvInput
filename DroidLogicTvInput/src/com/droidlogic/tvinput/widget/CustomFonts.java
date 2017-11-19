package com.droidlogic.tvinput.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

/**
 * Created by daniel on 06/11/2017.
 */

public class CustomFonts {
    final String TAG = "CustomFonts";
    Typeface mono_serif_tf;
    Typeface mono_serif_it_tf;
    Typeface casual_tf;
    Typeface casual_it_tf;
    Typeface prop_sans_tf;
    Typeface prop_sans_it_tf;
    Typeface small_capital_tf;
    Typeface small_capital_it_tf;
    CustomFonts(Context context)
    {
        try {
            mono_serif_tf = Typeface.createFromAsset(context.getAssets(), "fonts/cinecavD_mono.ttf");
            mono_serif_it_tf = Typeface.createFromAsset(context.getAssets(), "fonts/cinecavD_mono_it.ttf");
            casual_tf = Typeface.createFromAsset(context.getAssets(), "fonts/cinecavD_casual.ttf");
            casual_it_tf = Typeface.createFromAsset(context.getAssets(), "fonts/cinecavD_casual_it.ttf");
            prop_sans_tf = Typeface.createFromAsset(context.getAssets(), "fonts/cinecavD_serif.ttf");
            prop_sans_it_tf = Typeface.createFromAsset(context.getAssets(), "fonts/cinecavD_serif_it.ttf");
            small_capital_tf = Typeface.createFromAsset(context.getAssets(), "fonts/cinecavD_sc.ttf");
            small_capital_it_tf = Typeface.createFromAsset(context.getAssets(), "fonts/cinecavD_sc_it.ttf");
        } catch (Exception e)
        {
            Log.e(TAG, "error " + e.toString());
        }
    }
}
