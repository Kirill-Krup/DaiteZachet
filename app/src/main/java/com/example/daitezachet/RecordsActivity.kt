package com.example.daitezachet

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.daitezachet.levels.LevelRegistry
import com.example.daitezachet.records.LevelRecord
import com.example.daitezachet.records.RecordsStore

class RecordsActivity : AppCompatActivity() {

    private val BG_COLOR = Color.rgb(12, 12, 22)
    private val ACCENT = Color.rgb(0, 220, 180)
    private val ACCENT_DIM = Color.rgb(0, 130, 110)
    private val TILE_FILL = Color.argb(25, 0, 220, 180)
    private val TEXT_WHITE = Color.rgb(230, 240, 255)
    private val TEXT_MUTED = Color.rgb(100, 115, 145)
    private val TEXT_WARN = Color.rgb(255, 190, 80)

    private val pixelFont: Typeface by lazy {
        try { ResourcesCompat.getFont(this, R.font.pixel)!! }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goFullscreen()

        val root = FrameLayout(this).apply {
            setBackgroundColor(BG_COLOR)
        }

        val content = buildContent()
        root.addView(content)

        setContentView(root)

        content.alpha = 0f
        content.translationY = 20f
        content.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(260)
            .start()
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

    private fun buildContent(): View =
        ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                setPadding(dp(16), dp(20), dp(16), dp(20))

                addView(buildTopBar())
                addView(buildSummaryBlock())
                addView(spacer(dp(14)))
                addView(buildResetButton())
                addView(spacer(dp(18)))
                addView(sectionLabel("РЕКОРДЫ ПО УРОВНЯМ"))
                addView(spacer(dp(10)))
                addView(buildTableHeader())
                addView(spacer(dp(8)))
                addView(buildRecordsList())
                addView(spacer(dp(20)))
            })

        }

    private fun buildTopBar(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        addView(buildNavBtn("←") { finish() })
        addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
        addView(TextView(context).apply {
            text = "РЕКОРДЫ"
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

    private fun buildSummaryBlock(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        val totalDeaths = RecordsStore.getTotalDeaths(this@RecordsActivity, LevelRegistry.count)

        val records = (1..LevelRegistry.count).map { level ->
            level to RecordsStore.getLevelRecord(this@RecordsActivity, level)
        }

        val completed = records.count { it.second.completed }

        val hardestLevel = records
            .filter { it.second.totalDeaths > 0 || it.second.completed }
            .maxWithOrNull(
                compareBy<Pair<Int, LevelRecord>> { it.second.totalDeaths }
                    .thenByDescending { it.first }
            )

        addView(buildInfoCard(
            title = "ОБЩИЕ СМЕРТИ",
            value = totalDeaths.toString(),
            sub = "за все попытки"
        ))

        addView(spacer(dp(10)))

        addView(buildInfoCard(
            title = "ПРОЙДЕНО",
            value = "$completed / ${LevelRegistry.count}",
            sub = if (completed > 0) "общий прогресс по игре" else "ещё нет прохождений"
        ))

        addView(spacer(dp(10)))

        addView(buildInfoCard(
            title = "САМЫЙ СЛОЖНЫЙ",
            value = hardestLevel?.let { "УРОВЕНЬ ${it.first}" } ?: "—",
            sub = hardestLevel?.let { "${it.second.totalDeaths} смертей" } ?: "нет данных",
            valueColor = TEXT_WARN
        ))
    }

    private fun buildInfoCard(
        title: String,
        value: String,
        sub: String,
        valueColor: Int = TEXT_WHITE
    ): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(6).toFloat()
            setColor(TILE_FILL)
            setStroke(dp(1), ACCENT_DIM)
        }

        addView(TextView(context).apply {
            text = title
            typeface = pixelFont
            textSize = 10f
            setTextColor(ACCENT)
            letterSpacing = 0.08f
        })

        addView(spacer(dp(8)))

        addView(TextView(context).apply {
            text = value
            typeface = pixelFont
            textSize = 16f
            setTextColor(valueColor)
        })

        addView(spacer(dp(4)))

        addView(TextView(context).apply {
            text = sub
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextColor(TEXT_MUTED)
        })
    }

    private fun buildTableHeader(): LinearLayout = buildRow(
        level = "#",
        time = "ВРЕМЯ",
        deaths = "СМЕРТИ",
        isHeader = true
    )

    private fun buildRecordsList(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        for (level in 1..LevelRegistry.count) {
            val rec = RecordsStore.getLevelRecord(this@RecordsActivity, level)
            val timeText = if (rec.bestTimeMs == Long.MAX_VALUE) "—" else formatTime(rec.bestTimeMs)
            val deathsText = rec.totalDeaths.toString()

            addView(buildRow(
                level = level.toString(),
                time = timeText,
                deaths = deathsText,
                isHeader = false
            ))

            if (level != LevelRegistry.count) {
                addView(spacer(dp(8)))
            }
        }
    }

    private fun buildRow(
        level: String,
        time: String,
        deaths: String,
        isHeader: Boolean
    ): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(52)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(5).toFloat()
            setColor(if (isHeader) Color.argb(40, 0, 220, 180) else TILE_FILL)
            setStroke(dp(1), if (isHeader) ACCENT else ACCENT_DIM)
        }

        addView(buildCell(level, 0.9f, Gravity.START, isHeader))
        addView(buildCell(time, 1.5f, Gravity.CENTER, isHeader))
        addView(buildCell(deaths, 1.1f, Gravity.END, isHeader))
    }

    private fun buildCell(
        textValue: String,
        weight: Float,
        gravityValue: Int,
        isHeader: Boolean
    ): TextView = TextView(this).apply {
        text = textValue
        typeface = if (isHeader) pixelFont else Typeface.MONOSPACE
        textSize = if (isHeader) 10f else 12f
        setTextColor(if (isHeader) ACCENT else TEXT_WHITE)
        gravity = gravityValue
        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, weight)
    }

    private fun sectionLabel(text: String): TextView = TextView(this).apply {
        this.text = text
        typeface = pixelFont
        textSize = 11f
        setTextColor(ACCENT)
        letterSpacing = 0.08f
    }

    private fun buildNavBtn(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
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

    private fun spacer(h: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, h)
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hundredths = (ms % 1000) / 10
        return String.format("%d:%02d.%02d", minutes, seconds, hundredths)
    }

    private fun buildResetButton(): TextView = TextView(this).apply {
        text = "СБРОСИТЬ СТАТИСТИКУ"
        typeface = pixelFont
        textSize = 12f
        setTextColor(Color.rgb(255, 210, 210))
        gravity = Gravity.CENTER
        letterSpacing = 0.05f
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(48))

        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(4).toFloat()
            setColor(Color.argb(35, 220, 70, 70))
            setStroke(dp(1), Color.argb(180, 220, 90, 90))
        }

        setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this@RecordsActivity)
                .setTitle("Сбросить статистику?")
                .setMessage("Все рекорды и смерти по уровням будут удалены.")
                .setPositiveButton("Сбросить") { _, _ ->
                    RecordsStore.resetAll(this@RecordsActivity)
                    recreate()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}