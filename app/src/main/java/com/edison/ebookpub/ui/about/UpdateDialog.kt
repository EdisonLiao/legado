package com.edison.ebookpub.ui.about

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseDialogFragment
import com.edison.ebookpub.databinding.DialogUpdateBinding
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.model.Download
import com.edison.ebookpub.utils.setLayout
import com.edison.ebookpub.utils.toastOnUi
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin

class UpdateDialog() : BaseDialogFragment(R.layout.dialog_update) {

    constructor(newVersion: String, updateBody: String, url: String, name: String) : this() {
        arguments = Bundle().apply {
            putString("newVersion", newVersion)
            putString("updateBody", updateBody)
            putString("url", url)
            putString("name", name)
        }
    }

    val binding by viewBinding(DialogUpdateBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = arguments?.getString("newVersion")
        val updateBody = arguments?.getString("updateBody")
        if (updateBody == null) {
            toastOnUi("没有数据")
            dismiss()
            return
        }
        binding.textView.post {
            Markwon.builder(requireContext())
                .usePlugin(GlideImagesPlugin.create(requireContext()))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .build()
                .setMarkdown(binding.textView, updateBody)
        }
        if (!AppConfig.isGooglePlay) {
            binding.toolBar.inflateMenu(R.menu.app_update)
            binding.toolBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_download -> {
                        val url = arguments?.getString("url")
                        val name = arguments?.getString("name")
                        if (url != null && name != null) {
                            Download.start(requireContext(), url, name)
                        }
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

}