LOCAL_PATH := $(call my-dir)

DVB_PATH := $(wildcard external/dvb)
ifeq ($(DVB_PATH), )
	DVB_PATH := $(wildcard vendor/amlogic/external/dvb)
endif
ifeq ($(DVB_PATH), )
	DVB_PATH := $(wildcard vendor/amlogic/dvb)
endif

#######################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnidtvsubtitle
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := DTVSubtitle.cpp
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := external/libzvbi/src \
	$(DVB_PATH)/include/am_mw \
	$(DVB_PATH)/include/am_adp \
	bionic/libc/include \
	external/skia/include\
	$(DVB_PATH)/android/ndk/include

LOCAL_SHARED_LIBRARIES += libzvbi libam_mw libam_adp libskia liblog libcutils

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#######################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnidtvepgscanner
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := DTVEpgScanner.c
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := external/libzvbi/src \
	external/sqlite/dist \
	bionic/libc/include \
	$(DVB_PATH)/include/am_mw \
	$(DVB_PATH)/include/am_adp \
	$(DVB_PATH)/android/ndk/include

LOCAL_SHARED_LIBRARIES += libzvbi libam_mw libam_adp libskia liblog

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#######################################################################

