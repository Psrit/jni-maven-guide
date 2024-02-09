cmake_minimum_required(VERSION 3.15)

project(jni-guide LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(JAVA_INCLUDE_ROOT $ENV{JAVA_HOME}/include)

# TODO: add platform checking
set(JAVA_PLATFORM_INCLUDE_DIR ${JAVA_INCLUDE_ROOT}/linux)

set(JNI_OBJECT_INCLUDE_DIR ${PROJECT_SOURCE_DIR}/target/headers)

set(C_SOURCE_DIR ${PROJECT_SOURCE_DIR}/src/main/c)

add_library(jni-lib SHARED
    ${C_SOURCE_DIR}/Sample01.cpp
)
target_include_directories(jni-lib
    PUBLIC
    ${C_SOURCE_DIR}
    ${JNI_OBJECT_INCLUDE_DIR}
    ${JAVA_INCLUDE_ROOT}
    ${JAVA_PLATFORM_INCLUDE_DIR}
)
target_compile_options(jni-lib
    PUBLIC -Wall
)

string(TOLOWER ${CMAKE_HOST_SYSTEM_NAME} OS_TYPE)
string(TOLOWER ${CMAKE_HOST_SYSTEM_PROCESSOR} PROCESSOR_TYPE)
if (${PROCESSOR_TYPE} STREQUAL "x86_64")
    set(PROCESSOR_TYPE "x64")
elseif(${PROCESSOR_TYPE} STREQUAL "arm64")
    set(PROCESSOR_TYPE "arm64")
elseif(${PROCESSOR_TYPE} STREQUAL "amd64")
    set(PROCESSOR_TYPE "x64")
elseif(${PROCESSOR_TYPE} STREQUAL "i386")
    set(PROCESSOR_TYPE "x86")
elseif(${PROCESSOR_TYPE} STREQUAL "i486")
    set(PROCESSOR_TYPE "x86")
elseif(${PROCESSOR_TYPE} STREQUAL "i586")
    set(PROCESSOR_TYPE "x86")
elseif(${PROCESSOR_TYPE} STREQUAL "i686")
    set(PROCESSOR_TYPE "x86")
elseif(${PROCESSOR_TYPE} STREQUAL "i786")
    set(PROCESSOR_TYPE "x86")
elseif(${PROCESSOR_TYPE} STREQUAL "i886")
    set(PROCESSOR_TYPE "x86")
else()
    message(FATAL_ERROR "Unsupported architect type ${PROCESSOR_TYPE}")
endif()

file(GENERATE OUTPUT copy_libs.cmake
    CONTENT "file(COPY $<TARGET_FILE:jni-lib> DESTINATION ${PROJECT_SOURCE_DIR}/clib/${OS_TYPE}-${PROCESSOR_TYPE})"
)
add_custom_command(TARGET jni-lib
    POST_BUILD COMMAND ${CMAKE_COMMAND} -P copy_libs.cmake)