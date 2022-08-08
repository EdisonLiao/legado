package com.edison.ebookpub.lib.prefs

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.edison.ebookpub.R
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.lib.theme.accentColor
import com.edison.ebookpub.lib.theme.backgroundColor
import com.edison.ebookpub.utils.ColorUtils


class PreferenceCategory(context: Context, attrs: AttributeSet) :
    PreferenceCategory(context, attrs) {

    init {
        isPersistent = true
        layoutResource = R.layout.view_preference_category
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val view = holder.findViewById(R.id.preference_title)
        if (view is TextView) {  //  && !view.isInEditMode
            view.text = title
            if (view.isInEditMode) return
            view.setTextColor(context.accentColor)
            view.isVisible = !title.isNullOrEmpty()

            val da = holder.findViewById(R.id.preference_divider_above)
            val dividerColor = if (AppConfig.isNightTheme) {
                ColorUtils.withAlpha(
                    ColorUtils.shiftColor(context.backgroundColor, 1.05f),
                    0.5f
                )
            } else {
                ColorUtils.withAlpha(
                    ColorUtils.shiftColor(context.backgroundColor, 0.95f),
                    0.5f
                )
            }
            if (da is View) {
                da.setBackgroundColor(dividerColor)
                da.isVisible = holder.isDividerAllowedAbove
            }
            val db = holder.findViewById(R.id.preference_divider_below)
            if (db is View) {
                db.setBackgroundColor(dividerColor)
                db.isVisible = holder.isDividerAllowedBelow
            }
        }
    }

}
