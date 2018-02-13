#include <pthread.h>
#include <core/SkBitmap.h>
#ifdef SUPPORT_ADTV
#include <am_sub2.h>
#include <am_tt2.h>
#include <am_dmx.h>
#include <am_pes.h>
#include <am_misc.h>
#include <am_cc.h>
#endif
#include <stdlib.h>
#include <pthread.h>
#include <jni.h>
#include <android/log.h>
#include <cutils/properties.h>

#include <android/bitmap.h>


extern "C" {

    typedef struct {
        pthread_mutex_t  lock;
        void * sub_handle;
    #ifdef SUPPORT_ADTV
        AM_PES_Handle_t  pes_handle;
        AM_TT2_Handle_t  tt_handle;
        AM_CC_Handle_t   cc_handle;
    #endif
        int              dmx_id;
        int              filter_handle;
        jobject          obj;
        SkBitmap        *bitmap;
        jobject          obj_bitmap;
        int              bmp_w;
        int              bmp_h;
        int              bmp_pitch;
        uint8_t         *buffer;
        int             sub_w;
        int             sub_h;
    } TVSubtitleData;

#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG    "jnidtvsubtitle"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define CC_JSON_BUFFER_SIZE 8192


    static JavaVM   *gJavaVM = NULL;
    static jmethodID gUpdateID;
    static jfieldID  gBitmapID;
    static TVSubtitleData gSubtitleData;
    static jmethodID gUpdateDataID;
    static jfieldID gCC_JsonID;
    static jstring gCC_JsonStr;
    static jmethodID gPassJsonStr;
    static char gJsonStr[CC_JSON_BUFFER_SIZE];

    static jint sub_clear(JNIEnv *env, jobject obj);
    static void sub_update(jobject obj);
    static void data_update(jobject obj, char *json);
    static void notify_bitmap_changed(jobject bitmap)
    {
    }

#ifdef SUPPORT_ADTV
    static void draw_begin_cb(AM_TT2_Handle_t handle)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_TT2_GetUserData(handle);

        pthread_mutex_lock(&sub->lock);
    }

    static void draw_end_cb(AM_TT2_Handle_t handle)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_TT2_GetUserData(handle);

        pthread_mutex_unlock(&sub->lock);

        sub_update(sub->obj);
    }

    static void cc_draw_begin_cb(AM_CC_Handle_t handle, AM_CC_DrawPara_t *draw_para)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_CC_GetUserData(handle);

        pthread_mutex_lock(&sub->lock);

    }

    static void cc_draw_end_cb(AM_CC_Handle_t handle, AM_CC_DrawPara_t *draw_para)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_CC_GetUserData(handle);

        sub->sub_w = draw_para->caption_width;
        sub->sub_h = draw_para->caption_height;

        pthread_mutex_unlock(&sub->lock);
        sub_update(sub->obj);

    }

    static void cc_rating_cb(AM_CC_Handle_t handle, vbi_rating *pr)
    {
        if (pr) {
            char *tag = "Aratings";
            TVSubtitleData *sub = (TVSubtitleData *)AM_CC_GetUserData(handle);
            char json[64] = {0};
            snprintf(json, 64, "{%s:{g:%d,i:%d,dlsv:%d}}",
                    tag,
                    pr->auth,
                    pr->id,
                    pr->dlsv
                    );
            data_update(sub->obj, json);
        }
    }

    static void cc_data_cb(AM_CC_Handle_t handle, int mask)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_CC_GetUserData(handle);
        char json[64];
        char *ratings = "";

        if (!(mask & (1 << AM_CC_CAPTION_XDS))) {
                ratings = "Aratings:{},";
        }

        sprintf(json, "{%scc:{data:%d}}", ratings, mask);
        data_update(sub->obj, json);
    }

    static void dtvcc_nodata_cb(AM_CC_Handle_t handle)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_CC_GetUserData(handle);
        char json[64] = "{dtvcc:{nodata:1}}";
        data_update(sub->obj, json);
    }

    static void channel_cb(AM_CC_Handle_t handle, AM_CC_CaptionMode_t chn)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_CC_GetUserData(handle);
        char json[64];
        sprintf(json, "{dtvcc:{found:%d}}", (int)chn);
        data_update(sub->obj, json);
    }

    static void pes_tt_cb(AM_PES_Handle_t handle, uint8_t *buf, int size)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_PES_GetUserData(handle);

        AM_TT2_Decode(sub->tt_handle, buf, size);
    }

    static TVSubtitleData *sub_get_data(JNIEnv *env, jobject obj)
    {
        return &gSubtitleData;
    }

    static void pes_data_cb(int dev_no, int fhandle, const uint8_t *data, int len, void *user_data)
    {
        TVSubtitleData *sub = (TVSubtitleData *)user_data;

        AM_PES_Decode(sub->pes_handle, (uint8_t *)data, len);
    }

    static void pes_sub_cb(AM_PES_Handle_t handle, uint8_t *buf, int size)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_PES_GetUserData(handle);

        AM_SUB2_Decode(sub->sub_handle, buf, size);
    }

    static void show_sub_cb(void * handle, AM_SUB2_Picture_t *pic)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_SUB2_GetUserData(handle);

        LOGI("show sub");

        sub_update(sub->obj);
    }

    static uint64_t get_pts_cb(void *handle, uint64_t pts)
    {
        char buf[32];
        AM_ErrorCode_t ret;
        uint32_t v;
        uint64_t r;

        ret = AM_FileRead("/sys/class/tsync/pts_pcrscr", buf, sizeof(buf));
        if (!ret) {
            v = strtoul(buf, 0, 16);
            if (pts & (1LL << 32)) {
                r = ((uint64_t)v) | (1LL << 32);
            } else {
                r = (uint64_t)v;
            }
        } else {
            r = 0LL;
        }

        return r;
    }

    static int open_dmx(TVSubtitleData *data, int dmx_id, int pid)
    {
        AM_DMX_OpenPara_t op;
        struct dmx_pes_filter_params pesp;
        AM_ErrorCode_t ret;

        data->dmx_id = -1;
        data->filter_handle = -1;
        memset(&op, 0, sizeof(op));

        ret = AM_DMX_Open(dmx_id, &op);
        if (ret != AM_SUCCESS)
            goto error;
        data->dmx_id = dmx_id;

        ret = AM_DMX_AllocateFilter(dmx_id, &data->filter_handle);
        if (ret != AM_SUCCESS)
            goto error;

        ret = AM_DMX_SetBufferSize(dmx_id, data->filter_handle, 0x80000);
        if (ret != AM_SUCCESS)
            goto error;

        memset(&pesp, 0, sizeof(pesp));
        pesp.pid = pid;
        pesp.output = DMX_OUT_TAP;
        pesp.pes_type = DMX_PES_TELETEXT0;

        ret = AM_DMX_SetPesFilter(dmx_id, data->filter_handle, &pesp);
        if (ret != AM_SUCCESS)
            goto error;

        ret = AM_DMX_SetCallback(dmx_id, data->filter_handle, pes_data_cb, data);
        if (ret != AM_SUCCESS)
            goto error;

        ret = AM_DMX_StartFilter(dmx_id, data->filter_handle);
        if (ret != AM_SUCCESS)
            goto error;

        return 0;
error:
        if (data->filter_handle != -1) {
            AM_DMX_FreeFilter(dmx_id, data->filter_handle);
        }
        if (data->dmx_id != -1) {
            AM_DMX_Close(dmx_id);
        }

        return -1;
    }

    static int close_dmx(TVSubtitleData *data)
    {
        AM_DMX_FreeFilter(data->dmx_id, data->filter_handle);
        AM_DMX_Close(data->dmx_id);
        data->dmx_id = -1;
        data->filter_handle = -1;

        return 0;
    }

    static void sub_update(jobject obj)
    {
        JNIEnv *env;
        int ret;
        int attached = 0;

        if (!obj)
            return;

        ret = gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);

        if (ret < 0) {
            ret = gJavaVM->AttachCurrentThread(&env, NULL);
            if (ret < 0) {
                LOGE("Can't attach thread");
                return;
            }
            attached = 1;
        }
        env->CallVoidMethod(obj, gUpdateID, NULL);
        if (attached) {
            gJavaVM->DetachCurrentThread();
        }
    }

    static void json_update_cb(AM_CC_Handle_t handle)
    {
        TVSubtitleData *sub = (TVSubtitleData *)AM_CC_GetUserData(handle);
        JNIEnv *env;
        int ret;
        jstring data;
        int attached = 0;
        ret = gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);

        if (ret < 0) {
            ret = gJavaVM->AttachCurrentThread(&env, NULL);
            if (ret < 0) {
                LOGE("Can't attach thread");
                return;
            }
            attached = 1;
        }

        data = env->NewStringUTF(gJsonStr);
        env->CallVoidMethod(sub->obj, gPassJsonStr, data);
        env->DeleteLocalRef(data);

        if (attached) {
            gJavaVM->DetachCurrentThread();
        }

    }

    static void data_update(jobject obj, char *json)
    {
        JNIEnv *env;
        int ret;
        int attached = 0;

        if (!obj)
            return;

        ret = gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);

        if (ret < 0) {
            ret = gJavaVM->AttachCurrentThread(&env, NULL);
            if (ret < 0) {
                LOGE("Can't attach thread");
                return;
            }
            attached = 1;
        }
        jstring data = env->NewStringUTF(json);
        env->CallVoidMethod(obj, gUpdateDataID, data);
        env->DeleteLocalRef(data);
        if (attached) {
            gJavaVM->DetachCurrentThread();
        }
    }

unsigned long sub_config_get(char *config, unsigned long def)
{
    char prop_value[PROPERTY_VALUE_MAX], tmp[32];

    memset(tmp, 0, sizeof(tmp));
    snprintf(tmp, sizeof(tmp)-1, "%d", def);
    memset(prop_value, '\0', PROPERTY_VALUE_MAX);
    property_get(config, prop_value, "");
    if (strcmp(prop_value, "\0") != 0) {
        def = strtol(prop_value, NULL, 10);
    }
    return def;
}

static void setDvbDebugLogLevel() {
    AM_DebugSetLogLevel(property_get_int32("tv.dvb.loglevel", 1));
}

unsigned long getSDKVersion()
{
    unsigned long version = sub_config_get("ro.build.version.sdk", 0);
    LOGE("VERSION_NUM --- %ld", version);
    return version;
}
#endif

    static jint sub_init(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data;
        jclass bmp_clazz;
        jfieldID fid;
        jobject bmp;
        jlong hbmp;

        data = &gSubtitleData;
        memset(data, 0, sizeof(TVSubtitleData));

        pthread_mutex_init(&data->lock, NULL);

        data->obj = env->NewGlobalRef(obj);

        /*just init, no effect*/
        data->sub_w = 720;
        data->sub_h = 576;

        setDvbDebugLogLevel();
    #endif
        return 0;
    }

    static jint sub_destroy(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        if (data) {
            sub_clear(env, obj);

            if (data->obj) {
                pthread_mutex_lock(&data->lock);
                env->DeleteGlobalRef(data->obj);
                data->obj = NULL;
                pthread_mutex_unlock(&data->lock);
            }

            pthread_mutex_destroy(&data->lock);
        }
    #endif
        return 0;
    }

    static jint sub_lock(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        pthread_mutex_lock(&data->lock);
    #endif
        return 0;
    }

    static jint sub_unlock(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        pthread_mutex_unlock(&data->lock);
    #endif
        return 0;
    }

    static jint sub_clear(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        if (data->tt_handle) {
            AM_TT2_Destroy(data->tt_handle);
            data->tt_handle = NULL;
        }
    #endif
        return 0;
    }

    static jint sub_start_dvb_sub(JNIEnv *env, jobject obj, jint dmx_id, jint pid, jint page_id, jint anc_page_id)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);
        AM_PES_Para_t pesp;
        AM_SUB2_Para_t subp;
        int ret;

        setDvbDebugLogLevel();

        memset(&pesp, 0, sizeof(pesp));
        pesp.packet    = pes_sub_cb;
        pesp.user_data = data;
        ret = AM_PES_Create(&data->pes_handle, &pesp);
        if (ret != AM_SUCCESS)
            goto error;

        memset(&subp, 0, sizeof(subp));
        subp.show      = show_sub_cb;
        subp.get_pts   = get_pts_cb;
        subp.composition_id = page_id;
        subp.ancillary_id   = anc_page_id;
        subp.user_data = data;
        ret = AM_SUB2_Create(&data->sub_handle, &subp);
        if (ret != AM_SUCCESS)
            goto error;

        ret = AM_SUB2_Start(data->sub_handle);
        if (ret != AM_SUCCESS)
            goto error;

        ret = open_dmx(data, dmx_id, pid);
        if (ret < 0)
            goto error;

        return 0;
error:
        if (data->sub_handle) {
            AM_SUB2_Destroy(data->sub_handle);
            data->sub_handle = NULL;
        }
        if (data->pes_handle) {
            AM_PES_Destroy(data->pes_handle);
            data->pes_handle = NULL;
        }
    #endif
        return -1;
    }

    static jint sub_start_dtv_tt(JNIEnv *env, jobject obj, jint dmx_id, jint region_id, jint pid, jint page, jint sub_page, jboolean is_sub)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);
        AM_PES_Para_t pesp;
        AM_TT2_Para_t ttp;
        int ret;

        setDvbDebugLogLevel();

        if (!data->tt_handle) {
            memset(&ttp, 0, sizeof(ttp));
            ttp.draw_begin = draw_begin_cb;
            ttp.draw_end  = draw_end_cb;
            ttp.is_subtitle = is_sub;
            ttp.bitmap    = data->buffer;
            ttp.pitch     = data->bmp_pitch;
            ttp.user_data = data;
            ttp.default_region = region_id;
            ret = AM_TT2_Create(&data->tt_handle, &ttp);
            if (ret != AM_SUCCESS)
                goto error;
        } else {
            AM_TT2_SetSubtitleMode(data->tt_handle, is_sub);
        }

        AM_TT2_GotoPage(data->tt_handle, page, sub_page);
        AM_TT2_Start(data->tt_handle);

        memset(&pesp, 0, sizeof(pesp));
        pesp.packet    = pes_tt_cb;
        pesp.user_data = data;
        ret = AM_PES_Create(&data->pes_handle, &pesp);
        if (ret != AM_SUCCESS)
            goto error;

        ret = open_dmx(data, dmx_id, pid);
        if (ret < 0)
            goto error;

        LOGI("Teletext started at demux %d, pid %d, page %d", dmx_id, pid, page);
        return 0;
error:
        if (data->pes_handle) {
            AM_PES_Destroy(data->pes_handle);
            data->pes_handle = NULL;
        }
    #endif
        return -1;
    }

    static jint sub_stop_dvb_sub(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        close_dmx(data);
        AM_SUB2_Destroy(data->sub_handle);
        AM_PES_Destroy(data->pes_handle);
        pthread_mutex_lock(&data->lock);
        if (data->obj)
            sub_update(data->obj);
        pthread_mutex_unlock(&data->lock);

        data->sub_handle = NULL;
        data->pes_handle = NULL;
    #endif
        return 0;
    }

    static jint sub_stop_dtv_tt(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        close_dmx(data);
        AM_PES_Destroy(data->pes_handle);
        data->pes_handle = NULL;
        AM_TT2_Stop(data->tt_handle);

        pthread_mutex_lock(&data->lock);
        if (data->obj)
            sub_update(data->obj);
        pthread_mutex_unlock(&data->lock);
    #endif
        return 0;
    }

    static jint sub_tt_goto(JNIEnv *env, jobject obj, jint page)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        AM_TT2_GotoPage(data->tt_handle, page, AM_TT2_ANY_SUBNO);
    #endif
        return 0;
    }

    static jint sub_tt_color_link(JNIEnv *env, jobject obj, jint color)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        AM_TT2_ColorLink(data->tt_handle, (AM_TT2_Color_t)color);
    #endif
        return 0;
    }

    static jint sub_tt_home_link(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        AM_TT2_GoHome(data->tt_handle);
    #endif
        return 0;
    }

    static jint sub_tt_next(JNIEnv *env, jobject obj, jint dir)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        AM_TT2_NextPage(data->tt_handle, dir);
    #endif
        return 0;
    }

    static jint sub_tt_set_search_pattern(JNIEnv *env, jobject obj, jstring pattern, jboolean casefold)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);
        jclass clsstring = env->FindClass("java/lang/String");
        jstring strencode = env->NewStringUTF("utf-16");
        jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
        jbyteArray barr = (jbyteArray)env->CallObjectMethod(pattern, mid, strencode);
        jsize alen = env->GetArrayLength(barr);
        jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
        char *buf;

        buf = (char *)malloc(alen + 1);
        if (buf) {
            memcpy(buf, ba, alen);
            buf[alen] = 0;

            AM_TT2_SetSearchPattern(data->tt_handle, buf, casefold, AM_FALSE);
        }

        free(buf);
        env->DeleteLocalRef(strencode);
        env->ReleaseByteArrayElements(barr, ba, 0);
    #endif
        return 0;
    }

    static jint sub_tt_search(JNIEnv *env, jobject obj, jint dir)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        AM_TT2_Search(data->tt_handle, dir);
    #endif
        return 0;
    }

    static jint sub_start_atsc_cc(JNIEnv *env, jobject obj, jint source, jint caption, jint fg_color,
            jint fg_opacity, jint bg_color, jint bg_opacity, jint font_style, jint font_size)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);
        AM_CC_CreatePara_t cc_para;
        AM_CC_StartPara_t spara;
        int ret;

        setDvbDebugLogLevel();

        LOGI("start cc: caption %d, fgc %d, bgc %d, fgo %d, bgo %d, fsize %d, fstyle %d",
                caption, fg_color, bg_color, fg_opacity, bg_opacity, font_size, font_style);

        memset(&cc_para, 0, sizeof(cc_para));
        memset(&spara, 0, sizeof(spara));

        cc_para.bmp_buffer = data->buffer;
        cc_para.pitch = data->bmp_pitch;
        cc_para.draw_begin = cc_draw_begin_cb;
        cc_para.draw_end = cc_draw_end_cb;
        cc_para.user_data = (void *)data;
        cc_para.input = (AM_CC_Input_t)source;
        cc_para.rating_cb = cc_rating_cb;
        cc_para.data_cb = cc_data_cb;
        cc_para.data_timeout = 5000;//5s
        cc_para.switch_timeout = 3000;//3s
        spara.caption1                 = (AM_CC_CaptionMode_t)caption;
        spara.caption2                 = AM_CC_CAPTION_NONE;
        cc_para.json_update = json_update_cb;
        cc_para.json_buffer = gJsonStr;

        spara.user_options.bg_color    = (AM_CC_Color_t)bg_color;
        spara.user_options.fg_color    = (AM_CC_Color_t)fg_color;
        spara.user_options.bg_opacity  = (AM_CC_Opacity_t)bg_opacity;
        spara.user_options.fg_opacity  = (AM_CC_Opacity_t)fg_opacity;
        spara.user_options.font_size   = (AM_CC_FontSize_t)font_size;
        spara.user_options.font_style  = (AM_CC_FontStyle_t)font_style;

        ret = AM_CC_Create(&cc_para, &data->cc_handle);
        if (ret != AM_SUCCESS)
            goto error;

        ret = AM_CC_Start(data->cc_handle, &spara);
        if (ret != AM_SUCCESS)
            goto error;
        LOGI("start cc successfully!");
        return 0;
error:
        if (data->cc_handle != NULL) {
            AM_CC_Destroy(data->cc_handle);
        }
    #endif
        LOGI("start cc failed!");
        return -1;
    }

    static jint get_subtitle_piture_width(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);
        return data->sub_w;
    #else
        return -1;
    #endif
    }

    static jint get_subtitle_piture_height(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);
        return data->sub_h;
    #else
        return -1;
    #endif
    }

    static jint sub_start_atsc_dtvcc(JNIEnv *env, jobject obj, jint caption, jint fg_color,
            jint fg_opacity, jint bg_color, jint bg_opacity, jint font_style, jint font_size)
    {
    #ifdef SUPPORT_ADTV
        return sub_start_atsc_cc(env, obj, AM_CC_INPUT_USERDATA, caption, fg_color,
                fg_opacity, bg_color, bg_opacity, font_style, font_size);
    #else
        return -1;
    #endif
    }

    static jint sub_start_atsc_atvcc(JNIEnv *env, jobject obj, jint caption, jint fg_color,
            jint fg_opacity, jint bg_color, jint bg_opacity, jint font_style, jint font_size)
    {
    #ifdef SUPPORT_ADTV
        return sub_start_atsc_cc(env, obj, AM_CC_INPUT_VBI, caption, fg_color,
                fg_opacity, bg_color, bg_opacity, font_style, font_size);
    #else
        return -1;
    #endif
    }

    static jint sub_stop_atsc_cc(JNIEnv *env, jobject obj)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        LOGI("stop cc");
        AM_CC_Destroy(data->cc_handle);
        pthread_mutex_lock(&data->lock);
        if (data->obj)
            sub_update(data->obj);
        pthread_mutex_unlock(&data->lock);

        data->cc_handle = NULL;
    #endif
        return 0;
    }

    static jint sub_set_atsc_cc_options(JNIEnv *env, jobject obj, jint fg_color,
            jint fg_opacity, jint bg_color, jint bg_opacity, jint font_style, jint font_size)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);
        AM_CC_UserOptions_t param;

        setDvbDebugLogLevel();

        memset(&param, 0, sizeof(AM_CC_UserOptions_t));
        param.bg_color    = (AM_CC_Color_t)bg_color;
        param.fg_color    = (AM_CC_Color_t)fg_color;
        param.bg_opacity  = (AM_CC_Opacity_t)bg_opacity;
        param.fg_opacity  = (AM_CC_Opacity_t)fg_opacity;
        param.font_size   = (AM_CC_FontSize_t)font_size;
        param.font_style  = (AM_CC_FontStyle_t)font_style;

        AM_CC_SetUserOptions(data->cc_handle, &param);
    #endif
        return 0;
    }

    static jint sub_set_active(JNIEnv *env, jobject obj, jboolean active)
    {
    #ifdef SUPPORT_ADTV
        TVSubtitleData *data = sub_get_data(env, obj);

        if (active) {
            if (data->obj) {
                pthread_mutex_lock(&data->lock);
                env->DeleteGlobalRef(data->obj);
                data->obj = NULL;
                pthread_mutex_unlock(&data->lock);
            }

            data->obj = env->NewGlobalRef(obj);
        } else {
            if (env->IsSameObject(data->obj, obj)) {
                pthread_mutex_lock(&data->lock);
                env->DeleteGlobalRef(data->obj);
                data->obj = NULL;
                pthread_mutex_unlock(&data->lock);
            }
        }
    #endif
        return 0;
    }

    static JNINativeMethod gMethods[] = {
        /* name, signature, funcPtr */
        {"native_sub_init", "()I", (void *)sub_init},
        {"native_sub_destroy", "()I", (void *)sub_destroy},
        {"native_sub_lock", "()I", (void *)sub_lock},
        {"native_sub_unlock", "()I", (void *)sub_unlock},
        {"native_sub_clear", "()I", (void *)sub_clear},
        {"native_sub_start_dvb_sub", "(IIII)I", (void *)sub_start_dvb_sub},
        {"native_sub_start_dtv_tt", "(IIIIIZ)I", (void *)sub_start_dtv_tt},
        {"native_sub_stop_dvb_sub", "()I", (void *)sub_stop_dvb_sub},
        {"native_sub_stop_dtv_tt", "()I", (void *)sub_stop_dtv_tt},
        {"native_sub_tt_goto", "(I)I", (void *)sub_tt_goto},
        {"native_sub_tt_color_link", "(I)I", (void *)sub_tt_color_link},
        {"native_sub_tt_home_link", "()I", (void *)sub_tt_home_link},
        {"native_sub_tt_next", "(I)I", (void *)sub_tt_next},
        {"native_sub_tt_set_search_pattern", "(Ljava/lang/String;Z)I", (void *)sub_tt_set_search_pattern},
        {"native_sub_tt_search_next", "(I)I", (void *)sub_tt_search},
        {"native_get_subtitle_picture_width", "()I", (void *)get_subtitle_piture_width},
        {"native_get_subtitle_picture_height", "()I", (void *)get_subtitle_piture_height},
        {"native_sub_start_atsc_cc", "(IIIIIII)I", (void *)sub_start_atsc_dtvcc},
        {"native_sub_start_atsc_atvcc", "(IIIIIII)I", (void *)sub_start_atsc_atvcc},
        {"native_sub_stop_atsc_cc", "()I", (void *)sub_stop_atsc_cc},
        {"native_sub_set_atsc_cc_options", "(IIIIII)I", (void *)sub_set_atsc_cc_options},
        {"native_sub_set_active", "(Z)I", (void *)sub_set_active}
    };

    JNIEXPORT jint
        JNI_OnLoad(JavaVM *vm, void *reserved)
        {
            JNIEnv *env = NULL;
            jclass clazz;
            int rc;

            gJavaVM = vm;

            if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
                LOGE("GetEnv failed");
                return -1;
            }

            clazz = env->FindClass("com/droidlogic/tvinput/widget/DTVSubtitleView");
            if (clazz == NULL) {
                LOGE("FindClass com/droidlogic/tvinput/widget/DTVSubtitleView failed");
                return -1;
            }

            if ((rc = (env->RegisterNatives(clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0])))) < 0) {
                LOGE("RegisterNatives failed");
                return -1;
            }


            gUpdateID = env->GetMethodID(clazz, "update", "()V");

            gPassJsonStr = env->GetMethodID(clazz, "saveJsonStr", "(Ljava/lang/String;)V");

            gUpdateDataID = env->GetMethodID(clazz, "updateData", "(Ljava/lang/String;)V");


            LOGI("load jnitvsubtitle ok");
            return JNI_VERSION_1_4;
        }

    JNIEXPORT void
        JNI_OnUnload(JavaVM *vm, void *reserved)
        {
            JNIEnv *env = NULL;
            int i;

            if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
                return;
            }
        }

} /*extern "C"*/

