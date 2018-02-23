package me.cooper.rick.crowdcontrollerclient.activity.friend

import android.content.Intent
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
import me.cooper.rick.crowdcontrollerclient.api.UserClient
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus
import me.cooper.rick.crowdcontrollerclient.domain.AppDatabase
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import retrofit2.Response
import java.net.ConnectException

class FriendActivity : AppActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        FriendFragment.OnListFragmentInteractionListener {

    var friends: MutableList<FriendDto> = mutableListOf()
    var friendFragment: FriendFragment? = null
    var addFriendDialogView: View? = null
    var addFriendDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GetFriendsTask().execute()
        setContentView(R.layout.activity_friend)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        addFriendDialogView = layoutInflater.inflate(R.layout.content_add_friend, content)
        addFriendDialogView!!.btn_add_friend.setOnClickListener {
            AddFriendTask(addFriendDialogView!!.actv_user_detail.text.toString()).execute()
        }
        addFriendDialog = AlertDialog.Builder(this)
                .setTitle(R.string.header_add_friend)
                .setView(addFriendDialogView).create()
        addFriendDialogView!!.btn_cancel_add_friend.setOnClickListener {
            addFriendDialog!!.dismiss()
        }
        nav_view.setNavigationItemSelectedListener(this)

        friendFragment = FriendFragment()
        supportFragmentManager.beginTransaction()
                .replace(R.id.friendFragmentLayout, friendFragment)
                .commit()
        fab.setOnClickListener {
            addFriendDialogView!!.actv_user_detail.text.clear()
            addFriendDialog!!.show()
        }
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
            R.id.navCreateGroup -> {
                val intent = Intent(this, GroupActivity::class.java)
                startActivity(intent)
                // Handle the camera action
            }
            R.id.navNewFriend -> {

            }
            R.id.navSettings -> {

            }
            R.id.navSignOut -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun dealWithResponse(response: Response<Set<FriendDto>>?) {
        when {
            response == null -> {
                addFriendDialog!!.dismiss()
                showDismissablePopup(
                        getString(R.string.header_connection_failed),
                        getString(R.string.txt_connection_failed)
                )
            }
            HttpStatus.NOT_FOUND == response.code() -> {
                val detail = addFriendDialogView!!.actv_user_detail.text.toString()
                showDismissablePopup(
                        getString(R.string.header_friend_not_found),
                        getString(R.string.txt_friend_not_found, detail)
                )
            }
            else -> {
                addFriendDialog!!.dismiss()
                friends.clear()
                friends.addAll(response.body()!!.toSet())
                friendFragment?.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onListFragmentInteraction(item: FriendDto) {
        makeText(this, "${item.username} poked", LENGTH_LONG).show()
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the baseUserEntity.
     */
    inner class GetFriendsTask internal constructor(): AsyncTask<Void, Void, List<FriendDto>>() {

        override fun doInBackground(vararg params: Void): List<FriendDto> {
            val db = AppDatabase.getInstance(this@FriendActivity)
            val userId = db.userDao().select()?.id
            val userClient = ServiceGenerator.createService(
                    UserClient::class.java,
                    db.tokenDao().select()?.toTokenString()
            )

            // TODO ERROR HANDLING
            val response = try {
                userClient.friends(userId!!).execute()
            } catch (e: ConnectException) {
                null
            }
            val friends = response?.body()

            return friends?.toList() ?: emptyList()
        }

        override fun onPostExecute(friends: List<FriendDto>) {
            this@FriendActivity.friends.clear()
            this@FriendActivity.friends.addAll(friends)
            this@FriendActivity.friendFragment?.adapter?.notifyDataSetChanged()
        }

    }

    inner class AddFriendTask internal constructor(private val friendIdentifier: String):
            AsyncTask<Void, Void, Response<Set<FriendDto>>?>() {

        override fun doInBackground(vararg params: Void): Response<Set<FriendDto>>? {
            val db = AppDatabase.getInstance(this@FriendActivity)
            val userId = db.userDao().select()?.id
            val userClient = ServiceGenerator.createService(
                    UserClient::class.java,
                    db.tokenDao().select()?.toTokenString()
            )

            return try {
                userClient.addFriend(userId!!, friendIdentifier).execute()
            } catch (e: ConnectException) {
                null
            }
        }

        override fun onPostExecute(response: Response<Set<FriendDto>>?) {
            dealWithResponse(response)
        }

    }

}
