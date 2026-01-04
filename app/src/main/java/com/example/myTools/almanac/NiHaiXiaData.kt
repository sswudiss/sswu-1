package com.example.myTools.almanac


object NiHaiXiaData {

    /**
     * 根據 節氣名稱 或 特殊日子 獲取禁忌建議
     * @param termName 當前節氣名稱 (例如 "冬至")
     * @param isExactDay 今天是否剛好是該節氣交接日 (true/false)
     */
    fun getAdvice(termName: String, isExactDay: Boolean): String? {
        // 1. 優先匹配需要「當天」才生效的禁忌
        if (isExactDay) {
            val exactAdvice = exactDayMap[termName]
            if (exactAdvice != null) return exactAdvice
        }

        // 2. 其次匹配該節氣期間的通用養生建議
        return generalMap[termName]
    }

    // --- 特定日子的嚴格禁忌 (通常指交節氣當天) ---
    private val exactDayMap = mapOf(
        "冬至" to "【倪師叮嚀：冬至一陽生】\n今日乃陰極之至，陽氣始生。最忌行房、縱慾，否則極大損傷陽氣。宜早睡，靜養，吃湯圓/餃子補陽。",
        "夏至" to "【倪師叮嚀：夏至一陰生】\n今日陽極之至，陰氣始生。切忌行房，宜清淡飲食，避免大汗淋漓損傷心氣。",
        "立春" to "【倪師叮嚀：少陽之氣】\n春季養生重在「生」。今日切勿動怒、殺生。宜披髮緩行，廣步於庭。",
        "立秋" to "【倪師叮嚀】\n秋氣肅殺，今日宜收斂神氣，切勿悲憂。早臥早起，與雞俱興。"
    )

    // --- 節氣期間的通用養生建議 ---
    private val generalMap = mapOf(
        "小寒" to "【養生】腎氣最弱之時。宜養腎防寒，注意頭部與足部保暖。",
        "大寒" to "【養生】切忌大汗淋漓，損耗陽氣。飲食宜減鹹增苦，養心氣。",
        "雨水" to "【養生】溼氣漸重，脾胃易傷。飲食宜少酸多甘，以養脾氣。",
        "驚蟄" to "【養生】萬物復甦，病毒亦生。宜吃梨潤肺，預防流行病。",
        "清明" to "【養生】肝氣最旺之時。高血壓患者需注意情緒波動，宜飲菊花茶。",
        "芒種" to "【養生】溼熱之氣交蒸。容易睏倦（夏打盹），宜午睡片刻補氣。",
        "處暑" to "【養生】秋燥開始。宜滋陰潤肺，吃銀耳、百合，少吃辛辣。",
        "白露" to "【養生】早晚溫差大，最易誘發鼻炎、氣喘。切勿貪涼露宿。",
        "霜降" to "【養生】補冬不如補霜降。宜平補，吃柿子、栗子，保護脾胃。",
        "小雪" to "【養生】氣鬱則生病。宜多曬太陽，調節情緒，預防冬季憂鬱。",
        "大雪" to "【養生】去寒就溫，切勿暴暖。室內溫度不宜過高，以免毛孔開洩引發感冒。"
    )

    // 獲取黃/黑道日的解釋 (簡單版)
    fun getHuangDaoExplain(type: String): String {
        return if (type == "黃道") "【吉】諸事皆宜，百無禁忌。"
        else "【凶】諸事小心，大事勿用。"
    }
}