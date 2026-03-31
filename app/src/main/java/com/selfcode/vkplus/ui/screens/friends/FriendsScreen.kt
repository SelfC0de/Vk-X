package com.selfcode.vkplus.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.ui.theme.*

@Composable
fun FriendsScreen(viewModel: FriendsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Поиск друзей", color = OnSurfaceMuted) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = OnSurfaceMuted)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberBlue,
                unfocusedBorderColor = Divider,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                cursorColor = CyberBlue
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        if (!state.isLoading) {
            Text(
                text = "Друзья ${state.friends.size} · Онлайн ${state.onlineCount}",
                color = OnSurfaceMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CyberBlue
                )
                state.error != null -> Text(
                    state.error ?: "",
                    color = ErrorRed,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn {
                    items(state.filtered, key = { it.id }) { user ->
                        FriendRow(user)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRow(user: VKUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = user.photo100,
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant),
                contentScale = ContentScale.Crop
            )
            if (user.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(Background, CircleShape)
                        .padding(2.dp)
                        .background(CyberAccent, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(user.fullName, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            val sub = if (user.isOnline) "онлайн" else user.status?.takeIf { it.isNotBlank() } ?: ""
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    color = if (user.isOnline) CyberBlue else OnSurfaceMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
