package me.cooper.rick.crowdcontrollerclient.domain.repository

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import me.cooper.rick.crowdcontrollerclient.domain.entity.FriendEntity


@Dao
interface FriendsDao {

    @Insert
    fun insertAll(friends: Set<FriendEntity>)

    @Query("DELETE FROM friends")
    fun clear()

    @Query("SELECT * FROM friends")
    fun select(): List<FriendEntity>

}