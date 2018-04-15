package me.cooper.rick.crowdcontrollerclient.api.client

import io.reactivex.Observable
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.group.LocationDto
import me.cooper.rick.crowdcontrollerapi.dto.user.RegistrationDto
import me.cooper.rick.crowdcontrollerapi.dto.user.UserDto
import retrofit2.http.*


interface UserClient {

    /* USER */

    @GET(BASE_PATH)
    fun findAll(): Observable<List<UserDto>>

    @GET("$BASE_PATH/{userId}")
    fun find(@Path("userId") userId: Long): Observable<UserDto>

    @GET("$BASE_PATH/{userId}/directions")
    fun directions(@Path("userId") userId: Long): Observable<List<LocationDto>?>

    @POST(BASE_PATH)
    fun create(@Body registrationDto: RegistrationDto): Observable<UserDto>

    @PATCH("$BASE_PATH/{userId}/location")
    fun updateLocation(@Path("userId") userId: Long,
                       @Body locationDto: LocationDto): Observable<UserDto>

    @GET(FRIENDS_BASE_PATH)
    fun findFriends(@Path("userId") userId: Long): Observable<List<FriendDto>>

    @POST(FRIENDS_BASE_PATH)
    fun addFriend(@Path("userId") userId: Long,
                  @Body friendDto: FriendDto): Observable<List<FriendDto>>

    @PATCH("$FRIENDS_BASE_PATH/{friendId}")
    fun updateFriendship(@Path("userId") userId: Long,
                         @Path("friendId") friendId: Long,
                         @Body friendDto: FriendDto): Observable<List<FriendDto>>

    @DELETE("$FRIENDS_BASE_PATH/{friendId}")
    fun removeFriend(@Path("userId") userId: Long,
                     @Path("friendId") friendId: Long): Observable<List<FriendDto>>

    companion object {
        const val BASE_PATH = "/users"
        const val FRIENDS_BASE_PATH = "$BASE_PATH/{userId}/friends"
    }

}
