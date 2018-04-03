LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_CERTIFICATE := platform
LOCAL_PACKAGE_NAME := DroidLogicFactoryMenu

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_JAVA_LIBRARIES := droidlogic droidlogic-tv

LOCAL_PRIVATE_PLATFORM_APIS := true

include $(BUILD_PACKAGE)
