package com.edison.ebookpub.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.widget.FrameLayout
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import com.edison.ebookpub.R
import com.edison.ebookpub.databinding.ViewSelectActionBarBinding
import com.edison.ebookpub.lib.theme.*
import com.edison.ebookpub.utils.ColorUtils
import com.edison.ebookpub.utils.visible


@Suppress("unused")
class SelectActionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private var callBack: CallBack? = null
    private var selMenu: PopupMenu? = null
    private val binding =
        ViewSelectActionBarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setBackgroundColor(context.bottomBackground)
        elevation = context.elevation
        val textIsDark = ColorUtils.isColorLight(context.bottomBackground)
        val primaryTextColor = context.getPrimaryTextColor(textIsDark)
        val secondaryTextColor = context.getSecondaryTextColor(textIsDark)
        binding.cbSelectedAll.setTextColor(primaryTextColor)
        TintHelper.setTint(binding.cbSelectedAll, context.accentColor, !textIsDark)
        binding.ivMenuMore.setColorFilter(secondaryTextColor)
        binding.cbSelectedAll.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                callBack?.selectAll(isChecked)
            }
        }
        binding.btnRevertSelection.setOnClickListener { callBack?.revertSelection() }
        binding.btnSelectActionMain.setOnClickListener { callBack?.onClickSelectBarMainAction() }
        binding.ivMenuMore.setOnClickListener { selMenu?.show() }
    }

    fun setMainActionText(text: String) = binding.run {
        btnSelectActionMain.text = text
        btnSelectActionMain.visible()
    }

    fun setMainActionText(@StringRes id: Int) = binding.run {
        btnSelectActionMain.setText(id)
        btnSelectActionMain.visible()
    }

    fun inflateMenu(@MenuRes resId: Int): Menu? {
        selMenu = PopupMenu(context, binding.ivMenuMore)
        selMenu?.inflate(resId)
        binding.ivMenuMore.visible()
        return selMenu?.menu
    }

    fun setCallBack(callBack: CallBack) {
        this.callBack = callBack
    }

    fun setOnMenuItemClickListener(listener: PopupMenu.OnMenuItemClickListener) {
        selMenu?.setOnMenuItemClickListener(listener)
    }

    fun upCountView(selectCount: Int, allCount: Int) = binding.run {
        if (selectCount == 0) {
            cbSelectedAll.isChecked = false
        } else {
            cbSelectedAll.isChecked = selectCount >= allCount
        }

        //重置全选的文字
        if (cbSelectedAll.isChecked) {
            cbSelectedAll.text = context.getString(
                R.string.select_cancel_count,
                selectCount,
                allCount
            )
        } else {
            cbSelectedAll.text = context.getString(
                R.string.select_all_count,
                selectCount,
                allCount
            )
        }
        setMenuClickable(selectCount > 0)
    }

    private fun setMenuClickable(isClickable: Boolean) = binding.run {
        btnRevertSelection.isEnabled = isClickable
        btnRevertSelection.isClickable = isClickable
        btnSelectActionMain.isEnabled = isClickable
        btnSelectActionMain.isClickable = isClickable
        if (isClickable) {
            ivMenuMore.setColorFilter(context.primaryTextColor)
        } else {
            ivMenuMore.setColorFilter(context.secondaryTextColor)
        }
        ivMenuMore.isEnabled = isClickable
        ivMenuMore.isClickable = isClickable
    }

    interface CallBack {

        fun selectAll(selectAll: Boolean)

        fun revertSelection()

        fun onClickSelectBarMainAction() {}
    }
}