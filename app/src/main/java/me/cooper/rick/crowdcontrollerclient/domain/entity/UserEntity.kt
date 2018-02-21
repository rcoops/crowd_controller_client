package me.cooper.rick.crowdcontrollerclient.domain.entity

import android.arch.persistence.room.*
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto

/**
 * Created by rick on 16/02/18.
 */
@Entity(tableName = "user")
data class UserEntity(@PrimaryKey(autoGenerate = true) var id: Long = -1,
                      @ColumnInfo(name = "username") var username: String = "",
                      @ColumnInfo(name = "email") var email: String = "",
                      @ColumnInfo(name = "role") var role: String = "") {

    companion object {
        fun fromDto(dto: UserDto): UserEntity =
                UserEntity(
                        id = dto.id,
                        username = dto.username,
                        email = dto.email,
                        role = dto.roles.last()
                )
    }
}