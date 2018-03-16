package me.cooper.rick.crowdcontrollerclient.api.service

import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.SharedPreferences
import me.cooper.rick.crowdcontrollerapi.dto.group.CreateGroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupMemberDto
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator.createService
import me.cooper.rick.crowdcontrollerclient.util.call

object ApiService {

    private val sharedPrefRef = App.context!!.getString(R.string.user_details)
    private val tokenRef = App.context!!.getString(R.string.token)
    private val userIdRef = App.context!!.getString(R.string.user_id)

    private val pref: SharedPreferences = App.context!!
            .getSharedPreferences(sharedPrefRef, MODE_PRIVATE)

    val friends = mutableListOf<FriendDto>()
    var group: GroupDto? = null

    var refreshGroup: ((GroupDto?) -> Unit)? = null
    var refreshFriends: ((List<FriendDto>) -> Unit)? = null
    var errorConsumer: ((Throwable) -> Unit)? = null

    private fun getToken(): String = pref.getString(tokenRef, null)
    private fun getUserId(): Long = pref.getLong(userIdRef, -1)

    private fun userClient(): UserClient = createService(UserClient::class, getToken())
    private fun groupClient(): GroupClient = createService(GroupClient::class, getToken())

    fun getFriends() {
        userClient().findFriends(getUserId()).call(refreshFriends!!, errorConsumer!!)
    }

    fun addFriend(username: String) {
        userClient().addFriend(getUserId(), FriendDto(username = username))
                .call(refreshFriends!!, errorConsumer!!)
    }

    fun removeFriend(dto: FriendDto) {
        userClient().removeFriend(getUserId(), dto.id).call(refreshFriends!!, errorConsumer!!)
    }

    fun updateFriendship(dto: FriendDto) {
        userClient().updateFriendship(getUserId(), dto.id, dto).call(refreshFriends!!, errorConsumer!!)
    }

    fun getGroup(id: Long? = null, consumer: ((GroupDto) -> Unit)? = null) {
        val groupClient = groupClient()
        if (id != null ) groupClient.find(id).call(consumer ?: refreshGroup!!, errorConsumer!!)
        else group?.let { groupClient.find(it.id).call(consumer ?: refreshGroup!!, errorConsumer!!) }
    }

    fun createGroup(friends: List<FriendDto>, consumer: (GroupDto) -> Unit) {
        groupClient().create(CreateGroupDto(getUserId(), mapToGroupMembers(friends))).call(consumer, errorConsumer!!)
    }

    fun removeGroup(consumer: () -> Unit) {
        group?.let { groupClient().remove(it.id).call(consumer, errorConsumer!!) }
    }

    fun removeGroupMember(userId: Long, errorConsumer: ((Throwable) -> Unit)? = null) {
        group?.let {
            groupClient().removeMember(it.id, userId).call(refreshGroup!!, errorConsumer ?: this.errorConsumer!!)
        }
    }

    fun updateGroup(dto: GroupDto) {
        group?.let { groupClient().update(it.id, dto).call(refreshGroup!!, errorConsumer!!) }
    }

    fun promoteToAdmin(dto: GroupMemberDto) {
        group?.let { updateGroup(it.copy(adminId = dto.id)) }
    }

    private fun updateGroupMembers(groupMembers: List<GroupMemberDto>) {
        group?.let { updateGroup(it.copy(members = groupMembers)) }
    }

    fun addGroupMembers(friendsToAdd: List<FriendDto>) {
        if (friendsToAdd.isEmpty()) {
            refreshGroup?.let { it(group) }
        } else {
            group?.let { updateGroupMembers((it.members + mapToGroupMembers(friendsToAdd))) }
        }
    }

    fun updateFriends(friendDtos: List<FriendDto>) {
        friends.clear()
        friends.addAll(friendDtos)
    }

    fun getUnGroupedFriendNames(): Array<String> {
        return friends
                .filter(FriendDto::canJoinGroup)
                .map { it.username }
                .toTypedArray()
    }

    fun selectFriends(unGroupedNames: Array<String>,
                      selectedFriends: MutableList<FriendDto>): (DialogInterface, Int, Boolean) -> Unit {
        return { _, i, checked ->
            val friend = friends.find { it.username == unGroupedNames[i] }!!
            selectedFriends.apply { if (checked) add(friend) else remove(friend) }
        }
    }

    private fun mapToGroupMembers(friends: List<FriendDto>): List<GroupMemberDto> {
        return friends.map { GroupMemberDto.fromFriendDto(it) }
    }

}
