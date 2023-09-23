project(MujdeRel)
cmake_minimum_required(VERSION 3.10)

file(GLOB_RECURSE SOURCE_FILES "*.c" "*.cpp" "*.h" "*.hpp")
set(CMAKE_TOOLCHAIN_FILE "C:\\Tools\\android-ndk-r25c\\build\\cmake\\android.toolchain.cmake")

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})
target_compile_options(${PROJECT_NAME} PRIVATE -pedantic -Wall -Wextra -Werror)
target_include_directories(${PROJECT_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/Headers)
