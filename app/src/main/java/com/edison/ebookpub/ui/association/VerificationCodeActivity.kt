package com.edison.ebookpub.ui.association

import android.os.Bundle
import com.edison.ebookpub.base.BaseActivity
import com.edison.ebookpub.databinding.ActivityTranslucenceBinding
import com.edison.ebookpub.utils.showDialogFragment
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding

/**
 * 验证码
 */
class VerificationCodeActivity :
    BaseActivity<ActivityTranslucenceBinding>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("imageUrl")?.let {
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            showDialogFragment(
                VerificationCodeDialog(it, sourceOrigin)
            )
        } ?: finish()
    }

}