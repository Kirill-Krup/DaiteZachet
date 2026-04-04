package com.example.daitezachet.levels

import android.graphics.Color
import com.example.daitezachet.engine.GameEngine

/**
 * Уровень 2 — «Думай, прежде чем прыгать».
 *
 * Механики: два ключа (дверь автоматически когда оба собраны),
 *           кнопка-ловушка (засыпает пол шипами), шипы на полу.
 *
 * Платформы стоят близко — прыжок P1→P2 короткий, но МЕЖДУ ними кнопка-ловушка.
 * Нельзя приземляться на пол в промежутке — нужно прыгать напрямую P1→P2.
 *
 * Маршрут:
 *   Спавн → прыгнуть на P1 → взять красный ключ →
 *   прыгнуть НАПРЯМУЮ на P2 (не касаясь пола — там кнопка!) →
 *   взять синий ключ → дверь открылась автоматически →
 *   спрыгнуть с правого края P2 на чистый пол (x > 88%) → выход.
 *
 *  ╔════════════════════════════════════╗
 *  ║         [R]       [B]             ║
 *  ║  ────P1────  ──────P2──────     ▶ ║
 *  ║▲▲P1▲▲  [btn]  ▲▲▲▲▲P2▲▲▲▲       ║
 *  ╚════════════════════════════════════╝
 *
 *  [btn] — ловушка: засыпает весь пол шипами
 */
class Level02 : Level() {
    override val number   = 2
    override val hintText = "Не трогай кнопку!"

    override fun setup(engine: GameEngine) {
        // P1 — левая платформа
        engine.addPlatform(0.04f, 0.70f, 0.40f, 0.74f)

        // P2 — правая платформа, стоит близко к P1 (зазор 4%)
        engine.addPlatform(0.44f, 0.42f, 0.88f, 0.46f)

        // Красный ключ на P1, синий на P2
        engine.placeKey(0.22f, 0.70f, id = 1, color = Color.rgb(255, 80, 80))
        engine.placeKey(0.68f, 0.42f, id = 2, color = Color.rgb(80, 140, 255))

        // Шипы слева: нельзя пешком пройти от спавна к кнопке — нужно прыгать на P1
        engine.addSpikesFloor(0.10f, 0.38f)

        // Шипы справа: пол под P2 опасен — нельзя приземляться между платформами
        engine.addSpikesFloor(0.46f, 0.86f)

        // Дверь открывается сама когда собраны оба ключа, но визуально остаётся закрытой
        engine.door.visuallyOpen = false
        engine.openDoorWhenAllKeys()

        // Кнопка-ловушка: первое нажатие засыпает весь пол шипами (включая «остров» кнопки)
        engine.onButtonPressed = { e ->
            if (e.button.pressCount == 1) {
                e.addSpikesFloor(0.04f, 0.86f)
            }
        }

        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }
}
