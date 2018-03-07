package me.cooper.rick.crowdcontrollerclient.api.client

import io.reactivex.Observable
import me.cooper.rick.crowdcontrollerapi.dto.CreateGroupDto
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import retrofit2.Call
import retrofit2.http.*


interface GroupClient {

    @GET(BASE_PATH)
    fun groups(): Call<List<GroupDto>>

    @GET("$BASE_PATH/{groupId}")
    fun group(@Path("groupId") groupId: Long): Call<GroupDto>

    @GET("$BASE_PATH/{groupId}")
    fun groupObservable(@Path("groupId") groupId: Long): Observable<GroupDto>

    @POST(BASE_PATH)
    fun create(@Body groupDto: CreateGroupDto): Call<GroupDto>

    @PUT("$BASE_PATH/{groupId}")
    fun updateGroup(@Path("groupId") groupId: Long,
                    @Body groupDto: GroupDto): Call<GroupDto>

    @PUT("$BASE_PATH/{groupId}/members/{userId}")
    fun addToGroup(@Path("groupId") groupId: Long,
                   @Path("userId") userId: Long): Call<GroupDto>

    @DELETE("$BASE_PATH/{groupId}/members/{userId}")
    fun removeFromGroup(@Path("groupId") groupId: Long,
                        @Path("userId") userId: Long): Call<GroupDto>

    @DELETE("$BASE_PATH/{groupId}")
    fun removeGroup(@Path("groupId") groupId: Long): Call<Boolean>

    companion object {
        const val BASE_PATH = "/groups"
    }

}
