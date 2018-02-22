package me.cooper.rick.crowdcontrollerclient.domain.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.ForeignKey.CASCADE
import android.arch.persistence.room.PrimaryKey
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto

@Entity(tableName = "friends",
        foreignKeys = [
            ForeignKey(entity = UserEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["userId"],
                    onDelete = CASCADE)
        ])
data class FriendEntity(@PrimaryKey(autoGenerate = true) var id: Long? = null,
                        var userId: Long = -1,
                        var friendId: Long = -1,
                        var username: String = "",
                        var isInviter: Boolean = false,
                        var activated: Boolean = false) {

    fun toDto(): FriendDto {
        return FriendDto(friendId, username, isInviter, activated)
    }

    companion object {

        fun fromDto(userDto: UserDto): Set<FriendEntity> {
            return userDto.friends.map { FriendEntity.fromDto(userDto.id, it) }.toSet()
        }

        fun fromDto(userId: Long, dto: FriendDto): FriendEntity {
            return FriendEntity(
                    userId = userId,
                    friendId = dto.id,
                    username = dto.username,
                    isInviter = dto.isInviter,
                    activated = dto.activated)
        }

    }
}