package me.cooper.rick.crowdcontrollerclient.domain.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import me.cooper.rick.crowdcontrollerapi.dto.Token

@Entity(tableName = "token")
data class TokenEntity(@PrimaryKey var id: Long? = null,
                       @ColumnInfo(name = "access_token") var accessToken: String? = null,
                       @ColumnInfo(name = "token_type") var tokenType: String = "Bearer",
                       @ColumnInfo(name = "expires_in") var expiresIn: Int = -1,
                       @ColumnInfo(name = "scope") var scope: String = "read",
                       @ColumnInfo(name = "jti") var jti: String? = null) {


    fun toTokenString(): String = "${tokenType.capitalize()} $accessToken"

    companion object {

        fun fromDto(dto: Token): TokenEntity {
            return TokenEntity(
                    accessToken = dto.accessToken,
                    tokenType = dto.tokenType.capitalize(),
                    expiresIn = dto.expiresIn,
                    scope = dto.scope,
                    jti = dto.jti
            )
        }

    }
}