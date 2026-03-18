package com.example.myTools.birthday

import com.nlf.calendar.Lunar
import java.util.Calendar

/**
 * 獲取農曆月份名稱 (如：正月（1月）、二月、冬月（11月）、臘月（12月）)
 */
fun getLunarMonthName(month: Int): String {
    val names = arrayOf(
        "正月（1月）", "二月", "三月", "四月", "五月", "六月", 
        "七月", "八月", "九月", "十月", "冬月（11月）", "臘月（12月）"
    )
    return if (month in 1..12) names[month - 1] else "${month}月"
}

/**
 * 獲取農曆日期名稱 (如：初一、廿一、三十)
 */
fun getLunarDayName(day: Int): String {
    val chineseTen = arrayOf("初", "十", "廿", "三")
    val chineseNum = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")

    // Kotlin 的代碼優化建議。
    // 當 when 表達式中的所有條件都是對同一個變量進行相等性判斷時，
    // 建議將該變量作為 when 的參數（即 subject），這樣代碼會更簡潔、可讀性更好。
    return when (day) {
        10 -> "初十"
        20 -> "二十"
        30 -> "三十"
        else -> {
            val ten = day / 10
            val unit = day % 10
            if (unit == 0) chineseTen[ten] + chineseNum[9]
            else chineseTen[ten] + chineseNum[unit - 1]
        }
    }
}

/**
 * 根據農曆月日計算下一個公曆生日的 Calendar
 */
fun getNextBirthdayCalendar(lunarMonth: Int, lunarDay: Int): Calendar {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayLunar = Lunar.fromDate(today.time)
    var nextBirthdayLunar = Lunar.fromYmd(todayLunar.year, lunarMonth, lunarDay)
    var nextBirthdaySolar = nextBirthdayLunar.solar
    val targetCalendar = Calendar.getInstance()
    targetCalendar.set(nextBirthdaySolar.year, nextBirthdaySolar.month - 1, nextBirthdaySolar.day, 0, 0, 0)
    targetCalendar.set(Calendar.MILLISECOND, 0)

    if (targetCalendar.timeInMillis < today.timeInMillis) {
        nextBirthdayLunar = Lunar.fromYmd(todayLunar.year + 1, lunarMonth, lunarDay)
        nextBirthdaySolar = nextBirthdayLunar.solar
        targetCalendar.set(nextBirthdaySolar.year, nextBirthdaySolar.month - 1, nextBirthdaySolar.day, 0, 0, 0)
        targetCalendar.set(Calendar.MILLISECOND, 0)
    }
    return targetCalendar
}
