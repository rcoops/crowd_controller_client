package me.cooper.rick.crowdcontrollerclient.activity.friend

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerclient.R

/**
 * A fragment representing a list of Items.
 *
 *
 * Activities containing this fragment MUST implement the [OnListFragmentInteractionListener]
 * interface.
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class FriendFragment : Fragment() {

    private var mColumnCount = 1
    private var mListener: OnListFragmentInteractionListener? = null
    private lateinit var friends: MutableList<FriendDto>
    lateinit var adapter: FriendRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity !is FriendActivity) throw RuntimeException()
        friends = (activity as FriendActivity).friends
        arguments?.let { mColumnCount = it.getInt(ARG_COLUMN_COUNT) }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_friend_list, container, false)
        // Set the adapter
        if (view is RecyclerView) {
            val context = view.getContext()
            view.layoutManager = if (mColumnCount <= 1) {
                LinearLayoutManager(context)
            } else {
                GridLayoutManager(context, mColumnCount)
            }
            adapter = FriendRecyclerViewAdapter(friends, mListener!!)
            view.adapter = adapter
        }

        registerForContextMenu(view)
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context !is OnListFragmentInteractionListener) {
            throw RuntimeException("${context!!} must implement OnListFragmentInteractionListener")
        }
        mListener = context
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(friend: FriendDto, menuItem: MenuItem)
    }

    companion object {

        // TODO: Customize parameter argument names
        private val ARG_COLUMN_COUNT = "column-isToken"

        // TODO: Customize parameter initialization
        fun newInstance(columnCount: Int): FriendFragment {
            val fragment = FriendFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }
}
