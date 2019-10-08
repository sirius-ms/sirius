project(CLPWrapper)

# generate JSON of compiler flags for IDE Usage (can be removed)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

set(CMAKE_POSITION_INDEPENDENT_CODE ON) # for compiling shared lib on wsl

cmake_minimum_required (VERSION 3.5)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# DIRECTORIES
# main
set(SRC_MAIN_DIR ${CMAKE_SOURCE_DIR}/fragmentation_tree_construction/src/main/resources)
set(SRC_TEST_DIR ${CMAKE_SOURCE_DIR}/fragmentation_tree_construction/src/test/resources)
include_directories(${SRC_MAIN_DIR})
set(LIBRARY_OUTPUT_PATH ${CMAKE_SOURCE_DIR}/fragmentation_tree_construction/build/resources/main)
# test
include_directories(${SRC_TEST_DIR})
set(EXECUTABLE_OUTPUT_PATH ${CMAKE_SOURCE_DIR}/fragmentation_tree_construction/build/resources/test)

# coin include files
include_directories(/usr/include/coin)

find_library(CLP_COINUTILS CoinUtils HINTS ${CMAKE_SOURCE_DIR}/Clpinst/lib)
find_library(CLP_OSI Osi HINTS ${CMAKE_SOURCE_DIR}/Clpinst/lib)
find_library(CLP_OSICLP OsiClp HINTS ${CMAKE_SOURCE_DIR}/Clpinst/lib)
find_library(CLP_CLP Clp HINTS ${CMAKE_SOURCE_DIR}/Clpinst/lib)

add_library(CLPModel ${SRC_MAIN_DIR}/CLPModel.cpp)
add_library(CLPModelWrapper SHARED ${SRC_MAIN_DIR}/CLPModelWrapper.cpp)
target_link_libraries(CLPModel ${CLP_COINUTILS} ${CLP_OSI} ${CLP_OSICLP} ${CLP_CLP})
target_link_libraries(CLPModelWrapper CLPModel)

###########
# testing #
###########

if(CMAKE_PROJECT_NAME STREQUAL PROJECT_NAME)
  include(CTest)
endif()

if (BUILD_TESTING)
  add_executable(CLPTest ${SRC_TEST_DIR}/CLPTest.cpp)
  target_link_libraries(CLPTest CLPModel)
  add_test(NAME CLPTest COMMAND CLPTest)
endif()
