#pragma once
#include <QObject>
#include <QQmlEngine>

// Quickshell.Wayland — layer-shell concepts, kept as a real attached type so
// Omarchy's `WlrLayershell.layer: WlrLayer.Overlay` lines work untouched. The
// values steer which Android window type and focus mode the host gives the
// surface.
class WlrLayershellAttached : public QObject
{
    Q_OBJECT
    Q_PROPERTY(QString namespace MEMBER m_namespace NOTIFY changed)
    Q_PROPERTY(int layer MEMBER m_layer NOTIFY changed)
    Q_PROPERTY(int keyboardFocus MEMBER m_keyboardFocus NOTIFY changed)
public:
    using QObject::QObject;
signals:
    void changed();
private:
    QString m_namespace;
    int m_layer = 2;          // WlrLayer.Top
    int m_keyboardFocus = 0;  // WlrKeyboardFocus.None
};

class WlrLayershell : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(WlrLayershell)
    QML_ATTACHED(WlrLayershellAttached)
    QML_UNCREATABLE("WlrLayershell is only usable as an attached property")
public:
    using QObject::QObject;
    static WlrLayershellAttached *qmlAttachedProperties(QObject *object)
    {
        return new WlrLayershellAttached(object);
    }
};

// Window stacking, mapped to Android window types by the host:
// Background -> wallpaper, Bottom -> below apps, Top -> above apps,
// Overlay -> above the status bar and keyguard.
class WlrLayer : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(WlrLayer)
    QML_SINGLETON
public:
    using QObject::QObject;
    enum Layer { Background = 0, Bottom = 1, Top = 2, Overlay = 3 };
    Q_ENUM(Layer)
    Q_PROPERTY(int Background READ background CONSTANT)
    Q_PROPERTY(int Bottom READ bottom CONSTANT)
    Q_PROPERTY(int Top READ top CONSTANT)
    Q_PROPERTY(int Overlay READ overlay CONSTANT)
    int background() const { return Background; }
    int bottom() const { return Bottom; }
    int top() const { return Top; }
    int overlay() const { return Overlay; }
};

class WlrKeyboardFocus : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(WlrKeyboardFocus)
    QML_SINGLETON
public:
    using QObject::QObject;
    enum Focus { None = 0, OnDemand = 1, Exclusive = 2 };
    Q_ENUM(Focus)
    Q_PROPERTY(int None READ none CONSTANT)
    Q_PROPERTY(int OnDemand READ onDemand CONSTANT)
    Q_PROPERTY(int Exclusive READ exclusive CONSTANT)
    int none() const { return None; }
    int onDemand() const { return OnDemand; }
    int exclusive() const { return Exclusive; }
};
