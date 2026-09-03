package com.ideaclan.infinityforgetrackingtestkotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.factory.ads.api.BannerAdRenderer
import com.factory.core.designsystem.component.FactoryLoadingIndicator
import com.factory.core.designsystem.theme.FactoryTheme
import com.factory.core.logging.Logger
import com.factory.core.navigation.FactoryNavigator
import com.factory.core.tracking.InfinityForgeTrackingClient
import com.ideaclan.infinityforgetrackingtestkotlin.ui.FactoryNavHost
import com.ideaclan.infinityforgetrackingtestkotlin.ui.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var navigator: FactoryNavigator

    @Inject lateinit var bannerAdRenderer: BannerAdRenderer

    @Inject lateinit var trackingClient: InfinityForgeTrackingClient

    @Inject lateinit var logger: Logger

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val startDestination by viewModel.startDestination.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsState()

            FactoryTheme(themeMode = themeMode, dynamicColor = dynamicColorEnabled) {
                val destination = startDestination
                if (destination == null) {
                    FactoryLoadingIndicator(modifier = Modifier.fillMaxSize())
                } else {
                    FactoryNavHost(
                        navController = rememberNavController(),
                        navigator = navigator,
                        startDestination = destination,
                        bannerAdRenderer = bannerAdRenderer,
                        trackingClient = trackingClient,
                        logger = logger,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
