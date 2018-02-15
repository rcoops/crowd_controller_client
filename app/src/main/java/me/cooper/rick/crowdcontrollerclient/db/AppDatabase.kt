package me.cooper.rick.crowdcontrollerclient.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase


@Database(entities = [TokenEntity::class], version = 1)
abstract class AppDatabase: RoomDatabase() {

    abstract fun tokenDao(): TokenDao

}