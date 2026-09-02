package com.factory.ads.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Banners are a view, not a one-shot call, so they get their own small boundary rather
 * than living on [AdsController]. `ads-admob`'s implementation wraps a real `AdView` in
 * `AndroidView`; `FakeBannerAdRenderer` renders a fixed-size placeholder box so layout
 * tests/screenshots stay stable without the Google Mobile Ads SDK.
 */
interface BannerAdRenderer {
    @Composable
    fun Banner(placement: AdPlacement, modifier: Modifier)
}
