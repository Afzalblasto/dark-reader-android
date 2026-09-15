package com.darkreader.app.util

import android.content.Context
import android.net.Uri
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.BookmarkEntity
import com.google.gson.GsonBuilder
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.runBlocking

object BookmarkExporter {

    data class BookmarkExportData(
        val version: Int = 1,
        val exportDate: String,
        val documents: List<DocumentBookmarks>
    )

    data class DocumentBookmarks(
        val documentUri: String,
        val documentName: String,
        val bookmarks: List<BookmarkItem>
    )

    data class BookmarkItem(
        val pageNumber: Int,
        val label: String,
        val createdAt: Long
    )

    fun exportToJson(context: Context): String {
        return runBlocking {
            try {
                val database = AppDatabase.getInstance(context)
                val allBookmarks = database.bookmarkDao().getAllSync()
                
                val grouped = allBookmarks.groupBy { it.documentUri }
                val documents = grouped.map { (uri, bookmarksList) ->
                    val docName = bookmarksList.firstOrNull()?.documentName ?: "Unknown Document"
                    val items = bookmarksList.map { b ->
                        BookmarkItem(b.pageNumber, b.label, b.createdAt)
                    }
                    DocumentBookmarks(uri, docName, items)
                }
                
                val exportData = BookmarkExportData(
                    exportDate = FileUtils.formatDate(System.currentTimeMillis()),
                    documents = documents
                )
                
                val gson = GsonBuilder().setPrettyPrinting().create()
                gson.toJson(exportData)
            } catch (e: Exception) {
                e.printStackTrace()
                "{}"
            }
        }
    }

    fun importFromJson(context: Context, json: String): Int {
        return runBlocking {
            var count = 0
            try {
                val gson = GsonBuilder().create()
                val data = gson.fromJson(json, BookmarkExportData::class.java)
                val database = AppDatabase.getInstance(context)
                val dao = database.bookmarkDao()
                
                for (doc in data.documents) {
                    for (item in doc.bookmarks) {
                        val exists = dao.isBookmarked(doc.documentUri, item.pageNumber)
                        if (!exists) {
                            val newBookmark = BookmarkEntity(
                                documentUri = doc.documentUri,
                                documentName = doc.documentName,
                                pageNumber = item.pageNumber,
                                label = item.label,
                                createdAt = item.createdAt
                            )
                            dao.insert(newBookmark)
                            count++
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            count
        }
    }

    fun exportToFile(context: Context, outputUri: Uri) {
        try {
            val json = exportToJson(context)
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importFromFile(context: Context, inputUri: Uri): Int {
        var count = 0
        try {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val json = reader.readText()
                    count = importFromJson(context, json)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
}
