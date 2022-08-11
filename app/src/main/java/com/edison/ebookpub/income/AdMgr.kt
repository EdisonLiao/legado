package com.edison.ebookpub.income

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.edison.ebookpub.databinding.AdNativeTemplateBinding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

object AdMgr {

    fun requestNativeAd(
        context: Context,
        adUnitID: String,
        adRequestListener: IAdmobRequestListener
    ) {
        val builder = AdLoader.Builder(context, adUnitID)

        builder.forNativeAd { nativeAd ->
            if (context is Activity) {
                if (context.isFinishing || context.isChangingConfigurations || context.isDestroyed) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
            }

            adRequestListener.onLoadSuccess(nativeAd)
        }

        val adOptions = NativeAdOptions.Builder().build()
        builder.withNativeAdOptions(adOptions)

        val adLoader =
            builder
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            adRequestListener.onLoadFailed(loadAdError.code,loadAdError.message)
                        }
                    }
                )
                .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun populateNativeAdView(nativeAd: NativeAd, unifiedAdBinding: ViewBinding,containerView: ViewGroup){
        if (unifiedAdBinding is AdNativeTemplateBinding){
            val nativeAdView = unifiedAdBinding.root

            // Set the media view.
            nativeAdView.mediaView = unifiedAdBinding.adMedia

            // Set other ad assets.
            nativeAdView.headlineView = unifiedAdBinding.adHeadline
            nativeAdView.bodyView = unifiedAdBinding.adBody
            nativeAdView.callToActionView = unifiedAdBinding.adCallToAction
            nativeAdView.iconView = unifiedAdBinding.adAppIcon
            nativeAdView.priceView = unifiedAdBinding.adPrice
            nativeAdView.starRatingView = unifiedAdBinding.adStars
            nativeAdView.storeView = unifiedAdBinding.adStore
            nativeAdView.advertiserView = unifiedAdBinding.adAdvertiser

            // The headline and media content are guaranteed to be in every UnifiedNativeAd.
            unifiedAdBinding.adHeadline.text = nativeAd.headline
            nativeAd.mediaContent?.let { unifiedAdBinding.adMedia.setMediaContent(it) }

            // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
            // check before trying to display them.
            if (nativeAd.body == null) {
                unifiedAdBinding.adBody.visibility = View.INVISIBLE
            } else {
                unifiedAdBinding.adBody.visibility = View.VISIBLE
                unifiedAdBinding.adBody.text = nativeAd.body
            }

            if (nativeAd.callToAction == null) {
                unifiedAdBinding.adCallToAction.visibility = View.INVISIBLE
            } else {
                unifiedAdBinding.adCallToAction.visibility = View.VISIBLE
                unifiedAdBinding.adCallToAction.text = nativeAd.callToAction
            }

            if (nativeAd.icon == null) {
                unifiedAdBinding.adAppIcon.visibility = View.GONE
            } else {
                unifiedAdBinding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
                unifiedAdBinding.adAppIcon.visibility = View.VISIBLE
            }

            if (nativeAd.price == null) {
                unifiedAdBinding.adPrice.visibility = View.INVISIBLE
            } else {
                unifiedAdBinding.adPrice.visibility = View.VISIBLE
                unifiedAdBinding.adPrice.text = nativeAd.price
            }

            if (nativeAd.store == null) {
                unifiedAdBinding.adStore.visibility = View.INVISIBLE
            } else {
                unifiedAdBinding.adStore.visibility = View.VISIBLE
                unifiedAdBinding.adStore.text = nativeAd.store
            }

            if (nativeAd.starRating == null) {
                unifiedAdBinding.adStars.visibility = View.INVISIBLE
            } else {
                unifiedAdBinding.adStars.rating = nativeAd.starRating!!.toFloat()
                unifiedAdBinding.adStars.visibility = View.VISIBLE
            }

            if (nativeAd.advertiser == null) {
                unifiedAdBinding.adAdvertiser.visibility = View.INVISIBLE
            } else {
                unifiedAdBinding.adAdvertiser.text = nativeAd.advertiser
                unifiedAdBinding.adAdvertiser.visibility = View.VISIBLE
            }

            // This method tells the Google Mobile Ads SDK that you have finished populating your
            // native ad view with this native ad.
            nativeAdView.setNativeAd(nativeAd)

            // Get the video controller for the ad. One will always be provided, even if the ad doesn't
            // have a video asset.
            val vc = nativeAd.mediaContent?.videoController

            // Updates the UI to say whether or not this ad has a video asset.
            if (vc != null && vc.hasVideoContent()) {
                // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
                // VideoController will call methods on this object when events occur in the video
                // lifecycle.
                vc.videoLifecycleCallbacks =
                    object : VideoController.VideoLifecycleCallbacks() {
                        override fun onVideoEnd() {
                            // Publishers should allow native ads to complete video playback before
                            // refreshing or replacing them with another ad in the same UI location.
                            super.onVideoEnd()
                        }
                    }
            }
        }

        containerView.removeAllViews()
        containerView.addView(unifiedAdBinding.root)
    }




}