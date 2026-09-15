package com.darkreader.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentUri: String,
    val documentName: String,
    val pageNumber: Int,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
