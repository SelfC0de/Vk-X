package com.selfcode.vkplus.ui.screens.vkplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfcode.vkplus.ui.screens.exploits.ExploitsScreen
import com.selfcode.vkplus.ui.screens.tools.ToolsScreen
import com.selfcode.vkplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VKPlusScreen() {
    val tabs = listOf("Exploits", "Инструменты")
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = Surface,
            contentColor = CyberAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tab]),
                    color = CyberAccent
                )
            }
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = {
                        Text(title, fontSize = 14.sp,
                            fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (tab == i) CyberAccent else OnSurfaceMuted)
                    }
                )
            }
        }

        when (tab) {
            0 -> ExploitsScreen()
            1 -> ToolsScreen()
        }
    }
}
