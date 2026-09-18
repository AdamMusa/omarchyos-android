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
qt_add_executable(omarchyshell
    app/src/main.cpp
    app/src/AndroidBridge.cpp
    app/src/AndroidBridge.h
    app/src/ShellPaths.cpp
    app/src/ShellPaths.h
)

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
    quickshell_wayland quickshell_waylandplugin)

# The compat QML modules and the vendored Omarchy tree ride along as files next
# to the binary on the host build, and as assets in the Android package.
set_target_properties(omarchyshell PROPERTIES
    QT_ANDROID_PACKAGE_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/app/android
    QT_ANDROID_TARGET_SDK_VERSION 35
    QT_ANDROID_MIN_SDK_VERSION 34
    QT_ANDROID_EXTRA_LIBS ""
)
