package com.darkreader.app.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.darkreader.app.data.dao.TabDao
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.TabEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TabRepository(application: Application) {
    private val tabDao: TabDao = AppDatabase.getInstance(application).tabDao()

    fun getAll(): LiveData<List<TabEntity>> = tabDao.getAll()

    suspend fun getAllSync(): List<TabEntity> = withContext(Dispatchers.IO) {
        tabDao.getAllSync()
    }

    suspend fun getActive(): TabEntity? = withContext(Dispatchers.IO) {
        tabDao.getActive()
    }
    
    suspend fun getById(id: Long): TabEntity? = withContext(Dispatchers.IO) {
        tabDao.getById(id)
    }

    suspend fun insert(tab: TabEntity): Long = withContext(Dispatchers.IO) {
        tabDao.insert(tab)
    }

    suspend fun delete(tab: TabEntity) = withContext(Dispatchers.IO) {
        tabDao.delete(tab)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        tabDao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        tabDao.deleteAll()
    }

    suspend fun update(tab: TabEntity) = withContext(Dispatchers.IO) {
        tabDao.update(tab)
    }

    suspend fun setActive(id: Long) = withContext(Dispatchers.IO) {
        tabDao.setActive(id)
    }

    suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        tabDao.getCount()
    }
    
    suspend fun updatePositions(tabs: List<TabEntity>) = withContext(Dispatchers.IO) {
        tabs.forEachIndexed { index, tab ->
            tabDao.update(tab.copy(position = index))
        }
    }
}
