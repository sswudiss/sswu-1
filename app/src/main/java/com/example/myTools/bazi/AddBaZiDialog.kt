package com.example.myTools.bazi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBaZiDialog(
    initialRecord: BaZiRecord? = null,
    onDismiss: () -> Unit,
    onSave: (BaZiRecord) -> Unit
) {
    var name by remember { mutableStateOf(initialRecord?.name ?: "") }
    var year by remember { mutableStateOf(initialRecord?.year?.toString() ?: "1990") }
    var month by remember { mutableStateOf(initialRecord?.month?.toString() ?: "1") }
    var day by remember { mutableStateOf(initialRecord?.day?.toString() ?: "1") }
    var hour by remember { mutableStateOf(initialRecord?.hour?.toString() ?: "12") }
    var minute by remember { mutableStateOf(initialRecord?.minute?.toString() ?: "0") }
    var isLunar by remember { mutableStateOf(initialRecord?.isLunar ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRecord == null) "添加八字紀錄" else "編輯八字紀錄") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("農曆模式")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = isLunar, onCheckedChange = { isLunar = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("年") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = { Text("月") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("日") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it },
                        label = { Text("時") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it },
                        label = { Text("分") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val record = BaZiRecord(
                            id = initialRecord?.id ?: System.currentTimeMillis(),
                            name = name,
                            year = year.toIntOrNull() ?: 1990,
                            month = month.toIntOrNull() ?: 1,
                            day = day.toIntOrNull() ?: 1,
                            hour = hour.toIntOrNull() ?: 12,
                            minute = minute.toIntOrNull() ?: 0,
                            isLunar = isLunar
                        )
                        onSave(record)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
