package com.example.myTools.bazi

/**
 * 八字紀錄數據模型
 */
data class BaZiRecord(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val isLunar: Boolean = false,
    val isLeapMonth: Boolean = false, // 僅在農曆時有效
    val aiAnalysis: String? = null // AI 批注數據
)
