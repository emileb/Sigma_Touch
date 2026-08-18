cmake_minimum_required(VERSION 3.22)
project(quake4_builder)

# openQ4 is an SDL3 engine (like :AVP, unlike :Unreal / :UT99), so it uses the
# SDL-3.x checkout and libSDL3.so built by ndk-build.
set(SDL3_ANDROID_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/../../SDL2_OpenTouch/SDL-3.x/include")
set(SDL2_ANDROID_LIBS_DIR "${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}")
set(CLIBS_DIR "${CMAKE_SOURCE_DIR}/../../Clibs_OpenTouch")
set(TOUCH_CONTROLS_DIR "${CMAKE_SOURCE_DIR}/../../MobileTouchControls")

# OpenAL (openal-soft): openQ4's only sound backend is sound/OpenAL/*.
set(OPENAL_DIR "${CMAKE_SOURCE_DIR}/../../AudioLibs_OpenTouch/openal-soft")
if (DEFINED CMAKE_BUILD_TYPE AND CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(OPENAL_LIBRARY "${OPENAL_DIR}/build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib/${ANDROID_ABI}/libopenal-soft.so")
else ()
    set(OPENAL_LIBRARY "${OPENAL_DIR}/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/${ANDROID_ABI}/libopenal-soft.so")
endif ()
# openQ4 includes <AL/al.h>, so the parent of AL/ goes on the path.
set(OPENAL_INCLUDE_DIR "${OPENAL_DIR}/src/main/jni/openal/include")

# Fail the build if any shared library has unresolved symbols.
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
# calls bind to it.
add_library(saffal SHARED IMPORTED)
set_target_properties(saffal PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libsaffal.so")

set(SDL3_INCLUDE_DIR "${SDL3_ANDROID_INCLUDE_DIR}")
set(SDL3_LIBRARY "${SDL2_ANDROID_LIBS_DIR}/libSDL3.so")

# openQ4 consumes the game-library sources straight out of the companion repo.
set(OPENQ4_GAMELIBS_DIR "${CMAKE_SOURCE_DIR}/src/main/jni/openQ4-game")

add_subdirectory("src/main/jni/openQ4")
