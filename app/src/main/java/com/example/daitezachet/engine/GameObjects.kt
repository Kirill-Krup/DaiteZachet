package com.example.daitezachet.engine

import android.graphics.Color
import android.graphics.RectF

class Button(rect: RectF) {
    val bounds     = RectF(rect)
    var isPressed  = false
    var pressCount = 0
    var hidden     = false
}

class Door(rect: RectF) {
    val bounds       = RectF(rect)
    var isOpen       = false
    /** Если false — дверь рисуется как закрытая, даже когда isOpen = true. */
    var visuallyOpen = true
}

/**
 * @param id    уникальный номер ключа — используется в openDoorWhenKey(id)
 * @param color цвет ключа (android.graphics.Color ARGB)
 */
class Key(x: Float, y: Float, val id: Int = 0, val color: Int = Color.rgb(255, 215, 0)) {
    companion object { const val SIZE = 30f }
    val bounds      = RectF(x, y, x + SIZE, y + SIZE)
    var isCollected = false
}

/** Направление острия шипа. */
enum class SpikeDir {
    UP,     // острие вверх  (на полу / верх платформы)
    DOWN,   // острие вниз   (с потолка / низ платформы)
    LEFT,   // острие влево  (на правой стене / правый бок платформы)
    RIGHT   // острие вправо (на левой стене / левый бок платформы)
}

/**
 * @param dir  направление острия шипа
 */
class Spike(
    x: Float, y: Float,
    val width:  Float    = 28f,
    val height: Float    = 24f,
    val dir:    SpikeDir = SpikeDir.UP
) {
    val bounds = RectF(x, y, x + width, y + height)
}
