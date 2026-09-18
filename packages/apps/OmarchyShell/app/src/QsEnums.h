#pragma once
#include <QObject>
#include <QQmlEngine>

// Quickshell's plain enums. QML cannot declare upper-case properties, so these
// stay C++ types: `Edges.Top`, `PopupAdjustment.Slide`, `ExclusionMode.Ignore`
// then resolve exactly as they do upstream.
class Edges : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(Edges)
    QML_UNCREATABLE("Edges is an enum namespace")
public:
    enum Edge { None = 0, Top = 1, Left = 2, Right = 4, Bottom = 8 };
    Q_ENUM(Edge)
};

class PopupAdjustment : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(PopupAdjustment)
    QML_UNCREATABLE("PopupAdjustment is an enum namespace")
public:
    enum Adjustment {
        None = 0,
        SlideX = 1, SlideY = 2, Slide = 3,
        FlipX = 4, FlipY = 8, Flip = 12,
        ResizeX = 16, ResizeY = 32, Resize = 48,
        All = 63,
    };
    Q_ENUM(Adjustment)
};

class ExclusionMode : public QObject
{
    Q_OBJECT
    QML_NAMED_ELEMENT(ExclusionMode)
    QML_UNCREATABLE("ExclusionMode is an enum namespace")
public:
    // Normal reserves the exclusive zone, Ignore overlaps everything, Auto
    // derives the zone from the surface's anchors.
    enum Mode { Normal = 0, Ignore = 1, Auto = 2 };
    Q_ENUM(Mode)
};
