package me.cooper.rick.crowdcontrollerclient.fragment.listener

import android.support.v4.widget.SwipeRefreshLayout

interface FragmentInteractionListener {

    fun setFragmentProperties(swipeView: SwipeRefreshLayout, title: String)

    fun onSwipe(swipeView: SwipeRefreshLayout?)

}