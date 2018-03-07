package me.cooper.rick.crowdcontrollerclient.fragment.listener

import android.support.v4.widget.SwipeRefreshLayout
import me.cooper.rick.crowdcontrollerclient.fragment.AbstractAppFragment

interface FragmentInteractionListener {

    fun setFragmentProperties(fragment: AbstractAppFragment)

    fun onSwipe(swipeView: SwipeRefreshLayout?)

}