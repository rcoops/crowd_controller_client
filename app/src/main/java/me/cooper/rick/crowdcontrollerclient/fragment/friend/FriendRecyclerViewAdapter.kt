package me.cooper.rick.crowdcontrollerclient.fragment.friend

import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
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
                holder.txtOverlayView.visibility = View.GONE
                holder.cslConfirmView.visibility = View.GONE
                holder.txtOverlayView.text = ""
                holder.mView.setOnCreateContextMenuListener(holder as View.OnCreateContextMenuListener)
                holder.mView.isLongClickable = false
                holder.fabContextMenu.setOnClickListener { holder.mView.showContextMenu() }
                noButtonListeners(holder.fabAccept, holder.fabRefuse)
            }
            FriendDto.Status.AWAITING_ACCEPT -> {
                holder.txtOverlayView.visibility = View.VISIBLE
                holder.cslConfirmView.visibility = View.GONE
                holder.txtOverlayView.text = (mListener as AppActivity).getString(R.string.txt_awaiting_friend_accept)
                holder.mView.setOnCreateContextMenuListener(null)
                noButtonListeners(holder.fabAccept, holder.fabRefuse)
                // TODO - cancel invite
            }
            FriendDto.Status.TO_ACCEPT -> {
                holder.txtOverlayView.visibility = View.VISIBLE
                holder.cslConfirmView.visibility = View.VISIBLE
                holder.txtOverlayView.text = ""
                holder.mView.setOnCreateContextMenuListener(null)
                holder.fabAccept.setOnClickListener {
                            mListener.onListItemFriendInviteResponse(friendDto, true)
                }
                holder.fabRefuse.setOnClickListener {
                    mListener.onListItemFriendInviteResponse(friendDto, false)
                }
            }
        }
    }

    private fun noButtonListeners(btnAccept: FloatingActionButton?,
                                  btnDeny: FloatingActionButton?) {
        btnAccept?.setOnClickListener(null)
        btnDeny?.setOnClickListener(null)
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView),
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener {
        val fabAccept: FloatingActionButton = mView.findViewById(R.id.fab_accept_friend)
        val fabRefuse: FloatingActionButton = mView.findViewById(R.id.fab_refuse_friend)
        val txtContentView: TextView = mView.findViewById(R.id.txt_friend_content)
        val txtOverlayView: TextView = mView.findViewById(R.id.overlay_awaiting_confirm)
        val cslConfirmView: ConstraintLayout = mView.findViewById(R.id.layout_confirm_friend)
        val fabContextMenu: FloatingActionButton = mView.findViewById(R.id.fab_menu)
        var mItem: FriendDto? = null

        init { mView.isLongClickable = false }

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
