package com.example.daitezachet.levels

import android.graphics.Color
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.SpikeDir

class Level07 : Level() {
    override val number   = 2
    override val hintText = "Инверсия гравитации"

    private var gravityInverted = false

    override fun setup(engine: GameEngine) {
        gravityInverted = false

        // ================================================================
        // ПЛАТФОРМЫ
        // ================================================================

        // Левая верхняя платформа (под потолком)
        engine.addPlatform(0.36f, 0.00f, 0.42f, 0.08f)

        // Центральная платформа с ключом (пониже)
        engine.addPlatform(0.47f, 0.24f, 0.53f, 0.28f)

        // Правая верхняя платформа (под потолком)
        engine.addPlatform(0.58f, 0.00f, 0.64f, 0.08f)

        // ================================================================
        // КЛЮЧ
        // ================================================================

        engine.placeKey(0.50f, 0.24f, id = 1, color = Color.rgb(255, 215, 0))

        // ================================================================
        // ШИПЫ — ПОТОЛОК
        // ================================================================

        // Потолочные шипы: от левой стены до левой верхней платформы
        engine.addSpikesAt(0.02f, 0.36f, 0.00f, SpikeDir.DOWN)

        // Потолочные шипы между верхними платформами
        engine.addSpikesAt(0.43f, 0.57f, 0.00f, SpikeDir.DOWN)

        // Потолочные шипы: от правой верхней платформы до правой стены
        engine.addSpikesAt(0.64f, 1.00f, 0.00f, SpikeDir.DOWN)

        // ================================================================
        // ШИПЫ — ПОЛ
        // ================================================================

        engine.addSpikesFloor(0.07f, 1.00f)

        // ================================================================
        // ДВЕРЬ
        // ================================================================

        engine.openDoorWhenKey(1)
        engine.door.visuallyOpen = false

        // ================================================================
        // УСЛОВИЕ ПОБЕДЫ
        // ================================================================

        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }

        // ================================================================
        // ИНВЕРСИЯ ГРАВИТАЦИИ
        // ================================================================

        engine.onUpdate = { e, dt ->
            if (e.jumpRequested) {
                gravityInverted = !gravityInverted
                e.player.vy = if (gravityInverted) -300f else 300f
            }

            if (gravityInverted) {
                // Компенсация стандартной гравитации (1600 вниз) + своя вверх
                // Итог: -1600 px/s² (вверх)
                e.player.vy -= 2 * GameEngine.GRAVITY * dt
            }
        }
    }
}