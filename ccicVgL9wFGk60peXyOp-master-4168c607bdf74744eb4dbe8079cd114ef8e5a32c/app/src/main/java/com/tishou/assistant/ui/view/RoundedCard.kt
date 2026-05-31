package com.tishou.assistant.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tishou.assistant.R

/**
 * iOS 风格圆角卡片组件
 * 
 * 特性：
 * - 超大圆角 (24dp+)
 * - 细腻阴影层次
 * - 支持点击态动画
 * - 毛玻璃效果背景（可选）
 */
class RoundedCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cornerRadius: Float
    private val shadowColor: Int
    private val shadowRadius: Float
    private val shadowDx: Float
    private val shadowDy: Float
    private val backgroundColor: Int
    private val strokeColor: Int
    private val strokeWidth: Float
    private val isPressed: Boolean = false

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = android.graphics.BlurMaskFilter(shadowRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private val rectF = RectF()
    private val path = Path()

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundedCard, defStyleAttr, 0)
        
        cornerRadius = typedArray.getDimension(R.styleable.RoundedCard_cornerRadius, 24f)
        shadowColor = typedArray.getColor(R.styleable.RoundedCard_shadowColor, ContextCompat.getColor(context, R.color.text_secondary))
        shadowRadius = typedArray.getDimension(R.styleable.RoundedCard_shadowRadius, 8f)
        shadowDx = typedArray.getDimension(R.styleable.RoundedCard_shadowDx, 0f)
        shadowDy = typedArray.getDimension(R.styleable.RoundedCard_shadowDy, 4f)
        backgroundColor = typedArray.getColor(R.styleable.RoundedCard_cardBackgroundColor, ContextCompat.getColor(context, R.color.card_background))
        strokeColor = typedArray.getColor(R.styleable.RoundedCard_strokeColor, ContextCompat.getColor(context, R.color.stroke_light))
        strokeWidth = typedArray.getDimension(R.styleable.RoundedCard_strokeWidth, 1f)
        
        typedArray.recycle()
        
        setupPaints()
        setClickable(true)
        setFocusable(true)
    }

    private fun setupPaints() {
        backgroundPaint.color = backgroundColor
        strokePaint.color = strokeColor
        strokePaint.strokeWidth = strokeWidth
        shadowPaint.color = shadowColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        rectF.set(0f, 0f, width.toFloat(), height.toFloat())
        path.reset()
        path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)

        // 绘制阴影
        if (shadowRadius > 0) {
            canvas.save()
            canvas.translate(shadowDx, shadowDy)
            canvas.drawPath(path, shadowPaint)
            canvas.restore()
        }

        // 绘制背景
        canvas.drawPath(path, backgroundPaint)

        // 绘制边框
        if (strokeWidth > 0) {
            canvas.drawPath(path, strokePaint)
        }
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        if (pressed) {
            backgroundPaint.alpha = 200
        } else {
            backgroundPaint.alpha = 255
        }
        invalidate()
    }

    /**
     * 设置背景颜色
     */
    fun setCardBackgroundColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    /**
     * 设置边框颜色
     */
    fun setStrokeColor(color: Int) {
        strokePaint.color = color
        invalidate()
    }

    /**
     * 设置圆角半径
     */
    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }
}