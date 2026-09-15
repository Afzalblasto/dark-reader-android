package com.darkreader.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

object FileUtils {

    fun getFileType(name: String): String {
        val ext = getFileExtension(name)
        return when (ext) {
            "pdf" -> Constants.FILE_TYPE_PDF
            "cbz" -> Constants.FILE_TYPE_CBZ
            "jpg", "jpeg" -> Constants.FILE_TYPE_JPG
            "png" -> Constants.FILE_TYPE_PNG
            else -> Constants.FILE_TYPE_OTHER
        }
    }

    fun getFileExtension(name: String): String {
        val lastDotIndex = name.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < name.length - 1) {
            name.substring(lastDotIndex + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun getMimeType(name: String): String {
        val ext = getFileExtension(name)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun generateModifiedFileName(originalName: String): String {
        val lastDotIndex = originalName.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            val name = originalName.substring(0, lastDotIndex)
            val ext = originalName.substring(lastDotIndex)
            "${name}_modified$ext"
        } else {
            "${originalName}_modified"
        }
    }

    fun isImageFile(name: String): Boolean {
        val ext = getFileExtension(name)
        return ext == "jpg" || ext == "jpeg" || ext == "png"
    }

    fun isPdfFile(name: String): Boolean {
        return getFileExtension(name) == "pdf"
    }

    fun isCbzFile(name: String): Boolean {
        return getFileExtension(name) == "cbz"
    }

    fun isSupportedFile(name: String): Boolean {
        return Constants.SUPPORTED_EXTENSIONS.contains(getFileExtension(name))
    }

    fun getFileThumbnail(context: Context, file: DocumentFile, width: Int, height: Int): Bitmap? {
        val name = file.name ?: return null
        return when {
            isPdfFile(name) -> getPdfThumbnail(context, file.uri, width, height)
            isImageFile(name) -> getImageThumbnail(context, file.uri, width, height)
            isCbzFile(name) -> getCbzThumbnail(context, file.uri, width, height)
            else -> null
        }
    }

    private fun getPdfThumbnail(context: Context, uri: Uri, width: Int, height: Int): Bitmap? {
        var fd: android.os.ParcelFileDescriptor? = null
        var renderer: android.graphics.pdf.PdfRenderer? = null
        var page: android.graphics.pdf.PdfRenderer.Page? = null
        return try {
            fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            renderer = android.graphics.pdf.PdfRenderer(fd)
            if (renderer.pageCount <= 0) return null
            page = renderer.openPage(0)
            val safeWidth = width.coerceIn(32, 512)
            val safeHeight = height.coerceIn(32, 512)
            val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (_: Exception) {
            null
        } finally {
            runCatching { page?.close() }
            runCatching { renderer?.close() }
            runCatching { fd?.close() }
        }
    }

    private fun getImageThumbnail(context: Context, uri: Uri, width: Int, height: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options, width, height)
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getCbzThumbnail(context: Context, uri: Uri, width: Int, height: Int): Bitmap? {
        return try {
            var firstImageName: String? = null
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zis = ZipInputStream(inputStream)
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isImageFile(entry.name)) {
                        firstImageName = entry.name
                        break
                    }
                    entry = zis.nextEntry
                }
                zis.close()
            }
            val imageName = firstImageName ?: return null
            // Decode directly from the archive stream. The previous implementation used
            // readBytes(), temporarily allocating an entire high-resolution comic page.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null && entry.name != imageName) entry = zis.nextEntry
                    if (entry != null) BitmapFactory.decodeStream(zis, null, bounds)
                }
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, width, height)
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null && entry.name != imageName) entry = zis.nextEntry
                    if (entry != null) BitmapFactory.decodeStream(zis, null, options) else null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun copyUriToFile(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getUriFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
