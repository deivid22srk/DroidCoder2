cmake_minimum_required(VERSION 3.22.1)
project("droidcoder_native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find OpenSSL (for SHA-256)
find_package(OpenSSL REQUIRED)

# Native library
add_library(
    droidcoder_native
    SHARED
    native-lib.cpp
)

# Link libraries
target_link_libraries(
    droidcoder_native
    OpenSSL::Crypto
    android
    log
)
