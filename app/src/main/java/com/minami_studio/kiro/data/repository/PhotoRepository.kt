package com.minami_studio.kiro.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PhotoRepository {

    private fun photosDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(base, "photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun savePhoto(context: Context, uri: Uri): String? {
        return try {
            val filename = "${UUID.randomUUID()}.jpg"
            val dest = File(photosDir(context), filename)
            context.contentResolver.openInputStream(uri)?.use { input ->
                // 读取并压缩图片
                val bitmap = BitmapFactory.decodeStream(input) ?: return null
                FileOutputStream(dest).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
            }
            filename
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmap(context: Context, bitmap: Bitmap): String? {
        return try {
            val filename = "${UUID.randomUUID()}.jpg"
            val dest = File(photosDir(context), filename)
            FileOutputStream(dest).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            filename
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPhotoFile(context: Context, filename: String): File {
        return File(photosDir(context), filename)
    }

    fun getPhotoUri(context: Context, filename: String): Uri {
        return Uri.fromFile(getPhotoFile(context, filename))
    }

    fun delete(context: Context, filenames: List<String>) {
        val dir = photosDir(context)
        filenames.forEach { filename ->
            File(dir, filename).delete()
        }
    }

    fun saveAvatar(context: Context, uri: Uri): Boolean {
        return try {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            val dest = File(base, "profile_avatar.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return false
                FileOutputStream(dest).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAvatarFile(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "profile_avatar.jpg")
    }

    fun getAvatarUri(context: Context): Uri? {
        val file = getAvatarFile(context)
        return if (file.exists()) Uri.fromFile(file) else null
    }
}
