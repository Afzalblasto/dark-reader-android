package com.darkreader.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.darkreader.app.data.entity.SettingEntity

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    fun delete(key: String)
}
