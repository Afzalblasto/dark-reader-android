package com.darkreader.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentUri: String,
    val fileName: String,
    val fileType: String,
    val lastPage: Int = 0,
    val position: Int = 0,
    val openedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)
