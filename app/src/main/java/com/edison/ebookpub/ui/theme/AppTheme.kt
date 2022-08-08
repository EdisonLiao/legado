package com.edison.ebookpub.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.edison.ebookpub.help.config.ThemeConfig
import com.edison.ebookpub.lib.theme.accentColor
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.utils.ColorUtils
import splitties.init.appCtx

object AppTheme {

    val colors
        get() = if (ThemeConfig.isDarkTheme()) {
            darkColors(
                primary = Color(appCtx.accentColor),
                primaryVariant = Color(ColorUtils.darkenColor(appCtx.accentColor)),
                secondary = Color(appCtx.primaryColor),
                secondaryVariant = Color(appCtx.primaryColor)
            )
        } else {
            lightColors(
                primary = Color(appCtx.accentColor),
                primaryVariant = Color(ColorUtils.darkenColor(appCtx.accentColor)),
                secondary = Color(appCtx.primaryColor),
                secondaryVariant = Color(appCtx.primaryColor)
            )
        }

}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = AppTheme.colors,
        content = content
    )
}