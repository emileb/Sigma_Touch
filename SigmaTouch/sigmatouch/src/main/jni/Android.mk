TOP_DIR := $(call my-dir)
LOCAL_PATH := $(call my-dir)


# AVP (NakedAVP) is an SDL3 engine, so libSDL3.so is built alongside SDL2.
SDL3_ENABLED=1

include $(TOP_DIR)/SDL2_OpenTouch/Android.mk
include $(TOP_DIR)/Clibs_OpenTouch/Android.mk
include $(TOP_DIR)/Clibs_OpenTouch/jpeg8d/Android.mk

include $(TOP_DIR)/../../../../../SAFFAL/saffal/src/main/jni/Android.mk

include $(TOP_DIR)/AudioLibs_OpenTouch/fluidsynth-lite/src/Android.mk

include $(TOP_DIR)/gl4es/Android.mk
#include $(TOP_DIR)/AudioLibs_OpenTouch/openal-soft/Android.mk
include $(TOP_DIR)/AudioLibs_OpenTouch/libmad/Android.mk

include $(TOP_DIR)/MobileTouchControls/Android.mk
