package me.cooper.rick.crowdcontrollerclient.fragment.friend

import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.View.VISIBLE
import android.view.View.GONE
import android.widget.TextView
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto.Status
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto.Status.*
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment.OnFriendFragmentInteractionListener

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnFriendFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class FriendRecyclerViewAdapter(private val mValues: List<FriendDto>,
                                private val mListener: OnFriendFragmentInteractionListener) :
        RecyclerView.Adapter<FriendRecyclerViewAdapter.ViewHolder>() {

    lateinit var parent: ViewGroup

    private val statusUpdate: (FriendDto, Status) -> Unit = { friendDto, status ->
        mListener.onListItemFriendUpdate(friendDto.copy(status = status))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        this.parent = parent
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_friend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.txtContentView.text = mValues[position].username
        setStatusView(holder, holder.mItem!!)
    }

    private fun setStatusView(holder: ViewHolder, friendDto: FriendDto) {

        when (friendDto.status) {
            FriendDto.Status.CONFIRMED -> {
                holder.vwOverlay.visibility = GONE
                holder.cslConfirmView.visibility = GONE
                holder.mView.setOnCreateContextMenuListener(holder as View.OnCreateContextMenuListener)
                holder.mView.isLongClickable = false
                holder.fabContextMenu.setOnClickListener { holder.mView.showContextMenu() }
                holder.fabAccept.setOnClickListener(null)
                holder.fabRefuse.setOnClickListener(null)
            }
            FriendDto.Status.AWAITING_ACCEPT -> {
                holder.vwOverlay.visibility = VISIBLE
                holder.cslConfirmView.visibility = VISIBLE
                holder.fabAccept.visibility = GONE
                holder.mView.setOnCreateContextMenuListener(null)
                holder.txtAction.text = parent.context.getString(R.string.txt_cancel_friend_request)
                holder.fabAccept.setOnClickListener(null)
                holder.fabRefuse.setOnClickListener { statusUpdate(friendDto, INACTIVE) }
            }
            FriendDto.Status.TO_ACCEPT -> {
                holder.vwOverlay.visibility = VISIBLE
                holder.cslConfirmView.visibility = VISIBLE
                holder.fabAccept.visibility = VISIBLE
                holder.txtAction.text = parent.context.getString(R.string.txt_accept_friend_request)
                holder.mView.setOnCreateContextMenuListener(null)
                holder.fabAccept.setOnClickListener { statusUpdate(friendDto, CONFIRMED) }
                holder.fabRefuse.setOnClickListener { statusUpdate(friendDto, INACTIVE) }
            }
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView),
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener {
        val fabAccept: FloatingActionButton = mView.findViewById(R.id.fab_accept_friend)
        val fabRefuse: FloatingActionButton = mView.findViewById(R.id.fab_refuse_friend)
        val txtContentView: TextView = mView.findViewById(R.id.txt_friend_content)
        val vwOverlay: View = mView.findViewById(R.id.vw_friend_overlay)
        val txtAction: TextView = mView.findViewById(R.id.txt_friend_action)
        val cslConfirmView: ConstraintLayout = mView.findViewById(R.id.layout_friend_action)
        val fabContextMenu: FloatingActionButton = mView.findViewById(R.id.fab_friend_menu)
        var mItem: FriendDto? = null

        init {
            mView.isLongClickable = false
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            (mListener as AppActivity).menuInflater.inflate(R.menu.menu_context_friend, menu)

            menu!!.setHeaderView(LayoutInflater.from(parent.context)
                    .inflate(R.layout.context_header, parent, false)
                    .apply { (findViewById<TextView>(R.id.txt_header)).text = mItem!!.username })

            (0 until menu.size()).forEach { menu.getItem(it).setOnMenuItemClickListener(this) }
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            mListener.onListItemContextMenuSelection(mItem!!, item!!)
            return true
        }

        override fun toString(): String {
            return "${super.toString()} '${txtContentView.text}'"
        }

    }

}
