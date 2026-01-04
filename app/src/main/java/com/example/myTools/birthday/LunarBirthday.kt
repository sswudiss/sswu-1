package com.example.myTools.birthday

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nlf.calendar.Lunar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import androidx.core.content.edit

//農曆生日


data class BirthdayRecord(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val lunarMonth: Int,
    val lunarDay: Int,
    val remindList: List<Int> = listOf(1), // 天數列表
    // ★ 修改：改為時間列表，支援多個時間點
    val remindHours: List<Int> = listOf(9)
)

// 存儲管理器
object BirthdayManager {
    private const val PREF_NAME = "birthday_prefs"
    private const val KEY_LIST = "birthday_list"
    private val gson = Gson()

    // 在 BirthdayManager 物件內
    fun loadList(context: Context): List<BirthdayRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, null) ?: return emptyList()
        val type = object : TypeToken<List<BirthdayRecord>>() {}.type

        return try {
            val rawList: List<BirthdayRecord>? = gson.fromJson(json, type)
            // ★ 安全修復：如果讀出來的欄位是 null，強行給予預設值
            rawList?.map { record ->
                record.copy(
                    remindList = record.remindList,
                    remindHours = record.remindHours
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveList(context: Context, list: List<BirthdayRecord>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit { putString(KEY_LIST, json) }
        rescheduleAllAlarms(context, list)
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleBirthdayAlarm(context: Context, record: BirthdayRecord) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 雙重迴圈：針對每個「天數」的每個「時間」設定鬧鐘
        record.remindList.forEach { daysBefore ->
            record.remindHours.forEach { hour ->

                val nextBirthdayCal = getNextBirthdayCalendar(record.lunarMonth, record.lunarDay)
                val reminderCal = nextBirthdayCal.clone() as Calendar
                reminderCal.add(Calendar.DAY_OF_YEAR, -daysBefore)

                // 設定小時
                reminderCal.set(Calendar.HOUR_OF_DAY, hour)
                reminderCal.set(Calendar.MINUTE, 0)
                reminderCal.set(Calendar.SECOND, 0)

                if (reminderCal.timeInMillis < System.currentTimeMillis()) {
                    return@forEach // 這個時間點過期了，跳過
                }

                // 顯示文字
                val timeText = when (hour) {
                    in 5..11 -> "早上"
                    in 12..17 -> "下午"
                    else -> "晚上"
                }

                val msg = when (daysBefore) {
                    0 -> "今天是 ${record.name} 的農曆生日！"
                    1 -> "明天是 ${record.name} 的農曆生日"
                    else -> "${record.name} 的農曆生日還有 $daysBefore 天"
                }

                val intent = Intent(context, BirthdayReceiver::class.java).apply {
                    putExtra("name", record.name)
                    // 訊息範例：明天是 XX 生日 (下午 14:00 提醒)
                    putExtra("message", "$msg ($timeText $hour:00 提醒)")
                    putExtra("original_id", record.id)
                }

                // ID = (RecordID % 10萬) + (天數 * 10萬) + (小時 * 100)
                // 這樣可以保證同一筆資料，不同天、不同小時的鬧鐘 ID 都不一樣
                val uniqueRequestCode =
                    (record.id % 100000).toInt() + (daysBefore * 100000) + (hour * 100)

                val pendingIntent = PendingIntent.getBroadcast(
                    context, uniqueRequestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderCal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderCal.timeInMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    // 取消鬧鐘：需要遍歷所有可能的天數和小時
    fun cancelAlarm(context: Context, record: BirthdayRecord) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BirthdayReceiver::class.java)

        // 暴力清除所有可能的組合 (天數 0~30, 小時 0~23)
        for (d in 0..30) {
            for (h in 0..23) {
                val uniqueRequestCode = (record.id % 100000).toInt() + (d * 100000) + (h * 100)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, uniqueRequestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    private fun rescheduleAllAlarms(context: Context, list: List<BirthdayRecord>) {
        list.forEach { cancelAlarm(context, it) }
        list.forEach { scheduleBirthdayAlarm(context, it) }
    }
}


fun getNextBirthdayCalendar(lunarMonth: Int, lunarDay: Int): Calendar {
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val todayLunar = Lunar.fromDate(today.time)
    var nextBirthdayLunar = Lunar.fromYmd(todayLunar.year, lunarMonth, lunarDay)
    var nextBirthdaySolar = nextBirthdayLunar.solar

    val targetCalendar = Calendar.getInstance()
    targetCalendar.set(
        nextBirthdaySolar.year,
        nextBirthdaySolar.month - 1,
        nextBirthdaySolar.day,
        0,
        0,
        0
    )
    targetCalendar.set(Calendar.MILLISECOND, 0)

    if (targetCalendar.timeInMillis < today.timeInMillis) {
        nextBirthdayLunar = Lunar.fromYmd(todayLunar.year + 1, lunarMonth, lunarDay)
        nextBirthdaySolar = nextBirthdayLunar.solar
        targetCalendar.set(
            nextBirthdaySolar.year,
            nextBirthdaySolar.month - 1,
            nextBirthdaySolar.day,
            0,
            0,
            0
        )
        targetCalendar.set(Calendar.MILLISECOND, 0)
    }
    return targetCalendar
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarBirthdayScreen() {
    val context = LocalContext.current
    val birthdayList = remember { mutableStateListOf<BirthdayRecord>() }
    var showDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) BirthdayManager.saveList(context, birthdayList)
            else Toast.makeText(context, "未開啟通知權限，將無法收到生日提醒", Toast.LENGTH_LONG)
                .show()
        }
    )

    LaunchedEffect(Unit) {
        birthdayList.addAll(BirthdayManager.loadList(context))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun save() {
        BirthdayManager.saveList(context, birthdayList)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🎂 農曆生日管家",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD84315)
                    )
                },
                actions = {
                    IconButton(onClick = { sendTestNotification(context) }) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "測試通知",
                            tint = Color(0xFFD84315)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFF3E0))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFFFF5722),
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "新增") }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF3E0))
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
        ) {

            if (birthdayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("還沒有添加生日喔！", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val sortedList = birthdayList.sortedBy {
                        val nextCal = getNextBirthdayCalendar(it.lunarMonth, it.lunarDay)
                        val today = Calendar.getInstance()
                        nextCal.timeInMillis - today.timeInMillis
                    }
                    items(sortedList) { item ->
                        BirthdayCard(
                            record = item,
                            onDelete = {
                                BirthdayManager.cancelAlarm(context, item)
                                birthdayList.remove(item)
                                birthdayList.removeIf { it.id == item.id }
                                save()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddBirthdayDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, month, day, remindList, remindHours ->
                val newRecord = BirthdayRecord(
                    name = name,
                    lunarMonth = month,
                    lunarDay = day,
                    remindList = remindList,
                    remindHours = remindHours // 存列表
                )
                birthdayList.add(newRecord)
                save()
                showDialog = false
            }
        )
    }
}

@Composable
fun BirthdayCard(record: BirthdayRecord, onDelete: () -> Unit) {
    val nextCal = getNextBirthdayCalendar(record.lunarMonth, record.lunarDay)
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val diffMillis = nextCal.timeInMillis - today.timeInMillis
    val daysLeft = ceil(diffMillis / (1000.0 * 60 * 60 * 24)).toInt()

    val solarFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    val solarDateStr = solarFormat.format(nextCal.time)
    val weekDays = arrayOf("日", "一", "二", "三", "四", "五", "六")
    val weekStr = "星期${weekDays[nextCal.get(Calendar.DAY_OF_WEEK) - 1]}"

    // ★ 防護 1：確保 remindHours 不為 null
    val safeRemindHours = record.remindHours
    // ★ 防護 2：確保 remindList 不為 null
    val safeRemindList = record.remindList


    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Cake,
                contentDescription = null,
                tint = Color(0xFFFF5722),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "農曆： ${record.lunarMonth}月${record.lunarDay}日",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

//                Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "陽曆： $solarDateStr $weekStr",
                    fontSize = 16.sp,
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Medium
                )
//                }

                if (safeRemindList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // 顯示多個時間點
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Alarm,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        val timeStr = record.remindHours.sorted().joinToString(", ") { h ->
                            when (h) {
                                9 -> "上午9點"; 14 -> "下午2點"; 19 -> "晚上7點"; else -> "$h:00"
                            }
                        }
                        Text(
                            " $timeStr",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        record.remindList.sorted().forEach { days ->
                            val label = when (days) {
                                0 -> "當天"; 1 -> "1天前"; else -> "${days}天前"
                            }
                            Surface(
                                color = Color(0xFFE0F7FA),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    color = Color(0xFF006064),
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (daysLeft == 0) Text(
                    text = "今天!",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                else {
                    Text(text = "還有", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        text = "$daysLeft 天",
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "刪除",
                        tint = Color.LightGray
                    )
                }
            }
        }
    }
}

// 新增生日對話框 (時間也是複選)
@Composable
fun AddBirthdayDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, List<Int>, List<Int>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var monthInput by remember { mutableStateOf("") }
    var dayInput by remember { mutableStateOf("") }
    val selectedRemindDays = remember { mutableStateListOf(1) }

    // 時間改為複選，預設選9點
    val selectedRemindHours = remember { mutableStateListOf(9) }

    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        // 解除系統預設寬度限制
        properties = DialogProperties(usePlatformDefaultWidth = false),
        // 手動設定寬度為螢幕的 86%，讓內容更寬敞
        modifier = Modifier.fillMaxWidth(0.86f),
        title = { Text("新增農曆生日") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("姓名") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = monthInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) monthInput = it },
                        label = { Text("月") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = dayInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) dayInput = it },
                        label = { Text("日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // --- 時間選擇 (複選) ---
                Spacer(modifier = Modifier.height(16.dp))
                Text("提醒時間 (可多選)：", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                val timeOptions = listOf(9 to "上午9點", 14 to "下午2點", 19 to "晚上7點")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween // 讓選項平均分佈
                ) {
                    timeOptions.forEach { (hour, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)  // 每個選項佔 1/3 寬度
                                .clickable {
                                    if (selectedRemindHours.contains(hour)) selectedRemindHours.remove(
                                        hour
                                    )
                                    else selectedRemindHours.add(hour)
                                }
                        ) {
                            Checkbox(
                                checked = selectedRemindHours.contains(hour),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) selectedRemindHours.add(hour)
                                    else selectedRemindHours.remove(hour)
                                }
                            )
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                maxLines = 1,

                            )
                        }
                    }
                }

                // --- 天數選擇 ---
                Spacer(modifier = Modifier.height(8.dp))
                Text("提醒日期 (可多選)：", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                val dayOptions = listOf(0 to "當天", 1 to "1天前", 3 to "3天前", 7 to "7天前")
                Column {
                    dayOptions.chunked(2).forEach { rowOptions ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowOptions.forEach { (days, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (selectedRemindDays.contains(days)) selectedRemindDays.remove(
                                                days
                                            )
                                            else selectedRemindDays.add(days)
                                        }
                                ) {
                                    Checkbox(
                                        checked = selectedRemindDays.contains(days),
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) selectedRemindDays.add(days)
                                            else selectedRemindDays.remove(days)
                                        }
                                    )
                                    Text(
                                        text = label, fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = Color.Red,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val m = monthInput.toIntOrNull()
                val d = dayInput.toIntOrNull()
                if (name.isBlank()) errorText = "請輸入姓名"
                else if (m == null || m !in 1..12) errorText = "月份必須是 1-12"
                else if (d == null || d !in 1..30) errorText = "日期必須是 1-30"
                else onConfirm(
                    name,
                    m,
                    d,
                    selectedRemindDays.toList(),
                    selectedRemindHours.toList()
                )
            }) { Text("確定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

fun sendTestNotification(context: Context) {
    val intent = Intent(context, BirthdayReceiver::class.java).apply {
        putExtra("name", "測試員")
        putExtra("message", "這是一條測試通知，證明功能正常！")
        putExtra("id", 9999)
    }
    context.sendBroadcast(intent)
    Toast.makeText(context, "已發送測試廣播", Toast.LENGTH_SHORT).show()
}