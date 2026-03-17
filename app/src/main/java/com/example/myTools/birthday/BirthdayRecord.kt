package com.example.myTools.birthday

/**
 * 生日紀錄數據模型
 */
data class BirthdayRecord(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val lunarMonth: Int,
    val lunarDay: Int,
    val remindList: List<Int> = listOf(1), // 提醒天數 (如：0=當天, 1=1天前)
    val remindHours: List<Int> = listOf(9)  // 提醒小時 (如：9=上午9點)
)
