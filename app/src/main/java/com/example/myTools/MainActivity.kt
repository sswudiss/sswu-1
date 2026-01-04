package com.example.myTools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myTools.ui.theme.RulerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ★ MD3 標準寫法 ★：一行搞定全螢幕穿透
        //如果報紅，請檢查 build.gradle 中的 androidx.activity:activity-compose 版本是否大於 1.8.0
        enableEdgeToEdge()

        setContent {
            RulerTheme {
                MainScreen()
            }
        }
    }
}