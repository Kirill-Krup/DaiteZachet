package com.example.daitezachet

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.daitezachet.levels.LevelRegistry
import kotlin.math.ceil

class LevelSelectActivity : AppCompatActivity() {

    private val BG_COLOR    = Color.rgb(12, 12, 22)
    private val ACCENT      = Color.rgb(0, 220, 180)
    private val ACCENT_DIM  = Color.rgb(0, 130, 110)
    private val ACCENT_DARK = Color.rgb(0, 45, 38)
    private val TILE_FILL   = Color.argb(25, 0, 220, 180)
    private val TEXT_WHITE  = Color.rgb(230, 240, 255)
    private val TEXT_MUTED  = Color.rgb(100, 115, 145)

    private val levelsPerPage = 5
    private var currentPage = 0

    private lateinit var pageHost: FrameLayout
    private lateinit var pageText: TextView
    private lateinit var prevBtn: TextView
    private lateinit var nextBtn: TextView

    private val pixelFont: Typeface by lazy {
        try { ResourcesCompat.getFont(this, R.font.pixel)!! }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goFullscreen()

        val root = FrameLayout(this).apply { setBackgroundColor(BG_COLOR) }
        root.addView(GridOverlayView(this), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val content = buildContent()
        root.addView(content)
        setContentView(root)

        content.alpha = 0f
        content.translationY = 24f
        content.animate()
            .alpha(1f).translationY(0f).setDuration(350)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        updatePage(false, 0)
    }

    private fun goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = BG_COLOR
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun buildContent(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setPadding(dp(16), dp(20), dp(16), dp(20))

        addView(buildTopBar())
        addView(buildSpacer(dp(18)))
        addView(buildTitle())
        addView(buildSpacer(dp(8)))
        addView(buildSubtitle())
        addView(buildSpacer(dp(18)))
        addView(buildDivider())
        addView(buildSpacer(dp(24)))
        addView(buildPageArea(), LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        addView(buildSpacer(dp(12)))
        addView(buildPagerBar())
        addView(buildSpacer(dp(8)))
    }

    private fun buildTopBar(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        addView(buildBackButton())
        addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
        addView(TextView(context).apply {
            text = "${LevelRegistry.count} уровней"
            typeface = Typeface.MONOSPACE
            textSize = 10f
            setTextColor(TEXT_MUTED)
            gravity = Gravity.END
            letterSpacing = 0.04f
        })
    }

    private fun buildTitle(): TextView = TextView(this).apply {
        text = "ВЫБОР УРОВНЯ"
        typeface = pixelFont
        textSize = 22f
        setTextColor(TEXT_WHITE)
        gravity = Gravity.CENTER
        textAlignment = View.TEXT_ALIGNMENT_CENTER
        includeFontPadding = false
        letterSpacing = 0.05f
        setShadowLayer(14f, 0f, 0f, Color.argb(150, 0, 220, 180))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    private fun buildSubtitle(): TextView = TextView(this).apply {
        text = "выбери страдание по вкусу"
        typeface = Typeface.MONOSPACE
        textSize = 11f
        setTextColor(TEXT_MUTED)
        gravity = Gravity.CENTER
        textAlignment = View.TEXT_ALIGNMENT_CENTER
        includeFontPadding = false
        letterSpacing = 0.04f
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    private fun buildDivider(): View = View(this).apply {
        background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.TRANSPARENT, ACCENT, Color.TRANSPARENT)
        )
        layoutParams = LinearLayout.LayoutParams(dp(220), dp(1)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    private fun buildPageArea(): FrameLayout {
        pageHost = FrameLayout(this)
        // layoutParams задаётся снаружи через addView(..., LayoutParams(MATCH_PARENT, 0, 1f))
        return pageHost
    }

    // ↓ КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: LinearLayout.LayoutParams, не FrameLayout!
    private fun buildPagerBar(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        prevBtn = buildNavButton("← НАЗАД") {
            if (currentPage > 0) { currentPage--; updatePage(true, -1) }
        }
        addView(prevBtn)

        addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))

        pageText = TextView(context).apply {
            typeface = pixelFont
            textSize = 11f
            setTextColor(TEXT_WHITE)
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            letterSpacing = 0.05f
        }
        addView(pageText)

        addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))

        nextBtn = buildNavButton("ВПЕРЁД →") {
            if (currentPage < pageCount() - 1) { currentPage++; updatePage(true, 1) }
        }
        addView(nextBtn)
    }

    private fun buildBackButton(): TextView = buildNavButton("←") { finish() }

    private fun buildNavButton(label: String, onClick: () -> Unit): TextView {
        val bw = dp(2)
        val wide = label.length > 1

        fun makeBg(fill: Int): Drawable {
            val stroke = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(4).toFloat()
                setColor(ACCENT_DIM)
            }
            val inner = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(3).toFloat()
                setColor(fill)
            }
            return LayerDrawable(arrayOf(stroke, InsetDrawable(inner, bw)))
        }

        val states = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), makeBg(ACCENT_DARK))
            addState(intArrayOf(), makeBg(TILE_FILL))
        }

        return TextView(this).apply {
            text = label
            typeface = pixelFont
            textSize = if (wide) 11f else 20f
            setTextColor(ACCENT)
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            letterSpacing = if (wide) 0.04f else 0f
            background = states
            setShadowLayer(8f, 0f, 0f, Color.argb(120, 0, 220, 180))
            layoutParams = LinearLayout.LayoutParams(
                if (wide) dp(110) else dp(46),
                dp(42)
            )
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(70).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                false
            }
            setOnClickListener { onClick() }
        }
    }

    private fun buildPageGrid(startLevel: Int, endLevel: Int): LinearLayout {
        val availableWidth = resources.displayMetrics.widthPixels - dp(32)
        val margin = dp(8)
        // cellSize ограничен шириной, но строки займут высоту по weight — не по cellSize
        val cellSize = ((availableWidth - margin * 2 * 3) / 3).coerceAtMost(dp(100))
        val levels = (startLevel..endLevel).toList()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER)

            levels.chunked(3).forEach { rowLevels ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)

                    rowLevels.forEach { level ->
                        addView(buildLevelTile(level,
                            LinearLayout.LayoutParams(cellSize, cellSize).apply {
                                setMargins(margin, margin, margin, margin)
                            }
                        ))
                    }
                })
            }
        }
    }

    private fun buildRow(levels: List<Int>, cellSize: Int, margin: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            levels.forEach { level ->
                addView(buildLevelTile(level,
                    LinearLayout.LayoutParams(cellSize, cellSize).apply {
                        setMargins(margin, margin, margin, margin)
                    }
                ))
            }
        }

    private fun buildLevelTile(level: Int, params: LinearLayout.LayoutParams): TextView {
        val bw = dp(2)

        fun makeBg(fill: Int): Drawable {
            val stroke = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(ACCENT_DIM)
            }
            val inner = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(fill)
            }
            return LayerDrawable(arrayOf(stroke, InsetDrawable(inner, bw)))
        }

        val states = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), makeBg(ACCENT_DARK))
            addState(intArrayOf(), makeBg(TILE_FILL))
        }

        return TextView(this).apply {
            text = level.toString()
            typeface = pixelFont
            textSize = if (level < 10) 18f else 15f
            setTextColor(TEXT_WHITE)
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            background = states
            layoutParams = params

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                false
            }

            setOnClickListener {
                startActivity(
                    Intent(this@LevelSelectActivity, GameActivity::class.java)
                        .putExtra(GameActivity.EXTRA_START_LEVEL, level)
                )
            }
        }
    }

    private fun updatePage(animated: Boolean, direction: Int) {
        val totalPages = pageCount()
        val startLevel = currentPage * levelsPerPage + 1
        val endLevel = minOf(startLevel + levelsPerPage - 1, LevelRegistry.count)
        val newPage = buildPageGrid(startLevel, endLevel)

        if (!animated || pageHost.childCount == 0) {
            pageHost.removeAllViews()
            pageHost.addView(newPage)
        } else {
            val oldPage = pageHost.getChildAt(0)

            newPage.alpha = 0f
            newPage.translationX = if (direction > 0) dp(42).toFloat() else -dp(42).toFloat()
            pageHost.addView(newPage)

            oldPage.animate()
                .alpha(0f)
                .translationX(if (direction > 0) -dp(42).toFloat() else dp(42).toFloat())
                .setDuration(180)
                .withEndAction { pageHost.removeView(oldPage) }
                .start()

            newPage.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180)
                .start()
        }

        pageText.text = "${currentPage + 1} / $totalPages"
        prevBtn.visibility = if (currentPage > 0) View.VISIBLE else View.INVISIBLE
        nextBtn.visibility = if (currentPage < totalPages - 1) View.VISIBLE else View.INVISIBLE
    }

    private fun pageCount(): Int =
        ceil(LevelRegistry.count / levelsPerPage.toDouble()).toInt()

    private fun buildSpacer(h: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, h)
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density + 0.5f).toInt()

    private inner class GridOverlayView(context: android.content.Context) : View(context) {
        private val paint = Paint().apply {
            color = Color.argb(15, 0, 200, 160)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        override fun onDraw(canvas: Canvas) {
            val step = dp(40).toFloat()
            var x = 0f
            while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), paint); x += step }
            var y = 0f
            while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, paint); y += step }
        }
    }
}