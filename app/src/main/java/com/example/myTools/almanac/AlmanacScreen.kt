package com.example.myTools.almanac

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myTools.auspicious.PengZuData
import com.example.myTools.auspicious.SolarTermData
import com.nlf.calendar.Lunar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil


//黃曆頁面 (Almanac Screen) - 使用 lunar 庫

@Composable
fun AlmanacScreen(modifier: Modifier = Modifier) {
    // 1. 獲取時間與農曆物件
    val today = remember { Date() }
    val lunar = remember { Lunar.fromDate(today) }

    // 節氣計算邏輯
    // A. 獲取"當前"所處的節氣 (如果今天是冬至，就是冬至；如果過了3天，還是冬至)
    // getPrevJieQi(true) : true 表示如果今天剛好是節氣，也包含在內
    val currentJieQiObj = lunar.getPrevJieQi(true)
    val currentTermName = currentJieQiObj.name

    // B. 獲取"下一個"節氣
    // getNextJieQi(false): false 表示不包含今天，找未來的第一個
    val nextJieQiObj = lunar.getNextJieQi(false)
    val nextTermName = nextJieQiObj.name

    // C. 計算距離下個節氣還有幾天
    // 我們利用 Calendar 來計算兩個日期的天數差
    val daysLeft = remember {
        val todayCal = Calendar.getInstance()
        // 清除時分秒，只比對日期
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)

        val nextTermCal = Calendar.getInstance()
        val nextSolar = nextJieQiObj.solar
        // 注意：Solar 的月份是 1-12，Calendar 是 0-11
        nextTermCal.set(nextSolar.year, nextSolar.month - 1, nextSolar.day, 0, 0, 0)
        nextTermCal.set(Calendar.MILLISECOND, 0)

        val diffMillis = nextTermCal.timeInMillis - todayCal.timeInMillis
        ceil(diffMillis / (1000.0 * 3600 * 24)).toInt()
    }

    // D. 計算當前節氣已經過了幾天 (今天算第1天)
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
        // 差異天數 + 1 (當天算第1天)
        ceil(diffMillis / (1000.0 * 3600 * 24)).toInt() + 1
    }

    // 1. 十二值神 (例如：天刑)
    val tianShen = lunar.dayTianShen
    // 2. 黃道/黑道 (例如：黑道)
    val tianShenType = lunar.dayTianShenType
    // 3. 判斷顏色：黃道用金/紅，黑道用黑/灰
    val isHuangDao = tianShenType == "黃道"
    val shenColor = if (isHuangDao) Color(0xFFFFD700) else Color(0xFFBDBDBD) // 金色 vs 灰色
    // 判斷今天是否剛好是節氣交接日 (daysSince == 1)
    val specialAdvice = NiHaiXiaData.getAdvice(currentTermName, daysSince == 1)


    // 對話框開關
    var showTermDialog by remember { mutableStateOf(false) }   // 節氣對話框
    var showPengZuDialog by remember { mutableStateOf(false) } // 彭祖百忌對話框

    // 4. 主畫面佈局
    Column(
        modifier = modifier
            .background(Color(0xFFFDF5E6)) // 米黃色背景 (MD3: 背景色延伸到狀態欄)
            .windowInsetsPadding(WindowInsets.statusBars) // 內容避開狀態欄
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 公曆日期 ---
        Text(
            text = SimpleDateFormat(
                "yyyy年MM月dd日 EEEE",
                Locale.TRADITIONAL_CHINESE
            ).format(today),
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
                // 農曆日期
                Text(
                    text = lunar.dayInChinese,
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${lunar.monthInChinese}月 ${lunar.yearInGanZhi}年",
                    fontSize = 24.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = "【屬${lunar.yearShengXiao}】",
                    fontSize = 18.sp,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, shenColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$tianShen $tianShenType", // 顯示 "天刑 黑道"
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = shenColor
                        )
                    }
                }

                // --- ★ 新增：倪師/名師 叮嚀卡片 (如果有建議才顯示) ★ ---
                if (specialAdvice != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), // 淺橘色警告底
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "注意",
                                tint = Color(0xFFE65100), // 深橘紅
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = specialAdvice,
                                fontSize = 16.sp,
                                color = Color(0xFFBF360C), // 深褐色文字
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 宜忌卡片 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            YiJiCard(title = "宜", items = lunar.dayYi, color = Color(0xFF2E7D32))
            YiJiCard(title = "忌", items = lunar.dayJi, color = Color(0xFFC62828))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 節氣詳情 (可點擊) ---
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTermDialog = true } // 點擊開啟節氣對話框
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 左側圖標
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE0F2F1), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Eco, contentDescription = null, tint = Color(0xFF00695C))
                }

                Spacer(modifier = Modifier.width(18.dp))

                // 中間文字資訊
                Column(modifier = Modifier.weight(1f)) {
                    // 第一行：當前節氣 + 第幾天
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentTermName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Surface(
                            color = Color(0xFF5D4037),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "第${daysSince}天",
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 第二行：距離下一個節氣
                    Text(
                        text = "距離 $nextTermName 還有 $daysLeft 天",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }

                // 右側 Info 圖標
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "詳情",
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }


        Spacer(modifier = Modifier.height(12.dp))

        // --- 彭祖百忌 (可點擊) ---
        Surface(
            color = Color(0xFFEFEBE9), // 淺褐色背景
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPengZuDialog = true } // 點擊開啟彭祖對話框
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "彭祖百忌",
                    fontSize = 20.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 顯示原文
                Text(
                    text = "${lunar.pengZuGan}  ${lunar.pengZuZhi}",
                    fontSize = 18.sp,
                    color = Color(0xFF4E342E),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("點擊查看白話詳解", fontSize = 16.sp, color = Color.Gray)
                }
            }

        }


        // 1. 節氣詳情對話框
        if (showTermDialog) {
            AlertDialog(
                onDismissRequest = { showTermDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFB71C1C)
                    )
                },
                title = {
                    Text(
                        text = currentTermName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C)
                    )
                },
                text = {
                    // 從 SolarTermData 獲取
                    Text(
                        text = SolarTermData.getDescription(currentTermName),
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = Color.Black.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showTermDialog = false }) {
                        Text("關閉", color = Color(0xFF8BC34A))
                    }
                },
                containerColor = Color(0xFFFFF8E1)
            )
        }

        // 2. 彭祖百忌詳情對話框
        if (showPengZuDialog) {
            AlertDialog(
                onDismissRequest = { showPengZuDialog = false },
                title = {
                    Text(
                        text = "彭祖百忌詳解",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                },
                text = {
                    // 從 PengZuData 獲取
                    Text(
                        text = PengZuData.getExplanation(lunar.dayGan, lunar.dayZhi),
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = Color.Black.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPengZuDialog = false }) {
                        Text("了解", color = Color(0xFF8BC34A))
                    }
                },
                containerColor = Color(0xFFEFEBE9)
            )
        }
    }
}

    // 輔助組件：宜忌卡片
    @Composable
    fun YiJiCard(title: String, items: List<String>, color: Color) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.width(160.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val displayItems = items.take(6)
                if (displayItems.isEmpty()) {
                    Text(text = "無", color = Color.Gray, fontSize = 20.sp)
                } else {
                    displayItems.forEach { item ->
                        Text(
                            text = item,
                            fontSize = 20.sp,
                            color = Color.Black.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 2.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }