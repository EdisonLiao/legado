package com.edison.ebookpub.ui.book.read.config

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import com.edison.ebookpub.R
import com.edison.ebookpub.help.config.ReadBookConfig
import com.edison.ebookpub.lib.dialogs.alert
import com.edison.ebookpub.lib.theme.accentColor
import com.edison.ebookpub.ui.widget.text.StrokeTextView


class TextFontWeightConverter(context: Context, attrs: AttributeSet?) :
    StrokeTextView(context, attrs) {

    private val spannableString = SpannableString("中/粗/细")
    private var enabledSpan: ForegroundColorSpan = ForegroundColorSpan(context.accentColor)
    private var onChanged: (() -> Unit)? = null

    init {
        text = spannableString
        if (!isInEditMode) {
            upUi(ReadBookConfig.textBold)
        }
        setOnClickListener {
            selectType()
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun upUi(type: Int) {
        spannableString.removeSpan(enabledSpan)
        when (type) {
            0 -> spannableString.setSpan(enabledSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            1 -> spannableString.setSpan(enabledSpan, 2, 3, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            2 -> spannableString.setSpan(enabledSpan, 4, 5, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        text = spannableString
    }

    private fun selectType() {
        context.alert(titleResource = R.string.text_font_weight_converter) {
            items(context.resources.getStringArray(R.array.text_font_weight).toList()) { _, i ->
                ReadBookConfig.textBold = i
                upUi(i)
                onChanged?.invoke()
            }
        }
    }

    fun onChanged(unit: () -> Unit) {
        onChanged = unit
    }
}