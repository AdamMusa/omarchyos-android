#include "AndroidBridge.h"
#include "QsIo.h"
#include <QTimer>
#include <QEvent>
#include <QGuiApplication>

#include <QJsonDocument>
#include <QJsonArray>
#include <QJsonObject>
#include <QProcess>
#include <QSettings>
#include <QMetaProperty>
#include <QDebug>

#ifdef Q_OS_ANDROID
#include <QJniObject>
#include <QJniEnvironment>
#include <QCoreApplication>
#endif

namespace {

#ifdef Q_OS_ANDROID
constexpr auto kBridgeClass = "os/omarchy/shell/ShellBridge";

QJniObject bridge()
{
    return QJniObject::callStaticObjectMethod(
        kBridgeClass, "get", "()Los/omarchy/shell/ShellBridge;");
}

QString callString(const char *method)
{
    const auto obj = bridge();
    if (!obj.isValid()) return {};
    return obj.callObjectMethod(method, "()Ljava/lang/String;").toString();
}

// Java hands structured data back as JSON so there is one marshalling path
// rather than a JNI signature per field.
QVariantList callJsonList(const char *method)
{
    const QString json = callString(method);
    if (json.isEmpty()) return {};
    const auto doc = QJsonDocument::fromJson(json.toUtf8());
    return doc.isArray() ? doc.array().toVariantList() : QVariantList{};
}
#endif

AndroidBridge *g_instance = nullptr;

} // namespace

AndroidBridge::AndroidBridge(QObject *parent) : QObject(parent)
{
    g_instance = this;
    m_idleTimer.start();
    // Watching the application catches every input event delivered to the
    // shell's window, which is what the seat would have reported upstream.
    if (qApp) qApp->installEventFilter(this);
    // idleSeconds is polled by QML bindings; a one-second tick is enough
    // resolution for timeouts measured in minutes and costs nothing.
    auto *tick = new QTimer(this);
    tick->setInterval(1000);
    connect(tick, &QTimer::timeout, this, [this] { emit idleChanged(); });
    tick->start();
#ifdef Q_OS_ANDROID
    // Java pushes change notifications back through notifyStateChanged().
    QJniObject::callStaticMethod<void>(kBridgeClass, "attach", "()V");
#endif
}

bool AndroidBridge::nativeSystemBar() const
{
#ifdef Q_OS_ANDROID
    return QJniObject::callStaticMethod<jboolean>(
        "os/omarchy/shell/OmarchyActivity", "hasNativeSystemBar", "()Z");
#else
    return false;
#endif
}

QVariantMap AndroidBridge::systemInsets() const
{
#ifdef Q_OS_ANDROID
    const auto json = QJniObject::callStaticObjectMethod(
        "os/omarchy/shell/OmarchyActivity", "systemInsetsJson", "()Ljava/lang/String;").toString();
    return QJsonDocument::fromJson(json.toUtf8()).object().toVariantMap();
#else
    return {};
#endif
}

QString AndroidBridge::takeSystemBarAction() const
{
#ifdef Q_OS_ANDROID
    return QJniObject::callStaticObjectMethod(
        "os/omarchy/shell/OmarchyActivity", "takeSystemBarAction", "()Ljava/lang/String;").toString();
#else
    return {};
#endif
}

bool AndroidBridge::eventFilter(QObject *watched, QEvent *event)
{
    switch (event->type()) {
    case QEvent::MouseButtonPress:
    case QEvent::MouseMove:
    case QEvent::TouchBegin:
    case QEvent::TouchUpdate:
    case QEvent::KeyPress:
    case QEvent::Wheel:
    case QEvent::TabletPress:
        // First input of each idle stretch is logged: on a phone this is the
        // only evidence that touches are reaching the shell's window at all.
        if (m_idleTimer.elapsed() > 2000)
            qInfo("omarchy-shell: input %d after %lld ms idle",
                  int(event->type()), qlonglong(m_idleTimer.elapsed()));
        resetIdle();
        break;
    default:
        break;
    }
    return QObject::eventFilter(watched, event);
}

int AndroidBridge::idleSeconds() const
{
    return int(m_idleTimer.elapsed() / 1000);
}

void AndroidBridge::resetIdle()
{
    const bool wasIdle = m_idleTimer.elapsed() > 1000;
    m_idleTimer.restart();
    if (wasIdle) emit idleChanged();
}

void AndroidBridge::setIdleInhibited(bool inhibited)
{
    if (m_idleInhibited == inhibited) return;
    m_idleInhibited = inhibited;
    emit idleChanged();
}

void AndroidBridge::notifyStateChanged(const QString &what)
{
    if (what == QLatin1String("system-bar-action")) emit systemBarActionRequested();
    else if (what == QLatin1String("system-insets")) emit systemInsetsChanged();
    else if (what == QLatin1String("themes")) emit themeStateChanged();
    else if (what == QLatin1String("battery")) emit batteryChanged();
    else if (what == QLatin1String("audio")) emit audioChanged();
    else if (what == QLatin1String("network")) emit networkChanged();
    else if (what == QLatin1String("bluetooth")) emit bluetoothChanged();
    else if (what == QLatin1String("notifications")) emit notificationsChanged();
    else if (what == QLatin1String("media")) emit mediaChanged();
    else if (what == QLatin1String("tasks")) emit tasksChanged();
    else if (what == QLatin1String("packages")) emit installedAppsChanged();
    // Java reports screen on/off and user-activity from its own receivers;
    // a screen-on is user activity by definition.
    else if (what == QLatin1String("screen-on")) { resetIdle(); emit idleChanged(); }
    else if (what == QLatin1String("screen-off")) emit idleChanged();
    else if (what == QLatin1String("user-activity")) resetIdle();
}

#ifdef Q_OS_ANDROID
#define J_INT(method, fallback) (bridge().isValid() ? bridge().callMethod<jint>(method) : (fallback))
#define J_BOOL(method, fallback) (bridge().isValid() ? bridge().callMethod<jboolean>(method) : (fallback))
#define J_FLOAT(method, fallback) (bridge().isValid() ? bridge().callMethod<jfloat>(method) : (fallback))
#else
#define J_INT(method, fallback) (fallback)
#define J_BOOL(method, fallback) (fallback)
#define J_FLOAT(method, fallback) (fallback)
#endif

bool AndroidBridge::screenOn() const
{
#ifdef Q_OS_ANDROID
    return J_BOOL("isScreenOn", true);
#else
    return true;
#endif
}

bool AndroidBridge::idleInhibited() const
{
#ifdef Q_OS_ANDROID
    // An app holding a screen wake lock is Android's idle inhibitor.
    return m_idleInhibited || J_BOOL("isWakeLockHeld", false);
#else
    return m_idleInhibited;
#endif
}


int AndroidBridge::batteryPercent() const { return J_INT("batteryPercent", 100); }
bool AndroidBridge::charging() const { return J_BOOL("isCharging", false); }
qreal AndroidBridge::batteryTimeToEmpty() const { return J_INT("batteryTimeToEmpty", 0); }
qreal AndroidBridge::batteryTimeToFull() const { return J_INT("batteryTimeToFull", 0); }
qreal AndroidBridge::volume() const { return J_FLOAT("volume", 0.5f); }
bool AndroidBridge::muted() const { return J_BOOL("isMuted", false); }
qreal AndroidBridge::micVolume() const { return J_FLOAT("micVolume", 1.0f); }
bool AndroidBridge::micMuted() const { return J_BOOL("isMicMuted", false); }
int AndroidBridge::wifiSignal() const { return J_INT("wifiSignal", 4); }
bool AndroidBridge::bluetoothEnabled() const { return J_BOOL("bluetoothEnabled", false); }

void AndroidBridge::setVolume(qreal v)
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("setVolume", "(F)V", static_cast<jfloat>(v));
#else
    Q_UNUSED(v)
#endif
    emit audioChanged();
}

void AndroidBridge::setMuted(bool m)
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("setMuted", "(Z)V", static_cast<jboolean>(m));
#else
    Q_UNUSED(m)
#endif
    emit audioChanged();
}

QString AndroidBridge::audioOutputName() const
{
#ifdef Q_OS_ANDROID
    return callString("audioOutputName");
#else
    return QStringLiteral("Speaker");
#endif
}

QString AndroidBridge::networkType() const
{
#ifdef Q_OS_ANDROID
    return callString("networkType");
#else
    return QStringLiteral("wifi");
#endif
}

QString AndroidBridge::wifiSsid() const
{
#ifdef Q_OS_ANDROID
    return callString("wifiSsid");
#else
    return QStringLiteral("OmarchyOS");
#endif
}

#ifdef Q_OS_ANDROID
#define J_LIST(method) callJsonList(method)
#else
#define J_LIST(method) QVariantList{}
#endif

QVariantList AndroidBridge::wifiNetworks() const { return J_LIST("wifiNetworksJson"); }
QVariantList AndroidBridge::bluetoothDevices() const { return J_LIST("bluetoothDevicesJson"); }
QVariantList AndroidBridge::notifications() const { return J_LIST("notificationsJson"); }
QVariantList AndroidBridge::mediaSessions() const { return J_LIST("mediaSessionsJson"); }
QVariantList AndroidBridge::foregroundServices() const { return J_LIST("foregroundServicesJson"); }
QVariantList AndroidBridge::installedApps() const { return J_LIST("installedAppsJson"); }
QVariantList AndroidBridge::recentTasks() const { return J_LIST("recentTasksJson"); }

void AndroidBridge::launchApp(const QString &id) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("launchApp", "(Ljava/lang/String;)V",
                              QJniObject::fromString(id).object<jstring>());
#else
    qInfo() << "launchApp" << id;
#endif
}

void AndroidBridge::moveToTask(int taskId) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("moveToTask", "(I)V", static_cast<jint>(taskId));
#else
    Q_UNUSED(taskId)
#endif
}

void AndroidBridge::closeForegroundTask() const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("closeForegroundTask");
#endif
}

QUrl AndroidBridge::iconUrl(const QString &name) const
{
#ifdef Q_OS_ANDROID
    const QString url = bridge().callObjectMethod("iconUrl", "(Ljava/lang/String;)Ljava/lang/String;",
                          QJniObject::fromString(name).object<jstring>()).toString();
    return QUrl(url);
#else
    return QUrl(QStringLiteral("image://omarchy/") + name);
#endif
}

void AndroidBridge::setShellInset(const QString &surface, const QString &edge, int size,
                                  int layer, int keyboardFocus) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("setShellInset",
        "(Ljava/lang/String;Ljava/lang/String;III)V",
        QJniObject::fromString(surface).object<jstring>(),
        QJniObject::fromString(edge).object<jstring>(),
        static_cast<jint>(size), static_cast<jint>(layer), static_cast<jint>(keyboardFocus));
#else
    Q_UNUSED(surface) Q_UNUSED(edge) Q_UNUSED(size) Q_UNUSED(layer) Q_UNUSED(keyboardFocus)
#endif
}

void AndroidBridge::setOutsideTouchGrab(bool active) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("setOutsideTouchGrab", "(Z)V", static_cast<jboolean>(active));
#else
    Q_UNUSED(active)
#endif
}

void AndroidBridge::setLockVisible(bool visible) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("setLockVisible", "(Z)V", static_cast<jboolean>(visible));
#else
    Q_UNUSED(visible)
#endif
}

void AndroidBridge::openSettings(const QString &page) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("openSettings", "(Ljava/lang/String;)V",
                             QJniObject::fromString(page).object<jstring>());
#else
    Q_UNUSED(page)
#endif
}

void AndroidBridge::openAppInfo(const QString &id) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("openAppInfo", "(Ljava/lang/String;)V",
                             QJniObject::fromString(id).object<jstring>());
#else
    Q_UNUSED(id)
#endif
}

void AndroidBridge::execDetached(const QVariant &command) const
{
    // Omarchy's UI launches its own panels through the omarchy-shell CLI; on
    // Android the shell is this process, so the call never leaves it.
    const QStringList argv = command.canConvert<QStringList>()
        ? command.toStringList()
        : QStringList{ command.toString() };
    if (QsIpcHandler::handleCommandLine(argv)) return;

    QStringList parts;
    if (command.typeId() == QMetaType::QVariantList) {
        const auto list = command.toList();
        for (const auto &v : list) parts << v.toString();
    } else {
        parts = command.toString().split(QLatin1Char(' '), Qt::SkipEmptyParts);
    }
    if (parts.isEmpty()) return;

    // Omarchy's own commands live in /system_ext/bin on device; anything else
    // runs as a plain process.
    QProcess::startDetached(parts.first(), parts.mid(1));
}

void AndroidBridge::hyprlandCommand(const QString &request) const
{
    // Window-manager requests with no Android equivalent are logged rather
    // than silently dropped, so gaps show up while porting.
    qInfo("omarchy-shell: unmapped window command: %s", qPrintable(request));
}

void AndroidBridge::saveProperties(const QString &key, QObject *object) const
{
    if (!object || key.isEmpty()) return;
    QSettings settings(QStringLiteral("OmarchyOS"), QStringLiteral("omarchy-shell"));
    settings.beginGroup(QStringLiteral("persist/") + key);
    const auto *mo = object->metaObject();
    for (int i = mo->propertyOffset(); i < mo->propertyCount(); ++i) {
        const auto prop = mo->property(i);
        if (!prop.isReadable() || !prop.isWritable()) continue;
        settings.setValue(QString::fromLatin1(prop.name()), prop.read(object));
    }
}

void AndroidBridge::restoreProperties(const QString &key, QObject *object) const
{
    if (!object || key.isEmpty()) return;
    QSettings settings(QStringLiteral("OmarchyOS"), QStringLiteral("omarchy-shell"));
    settings.beginGroup(QStringLiteral("persist/") + key);
    const auto *mo = object->metaObject();
    for (int i = mo->propertyOffset(); i < mo->propertyCount(); ++i) {
        const auto prop = mo->property(i);
        if (!prop.isWritable()) continue;
        const QString name = QString::fromLatin1(prop.name());
        if (settings.contains(name)) prop.write(object, settings.value(name));
    }
}

void AndroidBridge::dismissNotification(const QString &key) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("dismissNotification", "(Ljava/lang/String;)V",
                              QJniObject::fromString(key).object<jstring>());
#else
    Q_UNUSED(key)
#endif
}

void AndroidBridge::beginAuth(QObject *) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("beginAuth");
#endif
}

void AndroidBridge::cancelAuth() const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("cancelAuth");
#endif
}

void AndroidBridge::submitPassword(const QString &password) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("submitPassword", "(Ljava/lang/String;)V",
                              QJniObject::fromString(password).object<jstring>());
#else
    Q_UNUSED(password)
#endif
}

void AndroidBridge::connectWifi(const QString &ssid, const QString &password) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("connectWifi", "(Ljava/lang/String;Ljava/lang/String;)V",
                              QJniObject::fromString(ssid).object<jstring>(),
                              QJniObject::fromString(password).object<jstring>());
#else
    Q_UNUSED(ssid) Q_UNUSED(password)
#endif
}

void AndroidBridge::setWifiEnabled(bool enabled) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("setWifiEnabled", "(Z)V", static_cast<jboolean>(enabled));
#else
    Q_UNUSED(enabled)
#endif
}

void AndroidBridge::setBluetoothEnabled(bool enabled) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("setBluetoothEnabled", "(Z)V", static_cast<jboolean>(enabled));
#else
    Q_UNUSED(enabled)
#endif
}

#ifdef Q_OS_ANDROID
extern "C" JNIEXPORT void JNICALL
Java_os_omarchy_shell_ShellBridge_nativeStateChanged(JNIEnv *env, jobject, jstring what)
{
    if (!g_instance) return;
    const char *chars = env->GetStringUTFChars(what, nullptr);
    const QString value = QString::fromUtf8(chars);
    env->ReleaseStringUTFChars(what, chars);
    QMetaObject::invokeMethod(g_instance, "notifyStateChanged", Qt::QueuedConnection,
                              Q_ARG(QString, value));
}
#endif

QVariantMap AndroidBridge::themeState() const {
#ifdef Q_OS_ANDROID
    return QJsonDocument::fromJson(callString("themeStateJson").toUtf8()).object().toVariantMap();
#else
    return {};
#endif
}
void AndroidBridge::refreshThemeMarketplace() const {
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("refreshThemeMarketplace", "()V");
#endif
}
void AndroidBridge::installTheme(const QString &id) const {
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("installTheme", "(Ljava/lang/String;)V", QJniObject::fromString(id).object<jstring>());
#endif
}
void AndroidBridge::applyTheme(const QString &id) const {
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("applyTheme", "(Ljava/lang/String;)V", QJniObject::fromString(id).object<jstring>());
#endif
}
