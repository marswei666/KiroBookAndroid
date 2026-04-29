package com.minami_studio.kiro.data.store

import android.content.Context
import com.minami_studio.kiro.data.model.CustomCategory
import com.minami_studio.kiro.data.model.Entry
import com.minami_studio.kiro.data.model.PlaceCategory
import com.minami_studio.kiro.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class EntryStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val entriesFile = File(context.filesDir, "entries.json")
    private val categoriesFile = File(context.filesDir, "customCategories.json")

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private val _customCategories = MutableStateFlow<List<CustomCategory>>(emptyList())
    val customCategories: StateFlow<List<CustomCategory>> = _customCategories.asStateFlow()

    init {
        loadEntries()
        loadCustomCategories()
        seedDefaultCategoriesIfNeeded()
        migrateCustomCategoriesIfNeeded()
        cleanupOrphanedCategoryIDs()
        migrateEntriesIfNeeded()
        normalizeOrphanedEntries()
    }

    // MARK: - Entry CRUD

    fun add(entry: Entry) {
        _entries.value = listOf(entry) + _entries.value
        saveEntries()
    }

    fun update(entry: Entry) {
        _entries.value = _entries.value.map { if (it.id == entry.id) entry else it }
        saveEntries()
    }

    fun delete(entry: Entry) {
        PhotoRepository.delete(context, entry.photoFilenames)
        _entries.value = _entries.value.filter { it.id != entry.id }
        saveEntries()
    }

    // MARK: - CustomCategory CRUD

    fun addCustomCategory(
        name: String,
        icon: String = "tag",
        sourcePlaceCategory: PlaceCategory? = null,
        localizedNames: Map<String, String> = emptyMap()
    ): CustomCategory {
        val cat = CustomCategory(
            name = name,
            icon = icon,
            sourcePlaceCategory = sourcePlaceCategory,
            localizedNames = localizedNames
        )
        _customCategories.value = _customCategories.value + cat
        saveCustomCategories()
        return cat
    }

    fun updateCustomCategory(cat: CustomCategory) {
        _customCategories.value = _customCategories.value.map { if (it.id == cat.id) cat else it }
        saveCustomCategories()
    }

    fun deleteCustomCategory(cat: CustomCategory) {
        _customCategories.value = _customCategories.value.filter { it.id != cat.id }
        _entries.value = _entries.value.map {
            if (it.customCategoryID == cat.id)
                it.copy(customCategoryID = null, category = PlaceCategory.other)
            else it
        }
        saveEntries()
        saveCustomCategories()
    }

    // MARK: - Helpers

    fun customCategoryFor(entry: Entry): CustomCategory? {
        val id = entry.customCategoryID ?: return null
        return _customCategories.value.firstOrNull { it.id == id }
    }

    fun categoryDisplayName(entry: Entry, langCode: String): String {
        val custom = customCategoryFor(entry)
        if (custom != null) {
            custom.sourcePlaceCategory?.let { return it.localizedName(langCode) }
            return custom.name
        }
        return entry.category.localizedName(langCode)
    }

    fun displayName(customCat: CustomCategory, langCode: String): String {
        customCat.localizedNames[langCode]?.takeIf { it.isNotEmpty() }?.let { return it }
        customCat.sourcePlaceCategory?.let { return it.localizedName(langCode) }
        return customCat.name
    }

    fun categoryIcon(entry: Entry): String {
        val custom = customCategoryFor(entry)
        if (custom != null) return custom.icon
        return entry.category.icon
    }

    // MARK: - Default Categories

    private fun seedDefaultCategoriesIfNeeded() {
        if (_customCategories.value.isNotEmpty()) return
        val defaults = listOf(
            Triple("咖啡馆", "cup_and_saucer", PlaceCategory.cafe),
            Triple("博物馆", "museum", PlaceCategory.museum),
            Triple("书店", "book", PlaceCategory.bookstore),
            Triple("酒吧", "wine_bar", PlaceCategory.bar),
            Triple("展览 / 美术馆", "palette", PlaceCategory.gallery),
            Triple("买手店", "shopping_bag", PlaceCategory.selectShop),
            Triple("餐厅", "restaurant", PlaceCategory.restaurant),
        )
        _customCategories.value = defaults.map { (name, icon, source) ->
            CustomCategory(name = name, icon = icon, sourcePlaceCategory = source)
        }
        saveCustomCategories()
    }

    // MARK: - Migration

    private fun migrateCustomCategoriesIfNeeded() {
        val iconMap = mapOf(
            "cup_and_saucer" to PlaceCategory.cafe,
            "museum" to PlaceCategory.museum,
            "book" to PlaceCategory.bookstore,
            "wine_bar" to PlaceCategory.bar,
            "palette" to PlaceCategory.gallery,
            "shopping_bag" to PlaceCategory.selectShop,
            "restaurant" to PlaceCategory.restaurant,
        )
        var changed = false
        val updated = _customCategories.value.map { cat ->
            if (cat.sourcePlaceCategory == null) {
                iconMap[cat.icon]?.let { source ->
                    changed = true
                    cat.copy(sourcePlaceCategory = source)
                } ?: cat
            } else cat
        }
        if (changed) {
            _customCategories.value = updated
            saveCustomCategories()
        }
    }

    private fun cleanupOrphanedCategoryIDs() {
        val validIDs = _customCategories.value.map { it.id }.toSet()
        var changed = false
        val updated = _entries.value.map { entry ->
            if (entry.customCategoryID != null && entry.customCategoryID !in validIDs) {
                changed = true
                entry.copy(customCategoryID = null)
            } else entry
        }
        if (changed) {
            _entries.value = updated
            saveEntries()
        }
    }

    private fun migrateEntriesIfNeeded() {
        var changed = false
        val updated = _entries.value.map { entry ->
            if (entry.customCategoryID == null) {
                val matched = _customCategories.value.firstOrNull { it.sourcePlaceCategory == entry.category }
                if (matched != null) {
                    changed = true
                    entry.copy(customCategoryID = matched.id)
                } else entry
            } else entry
        }
        if (changed) {
            _entries.value = updated
            saveEntries()
        }
    }

    // 将 customCategoryID=null 且 category 对应的品类已被删除的条目归到 .other
    // 修复删除品类前保存的存量数据
    private fun normalizeOrphanedEntries() {
        val activeSourceCategories = _customCategories.value
            .mapNotNull { it.sourcePlaceCategory }
            .toSet()
        var changed = false
        val updated = _entries.value.map { entry ->
            if (entry.customCategoryID == null
                && entry.category != PlaceCategory.other
                && entry.category !in activeSourceCategories
            ) {
                changed = true
                entry.copy(category = PlaceCategory.other)
            } else entry
        }
        if (changed) {
            _entries.value = updated
            saveEntries()
        }
    }

    // MARK: - Persistence

    private fun saveEntries() {
        try {
            entriesFile.writeText(json.encodeToString(_entries.value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadEntries() {
        try {
            if (entriesFile.exists()) {
                _entries.value = json.decodeFromString<List<Entry>>(entriesFile.readText())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCustomCategories() {
        try {
            categoriesFile.writeText(json.encodeToString(_customCategories.value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadCustomCategories() {
        try {
            if (categoriesFile.exists()) {
                _customCategories.value = json.decodeFromString<List<CustomCategory>>(categoriesFile.readText())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // MARK: - Backup

    fun exportJson(): String {
        return json.encodeToString(_entries.value)
    }

    fun importJson(jsonStr: String): Int {
        val imported = json.decodeFromString<List<Entry>>(jsonStr)
        val existingIds = _entries.value.map { it.id }.toSet()
        val newEntries = imported.filter { it.id !in existingIds }
        if (newEntries.isNotEmpty()) {
            _entries.value = newEntries + _entries.value
            saveEntries()
        }
        return newEntries.size
    }

    fun totalPhotoSize(): Long {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val photosDir = File(base, "photos")
        if (!photosDir.exists()) return 0
        return photosDir.listFiles()?.sumOf { it.length() } ?: 0
    }
}
