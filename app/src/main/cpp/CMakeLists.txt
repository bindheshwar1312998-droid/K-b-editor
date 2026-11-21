cmake_minimum_required(VERSION 3.22.1)
project("kbeditor_native")

add_library(kbeditor_native SHARED
    kbeditor_native.cpp
)

target_compile_options(kbeditor_native PRIVATE -O3)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

target_link_libraries(kbeditor_native
    ${log-lib}
    ${jnigraphics-lib}
    android
)
