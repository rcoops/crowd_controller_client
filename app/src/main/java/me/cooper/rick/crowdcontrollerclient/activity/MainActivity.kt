package me.cooper.rick.crowdcontrollerclient.activity

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_add_friend.view.*
import me.cooper.rick.crowdcontrollerapi.dto.CreateGroupDto
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment.OnFriendFragmentInteractionListener
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment.OnGroupFragmentInteractionListener
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.api.ClientTask
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import me.cooper.rick.crowdcontrollerclient.auth.DestroyTokenTask
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment
import retrofit2.Call

class MainActivity : AppActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnFriendFragmentInteractionListener,
        OnGroupFragmentInteractionListener {
    /*
    https://medium.com/@bherbst/managing-the-fragment-back-stack-373e87e4ff62
    Fragment transactions can involve two different types of tags. The one that most Android
    developers are familiar with is the Fragment tag, which you can use to find a specific Fragment
    in your FragmentManager later via findFragmentByTag(). This is useful for finding a Fragment
    when your application is in a particular state, but keep in mind that the Fragment needs to be
    added to your FragmentManager. If you have removed() or replaced() a Fragment and haven’t added
    it to the backstack, you won’t be able to find it.
     */

    val friends: MutableList<FriendDto> = mutableListOf()
    val group: MutableList<UserDto> = mutableListOf()
    private var groupId: Long = -1

    private lateinit var friendFragment: FriendFragment
    private lateinit var groupFragment: GroupFragment
    private lateinit var addFriendDialogView: View
    private lateinit var addFriendDialog: AlertDialog
    lateinit var swipeView: SwipeRefreshLayout

    private var getFriendsTask: GetFriendsTask? = null
    private var addFriendTask: AddFriendTask? = null
    private var removeFriendTask: RemoveFriendTask? = null
    private var destroyTokenTask: DestroyTokenTask? = null
    private var getGroupTask: GetGroupTask? = null
    private var createGroupTask: CreateGroupTask? = null
    private var friendRequestResponseTask: FriendRequestResponseTask? = null

    private val tasks = listOf(getFriendsTask, addFriendTask, removeFriendTask,
            destroyTokenTask, friendRequestResponseTask, getGroupTask, createGroupTask)

    private val refreshFriends: (List<FriendDto>) -> Unit = {
        refresh()
        friends.clear()
        friends.addAll(it)
        swipeView.apply { isRefreshing = false }
        friendFragment.updateView()
    }

    private val refreshGroup: (GroupDto) -> Unit = {
        refresh()
        groupId = it.id
        group.clear()
        group.addAll(it.members)
        swipeView.apply { isRefreshing = false }
        groupFragment.updateView()
    }

    private fun refresh() {
        destroyTasks()
        dismissDialogs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        drawer_layout.addDrawerListener(
                ActionBarDrawerToggle(this, drawer_layout, toolbar,
                        R.string.navigation_drawer_open, R.string.navigation_drawer_close)
                        .apply { syncState() }
        )

        addFriendDialogView = layoutInflater.inflate(R.layout.content_add_friend, content)
        addFriendDialogView.btn_add_friend.setOnClickListener {
            addFriendTask = AddFriendTask(addFriendDialogView.actv_user_detail.text.toString())
                    .apply { execute() }
        }
        addFriendDialog = AlertDialog.Builder(this)
                .setTitle(R.string.header_add_friend)
                .setView(addFriendDialogView).create()

        addFriendDialogView.btn_cancel_add_friend.setOnClickListener { dismissDialogs() }

        nav_view.setNavigationItemSelectedListener(this)

        friendFragment = FriendFragment()
        groupFragment = GroupFragment()
        onTabSelected()
        fab.setOnClickListener { addFriend() }
    }

    private fun onTabSelected() {
        supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, POP_BACK_STACK_INCLUSIVE)

        supportFragmentManager.beginTransaction()
                .replace(R.id.content_main, friendFragment)
                .addToBackStack(BACK_STACK_ROOT_TAG)
                .commit()
    }

    /**
     * Add a fragment on top of the current tab
     */
    private fun addFragmentOnTop(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.content_main, fragment)
                .addToBackStack(null)
                .commit()
    }

    private fun dismissDialogs() {
        dismissDialog(addFriendDialog)
    }

    private fun dismissDialog(alertDialog: AlertDialog) {
        if (alertDialog.isShowing) alertDialog.dismiss()
    }

    private fun addFriend() {
        addFriendDialogView.actv_user_detail.text.clear()
        addFriendDialog.show()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        onSwipe(null)
    }

    override fun onSwipe(swipeView: SwipeRefreshLayout?) {
        if (R.id.group_swipe_container == swipeView?.id) {
            getGroupTask = GetGroupTask(groupId).apply { execute() }
        } else getFriendsTask = GetFriendsTask().apply { execute() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.friend, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setFragmentProperties(swipeView: SwipeRefreshLayout, title: String) {
        this.swipeView = swipeView
        supportActionBar?.title = title
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.navCreateGroup -> {
                val unGroupedFriendNames = friends
                        .filter(FriendDto::canJoinGroup)
                        .map { it.username }
                        .toTypedArray()
                val selectedFriends = mutableListOf<Long>()
                AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMultiChoiceItems(unGroupedFriendNames, null, { _, index, isChecked ->
                            val friendId = friends
                                    .find { it.username == unGroupedFriendNames[index] }!!
                                    .id
                            if (isChecked) selectedFriends += friendId
                            else selectedFriends -= friendId
                        })
                        .setPositiveButton(android.R.string.ok, { _, _ ->
                            createGroupTask = CreateGroupTask(selectedFriends).apply { execute() }
                            addFragmentOnTop(GroupFragment())
                        })
                        .show()

                supportFragmentManager.fragments
            }
            R.id.navNewFriend -> addFriend()
            R.id.navSettings -> {}
            R.id.navSignOut -> DestroyTokenTask({ startActivity(LoginActivity::class) }).execute()
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }


    override fun onListItemContextMenuSelection(friend: FriendDto, menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_remove_friend -> showRemoveFriendDialog(friend)
            R.id.action_add_to_group -> {
                if (friend.isGrouped()) {
                    showDismissiblePopup("Grouped", "${friend.username} is already in a group!")
                } else {
                    TODO("create group")
                    addFragmentOnTop(GroupFragment())
                }
            }
            else -> throw NotImplementedError("Not Implemented!!")
        }
    }

    override fun onListItemFriendInviteResponse(friend: FriendDto, isAccepting: Boolean) {
        friendRequestResponseTask = FriendRequestResponseTask(friend.id, isAccepting)
                .apply { execute() }
    }

    override fun onListItemContextMenuSelection(friend: UserDto, menuItem: MenuItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onListFragmentInteraction(item: UserDto) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun destroyTasks() = tasks.forEach { it?.cancel(true) }

    private fun showRemoveFriendDialog(item: FriendDto) {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.header_confirm))
                .setMessage(getString(R.string.txt_confirm_remove_friend, item.username))
                .setPositiveButton(getString(android.R.string.ok), { _, _ -> removeFriend(item.id) })
                .setNegativeButton(getString(android.R.string.cancel), { _, _ -> })
                .show()
    }

    private fun removeFriend(id: Long) {
        removeFriendTask = RemoveFriendTask(id).apply { execute() }
    }

    inner class GetFriendsTask internal constructor()
        : ClientTask<UserClient, List<FriendDto>>(refreshFriends, UserClient::class) {

        override fun buildCall(client: UserClient, id: Long): Call<List<FriendDto>> {
            return client.friends(id)
        }

    }

    inner class AddFriendTask internal constructor(private val friendIdentifier: String)
        : ClientTask<UserClient, List<FriendDto>>(refreshFriends, UserClient::class) {

        override fun buildCall(client: UserClient, id: Long): Call<List<FriendDto>> {
            return client.addFriend(id, friendIdentifier)
        }

    }

    inner class RemoveFriendTask internal constructor(private val friendId: Long)
        : ClientTask<UserClient, List<FriendDto>>(refreshFriends, UserClient::class) {

        override fun buildCall(client: UserClient, id: Long): Call<List<FriendDto>> {
            return client.removeFriend(id, friendId)
        }

    }

    inner class FriendRequestResponseTask internal constructor(private val friendId: Long,
                                                               private val response: Boolean)
        : ClientTask<UserClient, List<FriendDto>>(refreshFriends, UserClient::class) {

        override fun buildCall(client: UserClient, id: Long): Call<List<FriendDto>> {
            return client.respondToFriendRequest(id, friendId, response)
        }

    }

    inner class CreateGroupTask internal constructor(private val friendIds: List<Long>)
        : ClientTask<GroupClient, GroupDto>(refreshGroup, GroupClient::class) {

        override fun buildCall(client: GroupClient, id: Long): Call<GroupDto> {
            return client.create(CreateGroupDto(id, friendIds))
        }

    }

    inner class GetGroupTask internal constructor(private val groupId: Long)
        : ClientTask<GroupClient, GroupDto>(refreshGroup, GroupClient::class) {

        override fun buildCall(client: GroupClient, id: Long): Call<GroupDto> {
            return client.group(groupId)
        }

    }

    companion object {
        const val BACK_STACK_ROOT_TAG = "root"
    }

}
