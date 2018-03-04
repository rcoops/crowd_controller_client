package me.cooper.rick.crowdcontrollerclient.fragment.friend

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerclient.fragment.listener.FragmentInteractionListener
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.MainActivity

class FriendFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var listener: OnFriendFragmentInteractionListener? = null

    private lateinit var adapter: FriendRecyclerViewAdapter
    private lateinit var swipeView: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity !is MainActivity) throw RuntimeException()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        swipeView = (inflater!!
                .inflate(R.layout.fragment_friend_list, container, false) as SwipeRefreshLayout)
                .apply { setOnRefreshListener(this@FriendFragment) }

        val view = swipeView.findViewById<RecyclerView>(R.id.list)
        adapter = FriendRecyclerViewAdapter((activity as MainActivity).friends, listener!!)
        view.adapter = adapter

        registerForContextMenu(view)

        return swipeView
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = if (context is OnFriendFragmentInteractionListener) context
        else throw RuntimeException("${context!!} must implement OnFriendFragmentInteractionListener")
    }

    override fun onResume() {
        super.onResume()
        listener?.setFragmentProperties(swipeView, TITLE)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun updateView() = adapter.notifyDataSetChanged()

    override fun onRefresh(): Unit = listener!!.onSwipe(swipeView)

    interface OnFriendFragmentInteractionListener: FragmentInteractionListener {

        fun onListItemContextMenuSelection(friend: FriendDto, menuItem: MenuItem)

        fun onListItemFriendInviteResponse(friend: FriendDto, isAccepting: Boolean)

    }

    companion object {
        private const val TITLE = "Friends"
    }

}
