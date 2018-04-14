package me.cooper.rick.crowdcontrollerclient.activity

import android.app.Activity
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.LENGTH_INDEFINITE
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.fasterxml.jackson.databind.JsonMappingException
import com.google.android.gms.common.api.ResolvableApiException
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_add_friend.view.*
import kotlinx.android.synthetic.main.content_main.*
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupMemberDto
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.acceptGroupInvite
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.addFriend
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.addGroupMembers
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.createGroup
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.errorConsumer
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.getFriends
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.getGroup
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.getUnGroupedFriendNames
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.group
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.promoteToAdmin
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.refreshGroupDetails
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.removeFriend
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.removeGroup
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.removeGroupMember
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.selectFriends
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.updateFriendship
import me.cooper.rick.crowdcontrollerclient.api.service.UpdateService
import me.cooper.rick.crowdcontrollerclient.api.util.buildConnectionExceptionResponse
import me.cooper.rick.crowdcontrollerclient.constant.VibratePattern
import me.cooper.rick.crowdcontrollerclient.fragment.AbstractAppFragment
import me.cooper.rick.crowdcontrollerclient.fragment.LocationFragment
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment.OnFriendFragmentInteractionListener
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment.OnGroupFragmentInteractionListener
import me.cooper.rick.crowdcontrollerclient.fragment.settings.GroupSettingsFragment
import me.cooper.rick.crowdcontrollerclient.fragment.settings.SettingsFragment
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnFriendFragmentInteractionListener,
        OnGroupFragmentInteractionListener,
        UpdateService.UpdateServiceListener,
        LocationFragment.OnFragmentInteractionListener,
        FragmentManager.OnBackStackChangedListener {

    private var mBound = false

    private var updateService: UpdateService? = null

    private val swipeRefreshLayouts = mutableSetOf<SwipeRefreshLayout>()

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
        errorConsumer = { handleApiException(it) }
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
            isRootFragment() -> { /* Do nothing if root */
            }
            else -> super.onBackPressed()
        }
    }

    private fun isRootFragment(): Boolean {
        return supportFragmentManager.findFragmentById(R.id.content_main) is FriendFragment
    }

    override fun onResume() {
        super.onResume()
        getFriends()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) updateService?.subscribeToLocationUpdates()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        playClick()
        when (item.itemId) {
            R.id.nav_add_friend -> showAddFriendDialog()
            R.id.nav_create_group -> showFriendSelectorDialog(getUnGroupedFriendNames(),
                    { friends -> createGroup(friends, { createGroup(it) }) })
            R.id.nav_group -> startTask {
                getGroup(null, {
                    refreshGroupDetails(it)
                    addFragmentOnTop(GroupFragment())
                })
            }
            R.id.nav_location -> startTask { addFragmentOnTop(LocationFragment()) }
            R.id.nav_group_leave -> startTask { removeGroupMember(getUserId(), { setNoGroup() }) }
            R.id.nav_group_settings -> addFragmentOnTop(GroupSettingsFragment())
            R.id.nav_group_close -> removeGroup({ setNoGroup() })
            R.id.nav_settings -> addFragmentOnTop(SettingsFragment())
            R.id.nav_sign_out -> startTask {
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

    /* FRAGMENT LISTENER */

    override fun onSwipe(swipeView: SwipeRefreshLayout) {
        when (swipeView.id) {
            R.id.group_swipe_container -> getGroup()
            R.id.friend_swipe_container -> getFriends()
        }
    }

    override fun pushView(swipeView: SwipeRefreshLayout) {
        swipeRefreshLayouts += swipeView
    }

    override fun popView(swipeView: SwipeRefreshLayout) {
        swipeRefreshLayouts -= swipeView
    }

    /* FRIEND FRAGMENT LISTENER */

    override fun onListItemContextMenuSelection(dto: FriendDto, menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_remove_friend -> {
                showConfirmDialog(dto.username, R.string.txt_confirm_remove_friend,
                        removeFriendListener(dto))
            }
            R.id.action_add_to_group -> if (dto.isGrouped()) {
                showGroupedPopup(dto)
            } else {
                startTask {
                    if (group?.adminId == getUserId()) {
                        addGroupMembers(listOf(dto))
                    } else {
                        createGroup(listOf(dto), { createGroup(it) })
                    }
                }
            }
        }
    }

    override fun onListItemFriendUpdate(dto: FriendDto) {
        val (stringId, listener) = when (dto.status) {
            FriendDto.Status.CONFIRMED -> Pair(R.string.txt_confirm_accept_friend,
                    dialogOnClickListener { updateFriendship(dto) })
            FriendDto.Status.INACTIVE -> Pair(R.string.txt_confirm_remove_friend,
                    removeFriendListener(dto))
            else -> throw NotImplementedError("this menu option doesn't exist")
        }
        showConfirmDialog(dto.username, stringId, listener)
    }

    /* GROUP FRAGMENT LISTENER */

    override fun onListItemContextMenuSelection(groupMember: GroupMemberDto, menuItem: MenuItem) {
        val (stringId, listener) = when (menuItem.itemId) {
            R.id.action_remove_member -> Pair(R.string.txt_confirm_remove_group_member,
                    dialogOnClickListener({ removeGroupMember(groupMember.id) }))
            R.id.action_promote_admin -> Pair(R.string.txt_confirm_promote_group_member,
                    dialogOnClickListener({ promoteToAdmin(groupMember) }))
            else -> throw NotImplementedError("this menu option doesn't exist")
        }
        showConfirmDialog(groupMember.username, stringId, listener)
    }

    override fun onInviteCancellation(groupMember: GroupMemberDto) {
        startTask { removeGroupMember(groupMember.id) }
    }

    override fun userId(): Long = getUserId()

    /* LOCATION FRAGMENT LISTENER */


    /* SERVICE LISTENER */

    override fun updateNavMenu(isGrouped: Boolean) {
        nav_view.menu.setGroupVisible(R.id.nav_group_grouped, isGrouped)
        nav_view.menu.setGroupVisible(R.id.nav_group_ungrouped, !isGrouped)
    }

    override fun handleApiException(e: Throwable) {
        when (e) {
            is ResolvableApiException -> e.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
            is JsonMappingException -> Log.d("EXCEPTION", e.message, e)
            is IOException -> handleResponse(buildConnectionExceptionResponse<Any>(e), {})
            is HttpException -> handleResponse(e.response(), {}, { dismissDialogs() })
            else -> throw e
        }
        dismissProgressBar()
    }

    override fun requestPermissions() = requestLocationPermissions()

    fun setAdminVisibility(isAdmin: Boolean) {
        nav_view.menu.setGroupVisible(R.id.nav_group_group_admin, isAdmin)
    }

    override fun notifyUserOfGroupInvite(groupId: Long, groupAdmin: String) {
        val notification = Snackbar.make(
                content,
                getString(R.string.txt_pending_grp_invite, groupAdmin),
                LENGTH_INDEFINITE
        ).apply {
            setAction(R.string.action_accept_grp_invite, { _ ->
                acceptGroupInvite(groupId, {
                    updateService?.hasPendingGroupInvite = false
                    createGroup(it)
                })
            })
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}

                override fun onViewDetachedFromWindow(v: View) {
                    updateService?.hasPendingGroupInvite = false
                }
            })
        }
        notification.show()
        playSound(SOUND_DING)
        vibrate(VibratePattern.NOTIFICATION)
    }

    fun dismissAfterTask() {
        dismissProgressBar()
        dismissDialogs()
    }

    /* PRIVATE STUFF */

    private fun initFragments() {
        supportFragmentManager.addOnBackStackChangedListener(this)

        supportFragmentManager.beginTransaction()
                .replace(R.id.content_main, FriendFragment())
                .addToBackStack(BACK_STACK_ROOT_TAG)
                .commit()
    }

    private fun setFragmentProperties(fragment: AbstractAppFragment) {
        supportActionBar?.title = fragment.getTitle()
        when (fragment) {
            is FriendFragment -> {
                fab.setOnClickListener { showAddFriendDialog() }
                fab.visibility = View.VISIBLE
                nav_view.menu.setGroupVisible(R.id.nav_group_friend, true)
            }
            is GroupFragment -> {
                fab.setOnClickListener {
                    showFriendSelectorDialog(getUnGroupedFriendNames(), { addGroupMembers(it) })
                }
                fab.visibility = if (getUserId() == group?.adminId) View.VISIBLE else View.GONE
                nav_view.menu.setGroupVisible(R.id.nav_group_friend, false)
            }
            is LocationFragment -> {
                fragment.updateView(group)
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

    private fun showAddFriendDialog() {
        addDialog(AlertDialog.Builder(this)
                .setTitle(R.string.header_add_friend)
                .setView(layoutInflater.inflate(R.layout.content_add_friend, content_main,
                        false).apply {
                    btn_add_friend.setOnClickListener {
                        startTask { addFriend(actv_user_detail.text.toString()) }
                    }
                    btn_cancel_add_friend.setOnClickListener { dismissDialogs() }
                })
                .show())
    }

    private fun showFriendSelectorDialog(unGroupedNames: Array<String>,
                                         consumer: (List<FriendDto>) -> Unit) {
        val selectedIds = mutableListOf<FriendDto>()
        addDialog(AlertDialog.Builder(this)
                .setTitle(title)
                .setMultiChoiceItems(unGroupedNames, null,
                        selectFriends(unGroupedNames, selectedIds))
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    startTask { consumer(selectedIds) }
                })
                .show())
    }

    private fun showGroupedPopup(friend: FriendDto) {
        val isInCurrentGroup = group?.members?.let { friend.id in it.map { it.id } } ?: false
        showDismissiblePopup("Grouped", "${friend.username} is already in " +
                "${if (isInCurrentGroup) "your" else "a"} group!")
    }

    private fun showConfirmDialog(name: String, stringId: Int,
                                  onOkListener: DialogInterface.OnClickListener) {
        addDialog(AlertDialog.Builder(this)
                .setTitle(getString(R.string.header_confirm))
                .setMessage(getString(stringId, name))
                .setPositiveButton(getString(android.R.string.ok), onOkListener)
                .setNegativeButton(getString(android.R.string.cancel), { _, _ -> })
                .show())
    }

    private fun createGroup(it: GroupDto) {
        refreshGroupDetails(it)
        addFragmentOnTop(GroupFragment())
        nav_view.menu.setGroupVisible(R.id.nav_group_grouped, true)
    }

    fun setNoGroup() {
        group = null
        supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, 0)
        getFriends()
        setAdminVisibility(false)
    }

    private fun dismissProgressBar() {
        swipeRefreshLayouts.forEach { it.isRefreshing = false }
        showProgress(false, content_main, progress)
    }

    companion object {
        const val BACK_STACK_ROOT_TAG = "root"
        const val REQUEST_CHECK_SETTINGS = 1
    }

    /* Convenience */

    private fun startTask(func: () -> Unit) {
        showProgress(true, content_main, progress)
        func()
    }

    private fun removeFriendListener(dto: FriendDto) = dialogOnClickListener { removeFriend(dto) }

    private fun dialogOnClickListener(func: () -> Unit): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ -> playClick(); startTask(func) }
    }

}
