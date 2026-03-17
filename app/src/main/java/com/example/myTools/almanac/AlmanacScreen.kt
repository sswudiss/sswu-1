package com.example.myTools.almanac

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myTools.almanac.SolarTermData
import com.nlf.calendar.Lunar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil


// 黃曆頁面 (Almanac Screen) - 使用 lunar 庫

@Composable
fun AlmanacScreen(modifier: Modifier = Modifier) {
    // 1. 獲取時間與農曆物件
    val today = remember { Date() }
    val lunar = remember { Lunar.fromDate(today) }

    // 2. 節氣計算邏輯
    val currentJieQiObj = lunar.getPrevJieQi(true)
    val currentTermName = currentJieQiObj.name

    val nextJieQiObj = lunar.getNextJieQi(false)
    val nextTermName = nextJieQiObj.name

    // 計算距離下個節氣還有幾天
    val daysLeft = remember {
        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)

        val nextTermCal = Calendar.getInstance()
        val nextSolar = nextJieQiObj.solar
        nextTermCal.set(nextSolar.year, nextSolar.month - 1, nextSolar.day, 0, 0, 0)
        nextTermCal.set(Calendar.MILLISECOND, 0)

        val diffMillis = nextTermCal.timeInMillis - todayCal.timeInMillis
        ceil(diffMillis / (1000.0 * 3600 * 24)).toInt()
    }

    // 計算當前節氣已經過了幾天
    val daysSince = remember {
        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)

        val currentTermCal = Calendar.getInstance()
        val currSolar = currentJieQiObj.solar
        currentTermCal.set(currSolar.year, currSolar.month - 1, currSolar.day, 0, 0, 0)
        currentTermCal.set(Calendar.MILLISECOND, 0)

        val diffMillis = todayCal.timeInMillis - currentTermCal.timeInMillis
        ceil(diffMillis / (1000.0 * 3600 * 24)).toInt() + 1
    }

    // 3. 十二生肖年表邏輯 - 前後6年 (共13年)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val zodiacList = remember(currentYear) {
        (-6..6).map { i ->
            val year = currentYear + i
            val l = Lunar.fromYmd(year, 6, 1)
            year to l.yearShengXiao
        }
    }

    // 對話框開關
    var showTermDialog by remember { mutableStateOf(false) }

    // 4. 主畫面佈局
    Column(
        modifier = modifier
            .background(Color(0xFFFDF5E6))
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 公曆日期 ---
        Text(
            text = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.TRADITIONAL_CHINESE).format(today),
            fontSize = 24.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- 農曆大卡片 ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = lunar.dayInChinese, fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "${lunar.monthInChinese}月 ${lunar.yearInGanZhi}年", fontSize = 24.sp, color = Color.White.copy(alpha = 0.9f))
                Text(text = "【屬${lunar.yearShengXiao}】", fontSize = 18.sp, color = Color(0xFFFFD700), modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 宜忌卡片 ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            YiJiCard(title = "宜", items = lunar.dayYi, color = Color(0xFF2E7D32))
            YiJiCard(title = "忌", items = lunar.dayJi, color = Color(0xFFC62828))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 節氣詳情卡片 ---
        Surface(
            color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().clickable { showTermDialog = true }
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).background(Color(0xFFE0F2F1), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Eco, contentDescription = null, tint = Color(0xFF00695C))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = currentTermName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color(0xFF5D4037), shape = RoundedCornerShape(6.dp)) {
                            Text(text = "第${daysSince}天", fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(text = SolarTermData.getDescription(currentTermName).replace("\n", " ").take(30) + "...", fontSize = 14.sp, color = Color.Gray, maxLines = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "距離 $nextTermName 還有 $daysLeft 天", fontSize = 15.sp, color = Color(0xFF795548))
                }
                Icon(Icons.Default.Info, contentDescription = "詳情", tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 十二生肖年表 (前後6年 & 自動居中) ---
        Text(
            text = "十二生肖年表 (最近前後6年)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val screenWidth = maxWidth
            val itemWidth = 80.dp
            val density = LocalDensity.current
            val zodiacListState = rememberLazyListState()

            LaunchedEffect(key1 = currentYear) {
                val centerOffset = with(density) { ((screenWidth - itemWidth) / 2).toPx().toInt() }
                // index 6 是中間 (對應於 -6..6 中的 0，即今年)
                zodiacListState.scrollToItem(index = 6, scrollOffset = -centerOffset)
            }

            LazyRow(
                state = zodiacListState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(zodiacList) { index, (year, shengXiao) ->
                    ZodiacCard(year = year, shengXiao = shengXiao, isCurrent = year == currentYear)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. 節氣詳情對話框
        if (showTermDialog) {
            AlertDialog(
                onDismissRequest = { showTermDialog = false },
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB71C1C)) },
                title = { Text(text = "節氣詳解", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(text = "當前節氣：$currentTermName", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = SolarTermData.getDescription(currentTermName), fontSize = 18.sp, lineHeight = 22.sp, color = Color.Black.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "即將到來：$nextTermName", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = SolarTermData.getDescription(nextTermName), fontSize = 18.sp, lineHeight = 22.sp, color = Color.Black.copy(alpha = 0.8f))
                    }
                },
                confirmButton = { TextButton(onClick = { showTermDialog = false }) { Text("確定", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold) } },
                containerColor = Color(0xFFFFF8E1)
            )
        }
    }
}

@Composable
fun YiJiCard(title: String, items: List<String>, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.width(160.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(40.dp).background(color, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            val displayItems = items.take(6)
            if (displayItems.isEmpty()) {
                Text(text = "無", color = Color.Gray, fontSize = 18.sp)
            } else {
                displayItems.forEach { item ->
                    Text(text = item, fontSize = 18.sp, color = Color.Black.copy(alpha = 0.8f), modifier = Modifier.padding(vertical = 2.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ZodiacCard(year: Int, shengXiao: String, isCurrent: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isCurrent) Color(0xFFB71C1C) else Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.width(80.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = year.toString(), fontSize = 14.sp, color = if (isCurrent) Color.White else Color.Gray, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = shengXiao, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isCurrent) Color(0xFFFFD700) else Color.Black)
        }
    }
}
