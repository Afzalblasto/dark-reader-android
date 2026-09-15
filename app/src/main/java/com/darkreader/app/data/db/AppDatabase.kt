package com.darkreader.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.darkreader.app.data.dao.BookmarkDao
import com.darkreader.app.data.dao.FavouriteDao
import com.darkreader.app.data.dao.SafeFileDao
import com.darkreader.app.data.dao.SettingDao
import com.darkreader.app.data.dao.TabDao
import com.darkreader.app.data.entity.BookmarkEntity
import com.darkreader.app.data.entity.FavouriteEntity
import com.darkreader.app.data.entity.SafeFileEntity
import com.darkreader.app.data.entity.SettingEntity
import com.darkreader.app.data.entity.TabEntity

@Database(
    entities = [
        BookmarkEntity::class,
        FavouriteEntity::class,
        TabEntity::class,
        SafeFileEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun tabDao(): TabDao
    abstract fun safeFileDao(): SafeFileDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "darkreader.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
