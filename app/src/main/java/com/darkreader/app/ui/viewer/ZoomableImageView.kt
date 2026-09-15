package com.darkreader.app.ui.viewer

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private var userScale = 1f
    private val minUserScale = 1f
    private val maxUserScale = 5f
    private var baseScale = 1f
    private val lastTouch = PointF()
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val target = (userScale * detector.scaleFactor).coerceIn(minUserScale, maxUserScale)
            val factor = target / userScale
            userScale = target
            matrix.postScale(factor, factor, detector.focusX, detector.focusY)
            checkAndAdjustBounds()
            imageMatrix = matrix
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (userScale > 1.2f) {
                resetZoom()
            } else {
                val target = 2.5f
                val factor = target / userScale
                userScale = target
                matrix.postScale(factor, factor, e.x, e.y)
                checkAndAdjustBounds()
                imageMatrix = matrix
            }
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
        isClickable = true
        isFocusable = true
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        post { resetZoom() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { resetZoom() }
    }

    fun resetZoom() {
        val drawable = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        if (drawableWidth <= 0f || drawableHeight <= 0f) return

        // Fit the whole page by default. This guarantees that no part of a PDF/CBZ page
        // is cropped and adapts to the actual reader viewport on phones and tablets.
        baseScale = minOf(viewWidth / drawableWidth, viewHeight / drawableHeight)
        baseScale = baseScale.coerceAtLeast(0.01f)

        val scaledWidth = drawableWidth * baseScale
        val scaledHeight = drawableHeight * baseScale
        val dx = (viewWidth - scaledWidth) / 2f
        val dy = (viewHeight - scaledHeight) / 2f

        matrix.reset()
        matrix.postScale(baseScale, baseScale)
        matrix.postTranslate(dx, dy)
        imageMatrix = matrix
        userScale = 1f
        isDragging = false
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouch.set(event.x, event.y)
                isDragging = userScale > 1.05f
                if (isDragging) parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress && userScale > 1.05f) {
                    val dx = event.x - lastTouch.x
                    val dy = event.y - lastTouch.y
                    lastTouch.set(event.x, event.y)
                    matrix.postTranslate(dx, dy)
                    checkAndAdjustBounds()
                    imageMatrix = matrix
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun checkAndAdjustBounds() {
        val drawable = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        matrix.mapRect(rect)

        var dx = 0f
        var dy = 0f

        if (rect.width() <= viewWidth) {
            dx = (viewWidth - rect.width()) / 2f - rect.left
        } else {
            if (rect.left > 0f) dx = -rect.left
            if (rect.right < viewWidth) dx = viewWidth - rect.right
        }

        if (rect.height() <= viewHeight) {
            dy = (viewHeight - rect.height()) / 2f - rect.top
        } else {
            if (rect.top > 0f) dy = -rect.top
            if (rect.bottom < viewHeight) dy = viewHeight - rect.bottom
        }

        if (dx != 0f || dy != 0f) matrix.postTranslate(dx, dy)
    }
}
