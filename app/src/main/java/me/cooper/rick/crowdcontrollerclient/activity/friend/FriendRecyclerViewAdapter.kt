package me.cooper.rick.crowdcontrollerclient.activity.friend

import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.FrameLayout
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
        when (friendDto.status) {
            FriendDto.Status.AWAITING_ACCEPT -> {
                holder.mOverlayView.visibility = View.VISIBLE
            }
            FriendDto.Status.TO_ACCEPT -> {
                holder.mConfirmView.visibility = View.VISIBLE
            }
            FriendDto.Status.ACTIVATED -> {
            }
        }
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
        var mItem: FriendDto? = null

        init {
            mView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            (mListener as AppActivity).menuInflater.inflate(R.menu.menu_context_friend, menu)

            menu!!.setHeaderView(LayoutInflater.from(parent.context)
                    .inflate(R.layout.context_header, parent, false)
                    .apply { (findViewById<TextView>(R.id.txt_header)).text = mItem!!.username })

            (0 until menu.size()).forEach { menu.getItem(it).setOnMenuItemClickListener(this) }
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            mListener.onListFragmentInteraction(mItem!!, item!!)
            return true
        }

        override fun toString(): String {
            return "${super.toString()} '${mContentView.text}'"
        }
    }
}
