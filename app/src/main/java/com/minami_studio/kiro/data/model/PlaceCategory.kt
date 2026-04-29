package com.minami_studio.kiro.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
enum class PlaceCategory(val chineseName: String, val icon: String) {
    cafe("咖啡馆", "cup_and_saucer"),
    museum("博物馆", "museum"),
    bookstore("书店", "book"),
    bar("酒吧", "wine_bar"),
    gallery("展览 / 美术馆", "palette"),
    selectShop("买手店", "shopping_bag"),
    restaurant("餐厅", "restaurant"),
    other("其他", "place");

    val materialIcon: ImageVector
        get() = when (this) {
            cafe -> Icons.Default.Coffee
            museum -> Icons.Default.Museum
            bookstore -> Icons.AutoMirrored.Filled.LibraryBooks
            bar -> Icons.Default.WineBar
            gallery -> Icons.Default.Image
            selectShop -> Icons.Default.ShoppingBag
            restaurant -> Icons.Default.Restaurant
            other -> Icons.Default.Place
        }

    fun localizedName(langCode: String): String = when (langCode) {
        "zh-Hans" -> chineseName
        "en" -> when (this) {
            cafe -> "Café"; museum -> "Museum"; bookstore -> "Bookstore"
            bar -> "Bar"; gallery -> "Gallery"; selectShop -> "Select Shop"
            restaurant -> "Restaurant"; other -> "Other"
        }
        "ja" -> when (this) {
            cafe -> "カフェ"; museum -> "博物館"; bookstore -> "本屋"
            bar -> "バー"; gallery -> "ギャラリー"; selectShop -> "セレクトショップ"
            restaurant -> "レストラン"; other -> "その他"
        }
        "ko" -> when (this) {
            cafe -> "카페"; museum -> "박물관"; bookstore -> "서점"
            bar -> "바"; gallery -> "갤러리"; selectShop -> "셀렉샵"
            restaurant -> "레스토랑"; other -> "기타"
        }
        "zh-Hant" -> when (this) {
            cafe -> "咖啡館"; museum -> "博物館"; bookstore -> "書店"
            bar -> "酒吧"; gallery -> "展覽 / 美術館"; selectShop -> "買手店"
            restaurant -> "餐廳"; other -> "其他"
        }
        else -> chineseName
    }
}
