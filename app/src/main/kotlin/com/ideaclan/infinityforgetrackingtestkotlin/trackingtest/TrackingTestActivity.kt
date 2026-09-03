package com.ideaclan.infinityforgetrackingtestkotlin.trackingtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.factory.core.designsystem.theme.FactoryTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hosts [TrackingTestRoute] as its own top-level Activity, deliberately outside
 * [com.ideaclan.infinityforgetrackingtestkotlin.ui.FactoryNavHost]'s shared nav graph
 * (`core-navigation`'s `FactoryDestination`) — this screen exists only in this test
 * app, so it does not belong in the shared, factory-owned navigation contract that
 * every generated app inherits. Reached from Settings via a plain `Intent`.
 */
@AndroidEntryPoint
class TrackingTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FactoryTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    TrackingTestRoute()
                }
            }
        }
    }
}
