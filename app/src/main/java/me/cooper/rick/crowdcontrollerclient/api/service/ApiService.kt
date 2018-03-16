package me.cooper.rick.crowdcontrollerclient.api.service

import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.SharedPreferences
import kotlinx.android.synthetic.main.activity_main.*
import me.cooper.rick.crowdcontrollerapi.dto.group.CreateGroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupMemberDto
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.App.Companion.currentActivity
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.R.id.nav_view
import me.cooper.rick.crowdcontrollerclient.activity.MainActivity
import me.cooper.rick.crowdcontrollerclient.activity.MainActivity.Companion.BACK_STACK_ROOT_TAG
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.fragment.LocationFragment
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator.createService
import me.cooper.rick.crowdcontrollerclient.util.call

object ApiService {

    private val sharedPrefRef = App.context.getString(R.string.user_details)
    private val tokenRef = App.context.getString(R.string.token)
    private val userIdRef = App.context.getString(R.string.user_id)

    private val pref: SharedPreferences = App.context
            .getSharedPreferences(sharedPrefRef, MODE_PRIVATE)

    val friends = mutableListOf<FriendDto>()
    var group: GroupDto? = null

    private val refreshGroup: ((GroupDto?) -> Unit) = { refreshGroupDetails(it) }
    private val refreshFriends: ((List<FriendDto>) -> Unit) = { updateFriends(it) }
    var errorConsumer: ((Throwable) -> Unit)? = null

    fun updateFriends(friendDtos: List<FriendDto>) {
        friends.clear()
        friends.addAll(friendDtos)
        (currentActivity as? MainActivity)?.apply {
            (supportFragmentManager
                    ?.findFragmentById(R.id.content_main) as? FriendFragment)?.updateView()
            dismissAfterTask()
        }
    }


    fun getFriends() {
        userClient().findFriends(getUserId()).call(refreshFriends, errorConsumer!!)
    }

    fun addFriend(username: String) {
        userClient().addFriend(getUserId(), FriendDto(username = username))
                .call(refreshFriends, errorConsumer!!)
    }

    fun removeFriend(dto: FriendDto) {
        userClient().removeFriend(getUserId(), dto.id).call(refreshFriends, errorConsumer!!)
    }

    fun updateFriendship(dto: FriendDto) {
        userClient().updateFriendship(getUserId(), dto.id, dto).call(refreshFriends, errorConsumer!!)
    }

    fun getGroup(id: Long? = null, consumer: ((GroupDto) -> Unit)? = null) {
        val groupClient = groupClient()
        if (id != null ) groupClient.find(id).call(consumer ?: refreshGroup, errorConsumer!!)
        else group?.let { groupClient.find(it.id).call(consumer ?: refreshGroup, errorConsumer!!) }
    }

    fun createGroup(friends: List<FriendDto>, consumer: (GroupDto) -> Unit) {
        groupClient().create(CreateGroupDto(getUserId(), mapToGroupMembers(friends))).call(consumer, errorConsumer!!)
    }

    fun removeGroup(consumer: () -> Unit) {
        group?.let { groupClient().remove(it.id).call(consumer, errorConsumer!!) }
    }

    fun removeGroupMember(userId: Long, errorConsumer: ((Throwable) -> Unit)? = null) {
        group?.let {
            groupClient().removeMember(it.id, userId).call(refreshGroup, errorConsumer ?: this.errorConsumer!!)
        }
    }

    private fun updateGroup(dto: GroupDto) {
        group?.let { groupClient().update(it.id, dto).call(refreshGroup, errorConsumer!!) }
    }

    fun promoteToAdmin(dto: GroupMemberDto) {
        group?.let { updateGroup(it.copy(adminId = dto.id)) }
    }

    fun addGroupMembers(friendsToAdd: List<FriendDto>) {
        if (friendsToAdd.isEmpty()) {
            refreshGroup?.let { it(group) }
        } else {
            group?.let { updateGroupMembers((it.members + mapToGroupMembers(friendsToAdd))) }
        }
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

    fun refreshGroupDetails(dto: GroupDto?) {
        if (isGroupMember(dto)) updateGroupDetails(dto!!) else setNoGroup()
    }

    private fun getToken(): String = pref.getString(tokenRef, null)
    private fun getUserId(): Long = pref.getLong(userIdRef, -1)

    private fun userClient(): UserClient = createService(UserClient::class, getToken())
    private fun groupClient(): GroupClient = createService(GroupClient::class, getToken())

    private fun isGroupMember(dto: GroupDto?) =
            dto != null && getUserId() in dto.members.map { it.id }

    private fun updateGroupDetails(dto: GroupDto) {
        group = dto
        val currentActivity = App.currentActivity
        val currentFragment = currentActivity?.supportFragmentManager?.findFragmentById(R.id.content_main)
        when (currentFragment) {
            is GroupFragment -> currentFragment.updateGroup(dto)
            is LocationFragment -> currentFragment.updateView(dto.location)
        }
        (currentActivity as? MainActivity)?.setAdminVisibility(getUserId() == group?.adminId)
        getFriends()
    }

    private fun setNoGroup() {
        group = null
        (currentActivity as? MainActivity)?.apply {
            supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, 0)
            getFriends()
            setAdminVisibility(false)
        }
    }

    private fun updateGroupMembers(groupMembers: List<GroupMemberDto>) {
        group?.let { updateGroup(it.copy(members = groupMembers)) }
    }

    private fun mapToGroupMembers(friends: List<FriendDto>): List<GroupMemberDto> {
        return friends.map { GroupMemberDto.fromFriendDto(it) }
    }

}
