
APP_ABI      :=  armeabi-v7a arm64-v8a

APP_PLATFORM:=android-16

APP_STL :=  c++_static

APP_CFLAGS += -O2

APP_CPPFLAGS += -std=c++11
APP_CPPFLAGS += -std=c++14

APP_CFLAGS += -mllvm -arm-assume-misaligned-load-store

APP_LDFLAGS += -Wl,-z,max-page-size=16384