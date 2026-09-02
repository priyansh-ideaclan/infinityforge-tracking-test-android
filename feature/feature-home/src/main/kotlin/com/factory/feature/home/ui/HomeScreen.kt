package com.factory.feature.home.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import com.factory.ads.api.AdPlacement
import com.factory.ads.api.BannerAdRenderer
import com.factory.core.database.NoteEntity
import com.factory.core.designsystem.component.FactoryErrorState
import com.factory.core.designsystem.component.FactoryLoadingIndicator

@Composable
fun HomeRoute(
    bannerAdRenderer: BannerAdRenderer,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeScreen(
        uiState = uiState,
        bannerAdRenderer = bannerAdRenderer,
        onRefresh = viewModel::refresh,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    bannerAdRenderer: BannerAdRenderer,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag(HomeScreenTestTags.REFRESH_BUTTON),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            bannerAdRenderer.Banner(placement = AdPlacement.BANNER_HOME, modifier = Modifier)
        },
    ) { padding ->
        when {
            uiState.isRefreshing && uiState.notes.isEmpty() ->
                FactoryLoadingIndicator(modifier = Modifier.padding(padding))
            uiState.errorMessage != null && uiState.notes.isEmpty() ->
                FactoryErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRefresh,
                    modifier = Modifier.padding(padding),
                )
            else -> NotesList(notes = uiState.notes, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun NotesList(notes: List<NoteEntity>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeScreenTestTags.NOTES_LIST),
    ) {
        items(notes, key = { it.id }) { note ->
            ListItem(
                headlineContent = { Text(note.title) },
                supportingContent = { Text(note.body) },
            )
        }
    }
}
