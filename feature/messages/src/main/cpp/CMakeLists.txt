cmake_minimum_required(VERSION 3.22.1)
project("oboe_voicemorph")

# Find Google Oboe package exported via Prefab
find_package(oboe REQUIRED CONFIG)

add_library(oboe_voicemorph SHARED
    OboeVoiceTransformerStream.cpp
)

target_include_directories(oboe_voicemorph PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(oboe_voicemorph
    oboe::oboe
    android
    log
)
