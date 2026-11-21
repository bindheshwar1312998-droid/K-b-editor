cmake_minimum_required(VERSION 3.22.1)
project("kbeditor_native")

if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/rnnoise/denoise.c")
    file(GLOB RNNOISE_SRCS "${CMAKE_CURRENT_SOURCE_DIR}/rnnoise/*.c")
    list(FILTER RNNOISE_SRCS EXCLUDE REGEX ".*(dump_weights_blob|rnnoise_demo|write_weights|test).*\\.c$")
    add_library(kbeditor_native SHARED
        kbeditor_native.cpp
        ${RNNOISE_SRCS}
    )
    target_include_directories(kbeditor_native PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/rnnoise")
    target_compile_definitions(kbeditor_native PRIVATE HAVE_RNNOISE=1)
else()
    add_library(kbeditor_native SHARED
        kbeditor_native.cpp
    )
endif()

target_compile_options(kbeditor_native PRIVATE -O3 -ffast-math)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

target_link_libraries(kbeditor_native
    ${log-lib}
    ${jnigraphics-lib}
    android
)
