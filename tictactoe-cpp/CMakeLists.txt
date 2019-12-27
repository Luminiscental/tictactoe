cmake_minimum_required (VERSION 2.7)
project(tictactoe)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

add_executable(tictactoe "${PROJECT_SOURCE_DIR}/main.cpp")
