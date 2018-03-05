package me.cooper.rick.crowdcontrollerclient.api.task.friends

import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerclient.api.task.AbstractClientTask
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient

abstract class AbstractFriendTask(consumer: (List<FriendDto>) -> Unit)
    : AbstractClientTask<UserClient, List<FriendDto>>(consumer, UserClient::class)
