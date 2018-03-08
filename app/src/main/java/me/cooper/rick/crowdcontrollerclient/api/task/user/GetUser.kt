package me.cooper.rick.crowdcontrollerclient.api.task.user

import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import retrofit2.Call


class GetUser(consumer: (UserDto) -> Unit) : AbstractUserTask(consumer) {

    override fun buildCall(client: UserClient, id: Long): Call<UserDto> = client.user(id)

}
