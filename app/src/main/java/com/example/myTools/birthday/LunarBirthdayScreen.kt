package com.example.myTools.birthday

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/*
* 1.BirthdayRecord.kt：純數據模型。
2.BirthdayUtils.kt：包含農曆名稱轉換及日期計算邏輯。
3.BirthdayManager.kt：負責數據持久化（SharedPreferences）及鬧鐘管理。
4.BirthdayCard.kt：獨立的列表卡片組件，支援長按修改與刪除確認。
5.AddBirthdayDialog.kt：獨立的對話框組件，支援新增與編輯模式。
6.LunarBirthdayScreen.kt：主頁面入口，負責組合以上所有模組。
* */


/**
 * 農曆生日提醒主頁面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarBirthdayScreen() {
    val context = LocalContext.current

    // 持久化數據狀態
    var birthdayList by remember { mutableStateOf(BirthdayManager.loadList(context)) }

    // UI 控制狀態
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<BirthdayRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<BirthdayRecord?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("農曆生日提醒", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        // 發送測試通知廣播
                        val intent = Intent(context, BirthdayReceiver::class.java).apply {
                            putExtra("name", "測試提醒")
                            putExtra("message", "通知功能測試成功！🎉")
                            putExtra("id", 999)
                        }
                        context.sendBroadcast(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "測試通知",
                            tint = Color(0xFFFF5722)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFDFDF6)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRecord = null
                    showAddDialog = true
                },
                containerColor = Color(0xFFFF5722),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
        },
        containerColor = Color(0xFFFDFDF6)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (birthdayList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("尚未添加生日紀錄", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(birthdayList, key = { it.id }) { record ->
                        BirthdayCard(
                            record = record,
                            onEdit = { editingRecord = record },
                            onDeleteRequest = { recordToDelete = record }
                        )
                    }
                }
            }

            // 新增或編輯對話框
            if (showAddDialog || editingRecord != null) {
                AddBirthdayDialog(
                    initialRecord = editingRecord,
                    onDismiss = {
                        showAddDialog = false
                        editingRecord = null
                    },
                    onConfirm = { name, month, day, reminds, hours ->
                        val newList = birthdayList.toMutableList()
                        if (editingRecord != null) {
                            // 編輯模式
                            val index = newList.indexOfFirst { it.id == editingRecord!!.id }
                            if (index != -1) {
                                newList[index] = editingRecord!!.copy(
                                    name = name,
                                    lunarMonth = month,
                                    lunarDay = day,
                                    remindList = reminds,
                                    remindHours = hours
                                )
                            }
                        } else {
                            // 新增模式
                            newList.add(
                                BirthdayRecord(
                                    name = name,
                                    lunarMonth = month,
                                    lunarDay = day,
                                    remindList = reminds,
                                    remindHours = hours
                                )
                            )
                        }

                        // 保存數據並更新 UI
                        BirthdayManager.saveList(context, newList)
                        birthdayList = newList

                        showAddDialog = false
                        editingRecord = null
                    }
                )
            }

            // 刪除確認對話框
            if (recordToDelete != null) {
                AlertDialog(
                    onDismissRequest = { recordToDelete = null },
                    title = { Text("刪除提醒") },
                    text = {
                        Text(
                            "要刪除 ${recordToDelete!!.name} 的生日提醒嗎？",
                            fontSize = 18.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val newList = birthdayList.filter { it.id != recordToDelete!!.id }
                                BirthdayManager.saveList(context, newList)
                                birthdayList = newList
                                recordToDelete = null
                            }
                        ) {
                            Text("刪除", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { recordToDelete = null }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}
