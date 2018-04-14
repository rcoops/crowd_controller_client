package me.cooper.rick.crowdcontrollerclient.fragment

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_registration.*
import me.cooper.rick.crowdcontrollerapi.dto.user.RegistrationDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.R.id.*

class RegistrationFragment : AbstractAppFragment() {

    private var listener: OnRegistrationListener? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_registration, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_register_account.setOnClickListener {
            val validationStatus = validateForm()
            if (validationStatus == FormValidation.VALID) {
                listener?.register(createDto())
            } else {
                validationAction(validationStatus)
            }
        }
    }

    private fun validationAction(validationStatus: RegistrationFragment.FormValidation) {
        when (validationStatus) {
            FormValidation.VALID -> listener?.register(createDto())
            else -> {
                listener?.showRegistrationErrorPopup(
                        validationStatus.error,
                        validationStatus.instruction,
                        { view?.findViewById<View>(validationStatus.uiId)?.requestFocus() }
                )
            }
        }
    }

    private fun validateForm(): FormValidation {
        val usernameStr = username.text.toString()
        val passwordStr = password.text.toString()
        val passwordConfirmStr = passwordConfirm.text.toString()
        val mobileNumberStr = mobileNumber.text.toString()
        return when {
            usernameStr.isBlank() -> FormValidation.EMPTY_USERNAME
            passwordStr.isBlank() -> FormValidation.EMPTY_PASSWORD
            passwordStr.length < 5 -> FormValidation.PASSWORD_TOO_SHORT
            passwordStr != passwordConfirmStr -> FormValidation.NON_MATCHING_PASSWORDS
            !isValidMobile(mobileNumberStr) -> FormValidation.MOBILE_INVALID
            else -> FormValidation.VALID
        }
    }

    private fun isValidMobile(mobileNumberStr: String) = mobileNumberStr.matches("07\\d{9}".toRegex())

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = (context as? OnRegistrationListener) ?:
            throw RuntimeException(context!!.toString() + " must implement OnRegistrationListener")
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun getTitle(): String = TITLE

    private fun createDto(): RegistrationDto = RegistrationDto(
            username.text.toString(),
            password.text.toString(),
            email.text.toString(),
            mobileNumber.text.toString()
    )

    interface OnRegistrationListener {
        fun register(dto: RegistrationDto)
        fun showRegistrationErrorPopup(error: String, instruction: String, consumer: () -> Unit)
    }

    enum class FormValidation(val error: String, val instruction: String, val uiId: Int) {
        EMPTY_USERNAME("Username Empty!", "Please enter a username", username),
        EMPTY_PASSWORD("Password Empty!", "Please enter a password", password),
        MOBILE_INVALID("Invalid Mobile Number!", "You must provide a mobile number of 11 digits starting with 07", mobileNumber),
        PASSWORD_TOO_SHORT("Password too Short!", "The password must be over 4 characters long", password),
        NON_MATCHING_PASSWORDS("Passwords do not match", "Your password and confirmation do not match, please try again", passwordConfirm),
        VALID("No Issues", "None required", -1)
    }

    companion object {
        private const val TITLE = "Registration"
    }

}
