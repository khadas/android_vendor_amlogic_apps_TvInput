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
LOCAL_C_INCLUDES := \
  bionic/libc/include \
  external/skia/include\
  external/skia/include/core \
  external/skia/include/config \
  libnativehelper/include_jni \
  frameworks/native/include

#DVB define
ifeq ($(BOARD_HAS_ADTV),true)
LOCAL_CFLAGS += -DSUPPORT_ADTV

LOCAL_C_INCLUDES += \
  external/libzvbi/src \
	$(DVB_PATH)/include/am_mw \
	$(DVB_PATH)/include/am_adp \
	$(DVB_PATH)/android/ndk/include \
	vendor/amlogic/external/libzvbi/src \
	$(BOARD_AML_VENDOR_PATH)/external/libzvbi/src

LOCAL_SHARED_LIBRARIES += \
  libzvbi \
  libam_mw \
  libam_adp
endif

LOCAL_SHARED_LIBRARIES += \
  liblog \
  libcutils

#LOCAL_STATIC_LIBRARIES := libskia

LOCAL_PRELINK_MODULE := false

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_PROPRIETARY_MODULE := true
endif

include $(BUILD_SHARED_LIBRARY)

#######################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnidtvepgscanner
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := DTVEpgScanner.c
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := \
	external/sqlite/dist \
	external/skia/include\
	external/skia/include/core \
	external/skia/include/config \
	libnativehelper/include_jni \
	bionic/libc/include

LOCAL_SHARED_LIBRARIES += \
  liblog \
  libcutils

#LOCAL_STATIC_LIBRARIES := libskia

LOCAL_PRELINK_MODULE := false

#DVB define
ifeq ($(BOARD_HAS_ADTV),true)
LOCAL_CFLAGS += -DSUPPORT_ADTV

LOCAL_C_INCLUDES += \
  external/libzvbi/src \
	$(DVB_PATH)/include/am_mw \
	$(DVB_PATH)/include/am_adp \
	$(DVB_PATH)/android/ndk/include \

LOCAL_SHARED_LIBRARIES += \
  libzvbi \
  libam_mw \
  libam_adp
endif

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_PROPRIETARY_MODULE := true
endif

include $(BUILD_SHARED_LIBRARY)
#######################################################################

include $(CLEAR_VARS)

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
MULTI_FONT_PATH_32 := $(TARGET_OUT_VENDOR)/lib/
MULTI_FONT_PATH_64 := $(TARGET_OUT_VENDOR)/lib64/
else
MULTI_FONT_PATH_32 := $(TARGET_OUT)/lib/
MULTI_FONT_PATH_64 := $(TARGET_OUT)/lib64/
endif

LOCAL_MODULE    := libvendorfont
LOCAL_MULTILIB := both
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

LOCAL_MODULE_PATH_32 := $(MULTI_FONT_PATH_32)
LOCAL_MODULE_PATH_64 := $(MULTI_FONT_PATH_64)
LOCAL_SRC_FILES_arm := arm/$(LOCAL_MODULE)$(LOCAL_MODULE_SUFFIX)
LOCAL_SRC_FILES_arm64 := arm64/$(LOCAL_MODULE)$(LOCAL_MODULE_SUFFIX)

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
    system/core/libutils/include \
    system/core/liblog/include \
    libnativehelper/include/nativehelper

LOCAL_SHARED_LIBRARIES += libvendorfont liblog libcutils

LOCAL_PRELINK_MODULE := false

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_PROPRIETARY_MODULE := true
endif

include $(BUILD_SHARED_LIBRARY)

#######################################################################
