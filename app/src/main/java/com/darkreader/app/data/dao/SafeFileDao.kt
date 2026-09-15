package com.darkreader.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.darkreader.app.data.entity.SafeFileEntity

@Dao
interface SafeFileDao {
    @Query("SELECT * FROM safe_files")
    fun getAll(): LiveData<List<SafeFileEntity>>

    @Query("SELECT * FROM safe_files")
    fun getAllSync(): List<SafeFileEntity>

    @Query("SELECT * FROM safe_files WHERE storedName = :storedName LIMIT 1")
    fun getByStoredName(storedName: String): SafeFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(safeFile: SafeFileEntity): Long

    @Delete
    fun delete(safeFile: SafeFileEntity)

    @Query("DELETE FROM safe_files WHERE storedName = :storedName")
    fun deleteByStoredName(storedName: String)
}
