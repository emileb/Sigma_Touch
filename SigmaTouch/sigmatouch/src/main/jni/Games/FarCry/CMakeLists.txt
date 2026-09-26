cmake_minimum_required(VERSION 3.22)
project(farcry_builder)

# NearChuckle is an SDL3 engine (like :AVP and :Quake4), so it uses the SDL-3.x
# checkout and libSDL3.so built by ndk-build.
set(SDL3_ANDROID_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/../../SDL2_OpenTouch/SDL-3.x/include")
set(SDL2_ANDROID_LIBS_DIR "${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}")
set(CLIBS_DIR "${CMAKE_SOURCE_DIR}/../../Clibs_OpenTouch")
set(TOUCH_CONTROLS_DIR "${CMAKE_SOURCE_DIR}/../../MobileTouchControls")
set(AUDIO_LIBS_DIR "${CMAKE_SOURCE_DIR}/../../AudioLibs_OpenTouch")

# OpenAL (openal-soft): CrySoundSystem's only non-Windows backend is OpenALSound.cpp.
set(OPENAL_DIR "${AUDIO_LIBS_DIR}/openal-soft")
if (DEFINED CMAKE_BUILD_TYPE AND CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(OPENAL_LIBRARY "${OPENAL_DIR}/build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib/${ANDROID_ABI}/libopenal-soft.so")
else ()
    set(OPENAL_LIBRARY "${OPENAL_DIR}/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/${ANDROID_ABI}/libopenal-soft.so")
endif ()
# The engine includes <AL/al.h>, so the parent of AL/ goes on the path.
set(OPENAL_INCLUDE_DIR "${OPENAL_DIR}/src/main/jni/openal/include")

# Fail the build if any shared library has unresolved symbols. The engine's
# Windows-era #pragma comment(lib, ...) lines become .deplibs entries under
# clang, which lld would otherwise try to resolve.
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--no-undefined -Wl,--no-dependent-libraries")

add_library(SDL3 SHARED IMPORTED)
set_target_properties(SDL3 PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libSDL3.so")

add_library(touchcontrols SHARED IMPORTED)
set_target_properties(touchcontrols PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libtouchcontrols.so")

# Named "openal" because that is the name CrySoundSystem's CMake links against.
add_library(openal SHARED IMPORTED)
set_target_properties(openal PROPERTIES
    IMPORTED_LOCATION "${OPENAL_LIBRARY}"
    INTERFACE_INCLUDE_DIRECTORIES "${OPENAL_INCLUDE_DIR}")

# SAFFAL filesystem interception layer: exports libc file symbols (fopen, open,
# stat, ...) that forward to the real libc, routing the engine's file access
# through scoped-storage. Linked directly (and before libc) so the engine's own
# calls bind to it.
add_library(saffal SHARED IMPORTED)
set_target_properties(saffal PROPERTIES IMPORTED_LOCATION "${SDL2_ANDROID_LIBS_DIR}/libsaffal.so")

# libvorbisfile, static, from the shared AudioLibs sources: CrySoundSystem's OGG
# decoder uses the ov_* API and links the target by this name.
set(OGGVORBIS_DIR "${AUDIO_LIBS_DIR}/liboggvorbis")
file(GLOB OGG_SOURCES "${OGGVORBIS_DIR}/libogg/*.c")
file(GLOB VORBIS_SOURCES "${OGGVORBIS_DIR}/libvorbis/*.c")
# Decoder only: the encoder needs mode tables the tree does not carry.
list(FILTER VORBIS_SOURCES EXCLUDE REGEX "vorbisenc\\.c$")
add_library(vorbisfile STATIC ${OGG_SOURCES} ${VORBIS_SOURCES})
target_include_directories(vorbisfile PUBLIC "${OGGVORBIS_DIR}/include")
target_compile_options(vorbisfile PRIVATE -ffast-math -fsigned-char -fPIC -w)

set(SDL3_INCLUDE_DIR "${SDL3_ANDROID_INCLUDE_DIR}")
set(SDL3_LIBRARY "${SDL2_ANDROID_LIBS_DIR}/libSDL3.so")

# CryCommon's Linux_Win32Wrapper.h includes <SDL3/SDL.h> and its GetTickCount()
# calls SDL_GetTicks(), so every module needs SDL3; Linux resolves that lazily at
# dlopen time, --no-undefined does not.
include_directories("${SDL3_INCLUDE_DIR}")
link_libraries(SDL3)
# Every module opens files itself (fopen, opendir, stat), and a dlopen()ed module only binds to SAFFAL
# when it is its own dependency: without this, secondary-storage paths from those modules go to libc.
link_libraries(saffal)

# The GL renderer needs Cg to compile shaders; the ES module loads the precompiled
# ARB assembly from the shader cache pak instead.
set(DISABLE_CG ON CACHE BOOL "" FORCE)

add_subdirectory("src/main/jni/NearChuckle")
