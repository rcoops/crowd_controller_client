package me.cooper.rick.crowdcontrollerclient.api.client

import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.RegistrationDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import retrofit2.Call
import retrofit2.http.*

interface UserClient {

    /* USER */

    @GET(BASE_PATH)
    fun users(): Call<List<UserDto>?>

    @GET("/{userId}")
    fun user(@Path("userId") userId: Long): Call<UserDto>

    @POST(BASE_PATH)
    fun create(@Body registrationDto: RegistrationDto): Call<UserDto>

    @PUT("$BASE_PATH/{userId}")
    fun update(@Path("userId") userId: Long, @Body userDto: UserDto): Call<UserDto>

    /* FRIENDS */

    @GET("$FRIENDS_BASE_PATH")
    fun friends(@Path("userId") userId: Long): Call<List<FriendDto>>

    @PUT("$FRIENDS_BASE_PATH/{friendIdentifier}")
    fun addFriend(@Path("userId") userId: Long,
                  @Path("friendIdentifier") friendIdentifier: String): Call<List<FriendDto>>

    @PUT("$FRIENDS_BASE_PATH/{friendId}/{isAccepting}")
    fun respondToFriendRequest(@Path("userId") userId: Long,
                               @Path("friendId") friendId: Long,
                               @Path("isAccepting") isAccepting: Boolean): Call<List<FriendDto>>

    @DELETE("$FRIENDS_BASE_PATH/{friendId}")
    fun removeFriend(@Path("userId") userId: Long,
                     @Path("friendId") friendId: Long): Call<List<FriendDto>>

    companion object {
        const val BASE_PATH = "/users"
        const val FRIENDS_BASE_PATH = "$BASE_PATH/{userId}/friends"
    }
    
}
