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
#include <QQuickWindow>
#include <QSurfaceFormat>
#include <QSGRendererInterface>
#include <QTimer>
#include <QImage>
#include <QFile>
#include <QFont>
#include <QFontDatabase>
#include <QDir>
#include <QDirIterator>
#include <QRegularExpression>

#include "AndroidBridge.h"
#include "ShellPaths.h"

int main(int argc, char *argv[])
{
    // Some emulated GPUs (the Android emulator's gfxstream among them) hand
    // Qt a context it cannot draw the scene graph with, which shows up as a
    // window that paints its clear colour and nothing else. A marker file in
    // app storage switches the renderer without needing a new build.
    {
        // A one-line marker file in app storage picks the renderer without a
        // rebuild: "software", "vulkan" or "opengl". Emulated GPUs differ in
        // which of the three they can actually hand Qt.
        QFile marker(QStringLiteral("/data/data/os.omarchy.shell/files/.render-backend"));
        if (marker.open(QIODevice::ReadOnly)) {
            // The file is a whitespace-separated list: the first token names the
            // backend, any later token is a flag. Passing the whole line to
            // QSG_RHI_BACKEND made Qt reject it ("Unknown key") and fall back.
            const QStringList tokens = QString::fromUtf8(marker.readAll())
                                           .toLower()
                                           .split(QRegularExpression("\\s+"), Qt::SkipEmptyParts);
            const QString backend = tokens.value(0);
            if (backend == QLatin1String("software")) {
                qputenv("QT_QUICK_BACKEND", "software");
            } else if (!backend.isEmpty() && backend != QLatin1String("default")) {
                qputenv("QSG_RHI_BACKEND", backend.toUtf8());
            }
            if (tokens.contains(QLatin1String("debug"))) {
                // Surfaces Qt's own EGL/RHI decisions in logcat, which is the
                // only way to see which config it asked the driver for.
                qputenv("QT_LOGGING_RULES", "qt.qpa.gl*=true;qt.qpa.egl*=true;qt.rhi.*=true");
            }
            qInfo("omarchy-shell: render backend requested: %s (flags: %s)",
                  qPrintable(backend), qPrintable(tokens.mid(1).join(QLatin1Char(','))));
        }
    }


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
    // Omarchy's glyphs come from fonts the desktop installs system-wide: its
    // own icon font for the menu and agent marks, and a Nerd Font behind the
    // fontconfig alias `monospace` for everything else. Android installs
    // neither, so the shell registers what it ships and points `monospace` at
    // the Nerd Font, which is what upstream's font bindings resolve through.
    {
        const QString omarchy = paths.omarchyPath();
        QStringList iconFamilies;
        QString monospaceFamily;
        for (const QString &dir : { omarchy + QStringLiteral("/default/fonts"),
                                    omarchy + QStringLiteral("/fonts") }) {
            QDirIterator it(dir, { QStringLiteral("*.ttf"), QStringLiteral("*.otf") },
                            QDir::Files, QDirIterator::Subdirectories);
            while (it.hasNext()) {
                const QString file = it.next();
                const int id = QFontDatabase::addApplicationFont(file);
                if (id < 0) {
                    qWarning("omarchy-shell: could not load font %s", qPrintable(file));
                    continue;
                }
                const QStringList families = QFontDatabase::applicationFontFamilies(id);
                iconFamilies += families;
                if (monospaceFamily.isEmpty())
                    for (const QString &family : families)
                        if (family.contains(QLatin1String("Nerd"), Qt::CaseInsensitive)
                            || family.contains(QLatin1String("Mono"), Qt::CaseInsensitive))
                            monospaceFamily = family;
            }
        }
        if (!monospaceFamily.isEmpty()) {
            // Upstream binds font.family to Style.fontFamily, which is the
            // literal "monospace"; the substitution is how that alias reaches
            // the bundled font, the same way fontconfig does on the desktop.
            QFont::insertSubstitution(QStringLiteral("monospace"), monospaceFamily);
            QFont::insertSubstitution(QStringLiteral("Monospace"), monospaceFamily);
        }
        qInfo("omarchy-shell: fonts registered: %s (monospace -> %s)",
              qPrintable(iconFamilies.join(QLatin1String(", "))),
              monospaceFamily.isEmpty() ? "system" : qPrintable(monospaceFamily));
    }

    engine.loadFromModule("OmarchyShell", "Main");

    // Render health. A blank screen has two very different causes — a scene
    // with nothing in it, or frames that never reach the Android surface — and
    // only the swap count tells them apart from outside the process.
    {
        auto *swaps = new int(0);
        QTimer::singleShot(0, &app, [&engine, swaps, &bridge] {
            const auto roots = engine.rootObjects();
            if (roots.isEmpty()) return;
            auto *window = qobject_cast<QQuickWindow *>(roots.first());
            if (!window) return;
            QObject::connect(window, &QQuickWindow::frameSwapped, window, [swaps] { ++*swaps; });
            QObject::connect(window, &QQuickWindow::sceneGraphError, window,
                             [](QQuickWindow::SceneGraphError, const QString &message) {
                                 qWarning("omarchy-shell: scene graph error: %s", qPrintable(message));
                             });
            auto *report = new QTimer(window);
            report->setInterval(5000);
            QObject::connect(report, &QTimer::timeout, window, [window, swaps, &bridge] {
                // More than one top-level window would mean the scene being
                // inspected is not the one on screen.
                const auto windows = QGuiApplication::allWindows();
                QStringList others;
                for (QWindow *w : windows)
                    if (w != window)
                        others << QStringLiteral("%1 %2x%3 vis=%4")
                                      .arg(w->metaObject()->className())
                                      .arg(w->width()).arg(w->height()).arg(int(w->isVisible()));
                qInfo("omarchy-shell: render swaps=%d idle=%ds exposed=%d visible=%d size=%dx%d dpr=%.3f "
                      "windows=%lld%s%s",
                      *swaps, bridge.idleSeconds(),
                      int(window->isExposed()), int(window->isVisible()),
                      window->width(), window->height(), window->devicePixelRatio(),
                      qlonglong(windows.size()), others.isEmpty() ? "" : " others: ",
                      qPrintable(others.join(QLatin1String("; "))));
            });
            report->start();
        });
    }

    // OMARCHY_SHELL_GRAB writes one frame to disk a few seconds after start.
    // Rendering bugs look identical to layout bugs from the outside; a grab
    // taken inside the process tells them apart.
    // "path" or "path@seconds": the shell takes tens of seconds to build its
    // surfaces, so a grab worth comparing with a screenshot has to be taken
    // after the scene is populated, not while it is still empty.
    QString grabPath = qEnvironmentVariable("OMARCHY_SHELL_GRAB");
    int grabDelayMs = 7000;
    if (const int at = grabPath.lastIndexOf(QLatin1Char('@')); at > 0) {
        bool ok = false;
        const int seconds = QStringView(grabPath).mid(at + 1).toInt(&ok);
        if (ok && seconds > 0) {
            grabDelayMs = seconds * 1000;
            grabPath.truncate(at);
        }
    }
    if (!grabPath.isEmpty()) {
        QTimer::singleShot(grabDelayMs, &app, [&engine, grabPath] {
            const auto roots = engine.rootObjects();
            if (roots.isEmpty()) return;
            auto *window = qobject_cast<QQuickWindow *>(roots.first());
            if (!window) return;
            const QImage frame = window->grabWindow();
            qInfo("omarchy-shell: grab %dx%d saved=%d", frame.width(), frame.height(),
                  int(frame.save(grabPath)));
        });
    }

    return app.exec();
}
