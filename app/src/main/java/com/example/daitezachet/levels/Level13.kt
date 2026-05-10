package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.Player
import com.example.daitezachet.engine.Spike
import com.example.daitezachet.engine.SpikeDir
import kotlin.math.*

class Level13 : Level() {

    override val number   = 13
    override val hintText = ")"

    companion object {
        private const val UP_Y = 0.5f
        private const val PLAT_L_X1 = 0.05f; private const val PLAT_L_X2 = 0.25f
        private const val PLAT_R_X1 = 0.75f; private const val PLAT_R_X2 = 0.95f
        private const val MID_X1 = 0.40f; private const val MID_X2 = 0.60f
        private const val MID_Y  = 0.75f
        private const val HOMING_SPEED = 340f
        private const val TURN_SPEED   = 2.2f
        private const val RAIN_SPEED      = 600f
        private const val RAIN_COUNT_MIN  = 2
        private const val RAIN_COUNT_MAX  = 5
        private const val INVERT_DURATION_SEC = 5f
        private const val PENDULUM_AMPLITUDE_FRAC = 0.28f
        private const val PENDULUM_FREQUENCY      = 1.6f
        private const val RAIN_WAVES_MAX = 3
    }

    private data class HomingProjectile(val spike: Spike, var angle: Float)
    private val homingProjectiles = mutableListOf<HomingProjectile>()

    private data class FallingSpike(val spike: Spike)
    private val fallingSpikes = mutableListOf<FallingSpike>()

    private var rainActive      = false
    private var rainRepeatCount = 0

    private var doorPhase        = 0
    private var shootTimer       = 0f
    private var doorPatchPlatIdx = -1

    private var midJumpAttempted = false
    private var trollActive      = false
    private var trollTimer       = 0f

    private var isControlsInverted = false
    private var invertTriggered    = false
    private var invertDuration     = 0f

    private var midPlatIdx   = -1
    private val midPlatOrig  = RectF()
    private var leftPlatIdx  = -1
    private val leftPlatOrig = RectF()
    private var rightPlatIdx  = -1
    private val rightPlatOrig = RectF()
    private val leftSpikes     = mutableListOf<Spike>()
    private val leftSpikesOrig = mutableListOf<RectF>()

    private var warningAlpha = 0f
    private var warningText  = ""

    private data class PendulumSpike(
        val spike: Spike,
        val centerX: Float,
        val amplitude: Float,
        val frequency: Float,
        var phase: Float
    )
    private val pendulums        = mutableListOf<PendulumSpike>()
    private var pendulumActive   = false
    private var pendulumTriggered = false
    private var pendulumMsgAlpha  = 0f

    private var platOscActive    = false
    private var platOscTriggered = false
    private var platOscDuration  = 0f
    private var platOscPhase     = 0f

    private var slowAfterKeyActive = false
    private var slowAfterKeyTimer  = 0f
    private var slowMsgAlpha       = 0f

    private var teleportTrollUsed = false
    private var teleportMsgAlpha  = 0f

    override fun setup(engine: GameEngine) {
        homingProjectiles.clear(); fallingSpikes.clear()
        leftSpikes.clear(); leftSpikesOrig.clear(); pendulums.clear()

        rainActive = false; rainRepeatCount = 0
        doorPhase = 0; shootTimer = 0f; doorPatchPlatIdx = -1
        midJumpAttempted = false; trollActive = false; trollTimer = 0f
        isControlsInverted = false; invertTriggered = false; invertDuration = 0f
        warningAlpha = 0f; warningText = ""
        pendulumActive = false; pendulumTriggered = false; pendulumMsgAlpha = 0f
        platOscActive = false; platOscTriggered = false; platOscDuration = 0f; platOscPhase = 0f
        slowAfterKeyActive = false; slowAfterKeyTimer = 0f; slowMsgAlpha = 0f
        teleportTrollUsed = false; teleportMsgAlpha = 0f

        engine.door.bounds.set(engine.room.doorRect)
        engine.platforms.clear()
        engine.spikes.clear()

        engine.addPlatform(PLAT_L_X1, UP_Y, PLAT_L_X2, UP_Y + 0.04f)
        leftPlatIdx = engine.platforms.size - 1
        leftPlatOrig.set(engine.platforms[leftPlatIdx])

        engine.addPlatform(PLAT_R_X1, UP_Y, PLAT_R_X2, UP_Y + 0.04f)
        rightPlatIdx = engine.platforms.size - 1
        rightPlatOrig.set(engine.platforms[rightPlatIdx])

        engine.addPlatform(MID_X1, MID_Y, MID_X2, MID_Y + 0.04f)
        midPlatIdx = engine.platforms.size - 1
        midPlatOrig.set(engine.platforms[midPlatIdx])

        val spikesBefore = engine.spikes.size
        engine.addSpikesFloor(0.25f, 0.40f)
        for (i in spikesBefore until engine.spikes.size) {
            leftSpikes.add(engine.spikes[i])
            leftSpikesOrig.add(RectF(engine.spikes[i].bounds))
        }
        engine.addSpikesFloor(0.60f, 0.75f)

        engine.placeKey(0.85f, UP_Y, id = 0, color = Color.YELLOW)

   
        engine.winCondition = { e -> e.playerInsideDoor() }

        engine.onUpdate = { e, dt -> tick(e, dt) }
    }

    private fun tick(engine: GameEngine, dt: Float) {
        checkProximityTriggers(engine)
        handleInversion(engine, dt)
        handleMidPlatformTroll(engine, dt)
        handleDoorTroll(engine, dt)
        updateHomingProjectiles(engine, dt)
        updateFallingSpikes(engine, dt)
        handlePendulums(engine, dt)
        handlePlatOscillation(engine, dt)
        handleSlowAfterKey(engine, dt)
        handleTeleportTroll(engine)

        if (warningAlpha     > 0f) warningAlpha     = (warningAlpha     - dt * 160f).coerceAtLeast(0f)
        if (teleportMsgAlpha > 0f) teleportMsgAlpha = (teleportMsgAlpha - dt * 80f).coerceAtLeast(0f)
        if (slowMsgAlpha     > 0f) slowMsgAlpha     = (slowMsgAlpha     - dt * 50f).coerceAtLeast(0f)
        if (pendulumMsgAlpha > 0f) pendulumMsgAlpha = (pendulumMsgAlpha - dt * 25f).coerceAtLeast(0f)
    }

    private fun checkProximityTriggers(engine: GameEngine) {
        val p = engine.player
        val w = engine.room.w

        val nearKeyZone = p.x + Player.WIDTH > w * 0.68f
        if (nearKeyZone && !rainActive && rainRepeatCount < RAIN_WAVES_MAX) {
            rainActive = true
            rainRepeatCount++
            spawnSpikeWave(engine)
        }
        if (!nearKeyZone && rainActive) rainActive = false

        if (!platOscTriggered && leftPlatIdx in engine.platforms.indices) {
            val lp = engine.platforms[leftPlatIdx]
            val onLeft = p.x + Player.WIDTH > lp.left && p.x < lp.right &&
                    abs((p.y + Player.HEIGHT) - lp.top) < 10f && p.vy >= 0f
            if (onLeft) {
                platOscTriggered = true; platOscActive = true
                platOscDuration = 5f; platOscPhase = 0f
                warningAlpha = 255f; warningText = "~ ПЛАТФОРМЫ НЕСТАБИЛЬНЫ ~"
            }
        }

        if (doorPhase == 2 && !invertTriggered) {
            val door = engine.door.bounds
            val dist = sqrt((p.x - door.centerX()).pow(2) + (p.y - door.centerY()).pow(2))
            if (dist < w * 0.20f) {
                invertTriggered = true; isControlsInverted = true
                invertDuration = INVERT_DURATION_SEC
            }
        }
    }

    private fun handleInversion(engine: GameEngine, dt: Float) {
        if (!isControlsInverted) return
        if (engine.moveLeft && !engine.moveRight)
            engine.player.x += 2f * Player.MOVE_SPEED * dt
        else if (engine.moveRight && !engine.moveLeft)
            engine.player.x -= 2f * Player.MOVE_SPEED * dt
        invertDuration -= dt
        if (invertDuration <= 0f) {
            isControlsInverted = false
            invertTriggered = false
        }
    }

    private fun spawnSpikeWave(engine: GameEngine) {
        val count  = RAIN_COUNT_MIN + (Math.random() * (RAIN_COUNT_MAX - RAIN_COUNT_MIN + 1)).toInt()
        val spikeW = 28f; val spikeH = 24f
        warningAlpha = 255f; warningText = "⚠ БЕРЕГИСЬ СВЕРХУ ⚠"
        repeat(count) {
            val x = engine.room.w * 0.60f + (Math.random() * (engine.room.w * 0.35f - spikeW)).toFloat()
            val spike = Spike(x, -spikeH - 10f, spikeW, spikeH, SpikeDir.DOWN)
            engine.spikes.add(spike); fallingSpikes.add(FallingSpike(spike))
        }
    }

    private fun updateFallingSpikes(engine: GameEngine, dt: Float) {
        val iter = fallingSpikes.iterator()
        while (iter.hasNext()) {
            val fs = iter.next()
            fs.spike.bounds.offset(0f, RAIN_SPEED * dt)
            if (fs.spike.bounds.top > engine.room.h + 50f) {
                engine.spikes.remove(fs.spike); iter.remove()
            }
        }
    }

    private fun handleMidPlatformTroll(engine: GameEngine, dt: Float) {
        if (trollActive) {
            trollTimer -= dt
            if (trollTimer <= 0f) {
                engine.platforms[midPlatIdx].set(midPlatOrig)
                leftSpikes.forEachIndexed { i, s -> s.bounds.set(leftSpikesOrig[i]) }
                trollActive = false
            }
            return
        }
        if (!midJumpAttempted) {
            val p      = engine.player
            val aboveX = p.x + Player.WIDTH > midPlatOrig.left && p.x < midPlatOrig.right
            val falling = p.vy > 0 &&
                    (p.y + Player.HEIGHT) > (midPlatOrig.top - 80f) &&
                    (p.y + Player.HEIGHT) < midPlatOrig.top
            if (aboveX && falling) {
                engine.platforms[midPlatIdx].set(-1000f, -1000f, -1000f, -1000f)
                leftSpikes.forEach { it.bounds.offset(engine.room.w * 0.15f, 0f) }
                midJumpAttempted = true; trollActive = true; trollTimer = 3f
            }
        }
    }

    private fun handleDoorTroll(engine: GameEngine, dt: Float) {
        val player = engine.player
        val door   = engine.door.bounds
        val w      = engine.room.w

        when (doorPhase) {
            0 -> {
                if (engine.collectedKeys.isNotEmpty() && abs(player.x - door.centerX()) < w * 0.12f) {
                    engine.platforms.add(RectF(engine.room.doorRect))
                    doorPatchPlatIdx = engine.platforms.size - 1
                    val newX = engine.room.wallThick + 20f
                    val newY = engine.room.h * 0.05f
                    door.set(newX, newY, newX + engine.room.doorRect.width(), newY + engine.room.doorRect.height())
                    doorPhase = 1
                }
            }
            1 -> {
                val dist = sqrt((player.x - door.centerX()).pow(2) + (player.y - door.centerY()).pow(2))
                if (dist < w * 0.15f) {
                    door.set(engine.room.doorRect)
                    if (doorPatchPlatIdx in engine.platforms.indices)
                        engine.platforms[doorPatchPlatIdx].set(-2000f, -2000f, -1990f, -1990f)


                    engine.door.isOpen = true

                    doorPhase = 2; shootTimer = 0f
                }
            }
            2 -> {
                shootTimer -= dt
                if (shootTimer <= 0f) { spawnHomingSpike(engine); shootTimer = 1.2f }
            }
        }
    }

    private fun spawnHomingSpike(engine: GameEngine) {
        val db    = engine.door.bounds
        val angle = atan2(engine.player.y - db.centerY(), engine.player.x - db.centerX())
        val spike = Spike(db.centerX(), db.centerY(), 28f, 24f, SpikeDir.LEFT)
        engine.spikes.add(spike); homingProjectiles.add(HomingProjectile(spike, angle))
    }

    private fun updateHomingProjectiles(engine: GameEngine, dt: Float) {
        val iter = homingProjectiles.iterator()
        while (iter.hasNext()) {
            val p = iter.next(); val b = p.spike.bounds
            val target = atan2(engine.player.bounds.centerY() - b.centerY(), engine.player.bounds.centerX() - b.centerX())
            var diff = target - p.angle
            while (diff >  PI) diff -= (2 * PI).toFloat()
            while (diff < -PI) diff += (2 * PI).toFloat()
            p.angle += diff * TURN_SPEED * dt
            b.offset(cos(p.angle) * HOMING_SPEED * dt, sin(p.angle) * HOMING_SPEED * dt)
            if (b.left < -100 || b.right > engine.room.w + 100 || b.top < -100 || b.bottom > engine.room.h + 100) {
                engine.spikes.remove(p.spike); iter.remove()
            }
        }
    }

    private fun handlePendulums(engine: GameEngine, dt: Float) {
        if (!pendulumActive) return
        if (engine.collectedKeys.isNotEmpty()) {
            pendulums.forEach { engine.spikes.remove(it.spike) }
            pendulums.clear(); pendulumActive = false; pendulumTriggered = false
            return
        }
        pendulums.forEach { p ->
            p.phase += p.frequency * dt
            val newCx = p.centerX + sin(p.phase) * p.amplitude
            p.spike.bounds.offsetTo(newCx - p.spike.bounds.width() / 2f, p.spike.bounds.top)
        }
    }

    private fun spawnPendulums(engine: GameEngine) {
        val w = engine.room.w; val h = engine.room.h
        val spikeW = 36f; val spikeH = 30f
        val amp = w * PENDULUM_AMPLITUDE_FRAC
        listOf(
            Triple(w * 0.50f, h * 0.32f, 0f),
            Triple(w * 0.35f, h * 0.50f, PI.toFloat() * 2f / 3f),
            Triple(w * 0.65f, h * 0.67f, PI.toFloat() * 4f / 3f)
        ).forEach { (cx, y, phase) ->
            val spike = Spike(cx - spikeW / 2f, y, spikeW, spikeH, SpikeDir.DOWN)
            engine.spikes.add(spike)
            pendulums.add(PendulumSpike(spike, cx, amp, PENDULUM_FREQUENCY, phase))
        }
        pendulumActive = true; pendulumMsgAlpha = 255f
        warningAlpha = 255f; warningText = "⚠ ОСТОРОЖНО, МАЯТНИКИ ⚠"
    }

    private fun handlePlatOscillation(engine: GameEngine, dt: Float) {
        if (!platOscActive) return
        platOscDuration -= dt; platOscPhase += dt * 7f
        val shiftY = sin(platOscPhase) * engine.room.h * 0.035f
        if (leftPlatIdx  in engine.platforms.indices) { engine.platforms[leftPlatIdx].set(leftPlatOrig);  engine.platforms[leftPlatIdx].offset(0f, shiftY) }
        if (rightPlatIdx in engine.platforms.indices) { engine.platforms[rightPlatIdx].set(rightPlatOrig); engine.platforms[rightPlatIdx].offset(0f, -shiftY) }
        if (platOscDuration <= 0f) {
            if (leftPlatIdx  in engine.platforms.indices) engine.platforms[leftPlatIdx].set(leftPlatOrig)
            if (rightPlatIdx in engine.platforms.indices) engine.platforms[rightPlatIdx].set(rightPlatOrig)
            platOscActive = false; platOscTriggered = false
        }
    }

    private fun handleSlowAfterKey(engine: GameEngine, dt: Float) {
        if (!slowAfterKeyActive && engine.collectedKeys.isNotEmpty()) {
            slowAfterKeyActive = true; slowAfterKeyTimer = 7f; slowMsgAlpha = 255f
        }
        if (slowAfterKeyActive && slowAfterKeyTimer > 0f) {
            slowAfterKeyTimer -= dt
            engine.player.vx *= 0.80f
            if (slowAfterKeyTimer <= 0f) slowAfterKeyActive = false
        }
    }

    private fun handleTeleportTroll(engine: GameEngine) {
        if (teleportTrollUsed) return
        val p  = engine.player
        val rp = engine.platforms.getOrNull(rightPlatIdx) ?: return
        val onRight = p.x + Player.WIDTH > rp.left && p.x < rp.right &&
                abs((p.y + Player.HEIGHT) - rp.top) < 8f && p.vy >= 0f
        if (onRight) {
            val lp = engine.platforms.getOrNull(leftPlatIdx) ?: return
            engine.player.x = lp.centerX() - Player.WIDTH / 2f
            engine.player.y = lp.top - Player.HEIGHT - 5f
            engine.player.vy = 0f
            teleportTrollUsed = true; teleportMsgAlpha = 255f
            spawnPendulums(engine)
        }
    }

    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
        val cx = engine.room.w / 2f

        if (doorPhase == 1) {
            paint.color = Color.CYAN; paint.textSize = 30f; paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Я ТУТ!", engine.door.bounds.centerX(), engine.door.bounds.top - 20f, paint)
        }
        if (isControlsInverted) {
            paint.color = Color.RED; paint.textSize = 40f; paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ИНВЕРСИЯ", cx, engine.room.h * 0.2f, paint)
        }
        if (warningAlpha > 0f) {
            paint.color = Color.argb(warningAlpha.toInt().coerceIn(0, 255), 255, 80, 0)
            paint.textSize = 36f; paint.textAlign = Paint.Align.CENTER
            canvas.drawText(warningText, cx, engine.room.h * 0.12f, paint)
        }
        if (pendulumMsgAlpha > 0f) {
            paint.color = Color.argb(pendulumMsgAlpha.toInt().coerceIn(0, 255), 255, 140, 0)
            paint.textSize = 28f; paint.textAlign = Paint.Align.CENTER
            canvas.drawText("~ МАЯТНИКИ АКТИВНЫ ~", cx, engine.room.h * 0.18f, paint)
        }
        if (platOscActive) {
            paint.color = Color.argb(160, 255, 200, 0)
            paint.textSize = 28f; paint.textAlign = Paint.Align.CENTER
            canvas.drawText("~ ПЛАТФОРМЫ НЕСТАБИЛЬНЫ ~", cx, engine.room.h * 0.18f, paint)
        }
        if (slowMsgAlpha > 0f) {
            paint.color = Color.argb(slowMsgAlpha.toInt().coerceIn(0, 255), 0, 220, 120)
            paint.textSize = 32f; paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ОСТАЛОСЬ ЧУТЬ-ЧУТЬ", cx, engine.room.h * 0.35f, paint)
        }
        if (teleportMsgAlpha > 0f) {
            paint.color = Color.argb(teleportMsgAlpha.toInt().coerceIn(0, 255), 200, 100, 255)
            paint.textSize = 34f; paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ТАМ БЫЛО НЕ ТАК БЕЗОПАСНО :)", cx, engine.room.h * 0.28f, paint)
        }
    }
}
