package me.cooper.rick.crowdcontrollerclient.api.task.group

import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import me.cooper.rick.crowdcontrollerclient.api.task.AbstractClientTask

abstract class AbstractGroupTask(consumer: (GroupDto) -> Unit)
    : AbstractClientTask<GroupClient, GroupDto>(consumer, GroupClient::class)
