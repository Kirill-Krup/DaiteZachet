package com.example.daitezachet.engine

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.daitezachet.AppearanceStore
import com.example.daitezachet.levels.Level
import com.example.daitezachet.levels.LevelRegistry
import com.example.daitezachet.records.RecordsStore

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    @Volatile private var running = false

    private lateinit var playerRenderer: PlayerRenderer
    private var levelStartMs = 0L
    private var currentLevelDeaths = 0
    private var resultSavedForThisRun = false
    private var screenW = 0f
    private var screenH = 0f
    private var gameH   = 0f
    private var finishedLevelTimeMs = -1L
    private lateinit var room:   Room
    private lateinit var engine: GameEngine
    private lateinit var input:  InputHandler
    private lateinit var level:  Level

    var startLevelNumber = 1
    private var levelNumber = 1

    // Overlay timers
    private var deathTimer = 0f;  private var showDeath = false
    private var winTimer   = 0f;  private var showWin   = false
    private val DEATH_DELAY = 1.0f
    private val WIN_DELAY   = 1.4f

    var onGameComplete: (() -> Unit)? = null
    var onExitLevel:    (() -> Unit)? = null

    // Exit button bounds — top-left corner, initialised in surfaceChanged
    private val btnExit = android.graphics.RectF()

    // -------------------------------------------------------------------------
    // Paints — allocated once

    private val wallPaint = Paint().apply {
        color = Color.rgb(230, 230, 230); isAntiAlias = false
    }
    private val doorClosedPaint = Paint().apply {
        color = Color.rgb(200, 150, 30); style = Paint.Style.FILL; isAntiAlias = true
    }
    private val doorOpenPaint = Paint().apply {
        color = Color.rgb(60, 220, 80); style = Paint.Style.STROKE
        strokeWidth = 5f; isAntiAlias = true
    }
    private val buttonPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val keyPaint    = Paint().apply {
        color = Color.rgb(255, 215, 0); style = Paint.Style.FILL; isAntiAlias = true
    }
    private val keyCollectedPaint = Paint().apply {
        color = Color.rgb(255, 215, 0); style = Paint.Style.FILL; isAntiAlias = true; alpha = 160
    }
    private val spikePaint = Paint().apply {
        color = Color.rgb(220, 50, 50); style = Paint.Style.FILL; isAntiAlias = true
    }
    private val playerPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val hudPaint    = Paint().apply {
        color = Color.WHITE; textSize = 52f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD
    }
    private val hintPaint = Paint().apply {
        color = Color.rgb(170, 170, 170); textSize = 38f; isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val exitPaint = Paint().apply {
        color = Color.argb(180, 220, 60, 60); style = Paint.Style.FILL; isAntiAlias = true
    }
    private val exitTextPaint = Paint().apply {
        color = Color.WHITE; textSize = 44f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val ctrlFillPaint = Paint().apply {
        style = Paint.Style.FILL; isAntiAlias = true
    }
    private val ctrlStrokePaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 3.5f
        color = Color.argb(180, 255, 255, 255); isAntiAlias = true
    }
    private val ctrlGlowPaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 8f; isAntiAlias = true
    }
    private val ctrlTextPaint = Paint().apply {
        color = Color.argb(240, 255, 255, 255); textSize = 58f
        isAntiAlias = true; textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val ctrlArrowPaint = Paint().apply {
        style = Paint.Style.FILL; isAntiAlias = true
    }
    private val overlayPaint = Paint()
    private val spikePathBuf = Path()

    // -------------------------------------------------------------------------
    // SurfaceHolder.Callback

    init { holder.addCallback(this) }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenW = width.toFloat()
        screenH = height.toFloat()
        gameH   = screenH * 0.78f
        btnExit.set(screenW - 110f, 8f, screenW - 8f, 78f)
        setupLevel(startLevelNumber)
        resume()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) { pause() }

    // -------------------------------------------------------------------------
    // Level setup

    private fun setupLevel(number: Int) {
        levelNumber = number
        level = LevelRegistry.get(number)
        room = Room(screenW, gameH)
        engine = GameEngine(room)
        playerRenderer = PlayerRenderer(AppearanceStore.load(context))
        input = InputHandler(screenW, screenH, gameH)

        levelStartMs = android.os.SystemClock.elapsedRealtime()
        currentLevelDeaths = 0
        resultSavedForThisRun = false
        finishedLevelTimeMs = -1L
        level.setup(engine)
        showDeath = false
        deathTimer = 0f
        showWin = false
        winTimer = 0f
    }


    fun resume() {
        if (running) return
        running = true
        thread = Thread(this, "GameThread").also { it.start() }
    }

    fun pause() {
        running = false
        try { thread?.join(600) } catch (_: InterruptedException) {}
        thread = null
    }

    // -------------------------------------------------------------------------
    // Game loop

    override fun run() {
        var lastNs = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt  = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastNs  = now

            update(dt)

            val canvas = holder.lockCanvas() ?: continue
            try { drawFrame(canvas) } finally { holder.unlockCanvasAndPost(canvas) }

            val frameMs = (System.nanoTime() - now) / 1_000_000L
            val sleepMs = 16L - frameMs
            if (sleepMs > 0) Thread.sleep(sleepMs)
        }
    }

    private fun update(dt: Float) {
        when {
            showDeath -> {
                deathTimer += dt
                if (deathTimer >= DEATH_DELAY) {
                    engine.reset()
                    level.setup(engine)
                    showDeath = false; deathTimer = 0f
                }
            }
            showWin -> {
                winTimer += dt
                if (winTimer >= WIN_DELAY) {
                    showWin = false; winTimer = 0f
                    val next = levelNumber + 1
                    if (next <= LevelRegistry.count) setupLevel(next)
                    else post { onGameComplete?.invoke() }
                }
            }
            else -> {
                engine.update(dt)

                if (engine.player.isDead && !showDeath) {
                    showDeath = true
                    deathTimer = 0f
                    currentLevelDeaths++
                    RecordsStore.addDeath(context, levelNumber)
                }

                if (engine.levelComplete && !showWin) {
                    showWin = true
                    winTimer = 0f

                    if (finishedLevelTimeMs < 0L) {
                        finishedLevelTimeMs = android.os.SystemClock.elapsedRealtime() - levelStartMs
                    }

                    if (!resultSavedForThisRun) {
                        RecordsStore.saveCompletion(context, levelNumber, finishedLevelTimeMs)
                        resultSavedForThisRun = true
                    }
                }
            }
        }
    }

    private fun drawFrame(canvas: Canvas) {
        canvas.drawColor(Color.rgb(18, 18, 28))

        drawRoom(canvas)
        drawDoor(canvas)
        drawButton(canvas)
        drawKey(canvas)
        drawSpikes(canvas)
        drawPlayer(canvas)
        drawHud(canvas)
        drawExitButton(canvas)
        drawControls(canvas)

        level.draw(canvas, engine, hudPaint)

        if (showDeath) drawDeathOverlay(canvas)
        if (showWin)   drawWinOverlay(canvas)
    }

    private fun drawRoom(canvas: Canvas) {
        engine.room.staticSolids.forEach { canvas.drawRect(it, wallPaint) }
        engine.platforms.forEach { canvas.drawRect(it, wallPaint) }
    }

    private fun drawDoor(canvas: Canvas) {
        val d = engine.door.bounds
        if (engine.door.isOpen && engine.door.visuallyOpen) {
            canvas.drawRect(d, doorOpenPaint)
        } else {
            canvas.drawRect(d, doorClosedPaint)
            val cx = d.centerX(); val cy = d.centerY()
            overlayPaint.color = Color.rgb(100, 60, 0); overlayPaint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy - 12f, 14f, overlayPaint)
            overlayPaint.color = Color.rgb(100, 60, 0)
            canvas.drawRect(cx - 8f, cy - 8f, cx + 8f, cy + 20f, overlayPaint)
            overlayPaint.color = Color.rgb(60, 30, 0)
            overlayPaint.style = Paint.Style.STROKE; overlayPaint.strokeWidth = 4f; overlayPaint.isAntiAlias = true
            canvas.drawCircle(cx, cy - 12f, 14f, overlayPaint)
        }
    }

    private fun drawButton(canvas: Canvas) {
        if (engine.button.hidden) return
        val b = engine.button.bounds
        buttonPaint.color = if (engine.button.isPressed)
            Color.rgb(160, 20, 20) else Color.rgb(230, 50, 50)
        // Slightly raised appearance when not pressed
        if (!engine.button.isPressed) {
            buttonPaint.color = Color.rgb(180, 30, 30)
            canvas.drawRoundRect(b.left - 3f, b.top - 6f, b.right + 3f, b.bottom, 6f, 6f, buttonPaint)
        }
        buttonPaint.color = if (engine.button.isPressed)
            Color.rgb(160, 20, 20) else Color.rgb(230, 50, 50)
        canvas.drawRoundRect(b, 6f, 6f, buttonPaint)
    }

    private fun drawKey(canvas: Canvas) {
        for (k in engine.keys) {
            keyPaint.color = if (k.isCollected) Color.argb(100,
                Color.red(k.color), Color.green(k.color), Color.blue(k.color))
            else k.color
            drawDiamond(canvas, k.bounds, keyPaint)
            if (!k.isCollected) {
                overlayPaint.color = Color.argb(200, 255, 255, 200)
                overlayPaint.style = Paint.Style.FILL
                canvas.drawCircle(k.bounds.left + 8f, k.bounds.top + 8f, 4f, overlayPaint)
            }
        }
    }

    private fun drawSpikes(canvas: Canvas) {
        for (spike in engine.spikes) {
            spikePathBuf.rewind()
            val b = spike.bounds
            when (spike.dir) {
                SpikeDir.UP -> {
                    spikePathBuf.moveTo(b.centerX(), b.top)
                    spikePathBuf.lineTo(b.right, b.bottom)
                    spikePathBuf.lineTo(b.left,  b.bottom)
                }
                SpikeDir.DOWN -> {
                    spikePathBuf.moveTo(b.centerX(), b.bottom)
                    spikePathBuf.lineTo(b.right, b.top)
                    spikePathBuf.lineTo(b.left,  b.top)
                }
                SpikeDir.LEFT -> {
                    spikePathBuf.moveTo(b.left,  b.centerY())
                    spikePathBuf.lineTo(b.right, b.top)
                    spikePathBuf.lineTo(b.right, b.bottom)
                }
                SpikeDir.RIGHT -> {
                    spikePathBuf.moveTo(b.right, b.centerY())
                    spikePathBuf.lineTo(b.left,  b.top)
                    spikePathBuf.lineTo(b.left,  b.bottom)
                }
            }
            spikePathBuf.close()
            canvas.drawPath(spikePathBuf, spikePaint)
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        playerRenderer.draw(canvas, engine.player, keyPaint) { c, bounds, paint ->
            drawDiamond(c, bounds, paint)
        }
    }

    private fun drawHud(canvas: Canvas) {
        overlayPaint.color = Color.argb(180, 10, 10, 18)
        overlayPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, screenW, 90f, overlayPaint)

        hudPaint.textAlign = Paint.Align.LEFT
        hudPaint.textSize  = 48f
        canvas.drawText("Уровень $levelNumber", 24f, 62f, hudPaint)

        hintPaint.alpha = 200
        canvas.drawText(level.hintText, screenW / 2f, 66f, hintPaint)


        val currentTimeMs = if (finishedLevelTimeMs >= 0L) {
            finishedLevelTimeMs
        } else {
            (android.os.SystemClock.elapsedRealtime() - levelStartMs).coerceAtLeast(0L)
        }
        val timeStr = formatTimeMs(currentTimeMs)
        val deathsStr = currentLevelDeaths.toString()

        hudPaint.textSize = 32f
        hudPaint.textAlign = Paint.Align.RIGHT
        hudPaint.color = Color.rgb(230, 230, 230)

        val statsRight = btnExit.left - 24f
        canvas.drawText("Время: $timeStr", statsRight, 42f, hudPaint)
        canvas.drawText("Смерти: $deathsStr", statsRight, 72f, hudPaint)

        hudPaint.color = Color.WHITE
    }

    private fun drawExitButton(canvas: Canvas) {
        canvas.drawRoundRect(btnExit, 12f, 12f, exitPaint)
        canvas.drawText("✕", btnExit.centerX(), btnExit.centerY() + 16f, exitTextPaint)
    }

    private fun drawControls(canvas: Canvas) {
        overlayPaint.color = Color.argb(220, 8, 8, 16)
        overlayPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, gameH, screenW, screenH, overlayPaint)

        overlayPaint.color = Color.argb(100, 100, 160, 255)
        canvas.drawRect(0f, gameH, screenW, gameH + 1.5f, overlayPaint)
        overlayPaint.color = Color.argb(40, 100, 160, 255)
        canvas.drawRect(0f, gameH + 1.5f, screenW, gameH + 5f, overlayPaint)

        drawCtrlButton(canvas, input.btnLeft,  "←", isMove = true)
        drawCtrlButton(canvas, input.btnRight, "→", isMove = true)
        drawCtrlButton(canvas, input.btnJump,  "↑", isMove = false)
    }

    private fun drawCtrlButton(canvas: Canvas, btn: android.graphics.RectF, label: String, isMove: Boolean) {
        val r = 18f
        val cx = btn.centerX()
        val cy = btn.centerY()

        val gradient = android.graphics.LinearGradient(
            cx, btn.top, cx, btn.bottom,
            intArrayOf(
                Color.argb(130, 40, 60, 120),
                Color.argb(180, 15, 20, 50)
            ),
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        ctrlFillPaint.shader = gradient
        canvas.drawRoundRect(btn, r, r, ctrlFillPaint)
        ctrlFillPaint.shader = null

        ctrlGlowPaint.color = if (isMove)
            Color.argb(60, 80, 140, 255)
        else
            Color.argb(80, 120, 80, 255)
        canvas.drawRoundRect(
            android.graphics.RectF(btn.left - 2f, btn.top - 2f, btn.right + 2f, btn.bottom + 2f),
            r + 2f, r + 2f, ctrlGlowPaint
        )

        ctrlStrokePaint.color = if (isMove)
            Color.argb(160, 100, 160, 255)
        else
            Color.argb(180, 160, 120, 255)
        canvas.drawRoundRect(btn, r, r, ctrlStrokePaint)

        val shineRect = android.graphics.RectF(
            btn.left + 10f, btn.top + 6f,
            btn.right - 10f, btn.top + 14f
        )
        overlayPaint.color = Color.argb(50, 255, 255, 255)
        overlayPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(shineRect, 6f, 6f, overlayPaint)

        ctrlTextPaint.color = if (isMove)
            Color.argb(230, 160, 210, 255)
        else
            Color.argb(230, 210, 180, 255)
        canvas.drawText(label, cx, cy + 22f, ctrlTextPaint)
    }

    private fun drawDeathOverlay(canvas: Canvas) {
        overlayPaint.color = Color.argb(170, 180, 0, 0)
        overlayPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, screenW, screenH, overlayPaint)
        overlayPaint.color      = Color.WHITE
        overlayPaint.textSize   = 82f
        overlayPaint.textAlign  = Paint.Align.CENTER
        overlayPaint.isAntiAlias = true
        canvas.drawText("Умер!", screenW / 2f, screenH / 2f, overlayPaint)
    }

    private fun drawWinOverlay(canvas: Canvas) {
        val shader = android.graphics.LinearGradient(
            0f, 0f, 0f, screenH,
            intArrayOf(
                Color.argb(210, 0, 140, 40),
                Color.argb(190, 0, 80, 30),
                Color.argb(210, 0, 140, 40)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        overlayPaint.shader = shader
        overlayPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, screenW, screenH, overlayPaint)
        overlayPaint.shader = null

        val cardW = screenW * 0.74f
        val cardH = screenH * 0.26f
        val cx = screenW / 2f
        val cy = screenH / 2f
        val left = cx - cardW / 2f
        val top = cy - cardH / 2f
        val right = cx + cardW / 2f
        val bottom = cy + cardH / 2f

        overlayPaint.color = Color.argb(220, 10, 22, 28)
        overlayPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(left, top, right, bottom, 24f, 24f, overlayPaint)

        overlayPaint.style = Paint.Style.STROKE
        overlayPaint.strokeWidth = 3f
        overlayPaint.color = Color.argb(230, 0, 220, 180)
        canvas.drawRoundRect(left + 2f, top + 2f, right - 2f, bottom - 2f, 22f, 22f, overlayPaint)

        val shineRect = android.graphics.RectF(
            left + 12f, top + 10f,
            right - 12f, top + cardH * 0.35f
        )
        val shineShader = android.graphics.LinearGradient(
            shineRect.left, shineRect.top, shineRect.left, shineRect.bottom,
            intArrayOf(
                Color.argb(70, 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        overlayPaint.shader = shineShader
        overlayPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(shineRect, 18f, 18f, overlayPaint)
        overlayPaint.shader = null

        val t = (winTimer / WIN_DELAY).coerceIn(0f, 1f)
        val scale = 1f + 0.06f * kotlin.math.sin(t * kotlin.math.PI).toFloat()

        canvas.save()
        canvas.scale(scale, scale, cx, cy)

        overlayPaint.color = Color.WHITE
        overlayPaint.textSize = 64f
        overlayPaint.textAlign = android.graphics.Paint.Align.CENTER
        overlayPaint.isAntiAlias = true
        canvas.drawText("УРОВЕНЬ ПРОЙДЕН!", cx, cy - 6f, overlayPaint)

        overlayPaint.textSize = 34f
        overlayPaint.color = Color.argb(230, 190, 255, 210)
        canvas.drawText("отличная работа", cx, cy + 40f, overlayPaint)

        canvas.restore()
    }


    private fun drawDiamond(canvas: Canvas, bounds: RectF, paint: Paint) {
        spikePathBuf.rewind()
        spikePathBuf.moveTo(bounds.centerX(), bounds.top)
        spikePathBuf.lineTo(bounds.right,     bounds.centerY())
        spikePathBuf.lineTo(bounds.centerX(), bounds.bottom)
        spikePathBuf.lineTo(bounds.left,      bounds.centerY())
        spikePathBuf.close()
        canvas.drawPath(spikePathBuf, paint)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!::input.isInitialized) return true

        if (event.actionMasked == MotionEvent.ACTION_DOWN ||
            event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            val idx = event.actionIndex
            if (btnExit.contains(event.getX(idx), event.getY(idx))) {
                post { onExitLevel?.invoke() }
                return true
            }
        }

        if (showDeath || showWin) return true
        return input.onTouch(event, engine)
    }

    private fun formatTimeMs(ms: Long): String {
        if (ms <= 0L || ms == Long.MAX_VALUE) return "—"
        val totalSec = ms / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        val millis  = ms % 1000 / 10
        return String.format("%d:%02d.%02d", minutes, seconds, millis)
    }
}
