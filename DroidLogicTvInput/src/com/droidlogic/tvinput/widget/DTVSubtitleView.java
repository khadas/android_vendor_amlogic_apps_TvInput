package com.droidlogic.tvinput.widget;

import android.content.Context;
import android.os.Handler;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import java.lang.Exception;
import java.nio.ByteBuffer;
import java.util.Locale;

import android.util.Log;
import android.view.accessibility.CaptioningManager;
import android.widget.Toast;
import android.graphics.Bitmap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.droidlogic.tvinput.widget.CcImplement;

public class DTVSubtitleView extends View {
    private static final String TAG = "DTVSubtitleView";

    private static Object lock = new Object();
    private static final int BUFFER_W = 1920;
    private static final int BUFFER_H = 1080;
    private static int BUFFER_STRIDE;

    private static final int MODE_NONE = 0;
    private static final int MODE_DTV_TT = 1;
    private static final int MODE_DTV_CC = 2;
    private static final int MODE_DVB_SUB = 3;
    private static final int MODE_ATV_TT = 4;
    private static final int MODE_ATV_CC = 5;
    private static final int MODE_AV_CC = 6;

    private static final int EDGE_NONE = 0;
    private static final int EDGE_OUTLINE = 1;
    private static final int EDGE_DROP_SHADOW = 2;
    private static final int EDGE_RAISED = 3;
    private static final int EDGE_DEPRESSED = 4;

    private static final int PLAY_NONE = 0;
    private static final int PLAY_SUB = 1;
    private static final int PLAY_TT  = 2;

    public static final int COLOR_RED = 0;
    public static final int COLOR_GREEN = 1;
    public static final int COLOR_YELLOW = 2;
    public static final int COLOR_BLUE = 3;

    public static final int CC_CAPTION_DEFAULT = 0;
    /*NTSC CC channels*/
    public static final int CC_CAPTION_CC1 = 1;
    public static final int CC_CAPTION_CC2 = 2;
    public static final int CC_CAPTION_CC3 = 3;
    public static final int CC_CAPTION_CC4 = 4;
    public static final int CC_CAPTION_TEXT1 =5;
    public static final int CC_CAPTION_TEXT2 = 6;
    public static final int CC_CAPTION_TEXT3 = 7;
    public static final int CC_CAPTION_TEXT4 = 8;
    /*DTVCC services*/
    public static final int CC_CAPTION_SERVICE1 = 9;
    public static final int CC_CAPTION_SERVICE2 = 10;
    public static final int CC_CAPTION_SERVICE3 = 11;
    public static final int CC_CAPTION_SERVICE4 = 12;
    public static final int CC_CAPTION_SERVICE5 = 13;
    public static final int CC_CAPTION_SERVICE6 = 14;

    public static final int CC_FONTSIZE_DEFAULT = 0;
    public static final int CC_FONTSIZE_SMALL = 1;
    public static final int CC_FONTSIZE_STANDARD = 2;
    public static final int CC_FONTSIZE_BIG = 3;

    public static final int CC_FONTSTYLE_DEFAULT = 0;
    public static final int CC_FONTSTYLE_MONO_SERIF = 1;
    public static final int CC_FONTSTYLE_PROP_SERIF = 2;
    public static final int CC_FONTSTYLE_MONO_NO_SERIF = 3;
    public static final int CC_FONTSTYLE_PROP_NO_SERIF = 4;
    public static final int CC_FONTSTYLE_CASUAL = 5;
    public static final int CC_FONTSTYLE_CURSIVE = 6;
    public static final int CC_FONTSTYLE_SMALL_CAPITALS = 7;

    public static final int CC_OPACITY_DEFAULT = 0;
    public static final int CC_OPACITY_TRANSPARET = 1;
    public static final int CC_OPACITY_TRANSLUCENT= 2;
    public static final int CC_OPACITY_SOLID = 3;
    public static final int CC_OPACITY_FLASH = 4;

    public static final int CC_COLOR_DEFAULT = 0;
    public static final int CC_COLOR_WHITE = 1;
    public static final int CC_COLOR_BLACK = 2;
    public static final int CC_COLOR_RED = 3;
    public static final int CC_COLOR_GREEN = 4;
    public static final int CC_COLOR_BLUE = 5;
    public static final int CC_COLOR_YELLOW = 6;
    public static final int CC_COLOR_MAGENTA = 7;
    public static final int CC_COLOR_CYAN = 8;
    static final int VFMT_ATV = 100;
    private Paint clear_paint;

    public static final int JSON_MSG_NORMAL = 0;

    private static int init_count = 0;
    private static CaptioningManager captioningManager = null;
    private static CcImplement.CaptionWindow cw = null;
    private static CustomFonts cf = null;
    private static CcImplement ci = null;
    private static String json_str;
    private Paint mPaint;

    public static boolean cc_is_started = false;

    private boolean isPreWindowMode = false;

    private native int native_sub_init();
    private native int native_sub_destroy();
    private native int native_sub_lock();
    private native int native_sub_unlock();
    private native int native_sub_clear();
    private native int native_sub_start_dvb_sub(int dmx_id, int pid, int page_id, int anc_page_id);
    private native int native_sub_start_dtv_tt(int dmx_id, int region_id, int pid, int page, int sub_page, boolean is_sub);
    private native int native_sub_stop_dvb_sub();
    private native int native_sub_stop_dtv_tt();
    private native int native_sub_tt_goto(int page);
    private native int native_sub_tt_color_link(int color);
    private native int native_sub_tt_home_link();
    private native int native_sub_tt_next(int dir);
    private native int native_sub_tt_set_search_pattern(String pattern, boolean casefold);
    private native int native_sub_tt_search_next(int dir);
    protected native int native_get_subtitle_picture_width();
    protected native int native_get_subtitle_picture_height();
    private native int native_sub_start_atsc_cc(int vfmt, int caption, int fg_color, int fg_opacity, int bg_color, int bg_opacity, int font_style, int font_size);
    private native int native_sub_start_atsc_atvcc(int caption, int fg_color, int fg_opacity, int bg_color, int bg_opacity, int font_style, int font_size);
    private native int native_sub_stop_atsc_cc();
    private native static int native_sub_set_atsc_cc_options(int fg_color, int fg_opacity, int bg_color, int bg_opacity, int font_style, int font_size);
    private native int native_sub_set_active(boolean active);
    private native void native_set_buffer(ByteBuffer array);

    static {
        System.loadLibrary("am_adp");
        System.loadLibrary("am_mw");
        System.loadLibrary("zvbi");
        System.loadLibrary("jnidtvsubtitle");
    }

    static public class DVBSubParams {
        private int dmx_id;
        private int pid;
        private int composition_page_id;
        private int ancillary_page_id;
        public DVBSubParams(int dmx_id, int pid, int page_id, int anc_page_id) {
            this.dmx_id              = dmx_id;
            this.pid                 = pid;
            this.composition_page_id = page_id;
            this.ancillary_page_id   = anc_page_id;
        }
    }

    static public class DTVTTParams {
        private int dmx_id;
        private int pid;
        private int page_no;
        private int sub_page_no;
        private int region_id;

        public DTVTTParams(int dmx_id, int pid, int page_no, int sub_page_no, int region_id) {
            this.dmx_id      = dmx_id;
            this.pid         = pid;
            this.page_no     = page_no;
            this.sub_page_no = sub_page_no;
            this.region_id   = region_id;
        }
    }

    static public class ATVTTParams {
    }

    static public class DTVCCParams {
        protected int vfmt;
        protected int caption_mode;
        protected int fg_color;
        protected int fg_opacity;
        protected int bg_color;
        protected int bg_opacity;
        protected int font_style;
        protected float font_size;

        public DTVCCParams(int vfmt, int caption, int fg_color, int fg_opacity,
                int bg_color, int bg_opacity, int font_style, float font_size) {
            this.vfmt = vfmt;
            this.caption_mode = caption;
            this.fg_color = fg_color;
            this.fg_opacity = fg_opacity;
            this.bg_color = bg_color;
            this.bg_opacity = bg_opacity;
            this.font_style = font_style;
            this.font_size = font_size;
        }
    }

    static public class ATVCCParams extends DTVCCParams {
        public ATVCCParams(int caption, int fg_color, int fg_opacity,
                int bg_color, int bg_opacity, int font_style, float font_size) {
            super(VFMT_ATV, caption, fg_color, fg_opacity,
                    bg_color, bg_opacity, font_style, font_size);
        }
    }
    static public class AVCCParams extends DTVCCParams {
        public AVCCParams(int caption, int fg_color, int fg_opacity,
                int bg_color, int bg_opacity, int font_style, float font_size) {
            super(VFMT_ATV, caption, fg_color, fg_opacity,
                    bg_color, bg_opacity, font_style, font_size);
        }
    }
    private class SubParams {
        int mode;
        int vfmt;
        DVBSubParams dvb_sub;
        DTVTTParams  dtv_tt;
        ATVTTParams  atv_tt;
        DTVCCParams  dtv_cc;
        ATVCCParams  atv_cc;
        AVCCParams  av_cc;

        private SubParams() {
            mode = MODE_NONE;
        }
    }

    private class TTParams {
        int mode;
        DTVTTParams dtv_tt;
        ATVTTParams atv_tt;

        private TTParams() {
            mode = MODE_NONE;
        }
    }

    private int disp_left = 0;
    private int disp_right = 0;
    private int disp_top = 0;
    private int disp_bottom = 0;
    private boolean active = true;

    private static SubParams sub_params;
    private static TTParams  tt_params;
    private static int       play_mode;
    private boolean   visible;
    private boolean   destroy;
    private static Bitmap bitmap = null;
    private static ByteBuffer bitmap_bytebuffer;
    private static DTVSubtitleView activeView = null;
    private void update() {
        //Log.d(TAG, "update");
        bitmap_bytebuffer.rewind();
        bitmap.copyPixelsFromBuffer(bitmap_bytebuffer);
        postInvalidate();
    }

    private void stopDecoder() {
        synchronized(lock) {
            if (!cc_is_started)
                return;

            switch (play_mode) {
                case PLAY_NONE:
                    break;
                case PLAY_TT:
                    switch (tt_params.mode) {
                        case MODE_DTV_TT:
                            native_sub_stop_dtv_tt();
                            break;
                        default:
                            break;
                    }
                    break;
                case PLAY_SUB:
                    switch (sub_params.mode) {
                        case MODE_DTV_TT:
                            native_sub_stop_dtv_tt();
                            break;
                        case MODE_DVB_SUB:
                            native_sub_stop_dvb_sub();
                            break;
                        case MODE_DTV_CC:
                        case MODE_ATV_CC:
                        case MODE_AV_CC:
                            native_sub_stop_atsc_cc();
                            break;
                        default:
                            break;
                    }
                    break;
            }
            cc_is_started = false;
            play_mode = PLAY_NONE;
        }
    }

    private void init() {
        synchronized(lock) {
            if (init_count == 0) {
                play_mode = PLAY_NONE;
                visible = true;
                destroy = false;
                tt_params = new TTParams();
                sub_params = new SubParams();

                if (native_sub_init() < 0) {
                }

                mPaint = new Paint();
                bitmap = Bitmap.createBitmap(BUFFER_W, BUFFER_H, Bitmap.Config.ARGB_8888);
                bitmap_bytebuffer = ByteBuffer.allocateDirect(BUFFER_H * BUFFER_W * 4);
                Log.e(TAG, "bitmap_bytebuffer " + bitmap_bytebuffer.capacity());
                native_set_buffer(bitmap_bytebuffer);

                //setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                cf = new CustomFonts(getContext());
                if (ci == null) {
                    ci = new CcImplement(getContext(), cf);
                }
                captioningManager = (CaptioningManager) getContext().getSystemService(Context.CAPTIONING_SERVICE);
                captioningManager.addCaptioningChangeListener(new CaptioningManager.CaptioningChangeListener() {
                    @Override
                    public void onEnabledChanged(boolean enabled) {
                        super.onEnabledChanged(enabled);
                        Log.e(TAG, "onenableChange");
                        ci.cc_setting.is_enabled = captioningManager.isEnabled();
                    }

                    @Override
                    public void onFontScaleChanged(float fontScale) {
                        super.onFontScaleChanged(fontScale);
                        Log.e(TAG, "onfontscaleChange");
                        ci.cc_setting.font_scale = captioningManager.getFontScale();
                    }

                    @Override
                    public void onLocaleChanged(Locale locale) {
                        super.onLocaleChanged(locale);
                        Log.e(TAG, "onlocaleChange");
                        ci.cc_setting.cc_locale = captioningManager.getLocale();
                    }

                    @Override
                    public void onUserStyleChanged(CaptioningManager.CaptionStyle userStyle) {
                        super.onUserStyleChanged(userStyle);
                        Log.e(TAG, "onUserStyleChange");
                        ci.cc_setting.has_foreground_color = userStyle.hasForegroundColor();
                        ci.cc_setting.has_background_color = userStyle.hasBackgroundColor();
                        ci.cc_setting.has_window_color = userStyle.hasWindowColor();
                        ci.cc_setting.has_edge_color = userStyle.hasEdgeColor();
                        ci.cc_setting.has_edge_type = userStyle.hasEdgeType();
                        ci.cc_setting.edge_type = userStyle.edgeType;
                        ci.cc_setting.edge_color = userStyle.edgeColor;
                        ci.cc_setting.foreground_color = userStyle.foregroundColor;
                        ci.cc_setting.foreground_opacity = userStyle.foregroundColor >>> 24;
                        ci.cc_setting.background_color = userStyle.backgroundColor;
                        ci.cc_setting.background_opacity = userStyle.backgroundColor >>> 24;
                        ci.cc_setting.window_color = userStyle.windowColor;
                        ci.cc_setting.window_opacity = userStyle.windowColor >>> 24;
                        /* Typeface is obsolete, we use local font */
                        ci.cc_setting.type_face = userStyle.getTypeface();
                    }
                });
                ci.cc_setting.UpdateCcSetting(captioningManager);
                Log.e(TAG, "subtitle view init");

            }
            init_count = 1;
        }
    }

    public DTVSubtitleView(Context context) {
        super(context);
        init();
    }

    public DTVSubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DTVSubtitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setMargin(int left, int top, int right, int bottom) {
        disp_left   = left;
        disp_top    = top;
        disp_right  = right;
        disp_bottom = bottom;
    }

    public void setActive(boolean active) {
        synchronized (lock) {
            if (active && (activeView != this) && (activeView != null)) {
                activeView.stopDecoder();
                activeView.active = false;
            }
            native_sub_set_active(active);
            this.active = active;
            if (active) {
                activeView = this;
                /*}else if (activeView == this){
                  activeView = null;*/
            }
            if (!isPreWindowMode)
                postInvalidate();
        }
    }

    public void setSubParams(DVBSubParams params) {
        synchronized(lock) {
            sub_params.mode = MODE_DVB_SUB;
            sub_params.dvb_sub = params;

            if (play_mode == PLAY_SUB)
                startSub();
        }
    }

    public void setSubParams(DTVTTParams params) {
        synchronized(lock) {
            sub_params.mode = MODE_DTV_TT;
            sub_params.dtv_tt = params;

            if (play_mode == PLAY_SUB)
                startSub();
        }
    }

    public void setSubParams(DTVCCParams params) {
        synchronized(lock) {
            sub_params.mode = MODE_DTV_CC;
            sub_params.dtv_cc = params;
            sub_params.vfmt = params.vfmt;

            if (play_mode == PLAY_SUB)
                startSub();
        }
    }

    public void setSubParams(ATVCCParams params) {
        synchronized(lock) {
            sub_params.mode = MODE_ATV_CC;
            sub_params.atv_cc = params;

            if (play_mode == PLAY_SUB)
                startSub();
        }
    }

    public void setSubParams(AVCCParams params) {
        synchronized(lock) {
            sub_params.mode = MODE_AV_CC;
            sub_params.av_cc = params;

            if (play_mode == PLAY_SUB)
                startSub();
        }
    }

    public void setTTParams(DTVTTParams params) {
        synchronized(lock) {
            tt_params.mode = MODE_DTV_TT;
            tt_params.dtv_tt = params;

            if (play_mode == PLAY_TT)
                startTT();
        }
    }

    public void setCaptionParams(DTVCCParams params) {
        synchronized(lock) {
            sub_params.dtv_cc = params;
            native_sub_set_atsc_cc_options(
                    params.fg_color,
                    params.fg_opacity,
                    params.bg_color,
                    params.bg_opacity,
                    params.font_style,
                    new Float(params.font_size).intValue());
        }
    }

    public void show() {
        if (visible)
            return;

        Log.d(TAG, "show");

        visible = true;
        update();
    }

    public void hide() {
        if (!visible)
            return;

        Log.d(TAG, "hide");

        visible = false;
        update();
    }

    public void startTT() {
        synchronized(lock) {
            if (activeView != this)
                return;
            stopDecoder();

            if (tt_params.mode == MODE_NONE)
                return;

            int ret = 0;
            switch (tt_params.mode) {
                case MODE_DTV_TT:
                    ret = native_sub_start_dtv_tt(tt_params.dtv_tt.dmx_id,
                            tt_params.dtv_tt.region_id,
                            tt_params.dtv_tt.pid,
                            tt_params.dtv_tt.page_no,
                            tt_params.dtv_tt.sub_page_no,
                            false);
                    break;
                default:
                    break;
            }

            if (ret >= 0)
                play_mode = PLAY_TT;
        }
    }

    public void startSub() {
        synchronized(lock) {
            if (activeView != this)
                return;

            stopDecoder();

            if (sub_params.mode == MODE_NONE)
                return;

            int ret = 0;
            switch (sub_params.mode) {
                case MODE_DVB_SUB:
                    ret = native_sub_start_dvb_sub(sub_params.dvb_sub.dmx_id,
                            sub_params.dvb_sub.pid,
                            sub_params.dvb_sub.composition_page_id,
                            sub_params.dvb_sub.ancillary_page_id);
                    break;
                case MODE_DTV_TT:
                    ret = native_sub_start_dtv_tt(sub_params.dtv_tt.dmx_id,
                            sub_params.dtv_tt.region_id,
                            sub_params.dtv_tt.pid,
                            sub_params.dtv_tt.page_no,
                            sub_params.dtv_tt.sub_page_no,
                            true);
                    break;
                case MODE_DTV_CC:
                    ret = native_sub_start_atsc_cc(
                            sub_params.vfmt,
                            sub_params.dtv_cc.caption_mode,
                            sub_params.dtv_cc.fg_color,
                            sub_params.dtv_cc.fg_opacity,
                            sub_params.dtv_cc.bg_color,
                            sub_params.dtv_cc.bg_opacity,
                            sub_params.dtv_cc.font_style,
                            new Float(sub_params.dtv_cc.font_size).intValue());
                    break;
                case MODE_ATV_CC:
                    ret = native_sub_start_atsc_atvcc(
                            sub_params.atv_cc.caption_mode,
                            sub_params.atv_cc.fg_color,
                            sub_params.atv_cc.fg_opacity,
                            sub_params.atv_cc.bg_color,
                            sub_params.atv_cc.bg_opacity,
                            sub_params.atv_cc.font_style,
                            new Float(sub_params.atv_cc.font_size).intValue());
                    break;
                case MODE_AV_CC:
                    ret = native_sub_start_atsc_atvcc(
                            sub_params.av_cc.caption_mode,
                            sub_params.av_cc.fg_color,
                            sub_params.av_cc.fg_opacity,
                            sub_params.av_cc.bg_color,
                            sub_params.av_cc.bg_opacity,
                            sub_params.av_cc.font_style,
                            new Float(sub_params.av_cc.font_size).intValue());
                    break;
                default:
                    ret = -1;
                    break;
            }

            if (ret >= 0) {
                cc_is_started = true;
                play_mode = PLAY_SUB;
            }
        }
    }

    public void stop() {
        synchronized(lock) {
            if (activeView != this)
                return;
            stopDecoder();
        }
    }

    public void clear() {
        synchronized(lock) {
            if (activeView != this)
                return;
            stopDecoder();
            native_sub_clear();
            tt_params.mode  = MODE_NONE;
            sub_params.mode = MODE_NONE;
        }
    }

    public void nextPage() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_next(1);
        }
    }

    public void previousPage() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_next(-1);
        }
    }

    public void gotoPage(int page) {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_goto(page);
        }
    }

    public void goHome() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_home_link();
        }
    }

    public void colorLink(int color) {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_color_link(color);
        }
    }

    public void setSearchPattern(String pattern, boolean casefold) {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_set_search_pattern(pattern, casefold);
        }
    }

    public void searchNext() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_search_next(1);
        }
    }

    public void searchPrevious() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_search_next(-1);
        }
    }


    /**
     * set the flag to indecate the preview window mode
     * @param flag [description]
     */
    public void setPreviewWindowMode(boolean flag) {
        isPreWindowMode = flag;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        ci.caption_screen.updateCaptionScreen(w, h);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect sr, dr;
        String json_data;

        switch (sub_params.mode)
        {
            case MODE_AV_CC:
            case MODE_DTV_CC:
            case MODE_ATV_CC:
                /* For atsc */
                if (!active || !visible || (play_mode == PLAY_NONE)) {
                    /* Clear canvas */
                    if (ci == null)
                        return;
                    ci.window_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                    canvas.drawPaint(ci.window_paint);
                    ci.window_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                    cw = null;
                    return;
                }
//                if (!ci.cc_setting.is_enabled)
//                    return;
                ci.caption_screen.updateCaptionScreen(canvas.getWidth(), canvas.getHeight());

                if (cw != null)
                    cw.draw(canvas);
                break;
            case MODE_DTV_TT:
            case MODE_DVB_SUB:
            case MODE_ATV_TT:
            /* Not atsc */
                native_sub_lock();
                dr = new Rect(disp_left, disp_top, getWidth() - disp_right, getHeight() - disp_bottom);
                if (play_mode == PLAY_TT || sub_params.mode == MODE_DTV_TT || sub_params.mode == MODE_ATV_TT) {
                    sr = new Rect(0, 0, 12 * 41, 10 * 25);
                } else if (play_mode == PLAY_SUB) {
                    sr = new Rect(0, 0, native_get_subtitle_picture_width(), native_get_subtitle_picture_height());
                } else {
                    sr = new Rect(0, 0, BUFFER_W, BUFFER_H);
                }
                canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                canvas.drawBitmap(bitmap, sr, dr, mPaint);
                native_sub_unlock();
                break;
            default:
                break;
        };
    }

    public void dispose() {
        synchronized(lock) {
            if (!destroy) {
                destroy = true;
                if (init_count == 0) {
                    stopDecoder();
                    native_sub_clear();
                    native_sub_destroy();
                    bitmap_bytebuffer = null;
                }
            }
        }
    }

    protected void finalize() throws Throwable {
        // Resource may not be available during gc process
        // dispose();
        super.finalize();
    }

    public void setVisible(boolean value) {
        Log.d(TAG, "force set visible to:" + value);
        visible = value;
        postInvalidate();
    }

    private SubtitleDataListener mSubtitleDataListener = null;
    Handler handler = new Handler()
    {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case JSON_MSG_NORMAL:
                    cw = (CcImplement.CaptionWindow)msg.obj;
                    postInvalidate();
                    break;
            }
        }
    };

    public void saveJsonStr(String str) {
        if (activeView != this)
            return;

        if (!TextUtils.isEmpty(str)) {
            handler.obtainMessage(JSON_MSG_NORMAL, ci.new CaptionWindow(str)).sendToTarget();
        }
    }

    public void updateData(String json) {
        if (mSubtitleDataListener != null) {
            mSubtitleDataListener.onSubtitleData(json);
        }
    }

    public String readSysFs(String name) {
        String value = null;
        if (mSubtitleDataListener != null) {
            value = mSubtitleDataListener.onReadSysFs(name);
        }
        return value;
    }

    public void writeSysFs(String name, String cmd) {
        if (mSubtitleDataListener != null) {
            mSubtitleDataListener.onWriteSysFs(name, cmd);
        }
    }

    public void setSubtitleDataListener(SubtitleDataListener l) {
        mSubtitleDataListener = l;
    }

    public interface SubtitleDataListener {
        public void onSubtitleData(String json);
        public String onReadSysFs(String node);
        public void onWriteSysFs(String node, String value);
    }

}

