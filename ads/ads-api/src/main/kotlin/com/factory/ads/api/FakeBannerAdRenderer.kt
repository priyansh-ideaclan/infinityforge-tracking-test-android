package com.factory.ads.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javax.inject.Inject

class FakeBannerAdRenderer @Inject constructor() : BannerAdRenderer {
    @Composable
    override fun Banner(placement: AdPlacement, modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Ad placeholder (${placement.name})",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
