package com.factory.ads.admob

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.factory.ads.api.AdPlacement
import com.factory.ads.api.BannerAdRenderer
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import javax.inject.Inject

class AdMobBannerRenderer @Inject constructor(
    private val adUnitIdResolver: AdUnitIdResolver,
) : BannerAdRenderer {

    @Composable
    override fun Banner(placement: AdPlacement, modifier: Modifier) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = adUnitIdResolver.resolve(placement)
                    loadAd(AdRequest.Builder().build())
                }
            },
        )
    }
}
