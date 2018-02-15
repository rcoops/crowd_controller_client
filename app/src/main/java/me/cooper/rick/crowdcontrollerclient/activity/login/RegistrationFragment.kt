package me.cooper.rick.crowdcontrollerclient.activity.login

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_registration.*
import me.cooper.rick.crowdcontrollerapi.dto.RegistrationDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto

import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.auth.RetrieveTokenTask
import me.cooper.rick.crowdcontrollerclient.auth.UserClient
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [RegistrationFragment.OnRegistrationListener] interface
 * to handle interaction events.
 * Use the [RegistrationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RegistrationFragment : Fragment() {

    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null

    private var mListener: OnRegistrationListener? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments.getString(ARG_PARAM1)
            mParam2 = arguments.getString(ARG_PARAM2)
        }
        RetrieveTokenTask(this.activity, { this.token = "${it.tokenType} ${it.accessToken}" })
                .execute()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_registration, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnRegisterAccount.setOnClickListener {
            // TODO validate password
            RegisterTask(token!!, createDto())
        }

    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnRegistrationListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnRegistrationListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun createDto(): RegistrationDto = RegistrationDto(
            username.text.toString(),
            password.text.toString(),
            email.text.toString(),
            mobileNumber.text.toString()
    )

    interface OnRegistrationListener {
        fun onFragmentInteraction(userDto: UserDto)
    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = "param1"
        private val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RegistrationFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String): RegistrationFragment {
            val fragment = RegistrationFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class RegisterTask internal constructor(
            private val token: String,
            private val registrationDto: RegistrationDto) : AsyncTask<Void, Void, UserDto>() {

        override fun doInBackground(vararg params: Void): UserDto {
            val userClient = ServiceGenerator.createService(
                    UserClient::class.java, token
            )
            val response = userClient
                    .create(registrationDto)
                    .execute()

            //TODO error checking

            return response.body() ?: UserDto()
        }

        override fun onPostExecute(result: UserDto) {
            mListener!!.onFragmentInteraction(result)
        }

    }
}
