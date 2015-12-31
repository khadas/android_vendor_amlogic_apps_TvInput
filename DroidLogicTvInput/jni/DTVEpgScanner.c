#include <am_epg.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "jnidtvepg"
#define log_info(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define log_error(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* EPG notify events*/
#define EVENT_PF_EIT_END             1
#define EVENT_SCH_EIT_END            2
#define EVENT_PMT_END                3
#define EVENT_SDT_END                4
#define EVENT_TDT_END                5
#define EVENT_NIT_END                6
#define EVENT_PROGRAM_AV_UPDATE      7
#define EVENT_PROGRAM_NAME_UPDATE    8
#define EVENT_PROGRAM_EVENTS_UPDATE  9
#define EVENT_TS_UPDATE              10

static JavaVM   *gJavaVM = NULL;
static jclass    gEventClass;
static jmethodID gEventInitID;
static jmethodID gOnEventID;
static jfieldID  gHandleID;
static jclass    gEvtClass;
static jmethodID gEvtInitID;
static jclass    gChannelClass;
static jmethodID gChannelInitID;

typedef struct{
	int dmx_id;
	int fend_id;
	AM_EPG_Handle_t handle;
	jobject obj;
}EPGData;

typedef struct {
	int type;
	int channelID;
	int programID;
	int dvbOrigNetID;
	int dvbTSID;
	int dvbServiceID;
	long time;
	int dvbVersion;
}EPGEventData;

typedef struct {
	int valid;
	char name[(64+4)*4 + 1];
	int mOriginalNetworkId;
	int mTransportStreamId;
	int mServiceId;
	int mType;
	int mServiceType;
	int mFrequency;
	int mBandwidth;
	int mModulation;
	int mSymbolRate;
	int mFEMisc;
	int mVideoPID;
	int mVideoFormat;
	AM_SI_AudioInfo_t mAudioInfo;
	int mPcrPID;
}EPGChannelData;

EPGChannelData gChannelMonitored = {.valid = 0};

static void epg_on_event(jobject obj, EPGEventData *evt_data)
{
	JNIEnv *env;
	int ret;
	int attached = 0;

	ret = (*gJavaVM)->GetEnv(gJavaVM, (void**) &env, JNI_VERSION_1_4);
	if (ret <0) {
		ret = (*gJavaVM)->AttachCurrentThread(gJavaVM,&env,NULL);
		if (ret <0) {
			log_error("callback handler:failed to attach current thread");
			return;
		}
		attached = 1;
	}
	jobject event = (*env)->NewObject(env, gEventClass, gEventInitID, obj, evt_data->type);
	(*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "channelID", "I"), evt_data->channelID);
	(*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "programID", "I"), evt_data->programID);
	(*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "dvbOrigNetID", "I"), evt_data->dvbOrigNetID);
	(*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "dvbTSID", "I"), evt_data->dvbTSID);
	(*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "dvbServiceID", "I"), evt_data->dvbServiceID);
	(*env)->SetLongField(env,event,(*env)->GetFieldID(env, gEventClass, "time", "J"), evt_data->time);
	(*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "dvbVersion", "I"), evt_data->dvbVersion);
	(*env)->CallVoidMethod(env, obj, gOnEventID, event);

	if (attached) {
		(*gJavaVM)->DetachCurrentThread(gJavaVM);
	}
}

static void epg_evt_callback(long dev_no, int event_type, void *param, void *user_data)
{
	EPGData *priv_data;
	EPGEventData edata;

	UNUSED(user_data);

	AM_EPG_GetUserData((AM_EPG_Handle_t)dev_no, (void**)&priv_data);
	if (!priv_data)
		return;
	memset(&edata, 0, sizeof(edata));
	switch (event_type) {
		case AM_EPG_EVT_NEW_NIT:
			log_info(".....................AM_EPG_EVT_NEW_NIT.................%d\n",(int)(long)param);
			edata.type = EVENT_NIT_END;
			edata.dvbVersion =(int)(long)param;
			epg_on_event(priv_data->obj, &edata);
			break;
		case AM_EPG_EVT_NEW_TDT:
		case AM_EPG_EVT_NEW_STT:
		{
			int utc_time;

			AM_EPG_GetUTCTime(&utc_time);
			edata.type = EVENT_TDT_END;
			edata.time = (long)utc_time;
			epg_on_event(priv_data->obj, &edata);
		}
			break;
		case AM_EPG_EVT_UPDATE_EVENTS:
			edata.type = EVENT_PROGRAM_EVENTS_UPDATE;
			edata.programID = (int)(long)param;
			epg_on_event(priv_data->obj, &edata);
			break;
		case AM_EPG_EVT_UPDATE_PROGRAM_AV:
			edata.type = EVENT_PROGRAM_AV_UPDATE;
			edata.programID = (int)(long)param;
			epg_on_event(priv_data->obj, &edata);
			break;
		case AM_EPG_EVT_UPDATE_PROGRAM_NAME:
			edata.type = EVENT_PROGRAM_NAME_UPDATE;
			edata.programID = (int)(long)param;
			epg_on_event(priv_data->obj, &edata);
			break;
		case AM_EPG_EVT_UPDATE_TS:
			edata.type = EVENT_TS_UPDATE;
			edata.channelID = (int)(long)param;
			epg_on_event(priv_data->obj, &edata);
			break;
		default:
			break;
	}
}

static jbyteArray get_byte_array(JNIEnv* env, const char *str)
{
	if (!str || !str[0])
		return NULL;

	int len = strlen(str);
	jbyteArray byteArray = (*env)->NewByteArray(env, len);
	(*env)->SetByteArrayRegion(env, byteArray, 0, len, str);

	return byteArray;
}


void Events_Update(AM_EPG_Handle_t handle, int event_count, AM_EPG_Event_t *pevents)
{
	int i;
	AM_EPG_Event_t *pevt;
	JNIEnv *env;
	int ret;
	int attached = 0;
	EPGData *priv_data;

	AM_EPG_GetUserData(handle, (void**)&priv_data);
	if (!priv_data)
		return;

	if (!event_count)
		return;

	ret = (*gJavaVM)->GetEnv(gJavaVM, (void**) &env, JNI_VERSION_1_4);
	if (ret <0) {
		ret = (*gJavaVM)->AttachCurrentThread(gJavaVM,&env,NULL);
		if (ret <0) {
			log_error("callback handler:failed to attach current thread");
			return;
		}
		attached = 1;
	}

	jobject event = (*env)->NewObject(env, gEventClass, gEventInitID, priv_data->obj, EVENT_PROGRAM_EVENTS_UPDATE);
	jobjectArray EvtsArray = (*env)->NewObjectArray(env, event_count, gEvtClass, 0);

	for (i = 0; i < event_count; i++) {
		pevt = &pevents[i];

		jobject evt = (*env)->NewObject(env, gEvtClass, gEvtInitID, event, 0);
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "src", "I"), pevt->src);
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "srv_id", "I"), pevt->srv_id);
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "ts_id", "I"), pevt->ts_id);
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "net_id", "I"), pevt->net_id);
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "evt_id", "I"), pevt->evt_id);
		(*env)->SetLongField(env,evt,(*env)->GetFieldID(env, gEvtClass, "start", "J"), (jlong)pevt->start);
		(*env)->SetLongField(env,evt,(*env)->GetFieldID(env, gEvtClass, "end", "J"), (jlong)pevt->end);
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "nibble", "I"), pevt->nibble);
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "parental_rating", "I"), pevt->parental_rating);

		(*env)->SetObjectField(env,evt,	(*env)->GetFieldID(env, gEvtClass, "name", "[B"), get_byte_array(env, pevt->name));
		(*env)->SetObjectField(env,evt,	(*env)->GetFieldID(env, gEvtClass, "desc", "[B"), get_byte_array(env, pevt->desc));
		(*env)->SetObjectField(env,evt,	(*env)->GetFieldID(env, gEvtClass, "ext_item", "[B"), get_byte_array(env, pevt->ext_item));
		(*env)->SetObjectField(env,evt,	(*env)->GetFieldID(env, gEvtClass, "ext_descr", "[B"), get_byte_array(env, pevt->ext_descr));
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "sub_flag", "I"), pevt->sub_flag);
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "sub_status", "I"), pevt->sub_status);
		(*env)->SetIntField(env,evt,(*env)->GetFieldID(env, gEvtClass, "source_id", "I"), pevt->source_id);
		(*env)->SetObjectField(env,evt,	(*env)->GetFieldID(env, gEvtClass, "rrt_ratings", "[B"), get_byte_array(env, pevt->rrt_ratings));

		(*env)->SetObjectArrayElement(env, EvtsArray, i, evt);

	}

	(*env)->SetObjectField(env,event,(*env)->GetFieldID(env, gEventClass, "evts", "[Lcom/droidlogic/tvinput/services/DTVEpgScanner$Event$Evt;"), EvtsArray);

	(*env)->CallVoidMethod(env, priv_data->obj, gOnEventID, event);

	if (attached) {
		(*gJavaVM)->DetachCurrentThread(gJavaVM);
	}
	AM_EPG_FreeEvents(event_count, pevents);
}

static void format_audio_strings(AM_SI_AudioInfo_t *ai, char *pids, char *fmts, char *langs)
{
	int i;

	if (ai->audio_count < 0)
		ai->audio_count = 0;

	pids[0] = 0;
	fmts[0] = 0;
	langs[0] = 0;
	for (i=0; i<ai->audio_count; i++)
	{
		if (i == 0)
		{
			snprintf(pids, 256, "%d", ai->audios[i].pid);
			snprintf(fmts, 256, "%d", ai->audios[i].fmt);
			sprintf(langs, "%s", ai->audios[i].lang);
		}
		else
		{
			snprintf(pids, 256, "%s %d", pids, ai->audios[i].pid);
			snprintf(fmts, 256, "%s %d", fmts, ai->audios[i].fmt);
			snprintf(langs, 256, "%s %s", langs, ai->audios[i].lang);
		}
	}
}

static int check_pmt_update(EPGChannelData *c1, EPGChannelData *c2)
{
	int ret=0;

	if (c1->mVideoPID != c2->mVideoPID || c1->mVideoFormat != c2->mVideoFormat)
	{
		//notify
		ret = 1;
	}
	else
	{
		int i, j;
		for (i=0; i<c1->mAudioInfo.audio_count; i++)
		{
			for (j=0; j<c2->mAudioInfo.audio_count; j++)
			{
				if (c1->mAudioInfo.audios[i].pid == c2->mAudioInfo.audios[j].pid &&
					c1->mAudioInfo.audios[i].fmt == c2->mAudioInfo.audios[j].fmt &&
					!strncmp(c1->mAudioInfo.audios[i].lang, c2->mAudioInfo.audios[j].lang, 3))
					break;
			}
			if (j >= c2->mAudioInfo.audio_count)
			{
				//notify
				ret = 1;
				break;
			}
		}
	}
	if (ret) {
		char str_prev_apids[256], str_cur_apids[256];
		char str_prev_afmts[256], str_cur_afmts[256];
		char str_prev_alangs[256], str_cur_alangs[256];
		log_info(">>> Video/Audio changed ");
		log_info("Video pid/fmt: (%d/%d) -> (%d/%d)", c1->mVideoPID, c1->mVideoFormat, c1->mVideoPID, c1->mVideoFormat);
		format_audio_strings(&c1->mAudioInfo, str_prev_apids, str_prev_afmts, str_prev_alangs);
		format_audio_strings(&c2->mAudioInfo, str_cur_apids, str_cur_afmts, str_cur_alangs);
		log_info("Audio pid/fmt/lang: ('%s'/'%s'/'%s') -> ('%s'/'%s'/'%s')",
			str_prev_apids, str_prev_afmts, str_prev_alangs, str_cur_apids, str_cur_afmts, str_cur_alangs);
	}
	return ret;
}

#define FUNC_get_int_array(_n, _S, _c, _s, _e) \
static jintArray get_##_n##_array(_S *s) \
{ \
	JNIEnv *env; \
	int attached = 0; \
	int ret = -1; \
	int i; \
	if (!s->_c) \
		return NULL; \
	ret = (*gJavaVM)->GetEnv(gJavaVM, (void**) &env, JNI_VERSION_1_4); \
	if (ret <0) { \
		ret = (*gJavaVM)->AttachCurrentThread(gJavaVM,&env,NULL); \
		if (ret <0) { \
			log_error("callback handler:failed to attach current thread"); \
			return NULL; \
		} \
		attached = 1; \
	} \
	jintArray result = (*env)->NewIntArray(env, s->_c); \
	int *pa_tmp = malloc(s->_c * sizeof(int)); \
	for (i=0; i < s->_c; i++) \
		pa_tmp[i] = s->_s[i]._e; \
	(*env)->SetIntArrayRegion(env, result, 0, s->_c, pa_tmp); \
	free(pa_tmp); \
	if (attached) { \
		log_info("callback handler:detach current thread"); \
		(*gJavaVM)->DetachCurrentThread(gJavaVM); \
	} \
	return result; \
}

FUNC_get_int_array(aids, AM_SI_AudioInfo_t, audio_count, audios, pid);
FUNC_get_int_array(afmts, AM_SI_AudioInfo_t, audio_count, audios, fmt);

#define FUNC_get_string_array(_n, _S, _c, _s, _e) \
static jobjectArray get_##_n##_array(_S *s)\
{ \
	JNIEnv *env; \
	jstring	 str; \
	jobjectArray args = 0; \
	int  attached = 0; \
	char sa[10]; \
	int  i=0; \
	int  ret = -1; \
	if (!s->_c) \
		return NULL; \
	ret = (*gJavaVM)->GetEnv(gJavaVM, (void**) &env, JNI_VERSION_1_4); \
	if (ret <0) { \
		ret = (*gJavaVM)->AttachCurrentThread(gJavaVM,&env,NULL); \
		if (ret <0) { \
			log_error("callback handler:failed to attach current thread"); \
			return NULL; \
		} \
		attached = 1; \
	} \
	args = (*env)->NewObjectArray(env, s->_c,(*env)->FindClass(env, "java/lang/String"),0); \
	for ( i=0; i < s->_c; i++ ) \
	{ \
		memcpy(sa, s->_s[i]._e, 10); \
		str = (*env)->NewStringUTF(env, sa);\
		(*env)->SetObjectArrayElement(env, args, i, str); \
	} \
	if (attached) { \
		log_info("callback handler:detach current thread"); \
		(*gJavaVM)->DetachCurrentThread(gJavaVM); \
	} \
	return args; \
}

FUNC_get_string_array(alangs, AM_SI_AudioInfo_t, audio_count, audios, lang);

static void PMT_Update(AM_EPG_Handle_t handle, dvbpsi_pmt_t *pmts)
{
	dvbpsi_pmt_t *pmt;
	dvbpsi_pmt_es_t *es;
	dvbpsi_descriptor_t *descr;
	EPGChannelData *pch_cur = &gChannelMonitored,
			ch;

	if (!pch_cur->valid)
		return;

	memset(&ch, 0, sizeof(ch));

	ch.mAudioInfo.audio_count = 0;
	ch.mVideoPID = 0x1fff;
	ch.mVideoFormat = -1;
	ch.mPcrPID = (pmts) ? pmts->i_pcr_pid : 0x1fff;

	AM_SI_LIST_BEGIN(pmts, pmt)
		AM_SI_LIST_BEGIN(pmt->p_first_es, es)
			AM_SI_ExtractAVFromES(es, &ch.mVideoPID, &ch.mVideoFormat, &ch.mAudioInfo);
		AM_SI_LIST_END()
	AM_SI_LIST_END()

	if (check_pmt_update(pch_cur, &ch)) {

		JNIEnv *env;
		int ret;
		int attached = 0;
		EPGData *priv_data;

		AM_EPG_GetUserData(handle, (void**)&priv_data);
		if (!priv_data)
			return;

		ret = (*gJavaVM)->GetEnv(gJavaVM, (void**) &env, JNI_VERSION_1_4);
		if (ret <0) {
			ret = (*gJavaVM)->AttachCurrentThread(gJavaVM,&env,NULL);
			if (ret <0) {
				log_error("callback handler:failed to attach current thread");
				return;
			}
			attached = 1;
		}

		jobject event = (*env)->NewObject(env, gEventClass, gEventInitID, priv_data->obj, EVENT_PROGRAM_AV_UPDATE);
		jobject channel = (*env)->NewObject(env, gChannelClass, gChannelInitID, event, 0);

		(*env)->SetIntField(env,channel,\
				(*env)->GetFieldID(env, gChannelClass, "mServiceId", "I"), pch_cur->mServiceId);
		(*env)->SetIntField(env,channel,\
				(*env)->GetFieldID(env, gChannelClass, "mVideoPid", "I"), ch.mVideoPID);
		(*env)->SetIntField(env,channel,\
				(*env)->GetFieldID(env, gChannelClass, "mVfmt", "I"), ch.mVideoFormat);
		(*env)->SetObjectField(env,channel,\
				(*env)->GetFieldID(env, gChannelClass, "mAudioPids", "[I"), get_aids_array(&ch.mAudioInfo));
		(*env)->SetObjectField(env,channel,\
				(*env)->GetFieldID(env, gChannelClass, "mAudioFormats", "[I"), get_afmts_array(&ch.mAudioInfo));
		(*env)->SetObjectField(env,channel,\
				(*env)->GetFieldID(env, gChannelClass, "mAudioLangs", "[Ljava/lang/String;"), get_alangs_array(&ch.mAudioInfo));
		(*env)->SetIntField(env,channel,\
				(*env)->GetFieldID(env, gChannelClass, "mPcrPid", "I"), ch.mPcrPID);

		(*env)->SetObjectField(env,event,(*env)->GetFieldID(env, gEventClass, "channel", "Lcom/droidlogic/app/tv/ChannelInfo;"), channel);

		(*env)->CallVoidMethod(env, priv_data->obj, gOnEventID, event);

		if (attached) {
			(*gJavaVM)->DetachCurrentThread(gJavaVM);
		}
	}
}


static void epg_create(JNIEnv* env, jobject obj, jint fend_id, jint dmx_id, jint src, jstring ordered_text_langs)
{
	AM_EPG_CreatePara_t para;
	EPGData *data;
	AM_ErrorCode_t ret;
	AM_FEND_OpenPara_t fend_para;
	AM_DMX_OpenPara_t dmx_para;

	data = (EPGData*)malloc(sizeof(EPGData));
	if (!data) {
		log_error("malloc failed");
		return;
	}
	data->dmx_id = dmx_id;
	log_info("Opening demux%d ...", dmx_id);
	memset(&dmx_para, 0, sizeof(dmx_para));
	AM_DMX_Open(dmx_id, &dmx_para);

	memset(&para, 0, sizeof(para));
	para.fend_dev = fend_id;
	para.dmx_dev  = dmx_id;
	para.source   = src;
	para.hdb      = NULL;
	const char *strlang = (*env)->GetStringUTFChars(env, ordered_text_langs, 0);
	if (strlang != NULL) {
		snprintf(para.text_langs, sizeof(para.text_langs), "%s", strlang);
		(*env)->ReleaseStringUTFChars(env, ordered_text_langs, strlang);
	}

	ret = AM_EPG_Create(&para, &data->handle);
	if (ret != AM_SUCCESS) {
		free(data);
		log_error("AM_EPG_Create failed");
		return;
	}

	data->obj = (*env)->NewGlobalRef(env,obj);

	(*env)->SetLongField(env, obj, gHandleID, (long)data);

	/*注册EIT通知事件*/
	AM_EVT_Subscribe((long)data->handle,AM_EPG_EVT_NEW_NIT,epg_evt_callback,NULL);
	AM_EVT_Subscribe((long)data->handle,AM_EPG_EVT_NEW_TDT,epg_evt_callback,NULL);
	AM_EVT_Subscribe((long)data->handle,AM_EPG_EVT_NEW_STT,epg_evt_callback,NULL);
	AM_EVT_Subscribe((long)data->handle,AM_EPG_EVT_UPDATE_EVENTS,epg_evt_callback,NULL);
	AM_EVT_Subscribe((long)data->handle,AM_EPG_EVT_UPDATE_PROGRAM_AV,epg_evt_callback,NULL);
	AM_EVT_Subscribe((long)data->handle,AM_EPG_EVT_UPDATE_PROGRAM_NAME,epg_evt_callback,NULL);
	AM_EVT_Subscribe((long)data->handle,AM_EPG_EVT_UPDATE_TS,epg_evt_callback,NULL);
	AM_EPG_SetUserData(data->handle, (void*)data);

	AM_EPG_SetEventsCallback(data->handle, Events_Update);
}

static void epg_destroy(JNIEnv* env, jobject obj)
{
	EPGData *data;

	data = (EPGData*)((*env)->GetLongField(env, obj, gHandleID));

	/*反注册EIT通知事件*/
	if (data) {
		AM_EVT_Unsubscribe((long)data->handle,AM_EPG_EVT_NEW_TDT,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe((long)data->handle,AM_EPG_EVT_NEW_STT,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe((long)data->handle,AM_EPG_EVT_UPDATE_EVENTS,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe((long)data->handle,AM_EPG_EVT_UPDATE_PROGRAM_AV,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe((long)data->handle,AM_EPG_EVT_UPDATE_PROGRAM_NAME,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe((long)data->handle,AM_EPG_EVT_UPDATE_TS,epg_evt_callback,NULL);
		AM_EPG_Destroy(data->handle);
		log_info("EPGScanner on demux%d sucessfully destroyed", data->dmx_id);
		log_info("Closing demux%d ...", data->dmx_id);
		AM_DMX_Close(data->dmx_id);
		if (data->obj)
			(*env)->DeleteGlobalRef(env, data->obj);
		free(data);
	}
}

static void epg_change_mode(JNIEnv* env, jobject obj, jint op, jint mode)
{
	EPGData *data;
	AM_ErrorCode_t ret;

	data = (EPGData*)((*env)->GetLongField(env, obj, gHandleID));

	ret = AM_EPG_ChangeMode(data->handle, op, mode);
	if (ret != AM_SUCCESS)
		log_error("AM_EPG_ChangeMode failed");
}

static int get_channel_data(JNIEnv* env, jobject obj, jobject channel, EPGChannelData *pch)
{
	memset(pch, 0, sizeof(*pch));

	if (!channel) {
		pch->valid = 0;
		return 0;
	}

	int i;
	jclass objclass =(*env)->FindClass(env,"com/droidlogic/app/tv/ChannelInfo");
	jstring Name = (*env)->GetObjectField(env, channel, (*env)->GetFieldID(env, objclass, "mDisplayName", "Ljava/lang/String;"));
	const char *cName = (*env)->GetStringUTFChars(env, Name, 0);
	strncpy(pch->name, cName, (64+4)*4);
	(*env)->ReleaseStringUTFChars(env, Name, cName);
	pch->mOriginalNetworkId = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mOriginalNetworkId", "I"));
	pch->mTransportStreamId = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mTransportStreamId", "I"));
	//pch->mType = (*env)->GetIntField(env, (*env)->GetFieldID(env, objclass, "mType", "I"),0);
	//pch->mServiceType = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mServiceType", "I"),0);
	pch->mServiceId = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mServiceId", "I"));
	pch->mFrequency = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mFrequency", "I"));
	pch->mBandwidth = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mBandwidth", "I"));
	//pch->mModulation = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mModulation", "I"),0);
	//pch->mSymbolRate = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mSymbolRate", "I"),0);
	//pch->mFEMisc = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mFEMisc", "I"),0);
	pch->mVideoPID = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mVideoPid", "I"));
	pch->mVideoFormat = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mVfmt", "I"));
	pch->mPcrPID = (*env)->GetIntField(env, channel, (*env)->GetFieldID(env, objclass, "mPcrPid", "I"));
	jintArray aids = (jintArray)(*env)->GetObjectField(env, channel, (*env)->GetFieldID(env, objclass, "mAudioPids", "[I"));
	jintArray afmts = (jintArray)(*env)->GetObjectField(env, channel, (*env)->GetFieldID(env, objclass, "mAudioFormats", "[I"));
	jobjectArray alangs = (jobjectArray)(*env)->GetObjectField(env, channel, (*env)->GetFieldID(env, objclass, "mAudioLangs", "[Ljava/lang/String;"));
	if (aids && afmts) {
		jint *paids = (*env)->GetIntArrayElements(env, aids, 0);
		jint *pafmts = (*env)->GetIntArrayElements(env, afmts, 0);
		pch->mAudioInfo.audio_count = (*env)->GetArrayLength(env, aids);
		for (i=0; i<pch->mAudioInfo.audio_count; i++) {
			jstring jstr = (*env)->GetObjectArrayElement(env, alangs, i);
			const char *str = (char *)(*env)->GetStringUTFChars(env, jstr, 0);
			pch->mAudioInfo.audios[i].pid = paids[i];
			pch->mAudioInfo.audios[i].fmt = pafmts[i];
			strncpy(pch->mAudioInfo.audios[i].lang, str, 10);
			(*env)->ReleaseStringUTFChars(env, jstr, str);
		}
		(*env)->ReleaseIntArrayElements(env, aids, paids, JNI_ABORT);
		(*env)->ReleaseIntArrayElements(env, afmts, pafmts, JNI_ABORT);
	}
	pch->valid = 1;
	return 0;
}

static void epg_monitor_service(JNIEnv* env, jobject obj, jobject channel)
{
	EPGData *data;
	AM_ErrorCode_t ret;
	EPGChannelData *pch = &gChannelMonitored;
	int ts_id, srv_id;

	//channel = NULL;

	int err = get_channel_data(env, obj, channel, pch);
	if (err) {
		log_error("EPGMonitorService get channel data failed");
		return;
	}

	if (pch->valid) {
		ts_id = pch->mTransportStreamId;
		srv_id = pch->mServiceId;
	} else {
		ts_id = srv_id = -1;
	}

	data = (EPGData*)((*env)->GetLongField(env, obj, gHandleID));
	ret = AM_EPG_MonitorServiceByID(data->handle, ts_id, srv_id, PMT_Update);
	if (ret != AM_SUCCESS) {
		log_error("AM_EPG_MonitorService failed");
	}
}

static void epg_set_dvb_text_coding(JNIEnv* env, jobject obj, jstring coding)
{
	const char *str = (*env)->GetStringUTFChars(env, coding, 0);

	UNUSED(obj);

	if (str != NULL) {
		if (!strcmp(str, "standard")) {
			AM_SI_SetDefaultDVBTextCoding("");
		} else {
			AM_SI_SetDefaultDVBTextCoding(str);
		}

		(*env)->ReleaseStringUTFChars(env, coding, str);
	}
}

static JNINativeMethod epg_methods[] =
{
	/* name, signature, funcPtr */
	{"native_epg_create", "(IIILjava/lang/String;)V", (void*)epg_create},
	{"native_epg_destroy", "()V", (void*)epg_destroy},
	{"native_epg_change_mode", "(II)V", (void*)epg_change_mode},
	{"native_epg_monitor_service", "(Lcom/droidlogic/app/tv/ChannelInfo;)V", (void*)epg_monitor_service},
	{"native_epg_set_dvb_text_coding", "(Ljava/lang/String;)V", (void*)epg_set_dvb_text_coding}
};

//JNIHelp.h ????
#ifndef NELEM
# define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif
static int registerNativeMethods(JNIEnv* env, const char* className,
                                 const JNINativeMethod* methods, int numMethods)
{
	int rc;
	jclass clazz;

	clazz = (*env)->FindClass(env, className);

	if (clazz == NULL)
		return -1;

	if ((rc = ((*env)->RegisterNatives(env, clazz, methods, numMethods))) < 0)
		return -1;

	return 0;
}

JNIEXPORT jint
JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;
	jclass clazz;

	UNUSED(reserved);

	if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK)
		return -1;

	if (registerNativeMethods(env, "com/droidlogic/tvinput/services/DTVEpgScanner", epg_methods, NELEM(epg_methods)) < 0)
		return -1;

	gJavaVM = vm;

	clazz = (*env)->FindClass(env, "com/droidlogic/tvinput/services/DTVEpgScanner");
	if (clazz == NULL) {
		log_error("FindClass com/droidlogic/tvinput/services/DTVEpgScanner failed");
		return -1;
	}

	gOnEventID = (*env)->GetMethodID(env, clazz, "onEvent", "(Lcom/droidlogic/tvinput/services/DTVEpgScanner$Event;)V");
	gHandleID = (*env)->GetFieldID(env, clazz, "native_handle", "J");
	gEventClass       = (*env)->FindClass(env, "com/droidlogic/tvinput/services/DTVEpgScanner$Event");
	gEventClass       = (jclass)(*env)->NewGlobalRef(env, (jobject)gEventClass);
	gEventInitID      = (*env)->GetMethodID(env, gEventClass, "<init>", "(Lcom/droidlogic/tvinput/services/DTVEpgScanner;I)V");

	gEvtClass       = (*env)->FindClass(env, "com/droidlogic/tvinput/services/DTVEpgScanner$Event$Evt");
	gEvtClass       = (jclass)(*env)->NewGlobalRef(env, (jobject)gEvtClass);
	gEvtInitID      = (*env)->GetMethodID(env, gEvtClass, "<init>", "(Lcom/droidlogic/tvinput/services/DTVEpgScanner$Event;)V");

	gChannelClass   = (*env)->FindClass(env, "com/droidlogic/app/tv/ChannelInfo");
	gChannelClass   = (jclass)(*env)->NewGlobalRef(env, (jobject)gChannelClass);
	gChannelInitID  = (*env)->GetMethodID(env, gChannelClass, "<init>", "()V");

	return JNI_VERSION_1_4;
}

JNIEXPORT void
JNI_OnUnload(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;

	UNUSED(reserved);

	if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		return;
	}

	(*env)->DeleteGlobalRef(env, (jobject)gChannelClass);
	(*env)->DeleteGlobalRef(env, (jobject)gEventClass);
	(*env)->DeleteGlobalRef(env, (jobject)gEvtClass);
}

