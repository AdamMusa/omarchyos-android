#include "QsIo.h"

#include <QFile>
#include <QFileInfo>
#include <QTextStream>
#include <QDir>
#include <QMetaObject>

// ---------------------------------------------------------------- Process

QsProcess::QsProcess(QObject *parent) : QObject(parent)
{
    connect(&m_process, &QProcess::started, this, &QsProcess::started);
    connect(&m_process, &QProcess::finished, this, [this](int code, QProcess::ExitStatus status) {
        // Flush whatever the parsers are holding before reporting the exit,
        // so QML handlers see complete output in onExited.
        if (m_stdoutParser) QMetaObject::invokeMethod(m_stdoutParser, "finish");
        if (m_stderrParser) QMetaObject::invokeMethod(m_stderrParser, "finish");
        emit exited(code, static_cast<int>(status));
        emit runningChanged();
    });
    connect(&m_process, &QProcess::readyReadStandardOutput, this, [this] {
        if (!m_stdoutParser) return;
        const QString chunk = QString::fromUtf8(m_process.readAllStandardOutput());
        // StdioCollector/SplitParser both expose read(string); upstream QML
        // attaches one of them and reads `text` or per-line signals.
        QMetaObject::invokeMethod(m_stdoutParser, "feed", Q_ARG(QString, chunk));
    });
    connect(&m_process, &QProcess::readyReadStandardError, this, [this] {
        if (!m_stderrParser) return;
        const QString chunk = QString::fromUtf8(m_process.readAllStandardError());
        QMetaObject::invokeMethod(m_stderrParser, "feed", Q_ARG(QString, chunk));
    });
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

void QsProcess::start()
{
    if (m_command.isEmpty()) return;
    if (!m_workingDirectory.isEmpty()) m_process.setWorkingDirectory(m_workingDirectory);
    if (!m_environment.isEmpty()) m_process.setEnvironment(m_environment);
    const QStringList args = m_command.mid(1);
    m_process.start(m_command.first(), args);
}

void QsProcess::startDetached()
{
    if (m_command.isEmpty()) return;
    QProcess::startDetached(m_command.first(), m_command.mid(1), m_workingDirectory);
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

QsIpcHandler::QsIpcHandler(QObject *parent) : QObject(parent) {}
QsIpcHandler::~QsIpcHandler() = default;

void QsIpcHandler::setTarget(const QString &target)
{
    if (target == m_target) return;
    m_target = target;
    emit targetChanged();
}

QString QsIpcHandler::dispatch(const QString &function, const QVariantList &args)
{
    if (!m_enabled) return QStringLiteral("disabled");
    QVariant result;
    // Upstream declares IPC entry points as plain QML functions on the handler.
    const bool ok = QMetaObject::invokeMethod(this, function.toUtf8().constData(),
                                              Qt::DirectConnection, Q_RETURN_ARG(QVariant, result),
                                              Q_ARG(QVariant, args.value(0)),
                                              Q_ARG(QVariant, args.value(1)));
    return ok ? result.toString() : QStringLiteral("unknown function: %1").arg(function);
}
