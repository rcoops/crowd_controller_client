package me.cooper.rick.crowdcontrollerclient.activity.friend

import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity
import me.cooper.rick.crowdcontrollerclient.activity.friend.FriendFragment.OnListFragmentInteractionListener

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class FriendRecyclerViewAdapter(
        private val mValues: List<FriendDto>,
        private val mListener: OnListFragmentInteractionListener) :
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
        holder.mContentView.text = mValues[position].username
        setStatusView(holder, holder.mItem!!)
    }

    private fun setStatusView(holder: ViewHolder, friendDto: FriendDto) {
        val btnAccept = holder.mView.findViewById<FloatingActionButton>(R.id.btn_accept_friend)
        val btnDeny = holder.mView.findViewById<FloatingActionButton>(R.id.btn_deny_friend)

        when (friendDto.status) {
            FriendDto.Status.ACTIVATED -> {//setHolderStatus(holder, friendDto, false, false)
                holder.mOverlayView.visibility = View.GONE
                holder.mConfirmView.visibility = View.GONE
                holder.mOverlayView.text = ""
                holder.mView.setOnCreateContextMenuListener(holder as View.OnCreateContextMenuListener)
                holder.mView.isLongClickable = false
                holder.mMainMenu.setOnClickListener { holder.mView.showContextMenu() }
                noButtonListeners(btnAccept, btnDeny)
            }
            FriendDto.Status.AWAITING_ACCEPT -> {//setHolderStatus(holder, friendDto, true, false)
                holder.mOverlayView.visibility = View.VISIBLE
                holder.mConfirmView.visibility = View.GONE
                holder.mOverlayView.text = (mListener as AppActivity).getString(R.string.txt_awaiting_friend_accept)
                holder.mView.setOnCreateContextMenuListener(null)
                noButtonListeners(btnAccept, btnDeny)
            }
            FriendDto.Status.TO_ACCEPT -> {//setHolderStatus(holder, friendDto, true, true)
                holder.mOverlayView.visibility = View.VISIBLE
                holder.mConfirmView.visibility = View.VISIBLE
                holder.mOverlayView.text = ""
                holder.mView.setOnCreateContextMenuListener(null)
                btnAccept?.setOnClickListener {
                            mListener.onListItemFriendInviteResponse(friendDto, true)
                }
                btnDeny?.setOnClickListener {
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

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView),
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener {
        val mContentView: TextView = mView.findViewById(R.id.txt_friend_content)
        val mOverlayView: TextView = mView.findViewById(R.id.overlay_awaiting_confirm)
        val mConfirmView: ConstraintLayout = mView.findViewById(R.id.layout_confirm_friend)
        val mMainMenu: FloatingActionButton = mView.findViewById(R.id.fab_menu)
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
            return "${super.toString()} '${mContentView.text}'"
        }
    }
}
