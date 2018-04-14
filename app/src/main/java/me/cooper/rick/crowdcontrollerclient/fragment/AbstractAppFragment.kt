package me.cooper.rick.crowdcontrollerclient.fragment

import android.content.Context
import android.support.v4.app.Fragment

abstract class AbstractAppFragment: Fragment() {

    var fragmentListener: FragmentListenerInterface? = null

    abstract fun getTitle(): String

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        fragmentListener = (context as? FragmentListenerInterface) ?:
                throw RuntimeException(context!!.toString() + " must implement FragmentListenerInterface")
    }

    interface FragmentListenerInterface {
        fun playClick()
    }

}
