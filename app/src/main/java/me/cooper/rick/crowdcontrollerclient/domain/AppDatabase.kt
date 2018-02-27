package me.cooper.rick.crowdcontrollerclient.domain

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import me.cooper.rick.crowdcontrollerclient.App.Companion.context
import me.cooper.rick.crowdcontrollerclient.domain.entity.TokenEntity
import me.cooper.rick.crowdcontrollerclient.domain.entity.UserEntity
import me.cooper.rick.crowdcontrollerclient.domain.repository.TokenDao
import me.cooper.rick.crowdcontrollerclient.domain.repository.UserDao


@Database(entities = [UserEntity::class], version = 10)
abstract class AppDatabase : RoomDatabase() {

//    abstract fun tokenDao(): TokenDao

    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDb(context).also { instance = it }
            }
        }

        private fun buildDb(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "app-db")
                    .fallbackToDestructiveMigration()
                    .build()
        }
    }
}