package me.cooper.rick.crowdcontrollerclient.domain.repository

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import me.cooper.rick.crowdcontrollerclient.domain.entity.UserEntity

@Dao
interface UserDao {

    @Insert
    fun insert(userEntity: UserEntity)

    @Query("SELECT * FROM user LIMIT 1")
    fun select(): UserEntity?

    @Query("SELECT 1 FROM user LIMIT 1")
    fun isToken(): Int

    @Query("DELETE FROM user")
    fun clear()

}