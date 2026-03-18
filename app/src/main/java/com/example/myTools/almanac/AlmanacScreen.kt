package com.example.myTools.almanac

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myTools.auspicious.AuspiciousQueryScreen
import com.example.myTools.settings.AppSettingsDialog
import com.example.myTools.ui.CommonTopBar
import com.nlf.calendar.Lunar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
fun AlmanacScreen(modifier: Modifier = Modifier) {
    val today = remember { Date() }
    val lunar = remember { Lunar.fromDate(today) }

    // 節氣計算
    val currentJieQiObj = lunar.getPrevJieQi(true)
    val currentTermName = currentJieQiObj.name
    val nextJieQiObj = lunar.getNextJieQi(false)
    val nextTermName = nextJieQiObj.name

    val daysSince = remember {
        val todayCal = Calendar.getInstance().apply {
            time = today; set(Calendar.HOUR_OF_DAY, 0); set(
            Calendar.MINUTE,
            0
        ); set(
            Calendar.SECOND,
            0
        ); set(Calendar.MILLISECOND, 0)
        }
        val currTermCal = Calendar.getInstance().apply {
            val s = currentJieQiObj.solar
            set(s.year, s.month - 1, s.day, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        ceil((todayCal.timeInMillis - currTermCal.timeInMillis) / (1000.0 * 3600 * 24)).toInt() + 1
    }

    // 生肖年表
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val zodiacList = remember(currentYear) {
        (-6..6).map { i ->
            val year = currentYear + i
            val l = Lunar.fromYmd(year, 6, 1)
            year to l.yearShengXiao
        }
    }

    // 狀態控制
    var showTermDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAuspiciousFullScreen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "黃曆",
                onSettingsClick = { showSettingsDialog = true },
                containerColor = Color(0xFFFDF5E6)
            )
        },
        containerColor = Color(0xFFFDF5E6)
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 農曆大卡片 ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${lunar.monthInChinese}月 ${lunar.yearInGanZhi}年",
                        fontSize = 26.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = lunar.dayInChinese,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "【屬${lunar.yearShengXiao}】",
                        fontSize = 20.sp,
                        color = Color.Yellow,
                        textAlign = TextAlign.Center
                    )

                    // --- 新曆日期與星期 ---
                    Text(
                        text = SimpleDateFormat(
                            "yyyy年MM月dd日 EEEE",
                            Locale.TRADITIONAL_CHINESE
                        ).format(today),
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // --- 宜忌卡片 ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                YiJiRow(title = "宜", items = lunar.dayYi, color = Color(0xFF2E7D32))
                YiJiRow(title = "忌", items = lunar.dayJi, color = Color(0xFFC62828))
            }

            Spacer(modifier = Modifier.height(26.dp))

            // --- 節氣詳情 ---
            Surface(
                color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTermDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE0F2F1), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Eco,
                            contentDescription = null,
                            tint = Color(0xFF00695C),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentTermName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(color = Color(0xFF5D4037), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "第${daysSince}天",
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(text = "即將到來：$nextTermName")
                    }
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- 生肖年表 ---
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val screenWidth = maxWidth
                val itemWidth = 75.dp
                val density = LocalDensity.current
                val zodiacListState = rememberLazyListState()
                LaunchedEffect(key1 = currentYear) {
                    zodiacListState.scrollToItem(
                        index = 6,
                        scrollOffset = -with(density) {
                            ((screenWidth - itemWidth) / 2).toPx().toInt()
                        })
                }
                LazyRow(
                    state = zodiacListState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(zodiacList) { _, (year, shengXiao) ->
                        ZodiacCard(
                            year = year,
                            shengXiao = shengXiao,
                            isCurrent = year == currentYear
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // --- 吉日按鈕 ---
            Button(
                onClick = { showAuspiciousFullScreen = true },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(27.dp),
                elevation = ButtonDefaults.buttonElevation(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("查看吉日 (Auspicious Days)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }


            // --- 對話框內容 ---
            if (showTermDialog) {
                Dialog(
                    onDismissRequest = { showTermDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "節氣詳解",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentTermName,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB71C1C)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = SolarTermData.getDescription(currentTermName),
                                fontSize = 20.sp,
                                lineHeight = 32.sp,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showTermDialog = false },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFB71C1C
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("確定", color = Color.White, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            if (showSettingsDialog) AppSettingsDialog(onDismiss = { showSettingsDialog = false })

            // --- 全螢幕吉日查詢 ---
            if (showAuspiciousFullScreen) {
                Dialog(
                    onDismissRequest = { showAuspiciousFullScreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false) // 關鍵：允許全螢幕
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFFDFDF6)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 頂部關閉按鈕列
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { showAuspiciousFullScreen = false }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "關閉",
                                        tint = Color.Gray
                                    )
                                }
                            }
                            // 嵌入原本的吉日查詢頁面
                            AuspiciousQueryScreen()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YiJiRow(title: String, items: List<String>, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(color, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 4
        ) {
            val displayItems = items.take(8)
            if (displayItems.isEmpty()) {
                Text(text = "無", color = Color.Gray, fontSize = 16.sp)
            } else {
                displayItems.forEach { item ->
                    Text(
                        text = item,
                        fontSize = 20.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ZodiacCard(year: Int, shengXiao: String, isCurrent: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isCurrent) Color(0xFFB71C1C) else Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.width(75.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = year.toString(),
                fontSize = 14.sp,
                color = if (isCurrent) Color.White else Color.Gray
            )
            Text(
                text = shengXiao,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) Color(0xFFFFD700) else Color.Black
            )
        }
    }
}
