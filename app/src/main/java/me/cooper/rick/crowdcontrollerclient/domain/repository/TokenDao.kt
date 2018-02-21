package me.cooper.rick.crowdcontrollerclient.domain.repository

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import me.cooper.rick.crowdcontrollerclient.domain.entity.TokenEntity

@Dao
interface TokenDao {

    @Insert
    fun insert(tokenEntity: TokenEntity)

    @Query("SELECT * FROM token LIMIT 1")
    fun select(): TokenEntity?

    @Query("SELECT 1 FROM token LIMIT 1")
    fun isToken(): Int

    @Query("DELETE FROM token")
    fun clear()

}