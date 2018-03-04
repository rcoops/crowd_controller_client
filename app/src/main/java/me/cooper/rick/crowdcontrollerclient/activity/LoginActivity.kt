package me.cooper.rick.crowdcontrollerclient.activity

import android.Manifest.permission.READ_CONTACTS
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.LoaderManager.LoaderCallbacks
import android.content.CursorLoader
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
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_login.*
import me.cooper.rick.crowdcontrollerapi.dto.Token
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.client.LoginClient
import me.cooper.rick.crowdcontrollerclient.api.util.BAD_PASSWORD
import me.cooper.rick.crowdcontrollerclient.api.util.handleConnectionException
import me.cooper.rick.crowdcontrollerclient.persistence.AppDatabase
import me.cooper.rick.crowdcontrollerclient.persistence.model.UserEntity
import me.cooper.rick.crowdcontrollerclient.fragment.RegistrationFragment
import me.cooper.rick.crowdcontrollerclient.util.OrdinalSuperscriptFormatter
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import retrofit2.Response
import java.io.IOException
import java.util.*

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppActivity(), LoaderCallbacks<Cursor>,
        RegistrationFragment.OnRegistrationListener {

    private var mAuthTask: UserLoginTask? = null
    private var mCheckTokenTask: CheckTokenTask? = null

    private val loginSuccess: (Any) -> Unit = { startActivity(MainActivity::class) }

    private val handleLoginError: (APIErrorDto) -> Unit = {
        destroyTasks()
        showProgress(false)
        if (it.error == BAD_PASSWORD) password?.requestFocus()
        else username?.requestFocus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCheckTokenTask = CheckTokenTask().apply { execute() }
        setContentView(R.layout.activity_login)
        // Set up the login form.
        populateAutoComplete()

        OrdinalSuperscriptFormatter(SpannableStringBuilder()).format(txt_header)

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
        btn_username_signin.setOnClickListener { attemptLogin() }
        btn_register.setOnClickListener {
            supportFragmentManager.beginTransaction()
                    .add(R.id.content, RegistrationFragment())
                    .addToBackStack("reg")
                    .commit()
        }
    }

    private fun populateAutoComplete() {
        if (mayRequestContacts()) {
            loaderManager.initLoader(0, null, this)
        }
    }

    private fun mayRequestContacts(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
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
        if (mAuthTask != null) return

        // Reset errors.
        username.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val usernameStr = username.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if (TextUtils.isEmpty(usernameStr)) {
            username.error = getString(R.string.error_field_required)
            focusView = username
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            showProgress(true)
            mAuthTask = UserLoginTask(usernameStr, passwordStr).apply { execute() }
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
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
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        return CursorLoader(this,
                // Retrieve data rows for the device baseUserEntity's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE),

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

    override fun destroyTasks() {
        mAuthTask?.cancel(true)
        mAuthTask = null
        mCheckTokenTask?.cancel(true)
        mCheckTokenTask = null
    }

    override fun onFragmentInteraction(userDto: Response<UserDto>) {
        supportFragmentManager.popBackStackImmediate()
        handleResponse(userDto, { registrationSuccessful(it) })
    }

    private fun registrationSuccessful(dto: UserDto) {
        showDismissiblePopup(
                getString(R.string.hdr_registration_successful),
                getString(R.string.txt_registration_successful),
                destroyTasksOnClickListener
        )
        username.setText(dto.username)
        password.text.clear()
        password.requestFocus()
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
            private val password: String) : AsyncTask<Void, Void, Response<Token>>() {

        override fun doInBackground(vararg params: Void): Response<Token> {
            val loginClient = ServiceGenerator.createService(
                    LoginClient::class,
                    getString(R.string.jwt_client_id),
                    getString(R.string.jwt_client_secret)
            )

            return try {
                val response = loginClient.getToken(getString(R.string.jwt_grant_type), username, password).execute()
                val body = response.body()
                if (body is Token) save(body)
                response
            } catch (e: IOException) {
                handleConnectionException(e)
            }
        }

        private fun save(token: Token) {
            val db = AppDatabase.getInstance(this@LoginActivity)
            val userDao = db.userDao()
            userDao.clear()

            userDao.insert(UserEntity.fromDto(token))
        }

        override fun onPostExecute(response: Response<Token>) {
            handleResponse(response, loginSuccess, handleLoginError)
        }

    }

    inner class CheckTokenTask : AsyncTask<Void, Void, String?>() {

        override fun doInBackground(vararg params: Void): String? {
            val db = AppDatabase.getInstance(this@LoginActivity)
            val userDao = db.userDao()
            return userDao.select()?.token
        }

        override fun onPostExecute(result: String?) {
            result?.let { loginSuccess(result) }
        }

    }


    companion object {

        private const val REQUEST_READ_CONTACTS = 1

    }

}
