package me.cooper.rick.crowdcontrollerclient.api.task.group

import me.cooper.rick.crowdcontrollerapi.dto.CreateGroupDto
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import retrofit2.Call

class CreateGroup(private val friendIds: List<Long>,
                  consumer: (GroupDto) -> Unit) : AbstractGroupTask(consumer) {

    override fun buildCall(client: GroupClient, id: Long): Call<GroupDto> {
        return client.create(CreateGroupDto(id, friendIds))
    }

}
