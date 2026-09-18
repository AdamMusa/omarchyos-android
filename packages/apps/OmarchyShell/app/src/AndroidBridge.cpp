#include "AndroidBridge.h"

#ifdef Q_OS_ANDROID
#include <QJniObject>
#include <QCoreApplication>
#include <QJniEnvironment>
#endif

namespace {
#ifdef Q_OS_ANDROID
// Java side lives in app/android/src/os/omarchy/shell/ShellBridge.java and is
// the only place that touches Android system services.
QJniObject bridge()
{
    return QJniObject::callStaticObjectMethod(
        "os/omarchy/shell/ShellBridge", "get", "()Los/omarchy/shell/ShellBridge;");
}
#endif
}

AndroidBridge::AndroidBridge(QObject *parent) : QObject(parent) {}

int AndroidBridge::batteryPercent() const
{
#ifdef Q_OS_ANDROID
    return bridge().callMethod<jint>("batteryPercent");
#else
    return 100;
#endif
}

bool AndroidBridge::charging() const
{
#ifdef Q_OS_ANDROID
    return bridge().callMethod<jboolean>("isCharging");
#else
    return false;
#endif
}

qreal AndroidBridge::volume() const
{
#ifdef Q_OS_ANDROID
    return bridge().callMethod<jfloat>("volume");
#else
    return 0.5;
#endif
}

void AndroidBridge::setVolume(qreal v)
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("setVolume", "(F)V", static_cast<jfloat>(v));
#else
    Q_UNUSED(v)
#endif
    emit audioChanged();
}

bool AndroidBridge::muted() const
{
#ifdef Q_OS_ANDROID
    return bridge().callMethod<jboolean>("isMuted");
#else
    return false;
#endif
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

QString AndroidBridge::networkType() const
{
#ifdef Q_OS_ANDROID
    return bridge().callObjectMethod<jstring>("networkType").toString();
#else
    return QStringLiteral("wifi");
#endif
}

QString AndroidBridge::wifiSsid() const
{
#ifdef Q_OS_ANDROID
    return bridge().callObjectMethod<jstring>("wifiSsid").toString();
#else
    return {};
#endif
}

int AndroidBridge::wifiSignal() const
{
#ifdef Q_OS_ANDROID
    return bridge().callMethod<jint>("wifiSignal");
#else
    return 4;
#endif
}

bool AndroidBridge::bluetoothEnabled() const
{
#ifdef Q_OS_ANDROID
    return bridge().callMethod<jboolean>("bluetoothEnabled");
#else
    return false;
#endif
}

QVariantList AndroidBridge::installedApps() const
{
#ifdef Q_OS_ANDROID
    return bridge().callObjectMethod("installedAppsJson", "()Ljava/lang/String;")
        .toString().isEmpty() ? QVariantList{} : QVariantList{};
#else
    return {};
#endif
}

void AndroidBridge::launchApp(const QString &packageName) const
{
#ifdef Q_OS_ANDROID
    bridge().callMethod<void>("launchApp", "(Ljava/lang/String;)V",
                              QJniObject::fromString(packageName).object<jstring>());
#else
    Q_UNUSED(packageName)
#endif
}

QVariantList AndroidBridge::activeNotifications() const { return {}; }
QVariantMap AndroidBridge::mediaSession() const { return {}; }
QVariantList AndroidBridge::recentTasks() const { return {}; }
