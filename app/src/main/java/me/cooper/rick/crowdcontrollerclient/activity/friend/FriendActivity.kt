package me.cooper.rick.crowdcontrollerclient.activity.friend

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_friend.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.app_bar_friend.*
import kotlinx.android.synthetic.main.content_add_friend.view.*
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity
import me.cooper.rick.crowdcontrollerclient.activity.group.GroupActivity
import me.cooper.rick.crowdcontrollerclient.activity.login.LoginActivity
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.api.task.UserTask
import me.cooper.rick.crowdcontrollerclient.auth.DestroyTokenTask
import retrofit2.Call

class FriendActivity : AppActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        FriendFragment.OnListFragmentInteractionListener {

    val friends: MutableList<FriendDto> = mutableListOf()

    private lateinit var friendFragment: FriendFragment
    private lateinit var addFriendDialogView: View
    private lateinit var addFriendDialog: AlertDialog
    lateinit var swipeView: SwipeRefreshLayout
    private var getFriendsTask: GetFriendsTask? = null
    private var addFriendTask: AddFriendTask? = null
    private var removeFriendTask: RemoveFriendTask? = null
    private var destroyTokenTask: DestroyTokenTask? = null
    private var friendRequestResponseTask: FriendRequestResponseTask? = null
    private val friendsTasks = listOf(getFriendsTask, addFriendTask, removeFriendTask,
            destroyTokenTask, friendRequestResponseTask)

    private val refreshFriends: (List<FriendDto>) -> Unit = {
        destroyTasks()
        dismissDialogs()
        friends.clear()
        friends.addAll(it)
        swipeView.apply { isRefreshing = false }
        friendFragment.adapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend)
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

        supportFragmentManager.beginTransaction()
                .replace(R.id.friend_fragment_content, friendFragment)
                .commit()
        fab.setOnClickListener { addFriend() }
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
        getFriendsTask = GetFriendsTask().apply { execute() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.friend, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.navCreateGroup -> startActivity(GroupActivity::class)
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
                if (friend.confirmedGroup) {
                    showDismissiblePopup("Grouped", "${friend.username} is already in a group!")
                } else {
                    startActivity(GroupActivity::class, Pair("friendId", friend.id))
                }
            }//makeText(this, "${item.username} poked", LENGTH_LONG).show()
            else -> throw NotImplementedError("Not Implemented!!")
        }
    }

    override fun onListItemFriendInviteResponse(friend: FriendDto, isAccepting: Boolean) {
        friendRequestResponseTask = FriendRequestResponseTask(friend.id, isAccepting)
                .apply { execute() }
    }

    override fun destroyTasks() = friendsTasks.forEach { it?.cancel(true) }

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

    inner class GetFriendsTask internal constructor() : UserTask<List<FriendDto>>(refreshFriends) {

        override fun buildCall(userClient: UserClient, id: Long): Call<List<FriendDto>> {
            return userClient.friends(id)
        }

    }

    inner class AddFriendTask internal constructor(private val friendIdentifier: String)
        : UserTask<List<FriendDto>>(refreshFriends) {

        override fun buildCall(userClient: UserClient, id: Long): Call<List<FriendDto>> {
            return userClient.addFriend(id, friendIdentifier)
        }

    }

    inner class RemoveFriendTask internal constructor(private val friendId: Long)
        : UserTask<List<FriendDto>>(refreshFriends) {

        override fun buildCall(userClient: UserClient, id: Long): Call<List<FriendDto>> {
            return userClient.removeFriend(id, friendId)
        }

    }

    inner class FriendRequestResponseTask internal constructor(private val friendId: Long,
                                                               private val response: Boolean)
        : UserTask<List<FriendDto>>(refreshFriends) {

        override fun buildCall(userClient: UserClient, id: Long): Call<List<FriendDto>> {
            return userClient.respondToFriendRequest(id, friendId, response)
        }

    }

}