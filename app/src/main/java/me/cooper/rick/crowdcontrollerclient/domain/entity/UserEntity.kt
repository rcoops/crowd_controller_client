package me.cooper.rick.crowdcontrollerclient.domain.entity

import android.arch.persistence.room.*
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto

/**
 * Created by rick on 16/02/18.
 */
@Entity(tableName = "user")
data class UserEntity(@PrimaryKey var id: Long? = null,
                      @ColumnInfo(name = "username") var username: String = "",
                      @ColumnInfo(name = "email") var email: String = "",
                      @ColumnInfo(name = "mobile_number") var mobileNumber: String = "",
                      @ColumnInfo(name = "role") var roles: String = "",
                      @ColumnInfo(name = "group") var group: Long? = null) {

    fun toDto(): UserDto {
        return UserDto(
                id!!,
                username,
                email,
                mobileNumber,
                roles = roles.split(",").toSet(),
                group = group
        )
    }

    companion object {
        fun fromDto(dto: UserDto): UserEntity {
            return UserEntity(
                    dto.id,
                    dto.username,
                    dto.email,
                    dto.mobileNumber,
                    dto.roles.joinToString(","),
                    dto.group
            )
        }
    }
}