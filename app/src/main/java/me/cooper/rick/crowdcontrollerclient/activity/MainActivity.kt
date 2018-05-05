package me.cooper.rick.crowdcontrollerclient.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import com.fasterxml.jackson.databind.JsonMappingException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_add_friend.view.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_update_password.view.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupMemberDto
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.user.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.LoginActivity.Companion.MIN_PASSWORD_LENGTH
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.addFriend
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.addGroupMembers
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.createGroup
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.errorConsumer
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.geofence
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.getFriends
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.getGroup
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.getUnGroupedFriendNames
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.group
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.promoteToAdmin
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.refreshGroupDetails
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.removeFriend
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.removeGroup
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.removeGroupMember
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.respondToInvite
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.selectFriends
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.updateFriendship
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.updatePassword
import me.cooper.rick.crowdcontrollerclient.api.service.GeofenceTransitionsIntentService
import me.cooper.rick.crowdcontrollerclient.api.service.UpdateService
import me.cooper.rick.crowdcontrollerclient.api.service.receiver.ResponseReceiver
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
        FragmentManager.OnBackStackChangedListener {

    private var mBound = false

    private var updateService: UpdateService? = null

    private val swipeRefreshLayouts = mutableSetOf<SwipeRefreshLayout>()

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var receiver: ResponseReceiver

    private var inGeoFence = false

    var groupInviteNotified = false

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

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
                        R.string.navigation_drawer_open, R.string.navigation_drawer_close).apply { syncState() }
        )

        nav_view.setNavigationItemSelectedListener(this)

        initFragments()
        errorConsumer = { handleApiException(it) }
        showProgress(true, content_main, progress)
        geofencingClient = LocationServices.getGeofencingClient(this)
        registerGeofenceResponseReceiver()
    }

    private fun registerGeofenceResponseReceiver() {
        receiver = ResponseReceiver()
        registerReceiver(
                receiver,
                IntentFilter(ResponseReceiver.ACTION).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        )
    }

    override fun setHeader(userDto: UserDto) {
        val navHeader = nav_view.getHeaderView(0)
        navHeader.txt_username.text = userDto.username
        val details = "<${if (userDto.email.isBlank()) userDto.mobileNumber else userDto.email}>"
        navHeader.txt_details.text = details
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
        errorConsumer = { handleApiException(it) }
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
            R.id.nav_create_group -> confirmCreateGroup()
            R.id.nav_group -> startTask {
                getGroup(null, {
                    refreshGroupDetails(it)
                    addFragmentOnTop(GroupFragment())
                })
            }
            R.id.nav_location -> startTask {
                updateService?.ensureGeofenceExists()
                addFragmentOnTop(LocationFragment())
            }
            R.id.nav_group_leave -> startTask { removeGroupMember(getUserId(), { setNoGroup() }) }
            R.id.nav_group_settings -> addFragmentOnTop(GroupSettingsFragment())
            R.id.nav_group_close -> removeGroup({ setNoGroup() })
            R.id.nav_settings -> addFragmentOnTop(SettingsFragment())
            R.id.nav_reset_password -> updatePasswordDialog()
            R.id.nav_sign_out -> logOut()
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logOut(username: String? = null) {
        startTask {
            editAppDetails { clear() }
            updateService?.cancelLocationUpdates()
            if (username != null) startActivityAfterReset(LoginActivity::class, username)
            else startActivity(LoginActivity::class)
        }
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
            R.id.action_remove_friend -> showConfirmDialog(
                    dto.username,
                    R.string.txt_confirm_remove_friend,
                    removeFriendListener(dto)
            )
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

    /* SERVICE LISTENER */

    override fun updateNavMenu(isGrouped: Boolean, hasAccepted: Boolean) {
        nav_view.menu.setGroupVisible(R.id.nav_group_grouped, isGrouped && hasAccepted)
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

    override fun notifyUserOfGroupInvite(groupId: Long, groupAdmin: String) {
        if (!groupInviteNotified) {
            groupInviteNotified = true
            showConfirmDialog(
                    groupAdmin,
                    R.string.txt_respond_grp_invite,
                    dialogOnClickListener {
                        respondToInvite(groupId, { it?.let { createGroup(it) } }, true)
                    },
                    dialogOnClickListener { respondToInvite(groupId, { }, false) },
                    { _ -> groupInviteNotified = false },
                    R.string.hdr_respond_grp_invite,
                    R.string.action_accept_grp_invite,
                    R.string.action_refuse_grp_invite
            )
            playSound(SOUND_DING)
            vibrate(VibratePattern.NOTIFICATION)
        }
    }

    override fun notifyOfGroupExpiry(dto: APIErrorDto) {
        supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, 0)
        showDismissiblePopup(dto.error, dto.errorDescription)
    }

    override fun setGeofence(centre: LatLng, radius: Float) {
        geofence = Geofence.Builder()
                .setRequestId("my geofence")
                .setCircularRegion(
                        centre.latitude,
                        centre.longitude,
                        radius
                )
                .setExpirationDuration(NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
    }

    @SuppressLint("MissingPermission")
    override fun addGeofence() {
        geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
            addOnSuccessListener { Log.i(TAG, "geofence added") }
            addOnFailureListener { Log.w(TAG, "failed to add geofence") }
        }
    }

    fun notifyGeofenceTransition(geofenceTransitionType: Int) {
        when (geofenceTransitionType) {
            GeofenceTransitionsIntentService.UNINTERESTING_TRANSITION -> return
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                reactToGeofenceTransition(VibratePattern.GEOFENCE_EXIT, SOUND_GEOFENCE_EXIT, false)
            }
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                reactToGeofenceTransition(VibratePattern.NOTIFICATION, SOUND_GEOFENCE_ENTER, true)
            }
        }
    }

    private fun reactToGeofenceTransition(vibratePattern: VibratePattern, sound: Int, isNowInGeofence: Boolean) {
        if (inGeoFence != isNowInGeofence) {
            vibrate(vibratePattern)
            playSound(sound)
            inGeoFence = !inGeoFence
        }
    }

    @SuppressLint("MissingPermission")
    override fun removeGeofence() {
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener { Log.i(TAG, "geofence removed") }
            addOnFailureListener { Log.w(TAG, "failed to remove geofence") }
        }
    }

    override fun updateMapSelfLocation(latLng: LatLng) {
        (supportFragmentManager
                .findFragmentById(R.id.content_main) as? LocationFragment)
                ?.drawLocationMarker(latLng)
    }

    fun setAdminVisibility(isAdmin: Boolean) {
        nav_view.menu.setGroupVisible(R.id.nav_group_group_admin, isAdmin)
    }

    fun dismissAfterTask() {
        dismissProgressBar()
        dismissDialogs()
    }

    /* PRIVATE STUFF */

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(listOf(geofence))
        }.build()
    }


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

    private fun updatePasswordDialog() {
        addDialog(AlertDialog.Builder(this)
                .setTitle(R.string.hdr_reset_password)
                .setView(layoutInflater.inflate(R.layout.content_update_password, content_main,
                        false).apply {
                    btn_confirm_update_password.setOnClickListener {
                        val newPassword = actv_new_password.text.toString()
                        val confirmPassword = actv_new_password_confirm.text.toString()
                        when {
                            newPassword.length < MIN_PASSWORD_LENGTH -> {
                                txt_password_invalid.text = getString(R.string.txt_update_password_invalid)
                            }
                            newPassword != confirmPassword -> {
                                txt_password_invalid.text = getString(R.string.txt_update_password_not_matching)
                            }
                            else -> {
                                dismissDialogs()
                                startTask {
                                    updatePassword(
                                            actv_old_password.text.toString(),
                                            newPassword,
                                            { logOut(it.username) }
                                    )
                                }
                            }
                        }
                    }
                    btn_cancel_update_password.setOnClickListener { dismissDialogs() }
                })
                .show(),
                getString(R.string.hdr_reset_password)
        )
    }

    private fun showAddFriendDialog() {
        fun hideKeyboard(edit: AutoCompleteTextView) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(edit.windowToken, 0)
        }
        val view = layoutInflater.inflate(R.layout.content_add_friend, content_main,
                false).apply {
            btn_cancel_add_friend.setOnClickListener {
                hideKeyboard(actv_user_detail)
                dismissDialogs()
                dismissDialog(getString(R.string.header_add_friend))
            }
            btn_add_friend.setOnClickListener {
                val userDetail = actv_user_detail.text.toString()
                hideKeyboard(actv_user_detail)
                startTask {  addFriend(userDetail) }
            }
        }
        addDialog(AlertDialog.Builder(this)
                .setTitle(R.string.header_add_friend)
                .setView(view)
                .show(),
                getString(R.string.header_add_friend)
        )
    }

    private fun showFriendSelectorDialog(unGroupedNames: Array<String>,
                                         consumer: (List<FriendDto>) -> Unit) {
        val selectedIds = mutableListOf<FriendDto>()
        addDialog(AlertDialog.Builder(this)
                .setTitle(getString(R.string.hdr_select_friends))
                .setMultiChoiceItems(unGroupedNames, null,
                        selectFriends(unGroupedNames, selectedIds))
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    startTask { consumer(selectedIds) }
                })
                .show(),
                getString(R.string.hdr_select_friends)
        )
    }

    private fun confirmCreateGroup() {
        addDialog(AlertDialog.Builder(this)
                .setTitle(R.string.hdr_create_grp_warning)
                .setMessage(R.string.txt_create_grp_warning)
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    showFriendSelectorDialog(getUnGroupedFriendNames(),
                            { friends -> createGroup(friends, { createGroup(it) }) })
                })
                .setNegativeButton(android.R.string.cancel, { _, _ -> })
                .show(),
                getString(R.string.hdr_create_grp_warning)
        )
    }

    private fun showGroupedPopup(friend: FriendDto) {
        val isInCurrentGroup = group?.members?.let { friend.id in it.map { it.id } } ?: false
        showDismissiblePopup("Grouped", "${friend.username} is already in " +
                "${if (isInCurrentGroup) "your" else "a"} group!")
    }

    private fun showConfirmDialog(name: String, messageStringId: Int,
                                  okConsumer: (dialog: DialogInterface, which: Int) -> Unit,
                                  cancelConsumer: (dialog: DialogInterface, which: Int) -> Unit = { _, _ -> },
                                  onDismissListener: (dialog: DialogInterface) -> Unit = { _ -> },
                                  headerId: Int = R.string.header_confirm,
                                  positiveButtonTextId: Int = android.R.string.ok,
                                  negativeButtonTextId: Int = android.R.string.cancel) {
        addDialog(AlertDialog.Builder(this)
                .setTitle(getString(headerId))
                .setMessage(getString(messageStringId, name))
                .setPositiveButton(getString(positiveButtonTextId), okConsumer)
                .setNegativeButton(getString(negativeButtonTextId), cancelConsumer)
                .setOnDismissListener(onDismissListener)
                .show(),
                getString(headerId)
        )
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
        private const val TAG = "MAIN"
    }

    /* Convenience */

    private fun startTask(func: () -> Unit) {
        showProgress(true, content_main, progress)
        func()
    }

    private fun removeFriendListener(dto: FriendDto) = dialogOnClickListener { removeFriend(dto) }

    private fun dialogOnClickListener(func: () -> Unit): (dialog: DialogInterface, which: Int) -> Unit {
        return { _, _ -> playClick(); startTask(func) }
    }

}
