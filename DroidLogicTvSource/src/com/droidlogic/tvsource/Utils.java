package com.droidlogic.tvsource;


import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

import com.droidlogic.app.tv.DroidLogicTvUtils;

public class Utils {
    private static final boolean DEBUG = true;

    public static final boolean SHOW_VIEW = true;
    public static final boolean HIDE_VIEW = false;

    public static final int UI_TYPE_SOURCE_INFO = 0;
    public static final int UI_TYPE_SOURCE_LIST = 1;
    public static final int UI_TYPE_ATV_CHANNEL_LIST = 2;
    public static final int UI_TYPE_DTV_CHANNEL_LIST = 3;
    public static final int UI_TYPE_ATV_FAV_LIST = 4;
    public static final int UI_TYPE_DTV_FAV_LIST = 5;
    public static final int UI_TYPE_NO_SINAL = 6;
    public static final int UI_TYPE_ALL_HIDE = 7;
    public static final int UI_TYPE_DTV_INFO = 8;
    /**These strings are used to construct TvInputInfo, now we use them to parse TvInputInfo**/
    public static final String DELIMITER_INFO_IN_ID = "/";
    public static final String PREFIX_HDMI_DEVICE = "HDMI";
    public static final String PREFIX_HARDWARE_DEVICE = "HW";

    @SuppressLint("UseSparseArrays")
    public static final Map<Integer, String> getVideoMap(){
        Map<Integer, String> mVideoFormat = new HashMap<Integer, String>();
        mVideoFormat.put(-1, "UNKNOWN");
        mVideoFormat.put(0, "MPEG12");
        mVideoFormat.put(1, "MPEG4");
        mVideoFormat.put(2, "H264");
        mVideoFormat.put(3, "MJPEG");
        mVideoFormat.put(4, "REAL");
        mVideoFormat.put(5, "JPEG");
        mVideoFormat.put(6, "VC1");
        mVideoFormat.put(7, "AVS");
        mVideoFormat.put(8, "SW");
        mVideoFormat.put(9, "H264MVC");
        mVideoFormat.put(10, "H264_4K2K");
        mVideoFormat.put(11, "HEVC");
        mVideoFormat.put(12, "H264_ENC");
        mVideoFormat.put(13, "JPEG_ENC");
        mVideoFormat.put(14, "VP9");
        return mVideoFormat;
    }

    @SuppressLint("UseSparseArrays")
    public static final Map<Integer, String> getAudioMap(){
        Map<Integer, String> mAudioFormat = new HashMap<Integer, String>();
        mAudioFormat.put(-1, "UNKNOWN");
        mAudioFormat.put(0, "MPEG");
        mAudioFormat.put(1, "PCM_S16LE");
        mAudioFormat.put(2, "AAC");
        mAudioFormat.put(3, "AC3");
        mAudioFormat.put(4, "ALAW");
        mAudioFormat.put(5, "MULAW");
        mAudioFormat.put(6, "DTS");
        mAudioFormat.put(7, "PCM_S16BE");
        mAudioFormat.put(8, "FLAC");
        mAudioFormat.put(9, "COOK");
        mAudioFormat.put(10, "PCM_U8");
        mAudioFormat.put(11, "ADPCM");
        mAudioFormat.put(12, "AMR");
        mAudioFormat.put(13, "RAAC");
        mAudioFormat.put(14, "WMA");
        mAudioFormat.put(15, "WMAPRO");
        mAudioFormat.put(16, "PCM_BLURAY");
        mAudioFormat.put(17, "ALAC");
        mAudioFormat.put(18, "VORBIS");
        mAudioFormat.put(19, "AAC_LATM");
        mAudioFormat.put(20, "APE");
        mAudioFormat.put(21, "EAC3");
        mAudioFormat.put(22, "PCM_WIFIDISPLAY");
        mAudioFormat.put(23, "DRA");
        mAudioFormat.put(24, "SIPR");
        mAudioFormat.put(25, "TRUEHD");
        mAudioFormat.put(26, "MPEG1");
        mAudioFormat.put(27, "MPEG2");
        mAudioFormat.put(28, "WMAVOI");
        mAudioFormat.put(29, "WMALOSSLESS");
        mAudioFormat.put(30, "UNSUPPORT");
        mAudioFormat.put(31, "MAX");
        return mAudioFormat;
    }

    public static void logd(String tag, String msg) {
        if (DEBUG)
            Log.d(tag, msg);
    }

    public static void loge(String tag, String msg) {
        if (DEBUG)
            Log.e(tag, msg);
    }

    public static int getChannelId(Uri uri) {
        return DroidLogicTvUtils.getChannelId(uri);
    }

}
