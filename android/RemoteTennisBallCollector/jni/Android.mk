LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
OPENCV_CAMERA_MODULES:=off
include $(OPENCV_PACKAGE_DIR)/share/OpenCV/OpenCV.mk
LOCAL_MODULE    := TennisBallCollector
LOCAL_SRC_FILES := jni_part.cpp

include $(BUILD_SHARED_LIBRARY)