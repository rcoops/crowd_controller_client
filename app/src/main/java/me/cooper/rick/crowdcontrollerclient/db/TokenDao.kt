package me.cooper.rick.crowdcontrollerclient.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

/**
 * Created by rick on 14/02/18.
 */
@Dao
interface TokenDao {

    @Insert
    fun insert(tokenEntity: TokenEntity)

    @Query("SELECT * FROM token LIMIT 1")
    fun getToken(): TokenEntity?

    @Delete
    fun delete(tokenEntity: TokenEntity)

}