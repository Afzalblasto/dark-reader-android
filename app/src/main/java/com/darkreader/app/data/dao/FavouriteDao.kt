package com.darkreader.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.darkreader.app.data.entity.FavouriteEntity

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourites ORDER BY addedAt DESC")
    fun getAll(): LiveData<List<FavouriteEntity>>

    @Query("SELECT * FROM favourites ORDER BY addedAt DESC")
    fun getAllSync(): List<FavouriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE filePath = :filePath)")
    fun isFavourite(filePath: String): Boolean

    @Query("SELECT * FROM favourites WHERE filePath = :filePath LIMIT 1")
    fun getByPath(filePath: String): FavouriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(favourite: FavouriteEntity): Long

    @Delete
    fun delete(favourite: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE filePath = :filePath")
    fun deleteByPath(filePath: String)
}
