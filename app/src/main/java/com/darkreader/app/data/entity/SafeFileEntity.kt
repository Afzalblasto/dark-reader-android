package com.darkreader.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safe_files")
data class SafeFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalName: String,
    val storedName: String,
    val fileType: String,
    val fileSize: Long = 0,
    val addedAt: Long = System.currentTimeMillis()
)
