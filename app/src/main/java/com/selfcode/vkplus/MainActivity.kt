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
import com.selfcode.vkplus.ui.screens.SplashScreen
import com.selfcode.vkplus.ui.screens.feed.FeedScreen
import com.selfcode.vkplus.ui.screens.friends.FriendsScreen
import com.selfcode.vkplus.ui.screens.messages.MessagesScreen
import com.selfcode.vkplus.ui.screens.profile.ProfileScreen
import com.selfcode.vkplus.ui.screens.settings.SettingsScreen
import com.selfcode.vkplus.ui.screens.about.AboutScreen
import com.selfcode.vkplus.ui.screens.exploits.ExploitsScreen
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
        var showAccountSwitcher by remember { mutableStateOf(false) }
        var addingAccount by remember { mutableStateOf(false) }

                // Show splash only before first auth screen
                var splashDone by remember { mutableStateOf(authState is AuthState.Authenticated) }

                when {
                    authState is AuthState.Checking -> {}

                    authState is AuthState.Authenticated -> {
                        AuthenticatedApp(
                            repository = repository,
                            onLogout = { authViewModel.logout() },
                            onAntiScreenChanged = { applySecureFlag(it) }
                        )
                    }

                    !splashDone -> {
                        SplashScreen(onFinished = { splashDone = true })
                    }

                    authState is AuthState.Unauthenticated -> {
                        AuthScreen(
                            onTokenReceived = { uri -> authViewModel.handleRedirectUri(uri) },
                            onQrAuthenticated = { authViewModel.checkToken() },
                            onManualToken = { token -> authViewModel.saveManualToken(token) }
                        )
                    }

                    authState is AuthState.InvalidToken -> {
                        AuthScreen(
                            onTokenReceived = { uri -> authViewModel.handleRedirectUri(uri) },
                            onQrAuthenticated = { authViewModel.checkToken() },
                            onManualToken = { token -> authViewModel.saveManualToken(token) },
                            invalidToken = true
                        )
                    }
                }
            }
        }
    }

    private fun applySecureFlag(enabled: Boolean) {
        if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

@Composable
fun AuthenticatedApp(repository: VKRepository, onLogout: () -> Unit, onAntiScreenChanged: (Boolean) -> Unit) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Feed) }
    var currentUser by remember { mutableStateOf<VKUser?>(null) }

    LaunchedEffect(Unit) {
        when (val r = repository.getCurrentUser()) {
            is VKResult.Success -> currentUser = r.data
            is VKResult.Error -> {}
        }
    }

    MainScaffold(
                    onSwitchAccount = { showAccountSwitcher = true },
                    onAddAccount = { addingAccount = true },
                    isMirrorActive = exploitsState.mirrorActive && exploitsState.mirroredName.isNotBlank(),
                    mirrorName = exploitsState.mirroredName,
                    mirrorPhoto = exploitsState.mirroredPhoto,currentScreen = currentScreen, user = currentUser, onNavigate = { currentScreen = it }) {
        when (currentScreen) {
            Screen.Feed -> FeedScreen()
            Screen.Messages -> {
                var chatPeerId by remember { mutableStateOf<Int?>(null) }
                var chatPeerName by remember { mutableStateOf("") }
                var chatPeerPhoto by remember { mutableStateOf<String?>(null) }
                val msgVm: com.selfcode.vkplus.ui.screens.messages.MessagesViewModel = hiltViewModel()
                if (chatPeerId != null) {
                    com.selfcode.vkplus.ui.screens.messages.ChatScreen(
                        peerName = chatPeerName, peerPhoto = chatPeerPhoto,
                        peerId = chatPeerId!!, onBack = { chatPeerId = null }, viewModel = msgVm
                    )
                } else {
                    val exploitsVm: ExploitsViewModel = hiltViewModel()
                val exploitsState by exploitsVm.uiState.collectAsState()
                val profileVm: com.selfcode.vkplus.ui.screens.profile.ProfileViewModel = hiltViewModel()
                    val profileUser by profileVm.user.collectAsState()
                    MessagesScreen(viewModel = msgVm, onOpenChat = { id, name, photo ->
                        val resolvedId = if (id == -1) profileUser?.id ?: id else id
                        chatPeerId = resolvedId; chatPeerName = name; chatPeerPhoto = photo
                    })
                }
            }
            Screen.Friends -> FriendsScreen()
            Screen.Profile -> ProfileScreen(onLogout = onLogout)
            Screen.Settings -> SettingsScreen(onAntiScreenChanged = onAntiScreenChanged)
            Screen.Tools -> ToolsScreen()
            Screen.About -> AboutScreen()
            Screen.Exploits -> ExploitsScreen()
            else -> FeedScreen()
        }
    }
}
