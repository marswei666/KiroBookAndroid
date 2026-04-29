package com.minami_studio.kiro.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Entry(
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerialName("name")
    val name: String,

    @SerialName("category")
    val category: PlaceCategory,

    @SerialName("note")
    val note: String = "",

    @SerialName("mood")
    val mood: Mood = Mood.relaxed,

    @SerialName("rating")
    val rating: Int = 4,

    @SerialName("city")
    val city: String = "",

    @SerialName("country")
    val country: String = "",

    @SerialName("latitude")
    val latitude: Double? = null,

    @SerialName("longitude")
    val longitude: Double? = null,

    @SerialName("photoFilenames")
    val photoFilenames: List<String> = emptyList(),

    @SerialName("isFavorite")
    val isFavorite: Boolean = false,

    @SerialName("visitedAt")
    val visitedAt: String = "",  // ISO 8601 格式

    @SerialName("createdAt")
    val createdAt: String = "",  // ISO 8601 格式

    @SerialName("tags")
    val tags: List<String> = emptyList(),

    @SerialName("customCategoryID")
    val customCategoryID: String? = null
) {
    val firstPhotoFilename: String? get() = photoFilenames.firstOrNull()

    val hasLocation: Boolean get() = latitude != null && longitude != null
}
