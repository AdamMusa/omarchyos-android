cmake_minimum_required(VERSION 3.21)
project(OmarchyShell VERSION 1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)

find_package(Qt6 6.8 REQUIRED COMPONENTS Core Gui Quick Qml QuickControls2)
qt_standard_project_setup(REQUIRES 6.8)

qt_add_executable(omarchyshell
    src/main.cpp
    src/AndroidBridge.cpp
    src/ShellPaths.cpp
)

qt_add_qml_module(omarchyshell
    URI OmarchyShell
    VERSION 1.0
    QML_FILES
        qml/Main.qml
        qml/ShellHost.qml
)

target_link_libraries(omarchyshell PRIVATE Qt6::Core Qt6::Gui Qt6::Quick Qt6::Qml Qt6::QuickControls2)

# The Quickshell API surface and the vendored Omarchy shell ship as QML
# resources so the app runs self-contained on device.
qt_add_resources(omarchyshell "shellfiles"
    PREFIX "/"
    BASE "${CMAKE_CURRENT_SOURCE_DIR}/.."
    FILES ${OMARCHY_SHELL_FILES}
)

set_property(TARGET omarchyshell PROPERTY QT_ANDROID_PACKAGE_SOURCE_DIR
    ${CMAKE_CURRENT_SOURCE_DIR}/android)
set_property(TARGET omarchyshell PROPERTY QT_ANDROID_TARGET_SDK_VERSION 35)
set_property(TARGET omarchyshell PROPERTY QT_ANDROID_MIN_SDK_VERSION 34)
set_property(TARGET omarchyshell PROPERTY QT_ANDROID_PACKAGE_NAME os.omarchy.shell)
