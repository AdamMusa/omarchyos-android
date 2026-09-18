#include "ShellPaths.h"

#include <QDir>
#include <QFile>
#include <QStandardPaths>
#include <QProcessEnvironment>

namespace {
// Shipped by the OS image: device/omarchy installs the Omarchy tree here.
constexpr auto kSystemOmarchyPath = "/product/etc/omarchy";
}

ShellPaths::ShellPaths(QObject *parent)
    : QObject(parent)
{
    const auto env = QProcessEnvironment::systemEnvironment();
    m_omarchyPath = env.value(QStringLiteral("OMARCHY_PATH"),
                              QString::fromLatin1(kSystemOmarchyPath));
    m_home = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
    m_stateHome = m_home + QStringLiteral("/.local/state");
    m_configHome = m_home + QStringLiteral("/.config/omarchy");
    ensureUserConfig();
}

void ShellPaths::ensureUserConfig()
{
    QDir().mkpath(m_configHome);
    QDir().mkpath(m_stateHome + QStringLiteral("/omarchy/current"));

    // Default theme link, mirroring ~/.local/state/omarchy/current/theme.
    const QString currentTheme = m_stateHome + QStringLiteral("/omarchy/current/theme");
    if (!QFile::exists(currentTheme)) {
        const QString defaultTheme = m_omarchyPath + QStringLiteral("/themes/tokyo-night");
        QFile::link(defaultTheme, currentTheme);
    }

    // shell.json: user config wins, defaults come from the image.
    const QString userShellJson = m_configHome + QStringLiteral("/shell.json");
    if (!QFile::exists(userShellJson)) {
        QFile::copy(m_omarchyPath + QStringLiteral("/config/omarchy/shell.json"), userShellJson);
        QFile(userShellJson).setPermissions(QFile::ReadOwner | QFile::WriteOwner);
    }
}

QStringList ShellPaths::qmlImportPaths() const
{
    // OMARCHY_SHELL_ROOT lets a build host point at the source tree; on device
    // both live under the image's Omarchy directory.
    const auto env = QProcessEnvironment::systemEnvironment();
    const QString appRoot = env.value(QStringLiteral("OMARCHY_SHELL_ROOT"),
                                      QStringLiteral("/product/etc/omarchy-shell"));
    return { appRoot + QStringLiteral("/quickshell-compat"), m_omarchyPath };
}
