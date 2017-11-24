package com.droidlogic.tvinput.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import java.lang.Exception;
import java.util.Locale;

import android.util.Log;
import android.view.accessibility.CaptioningManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.droidlogic.tvinput.widget.CcImplement;
/**
 * DTVSubtitleView提供数字/模拟电视字幕和图文信息支持.
 * 目前支持DVB subtitle, DTV/ATV teletext, ATSC/NTSC closed caption.
 */
public class DTVSubtitleView extends View {
    private static final String TAG = "DTVSubtitleView";

    private static Object lock = new Object();
    private static final int BUFFER_W = 1920;
    private static final int BUFFER_H = 1080;

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

    private int init_count = 0;
    public String cc_json_str;
    CaptioningManager captioningManager;
    CustomFonts cf;
    CcImplement ci;

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
    private native int native_sub_start_atsc_cc(int caption, int fg_color, int fg_opacity, int bg_color, int bg_opacity, int font_style, int font_size);
    private native int native_sub_start_atsc_atvcc(int caption, int fg_color, int fg_opacity, int bg_color, int bg_opacity, int font_style, int font_size);
    private native int native_sub_stop_atsc_cc();
    private native static int native_sub_set_atsc_cc_options(int fg_color, int fg_opacity, int bg_color, int bg_opacity, int font_style, int font_size);
    private native int native_sub_set_active(boolean active);

    static {
        System.loadLibrary("am_adp");
        System.loadLibrary("am_mw");
        System.loadLibrary("zvbi");
        System.loadLibrary("jnidtvsubtitle");
    }

    /**
     * DVB subtitle 参数
     */
    static public class DVBSubParams {
        private int dmx_id;
        private int pid;
        private int composition_page_id;
        private int ancillary_page_id;

        /**
         * 创建DVB subtitle参数
         * @param dmx_id 接收使用demux设备的ID
         * @param pid subtitle流的PID
         * @param page_id 字幕的page_id
         * @param anc_page_id 字幕的ancillary_page_id
         */
        public DVBSubParams(int dmx_id, int pid, int page_id, int anc_page_id) {
            this.dmx_id              = dmx_id;
            this.pid                 = pid;
            this.composition_page_id = page_id;
            this.ancillary_page_id   = anc_page_id;
        }
    }

    /**
     * 数字电视teletext图文参数
     */
    static public class DTVTTParams {
        private int dmx_id;
        private int pid;
        private int page_no;
        private int sub_page_no;
        private int region_id;

        /**
         * 创建数字电视teletext图文参数
         * @param dmx_id 接收使用demux设备的ID
         * @param pid 图文信息流的PID
         * @param page_no 要显示页号
         * @param sub_page_no 要显示的子页号
         */
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
        protected int caption_mode;
        protected int fg_color;
        protected int fg_opacity;
        protected int bg_color;
        protected int bg_opacity;
        protected int font_style;
        protected float font_size;

        public DTVCCParams(int caption, int fg_color, int fg_opacity,
                int bg_color, int bg_opacity, int font_style, float font_size) {
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
            super(caption, fg_color, fg_opacity,
                    bg_color, bg_opacity, font_style, font_size);
        }
    }
    static public class AVCCParams extends DTVCCParams {
        public AVCCParams(int caption, int fg_color, int fg_opacity,
                           int bg_color, int bg_opacity, int font_style, float font_size) {
            super(caption, fg_color, fg_opacity,
                           bg_color, bg_opacity, font_style, font_size);
        }
    }
    private class SubParams {
        int mode;
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
    private static boolean   visible;
    private static boolean   destroy;
    private static DTVSubtitleView activeView = null;
    private void update() {
        //Log.e(TAG, "update");
        postInvalidate();
    }


    private void stopDecoder() {
        synchronized(lock) {
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

            play_mode = PLAY_NONE;
        }
    }

    private void init() {
        synchronized(lock) {
            if (init_count == 0) {
                play_mode  = PLAY_NONE;
                visible    = true;
                destroy    = false;
                tt_params  = new TTParams();
                sub_params = new SubParams();

                if (native_sub_init() < 0) {
                }
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                cf = new CustomFonts(getContext());
                if (ci == null) {
                    ci = new CcImplement(getContext(), cf);
                }
                captioningManager = (CaptioningManager) getContext().getSystemService(Context.CAPTIONING_SERVICE);
                ci.cc_setting.UpdateCcSetting(captioningManager);
                Log.e(TAG, "subtitle view init");

            }
            init_count++;
        }
    }

    /**
     * 创建TVSubtitle控件
     */
    public DTVSubtitleView(Context context) {
        super(context);
        init();
    }

    /**
     * 创建TVSubtitle控件
     */
    public DTVSubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * 创建TVSubtitle控件
     */
    public DTVSubtitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     *设定显示边缘空隙
     *@param left 左边缘宽度
     *@param top 顶部边缘高度
     *@param right 右部边缘宽度
     *@param bottom 底部边缘高度
     */
    public void setMargin(int left, int top, int right, int bottom) {
        disp_left   = left;
        disp_top    = top;
        disp_right  = right;
        disp_bottom = bottom;
    }

    /**
     * 设定控件活跃状态
     * @param active 活跃/不活跃
     */
    public void setActive(boolean active) {
        synchronized(lock) {
            native_sub_set_active(active);
            this.active = active;
            if (active) {
                activeView = this;
                /*}else if (activeView == this){
                  activeView = null;*/
                }
        postInvalidate();
        }
    }

    /**
     * 设定字幕参数
     * @param params 字幕参数
     */
    public void setSubParams(DVBSubParams params) {
        synchronized(lock) {
            sub_params.mode = MODE_DVB_SUB;
            sub_params.dvb_sub = params;

            if (play_mode == PLAY_SUB)
                startSub();
        }
    }

    /**
     * 设定字幕参数
     * @param params 字幕参数
     */
    public void setSubParams(DTVTTParams params) {
        synchronized(lock) {
            sub_params.mode = MODE_DTV_TT;
            sub_params.dtv_tt = params;

            if (play_mode == PLAY_SUB)
                startSub();
        }
    }

    /**
     * 设定close caption字幕参数
     * @param params 字幕参数
     */
    public void setSubParams(DTVCCParams params) {
        synchronized(lock) {
            sub_params.mode = MODE_DTV_CC;
            sub_params.dtv_cc = params;

            if (play_mode == PLAY_SUB)
                startSub();
        }
    }

    /**
     * 设定close caption字幕参数
     * @param params 字幕参数
     */
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
    /**
     * 设定图文参数
     * @param params 字幕参数
     */
    public void setTTParams(DTVTTParams params) {
        synchronized(lock) {
            tt_params.mode = MODE_DTV_TT;
            tt_params.dtv_tt = params;

            if (play_mode == PLAY_TT)
                startTT();
        }
    }

    public static void setCaptionParams(DTVCCParams params) {
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

    /**
     * 显示字幕/图文信息
     */
    public void show() {
        if (visible)
            return;

        Log.d(TAG, "show");

        visible = true;
        update();
    }

    /**
     * 隐藏字幕/图文信息
     */
    public void hide() {
        if (!visible)
            return;

        Log.d(TAG, "hide");

        visible = false;
        update();
    }

    /**
     * 开始图文信息解析
     */
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

    /**
     * 开始字幕信息解析
     */
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
                    break;
            }

            if (ret >= 0)
                play_mode = PLAY_SUB;
        }
    }

    /**
     * 停止图文/字幕信息解析
     */
    public void stop() {
        synchronized(lock) {
            if (activeView != this)
                return;
            stopDecoder();
        }
    }

    /**
     * 停止图文/字幕信息解析并清除缓存数据
     */
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

    /**
     * 在图文模式下进入下一页
     */
    public void nextPage() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_next(1);
        }
    }

    /**
     * 在图文模式下进入上一页
     */
    public void previousPage() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_next(-1);
        }
    }

    /**
     * 在图文模式下跳转到指定页
     * @param page 要跳转到的页号
     */
    public void gotoPage(int page) {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_goto(page);
        }
    }

    /**
     * 在图文模式下跳转到home页
     */
    public void goHome() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_home_link();
        }
    }

    /**
     * 在图文模式下根据颜色跳转到指定链接
     * @param color 颜色，COLOR_RED/COLOR_GREEN/COLOR_YELLOW/COLOR_BLUE
     */
    public void colorLink(int color) {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_color_link(color);
        }
    }

    /**
     * 在图文模式下设定搜索字符串
     * @param pattern 搜索匹配字符串
     * @param casefold 是否区分大小写
     */
    public void setSearchPattern(String pattern, boolean casefold) {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_set_search_pattern(pattern, casefold);
        }
    }

    /**
     * 搜索下一页
     */
    public void searchNext() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_search_next(1);
        }
    }

    /**
     * 搜索上一页
     */
    public void searchPrevious() {
        synchronized(lock) {
            if (activeView != this)
                return;
            if (play_mode != PLAY_TT)
                return;

            native_sub_tt_search_next(-1);
        }
    }
    @Override
    public void onDraw(Canvas canvas) {
        Rect sr;
        Rect dr = new Rect(disp_left, disp_top, getWidth() - disp_right, getHeight() - disp_bottom);
        if (!active || !visible || (play_mode == PLAY_NONE)) {
            /* Clear canvas */
            Paint clear_paint = new Paint();
            clear_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawPaint(clear_paint);
            clear_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            return;
        }
        if (captioningManager == null) {
            Log.e(TAG, "captionManager is null!");
            return;
        }
        if (captioningManager.isEnabled() == false) {
            Log.e(TAG, "captionManager is false!");
            return;
        }

        if (play_mode == PLAY_TT || sub_params.mode == MODE_DTV_TT || sub_params.mode == MODE_ATV_TT) {
            sr = new Rect(0, 0, 12 * 41, 10 * 25);
        } else if (play_mode == PLAY_SUB) {
            sr = new Rect(0, 0, native_get_subtitle_picture_width(), native_get_subtitle_picture_height());
        } else {
            sr = new Rect(0, 0, BUFFER_W, BUFFER_H);
        }

        ci.caption_screen.updateCaptionScreen(canvas.getWidth(), canvas.getHeight());
        ci.cc_setting.UpdateCcSetting(captioningManager);
        CcImplement.CaptionWindow cw;
        if (cc_json_str != null) {
            Log.e(TAG, "json str: " + cc_json_str);
            cw = ci.new CaptionWindow(cc_json_str);
            cw.draw(canvas);
        }
//        native_sub_lock();
//        native_sub_unlock();
    }

    public void dispose() {
        synchronized(lock) {
            if (!destroy) {
                init_count--;
                destroy = true;
                if (init_count == 0) {
                    stopDecoder();
                    native_sub_clear();
                    native_sub_destroy();
                }
            }
        }
    }

    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    public void setVisible(boolean value) {
        Log.d(TAG, "force set visible to:" + value);
        visible = value;
    }

    private SubtitleDataListener mSubtitleDataListener = null;

    public void saveJsonStr(String str) {
        this.cc_json_str = str;
    }

    public void updateData(String json) {
        if (mSubtitleDataListener != null) {
            mSubtitleDataListener.onSubtitleData(json);
        }
    }

    public void setSubtitleDataListener(SubtitleDataListener l) {
        mSubtitleDataListener = l;
    }

    public interface SubtitleDataListener {
        public void onSubtitleData(String json);
    }

}

