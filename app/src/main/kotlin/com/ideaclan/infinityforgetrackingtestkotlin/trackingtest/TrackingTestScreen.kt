package com.ideaclan.infinityforgetrackingtestkotlin.trackingtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * A single-purpose developer/validation screen — not a product feature. Exercises
 * every InfinityForgeTrackingClient operation from the UI so tracking behavior is
 * observable and repeatable without reading logcat blind (Docs/INFINITYFORGE_TRACKING.md).
 * Deliberately plain Material3 buttons and a scrolling text log — no navigation graph
 * integration, no design-system polish, nothing a real product screen would need.
 */
@Composable
fun TrackingTestRoute(viewModel: TrackingTestViewModel = hiltViewModel()) {
    val log by viewModel.log.collectAsState()
    val identitySnapshot by viewModel.identitySnapshot.collectAsState()

    TrackingTestScreen(
        availability = viewModel.availability.name,
        providerNames = viewModel.providerNames,
        anonymousId = identitySnapshot.anonymousId,
        userId = identitySnapshot.userId,
        userPropertyCount = identitySnapshot.userPropertyCount,
        log = log,
        onTrackEvent = viewModel::trackEvent,
        onTrackScreen = viewModel::trackScreen,
        onIdentifyUser = viewModel::identifyUser,
        onSetUserProperty = viewModel::setUserProperty,
        onResetUser = viewModel::resetUser,
        onRecordMetric = viewModel::recordMetric,
        onTriggerInvalidEvent = viewModel::triggerInvalidEvent,
        onTriggerProviderFailure = viewModel::triggerProviderFailure,
    )
}

@Suppress("LongParameterList")
@Composable
fun TrackingTestScreen(
    availability: String,
    providerNames: List<String>,
    anonymousId: String,
    userId: String?,
    userPropertyCount: Int,
    log: List<String>,
    onTrackEvent: () -> Unit,
    onTrackScreen: () -> Unit,
    onIdentifyUser: () -> Unit,
    onSetUserProperty: () -> Unit,
    onResetUser: () -> Unit,
    onRecordMetric: () -> Unit,
    onTriggerInvalidEvent: () -> Unit,
    onTriggerProviderFailure: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "InfinityForge Tracking Test", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "availability=$availability  providers=${providerNames.ifEmpty { listOf("none") }}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "anonymous_id=$anonymousId  user_id=${userId ?: "(none)"}  user_properties=$userPropertyCount",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTrackEvent, modifier = Modifier.fillMaxWidth()) { Text("Track Event") }
            Button(onClick = onTrackScreen, modifier = Modifier.fillMaxWidth()) { Text("Track Screen") }
            Button(onClick = onIdentifyUser, modifier = Modifier.fillMaxWidth()) { Text("Identify User") }
            Button(onClick = onSetUserProperty, modifier = Modifier.fillMaxWidth()) { Text("Set User Property") }
            Button(onClick = onResetUser, modifier = Modifier.fillMaxWidth()) { Text("Reset User") }
            Button(onClick = onRecordMetric, modifier = Modifier.fillMaxWidth()) { Text("Record Metric") }
            Button(
                onClick = onTriggerInvalidEvent,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) { Text("Trigger Invalid Event") }
            Button(
                onClick = onTriggerProviderFailure,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) { Text("Trigger Provider Failure") }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(text = "Log", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(log) { entry ->
                Text(
                    text = entry,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
