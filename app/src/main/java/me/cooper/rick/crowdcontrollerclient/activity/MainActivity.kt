package me.cooper.rick.crowdcontrollerclient.activity

import android.content.DialogInterface
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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_add_friend.view.*
import kotlinx.android.synthetic.main.content_main.*
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment.OnFriendFragmentInteractionListener
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment.OnGroupFragmentInteractionListener
import me.cooper.rick.crowdcontrollerclient.api.task.friends.AddFriend
import me.cooper.rick.crowdcontrollerclient.api.task.friends.GetFriends
import me.cooper.rick.crowdcontrollerclient.api.task.friends.RemoveFriend
import me.cooper.rick.crowdcontrollerclient.api.task.friends.UpdateFriendship
import me.cooper.rick.crowdcontrollerclient.api.task.group.CreateGroup
import me.cooper.rick.crowdcontrollerclient.api.task.group.GetGroup
import me.cooper.rick.crowdcontrollerclient.auth.DestroyTokenTask
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment

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

    val friends = mutableListOf<FriendDto>()
    val group = mutableListOf<UserDto>()

    private var groupId: Long = -1L // TODO - should be persisted in user?

    private lateinit var friendFragment: FriendFragment
    private lateinit var groupFragment: GroupFragment
    private lateinit var swipeView: SwipeRefreshLayout

    private val refreshFriends: (List<FriendDto>) -> Unit = {
        refresh()
        friends.clear()
        friends.addAll(it)
        swipeView.apply { isRefreshing = false }
        friendFragment.updateView()
    }

    private val createGroup: (GroupDto) -> Unit = {
        refreshGroup(it)
        addFragmentOnTop(groupFragment)
    }

    private val refreshGroup: (GroupDto) -> Unit = {
        refresh()
        groupId = it.id
        group.clear()
        group.addAll(it.members)
        swipeView.apply { isRefreshing = false }
        groupFragment.updateView()
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

    private fun addFragmentOnTop(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.content_main, fragment)
                .addToBackStack(null)
                .commit()
    }

    private fun addFriend() {
        addDialog(AlertDialog.Builder(this)
                .setTitle(R.string.header_add_friend)
                .setView(layoutInflater.inflate(R.layout.content_add_friend, content_main,
                        false)
                        .apply {
                            btn_add_friend.setOnClickListener {
                                val dto = FriendDto(username = actv_user_detail.text.toString())
                                addTask(AddFriend(dto, refreshFriends).apply { execute() })
                            }
                            btn_cancel_add_friend.setOnClickListener { refresh() }
                        })
                .show())
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
            if (groupId != -1L) addTask(GetGroup(groupId, refreshGroup).apply { execute() })
        } else addTask(GetFriends(refreshFriends).apply { execute() })
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
        when (item.itemId) {
            R.id.navCreateGroup -> showFriendSelectorPopup(getUnGroupedFriendNames())
            R.id.navNewFriend -> addFriend()
            R.id.navSettings -> {
            }
            R.id.navSignOut -> DestroyTokenTask({ startActivity(LoginActivity::class) }).execute()
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun getUnGroupedFriendNames(): Array<String> {
        return friends
                .filter(FriendDto::canJoinGroup)
                .map { it.username }
                .toTypedArray()
    }

    private fun showFriendSelectorPopup(unGroupedNames: Array<String>) {
        val selectedIds = mutableListOf<Long>()
        addDialog(AlertDialog.Builder(this)
                .setTitle(title)
                .setMultiChoiceItems(unGroupedNames, null,
                        selectFriends(unGroupedNames, selectedIds))
                .setPositiveButton(android.R.string.ok, { _, _ -> createGroup(selectedIds) })
                .show())
    }

    private fun selectFriends(unGroupedNames: Array<String>,
                              selectedIds: MutableList<Long>): (DialogInterface, Int, Boolean) -> Unit {
        return { _, i, checked ->
            val id = friends.find { it.username == unGroupedNames[i] }!!.id
            selectedIds.apply { if (checked) add(id) else remove(id) }
        }
    }

    override fun onListItemContextMenuSelection(friend: FriendDto, menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_remove_friend -> {
                showUpdateFriendDialog(friend, R.string.txt_confirm_remove_friend,
                        removeFriend(friend.id))
            }
            R.id.action_add_to_group -> {
                if (friend.isGrouped()) showGroupedPopup(friend) else createGroup(listOf(friend.id))
            }
            else -> throw NotImplementedError("Not Implemented!!")
        }
    }

    private fun showGroupedPopup(friend: FriendDto) {
        showDismissiblePopup("Grouped", "${friend.username} is already in a group!")
    }

    override fun onListItemFriendUpdate(friend: FriendDto) {
        when (friend.status) {
            FriendDto.Status.CONFIRMED -> {
                showUpdateFriendDialog(friend, R.string.txt_confirm_accept_friend,
                        updateFriendship(friend))
            }
            FriendDto.Status.INACTIVE -> {
                showUpdateFriendDialog(friend, R.string.txt_confirm_remove_friend,
                        removeFriend(friend.id))
            }
            else -> {
            } // No action required
        }
    }

    override fun onListItemContextMenuSelection(friend: UserDto, menuItem: MenuItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onListFragmentInteraction(item: UserDto) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun showUpdateFriendDialog(item: FriendDto, stringId: Int,
                                       onOkListener: DialogInterface.OnClickListener) {
        addDialog(AlertDialog.Builder(this)
                .setTitle(getString(R.string.header_confirm))
                .setMessage(getString(stringId, item.username))
                .setPositiveButton(getString(android.R.string.ok), onOkListener)
                .setNegativeButton(getString(android.R.string.cancel), { _, _ -> })
                .show())
    }

    private fun createGroup(friendIds: List<Long>) {
        addTask(CreateGroup(friendIds, createGroup).apply { execute() })
    }

    private fun removeFriend(id: Long): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ ->
            addTask(RemoveFriend(id, refreshFriends).apply { execute() })
        }
    }

    private fun updateFriendship(friend: FriendDto): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ ->
            addTask(UpdateFriendship(friend, refreshFriends).apply { execute() })
        }
    }

    companion object {
        const val BACK_STACK_ROOT_TAG = "root"
    }

}
