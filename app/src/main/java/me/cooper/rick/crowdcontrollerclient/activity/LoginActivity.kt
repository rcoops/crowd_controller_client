package me.cooper.rick.crowdcontrollerclient.activity

import android.Manifest.permission.READ_CONTACTS
import android.app.LoaderManager.LoaderCallbacks
import android.content.CursorLoader
import android.content.DialogInterface.OnClickListener
import android.content.Loader
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_login.*
import me.cooper.rick.crowdcontrollerapi.dto.RegistrationDto
import me.cooper.rick.crowdcontrollerapi.dto.Token
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.client.LoginClient
import me.cooper.rick.crowdcontrollerclient.api.task.user.NewUser
import me.cooper.rick.crowdcontrollerclient.api.util.BAD_PASSWORD
import me.cooper.rick.crowdcontrollerclient.api.util.handleConnectionException
import me.cooper.rick.crowdcontrollerclient.fragment.RegistrationFragment
import me.cooper.rick.crowdcontrollerclient.util.OrdinalSuperscriptFormatter
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator.createService
import retrofit2.Response
import java.io.IOException

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppActivity(), LoaderCallbacks<Cursor>,
        RegistrationFragment.OnRegistrationListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startMainActivityIfLoggedIn()

        setContentView(R.layout.activity_login)

        populateAutoComplete() // Set up the login form

        OrdinalSuperscriptFormatter.format(txt_header)

        setListeners()

        requestLocationPermissions()
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        fun isGranted(grantResults: IntArray): Boolean {
            return grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED
        }
        when (requestCode) {
            REQUEST_READ_CONTACTS -> if (isGranted(grantResults)) populateAutoComplete()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
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

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {}

    override fun register(dto: RegistrationDto) {
        addTask(NewUser(dto, { successfulRegistration(it) }))
    }

    private fun startMainActivityIfLoggedIn() {
        if (getToken().isNotBlank()) startActivity(MainActivity::class, null)
    }

    private fun handleLoginError(it: APIErrorDto) {
        refresh(UserLoginTask::class)
        showProgress(false, login_form, login_progress)
        if (it.error == BAD_PASSWORD) password?.requestFocus()
        else username?.requestFocus()
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
        if (mayRequestContacts()) loaderManager.initLoader(0, null, this)
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
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (isTaskOfTypeRunning(UserLoginTask::class)) return

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
            showProgress(true, login_form, login_progress)
            addTask(UserLoginTask(usernameStr, passwordStr).apply { execute() })
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(this@LoginActivity,
                android.R.layout.simple_dropdown_item_1line, emailAddressCollection)

        username.setAdapter(adapter)
    }

    private fun successfulRegistration(dto: UserDto) {
        supportFragmentManager.popBackStackImmediate()
        destroyTasksOfType(NewUser::class)
        updateLoginForm(dto)
    }

    private fun updateLoginForm(dto: UserDto) {
        showDismissiblePopup(
                getString(R.string.hdr_registration_successful),
                getString(R.string.txt_registration_successful),
                OnClickListener { _, _ -> destroyTasksOfType(UserLoginTask::class) }
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
            val loginClient = createService(
                    LoginClient::class,
                    getString(R.string.jwt_client_id),
                    getString(R.string.jwt_client_secret)
            )

            return try {
                val response = loginClient
                        .getToken(getString(R.string.jwt_grant_type), username, password)
                        .execute()
                val body = response.body()
                if (body is Token) save(body)
                response
            } catch (e: IOException) {
                handleConnectionException(e)
            }
        }

        private fun save(token: Token) {
            editAppDetails {
                putString(getString(R.string.token), "${token.tokenType.capitalize()} ${token.accessToken}")
                putLong(getString(R.string.user_id), token.user!!.id)
            }
        }

        override fun onPostExecute(response: Response<Token>) {
            handleResponse(response, { startActivity(MainActivity::class, UserLoginTask::class) },
                    { handleLoginError(it) })
        }

    }

    companion object {
        private const val REQUEST_READ_CONTACTS = 1
    }

}
