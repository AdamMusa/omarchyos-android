#pragma once
#include <QObject>
#include <QProcess>
#include <QFileSystemWatcher>
#include <QQmlEngine>
#include <QJSValue>
#include <QVariantMap>

// Quickshell.Io — the pieces of upstream's IO module the Omarchy shell uses.
//
// Process: Omarchy drives the OS through omarchy-* commands (105 call sites).
// Keeping a real process API means those call sites stay untouched; the
// commands themselves are reimplemented for Android in /system_ext/bin.
class QsProcess : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(Process)
    Q_PROPERTY(bool running READ running WRITE setRunning NOTIFY runningChanged)
    Q_PROPERTY(QStringList command READ command WRITE setCommand NOTIFY commandChanged)
    Q_PROPERTY(QString workingDirectory READ workingDirectory WRITE setWorkingDirectory NOTIFY workingDirectoryChanged)
    Q_PROPERTY(QStringList environment READ environment WRITE setEnvironment NOTIFY environmentChanged)
    Q_PROPERTY(QObject *stdout MEMBER m_stdoutParser NOTIFY parsersChanged)
    Q_PROPERTY(QObject *stderr MEMBER m_stderrParser NOTIFY parsersChanged)
public:
    explicit QsProcess(QObject *parent = nullptr);

    bool running() const;
    void setRunning(bool running);
    QStringList command() const { return m_command; }
    void setCommand(const QStringList &command);
    QString workingDirectory() const { return m_workingDirectory; }
    void setWorkingDirectory(const QString &dir);
    QStringList environment() const { return m_environment; }
    void setEnvironment(const QStringList &env);

    Q_INVOKABLE void startDetached();
    Q_INVOKABLE void signal(int sig);
    Q_INVOKABLE void write(const QString &data);

signals:
    void runningChanged();
    void commandChanged();
    void workingDirectoryChanged();
    void environmentChanged();
    void parsersChanged();
    void started();
    void exited(int exitCode, int exitStatus);
    void streamFinished();

private:
    void start();
    QProcess m_process;
    QStringList m_command;
    QString m_workingDirectory;
    QStringList m_environment;
    QObject *m_stdoutParser = nullptr;
    QObject *m_stderrParser = nullptr;
};

// FileView: theme files, shell.json, state flags. Upstream watches these and
// reloads live when a theme switches; the watcher keeps that behaviour.
class QsFileView : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(FileView)
    Q_PROPERTY(QString path READ path WRITE setPath NOTIFY pathChanged)
    Q_PROPERTY(bool watchChanges READ watchChanges WRITE setWatchChanges NOTIFY watchChangesChanged)
    Q_PROPERTY(bool printErrors MEMBER m_printErrors)
    Q_PROPERTY(bool blockLoading MEMBER m_blockLoading)
    // Upstream writes config through a temp file + rename so a half-written
    // shell.json can never be observed; the same guarantee matters more on a
    // phone, where the process can be killed at any moment.
    Q_PROPERTY(bool atomicWrites MEMBER m_atomicWrites)
public:
    explicit QsFileView(QObject *parent = nullptr);

    QString path() const { return m_path; }
    void setPath(const QString &path);
    // Upstream exposes the contents as a function call: view.text()
    Q_INVOKABLE QString text() const { return m_text; }
    Q_INVOKABLE QByteArray data() const { return m_text.toUtf8(); }
    bool watchChanges() const { return m_watchChanges; }
    void setWatchChanges(bool watch);

    Q_INVOKABLE void reload();
    Q_INVOKABLE void setText(const QString &text);
    Q_INVOKABLE void writeAdapter();

signals:
    void pathChanged();
    void textChanged();
    void watchChangesChanged();
    void loaded();
    void loadFailed(const QString &error);
    void fileChanged();

private:
    void load();
    QString m_path;
    QString m_text;
    bool m_watchChanges = false;
    bool m_printErrors = true;
    bool m_blockLoading = false;
    bool m_atomicWrites = true;
    QFileSystemWatcher m_watcher;
};

// Stdout/stderr parsers. Omarchy attaches one of these to a Process and reads
// either whole-output text (StdioCollector) or line-by-line (SplitParser).
class QsStdioCollector : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(StdioCollector)
    Q_PROPERTY(QString data READ text NOTIFY textChanged)
    Q_PROPERTY(bool waitForEnd MEMBER m_waitForEnd)
public:
    using QObject::QObject;
    // Upstream exposes the contents as a function call: view.text()
    Q_INVOKABLE QString text() const { return m_text; }
    Q_INVOKABLE QByteArray data() const { return m_text.toUtf8(); }
    Q_INVOKABLE void feed(const QString &chunk) { m_text += chunk; emit textChanged(); }
    Q_INVOKABLE void finish() { emit streamFinished(); }
signals:
    void textChanged();
    void streamFinished();
private:
    QString m_text;
    bool m_waitForEnd = true;
};

class QsSplitParser : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(SplitParser)
    Q_PROPERTY(QString splitMarker MEMBER m_splitMarker)
public:
    using QObject::QObject;
    Q_INVOKABLE void feed(const QString &chunk);
    Q_INVOKABLE void finish();
signals:
    void read(const QString &data);
    void streamFinished();
private:
    QString m_buffer;
    QString m_splitMarker = QStringLiteral("\n");
};

// IpcHandler: upstream exposes shell functions over a socket so `omarchy-cmd`
// can poke the running shell. The Android build keeps the same target/function
// naming and serves it on a local socket that omarchy-mobile talks to.
class QsIpcHandler : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(IpcHandler)
    Q_PROPERTY(QString target READ target WRITE setTarget NOTIFY targetChanged)
    Q_PROPERTY(bool enabled MEMBER m_enabled)
public:
    explicit QsIpcHandler(QObject *parent = nullptr);
    ~QsIpcHandler() override;

    QString target() const { return m_target; }
    void setTarget(const QString &target);

    // Called by the host when a request for this target arrives.
    Q_INVOKABLE QString dispatch(const QString &function, const QVariantList &args);

signals:
    void targetChanged();

private:
    QString m_target;
    bool m_enabled = true;
};
