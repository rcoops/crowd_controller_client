package me.cooper.rick.crowdcontrollerclient.activity

import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_test.*
import kotlinx.android.synthetic.main.content_test.*
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.auth.RetrieveTokenTask
import me.cooper.rick.crowdcontrollerclient.auth.UserClient
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator

class TestActivity : AppCompatActivity() {

    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        setSupportActionBar(toolbar)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        RetrieveTokenTask(this, { this.token = "${it.tokenType} ${it.accessToken}" }).execute()
        btnUser.setOnClickListener {
            UserTask(token!!, intent.getStringExtra("username")).execute()
        }
        btnUsers.setOnClickListener {
            UsersTask(token!!).execute()
        }
    }

    private fun displayUser(user: UserDto) {
        Toast.makeText(this, user.toString(), Toast.LENGTH_LONG).show()
    }

    private fun listUsers(users: List<UserDto>?) {
        Toast.makeText(this, users?.joinToString(), Toast.LENGTH_LONG).show()
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the baseUserEntity.
     */
    inner class UserTask internal constructor(
            private val token: String,
            private val username: String): AsyncTask<Void, Void, UserDto>() {

        override fun doInBackground(vararg params: Void): UserDto {
            val userClient = ServiceGenerator.createService(
                    UserClient::class.java, token
            )
            val response = userClient
                    .me(username)
                    .execute()

            //TODO error checking

            return response.body() ?: UserDto()
        }

        override fun onPostExecute(result: UserDto) {
            displayUser(result)
        }

    }
    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the baseUserEntity.
     */
    inner class UsersTask internal constructor(
            private val token: String): AsyncTask<Void, Void, List<UserDto>?>() {

        override fun doInBackground(vararg params: Void): List<UserDto>? {
            val userClient = ServiceGenerator.createService(
                    UserClient::class.java, token
            )
            val response = userClient
                    .users()
                    .execute()

            //TODO error checking

            return response.body()
        }

        override fun onPostExecute(result: List<UserDto>?) {
            listUsers(result)
        }

    }

}
