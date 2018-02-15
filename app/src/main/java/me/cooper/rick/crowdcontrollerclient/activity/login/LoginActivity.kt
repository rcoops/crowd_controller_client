package me.cooper.rick.crowdcontrollerclient.activity.login

import android.Manifest.permission.READ_CONTACTS
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.LoaderManager.LoaderCallbacks
import android.arch.persistence.room.Room
import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import kotlinx.android.synthetic.main.activity_login.*
import me.cooper.rick.crowdcontrollerapi.dto.Token
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.TestActivity
import me.cooper.rick.crowdcontrollerclient.auth.LoginClient
import me.cooper.rick.crowdcontrollerclient.auth.UserClient
import me.cooper.rick.crowdcontrollerclient.db.AppDatabase
import me.cooper.rick.crowdcontrollerclient.db.TokenEntity
import me.cooper.rick.crowdcontrollerclient.util.OrdinalSuperscriptFormatter
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import java.net.ConnectException
import java.util.*

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity(), LoaderCallbacks<Cursor>,
        RegistrationFragment.OnRegistrationListener {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null

    private val REQUEST_READ_CONTACTS = 1

    protected val TAG = LoginActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Set up the login form.
        populateAutoComplete()
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        btnEmailSignIn.setOnClickListener { attemptLogin() }

        OrdinalSuperscriptFormatter(SpannableStringBuilder()).format(txtHeader)

        btnRegister.setOnClickListener {
            supportFragmentManager.beginTransaction()
                    .add(R.id.content, RegistrationFragment())
                    .addToBackStack("reg")
                    .commit()
        }
    }

    private fun populateAutoComplete() {
        if (!mayRequestContacts()) {
            return
        }

        loaderManager.initLoader(0, null, this)
    }

    private fun mayRequestContacts(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(username, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok,
                            { requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS) })
        } else {
            requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS)
        }
        return false
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete()
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        username.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val usernameStr = username.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(usernameStr)) {
            username.error = getString(R.string.error_field_required)
            focusView = username
            cancel = true
        }
//        else if (!isEmailValid(emailStr)) {
//            username.error = getString(R.string.error_invalid_email)
//            focusView = username
//            cancel = true
//        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(usernameStr, passwordStr)
            mAuthTask!!.execute(null as Void?)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        //TODO: Replace this with your own logic
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        //TODO: Replace this with your own logic
        return true//password.length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        return CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE),

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC")
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

        addEmailsToAutoComplete(emails)
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {

    }

    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(this@LoginActivity,
                android.R.layout.simple_dropdown_item_1line, emailAddressCollection)

        username.setAdapter(adapter)
    }

    private fun openActivity(token: Token?) {
        if (token != null) {
            if (token.accessToken != null) {
                SaveTokenTask(token).execute()
                val intent = Intent(this, TestActivity::class.java)
                intent.putExtra("username", username.text.toString())
                startActivity(intent)
            }

        }
    }

    private fun listUsers(users: List<UserDto>) {
        val userList = users.joinToString("\n")
        makeText(this, userList, LENGTH_LONG).show()
    }

    override fun onFragmentInteraction(userDto: UserDto) {
        supportFragmentManager.popBackStackImmediate()
        listUsers(listOf(userDto))
    }

    object ProfileQuery {
        val PROJECTION = arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY)
        val ADDRESS = 0
        val IS_PRIMARY = 1
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserLoginTask internal constructor(
            private val username: String,
            private val password: String): AsyncTask<Void, Void, Token?>() {

        override fun doInBackground(vararg params: Void): Token? {
            val userClient = ServiceGenerator.createService(
                    LoginClient::class.java,
//                    "nothing",
                    getString(R.string.jwt_client_id),
                    getString(R.string.jwt_client_secret)
            )

            // TODO ERROR HANDLING
            val response = try {
                userClient
                        .getToken(getString(R.string.jwt_grant_type), username, password)
                        .execute()
            } catch (e: ConnectException) {
                null
            }

            return response?.body()
        }

        override fun onPostExecute(result: Token?) {
            openActivity(result)
        }

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UsersTask internal constructor(
            private val token: String): AsyncTask<Void, Void, List<UserDto>>() {

        override fun doInBackground(vararg params: Void): List<UserDto> {
            val userClient = ServiceGenerator.createService(
                    UserClient::class.java, token
            )
            val response = userClient
                    .users()
                    .execute()

            //TODO error checking

            return response.body() ?: emptyList()
        }

        override fun onPostExecute(result: List<UserDto>) {
            listUsers(result)
        }

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class SaveTokenTask internal constructor(
            token: Token): AsyncTask<Void, Void, Unit>() {

        private val token: Token = token.copy(tokenType = token.tokenType.capitalize())

        override fun doInBackground(vararg params: Void) {
            val db = Room.databaseBuilder(this@LoginActivity, AppDatabase::class.java, "app-db").build()
            val dao = db.tokenDao()
            val savedToken = dao.getToken()
            if (savedToken != null) dao.delete(savedToken)
            dao.insert(TokenEntity.fromDto(token))
        }

    }


}
