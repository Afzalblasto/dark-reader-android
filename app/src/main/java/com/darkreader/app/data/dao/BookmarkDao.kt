package com.darkreader.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.darkreader.app.data.entity.BookmarkEntity

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY documentName, pageNumber")
    fun getAll(): LiveData<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY documentName, pageNumber")
    fun getAllSync(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE documentUri = :documentUri ORDER BY pageNumber")
    fun getByDocument(documentUri: String): LiveData<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE documentUri = :documentUri ORDER BY pageNumber")
    fun getByDocumentSync(documentUri: String): List<BookmarkEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE documentUri = :documentUri AND pageNumber = :pageNumber)")
    fun isBookmarked(documentUri: String, pageNumber: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookmark: BookmarkEntity): Long

    @Delete
    fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks WHERE documentUri = :documentUri")
    fun deleteByDocument(documentUri: String)

    @Update
    fun update(bookmark: BookmarkEntity)
}
