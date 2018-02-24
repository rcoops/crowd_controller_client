package me.cooper.rick.crowdcontrollerclient.api.client

import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.RegistrationDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import retrofit2.Call
import retrofit2.http.*

interface UserClient {

    /* USER */

    @GET("/users")
    fun users(): Call<List<UserDto>?>

    @GET("/users/{userId}")
    fun user(@Path("userId") userId: Long): Call<UserDto>

    @POST("/users")
    fun create(@Body registrationDto: RegistrationDto): Call<UserDto>

    @PUT("/users/{userId}")
    fun update(@Path("userId") userId: Long, @Body userDto: UserDto): Call<UserDto>

    /* FRIENDS */

    @GET("/users/{userId}/friends")
    fun friends(@Path("userId") userId: Long): Call<Set<FriendDto>>

    @PUT("/users/{userId}/friends/{friendIdentifier}")
    fun addFriend(@Path("userId") userId: Long,
                  @Path("friendIdentifier") friendIdentifier: String): Call<Set<FriendDto>>

    @PUT("/users/{userId}/friends/{friendId}/accept")
    fun acceptFriendRequest(@Path("userId") userId: Long,
                            @Path("friendId") friendId: Long): Call<Set<FriendDto>>

    @DELETE("/users/{userId}/friends/{friendId}")
    fun removeFriend(@Path("userId") userId: Long,
                     @Path("friendId") friendId: Long): Call<Set<FriendDto>>

}