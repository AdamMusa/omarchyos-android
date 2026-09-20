#include "ShellPaths.h"

#include <QDir>
#include <QFile>
#include <QStandardPaths>
#include <QProcessEnvironment>
#include <QDirIterator>
#include <QDebug>
#include <QJsonDocument>
#include <QJsonObject>
#include <QSaveFile>
#include <QElapsedTimer>
#include <QRegularExpression>

namespace {
// Shipped by the OS image: device/omarchy installs the Omarchy tree here.
constexpr auto kSystemOmarchyPath = "/product/etc/omarchy";
}

ShellPaths::ShellPaths(QObject *parent)
    : QObject(parent)
{
    const auto env = QProcessEnvironment::systemEnvironment();
    m_home = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);

    // The OS image installs Omarchy at /product/etc/omarchy. When that is not
    // there — a sideloaded shell, or first boot before the OTA lands — the copy
    // packaged with the app is unpacked into app storage instead, because the
    // theme files have to be real, watchable files for live theme switching.
    const QString systemPath = QString::fromLatin1(kSystemOmarchyPath);
    const bool systemTree = QFile::exists(systemPath + QStringLiteral("/shell/shell.qml"));
    m_omarchyPath = env.value(QStringLiteral("OMARCHY_PATH"),
                              systemTree ? systemPath : m_home + QStringLiteral("/omarchy"));

    m_stateHome = m_home + QStringLiteral("/.local/state");
    m_configHome = m_home + QStringLiteral("/.config/omarchy");

#ifdef Q_OS_ANDROID
    // Only ever unpack into the app's own directory, and only when that is
    // where omarchyPath points — the deploy step clears its target first, so it
    // must never run against a tree the app does not own.
    if (!systemTree && m_omarchyPath == m_home + QStringLiteral("/omarchy")) deployOmarchyTree();
#endif
    ensureUserConfig();
}

void ShellPaths::ensureUserConfig()
{
    QDir().mkpath(m_configHome);
    QDir().mkpath(m_stateHome + QStringLiteral("/omarchy/current"));

    // Default theme link, mirroring ~/.local/state/omarchy/current/theme.
    const QString currentTheme = m_stateHome + QStringLiteral("/omarchy/current/theme");
    if (!QFile::exists(currentTheme)) {
        const QString defaultTheme = m_omarchyPath + QStringLiteral("/themes/") + QStringLiteral(OMARCHY_DEFAULT_THEME);
        QFile::link(defaultTheme, currentTheme);
    }

    // Current wallpaper, mirroring ~/.local/state/omarchy/current/background.
    // Keep the actual Omarchy desktop artwork where it is shipped. Upgrade
    // only the old generated default, leaving custom wallpaper links intact.
    const QString currentBackground = m_stateHome + QStringLiteral("/omarchy/current/background");
    const QString desktopWallpaper = m_omarchyPath + QStringLiteral("/wallpapers/")
                                     + QStringLiteral(OMARCHY_DEFAULT_THEME) + QStringLiteral("/omarchy.png");
    if (QFile::exists(desktopWallpaper)
        && QFile::symLinkTarget(currentBackground) == m_omarchyPath + QStringLiteral("/wallpapers/")
           + QStringLiteral(OMARCHY_DEFAULT_THEME) + QStringLiteral("/omarchyos-")
           + QStringLiteral(OMARCHY_DEFAULT_THEME) + QStringLiteral(".png"))
        QFile::remove(currentBackground);
    if (!QFile::exists(currentBackground)) {
        if (QFileInfo(currentBackground).isSymLink()) QFile::remove(currentBackground);
        const QString wallpapers = m_omarchyPath + QStringLiteral("/wallpapers/")
                                   + QStringLiteral(OMARCHY_DEFAULT_THEME);
        const QStringList shipped = QDir(wallpapers).entryList(
            { QStringLiteral("*.png"), QStringLiteral("*.jpg") }, QDir::Files, QDir::Name);
        if (QFile::exists(desktopWallpaper))
            QFile::link(desktopWallpaper, currentBackground);
        else if (!shipped.isEmpty())
            QFile::link(wallpapers + QLatin1Char('/') + shipped.first(), currentBackground);
        else
            qWarning("omarchy-shell: no wallpaper shipped for theme %s", OMARCHY_DEFAULT_THEME);
    }

    // shell.json: user config wins, defaults come from the image.
    const QString userShellJson = m_configHome + QStringLiteral("/shell.json");
    if (!QFile::exists(userShellJson)) {
        QFile::copy(m_omarchyPath + QStringLiteral("/config/omarchy/shell.json"), userShellJson);
        QFile(userShellJson).setPermissions(QFile::ReadOwner | QFile::WriteOwner);
    }

    // Upgrade the shipped desktop defaults once, preserving the user's theme,
    // wallpaper and unrelated preferences. Retain the old layout for recovery.
    QFile current(userShellJson);
    QFile defaults(m_omarchyPath + QStringLiteral("/config/omarchy/shell.json"));
    if (current.open(QIODevice::ReadOnly) && defaults.open(QIODevice::ReadOnly)) {
        QJsonObject config = QJsonDocument::fromJson(current.readAll()).object();
        const QJsonObject mobile = QJsonDocument::fromJson(defaults.readAll()).object();
        if (mobile.value("mobileProfile").toInt() > config.value("mobileProfile").toInt()) {
            current.close();
            QFile::copy(userShellJson, userShellJson + QStringLiteral(".before-mobile"));
            for (const auto *key : {"bar", "disabledPlugins", "plugins", "mobileProfile"})
                config.insert(QLatin1String(key), mobile.value(QLatin1String(key)));
            QSaveFile updated(userShellJson);
            if (updated.open(QIODevice::WriteOnly)) {
                updated.write(QJsonDocument(config).toJson());
                updated.commit();
            }
        }
    }
}

void ShellPaths::deployOmarchyTree()
{
    // Resource -> disk copy, skipping files that are already current. Themes
    // must exist as real files: Omarchy's Color/Style singletons read
    // theme/colors.toml and shell.toml through FileView and watch them for
    // live theme switching.
    QElapsedTimer timer;
    timer.start();
    QFile assetRevision(QStringLiteral("assets:/omarchy/asset-revision.txt"));
    const QString revision = assetRevision.open(QIODevice::ReadOnly)
        ? QString::fromLatin1(assetRevision.readAll()).trimmed() : QString();
    if (!QRegularExpression(QStringLiteral("^[a-f0-9]{64}$")).match(revision).hasMatch())
        qFatal("omarchy-shell: missing or invalid packaged asset revision");
    const QString marker = m_omarchyPath + QStringLiteral("/.deployed-") + revision;
    const QStringList required = { QStringLiteral("/shell/shell.qml"),
                                   QStringLiteral("/shell/Commons/qmldir"),
                                   QStringLiteral("/default/fonts/omarchy/omarchy.ttf") };
    bool intact = QFile::exists(marker);
    for (const QString &path : required)
        intact = intact && QFileInfo(m_omarchyPath + path).size() > 0;
    if (intact) {
        qInfo("omarchy-shell: assets reused in %lld ms", qlonglong(timer.elapsed()));
        return;
    }

    if (m_omarchyPath != m_home + QStringLiteral("/omarchy")) {
        qWarning("omarchy-shell: refusing to deploy over %s", qPrintable(m_omarchyPath));
        return;
    }
    QDir(m_omarchyPath).removeRecursively();
    QDir().mkpath(m_omarchyPath);

    bool complete = true;
    int files = 0;
    QDirIterator it(QStringLiteral("assets:/omarchy"), QDir::Files, QDirIterator::Subdirectories);
    while (it.hasNext()) {
        const QString src = it.next();
        ++files;
        const QString dest = m_omarchyPath + src.mid(QStringLiteral("assets:/omarchy").size());
        QDir().mkpath(QFileInfo(dest).absolutePath());
        QFile input(src);
        QSaveFile output(dest);
        if (!input.open(QIODevice::ReadOnly) || !output.open(QIODevice::WriteOnly)) {
            complete = false;
            continue;
        }
        bool copied = true;
        while (!input.atEnd()) {
            const QByteArray chunk = input.read(64 * 1024);
            if (chunk.isEmpty() || output.write(chunk) != chunk.size()) {
                copied = false;
                break;
            }
        }
        if (!copied) output.cancelWriting();
        // QSaveFile commits only a fully written file and syncs it to disk.
        // A simulator power-off during deployment must not leave zero-byte
        // QML modules behind a successful deployment marker.
        if (!copied || !output.commit()) complete = false;
    }

    qInfo("omarchy-shell: assets deployed %d files in %lld ms", files, qlonglong(timer.elapsed()));

    // Upstream imports its own code as `qs.*`, which Quickshell resolves by
    // exposing the shell directory under that name. Android assets carry no
    // symlinks, so the link is recreated after unpacking.
    const QString qsAlias = m_omarchyPath + QStringLiteral("/qs");
    QFile::remove(qsAlias);
    if (!QFile::link(m_omarchyPath + QStringLiteral("/shell"), qsAlias))
        qWarning("omarchy-shell: could not create the qs alias for the shell tree");

    if (complete) {
        QSaveFile savedMarker(marker);
        if (savedMarker.open(QIODevice::WriteOnly)) {
            savedMarker.write("complete\n");
            savedMarker.commit();
        }
    } else {
        qWarning("omarchy-shell: incomplete asset deployment; will retry at next launch");
    }
}

QStringList ShellPaths::qmlImportPaths() const
{
    // OMARCHY_SHELL_ROOT lets a build host point at the source tree; on device
    // both live under the image's Omarchy directory.
    const auto env = QProcessEnvironment::systemEnvironment();
#ifdef Q_OS_ANDROID
    // Compat modules are compiled into the binary's QML resources; only the
    // Omarchy tree needs a filesystem import path.
    return { m_omarchyPath };
#else
    const QString appRoot = env.value(QStringLiteral("OMARCHY_SHELL_ROOT"),
                                      QStringLiteral("/product/etc/omarchy-shell"));
    return { appRoot + QStringLiteral("/quickshell-compat"), m_omarchyPath };
#endif
}
