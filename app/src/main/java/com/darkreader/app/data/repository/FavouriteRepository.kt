package com.darkreader.app.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.darkreader.app.data.dao.FavouriteDao
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.FavouriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavouriteRepository(application: Application) {
    private val favouriteDao: FavouriteDao = AppDatabase.getInstance(application).favouriteDao()

    fun getAll(): LiveData<List<FavouriteEntity>> = favouriteDao.getAll()

    suspend fun getAllSync(): List<FavouriteEntity> = withContext(Dispatchers.IO) {
        favouriteDao.getAllSync()
    }

    suspend fun isFavourite(filePath: String): Boolean = withContext(Dispatchers.IO) {
        favouriteDao.isFavourite(filePath)
    }

    suspend fun getByPath(filePath: String): FavouriteEntity? = withContext(Dispatchers.IO) {
        favouriteDao.getByPath(filePath)
    }

    suspend fun insert(favourite: FavouriteEntity): Long = withContext(Dispatchers.IO) {
        favouriteDao.insert(favourite)
    }

    suspend fun delete(favourite: FavouriteEntity) = withContext(Dispatchers.IO) {
        favouriteDao.delete(favourite)
    }

    suspend fun deleteByPath(filePath: String) = withContext(Dispatchers.IO) {
        favouriteDao.deleteByPath(filePath)
    }
}
