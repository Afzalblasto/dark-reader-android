package com.darkreader.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object CbzUtils {

    private val pageNamesCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()

    fun clearCache(context: Context, uri: Uri? = null) {
        try {
            if (uri != null) {
                pageNamesCache.remove(uri.toString())
                val cacheDir = File(context.cacheDir, "cbz_cache/${uri.toString().hashCode()}")
                cacheDir.deleteRecursively()
            } else {
                pageNamesCache.clear()
                File(context.cacheDir, "cbz_cache").deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPageCount(context: Context, uri: Uri): Int {
        return getPageNames(context, uri).size
    }

    fun getPageNames(context: Context, uri: Uri): List<String> {
        val key = uri.toString()
        pageNamesCache[key]?.let { return it }

        val names = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zis = ZipInputStream(inputStream)
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && FileUtils.isImageFile(entry.name)) {
                        names.add(entry.name)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                zis.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val sorted = names.sorted()
        pageNamesCache[key] = sorted
        return sorted
    }

    fun getPageBitmap(context: Context, uri: Uri, pageIndex: Int, maxWidth: Int): Bitmap? {
        val names = getPageNames(context, uri)
        if (pageIndex < 0 || pageIndex >= names.size) return null

        val cacheDir = File(context.cacheDir, "cbz_cache/${uri.toString().hashCode()}").apply { mkdirs() }
        val cacheFile = File(cacheDir, "page_$pageIndex.img")

        if (!cacheFile.exists()) {
            val targetName = names[pageIndex]
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val zis = ZipInputStream(inputStream)
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == targetName) {
                            FileOutputStream(cacheFile).use { out ->
                                zis.copyTo(out)
                            }
                            break
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                    zis.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!cacheFile.exists() || cacheFile.length() == 0L) return null

        cacheFile.setLastModified(System.currentTimeMillis())
        trimPageCache(cacheDir, keep = 12)

        return try {
            val options = BitmapFactory.Options()
            if (maxWidth > 0) {
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(cacheFile.absolutePath, options)
                var scale = 1
                while (options.outWidth / scale > maxWidth) {
                    scale *= 2
                }
                options.inSampleSize = scale
            }
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(cacheFile.absolutePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Keep only a small sliding window of extracted pages; full comics must not fill cache. */
    private fun trimPageCache(cacheDir: File, keep: Int) {
        cacheDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(keep)
            ?.forEach { it.delete() }
    }

    fun rebuildCbz(context: Context, inputUri: Uri, newOrder: List<Int>, outputStream: OutputStream) {
        val names = getPageNames(context, uri = inputUri)
        val orderedNames = newOrder.mapNotNull { if (it >= 0 && it < names.size) names[it] else null }
        
        try {
            val zos = ZipOutputStream(outputStream)
            for ((index, name) in orderedNames.withIndex()) {
                val bytes = getEntryBytes(context, inputUri, name) ?: continue
                val ext = FileUtils.getFileExtension(name)
                val newName = String.format("%03d.%s", index + 1, ext)
                zos.putNextEntry(ZipEntry(newName))
                zos.write(bytes)
                zos.closeEntry()
            }
            zos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addImages(context: Context, inputUri: Uri, imageUris: List<Uri>, position: Int, outputStream: OutputStream) {
        val names = getPageNames(context, inputUri)
        try {
            val zos = ZipOutputStream(outputStream)
            var currentIdx = 0
            
            for (i in 0 until position) {
                if (i < names.size) {
                    writeEntryToZip(context, inputUri, names[i], currentIdx++, zos)
                }
            }
            
            for (imageUri in imageUris) {
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    val bytes = input.readBytes()
                    val ext = FileUtils.getFileExtension(FileUtils.getUriFileName(context, imageUri) ?: "image.jpg")
                    val newName = String.format("%03d.%s", currentIdx++ + 1, ext.ifEmpty { "jpg" })
                    zos.putNextEntry(ZipEntry(newName))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
            
            for (i in position until names.size) {
                writeEntryToZip(context, inputUri, names[i], currentIdx++, zos)
            }
            zos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deletePages(context: Context, inputUri: Uri, pagesToDelete: Set<Int>, outputStream: OutputStream) {
        val names = getPageNames(context, inputUri)
        try {
            val zos = ZipOutputStream(outputStream)
            var currentIdx = 0
            for (i in names.indices) {
                if (!pagesToDelete.contains(i)) {
                    writeEntryToZip(context, inputUri, names[i], currentIdx++, zos)
                }
            }
            zos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportPages(context: Context, inputUri: Uri, pages: List<Int>, outputDir: File): List<File> {
        val names = getPageNames(context, inputUri)
        val exportedFiles = mutableListOf<File>()
        if (!outputDir.exists()) outputDir.mkdirs()
        
        try {
            for (pageIndex in pages) {
                if (pageIndex >= 0 && pageIndex < names.size) {
                    val name = names[pageIndex]
                    val bytes = getEntryBytes(context, inputUri, name) ?: continue
                    val ext = FileUtils.getFileExtension(name)
                    val file = File(outputDir, "page_${pageIndex + 1}.$ext")
                    FileOutputStream(file).use { out ->
                        out.write(bytes)
                    }
                    exportedFiles.add(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return exportedFiles
    }

    fun imagesToCbz(context: Context, imageUris: List<Uri>, outputStream: OutputStream) {
        try {
            val zos = ZipOutputStream(outputStream)
            for ((index, uri) in imageUris.withIndex()) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    val ext = FileUtils.getFileExtension(FileUtils.getUriFileName(context, uri) ?: "image.jpg")
                    val newName = String.format("%03d.%s", index + 1, ext.ifEmpty { "jpg" })
                    zos.putNextEntry(ZipEntry(newName))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
            zos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getEntryBytes(context: Context, uri: Uri, entryName: String): ByteArray? {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zis = ZipInputStream(inputStream)
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == entryName) {
                        val bytes = zis.readBytes()
                        zis.close()
                        return bytes
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                zis.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    private fun writeEntryToZip(context: Context, inputUri: Uri, originalName: String, newIndex: Int, zos: ZipOutputStream) {
        val bytes = getEntryBytes(context, inputUri, originalName) ?: return
        val ext = FileUtils.getFileExtension(originalName)
        val newName = String.format("%03d.%s", newIndex + 1, ext.ifEmpty { "jpg" })
        zos.putNextEntry(ZipEntry(newName))
        zos.write(bytes)
        zos.closeEntry()
    }
}
