package me.cooper.rick.crowdcontrollerclient.activity.login

import android.Manifest.permission.READ_CONTACTS
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.LoaderManager.LoaderCallbacks
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
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.Log
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
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity
import me.cooper.rick.crowdcontrollerclient.activity.friend.FriendActivity
import me.cooper.rick.crowdcontrollerclient.api.client.LoginClient
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus.Companion.BAD_REQUEST
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus.Companion.OK
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus.Companion.SERVICE_UNAVAILABLE
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus.Companion.UNAUTHORIZED
import me.cooper.rick.crowdcontrollerclient.domain.AppDatabase
import me.cooper.rick.crowdcontrollerclient.domain.entity.TokenEntity
import me.cooper.rick.crowdcontrollerclient.domain.entity.UserEntity
import me.cooper.rick.crowdcontrollerclient.util.OrdinalSuperscriptFormatter
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppActivity(), LoaderCallbacks<Cursor>,
        RegistrationFragment.OnRegistrationListener {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null

    private val REQUEST_READ_CONTACTS = 1

    private val TAG = LoginActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CheckTokenTask().execute()
        setContentView(R.layout.activity_login)
        // Set up the login form.
        populateAutoComplete()

        OrdinalSuperscriptFormatter(SpannableStringBuilder()).format(txtHeader)

        setListeners()
    }

    private fun setListeners() {
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        btnEmailSignIn.setOnClickListener { attemptLogin() }
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

        // Check for a valid password, if the baseUserEntity entered one.
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

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the baseUserEntity login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(usernameStr, passwordStr)
            mAuthTask!!.execute(null as Void?)
        }
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
                // Retrieve data rows for the device baseUserEntity's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE),

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the baseUserEntity hasn't specified one.
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

    private fun successfulLogin() {
        destroyAuthTask()
        startActivity(Intent(this, FriendActivity::class.java))
    }

    private fun failedLogin(header: String, message: String) {
        destroyAuthTask()
        showDismissablePopup(header, message)
    }

    private fun destroyAuthTask() {
        showProgress(false)
        mAuthTask?.cancel(true)
        mAuthTask = null
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
     * the baseUserEntity.
     */
    inner class UserLoginTask internal constructor(
            private val username: String,
            private val password: String): AsyncTask<Void, Void, Int>() {

        override fun doInBackground(vararg params: Void): Int {
            val userClient = ServiceGenerator.createService(
                    LoginClient::class.java,
                    getString(R.string.jwt_client_id),
                    getString(R.string.jwt_client_secret)
            )

            // TODO ERROR HANDLING
            val response =  try {
                userClient.getToken(getString(R.string.jwt_grant_type), username, password).execute()
            } catch (e: Exception) {
                when (e) {
                    is ConnectException, is SocketTimeoutException -> {
                        Response.error<Any>(SERVICE_UNAVAILABLE, ResponseBody
                                .create(MediaType.parse("text/plain"), "Connection Failed"))
                    }
                    else -> throw e
                }
            }
            val body = response.body()
            if (body is Token) {
                val db = AppDatabase.getInstance(this@LoginActivity)
                val tokenDao = db.tokenDao()
                val userDao = db.userDao()
                tokenDao.clear()
                userDao.clear()

                tokenDao.insert(TokenEntity.fromDto(body))
                userDao.insert(UserEntity.fromDto(body.user!!))

                Log.d("TOKEN", tokenDao.select().toString())
                Log.d("USER", userDao.select().toString())
            }

            return response.code()
        }

        override fun onPostExecute(responseCode: Int) {
            when (responseCode) {
                OK -> successfulLogin()
                BAD_REQUEST, UNAUTHORIZED -> failedLogin(getString(R.string.header_bad_credentials),
                        getString(R.string.txt_bad_credentials))
                SERVICE_UNAVAILABLE -> failedLogin(getString(R.string.header_connect_fail),
                        getString(R.string.txt_connect_fail))
            }
        }

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the baseUserEntity.
     */
    inner class CheckTokenTask: AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void): Boolean {
            val db = AppDatabase.getInstance(this@LoginActivity)
            val tokenDao = db.tokenDao()
            return tokenDao.select() != null
        }

        override fun onPostExecute(result: Boolean) {
            successfulLogin()
        }

    }


}
