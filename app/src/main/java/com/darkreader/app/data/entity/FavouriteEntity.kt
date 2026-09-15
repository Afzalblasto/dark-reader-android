package com.darkreader.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val fileType: String,
    val isDirectory: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
