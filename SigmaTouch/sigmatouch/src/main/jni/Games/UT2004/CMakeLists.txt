cmake_minimum_required(VERSION 3.15)
project(ut2004_builder)

set(JNI_DIR "${CMAKE_SOURCE_DIR}/../..")
set(PREBUILT_LIBS_DIR "${JNI_DIR}/../libs/${ANDROID_ABI}")
set(SDL2_INCLUDE_DIR "${JNI_DIR}/SDL2_OpenTouch/SDL2-2.0.12/include")
set(SDL2_IMAGE_INCLUDE_DIR "${JNI_DIR}/SDL2_OpenTouch/SDL2_image")
set(CLIBS_DIR "${JNI_DIR}/Clibs_OpenTouch")
set(TOUCH_CONTROLS_DIR "${JNI_DIR}/MobileTouchControls")

# Fail the build if the shared library has any unresolved symbols
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--no-undefined")

# The app's prebuilt SDL2 under the name the engine links.
add_library(SDL2::SDL2-static SHARED IMPORTED)
set_target_properties(SDL2::SDL2-static PROPERTIES
    IMPORTED_LOCATION "${PREBUILT_LIBS_DIR}/libSDL2.so"
    INTERFACE_INCLUDE_DIRECTORIES "${SDL2_INCLUDE_DIR}")

add_library(SDL2_image SHARED IMPORTED)
set_target_properties(SDL2_image PROPERTIES IMPORTED_LOCATION "${PREBUILT_LIBS_DIR}/libSDL2_image.so")

add_library(touchcontrols SHARED IMPORTED)
set_target_properties(touchcontrols PROPERTIES IMPORTED_LOCATION "${PREBUILT_LIBS_DIR}/libtouchcontrols.so")

# SAFFAL: exports the libc file calls and routes them through scoped storage; linked before libc.
add_library(saffal SHARED IMPORTED)
set_target_properties(saffal PROPERTIES IMPORTED_LOCATION "${PREBUILT_LIBS_DIR}/libsaffal.so")

# The engine's own gl4es fork and the recovered Karma, as local checkouts instead of fetched.
set(UE_OPENTOUCH ON)
set(FETCHCONTENT_SOURCE_DIR_GL4ES "${CMAKE_SOURCE_DIR}/src/main/jni/gl4es-ut2004")
set(FETCHCONTENT_SOURCE_DIR_METOOLKITKARMADECOMP "${CMAKE_SOURCE_DIR}/src/main/jni/metoolkit-karma-decomp")

add_subdirectory("src/main/jni/engine-ut2004")
