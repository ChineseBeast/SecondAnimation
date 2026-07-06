package com.example.UI

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.withStyledAttributes
import com.example.animationsecond.R

/**
 * 带文字显示的进度条自定义 View。
 * 支持圆角裁剪、进度动画、居中文字。
 */
class ProgressStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IProgressStrip {

    companion object {
        // 边框宽度
        private const val BORDER_WIDTH = 5f
        // 默认颜色
        private const val DEFAULT_BG = Color.GRAY
        private const val DEFAULT_FG = Color.BLUE
        // 进度范围
        private const val DEFAULT_MAX = 100
        private const val DEFAULT_MIN = 0
        // 圆角半径
        private const val BORDER_RADIUS = 20f
        // 文字默认值
        private const val DEFAULT_TXT_SIZE = 14f
        private const val DEFAULT_TXT_COLOR = Color.BLACK
        // 动画时长（毫秒）
        private const val ANIM_DURATION = 1000L

        private val ANIM_INTERP = AccelerateDecelerateInterpolator()
    }

    override var min: Int = DEFAULT_MIN
        set(value) {
            field = value
            invalidate()
        }

    override var max: Int = DEFAULT_MAX
        set(value) {
            field = value
            invalidate()
        }

    private var _value: Int = DEFAULT_MIN

    /**
     * 公开进度值。赋值时会启动 ValueAnimator 从当前值平滑过渡到目标值。
     * 注意：内部动画回调直接写 _value，避免死循环。
     */
    override var value: Int
        get() = _value
        set(target) {
            animator.cancel()
            animator.setIntValues(_value, target)
            animator.start()
        }

    override var progressBackground: Int = DEFAULT_BG
        set(value) {
            field = value
            invalidate()
        }

    var progressForeground: Int = DEFAULT_FG
        set(value) {
            field = value
            fillPaint.color = value
            invalidate()
        }

    override var text: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var textSize: Float = DEFAULT_TXT_SIZE
        set(value) {
            field = value
            textPaint.textSize = value
            invalidate()
        }

    var textColor: Int = DEFAULT_TXT_COLOR
        set(value) {
            field = value
            textPaint.color = value
            invalidate()
        }

    private val borderPaint: Paint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = BORDER_WIDTH
    }

    private val fillPaint: Paint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        style = Paint.Style.FILL
        color = progressForeground
    }

    private val textPaint: Paint = Paint().apply {
        color = textColor
        textSize = textSize
    }

    private val borderRect = RectF()
    private val textBounds = Rect()

    private val animator: ValueAnimator = ValueAnimator().apply {
        duration = ANIM_DURATION
        interpolator = ANIM_INTERP
        addUpdateListener { animation ->
            _value = animation.animatedValue as Int
            invalidate()
        }
    }

    init {
        // 从 XML 属性读取配置
        context.withStyledAttributes(attrs, R.styleable.ProgressStripView, defStyleAttr, 0) {
            _value = getInt(R.styleable.ProgressStripView_value, DEFAULT_MIN)
            max = getInt(R.styleable.ProgressStripView_max, DEFAULT_MAX)
            min = getInt(R.styleable.ProgressStripView_min, DEFAULT_MIN)
            text = getString(R.styleable.ProgressStripView_text) ?: ""
            textSize = getFloat(R.styleable.ProgressStripView_textSize, DEFAULT_TXT_SIZE)
            textColor = getColor(R.styleable.ProgressStripView_textColor, DEFAULT_TXT_COLOR)
            progressBackground = getColor(R.styleable.ProgressStripView_progressBackground, DEFAULT_BG)
            progressForeground = getColor(R.styleable.ProgressStripView_progressForeground, DEFAULT_FG)
            if (min > max) min = max
        }
        textPaint.color = textColor
        textPaint.textSize = textSize
        fillPaint.color = progressForeground
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bounds = contentBounds() ?: return
        drawClippedProgress(canvas, bounds)
        drawBorder(canvas, bounds)
        drawCenteredText(canvas)
    }

    /**
     * 计算扣除 padding 后的内容区域。
     * 如果宽或高 <= 0 返回 null，跳过绘制。
     */
    private fun contentBounds(): RectF? {
        val pl = paddingLeft.toFloat()
        val pt = paddingTop.toFloat()
        val pr = paddingRight.toFloat()
        val pb = paddingBottom.toFloat()
        val cw = width - pl - pr
        val ch = height - pt - pb
        return if (cw <= 0 || ch <= 0) null
        else RectF(pl, pt, pl + cw, pt + ch)
    }

    /**
     * 绘制被圆角裁剪的背景和进度条。
     * 原理：clipPath 裁剪画布 → 画矩形自动变成圆角，
     *       比 saveLayer + PorterDuff 性能更好。
     */
    private fun drawClippedProgress(canvas: Canvas, bounds: RectF) {
        val path = Path().apply {
            addRoundRect(
                bounds.left + BORDER_WIDTH, bounds.top + BORDER_WIDTH,
                bounds.right - BORDER_WIDTH, bounds.bottom - BORDER_WIDTH,
                BORDER_RADIUS, BORDER_RADIUS, Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipPath(path)

        fillPaint.color = progressBackground
        canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, fillPaint)

        fillPaint.color = progressForeground
        val fraction = if (max > min) (bounds.width() / (max - min).toFloat()) * value else 0f
        canvas.drawRect(
            bounds.left + BORDER_WIDTH, bounds.top + BORDER_WIDTH,
            bounds.left + BORDER_WIDTH + fraction, bounds.bottom - BORDER_WIDTH,
            fillPaint
        )
        canvas.restore()
    }

    /**
     * 绘制外边框（描边圆角矩形）。
     */
    private fun drawBorder(canvas: Canvas, bounds: RectF) {
        borderRect.set(
            bounds.left + BORDER_WIDTH, bounds.top + BORDER_WIDTH,
            bounds.right - BORDER_WIDTH, bounds.bottom - BORDER_WIDTH
        )
        canvas.drawRoundRect(borderRect, BORDER_RADIUS, BORDER_RADIUS, borderPaint)
    }

    /**
     * 在 View 正中央绘制文字（水平居中 + 垂直居中）。
     */
    private fun drawCenteredText(canvas: Canvas) {
        if (text.isEmpty()) return
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val fm = textPaint.fontMetrics
        val tx = width / 2f - (textBounds.right - textBounds.left) / 2f
        val ty = height / 2f + (fm.descent - fm.ascent) / 2f - Math.abs(fm.descent)
        canvas.drawText(text, tx, ty, textPaint)
    }
}
