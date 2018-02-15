package me.cooper.rick.crowdcontrollerclient.auth

import me.cooper.rick.crowdcontrollerapi.dto.RegistrationDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import retrofit2.Call
import retrofit2.http.*

interface UserClient {

    @GET("/users/{username}")
    fun me(@Path("username") username: String): Call<UserDto>

    @GET("/users")
    fun users(): Call<List<UserDto>>

    @POST("/users")
    fun create(@Body registrationDto: RegistrationDto): Call<UserDto>

    @PUT("/users/{username}")
    fun update(@Path("username") username: String, @Body userDto: UserDto): Call<UserDto>

}