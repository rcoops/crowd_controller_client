package me.cooper.rick.crowdcontrollerclient.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "token")
data class TokenEntity(@PrimaryKey var id: Long = -1,
                  @ColumnInfo(name = "access_token") var accessToken: String? = null,
                  @ColumnInfo(name = "token_type") var tokenType: String = "Bearer",
                  @ColumnInfo(name = "expires_in") var expiresIn: Int = -1,
                  @ColumnInfo(name = "scope") var scope: String = "read",
                  @ColumnInfo(name = "jti") var jti: String? = null) {

    companion object {
        fun fromDto(dto: me.cooper.rick.crowdcontrollerapi.dto.Token): TokenEntity =
                TokenEntity(
                        accessToken = dto.accessToken,
                        tokenType = dto.tokenType,
                        expiresIn = dto.expiresIn,
                        scope = dto.scope,
                        jti = dto.jti
                )
    }
}