package com.example.myTools

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.myTools.almanac.AlmanacScreen
import com.example.myTools.auspicious.AuspiciousQueryScreen
import com.example.myTools.birthday.LunarBirthdayScreen
import com.example.myTools.caliper.CaliperScreen
import com.example.myTools.luopan.LuopanScreen

@Composable
fun MainScreen() {
    // 0=黃曆, 1=吉日, 2=生日, 3=羅盤, 4=尺規
    var selectedIndex by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }

    // --- 判斷當前是否為深色模式頁面 (羅盤 或 尺規) ---
    val isDarkModePage = selectedIndex >= 3


    // --- 狀態欄控制 ---
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(selectedIndex) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)

        if (isDarkModePage) {
            // 3(羅盤) & 4(尺規)：隱藏狀態欄，全螢幕，文字白色(雖然隱藏了但設一下以防滑出)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.isAppearanceLightStatusBars = false
        } else {
            // 0, 1, 2：顯示狀態欄，文字黑色
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.isAppearanceLightStatusBars = true
        }
    }

    // --- MD3 動態色彩邏輯 ---
    // 深色頁面用黑色導航欄，淺色頁面用米白導航欄
    val targetBarColor = if (isDarkModePage) Color.Black else Color(0xFFFDFDF6)
    val navBarColor by animateColorAsState(
        targetValue = targetBarColor,
        animationSpec = tween(500),
        label = "BarColor"
    )

    // 深色頁面字變白，淺色頁面字變黑
    val targetContentColor = if (isDarkModePage) Color.White else Color.Black
    val navContentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(500),
        label = "ContentColor"
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = navBarColor,
                contentColor = navContentColor,
                tonalElevation = 0.dp
            ) {
                // 0: 黃曆
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedIndex == 0) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            "黃曆"
                        )
                    },
                    label = { Text("黃曆") },
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFB71C1C),
                        selectedTextColor = Color(0xFFB71C1C),
                        indicatorColor = Color(0xFFFFCDD2),
                        unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                        unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                    )
                )

                // 1: 吉日 (新)
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedIndex == 1) Icons.Filled.EventAvailable else Icons.Outlined.EventAvailable,
                            "吉日"
                        )
                    },
                    label = { Text("吉日") },
                    selected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2E7D32), // 綠色代表吉利
                        selectedTextColor = Color(0xFF2E7D32),
                        indicatorColor = Color(0xFFC8E6C9),    // 淺綠
                        unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                        unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                    )
                )

                // 2: 生日
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedIndex == 2) Icons.Filled.Cake else Icons.Outlined.Cake,
                            "生日"
                        )
                    },
                    label = { Text("生日") },
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE64A19),
                        selectedTextColor = Color(0xFFE64A19),
                        indicatorColor = Color(0xFFFFCCBC),
                        unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                        unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                    )
                )

                //3:羅盤
                NavigationBarItem(
                    // 改圖標
                    icon = {
                        Icon(Icons.Outlined.Explore, contentDescription = "羅盤")
                    }, // 需要 import Icons.Default.Explore
                    label = { Text("羅盤") },
                    selected = selectedIndex == 3,
                    onClick = { selectedIndex = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFD700), // 金色
                        selectedTextColor = Color(0xFFFFD700),
                        indicatorColor = Color(0xFF37474F),    // 深灰背景
                        // 未選中時：跟隨 navContentColor (因為是深色頁面，所以是白色)，但調淡一點
                        unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                        unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                    )
                )

                // 4: 尺規
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedIndex == 3) Icons.Filled.Straighten else Icons.Outlined.Straighten,
                            "尺規"
                        )
                    },
                    label = { Text("尺規") },
                    selected = selectedIndex == 4,
                    onClick = { selectedIndex = 4 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF2196F3),
                        unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                        unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedIndex) {
                0 -> AlmanacScreen(modifier = Modifier.fillMaxSize())
                1 -> AuspiciousQueryScreen()
                2 -> LunarBirthdayScreen()
                3 -> LuopanScreen()
                4 -> CaliperScreen()
            }
        }
    }
}