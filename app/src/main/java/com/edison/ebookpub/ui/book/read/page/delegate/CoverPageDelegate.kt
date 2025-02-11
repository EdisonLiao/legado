package com.edison.ebookpub.ui.book.read.page.delegate

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import com.edison.ebookpub.ui.book.read.page.ReadView
import com.edison.ebookpub.ui.book.read.page.entities.PageDirection
import com.edison.ebookpub.utils.screenshot

class CoverPageDelegate(readView: ReadView) : HorizontalPageDelegate(readView) {
    private val bitmapMatrix = Matrix()
    private val shadowDrawableR: GradientDrawable

    init {
        val shadowColors = intArrayOf(0x66111111, 0x00000000)
        shadowDrawableR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, shadowColors
        )
        shadowDrawableR.gradientType = GradientDrawable.LINEAR_GRADIENT
    }

    override fun onDraw(canvas: Canvas) {
        if (!isRunning) return
        val offsetX = touchX - startX

        if ((mDirection == PageDirection.NEXT && offsetX > 0)
            || (mDirection == PageDirection.PREV && offsetX < 0)
        ) {
            return
        }

        val distanceX = if (offsetX > 0) offsetX - viewWidth else offsetX + viewWidth
        if (mDirection == PageDirection.PREV) {
            if (offsetX <= viewWidth) {
                bitmapMatrix.setTranslate(distanceX, 0.toFloat())
                prevBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
                addShadow(distanceX.toInt(), canvas)
            } else {
                prevBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            }
        } else if (mDirection == PageDirection.NEXT) {
            bitmapMatrix.setTranslate(distanceX - viewWidth, 0.toFloat())
            nextBitmap?.let {
                canvas.apply {
                    save()
                    val width = it.width.toFloat()
                    val height = it.height.toFloat()
                    clipRect(width + offsetX, 0f, width, height)
                    drawBitmap(it, 0f, 0f, null)
                    restore()
                }
            }
            curBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
            addShadow(distanceX.toInt(), canvas)
        }
    }

    override fun setBitmap() {
        when (mDirection) {
            PageDirection.PREV -> {
                prevBitmap?.recycle()
                prevBitmap = prevPage.screenshot()
            }
            PageDirection.NEXT -> {
                nextBitmap?.recycle()
                nextBitmap = nextPage.screenshot()
                curBitmap?.recycle()
                curBitmap = curPage.screenshot()
            }
            else -> Unit
        }
    }

    private fun addShadow(left: Int, canvas: Canvas) {
        if (left < 0) {
            shadowDrawableR.setBounds(left + viewWidth, 0, left + viewWidth + 30, viewHeight)
            shadowDrawableR.draw(canvas)
        } else if (left > 0) {
            shadowDrawableR.setBounds(left, 0, left + 30, viewHeight)
            shadowDrawableR.draw(canvas)
        }
    }

    override fun onAnimStop() {
        if (!isCancel) {
            readView.fillPage(mDirection)
        }
    }

    override fun onAnimStart(animationSpeed: Int) {
        val distanceX: Float
        when (mDirection) {
            PageDirection.NEXT -> distanceX =
                if (isCancel) {
                    var dis = viewWidth - startX + touchX
                    if (dis > viewWidth) {
                        dis = viewWidth.toFloat()
                    }
                    viewWidth - dis
                } else {
                    -(touchX + (viewWidth - startX))
                }
            else -> distanceX =
                if (isCancel) {
                    -(touchX - startX)
                } else {
                    viewWidth - (touchX - startX)
                }
        }
        startScroll(touchX.toInt(), 0, distanceX.toInt(), 0, animationSpeed)
    }

}
