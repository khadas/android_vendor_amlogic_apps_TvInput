LOCAL_PATH := $(call my-dir)

DVB_PATH := $(wildcard external/dvb)
ifeq ($(DVB_PATH), )
	DVB_PATH := $(wildcard $(BOARD_AML_VENDOR_PATH)/external/dvb)
endif
ifeq ($(DVB_PATH), )
	DVB_PATH := $(wildcard vendor/amlogic/external/dvb)
endif
ifeq ($(DVB_PATH), )
	DVB_PATH := $(wildcard $(BOARD_AML_VENDOR_PATH)/dvb)
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
	$(DVB_PATH)/android/ndk/include \
	vendor/amlogic/external/libzvbi/src \
	$(BOARD_AML_VENDOR_PATH)/external/libzvbi/src

LOCAL_SHARED_LIBRARIES += libjnigraphics libzvbi libam_mw libam_adp libskia liblog libcutils

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

LOCAL_SHARED_LIBRARIES += libzvbi libam_mw libam_adp libskia liblog libcutils

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
#######################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := libvendorfont
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE_TAGS := optional

ifdef TARGET_2ND_ARCH
LOCAL_MULTILIB := both
LOCAL_MODULE_PATH_64 := $(TARGET_OUT)/lib64
LOCAL_SRC_FILES_64 := arm64/libvendorfont.so
LOCAL_MODULE_PATH_32 := $(TARGET_OUT)/lib
LOCAL_SRC_FILES_32 := arm/libvendorfont.so
else
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
LOCAL_SRC_FILES := arm/libvendorfont.so
endif
include $(BUILD_PREBUILT)

#######################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnifont
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := Fonts.cpp
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := \
    $(JNI_H_INCLUDE) \
    libnativehelper/include_jni \
    libnativehelper/include/nativehelper

LOCAL_SHARED_LIBRARIES += libvendorfont liblog libnativehelper libandroid_runtime libcutils

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#######################################################################
