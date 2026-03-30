package com.selfcode.vkplus

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.selfcode.vkplus.auth.AuthState
import com.selfcode.vkplus.auth.AuthViewModel
import com.selfcode.vkplus.data.local.SettingsStore
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import com.selfcode.vkplus.ui.components.MainScaffold
import com.selfcode.vkplus.ui.navigation.Screen
import com.selfcode.vkplus.ui.screens.AuthScreen
import com.selfcode.vkplus.ui.screens.feed.FeedScreen
import com.selfcode.vkplus.ui.screens.friends.FriendsScreen
import com.selfcode.vkplus.ui.screens.messages.MessagesScreen
import com.selfcode.vkplus.ui.screens.profile.ProfileScreen
import com.selfcode.vkplus.ui.screens.settings.SettingsScreen
import com.selfcode.vkplus.ui.screens.tools.ToolsScreen
import com.selfcode.vkplus.ui.theme.VKPlusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repository: VKRepository
    @Inject lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val antiScreen = runBlocking { settingsStore.antiScreen.first() }
        applySecureFlag(antiScreen)

        enableEdgeToEdge()
        setContent {
            VKPlusTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()

                when (authState) {
                    is AuthState.Checking -> {}
                    is AuthState.Unauthenticated -> {
                        AuthScreen(
                            onTokenReceived = { uri -> authViewModel.handleRedirectUri(uri) },
                            onQrAuthenticated = { authViewModel.checkToken() },
                            onManualToken = { token -> authViewModel.saveManualToken(token) }
                        )
                    }
                    is AuthState.InvalidToken -> {
                        AuthScreen(
                            onTokenReceived = { uri -> authViewModel.handleRedirectUri(uri) },
                            onQrAuthenticated = { authViewModel.checkToken() },
                            onManualToken = { token -> authViewModel.saveManualToken(token) },
                            invalidToken = true
                        )
                    }
                    is AuthState.Authenticated -> {
                        AuthenticatedApp(
                            repository = repository,
                            onLogout = { authViewModel.logout() },
                            onAntiScreenChanged = { applySecureFlag(it) }
                        )
                    }
                }
            }
        }
    }

    private fun applySecureFlag(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
fun AuthenticatedApp(
    repository: VKRepository,
    onLogout: () -> Unit,
    onAntiScreenChanged: (Boolean) -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Feed) }
    var currentUser by remember { mutableStateOf<VKUser?>(null) }

    LaunchedEffect(Unit) {
        when (val r = repository.getCurrentUser()) {
            is VKResult.Success -> currentUser = r.data
            is VKResult.Error -> {}
        }
    }

    MainScaffold(
        currentScreen = currentScreen,
        user = currentUser,
        onNavigate = { currentScreen = it }
    ) {
        when (currentScreen) {
            Screen.Feed -> FeedScreen()
            Screen.Messages -> MessagesScreen()
            Screen.Friends -> FriendsScreen()
            Screen.Profile -> ProfileScreen(onLogout = onLogout)
            Screen.Settings -> SettingsScreen(onAntiScreenChanged = onAntiScreenChanged)
            Screen.Tools -> ToolsScreen()
            else -> FeedScreen()
        }
    }
}
