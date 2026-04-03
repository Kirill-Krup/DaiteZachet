package com.example.daitezachet.levels

import android.graphics.Color
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.SpikeDir

/**
 * Уровень 4 — «Два ключа».
 *
 * Нужно собрать ОБА ключа (красный и синий), прежде чем откроется дверь.
 * Шипы на полу и боковые шипы на правой стене загораживают прямой путь.
 *
 *  ╔════════════════════════════════╗
 *  ║          [R]         [B]      ║
 *  ║  ─────P1          ─────P2  ▶▶║  ← боковые шипы справа
 *  ║▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲║
 *  ╚════════════════════════════════╝
 *
 *  R = красный ключ (id=1), B = синий ключ (id=2)
 *  Дверь открывается когда собраны оба.
 */
class Level04 : Level() {
    override val number   = 4
    override val hintText = "Собери оба ключа"

    override fun setup(engine: GameEngine) {
        // Две платформы на разной высоте
        engine.addPlatform(0.04f, 0.74f, 0.38f)   // P1 — левая
        engine.addPlatform(0.33f, 0.60f, 0.86f)   // P2 — правая

        // Красный ключ на P1, синий на P2
        engine.placeKey(0.20f, 0.74f, id = 1, color = Color.rgb(255, 80, 80))
        engine.placeKey(0.70f, 0.60f, id = 2, color = Color.rgb(80, 140, 255))

        // Шипы на полу — по центру, оставляем зону у спавна и у правого края
        engine.addSpikesFloor(0.10f, 0.82f)

        // Боковые шипы на правой стене — мешают просто подбежать к двери
        engine.addSpikesWall(0.10f, 0.40f, xr = 0.96f, dir = SpikeDir.LEFT)

        // Дверь открывается только когда собраны оба ключа
        engine.openDoorWhenAllKeys()

        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }
}
