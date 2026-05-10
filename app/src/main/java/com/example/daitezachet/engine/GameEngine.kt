package com.example.daitezachet.engine

import android.graphics.Color
import android.graphics.RectF

class GameEngine(val room: Room) {

    companion object {
        const val GRAVITY    = 1600f
        const val JUMP_GRACE = 0.12f   // первые N секунд после прыжка — отпускание не обрезает высоту
    }

    val player    = Player(room.playerSpawnX, room.playerSpawnY)
    val button    = Button(room.buttonRect)
    val door      = Door(room.doorRect)

    val keys          = mutableListOf<Key>()          // все ключи уровня
    val collectedKeys = mutableSetOf<Int>()           // id собранных ключей
    val spikes        = mutableListOf<Spike>()
    val platforms     = mutableListOf<RectF>()

    var levelComplete = false

    @Volatile var moveLeft      = false
    @Volatile var moveRight     = false
    @Volatile var jumpRequested = false
    @Volatile var jumpHeld      = false

    private var prevJumpHeld   = false
    private var jumpGraceTimer = 0f          // секунды до конца льготного окна прыжка

    // Хуки уровня
    var onButtonPressed:  ((GameEngine) -> Unit)?         = null
    var onButtonReleased: ((GameEngine) -> Unit)?         = null
    var onUpdate:         ((GameEngine, Float) -> Unit)?  = null
    var winCondition:     ((GameEngine) -> Boolean)?      = null
    /** Автоматически открывает дверь когда возвращает true (проверяется каждый тик). */
    var doorCondition:    ((GameEngine) -> Boolean)?      = null

    // -------------------------------------------------------------------------

    fun update(dt: Float) {
        if (levelComplete || player.isDead) return
        onUpdate?.invoke(this, dt)
        applyPhysics(dt)
        checkInteractions()
    }

    // -------------------------------------------------------------------------
    // Physics

    private fun applyPhysics(dt: Float) {
        player.vx = when {
            moveLeft && !moveRight -> -Player.MOVE_SPEED
            moveRight && !moveLeft ->  Player.MOVE_SPEED
            else                   ->  0f
        }
        if (jumpRequested && player.isOnGround) {
            player.vy = Player.JUMP_FORCE
            player.isOnGround = false
            jumpGraceTimer = JUMP_GRACE     // запускаем льготное окно
        }
        jumpRequested = false

        if (jumpGraceTimer > 0f) jumpGraceTimer -= dt

        // Jump cut: отпустил кнопку в полёте → обрезаем скорость вверх,
        // но только после истечения льготного окна (первые JUMP_GRACE секунд — без обрезки).
        if (prevJumpHeld && !jumpHeld && player.vy < 0f) {
            player.vy = player.vy.coerceAtLeast(-320f)
        }
        prevJumpHeld = jumpHeld

        player.vy += GRAVITY * dt
        player.vy  = player.vy.coerceAtMost(1200f)
        player.isOnGround = false

        player.x += player.vx * dt
        val solids = buildSolids()
        solids.forEach { resolveH(it) }

        player.y += player.vy * dt
        solids.forEach { resolveV(it) }
    }

    private fun buildSolids(): List<RectF> {
        val list = ArrayList<RectF>(room.staticSolids.size + platforms.size + 1)
        list.addAll(room.staticSolids)
        list.addAll(platforms)
        if (!door.isOpen) list.add(door.bounds)
        return list
    }

    private fun resolveH(rect: RectF) {
        val pb = player.bounds
        if (!RectF.intersects(pb, rect)) return
        val overlapL = pb.right  - rect.left
        val overlapR = rect.right - pb.left
        val overlapT = pb.bottom - rect.top
        val overlapB = rect.bottom - pb.top
        if (minOf(overlapT, overlapB) <= minOf(overlapL, overlapR)) return
        if (overlapL < overlapR) { player.x -= overlapL; if (player.vx > 0f) player.vx = 0f }
        else                     { player.x += overlapR; if (player.vx < 0f) player.vx = 0f }
    }

    private fun resolveV(rect: RectF) {
        val pb = player.bounds
        if (!RectF.intersects(pb, rect)) return
        val overlapT = pb.bottom - rect.top
        val overlapB = rect.bottom - pb.top
        val overlapL = pb.right  - rect.left
        val overlapR = rect.right - pb.left
        if (minOf(overlapL, overlapR) < minOf(overlapT, overlapB)) return
        if (overlapT < overlapB) { player.y -= overlapT; player.vy = 0f; player.isOnGround = true }
        else                     { player.y += overlapB; if (player.vy < 0f) player.vy = 0f }
    }

    // -------------------------------------------------------------------------
    // Interactions

    private fun checkInteractions() {
        val pb = player.bounds

        for (spike in spikes) {
            if (RectF.intersects(pb, spike.bounds)) { player.isDead = true; return }
        }

        for (k in keys) {
            if (!k.isCollected && RectF.intersects(pb, k.bounds)) {
                k.isCollected = true
                collectedKeys.add(k.id)
                player.hasKey = true
            }
        }

        if (!button.hidden) {
            val wasPressed = button.isPressed
            button.isPressed = player.isOnGround && RectF.intersects(pb, button.bounds)
            when {
                button.isPressed && !wasPressed -> { button.pressCount++; onButtonPressed?.invoke(this) }
                !button.isPressed && wasPressed -> onButtonReleased?.invoke(this)
            }
        }

        if (!door.isOpen) doorCondition?.let { if (it(this)) door.isOpen = true }

        if (door.isOpen) {
            val win = winCondition?.invoke(this) ?: playerInsideDoor(pb)
            if (win) levelComplete = true
        }
    }


    /**
     * Переместить кнопку.
     * xr — левый край кнопки (доля ширины), yr — нижний край кнопки (доля высоты).
     * Размер кнопки сохраняется (54×24 px).
     */
    fun placeButton(xr: Float, yr: Float) {
        val bw = button.bounds.width()
        val bh = button.bounds.height()
        val x  = room.w * xr
        val y  = room.h * yr
        button.bounds.set(x, y - bh, x + bw, y)
    }

    /** Платформа: все четыре границы в долях экрана (0.0–1.0). */
    fun addPlatform(x1r: Float, y1r: Float, x2r: Float, y2r: Float) {
        platforms.add(RectF(room.w * x1r, room.h * y1r, room.w * x2r, room.h * y2r))
    }

    /** Шипы острием ВВЕРХ на полу между x1r и x2r. */
    fun addSpikesFloor(x1r: Float, x2r: Float) {
        val y = room.h - room.wallThick - 24f
        var x = room.w * x1r
        while (x + 28f <= room.w * x2r) { spikes.add(Spike(x, y, dir = SpikeDir.UP)); x += 32f }
    }

    /**
     * Шипы на горизонтальной поверхности.
     * yr — верхняя грань поверхности (платформы или пол).
     * dir = UP  → острие вверх (лежат на поверхности).
     * dir = DOWN → острие вниз (висят снизу поверхности).
     */
    fun addSpikesAt(x1r: Float, x2r: Float, yr: Float, dir: SpikeDir = SpikeDir.UP) {
        val y = when (dir) {
            SpikeDir.UP   -> room.h * yr - 24f
            SpikeDir.DOWN -> room.h * yr + room.wallThick
            else          -> room.h * yr - 24f
        }
        var x = room.w * x1r
        while (x + 28f <= room.w * x2r) { spikes.add(Spike(x, y, dir = dir)); x += 32f }
    }

    /**
     * Боковые шипы вдоль вертикальной поверхности (стена / бок платформы).
     * xr — X поверхности (доля ширины).
     * dir = LEFT  → острие влево (шипы слева от xr, торчат влево).
     * dir = RIGHT → острие вправо (шипы справа от xr, торчат вправо).
     */
    fun addSpikesWall(y1r: Float, y2r: Float, xr: Float, dir: SpikeDir = SpikeDir.LEFT) {
        val spikeW = 24f; val spikeH = 28f
        val x = if (dir == SpikeDir.LEFT) room.w * xr - spikeW else room.w * xr
        var y = room.h * y1r
        while (y + spikeH <= room.h * y2r) {
            spikes.add(Spike(x, y, spikeW, spikeH, dir)); y += spikeH + 4f
        }
    }

    /**
     * Разместить ключ.
     * @param xr    центр по X (доля ширины)
     * @param yr    верхняя грань поверхности под ключом (доля высоты)
     * @param id    уникальный ID (для openDoorWhenKey)
     * @param color цвет (Color.rgb / Color.argb)
     */
    fun placeKey(xr: Float, yr: Float, id: Int = 0, color: Int = Color.rgb(255, 215, 0)) {
        keys.add(Key(room.w * xr - Key.SIZE / 2f, room.h * yr - Key.SIZE - 6f, id, color))
    }

    // --- Условия открытия двери ------------------------------------------

    /** Дверь открывается когда собраны ВСЕ ключи уровня. */
    fun openDoorWhenAllKeys() {
        doorCondition = { e -> e.keys.isNotEmpty() && e.keys.all { it.isCollected } }
    }

    /** Дверь открывается когда собран ЛЮБОЙ ключ. */
    fun openDoorWhenAnyKey() {
        doorCondition = { e -> e.collectedKeys.isNotEmpty() }
    }

    /** Дверь открывается когда собран ключ с конкретным [id]. */
    fun openDoorWhenKey(id: Int) {
        doorCondition = { e -> e.collectedKeys.contains(id) }
    }

    // -------------------------------------------------------------------------

    fun playerInsideDoor(pb: RectF = player.bounds): Boolean =
        pb.right  >= door.bounds.left  &&
        pb.left   <  door.bounds.right &&
        pb.bottom >  door.bounds.top   &&
        pb.top    <  door.bounds.bottom

    // -------------------------------------------------------------------------

    fun reset() {
        player.reset(room.playerSpawnX, room.playerSpawnY)
        button.isPressed  = false
        button.pressCount = 0
        button.hidden     = false
        door.isOpen       = false
        keys.forEach { it.isCollected = false }
        keys.clear()
        collectedKeys.clear()
        spikes.clear()
        platforms.clear()
        levelComplete  = false
        moveLeft       = false
        moveRight      = false
        jumpRequested  = false
        jumpHeld       = false
        prevJumpHeld   = false
        jumpGraceTimer = 0f
        doorCondition  = null
        // onButtonPressed/Released, onUpdate, winCondition — не сбрасываем,
        // Level.setup() перезапишет их после reset().
    }
}
