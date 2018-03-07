package me.cooper.rick.crowdcontrollerclient.fragment.group

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.MainActivity
import me.cooper.rick.crowdcontrollerclient.fragment.AbstractAppFragment
import me.cooper.rick.crowdcontrollerclient.fragment.listener.FragmentInteractionListener

class GroupFragment : AbstractAppFragment(), SwipeRefreshLayout.OnRefreshListener  {

    private var listener: OnGroupFragmentInteractionListener? = null

    // has to be nullable in case get group called before view instantiated
    private var adapter: GroupRecyclerViewAdapter? = null
    private lateinit var swipeView: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity !is MainActivity) throw RuntimeException()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        swipeView = (inflater!!
                .inflate(R.layout.fragment_group_list, container, false) as SwipeRefreshLayout)
                .apply { setOnRefreshListener(this@GroupFragment) }

        val view = swipeView.findViewById<RecyclerView>(R.id.list)
        adapter = GroupRecyclerViewAdapter((activity as MainActivity).groupMembers, listener!!)
        view.adapter = adapter

        registerForContextMenu(view)
        onRefresh()
        return swipeView
    }

    override fun onResume() {
        super.onResume()
        listener?.setFragmentProperties(this)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = if (context is OnGroupFragmentInteractionListener) context
        else throw RuntimeException("${context!!} must implement OnFriendFragmentInteractionListener")
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun updateView() = adapter?.notifyDataSetChanged()

    override fun onRefresh() = listener!!.onSwipe(swipeView)

    override fun getTitle(): String = TITLE

    override fun getSwipeView(): SwipeRefreshLayout = swipeView

    interface OnGroupFragmentInteractionListener: FragmentInteractionListener {

        fun onListItemContextMenuSelection(friend: UserDto, menuItem: MenuItem)

        fun onListFragmentInteraction(item: UserDto)

    }

    companion object {
        private const val TITLE = "Group"
    }

}
