package com.huanglongmao.onlinefmradio.core.util

/**
 * 翻译工具（完整移植 Flutter 版 translation_utils.dart 的语言名称映射）。
 */
object TranslationUtils {

    /** 语言名称翻译字典（英文小写 → 中文） */
    val languageNameMap: Map<String, String> = mapOf(
        "chinese" to "中文",
        "english" to "英语",
        "german" to "德语",
        "french" to "法语",
        "japanese" to "日语",
        "spanish" to "西班牙语",
        "italian" to "意大利语",
        "korean" to "韩语",
        "russian" to "俄语",
        "portuguese" to "葡萄牙语",
        "arabic" to "阿拉伯语",
        "hindi" to "印地语",
        "dutch" to "荷兰语",
        "swedish" to "瑞典语",
        "norwegian" to "挪威语",
        "danish" to "丹麦语",
        "finnish" to "芬兰语",
        "polish" to "波兰语",
        "turkish" to "土耳其语",
        "greek" to "希腊语",
        "hungarian" to "匈牙利语",
        "czech" to "捷克语",
        "slovak" to "斯洛伐克语",
        "croatian" to "克罗地亚语",
        "serbian" to "塞尔维亚语",
        "bulgarian" to "保加利亚语",
        "romanian" to "罗马尼亚语",
        "ukrainian" to "乌克兰语",
        "hebrew" to "希伯来语",
        "persian" to "波斯语",
        "thai" to "泰语",
        "vietnamese" to "越南语",
        "indonesian" to "印尼语",
        "malay" to "马来语",
        "tamil" to "泰米尔语",
        "telugu" to "泰卢固语",
        "marathi" to "马拉地语",
        "bengali" to "孟加拉语",
        "punjabi" to "旁遮普语",
        "gujarati" to "古吉拉特语",
        "kannada" to "卡纳达语",
        "malayalam" to "马拉雅拉姆语",
        "oriya" to "奥里亚语",
        "assamese" to "阿萨姆语",
        "nepali" to "尼泊尔语",
        "sinhalese" to "僧伽罗语",
        "burmese" to "缅甸语",
        "khmer" to "高棉语",
        "lao" to "老挝语",
        "mongolian" to "蒙古语",
        "tibetan" to "藏语",
        "uyghur" to "维吾尔语",
        "kazakh" to "哈萨克语",
        "uzbek" to "乌兹别克语",
        "tajik" to "塔吉克语",
        "kyrgyz" to "吉尔吉斯语",
        "turkmen" to "土库曼语",
        "azerbaijani" to "阿塞拜疆语",
        "georgian" to "格鲁吉亚语",
        "armenian" to "亚美尼亚语",
        "kurdish" to "库尔德语",
        "pashto" to "普什图语",
        "balochi" to "俾路支语",
        "sindhi" to "信德语",
        "saraiki" to "萨拉伊基语",
        "balti" to "巴尔蒂语",
        "ladakhi" to "拉达克语",
        "dogri" to "多格里语",
        "kashmiri" to "克什米尔语",
        "konkani" to "孔卡尼语",
        "santali" to "桑塔利语",
        "bodo" to "博多语",
        "mizo" to "米佐语",
        "kuki" to "库基语",
        "manipuri" to "曼尼普尔语",
        "naga" to "那加语",
        "karbi" to "卡比语",
        "garo" to "加罗语",
        "tripuri" to "特里普拉语",
        "khasi" to "卡西语",
        "catalan" to "加泰罗尼亚语",
        "galician" to "加利西亚语",
        "basque" to "巴斯克语",
        "welsh" to "威尔士语",
        "irish" to "爱尔兰语",
        "scottish gaelic" to "苏格兰盖尔语",
        "latin" to "拉丁语",
        "esperanto" to "世界语",
        "swahili" to "斯瓦希里语",
        "amharic" to "阿姆哈拉语",
        "yoruba" to "约鲁巴语",
        "igbo" to "伊博语",
        "hausa" to "豪萨语",
        "zulu" to "祖鲁语",
        "xhosa" to "科萨语",
        "afrikaans" to "南非荷兰语",
        "somali" to "索马里语",
        "tagalog" to "他加禄语",
        "filipino" to "菲律宾语",
        "cebuano" to "宿务语",
        "hawaiian" to "夏威夷语",
        "maori" to "毛利语",
        "samoan" to "萨摩亚语",
        "tongan" to "汤加语",
        "fijian" to "斐济语",
    )

    /**
     * 获取语言的中文显示名称。
     * 有翻译时返回 "中文（English）" 格式，否则返回原始名称。
     */
    fun getLanguageDisplayName(name: String): String {
        val lower = name.lowercase().trim()
        languageNameMap[lower]?.let { return "$it（$name）" }
        // 处理逗号分隔的多语言情况
        val parts = lower.split(',').map { it.trim() }
        if (parts.size > 1) {
            val translated = parts.joinToString(",") { languageNameMap[it] ?: capitalize(it) }
            val english = parts.joinToString(",") { capitalize(it) }
            return "$translated（$english）"
        }
        return name
    }

    /** 获取语言的纯中文名称（不含英文） */
    fun getLanguageChineseName(name: String): String? =
        languageNameMap[name.lowercase().trim()]

    private fun capitalize(s: String): String =
        if (s.isEmpty()) s
        else s.replaceFirstChar { it.uppercase() }
}
