package com.example.daitezachet.engine

import android.graphics.Color

enum class EyeStyle {
    NORMAL,
    ANGRY,
    SLEEPY,
    DEAD
}

enum class Hat {
    NONE,
    CAP,
    CROWN,
    WIZARD,
    TOP_HAT
}

data class PlayerAppearance(
    val bodyColor: Int   = Color.rgb(230, 230, 255),
    val eyeStyle: EyeStyle = EyeStyle.NORMAL,
    val hat: Hat           = Hat.NONE
)