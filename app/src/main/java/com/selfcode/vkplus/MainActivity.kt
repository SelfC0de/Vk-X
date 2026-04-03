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
import com.selfcode.vkplus.ui.screens.communities.CommunitiesScreen
import com.selfcode.vkplus.ui.screens.communities.CommunityScreen
import com.selfcode.vkplus.ui.screens.friends.FriendsScreen
import com.selfcode.vkplus.ui.screens.vkplus.VKPlusScreen
import com.selfcode.vkplus.ui.screens.feed.FeedScreen
import com.selfcode.vkplus.ui.screens.friends.FriendsScreen
import com.selfcode.vkplus.ui.screens.messages.MessagesScreen
import com.selfcode.vkplus.ui.screens.profile.ProfileScreen
import com.selfcode.vkplus.ui.screens.settings.SettingsScreen
import com.selfcode.vkplus.ui.screens.about.AboutScreen
import com.selfcode.vkplus.ui.screens.proxy.ProxyScreen
import com.selfcode.vkplus.ui.screens.communities.CommunitiesScreen
import com.selfcode.vkplus.ui.screens.communities.CommunityScreen
import com.selfcode.vkplus.ui.screens.search.SearchPeopleScreen
import com.selfcode.vkplus.ui.screens.photos.PhotosScreen
import com.selfcode.vkplus.ui.screens.profile.BlacklistScreen
import com.selfcode.vkplus.ui.screens.exploits.ExploitsScreen
import com.selfcode.vkplus.ui.screens.tools.ToolsScreen
import com.selfcode.vkplus.ui.theme.VKPlusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private fun handleDeepLink(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "vkplus" && data.host == "proxy") {
            // Intent received - ProxyScreen will handle via ViewModel
        }
    }

    @Inject lateinit var repository: VKRepository
    @Inject lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
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
fun AuthenticatedApp(repository: VKRepository, onLogout: () -> Unit, onAntiScreenChanged: (Boolean) -> Unit, onReloadAfterSwitch: () -> Unit = {}) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Feed) }
    var currentCommunityId by remember { mutableStateOf<Int?>(null) }
    var commChatPeerId by remember { mutableStateOf<Int?>(null) }
    var openProfileUserId by remember { mutableStateOf<String?>(null) }
    var commChatName by remember { mutableStateOf("") }
    var commChatPhoto by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<com.selfcode.vkplus.data.model.VKUser?>(null) }
    var showAccountSwitcher by remember { mutableStateOf(false) }
    var addingAccount by remember { mutableStateOf(false) }

    val exploitsVm: com.selfcode.vkplus.ui.screens.exploits.ExploitsViewModel = hiltViewModel()
    val exploitsState by exploitsVm.uiState.collectAsState()
    val profileVm: com.selfcode.vkplus.ui.screens.profile.ProfileViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        when (val r = repository.getCurrentUser()) {
            is VKResult.Success -> currentUser = r.data
            is VKResult.Error -> {}
        }
    }

    // User profile overlay from messages
    openProfileUserId?.let { uid ->
        com.selfcode.vkplus.ui.screens.profile.UserProfileScreen(
            userId = uid,
            onBack = { openProfileUserId = null },
            onWriteMessage = { peerId ->
                openProfileUserId = null
                commChatPeerId = peerId
                commChatName = ""
                commChatPhoto = null
                currentScreen = Screen.Messages
            }
        )
        return@AuthenticatedApp
    }

    // Account switcher overlay
    if (showAccountSwitcher) {
        com.selfcode.vkplus.auth.AccountSwitcherScreen(
            onSwitched = {
                showAccountSwitcher = false
                onReloadAfterSwitch()
            },
            onAddAccount = { showAccountSwitcher = false; addingAccount = true },
            onBack = { showAccountSwitcher = false }
        )
        return
    }

    // Add new account overlay - use shared authVm declared at top level
    if (addingAccount) {
        val addAuthVm: com.selfcode.vkplus.auth.AuthViewModel = hiltViewModel()
        com.selfcode.vkplus.ui.screens.AuthScreen(
            onTokenReceived = { uri ->
                addAuthVm.handleRedirectUri(uri)
                addingAccount = false
            },
            onQrAuthenticated = { addingAccount = false },
            onManualToken = { token ->
                addAuthVm.saveManualToken(token)
                addingAccount = false
            }
        )
        return
    }

    MainScaffold(
        onSwitchAccount = { showAccountSwitcher = true },
        onAddAccount = { addingAccount = true },
        isMirrorActive = exploitsState.mirrorActive && exploitsState.mirroredName.isNotBlank(),
        mirrorName = exploitsState.mirroredName,
        mirrorPhoto = exploitsState.mirroredPhoto,
        currentScreen = currentScreen,
        user = currentUser,
        onNavigate = { currentScreen = it }
    ) {
        when (currentScreen) {
            Screen.Feed -> FeedScreen()
            Screen.Messages -> {
                var chatPeerId by remember { mutableStateOf<Int?>(null) }
                var chatPeerName by remember { mutableStateOf("") }
                var chatPeerPhoto by remember { mutableStateOf<String?>(null) }
                // Open community messages if coming from CommunityScreen
                LaunchedEffect(commChatPeerId) {
                    commChatPeerId?.let { id ->
                        chatPeerId = id; chatPeerName = commChatName; chatPeerPhoto = commChatPhoto
                        commChatPeerId = null
                    }
                }
                val msgVm: com.selfcode.vkplus.ui.screens.messages.MessagesViewModel = hiltViewModel()
                val profileUser by profileVm.user.collectAsState()
                if (chatPeerId != null) {
                    com.selfcode.vkplus.ui.screens.messages.ChatScreen(
                        peerName = chatPeerName, peerPhoto = chatPeerPhoto,
                        peerId = chatPeerId!!, onBack = { chatPeerId = null },
                        onOpenProfile = { uid -> openProfileUserId = uid },
                        viewModel = msgVm
                    )
                } else {
                    MessagesScreen(viewModel = msgVm, onOpenChat = { id, name, photo ->
                        val resolvedId = if (id == -1) profileUser?.id ?: id else id
                        chatPeerId = resolvedId; chatPeerName = name; chatPeerPhoto = photo
                    })
                }
            }
            Screen.Friends -> FriendsScreen()
            Screen.Profile -> ProfileScreen(
                onLogout = onLogout,
                onNavigateToPhotos = { currentScreen = Screen.Photos },
                onNavigateToBlacklist = { currentScreen = Screen.Blacklist }
            )
            Screen.Settings -> SettingsScreen(onAntiScreenChanged = onAntiScreenChanged)
            Screen.Tools -> ToolsScreen()
            Screen.About -> AboutScreen()
            Screen.Proxy -> ProxyScreen()
            Screen.Exploits -> ExploitsScreen()
            Screen.Communities -> {
                if (currentCommunityId != null) {
                    CommunityScreen(
                        groupId = currentCommunityId!!,
                        onBack = { currentCommunityId = null },
                        onOpenMessages = { peerId, name, photo ->
                            currentScreen = Screen.Messages
                            currentCommunityId = null
                            commChatPeerId = peerId
                            commChatName = name
                            commChatPhoto = photo
                        }
                    )
                } else {
                    CommunitiesScreen(
                        onOpenCommunity = { id -> currentCommunityId = id }
                    )
                }
            }
            Screen.SearchPeople -> SearchPeopleScreen(onProfile = { currentScreen = Screen.Profile })
            Screen.Photos -> PhotosScreen()
            Screen.Blacklist -> BlacklistScreen()
            Screen.VKPlus -> VKPlusScreen()
            else -> FeedScreen()
        }
    }
}

