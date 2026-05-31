package com.tishou.assistant.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.tishou.assistant.R

/**
 * iOS 风格按钮组件
 * 
 * 特性：
 * - 圆角矩形设计
 * - 点击缩放动画
 * - 毛玻璃效果（可选）
 * - 支持加载状态
 */
class iOSButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var text: String = ""
    private var textColor: Int
    private var backgroundColor: Int
    private var pressedBackgroundColor: Int
    private var cornerRadius: Float
    private var textSize: Float
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private val path = Path()
    
    private var isPressed = false
    private var isLoading = false
    private var scale = 1f

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.iOSButton, defStyleAttr, 0)
        
        text = typedArray.getString(R.styleable.iOSButton_android_text) ?: ""
        textColor = typedArray.getColor(R.styleable.iOSButton_android_textColor, ContextCompat.getColor(context, R.color.text_on_primary))
        backgroundColor = typedArray.getColor(R.styleable.iOSButton_backgroundColor, ContextCompat.getColor(context, R.color.primary))
        pressedBackgroundColor = typedArray.getColor(R.styleable.iOSButton_pressedColor, ContextCompat.getColor(context, R.color.primary_dark))
        cornerRadius = typedArray.getDimension(R.styleable.iOSButton_cornerRadius, 28f)
        textSize = typedArray.getDimension(R.styleable.iOSButton_android_textSize, 16f)
        
        typedArray.recycle()
        
        textPaint.color = textColor
        textPaint.textSize = textSize
        backgroundPaint.color = backgroundColor
        
        isClickable = true
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scaledWidth = width * scale
        val scaledHeight = height * scale
        val offsetX = (width - scaledWidth) / 2
        val offsetY = (height - scaledHeight) / 2

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        // 绘制背景
        rectF.set(0f, 0f, width.toFloat(), height.toFloat())
        path.reset()
        path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.drawPath(path, backgroundPaint)

        // 绘制文字
        if (!isLoading) {
            val textX = width / 2f
            val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(text, textX, textY, textPaint)
        } else {
            // TODO: 绘制加载动画
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                scale = 0.95f
                backgroundPaint.color = pressedBackgroundColor
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                scale = 1f
                backgroundPaint.color = backgroundColor
                invalidate()
                
                if (event.action == MotionEvent.ACTION_UP && !isLoading) {
                    performClick()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    /**
     * 设置按钮文字
     */
    fun setText(text: String) {
        this.text = text
        invalidate()
    }

    /**
     * 设置背景颜色
     */
    fun setBackgroundColor(color: Int) {
        backgroundColor = color
        backgroundPaint.color = color
        invalidate()
    }

    /**
     * 设置加载状态
     */
    fun setLoading(loading: Boolean) {
        isLoading = loading
        invalidate()
    }

    /**
     * 设置启用状态
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.5f
        isClickable = enabled
    }
}