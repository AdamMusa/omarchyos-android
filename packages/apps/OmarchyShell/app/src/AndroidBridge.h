#pragma once
#include <QObject>
#include <QVariantList>
#include <QVariantMap>

// Thin JNI front door for the Android system services that stand in for the
// Linux daemons Omarchy's shell talks to. Each Quickshell.Services.* type in
// quickshell-compat reads its state from here, so the QML above stays upstream.
class AndroidBridge : public QObject
{
    Q_OBJECT
    Q_PROPERTY(int batteryPercent READ batteryPercent NOTIFY batteryChanged)
    Q_PROPERTY(bool charging READ charging NOTIFY batteryChanged)
    Q_PROPERTY(qreal volume READ volume WRITE setVolume NOTIFY audioChanged)
    Q_PROPERTY(bool muted READ muted WRITE setMuted NOTIFY audioChanged)
    Q_PROPERTY(QString networkType READ networkType NOTIFY networkChanged)
    Q_PROPERTY(QString wifiSsid READ wifiSsid NOTIFY networkChanged)
    Q_PROPERTY(int wifiSignal READ wifiSignal NOTIFY networkChanged)
    Q_PROPERTY(bool bluetoothEnabled READ bluetoothEnabled NOTIFY bluetoothChanged)
public:
    explicit AndroidBridge(QObject *parent = nullptr);

    int batteryPercent() const;
    bool charging() const;
    qreal volume() const;
    void setVolume(qreal v);
    bool muted() const;
    void setMuted(bool m);
    QString networkType() const;
    QString wifiSsid() const;
    int wifiSignal() const;
    bool bluetoothEnabled() const;

    // Installed apps for Omarchy's AppLibrary: name, icon, exec-equivalent.
    Q_INVOKABLE QVariantList installedApps() const;
    Q_INVOKABLE void launchApp(const QString &packageName) const;
    Q_INVOKABLE QVariantList activeNotifications() const;
    Q_INVOKABLE QVariantMap mediaSession() const;
    Q_INVOKABLE QVariantList recentTasks() const;

signals:
    void batteryChanged();
    void audioChanged();
    void networkChanged();
    void bluetoothChanged();
    void notificationsChanged();
    void mediaChanged();
};
