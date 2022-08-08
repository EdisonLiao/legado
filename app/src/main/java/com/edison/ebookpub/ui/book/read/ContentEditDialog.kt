package com.edison.ebookpub.ui.book.read

import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseDialogFragment
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.BookChapter
import com.edison.ebookpub.databinding.DialogContentEditBinding
import com.edison.ebookpub.databinding.DialogEditTextBinding
import com.edison.ebookpub.help.BookHelp
import com.edison.ebookpub.help.ContentProcessor
import com.edison.ebookpub.lib.dialogs.alert
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.model.ReadBook
import com.edison.ebookpub.model.webBook.WebBook
import com.edison.ebookpub.utils.applyTint
import com.edison.ebookpub.utils.sendToClip
import com.edison.ebookpub.utils.setLayout
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 内容编辑
 */
class ContentEditDialog : BaseDialogFragment(R.layout.dialog_content_edit) {

    val binding by viewBinding(DialogContentEditBinding::bind)
    val viewModel by viewModels<ContentEditViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = ReadBook.curTextChapter?.title
        initMenu()
        binding.toolBar.setOnClickListener {
            launch {
                val book = ReadBook.book ?: return@launch
                val chapter = withContext(IO) {
                    appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex)
                } ?: return@launch
                editTitle(chapter)
            }
        }
        viewModel.initContent {
            binding.contentView.setText(it)
            binding.contentView.post {
                binding.contentView.apply {
                    val lineIndex = layout.getLineForOffset(ReadBook.durChapterPos)
                    val lineHeight = layout.getLineTop(lineIndex)
                    scrollTo(0, lineHeight)
                }
            }
        }
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.content_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_save -> launch {
                    binding.contentView.text?.toString()?.let { content ->
                        withContext(IO) {
                            val book = ReadBook.book ?: return@withContext
                            val chapter = appDb.bookChapterDao
                                .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                                ?: return@withContext
                            BookHelp.saveText(book, chapter, content)
                        }
                    }
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                    dismiss()
                }
                R.id.menu_reset -> viewModel.initContent(true) { content ->
                    binding.contentView.setText(content)
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                }
                R.id.menu_copy_all -> requireContext()
                    .sendToClip("${binding.toolBar.title}\n${binding.contentView.text}")
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun editTitle(chapter: BookChapter) {
        alert {
            setTitle(R.string.edit)
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            alertBinding.editView.setText(chapter.title)
            setCustomView(alertBinding.root)
            okButton {
                chapter.title = alertBinding.editView.text.toString()
                launch {
                    withContext(IO) {
                        appDb.bookChapterDao.upDate(chapter)
                    }
                    binding.toolBar.title = chapter.getDisplayTitle()
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                }
            }
        }
    }

    class ContentEditViewModel(application: Application) : BaseViewModel(application) {

        var content: String? = null

        fun initContent(reset: Boolean = false, success: (String) -> Unit) {
            execute {
                val book = ReadBook.book ?: return@execute null
                val chapter = appDb.bookChapterDao
                    .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                    ?: return@execute null
                if (reset) {
                    content = null
                    BookHelp.delContent(book, chapter)
                    if (!book.isLocalBook()) ReadBook.bookSource?.let { bookSource ->
                        WebBook.getContentAwait(this, bookSource, book, chapter)
                    }
                }
                return@execute content ?: let {
                    val contentProcessor = ContentProcessor.get(book.name, book.origin)
                    val content = BookHelp.getContent(book, chapter) ?: return@let null
                    contentProcessor.getContent(book, chapter, content, includeTitle = false)
                        .joinToString("\n")
                }
            }.onSuccess {
                content = it
                success.invoke(it ?: "")
            }
        }

    }

}