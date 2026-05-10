package com.example.daitezachet

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.daitezachet.engine.*

class CustomizeActivity : AppCompatActivity() {

    private val BG_COLOR = Color.rgb(12, 12, 22)
    private val ACCENT = Color.rgb(0, 220, 180)
    private val ACCENT_DIM = Color.rgb(0, 130, 110)
    private val TILE_FILL = Color.argb(25, 0, 220, 180)
    private val TEXT_WHITE = Color.rgb(230, 240, 255)

    private val pixelFont: Typeface by lazy {
        try {
            ResourcesCompat.getFont(this, R.font.pixel)!!
        } catch (e: Exception) {
            Typeface.MONOSPACE
        }
    }

    private var currentAppearance = PlayerAppearance()
    private lateinit var previewView: PlayerPreviewView

    private val bodyColors = listOf(
        Color.rgb(230, 230, 255) to "белый",
        Color.rgb(0, 220, 180) to "мятный",
        Color.rgb(255, 100, 100) to "красный",
        Color.rgb(100, 180, 255) to "синий",
        Color.rgb(255, 180, 50) to "жёлтый",
        Color.rgb(180, 100, 255) to "фиолет",
        Color.rgb(100, 230, 80) to "зелёный",
        Color.rgb(255, 140, 200) to "розовый"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goFullscreen()

        currentAppearance = AppearanceStore.load(this)

        val root = FrameLayout(this).apply { setBackgroundColor(BG_COLOR) }
        val content = buildContent()
        root.addView(content)
        setContentView(root)

        content.alpha = 0f
        content.animate().alpha(1f).setDuration(300).start()
    }

    private fun goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = BG_COLOR
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // ───────────────────── Layout ─────────────────────

    private fun buildContent(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(dp(16), dp(20), dp(16), dp(20))

            // Топбар
            addView(buildTopBar())
            addView(spacer(dp(16)))

            // Основная зона: слева настройки, справа превью
            addView(buildMainZone())

            // Кнопка сохранить снизу
            addView(spacer(dp(12)))
            addView(buildSaveButton())
        }

    private fun buildTopBar(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            addView(buildNavBtn("←") { finish() })
            addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))

            addView(TextView(context).apply {
                text = "ПЕРСОНАЖ"
                typeface = pixelFont
                textSize = 18f
                setTextColor(TEXT_WHITE)
                gravity = Gravity.CENTER
                setShadowLayer(10f, 0f, 0f, Color.argb(120, 0, 220, 180))
            })

            addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            })
        }

    private fun buildMainZone(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)

            val scroll = ScrollView(context).apply {
                isFillViewport = true
                layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1.4f)

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    addView(sectionLabel("ЦВЕТ ТЕЛА"))
                    addView(spacer(dp(8)))
                    addView(buildColorRow())
                    addView(spacer(dp(16)))

                    addView(sectionLabel("КАСТОМНЫЙ ЦВЕТ"))
                    addView(spacer(dp(6)))
                    addView(buildCustomColorSection())
                    addView(spacer(dp(16)))

                    addView(sectionLabel("ГЛАЗА"))
                    addView(spacer(dp(8)))
                    addView(buildEyeRow())
                    addView(spacer(dp(16)))

                    addView(sectionLabel("ГОЛОВНОЙ УБОР"))
                    addView(spacer(dp(8)))
                    addView(buildHatRow())
                })
            }

            // Справа — превью
            previewView = PlayerPreviewView(this@CustomizeActivity)
            val previewParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply {
                setMargins(dp(12), 0, 0, 0)
            }

            addView(scroll)
            addView(previewView, previewParams)
        }

    private fun sectionLabel(text: String): TextView =
        TextView(this).apply {
            this.text = text
            typeface = pixelFont
            textSize = 11f
            setTextColor(ACCENT)
            letterSpacing = 0.08f
        }

    // ───────────── Предустановленные цвета ─────────────

    private fun buildColorRow(): HorizontalScrollView =
        HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                bodyColors.forEach { (color, _) ->
                    addView(buildColorSwatch(color))
                }
            })
        }

    private fun buildColorSwatch(color: Int): View {
        val size = dp(40)
        val margin = dp(6)

        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 0, margin, 0)
            }

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke(
                    if (currentAppearance.bodyColor == color) dp(3) else dp(1),
                    if (currentAppearance.bodyColor == color) ACCENT else Color.argb(80, 200, 200, 200)
                )
            }

            tag = color

            setOnClickListener {
                currentAppearance = currentAppearance.copy(bodyColor = color)
                AppearanceStore.save(this@CustomizeActivity, currentAppearance)
                refreshSwatches(parent as LinearLayout, color)
                previewView.invalidate()
            }
        }
    }

    private fun refreshSwatches(row: LinearLayout, selectedColor: Int) {
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i) ?: continue
            val color = child.tag as? Int ?: continue
            (child.background as? GradientDrawable)?.setStroke(
                if (color == selectedColor) dp(3) else dp(1),
                if (color == selectedColor) ACCENT else Color.argb(80, 200, 200, 200)
            )
        }
    }

    // ───────────── Кастомный цвет (RGB) ─────────────

    private fun buildCustomColorSection(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            val previewBox = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(28)).apply {
                    bottomMargin = dp(6)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(4).toFloat()
                    setColor(currentAppearance.bodyColor)
                    setStroke(dp(1), ACCENT_DIM)
                }
            }

            val (initR, initG, initB) = Triple(
                Color.red(currentAppearance.bodyColor),
                Color.green(currentAppearance.bodyColor),
                Color.blue(currentAppearance.bodyColor)
            )

            val seekR = buildColorSeekBar("R", initR, Color.RED) { v ->
                updateCustomColor(previewBox, v, null, null)
            }
            val seekG = buildColorSeekBar("G", initG, Color.GREEN) { v ->
                updateCustomColor(previewBox, null, v, null)
            }
            val seekB = buildColorSeekBar("B", initB, Color.BLUE) { v ->
                updateCustomColor(previewBox, null, null, v)
            }

            addView(previewBox)
            addView(seekR)
            addView(seekG)
            addView(seekB)

            setOnLongClickListener {
                // долгий тап по блоку — применить текущий кастомный цвет
                AppearanceStore.save(this@CustomizeActivity, currentAppearance)
                Toast.makeText(context, "Кастомный цвет применён", Toast.LENGTH_SHORT).show()
                true
            }
        }

    private var tmpR = -1
    private var tmpG = -1
    private var tmpB = -1

    private fun updateCustomColor(previewBox: View, rVal: Int?, gVal: Int?, bVal: Int?) {
        if (tmpR == -1) tmpR = Color.red(currentAppearance.bodyColor)
        if (tmpG == -1) tmpG = Color.green(currentAppearance.bodyColor)
        if (tmpB == -1) tmpB = Color.blue(currentAppearance.bodyColor)

        if (rVal != null) tmpR = rVal
        if (gVal != null) tmpG = gVal
        if (bVal != null) tmpB = bVal

        val newColor = Color.rgb(tmpR, tmpG, tmpB)
        currentAppearance = currentAppearance.copy(bodyColor = newColor)
        (previewBox.background as? GradientDrawable)?.setColor(newColor)

        // чтобы рамки пресетов сбросились как "неселект"
        previewView.invalidate()
    }

    private fun buildColorSeekBar(
        label: String,
        initial: Int,
        tint: Int,
        onChange: (Int) -> Unit
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            val tv = TextView(context).apply {
                text = "$label: $initial"
                typeface = pixelFont
                textSize = 10f
                setTextColor(TEXT_WHITE)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }

            val seek = SeekBar(context).apply {
                max = 255
                progress = initial
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(8)
                }
                progressDrawable.setTint(tint)
                thumb.setTint(tint)

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                        tv.text = "$label: $value"
                        onChange(value)
                    }

                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {
                        // при отпускании ползунка — сохраняем
                        AppearanceStore.save(this@CustomizeActivity, currentAppearance)
                    }
                })
            }

            addView(tv)
            addView(seek)
        }

    // ───────────── Глаза и шляпа ─────────────

    private fun buildEyeRow(): HorizontalScrollView =
        HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val styles = listOf(
                    EyeStyle.NORMAL to "обычные",
                    EyeStyle.ANGRY to "злые",
                    EyeStyle.SLEEPY to "сонные",
                    EyeStyle.DEAD to "x-глаза"
                )

                styles.forEachIndexed { index, (style, label) ->
                    addView(buildOptionChip(label, currentAppearance.eyeStyle == style) {
                        currentAppearance = currentAppearance.copy(eyeStyle = style)
                        AppearanceStore.save(this@CustomizeActivity, currentAppearance)
                        refreshChips(this, index)
                        previewView.invalidate()
                    })
                }
            })
        }

    private fun buildHatRow(): HorizontalScrollView =
        HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val hats = listOf(
                    Hat.NONE to "нет",
                    Hat.CAP to "кепка",
                    Hat.CROWN to "корона",
                    Hat.WIZARD to "маг",
                    Hat.TOP_HAT to "цилиндр"
                )

                hats.forEachIndexed { index, (hat, label) ->
                    addView(buildOptionChip(label, currentAppearance.hat == hat) {
                        currentAppearance = currentAppearance.copy(hat = hat)
                        AppearanceStore.save(this@CustomizeActivity, currentAppearance)
                        refreshChips(this, index)
                        previewView.invalidate()
                    })
                }
            })
        }

    private fun buildOptionChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ): TextView =
        TextView(this).apply {
            text = label
            typeface = pixelFont
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(if (selected) BG_COLOR else ACCENT)

            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, dp(34)).apply {
                setMargins(0, 0, dp(8), 0)
            }
            setPadding(dp(12), 0, dp(12), 0)

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(6).toFloat()
                setColor(if (selected) ACCENT else TILE_FILL)
                setStroke(dp(1), ACCENT_DIM)
            }

            setOnClickListener { onClick() }
        }

    private fun refreshChips(row: LinearLayout, selectedIdx: Int) {
        for (i in 0 until row.childCount) {
            val chip = row.getChildAt(i) as? TextView ?: continue
            val selected = i == selectedIdx
            chip.setTextColor(if (selected) BG_COLOR else ACCENT)
            (chip.background as? GradientDrawable)?.setColor(if (selected) ACCENT else TILE_FILL)
        }
    }

    // ───────────── Кнопка сохранить ─────────────

    private fun buildSaveButton(): TextView =
        TextView(this).apply {
            text = "СОХРАНИТЬ"
            typeface = pixelFont
            textSize = 13f
            setTextColor(BG_COLOR)
            gravity = Gravity.CENTER
            letterSpacing = 0.06f
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52))

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(4).toFloat()
                setColor(ACCENT)
            }

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(60).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
                false
            }

            setOnClickListener {
                AppearanceStore.save(this@CustomizeActivity, currentAppearance)
                finish()
            }
        }

    private fun buildNavBtn(label: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            typeface = Typeface.DEFAULT_BOLD
            textSize = 20f
            setTextColor(ACCENT)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(4).toFloat()
                setColor(TILE_FILL)
                setStroke(dp(1), ACCENT_DIM)
            }

            setOnClickListener { onClick() }
        }

    private fun spacer(h: Int): View =
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, h)
        }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density + 0.5f).toInt()

    // ───────────── Превью справа ─────────────

    inner class PlayerPreviewView(context: Context) : View(context) {
        private val bgPaint = Paint().apply {
            color = Color.argb(60, 0, 220, 180)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val borderPaint = Paint().apply {
            color = ACCENT_DIM
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, 16f, 16f, bgPaint)
            canvas.drawRoundRect(rect, 16f, 16f, borderPaint)

            val pw = width * 0.30f   // было 0.34f
            val ph = height * 0.44f  // было 0.50f
            val px = (width - pw) / 2f
            val py = (height - ph) / 2f + dp(20)

            drawPreviewPlayer(canvas, RectF(px, py, px + pw, py + ph))
        }

        private fun drawPreviewPlayer(canvas: Canvas, pb: RectF) {
            val app = currentAppearance

            val bodyPaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
                color = app.bodyColor
            }

            val eyePaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
                color = Color.rgb(18, 18, 28)
            }

            val hatPaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            canvas.drawRect(pb, bodyPaint)

            val eyeY = pb.top + pb.height() * 0.30f
            val lx = pb.left + pb.width() * 0.27f
            val rx = pb.right - pb.width() * 0.27f
            val eyeR = pb.width() * 0.07f

            when (app.eyeStyle) {
                EyeStyle.NORMAL -> {
                    canvas.drawCircle(lx, eyeY, eyeR, eyePaint)
                    canvas.drawCircle(rx, eyeY, eyeR, eyePaint)
                }

                EyeStyle.ANGRY -> {
                    eyePaint.style = Paint.Style.STROKE
                    eyePaint.strokeWidth = eyeR * 0.8f
                    canvas.drawLine(lx - eyeR, eyeY, lx + eyeR, eyeY, eyePaint)
                    canvas.drawLine(rx - eyeR, eyeY, rx + eyeR, eyeY, eyePaint)
                    canvas.drawLine(lx - eyeR, eyeY - eyeR * 1.6f, lx + eyeR, eyeY - eyeR, eyePaint)
                    canvas.drawLine(rx - eyeR, eyeY - eyeR, rx + eyeR, eyeY - eyeR * 1.6f, eyePaint)
                    eyePaint.style = Paint.Style.FILL
                }

                EyeStyle.SLEEPY -> {
                    eyePaint.style = Paint.Style.STROKE
                    eyePaint.strokeWidth = eyeR * 0.7f
                    canvas.drawArc(RectF(lx - eyeR, eyeY - eyeR, lx + eyeR, eyeY + eyeR), 0f, 180f, false, eyePaint)
                    canvas.drawArc(RectF(rx - eyeR, eyeY - eyeR, rx + eyeR, eyeY + eyeR), 0f, 180f, false, eyePaint)
                    eyePaint.style = Paint.Style.FILL
                }

                EyeStyle.DEAD -> {
                    eyePaint.style = Paint.Style.STROKE
                    eyePaint.strokeWidth = eyeR * 0.7f
                    canvas.drawLine(lx - eyeR, eyeY - eyeR, lx + eyeR, eyeY + eyeR, eyePaint)
                    canvas.drawLine(lx + eyeR, eyeY - eyeR, lx - eyeR, eyeY + eyeR, eyePaint)
                    canvas.drawLine(rx - eyeR, eyeY - eyeR, rx + eyeR, eyeY + eyeR, eyePaint)
                    canvas.drawLine(rx + eyeR, eyeY - eyeR, rx - eyeR, eyeY + eyeR, eyePaint)
                    eyePaint.style = Paint.Style.FILL
                }
            }

            val cx = pb.centerX()
            val top = pb.top
            val hw = pb.width()

            when (app.hat) {
                Hat.NONE -> Unit

                Hat.CAP -> {
                    hatPaint.color = Color.rgb(30, 30, 180)
                    canvas.drawRect(cx - hw * 0.45f, top - hw * 0.22f, cx + hw * 0.45f, top, hatPaint)

                    hatPaint.color = Color.rgb(20, 20, 140)
                    canvas.drawRect(cx - hw * 0.55f, top - hw * 0.10f, cx + hw * 0.15f, top, hatPaint)
                }

                Hat.CROWN -> {
                    hatPaint.color = Color.rgb(255, 200, 0)
                    canvas.drawRect(cx - hw * 0.42f, top - hw * 0.18f, cx + hw * 0.42f, top, hatPaint)

                    path.rewind()
                    val bL = cx - hw * 0.42f
                    val bR = cx + hw * 0.42f
                    val bY = top - hw * 0.18f
                    val peakY = top - hw * 0.42f

                    path.moveTo(bL, bY)
                    path.lineTo(bL + hw * 0.18f, peakY)
                    path.lineTo(bL + hw * 0.34f, bY)

                    path.moveTo(cx - hw * 0.18f, bY)
                    path.lineTo(cx, peakY - hw * 0.04f)
                    path.lineTo(cx + hw * 0.18f, bY)

                    path.moveTo(bR - hw * 0.34f, bY)
                    path.lineTo(bR - hw * 0.18f, peakY)
                    path.lineTo(bR, bY)

                    path.close()
                    canvas.drawPath(path, hatPaint)
                }

                Hat.WIZARD -> {
                    hatPaint.color = Color.rgb(90, 20, 160)
                    canvas.drawRect(cx - hw * 0.50f, top - hw * 0.12f, cx + hw * 0.50f, top, hatPaint)

                    path.rewind()
                    path.moveTo(cx - hw * 0.35f, top - hw * 0.12f)
                    path.lineTo(cx, top - hw * 0.70f)
                    path.lineTo(cx + hw * 0.35f, top - hw * 0.12f)
                    path.close()
                    canvas.drawPath(path, hatPaint)

                    hatPaint.color = Color.rgb(255, 230, 0)
                    canvas.drawCircle(cx, top - hw * 0.48f, hw * 0.07f, hatPaint)
                }

                Hat.TOP_HAT -> {
                    hatPaint.color = Color.rgb(20, 20, 20)
                    canvas.drawRect(cx - hw * 0.55f, top - hw * 0.12f, cx + hw * 0.55f, top, hatPaint)
                    canvas.drawRect(cx - hw * 0.34f, top - hw * 0.58f, cx + hw * 0.34f, top - hw * 0.12f, hatPaint)

                    hatPaint.color = Color.rgb(200, 200, 200)
                    canvas.drawRect(cx - hw * 0.34f, top - hw * 0.28f, cx + hw * 0.34f, top - hw * 0.20f, hatPaint)
                }
            }
        }
    }
}