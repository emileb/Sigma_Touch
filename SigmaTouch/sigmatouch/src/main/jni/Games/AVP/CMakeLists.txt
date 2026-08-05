cmake_minimum_required(VERSION 3.15)
project(avp_builder)

# NakedAVP is an SDL3 engine (unlike :Unreal / :UT99, which are SDL2), so it
# uses the SDL-3.x checkout and libSDL3.so built by ndk-build.
set(SDL3_ANDROID_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/../../SDL2_OpenTouch/SDL-3.x/include")
set(SDL2_ANDROID_LIBS_DIR "${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}")
set(CLIBS_DIR "${CMAKE_SOURCE_DIR}/../../Clibs_OpenTouch")
set(TOUCH_CONTROLS_DIR "${CMAKE_SOURCE_DIR}/../../MobileTouchControls")

# OpenAL (openal-soft): AVP's sound backend (src/openal.c) is OpenAL only. Pull
# in the .so the :openal Gradle module produced, same as the other engines.
set(OPENAL_DIR "${CMAKE_SOURCE_DIR}/../../AudioLibs_OpenTouch/openal-soft")
if (DEFINED CMAKE_BUILD_TYPE AND CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(OPENAL_LIBRARY "${OPENAL_DIR}/build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib/${ANDROID_ABI}/libopenal-soft.so")
else ()
    set(OPENAL_LIBRARY "${OPENAL_DIR}/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/${ANDROID_ABI}/libopenal-soft.so")
endif ()
# AVP includes <al.h>/<alc.h> unprefixed, so the AL/ folder itself goes on the path.
set(OPENAL_INCLUDE_DIR "${OPENAL_DIR}/src/main/jni/openal/include/AL")

# FFmpeg: prebuilt static PIC libs shared with q2repro, rebuilt with the Bink
# demuxer and decoders for AvP's intros/outros and music. See docs/engines/avp.md.
set(FFMPEG_INCLUDE_DIR "${CLIBS_DIR}/ffmpeg/${ANDROID_ABI}/include")
set(FFMPEG_LIBRARY_DIR "${CLIBS_DIR}/ffmpeg/${ANDROID_ABI}/lib")

# Fail the build if the shared library has any unresolved symbols
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--no-undefined")

add_library(SDL3 SHARED IMPORTED)
set_target_properties(SDL3 PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libSDL3.so")

add_library(touchcontrols SHARED IMPORTED)
set_target_properties(touchcontrols PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libtouchcontrols.so")

add_library(openal-soft SHARED IMPORTED)
set_target_properties(openal-soft PROPERTIES IMPORTED_LOCATION "${OPENAL_LIBRARY}")

# SAFFAL filesystem interception layer: exports libc file symbols (fopen, open,
# stat, ...) that forward to the real libc, routing the engine's file access
# through scoped-storage. Linked directly (and before libc) so the engine's own
# calls bind to it, the same way MobileTouchControls links saffal.
add_library(saffal SHARED IMPORTED)
set_target_properties(saffal PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libsaffal.so")

# libGL4ES is deliberately *not* an imported target: the engine dlopen()s it at
# runtime (src/oglfunc.c) and resolves every GL entry point from it, so there is
# no link-time dependency. See "AVP (NakedAVP) engine notes" in CLAUDE.md.

set(SDL3_INCLUDE_DIR "${SDL3_ANDROID_INCLUDE_DIR}")
set(SDL3_LIBRARY "${SDL2_ANDROID_LIBS_DIR}/libSDL3.so")

add_subdirectory("src/main/jni/NakedAVP")
