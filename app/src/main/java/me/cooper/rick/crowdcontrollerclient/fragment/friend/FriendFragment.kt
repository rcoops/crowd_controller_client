package me.cooper.rick.crowdcontrollerclient.fragment.friend

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.MainActivity
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.friends
import me.cooper.rick.crowdcontrollerclient.fragment.AbstractAppFragment
import me.cooper.rick.crowdcontrollerclient.fragment.listener.SwipeFragmentInteractionListener

class FriendFragment : AbstractAppFragment(), SwipeRefreshLayout.OnRefreshListener {

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
        adapter = FriendRecyclerViewAdapter(friends, listener!!)
        view.adapter = adapter

        registerForContextMenu(view)

        listener?.pushView(swipeView)
        return swipeView
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = (context as? OnFriendFragmentInteractionListener) ?:
        throw RuntimeException("${context!!} must implement OnFriendFragmentInteractionListener")
    }

    override fun onDetach() {
        super.onDetach()
        listener?.popView(swipeView)
        listener = null
    }

    fun updateView() = adapter.notifyDataSetChanged()

    override fun onRefresh(): Unit = listener!!.onSwipe(swipeView)

    override fun getTitle(): String = TITLE

    interface OnFriendFragmentInteractionListener: SwipeFragmentInteractionListener {

        fun onListItemContextMenuSelection(dto: FriendDto, menuItem: MenuItem)

        fun onListItemFriendUpdate(dto: FriendDto)

    }

    companion object {
        private const val TITLE = "Friends"
    }

}
