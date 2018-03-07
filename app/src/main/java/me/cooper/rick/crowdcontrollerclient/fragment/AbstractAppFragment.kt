package me.cooper.rick.crowdcontrollerclient.fragment

import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout

abstract class AbstractAppFragment: Fragment() {

    abstract fun getTitle(): String
    abstract fun getSwipeView(): SwipeRefreshLayout?

}
