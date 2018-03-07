package me.cooper.rick.crowdcontrollerclient.api.task.group

import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import retrofit2.Call

class GetGroup(private val groupId: Long, consumer: (GroupDto) -> Unit)
    : AbstractGroupTask(consumer) {

    override fun buildCall(client: GroupClient, id: Long): Call<GroupDto> {
        return client.find(groupId)
    }

}
