package com.edison.ebookpub.lib.prefs

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import com.edison.ebookpub.R
import com.edison.ebookpub.lib.theme.accentColor
import com.edison.ebookpub.lib.theme.bottomBackground
import com.edison.ebookpub.lib.theme.getPrimaryTextColor
import com.edison.ebookpub.lib.theme.getSecondaryTextColor
import com.edison.ebookpub.utils.ColorUtils
import splitties.views.onLongClick
import kotlin.math.roundToInt

class Preference(context: Context, attrs: AttributeSet) :
    androidx.preference.Preference(context, attrs) {

    private var onLongClick: ((preference: Preference) -> Boolean)? = null
    private val isBottomBackground: Boolean

    init {
        layoutResource = R.layout.view_preference
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference)
        isBottomBackground = typedArray.getBoolean(R.styleable.Preference_isBottomBackground, false)
        typedArray.recycle()
    }

    companion object {

        fun <T : View> bindView(
            context: Context,
            it: PreferenceViewHolder?,
            icon: Drawable?,
            title: CharSequence?,
            summary: CharSequence?,
            weightLayoutRes: Int? = null,
            viewId: Int? = null,
            weightWidth: Int = 0,
            weightHeight: Int = 0,
            isBottomBackground: Boolean = false
        ): T? {
            if (it == null) return null
            val tvTitle = it.findViewById(R.id.preference_title) as TextView
            tvTitle.text = title
            tvTitle.isVisible = title != null && title.isNotEmpty()
            val tvSummary = it.findViewById(R.id.preference_desc) as? TextView
            tvSummary?.let {
                tvSummary.text = summary
                tvSummary.isGone = summary.isNullOrEmpty()
            }
            if (isBottomBackground && !tvTitle.isInEditMode) {
                val isLight = ColorUtils.isColorLight(context.bottomBackground)
                val pTextColor = context.getPrimaryTextColor(isLight)
                tvTitle.setTextColor(pTextColor)
                val sTextColor = context.getSecondaryTextColor(isLight)
                tvSummary?.setTextColor(sTextColor)
            }
            val iconView = it.findViewById(R.id.preference_icon)
            if (iconView is ImageView) {
                iconView.isVisible = icon != null
                iconView.setImageDrawable(icon)
                iconView.setColorFilter(context.accentColor)
            }

            if (weightLayoutRes != null && weightLayoutRes != 0 && viewId != null && viewId != 0) {
                val lay = it.findViewById(R.id.preference_widget)
                if (lay is FrameLayout) {
                    var needRequestLayout = false
                    var v = it.itemView.findViewById<T>(viewId)
                    if (v == null) {
                        val inflater: LayoutInflater = LayoutInflater.from(context)
                        val childView = inflater.inflate(weightLayoutRes, null)
                        lay.removeAllViews()
                        lay.addView(childView)
                        lay.isVisible = true
                        v = lay.findViewById(viewId)
                    } else
                        needRequestLayout = true

                    if (weightWidth > 0 || weightHeight > 0) {
                        val lp = lay.layoutParams
                        if (weightHeight > 0)
                            lp.height =
                                (context.resources.displayMetrics.density * weightHeight).roundToInt()
                        if (weightWidth > 0)
                            lp.width =
                                (context.resources.displayMetrics.density * weightWidth).roundToInt()
                        lay.layoutParams = lp
                    } else if (needRequestLayout)
                        v.requestLayout()

                    return v
                }
            }

            return null
        }

    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        bindView<View>(
            context,
            holder,
            icon,
            title,
            summary,
            isBottomBackground = isBottomBackground
        )
        super.onBindViewHolder(holder)
        onLongClick?.let { listener ->
            holder.itemView.onLongClick {
                listener.invoke(this)
            }
        }
    }

    fun onLongClick(listener: (preference: Preference) -> Boolean) {
        onLongClick = listener
    }

}
