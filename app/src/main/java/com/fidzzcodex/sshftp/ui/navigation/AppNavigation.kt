package com.fidzzcodex.sshftp.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fidzzcodex.sshftp.ui.AppViewModel
import com.fidzzcodex.sshftp.ui.components.NeoTabRow
import com.fidzzcodex.sshftp.ui.components.NeoTopBar
import com.fidzzcodex.sshftp.ui.screens.*

sealed class Screen(val label: String) {
    object FileManager : Screen("Files")
    object Terminal   : Screen("Terminal")
    object Sessions   : Screen("Sessions")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(vm: AppViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Sessions) }
    var showSettings by remember { mutableStateOf(false) }

    val screens = listOf(Screen.FileManager, Screen.Terminal, Screen.Sessions)
    val icons = listOf(
        Icons.Default.FolderOpen,
        Icons.Default.Terminal,
        Icons.Default.Storage,
    )

    Column(Modifier.fillMaxSize()) {
        NeoTopBar(
            title       = "SSH • SFTP",
            subtitle    = if (state.isConnected) "${state.connectedUser}@${state.connectedHost}" else "Not connected",
            isConnected = state.isConnected,
            onSettingsClick = { showSettings = true },
        )

        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState   = currentScreen,
                transitionSpec = {
                    slideInHorizontally { if (targetState.label > initialState.label) it else -it } togetherWith
                    slideOutHorizontally { if (targetState.label > initialState.label) -it else it }
                },
                label = "screenTransition",
            ) { screen ->
                when (screen) {
                    Screen.FileManager -> FileManagerScreen(vm)
                    Screen.Terminal    -> TerminalScreen(vm)
                    Screen.Sessions    -> SessionsScreen(vm) { currentScreen = Screen.FileManager }
                }
            }
        }

        NeoTabRow(
            tabs          = screens.map { it.label },
            selectedIndex = screens.indexOf(currentScreen),
            onTabSelected = { currentScreen = screens[it] },
            icons         = icons,
        )
    }

    if (showSettings) {
        SettingsDialog(vm = vm, onDismiss = { showSettings = false })
    }
}
