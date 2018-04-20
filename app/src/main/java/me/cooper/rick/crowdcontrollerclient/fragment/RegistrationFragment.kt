package me.cooper.rick.crowdcontrollerclient.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_registration.*
import me.cooper.rick.crowdcontrollerapi.dto.user.RegistrationDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.R.id.*
import java.util.regex.Pattern

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
            fragmentListener?.playClick()
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
        val emailStr = email.text.toString()
        return when {
            usernameStr.isBlank() -> FormValidation.EMPTY_USERNAME
            passwordStr.isBlank() -> FormValidation.EMPTY_PASSWORD
            passwordStr.length < 5 -> FormValidation.PASSWORD_TOO_SHORT
            passwordStr != passwordConfirmStr -> FormValidation.NON_MATCHING_PASSWORDS
            !isValidEmail(emailStr) -> FormValidation.EMAIL_INVALID
            else -> FormValidation.VALID
        }
    }

    private fun isValidEmail(email: String) = EmailValidator.validate(email)

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = (context as? OnRegistrationListener) ?: throw RuntimeException(context!!.toString() + " must implement OnRegistrationListener")
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
        EMAIL_INVALID("Invalid Email!", "You must provide a valid email", email),
        PASSWORD_TOO_SHORT("Password too Short!", "The password must be over 4 characters long", password),
        NON_MATCHING_PASSWORDS("Passwords do not match", "Your password and confirmation do not match, please try again", passwordConfirm),
        VALID("No Issues", "None required", -1)
    }

    companion object {
        private const val TITLE = "Registration"
    }

    // https://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
    object EmailValidator {
        private val EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"

        private val pattern = Pattern.compile(EMAIL_PATTERN)

        fun validate(hex: String): Boolean {
            val matcher = pattern.matcher(hex)
            return matcher.matches()
        }

    }

}
