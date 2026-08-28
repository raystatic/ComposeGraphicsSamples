package com.example.graphicspoc

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

class LuminousBackgroundDrawable(
    private val context: Context,
    @ColorInt private var accentColor: Int = Color(0xFF8DC5FD).toArgb(),
    private val backgroundColor: Int = Color.White.toArgb(),
    private val isTopDown: Boolean = false,
) : Drawable() {

    @ColorInt
    private val defaultAccentColor: Int = accentColor

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        style = Paint.Style.FILL
    }
    private val gradientColors = IntArray(GRADIENT_SPECS.size)
    private val gradientMatrix = Matrix()
    private var topHalfHeight: Float = 0f
    private var bottomHalfHeight: Float = 0f

    private val baseHeight: Float = 450f * context.resources.displayMetrics.density
    private val verticalRadius: Float = baseHeight
    private val horizontalScale: Float = 2.3f

    private val initialGradientInitialOffset: Float = 0.0f
    private var gradientStops: FloatArray = FloatArray(GRADIENT_SPECS.size)
    private var shader: RadialGradient? = null
    private var colorAnimator: ValueAnimator? = null

    init {
        rebuildGradientColors()
    }

    private fun rebuildGradientColors() {
        val baseAlpha = android.graphics.Color.alpha(accentColor)
        for (i in GRADIENT_SPECS.indices) {
            gradientColors[i] =
                ColorUtils.setAlphaComponent(accentColor, (GRADIENT_SPECS[i].second * baseAlpha).toInt())
        }
    }

    private fun rebuildShader() {
        if (bounds.isEmpty) return
        shader = RadialGradient(
            0f,
            0f,
            1f,
            gradientColors,
            gradientStops,
            Shader.TileMode.CLAMP,
        ).apply { setLocalMatrix(gradientMatrix) }
    }

    fun setAccentColor(@ColorInt newColor: Int) {
        if (accentColor == newColor) return
        accentColor = newColor
        rebuildGradientColors()
        rebuildShader()
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (bounds.isEmpty) return
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        val thresholdMin = baseHeight * 1.2f
        val thresholdMax = baseHeight * 1.6f
        val maxAdd = 64f * context.resources.displayMetrics.density

        val addedHeight = when {
            height > thresholdMax -> maxAdd
            height < thresholdMin -> 0f
            else -> maxAdd * (height - thresholdMin) / (thresholdMax - thresholdMin)
        }

        topHalfHeight = height - (baseHeight + addedHeight)
        bottomHalfHeight = baseHeight + addedHeight

        val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
        val horizontalRadius = horizontalScale * screenWidth / 2f

        val startOffset: Float = initialGradientInitialOffset
        gradientStops = FloatArray(GRADIENT_SPECS.size) { i ->
            startOffset + (1.0f - startOffset) * GRADIENT_SPECS[i].first
        }

        gradientMatrix.reset()
        gradientMatrix.setScale(horizontalRadius, verticalRadius)
        val centerY = if (isTopDown) bottomHalfHeight else topHalfHeight
        gradientMatrix.postTranslate(width * 0.5f, centerY)

        rebuildShader()
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        canvas.drawColor(backgroundColor)

        paint.shader = shader
        if (isTopDown) {
            canvas.drawRect(0f, 0f, width, bottomHalfHeight, paint)
        } else {
            canvas.drawRect(0f, topHalfHeight, width, height, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private val GRADIENT_SPECS = listOf(
            0.000f to 0.000f,
            0.450f to 0.000f,
            0.505f to 0.020f,
            0.560f to 0.060f,
            0.615f to 0.130f,
            0.670f to 0.210f,
            0.725f to 0.310f,
            0.780f to 0.420f,
            0.835f to 0.540f,
            0.890f to 0.680f,
            0.945f to 0.840f,
            1.000f to 1.000f,
        )
    }
}