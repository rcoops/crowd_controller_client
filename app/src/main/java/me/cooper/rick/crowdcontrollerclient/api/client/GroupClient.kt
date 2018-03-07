package me.cooper.rick.crowdcontrollerclient.api.client

import io.reactivex.Observable
import me.cooper.rick.crowdcontrollerapi.dto.CreateGroupDto
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import retrofit2.Call
import retrofit2.http.*


interface GroupClient {

    @GET(BASE_PATH)
    fun findAll(): Call<List<GroupDto>>

    @GET("$BASE_PATH/{groupId}")
    fun find(@Path("groupId") groupId: Long): Call<GroupDto>

    @GET("$BASE_PATH/{groupId}")
    fun groupObservable(@Path("groupId") groupId: Long): Observable<GroupDto>

    @POST(BASE_PATH)
    fun create(@Body groupDto: CreateGroupDto): Call<GroupDto>

    @PUT("$BASE_PATH/{groupId}")
    fun update(@Path("groupId") groupId: Long,
               @Body groupDto: GroupDto): Call<GroupDto>

    @PATCH("$BASE_PATH/{groupId}/members/{userId}")
    fun acceptInvite(@Path("groupId") groupId: Long,
                     @Path("userId") userId: Long)

    @DELETE("$BASE_PATH/{groupId}")
    fun remove(@Path("groupId") groupId: Long): Call<Boolean>

    companion object {
        const val BASE_PATH = "/groups"
    }

}
