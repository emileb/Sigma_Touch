cmake_minimum_required(VERSION 4.1)
project(unreal_builder)

set(SDL2_ANDROID_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/../../SDL2_OpenTouch/SDL2-2.0.12/include")
set(SDL2_IMAGE_ANDROID_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/../../SDL2_OpenTouch/SDL2_image")
set(SDL2_ANDROID_LIBS_DIR "${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}")
set(CLIBS_DIR "${CMAKE_SOURCE_DIR}/../../Clibs_OpenTouch")
set(TOUCH_CONTROLS_DIR "${CMAKE_SOURCE_DIR}/../../MobileTouchControls")

# OpenAL (openal-soft): pull in the .so the :openal Gradle module produced, the
# same way openal-soft itself imports oboe's merged_native_libs output.
set(OPENAL_DIR "${CMAKE_SOURCE_DIR}/../../AudioLibs_OpenTouch/openal-soft")
if (DEFINED CMAKE_BUILD_TYPE AND CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(OPENAL_LIBRARY "${OPENAL_DIR}/build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib/${ANDROID_ABI}/libopenal-soft.so")
else ()
    set(OPENAL_LIBRARY "${OPENAL_DIR}/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/${ANDROID_ABI}/libopenal-soft.so")
endif ()
set(OPENAL_INCLUDE_DIR "${OPENAL_DIR}/src/main/jni/openal/include")

# libxmp (lite): NOpenALDrv (Source/NOpenALDrv) needs it for tracker music
# playback. Pull in the .so the :libxmp Gradle module produced, same pattern
# as openal-soft/oboe above.
set(LIBXMP_DIR "${CMAKE_SOURCE_DIR}/../../AudioLibs_OpenTouch/libxmp")
if (DEFINED CMAKE_BUILD_TYPE AND CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(LIBXMP_LIBRARY "${LIBXMP_DIR}/build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib/${ANDROID_ABI}/libxmp-lite.so")
else ()
    set(LIBXMP_LIBRARY "${LIBXMP_DIR}/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/${ANDROID_ABI}/libxmp-lite.so")
endif ()
set(LIBXMP_INCLUDE_DIR "${LIBXMP_DIR}/src/main/jni/libxmp/include")

# Fail the build if the shared library has any unresolved symbols
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--no-undefined")

add_library(SDL2 SHARED IMPORTED)
set_target_properties(SDL2 PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libSDL2.so")

add_library(SDL2_image SHARED IMPORTED)
set_target_properties(SDL2_image PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libSDL2_image.so")

add_library(touchcontrols SHARED IMPORTED)
set_target_properties(touchcontrols PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libtouchcontrols.so")

add_library(openal-soft SHARED IMPORTED)
set_target_properties(openal-soft PROPERTIES IMPORTED_LOCATION "${OPENAL_LIBRARY}")

add_library(xmp-lite SHARED IMPORTED)
set_target_properties(xmp-lite PROPERTIES IMPORTED_LOCATION "${LIBXMP_LIBRARY}")

# SAFFAL filesystem interception layer: exports libc file symbols (fopen, open,
# stat, ...) that forward to the real libc, routing the engine's file access
# through scoped-storage. Linked directly (and before libc) so the engine's own
# calls bind to it, the same way MobileTouchControls links saffal.
add_library(saffal SHARED IMPORTED)
set_target_properties(saffal PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libsaffal.so")

# UE1 (Source/CMakeLists.txt) honours these if set before add_subdirectory, so
# point it at the prebuilt SDL2 shared lib rather than letting it find its own.
set(SDL2_INCLUDE_DIR "${SDL2_ANDROID_INCLUDE_DIR}")
set(SDL2_LIBRARY "${SDL2_ANDROID_LIBS_DIR}/libSDL2.so")

add_subdirectory("src/main/jni/UE1/Source")
