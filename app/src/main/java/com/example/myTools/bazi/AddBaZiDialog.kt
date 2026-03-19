package com.example.myTools.bazi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBaZiDialog(
    initialRecord: BaZiRecord? = null,
    onDismiss: () -> Unit,
    onSave: (BaZiRecord) -> Unit
) {
    // 改用 TextFieldValue 以便控制選取範圍 (Selection)
    var name by remember { mutableStateOf(TextFieldValue(initialRecord?.name ?: "")) }
    var year by remember { mutableStateOf(TextFieldValue(initialRecord?.year?.toString() ?: "1990")) }
    var month by remember { mutableStateOf(TextFieldValue(initialRecord?.month?.toString() ?: "1")) }
    var day by remember { mutableStateOf(TextFieldValue(initialRecord?.day?.toString() ?: "1")) }
    var hour by remember { mutableStateOf(TextFieldValue(initialRecord?.hour?.toString() ?: "12")) }
    var isLunar by remember { mutableStateOf(initialRecord?.isLunar ?: false) }

    val focusManager = LocalFocusManager.current
    
    val focusRequesterYear = remember { FocusRequester() }
    val focusRequesterMonth = remember { FocusRequester() }
    val focusRequesterDay = remember { FocusRequester() }
    val focusRequesterHour = remember { FocusRequester() }

    // 驗證狀態 (讀取 .text 屬性)
    val yearInt = year.text.toIntOrNull()
    val monthInt = month.text.toIntOrNull()
    val dayInt = day.text.toIntOrNull()
    val hourInt = hour.text.toIntOrNull()

    val isYearError = yearInt == null || yearInt !in 1900..2100
    val isMonthError = monthInt == null || monthInt !in 1..12
    
    // 農曆最大 30 天，公曆最大 31 天
    val maxDay = if (isLunar) 30 else 31
    val isDayError = dayInt == null || dayInt !in 1..maxDay
    
    val isHourError = hourInt == null || hourInt !in 0..23

    val inputTextStyle = TextStyle(fontSize = 18.sp)
    val labelTextStyle = TextStyle(fontSize = 14.sp)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(0.92f), // 控制對話框佔螢幕寬度的比例
        title = { 
            Text(
                if (initialRecord == null) "添加八字紀錄" else "編輯八字紀錄",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            ) 
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名 (選填)", style = labelTextStyle) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) name = name.copy(selection = TextRange(0, name.text.length)) },
                    singleLine = true,
                    textStyle = inputTextStyle,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusRequesterYear.requestFocus() })
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("農曆模式", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = isLunar, onCheckedChange = { isLunar = it })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("年", style = labelTextStyle) },
                        modifier = Modifier
                            .weight(1.2f)
                            .focusRequester(focusRequesterYear)
                            .onFocusChanged { if (it.isFocused) year = year.copy(selection = TextRange(0, year.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterMonth.requestFocus() }),
                        isError = isYearError,
                        supportingText = {
                            if (isYearError) Text("1900-2100", color = MaterialTheme.colorScheme.error)
                        }
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = {
                            val label = if (isLunar) "月 (${getLunarMonthName(monthInt ?: 0)})" else "月"
                            Text(label, style = labelTextStyle)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .focusRequester(focusRequesterMonth)
                            .onFocusChanged { if (it.isFocused) month = month.copy(selection = TextRange(0, month.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterDay.requestFocus() }),
                        isError = isMonthError,
                        supportingText = {
                            if (isMonthError) Text("1-12月", color = MaterialTheme.colorScheme.error)
                        }
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = {
                            val label = if (isLunar) "日 (${getLunarDayName(dayInt ?: 0)})" else "日"
                            Text(label, style = labelTextStyle)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .focusRequester(focusRequesterDay)
                            .onFocusChanged { if (it.isFocused) day = day.copy(selection = TextRange(0, day.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterHour.requestFocus() }),
                        isError = isDayError,
                        supportingText = {
                            if (isDayError) Text("1-${maxDay}日", color = MaterialTheme.colorScheme.error)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it },
                        label = { 
                            Text("時 (${getHourBranchName(hourInt ?: 0)})", style = labelTextStyle) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesterHour)
                            .onFocusChanged { if (it.isFocused) hour = hour.copy(selection = TextRange(0, hour.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        isError = isHourError,
                        supportingText = {
                            if (isHourError) Text("請輸入 0-23 之間的數字", color = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isYearError && !isMonthError && !isDayError && !isHourError,
                onClick = {
                    val finalName = if (name.text.isBlank()) "未命名" else name.text
                    val record = BaZiRecord(
                        id = initialRecord?.id ?: System.currentTimeMillis(),
                        name = finalName,
                        year = yearInt!!,
                        month = monthInt!!,
                        day = dayInt!!,
                        hour = hourInt!!,
                        minute = 0,
                        isLunar = isLunar
                    )
                    onSave(record)
                }
            ) {
                Text("保存", fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", fontSize = 16.sp) }
        }
    )
}

private fun getLunarMonthName(m: Int): String {
    val names = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "臘")
    return if (m in 1..12) "${names[m - 1]}月" else "${m}月"
}

private fun getLunarDayName(d: Int): String {
    val first = listOf("初", "十", "廿", "卅")
    val second = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    return when {
        d == 10 -> "初十"
        d == 20 -> "二十"
        d == 30 -> "三十"
        d in 1..30 -> "${first[(d - 1) / 10]}${second[(d - 1) % 10]}"
        else -> d.toString()
    }
}

private fun getHourBranchName(h: Int): String {
    return when (h) {
        23, 0 -> "子時"
        1, 2 -> "丑時"
        3, 4 -> "寅時"
        5, 6 -> "卯時"
        7, 8 -> "辰時"
        9, 10 -> "巳時"
        11, 12 -> "午時"
        13, 14 -> "未時"
        15, 16 -> "申時"
        17, 18 -> "酉時"
        19, 20 -> "戌時"
        21, 22 -> "亥時"
        else -> "${h}點"
    }
}
