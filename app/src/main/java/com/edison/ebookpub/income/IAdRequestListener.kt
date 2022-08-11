package com.edison.ebookpub.income

import com.google.android.gms.ads.nativead.NativeAd

interface IAdRequestListener<AD> {
    
    fun onLoadSuccess(adObject: AD)
    
    fun onLoadFailed(errorCode: Int, errorMsg: String)
}

interface IAdmobRequestListener: IAdRequestListener<NativeAd>