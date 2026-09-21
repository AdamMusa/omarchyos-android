#pragma once
#include <QObject>
#include <QVariantList>
#include <QVariantMap>
#include <QUrl>
#include <QElapsedTimer>

// The one place the shell touches Android. Every Quickshell.Services.* type in
// quickshell-compat reads its state from here, so Omarchy's QML above stays
// upstream. The Java side (app/android/src/os/omarchy/shell/ShellBridge.java)
// does the actual talking to system services.
class AndroidBridge : public QObject
{
    Q_OBJECT
    Q_PROPERTY(bool nativeSystemBar READ nativeSystemBar CONSTANT)
    Q_PROPERTY(QVariantMap systemInsets READ systemInsets NOTIFY systemInsetsChanged)
    // Idle state. Omarchy's screensaver and lock are driven by the Wayland
    // idle-notify protocol upstream; on Android idleness is the absence of
    // input to the shell window plus the display's own interactive state.
    Q_PROPERTY(QVariantMap themeState READ themeState NOTIFY themeStateChanged)
    Q_PROPERTY(int idleSeconds READ idleSeconds NOTIFY idleChanged)
    Q_PROPERTY(bool screenOn READ screenOn NOTIFY idleChanged)
    Q_PROPERTY(bool idleInhibited READ idleInhibited NOTIFY idleChanged)

    Q_PROPERTY(int batteryPercent READ batteryPercent NOTIFY batteryChanged)
    Q_PROPERTY(bool charging READ charging NOTIFY batteryChanged)
    Q_PROPERTY(qreal batteryTimeToEmpty READ batteryTimeToEmpty NOTIFY batteryChanged)
    Q_PROPERTY(qreal batteryTimeToFull READ batteryTimeToFull NOTIFY batteryChanged)
    Q_PROPERTY(qreal volume READ volume WRITE setVolume NOTIFY audioChanged)
    Q_PROPERTY(bool muted READ muted WRITE setMuted NOTIFY audioChanged)
    Q_PROPERTY(qreal micVolume READ micVolume NOTIFY audioChanged)
    Q_PROPERTY(bool micMuted READ micMuted NOTIFY audioChanged)
    Q_PROPERTY(QString audioOutputName READ audioOutputName NOTIFY audioChanged)
    Q_PROPERTY(QString networkType READ networkType NOTIFY networkChanged)
    Q_PROPERTY(QString wifiSsid READ wifiSsid NOTIFY networkChanged)
    Q_PROPERTY(int wifiSignal READ wifiSignal NOTIFY networkChanged)
    Q_PROPERTY(QVariantList wifiNetworks READ wifiNetworks NOTIFY networkChanged)
    Q_PROPERTY(bool bluetoothEnabled READ bluetoothEnabled NOTIFY bluetoothChanged)
    Q_PROPERTY(QVariantList bluetoothDevices READ bluetoothDevices NOTIFY bluetoothChanged)
    Q_PROPERTY(QVariantList notifications READ notifications NOTIFY notificationsChanged)
    Q_PROPERTY(QVariantList mediaSessions READ mediaSessions NOTIFY mediaChanged)
    Q_PROPERTY(QVariantList foregroundServices READ foregroundServices NOTIFY notificationsChanged)
public:
    explicit AndroidBridge(QObject *parent = nullptr);

    bool nativeSystemBar() const;
    QVariantMap systemInsets() const;
    Q_INVOKABLE QString takeSystemBarAction() const;
    QVariantMap themeState() const;
    Q_INVOKABLE void refreshThemeMarketplace() const;
    Q_INVOKABLE void installTheme(const QString &id) const;
    Q_INVOKABLE void applyTheme(const QString &id) const;
    Q_INVOKABLE void openThemes() { emit themeBrowserRequested(); }
    int batteryPercent() const;
    bool charging() const;
    qreal batteryTimeToEmpty() const;
    qreal batteryTimeToFull() const;
    qreal volume() const;
    void setVolume(qreal v);
    bool muted() const;
    void setMuted(bool m);
    qreal micVolume() const;
    bool micMuted() const;
    QString audioOutputName() const;
    QString networkType() const;
    QString wifiSsid() const;
    int wifiSignal() const;
    QVariantList wifiNetworks() const;
    bool bluetoothEnabled() const;
    QVariantList bluetoothDevices() const;
    QVariantList notifications() const;
    QVariantList mediaSessions() const;
    QVariantList foregroundServices() const;

    // App library + task list, standing in for .desktop entries and toplevels.
    Q_INVOKABLE QVariantList installedApps() const;
    Q_INVOKABLE QVariantList recentTasks() const;
    Q_INVOKABLE void launchApp(const QString &id) const;
    Q_INVOKABLE void openSettings(const QString &page) const;
    Q_INVOKABLE void openAppInfo(const QString &id) const;
    Q_INVOKABLE void moveToTask(int taskId) const;
    Q_INVOKABLE void closeForegroundTask() const;
    Q_INVOKABLE QUrl iconUrl(const QString &name) const;

    // Shell surfaces: insets, touch grabs, lock visibility.
    Q_INVOKABLE void setShellInset(const QString &surface, const QString &edge, int size,
                                   int layer = 2, int keyboardFocus = 0) const;
    Q_INVOKABLE void setOutsideTouchGrab(bool active) const;
    Q_INVOKABLE void setLockVisible(bool visible) const;

    // Commands. Omarchy drives the OS through omarchy-* scripts; on device
    // those are the shims in /system_ext/bin, run through the app's shell.
    Q_INVOKABLE void execDetached(const QVariant &command) const;
    Q_INVOKABLE void hyprlandCommand(const QString &request) const;

    // Persistence for PersistentProperties.
    Q_INVOKABLE void saveProperties(const QString &key, QObject *object) const;
    Q_INVOKABLE void restoreProperties(const QString &key, QObject *object) const;

    // Notifications + auth.
    Q_INVOKABLE void dismissNotification(const QString &key) const;
    Q_INVOKABLE void beginAuth(QObject *context) const;
    Q_INVOKABLE void cancelAuth() const;
    Q_INVOKABLE void submitPassword(const QString &password) const;

    // Network + bluetooth actions.
    Q_INVOKABLE void connectWifi(const QString &ssid, const QString &password) const;
    Q_INVOKABLE void setWifiEnabled(bool enabled) const;
    Q_INVOKABLE void setBluetoothEnabled(bool enabled) const;

    // Called from Java when system state changes.
    Q_INVOKABLE void notifyStateChanged(const QString &what);

    int idleSeconds() const;
    bool screenOn() const;
    bool idleInhibited() const;
    // Any input to the shell window counts as activity, and so does an
    // explicit wake from QML (a notification arriving, a media key).
    Q_INVOKABLE void resetIdle();
    Q_INVOKABLE void setIdleInhibited(bool inhibited);

protected:
    bool eventFilter(QObject *watched, QEvent *event) override;

signals:
    void systemBarActionRequested();
    void systemInsetsChanged();
    void themeStateChanged();
    void themeBrowserRequested();
    void batteryChanged();
    void audioChanged();
    void networkChanged();
    void bluetoothChanged();
    void notificationsChanged();
    void notificationPosted(const QVariantMap &notification);
    void mediaChanged();
    void tasksChanged();
    void installedAppsChanged();
    void authResult(bool ok);
    void idleChanged();

private:
    // Time since the last input event reached the shell window.
    QElapsedTimer m_idleTimer;
    bool m_idleInhibited = false;
};
