package com.minami_studio.kiro.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CustomCategory(
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerialName("name")
    val name: String,

    @SerialName("icon")
    val icon: String = "tag",

    @SerialName("sourcePlaceCategory")
    val sourcePlaceCategory: PlaceCategory? = null,

    @SerialName("localizedNames")
    val localizedNames: Map<String, String> = emptyMap()
) {
    fun displayName(langCode: String): String {
        // 1. 该语言有用户自定义覆盖 → 优先使用
        localizedNames[langCode]?.takeIf { it.isNotEmpty() }?.let { return it }
        // 2. 默认品类 → 自动翻译
        sourcePlaceCategory?.let { return it.localizedName(langCode) }
        // 3. 纯自定义品类 → 用存储名
        return name
    }
}
