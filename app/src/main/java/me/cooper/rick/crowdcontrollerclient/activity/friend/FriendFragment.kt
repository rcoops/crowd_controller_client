package me.cooper.rick.crowdcontrollerclient.activity.friend

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
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
class FriendFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var mListener: OnListFragmentInteractionListener? = null
    private lateinit var friends: MutableList<FriendDto>
    lateinit var adapter: FriendRecyclerViewAdapter
    lateinit var swipeView: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity !is FriendActivity) throw RuntimeException()
        friends = (activity as FriendActivity).friends
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        swipeView = (inflater!!
                .inflate(R.layout.fragment_friend_list, container, false) as SwipeRefreshLayout)
                .apply { setOnRefreshListener(this@FriendFragment) }

        val view = swipeView.findViewById<RecyclerView>(R.id.list)
        val context = swipeView.context
        view.layoutManager = LinearLayoutManager(context)
        adapter = FriendRecyclerViewAdapter(friends, mListener!!)
        view.adapter = adapter

        registerForContextMenu(view)
        (activity as FriendActivity).swipeView = swipeView
        return swipeView
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

    override fun onRefresh() {
        mListener?.onSwipe(swipeView)
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
        fun onListItemContextMenuSelection(friend: FriendDto, menuItem: MenuItem)

        fun onListItemFriendInviteResponse(friend: FriendDto, isAccepting: Boolean)
        fun onSwipe(swipeView: SwipeRefreshLayout?)
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
