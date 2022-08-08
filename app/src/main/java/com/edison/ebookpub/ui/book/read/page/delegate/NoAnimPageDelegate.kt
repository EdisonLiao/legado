package com.edison.ebookpub.ui.book.read.page.delegate

import android.graphics.Canvas
import com.edison.ebookpub.ui.book.read.page.ReadView
import com.edison.ebookpub.ui.book.read.page.entities.PageDirection

class NoAnimPageDelegate(readView: ReadView) : HorizontalPageDelegate(readView) {

    override fun setDirection(direction: PageDirection) {
        mDirection = direction
    }

    override fun onAnimStart(animationSpeed: Int) {
        if (!isCancel) {
            readView.fillPage(mDirection)
        }
        stopScroll()
    }

    override fun onDraw(canvas: Canvas) {
        // nothing
    }

    override fun onAnimStop() {
        // nothing
    }


}