package me.cooper.rick.crowdcontrollerclient.fragment.group

import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.View.*
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_list_item.view.*
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupMemberDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment.OnGroupFragmentInteractionListener

class GroupRecyclerViewAdapter(private var privateGroup: GroupDto,
                               private val mListener: OnGroupFragmentInteractionListener) :
        RecyclerView.Adapter<GroupRecyclerViewAdapter.ViewHolder>() {

    lateinit var me: String

    var group: GroupDto
        get() = privateGroup
        set(group) {
            privateGroup = group.copy(members = group.members.sortedWith(
                    compareBy({ it.id != group.adminId },
                            { it.id != mListener.userId() },
                            { it.username }
                    ))
            )
            notifyDataSetChanged()
        }

    lateinit var parent: ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        this.parent = parent
        me = parent.context.getString(R.string.txt_me)
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userId = mListener.userId()
        holder.groupMemberDto = privateGroup.members[position]
        holder.vwRoot.txt_content.text = privateGroup.members[position].username
        setStatusView(holder, holder.groupMemberDto, userId)
    }

    private fun setStatusView(holder: ViewHolder, groupMember: GroupMemberDto, userId: Long) {

        if (groupMember.id == group.adminId) {
            setReadOnly(holder)
            holder.vwRoot.leader_icon.visibility = VISIBLE
        } else {
            holder.vwRoot.leader_icon.visibility = GONE
            when {
                userId != group.adminId -> setReadOnly(holder)
                groupMember.groupAccepted -> setView(holder, GONE, GONE, VISIBLE, holder,
                        { holder.vwRoot.showContextMenu() })
                else ->
                    setView(holder, VISIBLE, VISIBLE, INVISIBLE,
                            null, null,
                            parent.context.getString(R.string.txt_cancel_group_invite),
                            { mListener.onInviteCancellation(groupMember) })
            }
        }
//        when {
//            group.adminId == groupMember.id -> {
//                if (groupMember.id == userId) holder.vwRoot.txt_content.text = me
//                setReadOnly(holder)
//                holder.vwRoot.leader_icon.visibility = VISIBLE
//            }
//            groupMember.id == userId -> {
//                holder.vwRoot.txt_content.text = me
//                setReadOnly(holder)
//            }
//            groupMember.groupAccepted -> setView(holder, GONE, GONE, VISIBLE, holder,
//                    { holder.vwRoot.showContextMenu() })
//            else -> {
//                setView(holder, VISIBLE, VISIBLE, GONE,
//                        null, null,
//                        parent.context.getString(R.string.txt_cancel_group_invite),
//                        { mListener.onInviteCancellation(groupMember) })
//            }
//        }
//    }

//    private fun groupAdminView(holder: ViewHolder, groupMember: GroupMemberDto, userId: Long) {
//        when (groupMember.id) {
//            userId ->
//        }
    }

    private fun setReadOnly(holder: ViewHolder) {
        setView(holder, GONE, GONE, INVISIBLE, null, null)
        holder.vwRoot.fab_menu.visibility = INVISIBLE
    }

    private fun setView(holder: ViewHolder, overlayVisibility: Int, actionPanelVisibility: Int,
                        fabMenuVisibility: Int,
                        contextMenuListener: View.OnCreateContextMenuListener?,
                        menuOnClickListener: (() -> Unit)?, action: String? = "",
                        refuseListener: (() -> Unit)? = null) {
        val vwRoot = holder.vwRoot
        vwRoot.vw_overlay.visibility = overlayVisibility
        vwRoot.layout_action.visibility = actionPanelVisibility
        vwRoot.fab_accept_invite.visibility = GONE

        vwRoot.fab_menu.visibility = fabMenuVisibility

        vwRoot.setOnCreateContextMenuListener(contextMenuListener)
        vwRoot.fab_menu.setOnClickListener { menuOnClickListener?.let { menuOnClickListener() } }
        vwRoot.isLongClickable = false

        vwRoot.txt_action.text = action

        vwRoot.fab_refuse_invite.setOnClickListener { refuseListener?.let { refuseListener() } }

        vwRoot.leader_icon.visibility = GONE
    }

    override fun getItemCount(): Int = privateGroup.members.size

    inner class ViewHolder(val vwRoot: View) : RecyclerView.ViewHolder(vwRoot),
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener {

        lateinit var groupMemberDto: GroupMemberDto

        init {
            vwRoot.fab_menu.visibility = if (mListener.userId() == group.adminId) VISIBLE else GONE
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            (mListener as AppActivity).menuInflater.inflate(R.menu.menu_context_group, menu)

            menu!!.setHeaderView(LayoutInflater.from(parent.context)
                    .inflate(R.layout.context_header, parent, false)
                    .apply { (findViewById<TextView>(R.id.txt_header)).text = groupMemberDto.username })

            (0 until menu.size()).forEach { menu.getItem(it).setOnMenuItemClickListener(this) }
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            mListener.onListItemContextMenuSelection(groupMemberDto, item!!)
            return true
        }

        override fun toString(): String {
            return "${super.toString()} '${vwRoot.txt_content.text}'"
        }
    }

}
