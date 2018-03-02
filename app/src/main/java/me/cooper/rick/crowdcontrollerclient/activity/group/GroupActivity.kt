package me.cooper.rick.crowdcontrollerclient.activity.group

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_group.*
import kotlinx.android.synthetic.main.app_bar_group.*
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.group.dummy.DummyContent

class GroupActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        GroupFragment.OnListFragmentInteractionListener {

    private lateinit var groupFragment: GroupFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group)
        setSupportActionBar(toolbar)
        val friendId = intent.getLongExtra("friendId", -1)
        if (friendId != -1L) throw RuntimeException("Intent was not received")

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        groupFragment = GroupFragment()
//        val friends = me.cooper.rick.crowdcontrollerclient.activity.friend.dummy.DummyContent.FRIENDS
        btnAdd.setOnClickListener {
//            val rand = Random()
//            val index = rand.nextInt(friends.size)
//            val friend = friends[index]
//            DummyContent.ITEMS.add(DummyContent.DummyItem(index.toString(), friend.content, ""))
//            groupFragment?.updateView()
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.groupFragmentLayout, groupFragment)
                .commit()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.group, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_location -> {
                val intent = Intent(this, LocationActivity::class.java)
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onListFragmentInteraction(item: DummyContent.DummyItem) {
        // TODO
    }
}
