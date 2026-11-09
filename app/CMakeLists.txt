cmake_minimum_required(VERSION 3.22.1)

project("helium_tv_native_bridge" C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 1. Compile C++ Native Hardware & FPS Engine
add_library(
    helium_native
    SHARED
    src/main/cpp/helium_native.cpp
)

find_library(log-lib log)
target_link_libraries(
    helium_native
    ${log-lib}
)

# 2. Integrate Rust Native Adblocker Engine via Cargo
set(RUST_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/src/main/rust")
set(RUST_TARGET "")

if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(RUST_TARGET "aarch64-linux-android")
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(RUST_TARGET "armv7-linux-androideabi")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    set(RUST_TARGET "x86_64-linux-android")
elseif(${ANDROID_ABI} STREQUAL "x86")
    set(RUST_TARGET "i686-linux-android")
endif()

if(NOT "${RUST_TARGET}" STREQUAL "")
    add_custom_target(
        cargo_build_helium_adblock
        COMMAND cargo build --target ${RUST_TARGET} --release
        WORKING_DIRECTORY ${RUST_SRC_DIR}
        COMMENT "Compiling Rust Adblock Engine with Cargo for ${RUST_TARGET}"
    )

    add_library(helium_adblock SHARED IMPORTED)
    set_target_properties(
        helium_adblock
        PROPERTIES
        IMPORTED_LOCATION "${RUST_SRC_DIR}/target/${RUST_TARGET}/release/libhelium_adblock.so"
    )

    add_dependencies(helium_native cargo_build_helium_adblock)
endif()
