package me.cooper.rick.crowdcontrollerclient.activity.friend

import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
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
import me.cooper.rick.crowdcontrollerclient.api.util.handleConnectionException
import me.cooper.rick.crowdcontrollerclient.auth.DestroyTokenTask
import me.cooper.rick.crowdcontrollerclient.domain.AppDatabase
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import retrofit2.Response
import java.io.IOException

class FriendActivity : AppActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        FriendFragment.OnListFragmentInteractionListener {

    var friends: MutableList<FriendDto> = mutableListOf()

    private lateinit var friendFragment: FriendFragment
    private lateinit var addFriendDialogView: View
    private lateinit var addFriendDialog: AlertDialog

    private var getFriendsTask: GetFriendsTask? = null
    private var addFriendTask: AddFriendTask? = null
    private var removeFriendTask: RemoveFriendTask? = null
    private var destroyTokenTask: DestroyTokenTask? = null
    private val friendsTasks = listOf(getFriendsTask, addFriendTask, removeFriendTask, destroyTokenTask)

    private val refreshFriends: (Set<FriendDto>) -> Unit = {
        destroyTasks()
        friends.clear()
        friends.addAll(it)
        friendFragment.adapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getFriendsTask = GetFriendsTask()
        getFriendsTask!!.execute()
        setContentView(R.layout.activity_friend)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        addFriendDialogView = layoutInflater.inflate(R.layout.content_add_friend, content)
        addFriendDialogView.btn_add_friend.setOnClickListener {
            addFriendTask = AddFriendTask(addFriendDialogView.actv_user_detail.text.toString())
            addFriendTask!!.execute()
        }
        addFriendDialog = AlertDialog.Builder(this)
                .setTitle(R.string.header_add_friend)
                .setView(addFriendDialogView).create()
        addFriendDialogView.btn_cancel_add_friend.setOnClickListener {
            addFriendDialog.dismiss()
        }
        nav_view.setNavigationItemSelectedListener(this)

        friendFragment = FriendFragment()
        supportFragmentManager.beginTransaction()
                .replace(R.id.friend_fragment_content, friendFragment)
                .commit()
        fab.setOnClickListener { addFriend() }
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
            R.id.navCreateGroup -> { startActivity(GroupActivity::class) }
            R.id.navNewFriend -> { addFriend() }
            R.id.navSettings -> {

            }
            R.id.navSignOut -> DestroyTokenTask({ startActivity(LoginActivity::class) }).execute()
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onListFragmentInteraction(item: FriendDto, menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_remove_friend -> showRemoveFriendDialog(item)
            R.id.action_add_to_group -> makeText(this, "${item.username} poked", LENGTH_LONG).show()
            else -> throw NotImplementedError("Not Implemented!!")
        }
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

    inner class GetFriendsTask internal constructor() : AsyncTask<Void, Void, Response<Set<FriendDto>>>() {

        override fun doInBackground(vararg params: Void): Response<Set<FriendDto>> {
            val db = AppDatabase.getInstance(this@FriendActivity)
            val userId = db.userDao().select()?.id
            val userClient = ServiceGenerator.createService(
                    UserClient::class.java,
                    db.tokenDao().select()?.toTokenString()
            )

            return try {
                userClient.friends(userId!!).execute()
            } catch (e: IOException) {
                handleConnectionException(e)
            }
        }

        override fun onPostExecute(response: Response<Set<FriendDto>>) {
            handleResponse(response, refreshFriends)
        }

    }

    inner class RemoveFriendTask internal constructor(private val friendId: Long) :
            AsyncTask<Void, Void, Response<Set<FriendDto>>>() {

        override fun doInBackground(vararg params: Void): Response<Set<FriendDto>> {
            val db = AppDatabase.getInstance(this@FriendActivity)
            val userId = db.userDao().select()?.id
            val userClient = ServiceGenerator.createService(
                    UserClient::class.java,
                    db.tokenDao().select()?.toTokenString()
            )

            return try {
                userClient.removeFriend(userId!!, friendId).execute()
            } catch (e: IOException) {
                handleConnectionException(e)
            }
        }

        override fun onPostExecute(response: Response<Set<FriendDto>>) {
            handleResponse(response, refreshFriends)
        }

    }

    inner class AddFriendTask internal constructor(private val friendIdentifier: String) :
            AsyncTask<Void, Void, Response<Set<FriendDto>>>() {

        override fun doInBackground(vararg params: Void): Response<Set<FriendDto>> {
            val db = AppDatabase.getInstance(this@FriendActivity)
            val userId = db.userDao().select()?.id
            val userClient = ServiceGenerator.createService(
                    UserClient::class.java,
                    db.tokenDao().select()?.toTokenString()
            )

            return try {
                userClient.addFriend(userId!!, friendIdentifier).execute()
            } catch (e: IOException) {
                handleConnectionException(e)
            }
        }

        override fun onPostExecute(response: Response<Set<FriendDto>>) {
            handleResponse(response, refreshFriends)
        }

    }

}
