package me.cooper.rick.crowdcontrollerclient.fragment.friend

import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_list_item.view.*
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto.Status
import me.cooper.rick.crowdcontrollerapi.dto.user.FriendDto.Status.*
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity
import me.cooper.rick.crowdcontrollerclient.fragment.friend.FriendFragment.OnFriendFragmentInteractionListener

class FriendRecyclerViewAdapter(private val friends: List<FriendDto>,
                                private val listener: OnFriendFragmentInteractionListener) :
        RecyclerView.Adapter<FriendRecyclerViewAdapter.ViewHolder>() {

    lateinit var parent: ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        this.parent = parent
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.friendDto = friends[position]
        holder.vwRoot.txt_content.text = friends[position].username
        setStatusView(holder)
    }

    override fun getItemCount(): Int = friends.size

    private fun setStatusView(holder: ViewHolder) {
        val friendDto = holder.friendDto
        when (friendDto.status) {
            CONFIRMED -> setView(holder, GONE, GONE, GONE, holder,
                    { holder.vwRoot.showContextMenu() })
            AWAITING_ACCEPT -> setView(holder, VISIBLE, VISIBLE, GONE,
                    null, null,
                        parent.context.getString(R.string.txt_cancel_friend_request),
                        { statusUpdate(friendDto, INACTIVE) })
            TO_ACCEPT -> setView(holder, VISIBLE, VISIBLE, VISIBLE,
                    null, null,
                        parent.context.getString(R.string.txt_accept_friend_request),
                        { statusUpdate(friendDto, INACTIVE) },
                        { statusUpdate(friendDto, CONFIRMED) })
            INACTIVE -> setView(holder, GONE, GONE, GONE,
                    null, null)
        }
    }

    private fun statusUpdate(friendDto: FriendDto, status: Status) {
        listener.onListItemFriendUpdate(friendDto.copy(status = status))
    }

    private fun setView(holder: ViewHolder, overlayVisibility: Int, actionPanelVisibility: Int,
                        fabAcceptVisiblity: Int, contextMenuListener: View.OnCreateContextMenuListener?,
                        menuOnClickListener: (() -> Unit)?, action: String? = "",
                        refuseListener: (() -> Unit)? = null, acceptListener: (() -> Unit)? = null) {
        val vwRoot = holder.vwRoot
        vwRoot.vw_overlay.visibility = overlayVisibility
        vwRoot.layout_action.visibility = actionPanelVisibility
        vwRoot.fab_accept_invite.visibility = fabAcceptVisiblity

        vwRoot.setOnCreateContextMenuListener(contextMenuListener)
        vwRoot.fab_menu.setOnClickListener { menuOnClickListener?.let { menuOnClickListener() } }
        vwRoot.isLongClickable = false

        vwRoot.txt_action.text = action

        vwRoot.fab_accept_invite.setOnClickListener { acceptListener?.let { acceptListener() } }
        vwRoot.fab_refuse_invite.setOnClickListener { refuseListener?.let { refuseListener() } }
    }

    inner class ViewHolder(val vwRoot: View) : RecyclerView.ViewHolder(vwRoot),
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener {

        lateinit var friendDto: FriendDto

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            (listener as AppActivity).menuInflater.inflate(R.menu.menu_context_friend, menu)

            menu!!.setHeaderView(LayoutInflater.from(parent.context)
                    .inflate(R.layout.context_header, parent, false)
                    .apply { (findViewById<TextView>(R.id.txt_header)).text = friendDto.username })

            (0 until menu.size()).forEach { menu.getItem(it).setOnMenuItemClickListener(this) }
            menu.findItem(R.id.action_add_to_group).isVisible = friendDto.canJoinGroup()
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            listener.onListItemContextMenuSelection(friendDto, item!!)
            return true
        }

        override fun toString(): String {
            return "${super.toString()} '${vwRoot.txt_content.text}'"
        }

    }

}
