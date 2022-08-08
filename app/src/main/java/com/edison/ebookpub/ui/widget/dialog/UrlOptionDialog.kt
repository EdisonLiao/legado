package com.edison.ebookpub.ui.widget.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.edison.ebookpub.constant.AppConst
import com.edison.ebookpub.databinding.DialogUrlOptionEditBinding
import com.edison.ebookpub.model.analyzeRule.AnalyzeUrl
import com.edison.ebookpub.utils.GSON
import com.edison.ebookpub.utils.setLayout

class UrlOptionDialog(context: Context, private val success: (String) -> Unit) : Dialog(context) {

    val binding = DialogUrlOptionEditBinding.inflate(layoutInflater)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.editMethod.setFilterValues("POST", "GET")
        binding.editCharset.setFilterValues(AppConst.charsets)
        binding.tvOk.setOnClickListener {
            success.invoke(GSON.toJson(getUrlOption()))
            dismiss()
        }
    }

    private fun getUrlOption(): AnalyzeUrl.UrlOption {
        val urlOption = AnalyzeUrl.UrlOption()
        urlOption.useWebView(binding.cbUseWebView.isChecked)
        urlOption.setMethod(binding.editMethod.text.toString())
        urlOption.setCharset(binding.editCharset.text.toString())
        urlOption.setHeaders(binding.editHeaders.text.toString())
        urlOption.setBody(binding.editBody.text.toString())
        urlOption.setRetry(binding.editRetry.text.toString())
        urlOption.setType(binding.editType.text.toString())
        urlOption.setWebJs(binding.editWebJs.text.toString())
        urlOption.setJs(binding.editJs.text.toString())
        return urlOption
    }

}