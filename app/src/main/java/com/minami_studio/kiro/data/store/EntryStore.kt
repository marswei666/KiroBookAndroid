package com.minami_studio.kiro.data.store

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.minami_studio.kiro.data.model.CustomCategory
import com.minami_studio.kiro.data.model.Entry
import com.minami_studio.kiro.data.model.PlaceCategory
import com.minami_studio.kiro.data.repository.PhotoRepository
import com.minami_studio.kiro.data.sync.CloudSyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class EntryStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val entriesFile = File(context.filesDir, "entries.json")
    private val categoriesFile = File(context.filesDir, "customCategories.json")
    private val cloudSyncService = CloudSyncService(context)

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
        migratePhotoFilenamesIfNeeded()
    }

    // MARK: - Entry CRUD

    fun add(entry: Entry) {
        _entries.value = listOf(entry) + _entries.value
        saveEntries()
        triggerCloudSync()
    }

    fun update(entry: Entry) {
        _entries.value = _entries.value.map { if (it.id == entry.id) entry else it }
        saveEntries()
    }

    fun delete(entry: Entry) {
        PhotoRepository.delete(context, entry.photoFilenames)
        _entries.value = _entries.value.filter { it.id != entry.id }
        saveEntries()
        triggerCloudSync()
    }

    private fun triggerCloudSync() {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("profile_name", "") ?: ""
        cloudSyncService.syncStats(_entries.value, userName)
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

    private fun migratePhotoFilenamesIfNeeded() {
        val datePrefix = Regex("""^\d{4}-\d{2}-\d{2}_""")
        var changed = false
        val photosDir = run {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            java.io.File(base, "photos")
        }
        val updated = _entries.value.map { entry ->
            val dateStr = entry.visitedAt.takeIf { it.isNotEmpty() }?.substring(0, 10)
                ?: return@map entry
            val newFilenames = entry.photoFilenames.map { filename ->
                if (datePrefix.containsMatchIn(filename)) return@map filename
                val newName = "${dateStr}_${filename}"
                val src = java.io.File(photosDir, filename)
                val dst = java.io.File(photosDir, newName)
                if (src.exists() && src.renameTo(dst)) {
                    changed = true
                    newName
                } else filename
            }
            if (newFilenames != entry.photoFilenames) entry.copy(photoFilenames = newFilenames) else entry
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

    fun exportZip(): Uri? {
        return try {
            val zipFile = File(context.cacheDir, "kiro_backup.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                zos.putNextEntry(ZipEntry("entries.json"))
                zos.write(json.encodeToString(_entries.value).toByteArray())
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("customCategories.json"))
                zos.write(json.encodeToString(_customCategories.value).toByteArray())
                zos.closeEntry()

                val base = context.getExternalFilesDir(null) ?: context.filesDir
                File(base, "photos").listFiles()?.forEach { file ->
                    zos.putNextEntry(ZipEntry("photos/${file.name}"))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }

                val avatar = File(base, "profile_avatar.jpg")
                if (avatar.exists()) {
                    zos.putNextEntry(ZipEntry("profile_avatar.jpg"))
                    avatar.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }

                val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val profileJson = """{"profile_name":${json.encodeToString(prefs.getString("profile_name", "") ?: "")},"profile_tagline":${json.encodeToString(prefs.getString("profile_tagline", "") ?: "")}}"""
                zos.putNextEntry(ZipEntry("profile.json"))
                zos.write(profileJson.toByteArray())
                zos.closeEntry()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importZip(uri: Uri): Int {
        return try {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            val photosDir = File(base, "photos").also { it.mkdirs() }
            var entriesJson: String? = null
            var categoriesJson: String? = null
            var profileJson: String? = null

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "entries.json" ->
                                entriesJson = zis.readBytes().decodeToString()
                            entry.name == "customCategories.json" ->
                                categoriesJson = zis.readBytes().decodeToString()
                            entry.name.startsWith("photos/") -> {
                                val filename = entry.name.removePrefix("photos/")
                                if (filename.isNotEmpty()) {
                                    val dest = File(photosDir, filename)
                                    if (!dest.exists()) FileOutputStream(dest).use { zis.copyTo(it) }
                                }
                            }
                            entry.name == "profile_avatar.jpg" -> {
                                val dest = File(base, "profile_avatar.jpg")
                                if (!dest.exists()) FileOutputStream(dest).use { zis.copyTo(it) }
                            }
                            entry.name == "profile.json" ->
                                profileJson = zis.readBytes().decodeToString()
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            categoriesJson?.let { catJson ->
                val rawImported = json.decodeFromString<List<CustomCategory>>(catJson)

                // 对导入列表内部先去重（旧数据可能已有重复品类）
                val referencedIds = _entries.value.mapNotNull { it.customCategoryID }.toSet()
                val dedupeRemap = mutableMapOf<String, String>() // 被淘汰的导入 ID -> 保留的导入 ID
                val imported = rawImported
                    .groupBy { it.sourcePlaceCategory }
                    .flatMap { (sourceCat, group) ->
                        if (sourceCat == null || group.size == 1) {
                            group
                        } else {
                            val winner = group.firstOrNull { it.id in referencedIds } ?: group.first()
                            group.filter { it.id != winner.id }.forEach { loser ->
                                dedupeRemap[loser.id] = winner.id
                            }
                            listOf(winner)
                        }
                    }

                val importedIds = imported.map { it.id }.toSet()
                val importedSourceCats = imported.mapNotNull { it.sourcePlaceCategory }.toSet()

                // 建立本地冲突品类 -> 导入品类的映射，以及导入内部去重的映射
                val replaceMap = mutableMapOf<String, String>()
                replaceMap.putAll(dedupeRemap)
                _customCategories.value.forEach { existing ->
                    if (existing.id !in importedIds && existing.sourcePlaceCategory in importedSourceCats) {
                        val replacement = imported.firstOrNull { it.sourcePlaceCategory == existing.sourcePlaceCategory }
                        if (replacement != null) replaceMap[existing.id] = replacement.id
                    }
                }

                // 先更新打卡记录中的引用
                if (replaceMap.isNotEmpty()) {
                    _entries.value = _entries.value.map { entry ->
                        val newId = replaceMap[entry.customCategoryID]
                        if (newId != null) entry.copy(customCategoryID = newId) else entry
                    }
                    saveEntries()
                }

                // 再移除冲突品类，追加去重后的导入品类
                val kept = _customCategories.value.filter { existing ->
                    existing.id !in importedIds &&
                    (existing.sourcePlaceCategory == null || existing.sourcePlaceCategory !in importedSourceCats)
                }
                val merged = kept + imported
                if (merged != _customCategories.value) {
                    _customCategories.value = merged
                    saveCustomCategories()
                }
            }

            profileJson?.let {
                try {
                    val obj = org.json.JSONObject(it)
                    val name = obj.optString("profile_name", "")
                    val tagline = obj.optString("profile_tagline", "")
                    context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putString("profile_name", name)
                        .putString("profile_tagline", tagline)
                        .apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val count = entriesJson?.let { importEntriesFromJson(it) } ?: 0
            count
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun importEntriesFromJson(jsonStr: String): Int {
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
