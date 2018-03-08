package me.cooper.rick.crowdcontrollerclient.api.client

import io.reactivex.Observable
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.LocationDto
import me.cooper.rick.crowdcontrollerapi.dto.RegistrationDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import retrofit2.Call
import retrofit2.http.*


interface UserClient {

    /* USER */

    @GET(BASE_PATH)
    fun users(): Observable<List<UserDto>>

    @GET("$BASE_PATH/{userId}")
    fun userO(@Path("userId") userId: Long): Observable<UserDto>

    @GET("$BASE_PATH/{userId}")
    fun user(@Path("userId") userId: Long): Call<UserDto>

    @POST(BASE_PATH)
    fun create(@Body registrationDto: RegistrationDto): Call<UserDto>

    @PATCH("$BASE_PATH/{userId}/location")
    fun updateLocation(@Path("userId") userId: Long,
                       @Body locationDto: LocationDto): Call<UserDto>

    /* FRIENDS */

    @GET(FRIENDS_BASE_PATH)
    fun friends(@Path("userId") userId: Long): Call<List<FriendDto>>

    @POST(FRIENDS_BASE_PATH)
    fun addFriend(@Path("userId") userId: Long,
                  @Body friendDto: FriendDto): Call<List<FriendDto>>

    @PATCH("$FRIENDS_BASE_PATH/{friendId}")
    fun updateFriendship(@Path("userId") userId: Long,
                         @Path("friendId") friendId: Long,
                         @Body friendDto: FriendDto): Call<List<FriendDto>>

    @DELETE("$FRIENDS_BASE_PATH/{friendId}")
    fun removeFriend(@Path("userId") userId: Long,
                     @Path("friendId") friendId: Long): Call<List<FriendDto>>

    companion object {
        const val BASE_PATH = "/users"
        const val FRIENDS_BASE_PATH = "$BASE_PATH/{userId}/friends"
    }

}
