#pragma once
#include <QObject>
#include <QString>
#include <QUrl>
#include <QStringList>

// Resolves the on-device equivalents of the paths Omarchy's shell expects.
// Upstream reads $OMARCHY_PATH, $HOME/.config/omarchy and
// $HOME/.local/state/omarchy/current/theme; on Android those live in the
// system image and in the shell's app data directory.
class ShellPaths : public QObject
{
    Q_OBJECT
    Q_PROPERTY(QString omarchyPath READ omarchyPath CONSTANT)
    Q_PROPERTY(QString home READ home CONSTANT)
    Q_PROPERTY(QString stateHome READ stateHome CONSTANT)
    Q_PROPERTY(QString configHome READ configHome CONSTANT)
    Q_PROPERTY(QUrl shellQmlUrl READ shellQmlUrl CONSTANT)
public:
    explicit ShellPaths(QObject *parent = nullptr);
    QString omarchyPath() const { return m_omarchyPath; }
    QString home() const { return m_home; }
    QString stateHome() const { return m_stateHome; }
    QString configHome() const { return m_configHome; }
    // OMARCHY_SHELL_QML points the host at a different root — used by the
    // probe QML that exercises one compat type at a time.
    QUrl shellQmlUrl() const
    {
        const QString override =
            qEnvironmentVariable("OMARCHY_SHELL_QML");
        return QUrl::fromLocalFile(override.isEmpty()
            ? m_omarchyPath + QStringLiteral("/shell/shell.qml") : override);
    }

    // Seeds $HOME/.config/omarchy and the current-theme state on first run so
    // the shell finds the same files it would on a fresh Omarchy install.
    Q_INVOKABLE void ensureUserConfig();

    // Import paths for quickshell-compat and the vendored Omarchy tree.
    QStringList qmlImportPaths() const;

private:
    // Copies the packaged Omarchy tree out of the app's resources on first run
    // and after an update, so themes and defaults are real files on disk.
    void deployOmarchyTree();

    QString m_omarchyPath;
    QString m_home;
    QString m_stateHome;
    QString m_configHome;
};
