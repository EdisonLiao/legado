package com.edison.ebookpub.ui.replace.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.viewModels
import com.edison.ebookpub.R
import com.edison.ebookpub.base.VMBaseActivity
import com.edison.ebookpub.data.entities.ReplaceRule
import com.edison.ebookpub.databinding.ActivityReplaceEditBinding
import com.edison.ebookpub.lib.dialogs.SelectItem
import com.edison.ebookpub.ui.widget.dialog.TextDialog
import com.edison.ebookpub.ui.widget.keyboard.KeyboardToolPop
import com.edison.ebookpub.utils.showDialogFragment
import com.edison.ebookpub.utils.toastOnUi
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding

/**
 * 编辑替换规则
 */
class ReplaceEditActivity :
    VMBaseActivity<ActivityReplaceEditBinding, ReplaceEditViewModel>(false),
    KeyboardToolPop.CallBack {

    companion object {

        fun startIntent(
            context: Context,
            id: Long = -1,
            pattern: String? = null,
            isRegex: Boolean = false,
            scope: String? = null
        ): Intent {
            val intent = Intent(context, ReplaceEditActivity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("pattern", pattern)
            intent.putExtra("isRegex", isRegex)
            intent.putExtra("scope", scope)
            return intent
        }

    }

    override val binding by viewBinding(ActivityReplaceEditBinding::inflate)
    override val viewModel by viewModels<ReplaceEditViewModel>()

    private val softKeyboardTool by lazy {
        KeyboardToolPop(this, this, binding.root, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        softKeyboardTool.attachToWindow(window)
        viewModel.initData(intent) {
            upReplaceView(it)
        }
        binding.ivHelp.setOnClickListener {
            showHelp("regexHelp")
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                val rule = getReplaceRule()
                if (!rule.isValid()) {
                    toastOnUi(R.string.replace_rule_invalid)
                } else {
                    viewModel.save(rule) {
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        softKeyboardTool.dismiss()
    }

    private fun upReplaceView(replaceRule: ReplaceRule) = binding.run {
        etName.setText(replaceRule.name)
        etGroup.setText(replaceRule.group)
        etReplaceRule.setText(replaceRule.pattern)
        cbUseRegex.isChecked = replaceRule.isRegex
        etReplaceTo.setText(replaceRule.replacement)
        cbScopeTitle.isChecked = replaceRule.scopeTitle
        cbScopeContent.isChecked = replaceRule.scopeContent
        etScope.setText(replaceRule.scope)
        etTimeout.setText(replaceRule.timeoutMillisecond.toString())
    }

    private fun getReplaceRule(): ReplaceRule = binding.run {
        val replaceRule: ReplaceRule = viewModel.replaceRule ?: ReplaceRule()
        replaceRule.name = etName.text.toString()
        replaceRule.group = etGroup.text.toString()
        replaceRule.pattern = etReplaceRule.text.toString()
        replaceRule.isRegex = cbUseRegex.isChecked
        replaceRule.replacement = etReplaceTo.text.toString()
        replaceRule.scopeTitle = cbScopeTitle.isChecked
        replaceRule.scopeContent = cbScopeContent.isChecked
        replaceRule.scope = etScope.text.toString()
        replaceRule.timeoutMillisecond = etTimeout.text.toString().ifEmpty { "3000" }.toLong()
        return replaceRule
    }

    override fun helpActions(): List<SelectItem<String>> {
        return arrayListOf(
            SelectItem("正则教程", "regexHelp")
        )
    }

    override fun onHelpActionSelect(action: String) {
        when (action) {
            "regexHelp" -> showHelp("regexHelp")
        }
    }

    override fun sendText(text: String) {
        if (text.isBlank()) return
        val view = window?.decorView?.findFocus()
        if (view is EditText) {
            val start = view.selectionStart
            val end = view.selectionEnd
            //获取EditText的文字
            val edit = view.editableText
            if (start < 0 || start >= edit.length) {
                edit.append(text)
            } else {
                //光标所在位置插入文字
                edit.replace(start, end, text)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun showHelp(fileName: String) {
        //显示目录help下的帮助文档
        val mdText = String(assets.open("help/${fileName}.md").readBytes())
        showDialogFragment(TextDialog(mdText, TextDialog.Mode.MD))
    }

}
