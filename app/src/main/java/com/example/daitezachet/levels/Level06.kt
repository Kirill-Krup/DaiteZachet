package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.daitezachet.engine.GameEngine

/**
 * Уровень «Стратагема» – кодовая последовательность перед дверью.
 *
 * Игрок стартует слева, пол усеян шипами. Единственный безопасный путь –
 * запрыгнуть на длинную платформу и дойти до выделенной зоны у правого края.
 * Там активируется испытание: после короткой задержки нужно нажать 8 кнопок
 * движения в правильном порядке (← → ↑ ← → ↑ ← →). Ошибка или истечение
 * времени — мгновенная смерть. Успех открывает дверь к выходу.
 */
class Level06 : Level() {
    override val number   = 2
    override val hintText = "Введи код у двери"

    // Состояния испытания
    private var preChallenge       = false   // задержка перед началом ввода
    private var challengeActive    = false   // активный ввод
    private var challengeCompleted = false   // код введён верно
    private var sequenceIndex      = 0       // текущая позиция в последовательности
    private var timer              = 2.0f    // основное время (секунд)
    private var preTimer           = 2.0f    // время задержки перед стартом

    // Для отслеживания фронтов нажатий движения
    private var prevLeft  = false
    private var prevRight = false

    // Направления кнопок
    private enum class Dir { LEFT, RIGHT, UP }

    // 8-элементная последовательность
    private val sequence = listOf(
        Dir.LEFT, Dir.UP, Dir.RIGHT, Dir.UP,
        Dir.LEFT, Dir.UP, Dir.UP, Dir.RIGHT
    )

    // Зона активации (в долях комнаты) – правый край платформы
    private val triggerLeft  = 0.80f
    private val triggerRight = 0.92f
    private val platformTop  = 0.68f   // верхняя грань платформы

    override fun setup(engine: GameEngine) {
        // Сброс всех переменных состояния
        preChallenge       = false
        challengeActive    = false
        challengeCompleted = false
        sequenceIndex      = 0
        timer              = 2.0f
        preTimer           = 2.0f
        prevLeft           = false
        prevRight          = false

        // Платформа почти через всю комнату, заканчивается у двери
        engine.addPlatform(0.05f, 0.68f, 0.95f, 0.72f)

        // Весь пол усеян шипами, кроме самого левого края у спавна
        engine.addSpikesFloor(0.08f, 1.0f)

        // Дверь изначально закрыта, визуально будем показывать открытие
        engine.door.isOpen = false
        engine.door.visuallyOpen = true

        // Победа: дверь открыта и игрок в проёме
        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }

        // Каждый кадр проверяем активацию, задержку и ввод
        engine.onUpdate = { e, dt ->
            if (!preChallenge && !challengeActive && !challengeCompleted) {
                checkActivation(e)
            }
            if (preChallenge) {
                preTimer -= dt
                if (preTimer <= 0f) {
                    // Задержка истекла – запускаем основное испытание
                    preChallenge = false
                    challengeActive = true
                    timer = 2.0f
                    sequenceIndex = 0
                    // Синхронизируем состояние кнопок, чтобы удержание не считалось новым нажатием
                    prevLeft  = e.moveLeft
                    prevRight = e.moveRight
                }
            } else if (challengeActive) {
                updateChallenge(e, dt)
            }
        }
    }

    /**
     * Проверяет, стоит ли игрок на платформе в зоне активации.
     */
    private fun checkActivation(engine: GameEngine) {
        val player = engine.player
        if (!player.isOnGround) return
        val pb = player.bounds
        val room = engine.room

        val onTriggerZone = pb.centerX() >= room.w * triggerLeft &&
                pb.centerX() <= room.w * triggerRight &&
                pb.bottom >= room.h * platformTop - 4f &&
                pb.bottom <= room.h * platformTop + 4f
        if (onTriggerZone) {
            preChallenge = true
            preTimer = 2.0f
        }
    }

    /**
     * Обрабатывает таймер и ввод последовательности во время активного испытания.
     */
    private fun updateChallenge(engine: GameEngine, dt: Float) {
        timer -= dt
        if (timer <= 0f) {
            // Время вышло — смерть
            engine.player.isDead = true
            challengeActive = false
            return
        }

        // Определяем фронты нажатий (переход из отжатого в нажатое)
        val leftPressed  = engine.moveLeft && !prevLeft
        val rightPressed = engine.moveRight && !prevRight
        val jumpPressed  = engine.jumpRequested   // импульсный флаг, сбрасывается физикой

        if (leftPressed || rightPressed || jumpPressed) {
            val expected = sequence[sequenceIndex]
            val correct = when {
                leftPressed  -> expected == Dir.LEFT
                rightPressed -> expected == Dir.RIGHT
                jumpPressed  -> expected == Dir.UP
                else         -> false
            }

            if (correct) {
                sequenceIndex++
                if (sequenceIndex == sequence.size) {
                    // Вся последовательность введена верно
                    challengeActive = false
                    challengeCompleted = true
                    engine.door.isOpen = true
                }
            } else {
                // Ошибка ввода — смерть
                engine.player.isDead = true
                challengeActive = false
            }
        }

        // Запоминаем состояние кнопок для следующего кадра
        prevLeft  = engine.moveLeft
        prevRight = engine.moveRight
    }

    // ----------------------- Отрисовка интерфейса -----------------------
    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
        val w = engine.room.w
        val h = engine.room.h

        // 1. Визуальная отметка зоны активации (всегда, пока испытание не пройдено)
        if (!challengeCompleted) {
            val markerLeft = w * triggerLeft
            val markerTop = h * platformTop - 12f
            val markerRight = w * triggerRight
            val markerBottom = h * platformTop + 6f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.argb(120, 255, 255, 0)
            canvas.drawRect(markerLeft, markerTop, markerRight, markerBottom, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 32f
            paint.color = Color.YELLOW
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Встань здесь", (markerLeft + markerRight) / 2f, markerTop - 6f, paint)
        }

        // 2. Панель с подсказками и таймером (только когда идёт испытание или задержка)
        if (!preChallenge && !challengeActive && !challengeCompleted) return

        val panelLeft   = w * 0.1f
        val panelRight  = w * 0.9f
        val panelTop    = h * 0.15f
        val panelBottom = h * 0.45f

        // Полупрозрачный фон
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(200, 10, 10, 25)
        canvas.drawRect(panelLeft, panelTop, panelRight, panelBottom, paint)

        if (preChallenge) {
            // Фаза задержки
            paint.color = Color.WHITE
            paint.textSize = 56f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Приготовьтесь... %.0f".format(preTimer.coerceAtLeast(0f)),
                w * 0.5f, h * 0.25f, paint)
            drawSequence(canvas, paint, sequence, -1, w * 0.5f, h * 0.35f, false)
        } else if (challengeActive) {
            // Активный ввод
            paint.color = Color.WHITE
            paint.textSize = 56f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("%.1f".format(timer), w * 0.5f, h * 0.25f, paint)
            drawSequence(canvas, paint, sequence, sequenceIndex, w * 0.5f, h * 0.38f, true)
        } else if (challengeCompleted) {
            // Успех
            paint.color = Color.rgb(60, 220, 80)
            paint.textSize = 48f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Доступ открыт", w * 0.5f, h * 0.30f, paint)
        }
    }

    /**
     * Рисует последовательность стрелок.
     * @param currentIdx индекс текущего вводимого символа (-1 если ни один не введён)
     * @param showProgress подсвечивать ли пройденные символы зелёным, а текущий жёлтым
     */
    private fun drawSequence(
        canvas: Canvas,
        paint: Paint,
        seq: List<Dir>,
        currentIdx: Int,
        centerX: Float,
        baseY: Float,
        showProgress: Boolean
    ) {
        paint.textSize = 58f
        paint.textAlign = Paint.Align.CENTER
        val symbols = seq.map {
            when (it) {
                Dir.LEFT  -> "←"
                Dir.RIGHT -> "→"
                Dir.UP    -> "↑"
            }
        }
        // Рассчитываем общую ширину для центрирования
        val totalWidth = symbols.sumOf { paint.measureText(it).toDouble() }.toFloat() +
                (symbols.size - 1) * 20f
        var x = centerX - totalWidth / 2f + paint.measureText(symbols[0]) / 2f

        for (i in symbols.indices) {
            val isPassed  = i < currentIdx
            val isCurrent = i == currentIdx
            paint.color = when {
                isPassed && showProgress  -> Color.rgb(60, 220, 80)   // пройдено
                isCurrent && showProgress -> Color.rgb(255, 220, 40)  // текущий
                else                      -> Color.argb(100, 200, 200, 200) // будущий
            }
            canvas.drawText(symbols[i], x, baseY, paint)
            x += paint.measureText(symbols[i]) + 20f
        }
    }
}