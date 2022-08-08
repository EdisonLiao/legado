package com.edison.ebookpub.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.edison.ebookpub.lib.theme.Selector
import com.edison.ebookpub.lib.theme.ThemeStore
import com.edison.ebookpub.lib.theme.bottomBackground
import com.edison.ebookpub.lib.theme.getSecondaryTextColor
import com.edison.ebookpub.utils.ColorUtils

class ThemeBottomNavigationVIew(context: Context, attrs: AttributeSet) :
    BottomNavigationView(context, attrs) {

    init {
        val bgColor = context.bottomBackground
        setBackgroundColor(bgColor)
        val textIsDark = ColorUtils.isColorLight(bgColor)
        val textColor = context.getSecondaryTextColor(textIsDark)
        val colorStateList = Selector.colorBuild()
            .setDefaultColor(textColor)
            .setSelectedColor(ThemeStore.accentColor(context)).create()
        itemIconTintList = colorStateList
        itemTextColor = colorStateList
    }

}