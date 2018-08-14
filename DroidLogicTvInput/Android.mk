LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_CERTIFICATE := platform
LOCAL_PACKAGE_NAME := DroidLogicTvInput

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_JAVA_LIBRARIES := droidlogic droidlogic-tv

LOCAL_JNI_SHARED_LIBRARIES := \
    libjnidtvsubtitle \
    libjnidtvepgscanner \
    libjnifont

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_PROPRIETARY_MODULE := true
endif

#LOCAL_PRIVILEGED_MODULE := true
LOCAL_PRIVATE_PLATFORM_APIS := true

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))

