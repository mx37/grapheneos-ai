# Install script for directory: /home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/usr/local")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Release")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "1")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set path to fallback-tool for dependency-resolution.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/home/satory/Android/Sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/arm64/ggml/src/cmake_install.cmake")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml.so")
    file(RPATH_CHECK
         FILE "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml.so"
         RPATH "")
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE SHARED_LIBRARY FILES "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/arm64/bin/libggml.so")
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml.so")
    if(CMAKE_INSTALL_DO_STRIP)
      execute_process(COMMAND "/home/satory/Android/Sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml.so")
    endif()
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE FILE FILES
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-cpu.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-alloc.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-backend.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-blas.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-cann.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-cpp.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-cuda.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-opt.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-metal.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-rpc.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-virtgpu.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-sycl.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-vulkan.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-webgpu.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/ggml-zendnn.h"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/llama.cpp-src/ggml/include/gguf.h"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml-base.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml-base.so")
    file(RPATH_CHECK
         FILE "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml-base.so"
         RPATH "")
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE SHARED_LIBRARY FILES "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/arm64/bin/libggml-base.so")
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml-base.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml-base.so")
    if(CMAKE_INSTALL_DO_STRIP)
      execute_process(COMMAND "/home/satory/Android/Sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libggml-base.so")
    endif()
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/ggml" TYPE FILE FILES
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/arm64/ggml/ggml-config.cmake"
    "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/arm64/ggml/ggml-version.cmake"
    )
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
if(CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "/home/satory/Documents/grapheneosai/app/src/main/cpp/llama/build-android/arm64/ggml/install_local_manifest.txt"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()
