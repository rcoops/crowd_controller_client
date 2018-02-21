package me.cooper.rick.crowdcontrollerclient.domain.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto

@Entity(tableName = "friends",
        foreignKeys = [
            ForeignKey(entity = UserEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["userId"])
        ])
class FriendEntity(@PrimaryKey(autoGenerate = true) var id: Long = -1,
                   var userId: Long = -1,
                   var tag: String = "",
                   var isInviter: Boolean = false,
                   var activated: Boolean = false) {

    companion object {

        fun fromDto(userDto: UserDto): Set<FriendEntity> {
            return userDto.friends.map { FriendEntity.fromDto(userDto.id, it) }.toSet()
        }

        private fun fromDto(userId: Long, dto: FriendDto): FriendEntity {
            return FriendEntity(userId = userId,
                    tag = dto.tag,
                    isInviter = dto.isInviter,
                    activated = dto.activated)
        }

    }
}