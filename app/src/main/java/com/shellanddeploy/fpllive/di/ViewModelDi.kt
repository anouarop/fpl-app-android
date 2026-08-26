package com.shellanddeploy.fpllive.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shellanddeploy.fpllive.FplApp

/** Returns the [FplApp] application instance for manual DI. */
@Composable
fun fplApp(): FplApp = LocalContext.current.applicationContext as FplApp

/**
 * Creates a view model that only needs app-level services (no SavedStateHandle).
 * Usage: `val vm = fplViewModel { app -> SearchViewModel(app.repository, app.settings) }`
 */
@Composable
inline fun <reified VM : ViewModel> fplViewModel(crossinline create: (FplApp) -> VM): VM {
    val app = fplApp()
    return viewModel(factory = viewModelFactory {
        initializer { create(app) }
    })
}

/**
 * Creates a view model that also needs a [SavedStateHandle] (e.g. for nav arguments).
 * Usage: `val vm = fplViewModelWithArgs { app, handle -> PlayerDetailViewModel(app.repository, app.settings, handle) }`
 */
@Composable
inline fun <reified VM : ViewModel> fplViewModelWithArgs(crossinline create: (FplApp, SavedStateHandle) -> VM): VM {
    val app = fplApp()
    return viewModel(factory = viewModelFactory {
        initializer { create(app, createSavedStateHandle()) }
    })
}
