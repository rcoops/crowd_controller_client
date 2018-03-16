package me.cooper.rick.crowdcontrollerclient.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_registration.*
import me.cooper.rick.crowdcontrollerapi.dto.user.RegistrationDto
import me.cooper.rick.crowdcontrollerclient.R

class RegistrationFragment : AbstractAppFragment() {

    private var listener: OnRegistrationListener? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_registration, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnRegisterAccount.setOnClickListener { listener?.register(createDto()) }
    }

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
    }

    companion object {
        private const val TITLE = "Registration"
    }

}
