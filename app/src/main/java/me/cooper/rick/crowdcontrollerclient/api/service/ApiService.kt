package me.cooper.rick.crowdcontrollerclient.api.service

import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.SharedPreferences
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Observable
import me.cooper.rick.crowdcontrollerapi.dto.group.*
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.user.PasswordResetDto
import me.cooper.rick.crowdcontrollerapi.dto.user.RegistrationDto
import me.cooper.rick.crowdcontrollerapi.dto.user.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity
import me.cooper.rick.crowdcontrollerclient.activity.MainActivity
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.fragment.LocationFragment
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator.createService
import me.cooper.rick.crowdcontrollerclient.util.call
import java.lang.ref.WeakReference

object ApiService {

    private const val sharedPrefRef ="details"
    private const val tokenRef = "token"
    private const val userIdRef = "userId"

    val friends = mutableListOf<FriendDto>()
    var group: GroupDto? = null
    var lastLocation: LatLng? = null
    var destination: LatLng? = null

    private val refreshGroup: ((GroupDto?) -> Unit) = { refreshGroupDetails(it) }
    private val refreshFriends: ((List<FriendDto>) -> Unit) = { updateFriends(it) }
    var errorConsumer: (Throwable) -> Unit = {}

    var currentActivity: WeakReference<AppActivity?> = WeakReference(null)

    var geofence: Geofence? = null
    var geofenceLimit: Double? = null
    lateinit var geofenceCentre: LatLng

    fun setActivity(activity: AppActivity?) {
        currentActivity = WeakReference(activity)
    }

    private fun getSharedPref(): SharedPreferences? {
        return currentActivity.get()?.getSharedPreferences(sharedPrefRef, MODE_PRIVATE)
    }

    fun updateFriends(friendDtos: List<FriendDto>) {
        friends.clear()
        friends.addAll(friendDtos)
        (currentActivity.get() as? MainActivity)?.apply {
            (supportFragmentManager
                    ?.findFragmentById(R.id.content_main) as? FriendFragment)?.updateView()
            dismissAfterTask()
        }
    }

    fun getFriends() {
        userClient().findFriends(getUserId()).call(refreshFriends)
    }

    fun addFriend(username: String) {
        userClient().addFriend(getUserId(), FriendDto(username = username))
                .call(refreshFriends)
    }

    fun removeFriend(dto: FriendDto) {
        userClient().removeFriend(getUserId(), dto.id).call(refreshFriends)
    }

    fun updateFriendship(dto: FriendDto) {
        userClient().updateFriendship(getUserId(), dto.id, dto).call(refreshFriends)
    }

    fun updatePassword(oldPassword: String, newPassword: String,
                       successConsumer: (UserDto) -> Unit) {
        val userId = getUserId()
        userClient().updatePassword(userId, PasswordResetDto(userId, oldPassword, newPassword))
                .call(successConsumer, errorConsumer)
    }

    fun getGroup(id: Long? = null, consumer: ((GroupDto) -> Unit)? = null) {
        val groupClient = groupClient()
        if (id != null ) {
            groupClient
                    .group(id)
                    .call(consumer ?: refreshGroup)
        } else {
            group?.let {
                groupClient
                    .group(it.id)
                    .call(consumer ?: refreshGroup)
            }
        }
    }

    fun requestPasswordReset(email: String) {
        userClient().requestPasswordReset(RegistrationDto(email = email)).call()
    }

    fun createGroup(friends: List<FriendDto>, consumer: (GroupDto) -> Unit) {
        groupClient()
                .create(CreateGroupDto(getUserId(), mapToGroupMembers(friends)))
                .call(consumer)
    }

    fun respondToInvite(groupId: Long, consumer: (GroupDto?) -> Unit, isAccept: Boolean) {
        groupClient()
                .respondToInvite(groupId, getUserId(), isAccept)
                .call(consumer)
    }

    fun removeGroup(consumer: () -> Unit) {
        group?.let { groupClient().remove(it.id).call(consumer, errorConsumer) }
    }

    fun removeGroupMember(userId: Long, errorConsumer: ((Throwable) -> Unit)? = null) {
        group?.let {
            groupClient()
                    .removeMember(it.id, userId)
                    .call(refreshGroup, errorConsumer ?: this.errorConsumer)
        }
    }

    private fun updateGroup(dto: GroupDto) {
        group?.let { groupClient().update(it.id, dto).call(refreshGroup) }
    }

    fun promoteToAdmin(dto: GroupMemberDto) {
        group?.let { updateGroup(it.copy(adminId = dto.id)) }
    }

    fun addGroupMembers(friendsToAdd: List<FriendDto>) {
        if (friendsToAdd.isEmpty()) {
            refreshGroup(group)
        } else {
            group?.let { updateGroupMembers((it.members + mapToGroupMembers(friendsToAdd))) }
        }
    }

    fun getUnGroupedFriendNames(): Array<String> {
        return friends.filter(FriendDto::canJoinGroup)
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

    fun updateGroupSettings(groupSettingsDto: GroupSettingsDto) {
        group?.let {
            groupClient().updateSettings(it.id, groupSettingsDto).call()
        }
    }

    fun refreshGroupDetails(dto: GroupDto?) {
        if (isGroupMember(dto)) updateGroupDetails(dto!!) else setNoGroup()
    }

    private fun getToken(): String = getSharedPref()?.getString(tokenRef, null) ?: ""
    private fun getUserId(): Long = getSharedPref()?.getLong(userIdRef, -1) ?: -1

    private fun userClient(): UserClient = createService(UserClient::class, getToken())
    private fun groupClient(): GroupClient = createService(GroupClient::class, getToken())

    private fun isGroupMember(dto: GroupDto?) =
            dto != null && getUserId() in dto.members.map { it.id }

    private fun updateGroupDetails(dto: GroupDto) {
        group = dto
        val currentFragment = currentActivity.get()?.supportFragmentManager?.findFragmentById(R.id.content_main)
        when (currentFragment) {
            is GroupFragment -> currentFragment.updateGroup(dto)
            is LocationFragment -> currentFragment.updateView(dto)
        }
        (currentActivity.get() as? MainActivity)?.setAdminVisibility(getUserId() == group?.adminId)
        getFriends()
    }

    private fun setNoGroup() {
        (currentActivity.get() as? MainActivity)?.apply { setNoGroup() }
    }

    private fun updateGroupMembers(groupMembers: List<GroupMemberDto>) {
        group?.let { updateGroup(it.copy(members = groupMembers)) }
    }

    private fun mapToGroupMembers(friends: List<FriendDto>): List<GroupMemberDto> {
        return friends.map { GroupMemberDto.fromFriendDto(it) }
    }

    fun <T> Observable<T>.call(successConsumer: (T) -> Unit = {}) {
        call(successConsumer, errorConsumer)
    }

}
