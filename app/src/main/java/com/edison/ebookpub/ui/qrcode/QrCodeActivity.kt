package com.edison.ebookpub.ui.qrcode

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.zxing.Result
import com.king.zxing.CameraScan.OnScanResultCallback
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseActivity
import com.edison.ebookpub.databinding.ActivityQrcodeCaptureBinding
import com.edison.ebookpub.utils.QRCodeUtils
import com.edison.ebookpub.utils.SelectImageContract
import com.edison.ebookpub.utils.launch
import com.edison.ebookpub.utils.readBytes
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding

class QrCodeActivity : BaseActivity<ActivityQrcodeCaptureBinding>(), OnScanResultCallback {

    override val binding by viewBinding(ActivityQrcodeCaptureBinding::inflate)

    private val selectQrImage = registerForActivityResult(SelectImageContract()) {
        it?.uri?.readBytes(this)?.let { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            onScanResultCallback(QRCodeUtils.parseCodeResult(bitmap))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val fTag = "qrCodeFragment"
        val qrCodeFragment = QrCodeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_content, qrCodeFragment, fTag)
            .commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.qr_code_scan, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_choose_from_gallery -> selectQrImage.launch()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onScanResultCallback(result: Result?): Boolean {
        val intent = Intent()
        intent.putExtra("result", result?.text)
        setResult(RESULT_OK, intent)
        finish()
        return true
    }

}