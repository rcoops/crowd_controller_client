package me.cooper.rick.crowdcontrollerclient.fragment.group

import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_list_item.view.*
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity
import me.cooper.rick.crowdcontrollerclient.fragment.group.GroupFragment.OnGroupFragmentInteractionListener

class GroupRecyclerViewAdapter(private val mValues: List<UserDto>,
                               private val mListener: OnGroupFragmentInteractionListener) :
        RecyclerView.Adapter<GroupRecyclerViewAdapter.ViewHolder>() {

    lateinit var parent: ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        this.parent = parent
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mView.txt_content.text = mValues[position].username

        setStatusView(holder, holder.mItem!!)
    }

    private fun setStatusView(holder: ViewHolder, user: UserDto) {

    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView),
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener {
        var mItem: UserDto? = null

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
            return "${super.toString()} '${mView.txt_content.text}'"
        }
    }

}
