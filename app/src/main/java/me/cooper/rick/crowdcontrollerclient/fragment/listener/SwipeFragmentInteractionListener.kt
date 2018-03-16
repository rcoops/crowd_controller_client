package me.cooper.rick.crowdcontrollerclient.fragment.listener

import android.support.v4.widget.SwipeRefreshLayout
import me.cooper.rick.crowdcontrollerclient.fragment.AbstractAppFragment

interface SwipeFragmentInteractionListener {

    fun onSwipe(swipeView: SwipeRefreshLayout)
    fun pushView(swipeView: SwipeRefreshLayout)
    fun popView(swipeView: SwipeRefreshLayout)

}
