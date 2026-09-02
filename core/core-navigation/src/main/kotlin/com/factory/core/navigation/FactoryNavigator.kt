package com.factory.core.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A cross-feature navigation command, decoupled from any specific `NavController` API. */
sealed interface NavigationCommand {
    data class NavigateTo(val destination: FactoryDestination, val popUpToStart: Boolean = false) :
        NavigationCommand
    data object NavigateBack : NavigationCommand
}

/**
 * Lets a feature's ViewModel request navigation (e.g. "go Home after successful login")
 * without holding a `NavController` or depending on another feature module. The single
 * top-level `NavHost` (composed in the `app` module, the only module that sees every
 * feature) collects [commands] and applies them to its own `NavController`.
 */
interface FactoryNavigator {
    val commands: SharedFlow<NavigationCommand>
    suspend fun navigate(destination: FactoryDestination, popUpToStart: Boolean = false)
    suspend fun navigateBack()
}

@Singleton
class DefaultFactoryNavigator @Inject constructor() : FactoryNavigator {
    private val _commands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 1)
    override val commands: SharedFlow<NavigationCommand> = _commands

    override suspend fun navigate(destination: FactoryDestination, popUpToStart: Boolean) {
        _commands.emit(NavigationCommand.NavigateTo(destination, popUpToStart))
    }

    override suspend fun navigateBack() {
        _commands.emit(NavigationCommand.NavigateBack)
    }
}
