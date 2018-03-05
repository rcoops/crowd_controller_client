package me.cooper.rick.crowdcontrollerclient.api.task.friends

import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import retrofit2.Call

class AddFriend(private val friendDto: FriendDto, consumer: (List<FriendDto>) -> Unit)
    : AbstractFriendTask(consumer) {

    override fun buildCall(client: UserClient, id: Long): Call<List<FriendDto>> {
        return client.addFriend(id, friendDto)
    }

}
