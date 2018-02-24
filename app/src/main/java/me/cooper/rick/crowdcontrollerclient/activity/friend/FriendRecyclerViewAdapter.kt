package me.cooper.rick.crowdcontrollerclient.activity.friend

import android.content.Context
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.AdapterView
import android.widget.TextView
import me.cooper.rick.crowdcontrollerapi.dto.FriendDto
import me.cooper.rick.crowdcontrollerclient.App
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
        private val mListener: OnListFragmentInteractionListener
): RecyclerView.Adapter<FriendRecyclerViewAdapter.ViewHolder>() {

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
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView),
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener {
        val mContentView: TextView = mView.findViewById(R.id.content)
        var mItem: FriendDto? = null

        init {
            mView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            (mListener as AppActivity).menuInflater.inflate(R.menu.menu_context_friend, menu)

            menu!!.setHeaderView(LayoutInflater.from(parent.context)
                    .inflate(R.layout.context_header, parent, false)
                    .apply { (findViewById<TextView>(R.id.txt_header)).text = mItem!!.username })

            for (i in 0 until menu.size()) menu.getItem(i).setOnMenuItemClickListener(this)
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
