package com.example.myTools.birthday

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BirthdayCard(
    record: BirthdayRecord,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val nextCal = getNextBirthdayCalendar(record.lunarMonth, record.lunarDay)
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffMillis = nextCal.timeInMillis - today.timeInMillis
    val daysLeft = ceil(diffMillis / (1000.0 * 60 * 60 * 24)).toInt()
    val solarDateStr = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(nextCal.time)
    val weekStr = "星期${
        arrayOf(
            "日",
            "一",
            "二",
            "三",
            "四",
            "五",
            "六"
        )[nextCal.get(Calendar.DAY_OF_WEEK) - 1]
    }"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier.combinedClickable(
            onClick = { /* 預留查看詳情 */ },
            onLongClick = onEdit
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Cake,
                null,
                tint = Color(0xFFFF5722),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "農曆： ${getLunarMonthName(record.lunarMonth)}${getLunarDayName(record.lunarDay)}",
                    fontSize = 18.sp
                )
                Text(
                    text = "陽曆： $solarDateStr $weekStr",
                    fontSize = 18.sp
                )
                if (record.remindList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Alarm,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        val timeStr = record.remindHours.sorted().joinToString(", ") { h ->
                            when (h) {
                                9 -> "上午9點"; 14 -> "下午2點"; 19 -> "晚上7點"; else -> "$h:00"
                            }
                        }
                        Text(
                            " $timeStr",
                            fontSize = 16.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        record.remindList.sorted().forEach { days ->
                            Surface(color = Color(0xFFE0F7FA), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = if (days == 0) "當天" else "${days}天前",
                                    fontSize = 14.sp,
                                    color = Color(0xFF006064),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (daysLeft == 0) {
                Text("今天!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            } else {
                Text("還有$daysLeft 天", fontSize = 18.sp)
            }
            IconButton(onClick = onDeleteRequest, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}
