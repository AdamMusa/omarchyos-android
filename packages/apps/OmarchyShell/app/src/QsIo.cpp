#include "QsIo.h"
#include <QHash>
#include <QMetaMethod>

#include <QFile>
#include <QFileInfo>
#include <QTextStream>
#include <QDir>
#include <QMetaObject>
#ifdef Q_OS_ANDROID
#include <QJniObject>
#endif
#include <QCoreApplication>

// ---------------------------------------------------------------- Process

QsProcess::QsProcess(QObject *parent) : QObject(parent)
{
    connect(&m_process, &QProcess::started, this, &QsProcess::started);
    connect(&m_process, &QProcess::errorOccurred, this, [this](QProcess::ProcessError error) {
        // A command that cannot start is the difference between a working
        // shell and an empty bar, so it is logged rather than swallowed.
        qWarning("omarchy-shell: command failed (%d): %s", int(error),
                 qPrintable(m_command.join(QLatin1Char(' ')).left(160)));
        emit runningChanged();
    });
    connect(&m_process, &QProcess::finished, this, [this](int code, QProcess::ExitStatus status) {
        // Flush whatever the parsers are holding before reporting the exit,
        // so QML handlers see complete output in onExited.
        if (m_stdoutParser) QMetaObject::invokeMethod(m_stdoutParser, "finish");
        if (m_stderrParser) QMetaObject::invokeMethod(m_stderrParser, "finish");
        emit exited(code, static_cast<int>(status));
        emit runningChanged();
    });
    connect(&m_process, &QProcess::readyReadStandardOutput, this, [this] {
        // Read a bounded chunk rather than readAllStandardOutput(): a child
        // that never stops printing would otherwise have the whole of its
        // output allocated in one go, which is how a runaway scan took the
        // shell down with std::bad_alloc. Omarchy's scans are written for a
        // desktop filesystem; the same `find` on Android walks far more.
        m_process.setReadChannel(QProcess::StandardOutput);
        const QByteArray bytes = m_process.read(kReadChunkBytes);
        if (!m_stdoutParser) return;
        m_stdoutBytes += bytes.size();
        if (m_stdoutBytes > kMaxCollectedBytes) {
            qWarning("omarchy-shell: %s printed over %lld MiB; stopping it",
                     qPrintable(m_command.join(QLatin1Char(' ')).left(80)),
                     qlonglong(kMaxCollectedBytes / (1024 * 1024)));
            m_process.kill();
            return;
        }
        const QString chunk = QString::fromUtf8(bytes);
        // StdioCollector/SplitParser both expose read(string); upstream QML
        // attaches one of them and reads `text` or per-line signals.
        QMetaObject::invokeMethod(m_stdoutParser, "feed", Q_ARG(QString, chunk));
    });
    connect(&m_process, &QProcess::readyReadStandardError, this, [this] {
        m_process.setReadChannel(QProcess::StandardError);
        const QByteArray bytes = m_process.read(kReadChunkBytes);
        if (!m_stderrParser) return;
        m_stderrBytes += bytes.size();
        if (m_stderrBytes > kMaxCollectedBytes) {
            qWarning("omarchy-shell: %s wrote over %lld MiB to stderr; stopping it",
                     qPrintable(m_command.join(QLatin1Char(' ')).left(80)),
                     qlonglong(kMaxCollectedBytes / (1024 * 1024)));
            m_process.kill();
            return;
        }
        QMetaObject::invokeMethod(m_stderrParser, "feed", Q_ARG(QString, QString::fromUtf8(bytes)));
    });
}

QsProcess::~QsProcess()
{
    if (m_process.state() == QProcess::NotRunning) return;
    // QProcess's own destructor drains whatever the child still has buffered
    // before it gives up, and a command that is producing output faster than
    // the shell consumes it makes that allocation unbounded — which is how a
    // reload during a long scan ended in std::bad_alloc. Detach from the
    // output first, then stop the child.
    m_process.closeReadChannel(QProcess::StandardOutput);
    m_process.closeReadChannel(QProcess::StandardError);
    m_process.kill();
    m_process.waitForFinished(200);
}

bool QsProcess::running() const
{
    return m_process.state() != QProcess::NotRunning;
}

void QsProcess::setRunning(bool running)
{
    if (running == this->running()) return;
    if (running) start();
    else m_process.terminate();
    emit runningChanged();
}

void QsProcess::setCommand(const QStringList &command)
{
    if (command == m_command) return;
    m_command = command;
    emit commandChanged();
}

void QsProcess::setWorkingDirectory(const QString &dir)
{
    if (dir == m_workingDirectory) return;
    m_workingDirectory = dir;
    emit workingDirectoryChanged();
}

void QsProcess::setEnvironment(const QStringList &env)
{
    if (env == m_environment) return;
    m_environment = env;
    emit environmentChanged();
}

QStringList QsProcess::resolveCommand(const QStringList &command)
{
    if (command.isEmpty()) return command;

#ifdef Q_OS_ANDROID
    QStringList resolved = command;
    const QString program = QFileInfo(resolved.first()).fileName();

    // Omarchy's scripts are real bash — the plugin scan alone uses process
    // substitution and [[ ]], which Android's mksh cannot parse. OmarchyOS
    // ships bash: in the image at /system_ext/bin/bash, and inside the app
    // package as a native library, which is the only app-owned location
    // Android allows execution from.
    if (program == QLatin1String("bash")) {
        static const QString packaged = [] {
            const QJniObject bridge = QJniObject::callStaticObjectMethod(
                "os/omarchy/shell/ShellBridge", "get", "()Los/omarchy/shell/ShellBridge;");
            const QString dir = bridge.isValid()
                ? bridge.callObjectMethod("nativeLibraryDir", "()Ljava/lang/String;").toString()
                : QString();
            return dir.isEmpty() ? QString() : dir + QStringLiteral("/libbash.so");
        }();
        if (QFileInfo::exists(QStringLiteral("/system_ext/bin/bash")))
            resolved[0] = QStringLiteral("/system_ext/bin/bash");
        else if (QFileInfo::exists(packaged))
            resolved[0] = packaged;
        else
            resolved[0] = QStringLiteral("/system/bin/sh");
        return resolved;
    }

    if (program == QLatin1String("sh")) {
        resolved[0] = QStringLiteral("/system/bin/sh");
        return resolved;
    }

    // Omarchy's own commands ship in the system image; the coreutils it also
    // calls (readlink, cat, ls) are Android's toybox. QProcess does not search
    // a PATH, so bare names are resolved here, in that order.
    if (!resolved.first().contains(QLatin1Char('/'))) {
        for (const auto *dir : { "/system_ext/bin/", "/system/bin/", "/vendor/bin/" }) {
            const QString candidate = QLatin1String(dir) + resolved.first();
            if (QFileInfo::exists(candidate)) {
                resolved[0] = candidate;
                break;
            }
        }
    }
    return resolved;
#else
    return command;
#endif
}

void QsProcess::start()
{
    if (m_command.isEmpty()) return;

    // An omarchy-shell call is this process talking to itself; answer it here
    // rather than spawning a CLI that would only reconnect over a socket.
    QString reply;
    if (QsIpcHandler::handleCommandLine(m_command, &reply)) {
        if (m_stdoutParser) {
            QMetaObject::invokeMethod(m_stdoutParser, "feed", Q_ARG(QString, reply));
            QMetaObject::invokeMethod(m_stdoutParser, "finish");
        }
        emit exited(0, int(QProcess::NormalExit));
        emit runningChanged();
        return;
    }
    if (!m_workingDirectory.isEmpty()) m_process.setWorkingDirectory(m_workingDirectory);

    QStringList env = m_environment;
#ifdef Q_OS_ANDROID
    if (env.isEmpty()) {
        env = QProcess::systemEnvironment();
        env << QStringLiteral("PATH=/system_ext/bin:/system/bin:/vendor/bin");
    }
#endif
    if (!env.isEmpty()) m_process.setEnvironment(env);

    m_stdoutBytes = 0;
    m_stderrBytes = 0;
    const QStringList resolved = resolveCommand(m_command);
    qInfo("omarchy-shell: exec %s", qPrintable(resolved.join(QLatin1Char(' ')).left(120)));
    m_process.start(resolved.first(), resolved.mid(1));
}

void QsProcess::startDetached()
{
    if (QsIpcHandler::handleCommandLine(m_command)) return;
    if (m_command.isEmpty()) return;
    const QStringList resolved = resolveCommand(m_command);
    QProcess::startDetached(resolved.first(), resolved.mid(1), m_workingDirectory);
}

void QsProcess::signal(int sig)
{
    if (sig == 15) m_process.terminate();
    else if (sig == 9) m_process.kill();
}

void QsProcess::write(const QString &data)
{
    m_process.write(data.toUtf8());
}

// ---------------------------------------------------------------- Parsers

void QsSplitParser::feed(const QString &chunk)
{
    m_buffer += chunk;
    int idx;
    while ((idx = m_buffer.indexOf(m_splitMarker)) >= 0) {
        emit read(m_buffer.left(idx));
        m_buffer.remove(0, idx + m_splitMarker.size());
    }
}

void QsSplitParser::finish()
{
    if (!m_buffer.isEmpty()) {
        emit read(m_buffer);
        m_buffer.clear();
    }
    emit streamFinished();
}

// --------------------------------------------------------------- FileView

QsFileView::QsFileView(QObject *parent) : QObject(parent)
{
    connect(&m_watcher, &QFileSystemWatcher::fileChanged, this, [this] {
        emit fileChanged();
        load();
        // Some writers replace the file, which drops the watch.
        if (m_watchChanges && !m_watcher.files().contains(m_path))
            m_watcher.addPath(m_path);
    });
}

void QsFileView::setPath(const QString &path)
{
    if (path == m_path) return;
    if (!m_path.isEmpty()) m_watcher.removePath(m_path);
    m_path = path;
    emit pathChanged();
    load();
    if (m_watchChanges && QFileInfo::exists(m_path)) m_watcher.addPath(m_path);
}

void QsFileView::setWatchChanges(bool watch)
{
    if (watch == m_watchChanges) return;
    m_watchChanges = watch;
    if (watch && QFileInfo::exists(m_path)) m_watcher.addPath(m_path);
    else if (!watch && !m_path.isEmpty()) m_watcher.removePath(m_path);
    emit watchChangesChanged();
}

void QsFileView::load()
{
    QFile file(m_path);
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        if (m_printErrors) qWarning("FileView: cannot read %s", qPrintable(m_path));
        emit loadFailed(file.errorString());
        return;
    }
    m_text = QString::fromUtf8(file.readAll());
    emit textChanged();
    emit loaded();
}

void QsFileView::reload() { load(); }

void QsFileView::setText(const QString &text)
{
    QDir().mkpath(QFileInfo(m_path).absolutePath());

    if (m_atomicWrites) {
        const QString tmpPath = m_path + QStringLiteral(".tmp");
        QFile tmp(tmpPath);
        if (!tmp.open(QIODevice::WriteOnly | QIODevice::Truncate)) return;
        tmp.write(text.toUtf8());
        tmp.flush();
        tmp.close();
        QFile::remove(m_path);
        if (!QFile::rename(tmpPath, m_path)) {
            QFile::remove(tmpPath);
            return;
        }
    } else {
        QFile file(m_path);
        if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate)) return;
        file.write(text.toUtf8());
    }

    m_text = text;
    emit textChanged();
}

void QsFileView::writeAdapter() { setText(m_text); }

// ------------------------------------------------------------- IpcHandler

namespace {
// Live IPC handlers by target name. Upstream keys its socket protocol the same
// way, so a target is unique and the last handler to claim one wins.
QHash<QString, QsIpcHandler *> &ipcHandlers()
{
    static QHash<QString, QsIpcHandler *> handlers;
    return handlers;
}

// Splits a shell command string the way the CLI would be invoked, honouring
// single and double quotes so a JSON payload survives as one argument.
QStringList tokenize(const QString &line)
{
    QStringList out;
    QString current;
    QChar quote;
    bool any = false;
    for (const QChar c : line) {
        if (!quote.isNull()) {
            if (c == quote) quote = QChar();
            else current += c;
        } else if (c == QLatin1Char('\'') || c == QLatin1Char('"')) {
            quote = c;
            any = true;
        } else if (c.isSpace()) {
            if (any || !current.isEmpty()) out << current;
            current.clear();
            any = false;
        } else {
            current += c;
        }
    }
    if (any || !current.isEmpty()) out << current;
    return out;
}
} // namespace

QsIpcHandler::QsIpcHandler(QObject *parent) : QObject(parent) {}

QsIpcHandler::~QsIpcHandler()
{
    if (!m_target.isEmpty() && ipcHandlers().value(m_target) == this)
        ipcHandlers().remove(m_target);
}

void QsIpcHandler::setTarget(const QString &target)
{
    if (target == m_target) return;
    if (!m_target.isEmpty() && ipcHandlers().value(m_target) == this)
        ipcHandlers().remove(m_target);
    m_target = target;
    if (!m_target.isEmpty()) ipcHandlers().insert(m_target, this);
    emit targetChanged();
}

QString QsIpcHandler::call(const QString &target, const QString &function,
                           const QVariantList &args)
{
    QsIpcHandler *handler = ipcHandlers().value(target);
    if (!handler) {
        qWarning("omarchy-shell: no ipc target %s", qPrintable(target));
        return QStringLiteral("unknown target: %1").arg(target);
    }
    return handler->dispatch(function, args);
}

bool QsIpcHandler::handleCommandLine(const QStringList &command, QString *reply)
{
    QStringList argv = command;
    // `bash -lc "omarchy-shell …"` is how the bar runs it; unwrap to the
    // command the CLI would have seen.
    if (argv.size() >= 3 && QFileInfo(argv.first()).fileName().startsWith(QLatin1String("bash"))
        && argv.at(1).startsWith(QLatin1Char('-')))
        argv = tokenize(argv.at(2));
    if (argv.size() < 3) return false;
    if (QFileInfo(argv.first()).fileName() != QLatin1String("omarchy-shell")) return false;

    const QString target = argv.at(1);
    const QString function = argv.at(2);
    QVariantList args;
    for (int i = 3; i < argv.size(); ++i) args << argv.at(i);
    const QString result = call(target, function, args);
    qInfo("omarchy-shell: ipc %s.%s -> %s", qPrintable(target), qPrintable(function),
          qPrintable(result.left(60)));
    if (reply) *reply = result;
    return true;
}

QString QsIpcHandler::dispatch(const QString &function, const QVariantList &args)
{
    if (!m_enabled) return QStringLiteral("disabled");
    // Upstream declares IPC entry points as plain QML functions, and QML
    // compiles a typed declaration (`function toggle(id: string)`) into a slot
    // taking QString rather than QVariant. invokeMethod only matches an exact
    // signature, so the method is found by name and its own parameter types
    // are what the arguments are converted to.
    const QByteArray name = function.toUtf8();
    const QMetaObject *meta = metaObject();
    for (int i = 0; i < meta->methodCount(); ++i) {
        const QMetaMethod method = meta->method(i);
        if (method.name() != name) continue;
        const int count = method.parameterCount();
        if (count > 4 || count > args.size()) continue;

        // The converted values must outlive the call: QGenericArgument holds a
        // pointer into each one.
        QVariant values[4];
        QGenericArgument passed[4];
        bool convertible = true;
        for (int p = 0; p < count; ++p) {
            values[p] = args.value(p);
            const QMetaType wanted = method.parameterMetaType(p);
            if (wanted.isValid() && values[p].metaType() != wanted
                && !values[p].convert(wanted)) {
                convertible = false;
                break;
            }
            passed[p] = QGenericArgument(values[p].typeName(), values[p].constData());
        }
        if (!convertible) continue;

        // Try with a return value first, then without: a QML function declared
        // `: void` has no return slot, and one declared `: string` will not
        // invoke unless a matching return argument is supplied.
        QVariant result;
        if (method.invoke(this, Qt::DirectConnection, Q_RETURN_ARG(QVariant, result),
                          passed[0], passed[1], passed[2], passed[3]))
            return result.isValid() ? result.toString() : QStringLiteral("ok");
        if (method.invoke(this, Qt::DirectConnection, passed[0], passed[1],
                          passed[2], passed[3]))
            return QStringLiteral("ok");
        qWarning("omarchy-shell: ipc %s.%s matched but would not invoke",
                 qPrintable(m_target), qPrintable(function));
    }
    // Nothing matched: say what the target does expose, so a rename upstream
    // or a signature this code cannot build is obvious from one log line.
    QStringList known;
    for (int i = 0; i < meta->methodCount(); ++i) {
        const QMetaMethod method = meta->method(i);
        if (method.methodType() == QMetaMethod::Signal) continue;
        QStringList params;
        for (int p = 0; p < method.parameterCount(); ++p)
            params << QString::fromLatin1(method.parameterMetaType(p).name());
        known << QStringLiteral("%1(%2)").arg(QString::fromUtf8(method.name()),
                                              params.join(QLatin1Char(',')));
    }
    qWarning("omarchy-shell: ipc %s has no %s; it exposes: %s", qPrintable(m_target),
             qPrintable(function), qPrintable(known.join(QLatin1String(" "))));
    return QStringLiteral("unknown function: %1").arg(function);
}
