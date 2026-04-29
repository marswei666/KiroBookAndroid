package com.minami_studio.kiro.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File

data class ExifLocation(
    val latitude: Double,
    val longitude: Double
)

object ExifUtils {

    private const val TAG = "ExifUtils"

    /**
     * 从 content URI 提取 EXIF GPS 位置。
     * 优先用 MediaStore 查询原始文件路径（避免系统相册选择器去除 EXIF）。
     */
    fun extractLocation(context: Context, uri: Uri): ExifLocation? {
        Log.d(TAG, "extractLocation: uri=$uri, scheme=${uri.scheme}")

        // 方法1: 通过 MediaStore 获取原始文件路径，直接读取 EXIF
        val realPath = getRealPathFromUri(context, uri)
        Log.d(TAG, "Real path: $realPath")
        if (realPath != null) {
            try {
                val exif = ExifInterface(realPath)
                val latLong = exif.latLong
                Log.d(TAG, "EXIF latLong from file: $latLong")
                if (latLong != null) {
                    Log.d(TAG, "EXIF GPS via file path: ${latLong[0]}, ${latLong[1]}")
                    return ExifLocation(latLong[0], latLong[1])
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read EXIF from file path", e)
            }
        }

        // 方法2: 通过 FileDescriptor
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            Log.d(TAG, "FileDescriptor: $pfd")
            if (pfd != null) {
                try {
                    val exif = ExifInterface(pfd.fileDescriptor)
                    val latLong = exif.latLong
                    Log.d(TAG, "EXIF latLong from fd: $latLong")
                    if (latLong != null) {
                        Log.d(TAG, "EXIF GPS via fd: ${latLong[0]}, ${latLong[1]}")
                        return ExifLocation(latLong[0], latLong[1])
                    }
                } finally {
                    pfd.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read EXIF via fd", e)
        }

        // 方法3: 通过 InputStream
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val latLong = exif.latLong
                Log.d(TAG, "EXIF latLong from stream: $latLong")
                if (latLong != null) {
                    Log.d(TAG, "EXIF GPS via stream: ${latLong[0]}, ${latLong[1]}")
                    return ExifLocation(latLong[0], latLong[1])
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read EXIF via stream", e)
        }

        Log.d(TAG, "No EXIF GPS data found for $uri")
        return null
    }

    fun extractDate(context: Context, uri: Uri): String? {
        val realPath = getRealPathFromUri(context, uri)
        if (realPath != null) {
            try {
                val exif = ExifInterface(realPath)
                exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { return it }
            } catch (_: Exception) {}
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 通过 MediaStore 查询照片的真实文件路径（绕过系统选择器的 EXIF 剥离）
     */
    private fun getRealPathFromUri(context: Context, uri: Uri): String? {
        return try {
            // 如果是 content:// URI，通过 MediaStore 查询真实路径
            if (uri.scheme == "content") {
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        val path = cursor.getString(idx)
                        if (path != null && File(path).exists()) {
                            Log.d(TAG, "Real path from MediaStore: $path")
                            return path
                        }
                    }
                }
            }
            // 如果是 file:// URI，直接返回路径
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null && File(path).exists()) {
                    return path
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get real path from URI", e)
            null
        }
    }
}
