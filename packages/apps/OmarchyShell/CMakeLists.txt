cmake_minimum_required(VERSION 3.21)
project(OmarchyShellProject VERSION 1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)

find_package(Qt6 6.8 REQUIRED COMPONENTS Core Gui Quick Qml QuickControls2)
qt_standard_project_setup(REQUIRES 6.8)

# Quickshell — the base module: QML surface types plus the C++ enum namespaces.
qt_add_library(quickshell_base STATIC)
target_include_directories(quickshell_base PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/app/src)
target_link_libraries(quickshell_base PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
set_source_files_properties(quickshell-compat/Quickshell/ShellSurfaceRoot.qml
    PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
set_source_files_properties(quickshell-compat/Quickshell/Quickshell.qml
    PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
set_source_files_properties(quickshell-compat/Quickshell/DesktopEntries.qml
    PROPERTIES QT_QML_SINGLETON_TYPE TRUE)

qt_add_qml_module(quickshell_base
    URI Quickshell
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell
    SOURCES app/src/QsEnums.h
    QML_FILES
        quickshell-compat/Quickshell/AnchorRect.qml
        quickshell-compat/Quickshell/DesktopEntries.qml
        quickshell-compat/Quickshell/FloatingWindow.qml
        quickshell-compat/Quickshell/IconImage.qml
        quickshell-compat/Quickshell/LazyLoader.qml
        quickshell-compat/Quickshell/PanelWindow.qml
        quickshell-compat/Quickshell/PersistentProperties.qml
        quickshell-compat/Quickshell/PopupAnchor.qml
        quickshell-compat/Quickshell/PopupWindow.qml
        quickshell-compat/Quickshell/QsMenuOpener.qml
        quickshell-compat/Quickshell/QsWindow.qml
        quickshell-compat/Quickshell/Quickshell.qml
        quickshell-compat/Quickshell/Region.qml
        quickshell-compat/Quickshell/Scope.qml
        quickshell-compat/Quickshell/PanelAnchors.qml
        quickshell-compat/Quickshell/PanelMargins.qml
        quickshell-compat/Quickshell/QsObjectModel.qml
        quickshell-compat/Quickshell/ShellRoot.qml
        quickshell-compat/Quickshell/TransformWatcher.qml
        quickshell-compat/Quickshell/ShellSurfaceRoot.qml
        quickshell-compat/Quickshell/SurfaceFormat.qml
        quickshell-compat/Quickshell/SystemClock.qml
        quickshell-compat/Quickshell/Variants.qml
)

# Quickshell.Wayland — attached layer-shell types (C++ half).
qt_add_library(quickshell_wayland STATIC)
target_include_directories(quickshell_wayland PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/app/src)
target_link_libraries(quickshell_wayland PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
set_source_files_properties(quickshell-compat/Quickshell/Wayland/ToplevelManager.qml
    PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
qt_add_qml_module(quickshell_wayland
    URI Quickshell.Wayland
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Wayland
    SOURCES app/src/QsWayland.h
    QML_FILES
        quickshell-compat/Quickshell/Wayland/IdleMonitor.qml
        quickshell-compat/Quickshell/Wayland/ToplevelManager.qml
        quickshell-compat/Quickshell/Wayland/WlSessionLock.qml
        quickshell-compat/Quickshell/Wayland/WlSessionLockSurface.qml
)

# Quickshell.Io — the C++ half of the compat layer (Process, FileView, IpcHandler).
qt_add_library(quickshell_io STATIC)
target_include_directories(quickshell_io PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/app/src)
target_link_libraries(quickshell_io PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_io
    URI Quickshell.Io
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Io
    SOURCES
        app/src/QsIo.cpp
        app/src/QsIo.h
)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/app/src)

# The remaining Quickshell modules are pure QML; each is a real QML module so
# it ships inside the app on Android rather than relying on an import path.

set_source_files_properties(quickshell-compat/Quickshell/Hyprland/Hyprland.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
qt_add_library(quickshell_hyprland STATIC)
target_link_libraries(quickshell_hyprland PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_hyprland
    URI Quickshell.Hyprland
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Hyprland
    QML_FILES
        quickshell-compat/Quickshell/Hyprland/Hyprland.qml
        quickshell-compat/Quickshell/Hyprland/HyprlandFocusGrab.qml
)

set_source_files_properties(quickshell-compat/Quickshell/Services/Pipewire/Pipewire.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
qt_add_library(quickshell_pipewire STATIC)
target_link_libraries(quickshell_pipewire PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_pipewire
    URI Quickshell.Services.Pipewire
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Services/Pipewire
    QML_FILES
        quickshell-compat/Quickshell/Services/Pipewire/Pipewire.qml
        quickshell-compat/Quickshell/Services/Pipewire/PwNodePeakMonitor.qml
        quickshell-compat/Quickshell/Services/Pipewire/PwObjectTracker.qml
)

set_source_files_properties(quickshell-compat/Quickshell/Services/UPower/UPower.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
qt_add_library(quickshell_upower STATIC)
target_link_libraries(quickshell_upower PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_upower
    URI Quickshell.Services.UPower
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Services/UPower
    QML_FILES
        quickshell-compat/Quickshell/Services/UPower/UPower.qml
)

set_source_files_properties(quickshell-compat/Quickshell/Services/Mpris/Mpris.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
qt_add_library(quickshell_mpris STATIC)
target_link_libraries(quickshell_mpris PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_mpris
    URI Quickshell.Services.Mpris
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Services/Mpris
    QML_FILES
        quickshell-compat/Quickshell/Services/Mpris/Mpris.qml
)

qt_add_library(quickshell_notifications STATIC)
target_link_libraries(quickshell_notifications PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_notifications
    URI Quickshell.Services.Notifications
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Services/Notifications
    QML_FILES
        quickshell-compat/Quickshell/Services/Notifications/NotificationServer.qml
        quickshell-compat/Quickshell/Services/Notifications/Notification.qml
)

set_source_files_properties(quickshell-compat/Quickshell/Services/SystemTray/SystemTray.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
qt_add_library(quickshell_systemtray STATIC)
target_link_libraries(quickshell_systemtray PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_systemtray
    URI Quickshell.Services.SystemTray
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Services/SystemTray
    QML_FILES
        quickshell-compat/Quickshell/Services/SystemTray/SystemTray.qml
)

qt_add_library(quickshell_pam STATIC)
target_link_libraries(quickshell_pam PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_pam
    URI Quickshell.Services.Pam
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Services/Pam
    QML_FILES
        quickshell-compat/Quickshell/Services/Pam/PamContext.qml
)

qt_add_library(quickshell_polkit STATIC)
target_link_libraries(quickshell_polkit PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_polkit
    URI Quickshell.Services.Polkit
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Services/Polkit
    QML_FILES
        quickshell-compat/Quickshell/Services/Polkit/PolkitAgent.qml
)

set_source_files_properties(quickshell-compat/Quickshell/Networking/NetworkManager.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
qt_add_library(quickshell_networking STATIC)
target_link_libraries(quickshell_networking PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_networking
    URI Quickshell.Networking
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Networking
    QML_FILES
        quickshell-compat/Quickshell/Networking/NetworkManager.qml
)

set_source_files_properties(quickshell-compat/Quickshell/Bluetooth/Bluetooth.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)
qt_add_library(quickshell_bluetooth STATIC)
target_link_libraries(quickshell_bluetooth PUBLIC Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml)
qt_add_qml_module(quickshell_bluetooth
    URI Quickshell.Bluetooth
    VERSION 1.0
    OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/modules/Quickshell/Bluetooth
    QML_FILES
        quickshell-compat/Quickshell/Bluetooth/Bluetooth.qml
)

qt_add_executable(omarchyshell
    app/src/main.cpp
    app/src/AndroidBridge.cpp
    app/src/AndroidBridge.h
    app/src/ShellPaths.cpp
    app/src/ShellPaths.h
)

set(OMARCHY_BASH_BINARY "/toolchain/bash-android/libbash.so"
    CACHE FILEPATH "bash built for Android by tools/build-bash.sh")

string(TIMESTAMP OMARCHY_SHELL_BUILD_ID "%Y%m%d%H%M" UTC)
# Omarchy's own default theme, used for the first-run current-theme link.
set(OMARCHY_DEFAULT_THEME "tokyo-night" CACHE STRING "Theme selected on first run")
target_compile_definitions(omarchyshell PRIVATE
    OMARCHY_SHELL_BUILD_ID="${OMARCHY_SHELL_BUILD_ID}"
    OMARCHY_DEFAULT_THEME="${OMARCHY_DEFAULT_THEME}")

if(ANDROID)
    # Ship Omarchy itself inside the package: shell QML, config defaults and
    # all 23 themes, so theming on the phone is literally Omarchy's. They go in
    # as Android assets rather than Qt resources — it keeps the binary small and
    # lets the shell unpack real, watchable files at first run. The `qs`
    # compatibility symlink is skipped so nothing is packaged twice.
    set(OMARCHY_TREE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/omarchy)
    set(OMARCHY_ASSETS ${CMAKE_CURRENT_SOURCE_DIR}/app/android/assets/omarchy)
    add_custom_target(omarchy_assets ALL
        COMMAND ${CMAKE_COMMAND} -E rm -rf ${OMARCHY_ASSETS}
        COMMAND ${CMAKE_COMMAND} -E make_directory ${OMARCHY_ASSETS}
        COMMAND rsync -a --exclude qs ${OMARCHY_TREE_DIR}/shell ${OMARCHY_TREE_DIR}/config
                ${OMARCHY_TREE_DIR}/default ${OMARCHY_TREE_DIR}/themes ${OMARCHY_ASSETS}/
        # The vendored tree stays byte-identical to upstream; what this Qt
        # cannot parse is fixed on the staged copy.
        COMMAND bash ${CMAKE_CURRENT_SOURCE_DIR}/third_party/patches/apply.sh ${OMARCHY_ASSETS}
        COMMAND python3 ${CMAKE_CURRENT_SOURCE_DIR}/mobile/apply.py ${OMARCHY_ASSETS}
        # OmarchyOS's own assets: the Nerd Font the desktop installs through
        # pacman, and one portrait wallpaper per theme.
        COMMAND rsync -a ${CMAKE_CURRENT_SOURCE_DIR}/fonts ${CMAKE_CURRENT_SOURCE_DIR}/wallpapers
                ${OMARCHY_ASSETS}/
        COMMENT "Staging the Omarchy tree into the Android package")
    add_dependencies(omarchyshell omarchy_assets)

    # bash goes in as a jniLib rather than a Qt "extra library": it must land
    # in the app's lib directory (the only place Android allows an app to
    # execute from), but it is an executable, so Qt must not dlopen it.
    add_custom_target(omarchy_bash ALL
        COMMAND ${CMAKE_COMMAND} -E make_directory
                ${CMAKE_CURRENT_SOURCE_DIR}/app/android/libs/arm64-v8a
        COMMAND ${CMAKE_COMMAND} -E copy_if_different ${OMARCHY_BASH_BINARY}
                ${CMAKE_CURRENT_SOURCE_DIR}/app/android/libs/arm64-v8a/libbash.so
        COMMENT "Staging bash into the Android package")
    add_dependencies(omarchyshell omarchy_bash)
endif()

qt_add_qml_module(omarchyshell
    URI OmarchyShell
    VERSION 1.0
    QML_FILES
        app/qml/Main.qml
        app/qml/ShellHost.qml
)

target_link_libraries(omarchyshell PRIVATE
    Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml Qt6::QuickControls2
    quickshell_base quickshell_baseplugin
    quickshell_io quickshell_ioplugin
    quickshell_wayland quickshell_waylandplugin
    quickshell_hyprland quickshell_hyprlandplugin quickshell_pipewire quickshell_pipewireplugin quickshell_upower quickshell_upowerplugin quickshell_mpris quickshell_mprisplugin quickshell_notifications quickshell_notificationsplugin quickshell_systemtray quickshell_systemtrayplugin quickshell_pam quickshell_pamplugin quickshell_polkit quickshell_polkitplugin quickshell_networking quickshell_networkingplugin quickshell_bluetooth quickshell_bluetoothplugin)

# The compat QML modules and the vendored Omarchy tree ride along as files next
# to the binary on the host build, and as assets in the Android package.
set_target_properties(omarchyshell PROPERTIES
    QT_ANDROID_PACKAGE_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/app/android
    QT_ANDROID_TARGET_SDK_VERSION 35
    QT_ANDROID_MIN_SDK_VERSION 34
    QT_ANDROID_EXTRA_LIBS ""
)
