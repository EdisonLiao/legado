package com.edison.ebookpub.ui.widget.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseDialogFragment
import com.edison.ebookpub.databinding.DialogPhotoViewBinding
import com.edison.ebookpub.help.BookHelp
import com.edison.ebookpub.help.glide.ImageLoader
import com.edison.ebookpub.help.glide.OkHttpModelLoader
import com.edison.ebookpub.model.BookCover
import com.edison.ebookpub.model.ReadBook
import com.edison.ebookpub.utils.setLayout
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding

/**
 * 显示图片
 */
class PhotoDialog() : BaseDialogFragment(R.layout.dialog_photo_view) {

    constructor(src: String, sourceOrigin: String? = null) : this() {
        arguments = Bundle().apply {
            putString("src", src)
            putString("sourceOrigin", sourceOrigin)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, 1f)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let { arguments ->
            arguments.getString("src")?.let { src ->
                val file = ReadBook.book?.let { book ->
                    BookHelp.getImage(book, src)
                }
                if (file?.exists() == true) {
                    ImageLoader.load(requireContext(), file)
                        .error(R.drawable.image_loading_error)
                        .into(binding.photoView)
                } else {
                    ImageLoader.load(requireContext(), src).apply {
                        arguments.getString("sourceOrigin")?.let { sourceOrigin ->
                            apply(
                                RequestOptions().set(
                                    OkHttpModelLoader.sourceOriginOption,
                                    sourceOrigin
                                )
                            )
                        }
                    }.error(BookCover.defaultDrawable)
                        .into(binding.photoView)
                }
            }
        }

    }

}
