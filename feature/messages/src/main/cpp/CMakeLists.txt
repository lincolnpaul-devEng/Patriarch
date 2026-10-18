cmake_minimum_required(VERSION 3.22.1)
project("oboe_voicemorph")

# Find Google Oboe package exported via Prefab
find_package(oboe REQUIRED CONFIG)

add_library(oboe_voicemorph SHARED
    OboeVoiceTransformerStream.cpp
)

# Binary hardening: stack canaries, fortified libc calls, hidden symbols, PIC,
# full RELRO + immediate binding, non-executable stack, dead-code stripping.
target_compile_options(oboe_voicemorph PRIVATE
    -fstack-protector-strong
    -D_FORTIFY_SOURCE=2
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -fPIC
    -ffunction-sections
    -fdata-sections
    -Wformat -Wformat-security -Werror=format-security
)
target_link_options(oboe_voicemorph PRIVATE
    -Wl,-z,relro,-z,now
    -Wl,-z,noexecstack
    -Wl,--gc-sections
    -Wl,--exclude-libs,ALL
    -Wl,--build-id=none
    -s
)

target_include_directories(oboe_voicemorph PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(oboe_voicemorph
    oboe::oboe
    android
    log
)
