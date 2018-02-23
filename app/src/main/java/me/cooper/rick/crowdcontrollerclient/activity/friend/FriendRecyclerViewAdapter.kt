package me.cooper.rick.crowdcontrollerclient.activity.friend

import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.AdapterView
import android.widget.TextView
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.friend.FriendFragment.OnListFragmentInteractionListener

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class FriendRecyclerViewAdapter(
        private val mValues: List<FriendDto>,
        private val mListener: OnListFragmentInteractionListener
): RecyclerView.Adapter<FriendRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_friend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mContentView.text = mValues[position].username

        holder.mView.setOnClickListener {
            mListener.onListFragmentInteraction(holder.mItem!!)
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView),
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener {
        val mContentView: TextView = mView.findViewById(R.id.content)
        val btnContextMenu: FloatingActionButton = mView.findViewById(R.id.btnContextMenu)
        var mItem: FriendDto? = null

        init {
            btnContextMenu.setOnClickListener {
                mListener.onListFragmentInteraction(mItem!!)
            }
            mView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
//            val info = menuInfo as AdapterView.AdapterContextMenuInfo
            menu!!.setHeaderTitle(mItem!!.username)
            val menuItemNames = App.context!!.resources.getStringArray(R.array.menu_context_friend)
            menuItemNames.forEachIndexed { i, menuItemName ->
                val menuItem = menu.add(Menu.NONE, i, i, menuItemName)
                menuItem.setOnMenuItemClickListener(this)
            }
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            mListener.onListFragmentInteraction(mItem!!)
            return true
        }

        override fun toString(): String {
            return "${super.toString()} '${mContentView.text}'"
        }
    }
}
