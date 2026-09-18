// OmarchyShell — host process for Omarchy's QML shell on Android.
//
// One long-running process owns every shell surface, the same way Omarchy 4's
// Quickshell host does on the desktop: bar, menu, panels, notifications, OSD
// and lock live as windows of this process rather than as separate apps.
#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickStyle>
#include <QLoggingCategory>

#include "AndroidBridge.h"
#include "ShellPaths.h"

int main(int argc, char *argv[])
{
    QGuiApplication app(argc, argv);
    app.setApplicationName(QStringLiteral("omarchy-shell"));
    app.setOrganizationName(QStringLiteral("OmarchyOS"));
    QQuickStyle::setStyle(QStringLiteral("Basic"));

    ShellPaths paths;
    AndroidBridge bridge;

    QQmlApplicationEngine engine;
    // quickshell-compat provides the `Quickshell*` modules; the vendored
    // Omarchy tree provides `qs.*` exactly as upstream lays it out.
    // Compat modules first, then the vendored Omarchy tree (its qmldirs
    // declare the `qs.*` modules the shell imports).
    for (const QString &path : paths.qmlImportPaths())
        engine.addImportPath(path);
    engine.rootContext()->setContextProperty(QStringLiteral("AndroidBridge"), &bridge);
    engine.rootContext()->setContextProperty(QStringLiteral("ShellPaths"), &paths);

    QObject::connect(&engine, &QQmlApplicationEngine::objectCreationFailed, &app,
                     []() { QCoreApplication::exit(-1); }, Qt::QueuedConnection);
    engine.loadFromModule("OmarchyShell", "Main");
    return app.exec();
}
