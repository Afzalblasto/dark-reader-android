package com.darkreader.app.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.darkreader.app.data.dao.BookmarkDao
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.BookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookmarkRepository(application: Application) {
    private val bookmarkDao: BookmarkDao = AppDatabase.getInstance(application).bookmarkDao()

    fun getAll(): LiveData<List<BookmarkEntity>> = bookmarkDao.getAll()
    
    fun getByDocument(documentUri: String): LiveData<List<BookmarkEntity>> = bookmarkDao.getByDocument(documentUri)

    suspend fun getAllSync(): List<BookmarkEntity> = withContext(Dispatchers.IO) {
        bookmarkDao.getAllSync()
    }

    suspend fun getByDocumentSync(documentUri: String): List<BookmarkEntity> = withContext(Dispatchers.IO) {
        bookmarkDao.getByDocumentSync(documentUri)
    }
    
    suspend fun isBookmarked(documentUri: String, pageNumber: Int): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.isBookmarked(documentUri, pageNumber)
    }

    suspend fun insert(bookmark: BookmarkEntity): Long = withContext(Dispatchers.IO) {
        bookmarkDao.insert(bookmark)
    }

    suspend fun delete(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        bookmarkDao.delete(bookmark)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteById(id)
    }

    suspend fun deleteByDocument(documentUri: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteByDocument(documentUri)
    }

    suspend fun update(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        bookmarkDao.update(bookmark)
    }
}
