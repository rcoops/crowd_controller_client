package me.cooper.rick.crowdcontrollerclient.activity

import android.app.Activity
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import android.view.View
import com.google.android.gms.common.api.ResolvableApiException
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_add_friend.view.*
import kotlinx.android.synthetic.main.content_main.*
import me.cooper.rick.crowdcontrollerapi.dto.CreateGroupDto
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.service.UpdateService
import me.cooper.rick.crowdcontrollerclient.api.util.buildConnectionExceptionResponse
import me.cooper.rick.crowdcontrollerclient.fragment.AbstractAppFragment
import me.cooper.rick.crowdcontrollerclient.fragment.LocationFragment
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment.OnFriendFragmentInteractionListener
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment.OnGroupFragmentInteractionListener
import me.cooper.rick.crowdcontrollerclient.util.call
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnFriendFragmentInteractionListener,
        OnGroupFragmentInteractionListener,
        UpdateService.UpdateServiceListener,
        LocationFragment.OnFragmentInteractionListener,
        FragmentManager.OnBackStackChangedListener {

    val friends = mutableListOf<FriendDto>()
    val groupMembers = mutableListOf<UserDto>()
    private var group: GroupDto? = null

    private var mBound = false

    private var updateService: UpdateService? = null

    private lateinit var friendFragment: FriendFragment
    private lateinit var groupFragment: GroupFragment
    private lateinit var locationFragment: LocationFragment
    private var swipeView: SwipeRefreshLayout? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
            updateService = (iBinder as UpdateService.LocalBinder).service
            updateService?.registerListener(this@MainActivity)
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            updateService = null
            mBound = false
        }
    }

    private val refreshFriends: (List<FriendDto>) -> Unit = { updateFriends(it) }

    private val createGroup: (GroupDto) -> Unit = {
        refreshGroupDetails(it)
        addFragmentOnTop(groupFragment)
    }

    private val refreshGroup: (GroupDto) -> Unit = { refreshGroupDetails(it) }

    /* ACTIVITY OVERRIDES */

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

        initFragments()
        showProgress(true, content_main, progress)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, UpdateService::class.java), serviceConnection,
                BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> {
                drawer_layout.closeDrawer(GravityCompat.START)
            }
            isRootFragment() -> {}
            else -> super.onBackPressed()
        }
    }

    private fun isRootFragment(): Boolean {
        return supportFragmentManager.findFragmentById(R.id.content_main) is FriendFragment
    }

    override fun onResume() {
        super.onResume()
        onSwipe(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) updateService?.startTracking()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_add_friend -> addFriend()
            R.id.nav_create_group -> showFriendSelectorPopup(getUnGroupedFriendNames(),
                    { createGroup(it) })
            R.id.nav_group -> {
                addFragmentOnTop(groupFragment)
            }
            R.id.nav_location -> {
                showProgress(true, content_main, progress)
                addFragmentOnTop(locationFragment)
            }
            R.id.nav_group_leave -> {
            }
            R.id.nav_clustering_toggle -> {

            }
            R.id.nav_group_close -> {

            }
            R.id.nav_settings -> {
            }
            R.id.nav_sign_out -> {
                editAppDetails { clear() }
                startActivity(LoginActivity::class)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    /* BACK STACK LISTENER */

    override fun onBackStackChanged() {
        val fragment = supportFragmentManager
                .findFragmentById(R.id.content_main) as? AbstractAppFragment ?: return
        setFragmentProperties(fragment)
    }

    /* SERVICE BINDING */

    override fun bindService(service: Intent?, conn: ServiceConnection?, flags: Int): Boolean {
        mBound = super.bindService(service, conn, flags)
        return mBound
    }

    override fun unbindService(conn: ServiceConnection?) {
        if (mBound) {
            updateService?.unregisterListener(this)
            super.unbindService(conn)
            mBound = false
        }
    }

    /* API CALLS */

    private fun getFriends() {
        showProgress(true, content_main, progress)
        userClient!!.findFriends(getUserId()).call(refreshFriends)
    }

    private fun getGroup(groupId: Long) {
        showProgress(true, content_main, progress)
        groupClient!!.find(groupId).call(refreshGroup)
    }

    private fun addFriend(dto: FriendDto) {
        showProgress(true, content_main, progress)
        userClient!!.addFriend(getUserId(), dto).call(refreshFriends)
    }

    private fun removeFriend(dto: FriendDto) {
        showProgress(true, content_main, progress)
        userClient!!.removeFriend(getUserId(), dto.id).call(refreshFriends)
    }

    private fun updateFriendship(dto: FriendDto) {
        showProgress(true, content_main, progress)
        userClient!!.updateFriendship(getUserId(), dto.id, dto).call(refreshFriends)
    }

    private fun createGroup(friendIds: List<Long>) {
        showProgress(true, content_main, progress)
        groupClient!!.create(CreateGroupDto(getUserId(), friendIds)).call(createGroup)
    }

    private fun updateGroup(group: GroupDto) {
        groupClient?.update(group.id, group)?.call(refreshGroup)
    }

    /* FRAGMENT LISTENER */

    override fun onSwipe(swipeView: SwipeRefreshLayout?) {
        when (swipeView?.id) {
            R.id.group_swipe_container -> group?.let { getGroup(it.id) }
            R.id.friend_swipe_container -> getFriends()
            else -> {
            }
        }
    }

    /* FRIEND FRAGMENT LISTENER */

    override fun onListItemContextMenuSelection(friend: FriendDto, menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_remove_friend -> {
                showUpdateFriendDialog(friend, R.string.txt_confirm_remove_friend,
                        removeFriendListener(friend))
            }
            R.id.action_add_to_group -> {
                if (friend.isGrouped()) showGroupedPopup(friend) else createGroup(listOf(friend.id))
            }
            else -> throw NotImplementedError("Not Implemented!!")
        }
    }

    override fun onListItemFriendUpdate(dto: FriendDto) {
        when (dto.status) {
            FriendDto.Status.CONFIRMED -> {
                showUpdateFriendDialog(dto, R.string.txt_confirm_accept_friend,
                        DialogInterface.OnClickListener { _, _ -> updateFriendship(dto) })
            }
            FriendDto.Status.INACTIVE -> {
                showUpdateFriendDialog(dto, R.string.txt_confirm_remove_friend,
                        removeFriendListener(dto))
            }
            else -> {
            } // No action required
        }
    }

    /* GROUP FRAGMENT LISTENER */

    override fun onListItemContextMenuSelection(groupMember: UserDto, menuItem: MenuItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onListFragmentInteraction(groupMember: UserDto) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /* LOCATION FRAGMENT LISTENER */


    /* SERVICE LISTENER */

    override fun onUpdate(userDto: UserDto) {
        editAppDetails { putLong(getString(R.string.user_id), userDto.id) }
        updateFriends(userDto.friends)
        val grouped = userDto.group != null
        nav_view.menu.setGroupVisible(R.id.nav_group_grouped, grouped)
        nav_view.menu.setGroupVisible(R.id.nav_group_ungrouped, !grouped)
    }

    override fun onUpdate(groupDto: GroupDto) = refreshGroupDetails(groupDto)

    override fun handleApiException(e: Throwable) {
        when (e) {
            is ResolvableApiException -> e.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
            is IOException -> handleResponse(buildConnectionExceptionResponse<Any>(e), {})
            is HttpException -> handleResponse(e.response(), {}, { dismissDialogs() })
            else -> throw e
        }
        swipeView?.apply { isRefreshing = false }
    }

    override fun requestPermissions() = requestLocationPermissions()

    /* PRIVATE STUFF */

    private fun initFragments() {
        friendFragment = FriendFragment()
        groupFragment = GroupFragment()
        locationFragment = LocationFragment()
        supportFragmentManager.addOnBackStackChangedListener(this)

        supportFragmentManager.beginTransaction()
                .replace(R.id.content_main, friendFragment)
                .addToBackStack(BACK_STACK_ROOT_TAG)
                .commit()
    }

    private fun setFragmentProperties(fragment: AbstractAppFragment) {
        this.swipeView = fragment.getSwipeView()
        supportActionBar?.title = fragment.getTitle()
        when (fragment) {
            is FriendFragment -> {
                fab.setOnClickListener { addFriend() }
                fab.visibility = View.VISIBLE
                nav_view.menu.setGroupVisible(R.id.nav_group_friend, true)
            }
            is GroupFragment -> {
                fab.setOnClickListener {
                    showFriendSelectorPopup(getUnGroupedFriendNames(), { addGroupMembers(it) })
                }
                fab.visibility = View.VISIBLE
                nav_view.menu.setGroupVisible(R.id.nav_group_friend, false)
            }
            is LocationFragment -> {
                fab.setOnClickListener(null)
                fab.visibility = View.GONE
                nav_view.menu.setGroupVisible(R.id.nav_group_friend, false)
            }
        }
    }

    private fun addFragmentOnTop(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.content_main, fragment)
                .addToBackStack(null)
                .commit()

        showProgress(false, content_main, progress)
    }

    private fun addFriend() {
        addDialog(AlertDialog.Builder(this)
                .setTitle(R.string.header_add_friend)
                .setView(layoutInflater.inflate(R.layout.content_add_friend, content_main,
                        false)
                        .apply {
                            btn_add_friend.setOnClickListener {
                                addFriend(FriendDto(username = actv_user_detail.text.toString()))
                                dismissDialogs()
                            }
                            btn_cancel_add_friend.setOnClickListener { dismissDialogs() }
                        })
                .show())
    }

    private fun refreshGroupDetails(it: GroupDto) {
        group = it
        groupMembers.clear()
        groupMembers.addAll(it.members)
        groupFragment.updateView()
        locationFragment.updateView(it.locationDto)
        nav_view.menu.setGroupVisible(R.id.nav_group_group_admin, getUserId() == group?.adminId)
        swipeView?.apply { isRefreshing = false }
        showProgress(false, content_main, progress)
    }

    private fun addGroupMembers(friendIds: List<Long>) {
        val friendsToAdd = friends.filter { friendIds.contains(it.id) }
        if (friendsToAdd.isEmpty()) return // TODO popup for friend not friend
        if (group == null) return
        val newMembers = (group!!.members + friendIds.map { UserDto(id = it) })
        updateGroup(group!!.copy(members = newMembers))
    }

    private fun getUnGroupedFriendNames(): Array<String> {
        return friends
                .filter(FriendDto::canJoinGroup)
                .map { it.username }
                .toTypedArray()
    }

    private fun updateFriends(friends: List<FriendDto>) {
        this.friends.clear()
        this.friends.addAll(friends)
        friendFragment.updateView()
        showProgress(false, content_main, progress)
        swipeView?.apply { isRefreshing = false }
    }

    private fun showFriendSelectorPopup(unGroupedNames: Array<String>, consumer: (List<Long>) -> Unit) {
        val selectedIds = mutableListOf<Long>()
        addDialog(AlertDialog.Builder(this)
                .setTitle(title)
                .setMultiChoiceItems(unGroupedNames, null,
                        selectFriends(unGroupedNames, selectedIds))
                .setPositiveButton(android.R.string.ok, { _, _ -> consumer(selectedIds) })
                .show())
    }

    private fun selectFriends(unGroupedNames: Array<String>,
                              selectedIds: MutableList<Long>): (DialogInterface, Int, Boolean) -> Unit {
        return { _, i, checked ->
            val id = friends.find { it.username == unGroupedNames[i] }!!.id
            selectedIds.apply { if (checked) add(id) else remove(id) }
        }
    }

    private fun showGroupedPopup(friend: FriendDto) {
        showDismissiblePopup("Grouped", "${friend.username} is already in a group!")
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

    companion object {
        const val BACK_STACK_ROOT_TAG = "root"
        const val REQUEST_CHECK_SETTINGS = 1
    }

    /* Convenience */

    private fun removeFriendListener(dto: FriendDto): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ -> removeFriend(dto) }
    }

    private fun <T> Observable<T>.call(consumer: (T) -> Unit) {
        call(consumer, { handleApiException(it) })
    }

}
