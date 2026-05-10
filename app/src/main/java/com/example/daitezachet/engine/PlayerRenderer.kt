package com.example.daitezachet.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

class PlayerRenderer(private val appearance: PlayerAppearance) {

    private val bodyPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val eyePaint  = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true; color = Color.rgb(18, 18, 28) }
    private val hatPaint  = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val path      = Path()

    private fun bodyColor(hasKey: Boolean): Int = appearance.bodyColor

    fun draw(canvas: Canvas, player: Player, keyPaint: Paint, drawDiamond: (Canvas, RectF, Paint) -> Unit) {
        val pb = player.bounds

        bodyPaint.color = bodyColor(player.hasKey)
        canvas.drawRect(pb, bodyPaint)

        drawEyes(canvas, pb, player.isDead)

        drawHat(canvas, pb)

        if (player.hasKey) {
            val kBounds = RectF(pb.centerX() - 12f, pb.top - 26f, pb.centerX() + 12f, pb.top - 2f)
            drawDiamond(canvas, kBounds, keyPaint)
        }
    }

    private fun drawEyes(canvas: Canvas, pb: RectF, isDead: Boolean) {
        val eyeY = pb.top + 16f
        val lx   = pb.left  + 12f
        val rx   = pb.right - 12f

        if (isDead) {
            drawCross(canvas, lx, eyeY, 6f)
            drawCross(canvas, rx, eyeY, 6f)
            return
        }

        when (appearance.eyeStyle) {
            EyeStyle.NORMAL -> {
                canvas.drawCircle(lx, eyeY, 5f, eyePaint)
                canvas.drawCircle(rx, eyeY, 5f, eyePaint)
            }
            EyeStyle.ANGRY -> {
                eyePaint.strokeWidth = 4f; eyePaint.style = Paint.Style.STROKE
                canvas.drawLine(lx - 5f, eyeY, lx + 5f, eyeY, eyePaint)
                canvas.drawLine(rx - 5f, eyeY, rx + 5f, eyeY, eyePaint)
                eyePaint.strokeWidth = 3f
                canvas.drawLine(lx - 6f, eyeY - 9f, lx + 6f, eyeY - 5f, eyePaint)
                canvas.drawLine(rx - 6f, eyeY - 5f, rx + 6f, eyeY - 9f, eyePaint)
                eyePaint.style = Paint.Style.FILL; eyePaint.strokeWidth = 1f
            }
            EyeStyle.SLEEPY -> {
                canvas.drawArc(RectF(lx - 5f, eyeY - 5f, lx + 5f, eyeY + 5f), 0f, 180f, false, eyePaint)
                canvas.drawArc(RectF(rx - 5f, eyeY - 5f, rx + 5f, eyeY + 5f), 0f, 180f, false, eyePaint)
            }
            EyeStyle.DEAD -> {
                drawCross(canvas, lx, eyeY, 6f)
                drawCross(canvas, rx, eyeY, 6f)
            }
        }
    }

    private fun drawCross(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        eyePaint.style = Paint.Style.STROKE; eyePaint.strokeWidth = 3.5f
        canvas.drawLine(cx - r, cy - r, cx + r, cy + r, eyePaint)
        canvas.drawLine(cx + r, cy - r, cx - r, cy + r, eyePaint)
        eyePaint.style = Paint.Style.FILL; eyePaint.strokeWidth = 1f
    }

    private fun drawHat(canvas: Canvas, pb: RectF) {
        val cx = pb.centerX()
        val top = pb.top
        val hw  = pb.width()

        when (appearance.hat) {
            Hat.NONE -> Unit

            Hat.CAP -> {
                hatPaint.color = Color.rgb(30, 30, 180)
                canvas.drawRect(cx - hw * 0.5f, top - 18f, cx + hw * 0.5f, top, hatPaint)  // тулья
                hatPaint.color = Color.rgb(20, 20, 140)
                canvas.drawRect(cx - hw * 0.65f, top - 8f, cx + hw * 0.1f, top, hatPaint)  // козырёк
            }

            Hat.CROWN -> {
                hatPaint.color = Color.rgb(255, 200, 0)
                canvas.drawRect(cx - hw * 0.45f, top - 12f, cx + hw * 0.45f, top, hatPaint)
                path.rewind()
                val bL = cx - hw * 0.45f
                val bR = cx + hw * 0.45f
                val bY = top - 12f
                val peakY = top - 28f
                path.moveTo(bL, bY); path.lineTo(bL + 10f, peakY); path.lineTo(bL + 20f, bY)
                path.moveTo(cx - 10f, bY); path.lineTo(cx, peakY); path.lineTo(cx + 10f, bY)
                path.moveTo(bR - 20f, bY); path.lineTo(bR - 10f, peakY); path.lineTo(bR, bY)
                path.close()
                canvas.drawPath(path, hatPaint)
            }

            Hat.WIZARD -> {
                hatPaint.color = Color.rgb(90, 20, 160)
                canvas.drawRect(cx - hw * 0.55f, top - 8f, cx + hw * 0.55f, top, hatPaint)
                path.rewind()
                path.moveTo(cx - hw * 0.4f, top - 8f)
                path.lineTo(cx, top - 48f)
                path.lineTo(cx + hw * 0.4f, top - 8f)
                path.close()
                canvas.drawPath(path, hatPaint)
                hatPaint.color = Color.rgb(255, 230, 0)
                canvas.drawCircle(cx, top - 30f, 5f, hatPaint)
            }

            Hat.TOP_HAT -> {
                hatPaint.color = Color.rgb(20, 20, 20)
                canvas.drawRect(cx - hw * 0.6f, top - 8f, cx + hw * 0.6f, top, hatPaint)
                canvas.drawRect(cx - hw * 0.38f, top - 40f, cx + hw * 0.38f, top - 8f, hatPaint)
                hatPaint.color = Color.rgb(200, 200, 200)
                canvas.drawRect(cx - hw * 0.38f, top - 15f, cx + hw * 0.38f, top - 10f, hatPaint)
            }
        }
    }

    private fun blendColor(c1: Int, c2: Int, t: Float): Int {
        val r = (Color.red(c1)   * (1 - t) + Color.red(c2)   * t).toInt()
        val g = (Color.green(c1) * (1 - t) + Color.green(c2) * t).toInt()
        val b = (Color.blue(c1)  * (1 - t) + Color.blue(c2)  * t).toInt()
        return Color.rgb(r, g, b)
    }
}