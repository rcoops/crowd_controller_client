package me.cooper.rick.crowdcontrollerclient.api.client

import me.cooper.rick.crowdcontrollerapi.dto.user.Token
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

interface LoginClient {

    @POST("/oauth/token")
    fun getToken(
            @Query("grant_type") grantType: String,
            @Query("username") username: String,
            @Query("password") password: String): Call<Token>

}