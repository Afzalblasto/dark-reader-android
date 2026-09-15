package com.darkreader.app.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.darkreader.app.data.dao.SafeFileDao
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.SafeFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafeRepository(application: Application) {
    private val safeFileDao: SafeFileDao = AppDatabase.getInstance(application).safeFileDao()

    fun getAll(): LiveData<List<SafeFileEntity>> = safeFileDao.getAll()

    suspend fun getAllSync(): List<SafeFileEntity> = withContext(Dispatchers.IO) {
        safeFileDao.getAllSync()
    }
    
    suspend fun getByStoredName(storedName: String): SafeFileEntity? = withContext(Dispatchers.IO) {
        safeFileDao.getByStoredName(storedName)
    }

    suspend fun insert(safeFile: SafeFileEntity): Long = withContext(Dispatchers.IO) {
        safeFileDao.insert(safeFile)
    }

    suspend fun delete(safeFile: SafeFileEntity) = withContext(Dispatchers.IO) {
        safeFileDao.delete(safeFile)
    }

    suspend fun deleteByStoredName(storedName: String) = withContext(Dispatchers.IO) {
        safeFileDao.deleteByStoredName(storedName)
    }
}
