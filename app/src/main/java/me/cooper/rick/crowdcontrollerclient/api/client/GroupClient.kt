package me.cooper.rick.crowdcontrollerclient.api.client

import io.reactivex.Completable
import io.reactivex.Observable
import me.cooper.rick.crowdcontrollerapi.dto.group.CreateGroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupSettingsDto
import retrofit2.http.*

interface GroupClient {

    @GET(BASE_PATH)
    fun findAll(): Observable<List<GroupDto>>

    @GET("$BASE_PATH/{groupId}")
    fun find(@Path("groupId") groupId: Long): Observable<GroupDto>

    @POST(BASE_PATH)
    fun create(@Body groupDto: CreateGroupDto): Observable<GroupDto>

    @PUT("$BASE_PATH/{groupId}")
    fun update(@Path("groupId") groupId: Long,
               @Body groupDto: GroupDto): Observable<GroupDto>

    @PUT("$BASE_PATH/{groupId}/settings")
    fun updateSettings(@Path("groupId") groupId: Long,
               @Body groupSettingsDto: GroupSettingsDto): Observable<GroupDto>

    @PATCH("$BASE_PATH/{groupId}/members/{userId}")
    fun acceptInvite(@Path("groupId") groupId: Long,
                     @Path("userId") userId: Long): Observable<GroupDto>

    @DELETE("$BASE_PATH/{groupId}")
    fun remove(@Path("groupId") groupId: Long): Completable

    @DELETE("$BASE_PATH/{groupId}/members/{userId}")
    fun removeMember(@Path("groupId") groupId: Long,
                     @Path("userId") userId: Long): Observable<GroupDto>

    companion object {
        const val BASE_PATH = "/groups"
    }

}
