package com.edison.ebookpub.ui.book.read.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseDialogFragment
import com.edison.ebookpub.constant.EventBus
import com.edison.ebookpub.databinding.DialogTipConfigBinding
import com.edison.ebookpub.help.config.ReadBookConfig
import com.edison.ebookpub.help.config.ReadTipConfig
import com.edison.ebookpub.lib.dialogs.selector
import com.edison.ebookpub.utils.*
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding


class TipConfigDialog : BaseDialogFragment(R.layout.dialog_tip_config) {

    companion object {
        const val TIP_COLOR = 7897
    }

    private val binding by viewBinding(DialogTipConfigBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initEvent()
        observeEvent<String>(EventBus.TIP_COLOR) {
            upTvTipColor()
        }
    }

    private fun initView() {
        binding.rgTitleMode.checkByIndex(ReadBookConfig.titleMode)
        binding.dsbTitleSize.progress = ReadBookConfig.titleSize
        binding.dsbTitleTop.progress = ReadBookConfig.titleTopSpacing
        binding.dsbTitleBottom.progress = ReadBookConfig.titleBottomSpacing

        binding.tvHeaderShow.text =
            ReadTipConfig.getHeaderModes(requireContext())[ReadTipConfig.headerMode]
        binding.tvFooterShow.text =
            ReadTipConfig.getFooterModes(requireContext())[ReadTipConfig.footerMode]

        ReadTipConfig.run {
            tipNames.let { tipNames ->
                binding.tvHeaderLeft.text =
                    tipNames.getOrElse(tipValues.indexOf(tipHeaderLeft)) { tipNames[none] }
                binding.tvHeaderMiddle.text =
                    tipNames.getOrElse(tipValues.indexOf(tipHeaderMiddle)) { tipNames[none] }
                binding.tvHeaderRight.text =
                    tipNames.getOrElse(tipValues.indexOf(tipHeaderRight)) { tipNames[none] }
                binding.tvFooterLeft.text =
                    tipNames.getOrElse(tipValues.indexOf(tipFooterLeft)) { tipNames[none] }
                binding.tvFooterMiddle.text =
                    tipNames.getOrElse(tipValues.indexOf(tipFooterMiddle)) { tipNames[none] }
                binding.tvFooterRight.text =
                    tipNames.getOrElse(tipValues.indexOf(tipFooterRight)) { tipNames[none] }
            }
        }
        upTvTipColor()
    }

    private fun upTvTipColor() {
        binding.tvTipColor.text =
            if (ReadTipConfig.tipColor == 0) {
                "跟随正文"
            } else {
                "#${ReadTipConfig.tipColor.hexString}"
            }
    }

    private fun initEvent() = binding.run {
        rgTitleMode.setOnCheckedChangeListener { _, checkedId ->
            ReadBookConfig.titleMode = rgTitleMode.getIndexById(checkedId)
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbTitleSize.onChanged = {
            ReadBookConfig.titleSize = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbTitleTop.onChanged = {
            ReadBookConfig.titleTopSpacing = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbTitleBottom.onChanged = {
            ReadBookConfig.titleBottomSpacing = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        llHeaderShow.setOnClickListener {
            val headerModes = ReadTipConfig.getHeaderModes(requireContext())
            context?.selector(items = headerModes.values.toList()) { _, i ->
                ReadTipConfig.headerMode = headerModes.keys.toList()[i]
                tvHeaderShow.text = headerModes[ReadTipConfig.headerMode]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        llFooterShow.setOnClickListener {
            val footerModes = ReadTipConfig.getFooterModes(requireContext())
            context?.selector(items = footerModes.values.toList()) { _, i ->
                ReadTipConfig.footerMode = footerModes.keys.toList()[i]
                tvFooterShow.text = footerModes[ReadTipConfig.footerMode]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        llHeaderLeft.setOnClickListener {
            context?.selector(items = ReadTipConfig.tipNames) { _, i ->
                val tipValue = ReadTipConfig.tipValues[i]
                clearRepeat(tipValue)
                ReadTipConfig.tipHeaderLeft = tipValue
                tvHeaderLeft.text = ReadTipConfig.tipNames[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        llHeaderMiddle.setOnClickListener {
            context?.selector(items = ReadTipConfig.tipNames) { _, i ->
                val tipValue = ReadTipConfig.tipValues[i]
                clearRepeat(tipValue)
                ReadTipConfig.tipHeaderMiddle = tipValue
                tvHeaderMiddle.text = ReadTipConfig.tipNames[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        llHeaderRight.setOnClickListener {
            context?.selector(items = ReadTipConfig.tipNames) { _, i ->
                val tipValue = ReadTipConfig.tipValues[i]
                clearRepeat(tipValue)
                ReadTipConfig.tipHeaderRight = tipValue
                tvHeaderRight.text = ReadTipConfig.tipNames[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        llFooterLeft.setOnClickListener {
            context?.selector(items = ReadTipConfig.tipNames) { _, i ->
                val tipValue = ReadTipConfig.tipValues[i]
                clearRepeat(tipValue)
                ReadTipConfig.tipFooterLeft = tipValue
                tvFooterLeft.text = ReadTipConfig.tipNames[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        llFooterMiddle.setOnClickListener {
            context?.selector(items = ReadTipConfig.tipNames) { _, i ->
                val tipValue = ReadTipConfig.tipValues[i]
                clearRepeat(tipValue)
                ReadTipConfig.tipFooterMiddle = tipValue
                tvFooterMiddle.text = ReadTipConfig.tipNames[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        llFooterRight.setOnClickListener {
            context?.selector(items = ReadTipConfig.tipNames) { _, i ->
                val tipValue = ReadTipConfig.tipValues[i]
                clearRepeat(tipValue)
                ReadTipConfig.tipFooterRight = tipValue
                tvFooterRight.text = ReadTipConfig.tipNames[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        llTipColor.setOnClickListener {
            context?.selector(items = arrayListOf("跟随正文", "自定义")) { _, i ->
                when (i) {
                    0 -> {
                        ReadTipConfig.tipColor = 0
                        upTvTipColor()
                        postEvent(EventBus.UP_CONFIG, true)
                    }
                    1 -> ColorPickerDialog.newBuilder()
                        .setShowAlphaSlider(false)
                        .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                        .setDialogId(TIP_COLOR)
                        .show(requireActivity())
                }
            }
        }
    }

    private fun clearRepeat(repeat: Int) = ReadTipConfig.apply {
        if (repeat != none) {
            if (tipHeaderLeft == repeat) {
                tipHeaderLeft = none
                binding.tvHeaderLeft.text = tipNames[none]
            }
            if (tipHeaderMiddle == repeat) {
                tipHeaderMiddle = none
                binding.tvHeaderMiddle.text = tipNames[none]
            }
            if (tipHeaderRight == repeat) {
                tipHeaderRight = none
                binding.tvHeaderRight.text = tipNames[none]
            }
            if (tipFooterLeft == repeat) {
                tipFooterLeft = none
                binding.tvFooterLeft.text = tipNames[none]
            }
            if (tipFooterMiddle == repeat) {
                tipFooterMiddle = none
                binding.tvFooterMiddle.text = tipNames[none]
            }
            if (tipFooterRight == repeat) {
                tipFooterRight = none
                binding.tvFooterRight.text = tipNames[none]
            }
        }
    }

}