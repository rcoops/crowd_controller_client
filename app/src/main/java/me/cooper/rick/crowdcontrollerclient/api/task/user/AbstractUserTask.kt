package me.cooper.rick.crowdcontrollerclient.api.task.user

import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.api.task.AbstractClientTask

/**
 * Created by rick on 07/03/18.
 */
abstract class AbstractUserTask(consumer: (UserDto) -> Unit)
    : AbstractClientTask<UserClient, UserDto>(consumer, UserClient::class)
