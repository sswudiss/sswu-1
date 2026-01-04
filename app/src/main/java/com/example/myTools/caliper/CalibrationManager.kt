package com.example.myTools.caliper

import android.content.Context
import androidx.core.content.edit

object CalibrationManager {
    private const val PREF_NAME = "ruler_prefs"
    private const val KEY_CALIBRATION_FACTOR = "calibration_factor"

    // 讀取校準係數 (默認為 1.0，即不調整)
    fun loadFactor(context: Context): Float {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_CALIBRATION_FACTOR, 1.0f)
    }

    // 儲存校準係數
    fun saveFactor(context: Context, factor: Float) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putFloat(KEY_CALIBRATION_FACTOR, factor) }
    }
}