package com.darkreader.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.darkreader.app.data.entity.TabEntity

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY position")
    fun getAll(): LiveData<List<TabEntity>>

    @Query("SELECT * FROM tabs ORDER BY position")
    fun getAllSync(): List<TabEntity>

    @Query("SELECT * FROM tabs WHERE isActive = 1 LIMIT 1")
    fun getActive(): TabEntity?

    @Query("SELECT * FROM tabs WHERE id = :id LIMIT 1")
    fun getById(id: Long): TabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tab: TabEntity): Long

    @Delete
    fun delete(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM tabs")
    fun deleteAll()

    @Update
    fun update(tab: TabEntity)

    @Query("UPDATE tabs SET isActive = 1 WHERE id = :id")
    fun activate(id: Long)

    @Query("UPDATE tabs SET isActive = 0 WHERE id != :id")
    fun deactivateOthers(id: Long)

    @Transaction
    fun setActive(id: Long) {
        deactivateOthers(id)
        activate(id)
    }

    @Query("SELECT COUNT(*) FROM tabs")
    fun getCount(): Int
}
