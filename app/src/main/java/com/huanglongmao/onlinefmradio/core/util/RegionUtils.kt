package com.huanglongmao.onlinefmradio.core.util

/**
 * 地区（大洲）映射工具：ISO 3166-1 alpha-2 国家代码 → 大洲中文名。
 * 供主页国家/语言 Tab 与国家/语言列表页的级联筛选共用。
 */

/** 大洲展示顺序（固定业务顺序，非字典序） */
val REGION_ORDER = listOf("亚洲", "欧洲", "北美洲", "南美洲", "非洲", "大洋洲", "其他")

/** 未覆盖国家代码的兜底地区名 */
const val REGION_FALLBACK = "其他"

/** ISO 3166-1 alpha-2 → 大洲（按区域分组，未覆盖的归入"其他"） */
private val regionGroups: Map<String, List<String>> = linkedMapOf(
    "亚洲" to listOf(
        "CN", "JP", "KR", "KP", "MN", "HK", "MO", "TW",
        "IN", "PK", "BD", "LK", "NP", "BT", "MV",
        "ID", "MY", "SG", "TH", "VN", "PH", "MM", "KH", "LA", "BN",
        "KZ", "UZ", "TM", "KG", "TJ", "AF", "MN",
        "IR", "IQ", "IL", "PS", "JO", "LB", "SY", "SA", "AE", "YE", "OM", "QA", "BH", "KW", "TR", "GE", "AM", "AZ", "CY",
    ),
    "欧洲" to listOf(
        "GB", "IE", "FR", "DE", "IT", "ES", "PT", "NL", "BE", "LU", "CH", "AT",
        "SE", "NO", "DK", "FI", "IS",
        "PL", "CZ", "SK", "HU", "RO", "BG", "GR", "HR", "SI", "RS", "BA", "MK", "ME", "AL", "XK",
        "LT", "LV", "EE", "BY", "UA", "MD", "RU", "MT", "MC", "AD", "SM", "LI",
    ),
    "北美洲" to listOf(
        "US", "CA", "MX", "GL",
        "GT", "BZ", "SV", "HN", "NI", "CR", "PA",
        "CU", "HT", "DO", "JM", "TT", "BS", "BB", "AI", "AG", "DM", "GD", "KN", "LC", "VC", "PR", "GP", "MQ", "CW", "AW", "SX", "BL", "MF", "KY", "VI", "TC", "MS", "BM",
    ),
    "南美洲" to listOf(
        "BR", "AR", "CL", "PE", "CO", "VE", "EC", "UY", "PY", "BO", "GY", "SR", "GF", "FK",
    ),
    "非洲" to listOf(
        "EG", "LY", "TN", "DZ", "MA", "EH",
        "SD", "SS", "ET", "ER", "DJ", "SO", "KE", "UG", "TZ", "RW", "BI",
        "CD", "CG", "CF", "CM", "NG", "BJ", "TG", "GH", "CI", "LR", "SL", "GN", "GM", "BF", "ML", "NE", "TD", "SN", "GW", "CV", "ST", "GQ", "GA",
        "ZM", "ZW", "MW", "MZ", "AO", "NA", "BW", "SZ", "LS", "ZA",
        "MG", "MU", "KM", "YT", "SC", "RE", "SH",
    ),
    "大洋洲" to listOf(
        "AU", "NZ", "PG", "FJ", "SB", "VU", "NC", "PF", "WS", "TO", "TV",
        "NR", "KI", "FM", "MH", "PW", "GU", "CK", "NU", "AS", "TK", "PN", "NF", "WF", "MP",
    ),
)

/** 国家代码 → 地区 快速查询表 */
private val countryCodeToRegion: Map<String, String> by lazy {
    val map = HashMap<String, String>()
    regionGroups.forEach { (region, codes) -> codes.forEach { map[it] = region } }
    map
}

/** 国家代码 → 地区（大洲），未覆盖归入 [REGION_FALLBACK] */
fun regionOfCountryCode(code: String): String =
    countryCodeToRegion[code.uppercase()] ?: REGION_FALLBACK
