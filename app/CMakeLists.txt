cmake_minimum_required(VERSION 3.22.1)

project("helium_tv_native" C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# C++ Native Hardware, FPS & Memory Buffer Engine
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
