package com.lb.fast_scroller_and_recycler_view_fixes_library

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

//https://androidx.de/androidx/car/util/GridLayoutManagerUtils.html
/**
 * Utility class that helps navigating in GridLayoutManager.
 *
 *
 * Assumes parameter `RecyclerView` uses [GridLayoutManager].
 *
 *
 * Assumes the orientation of `GridLayoutManager` is vertical.
 */
object GridLayoutManagerUtils {
    /**
     * Returns whether or not the given view is on the last row of a `RecyclerView` with a
     * [GridLayoutManager].
     *
     * @param view   The view to inspect.
     * @param parent [RecyclerView] that contains the given view.
     * @return `true` if the given view is on the last row of the `RecyclerView`.
     */
    fun isOnLastRow(view: View, parent: RecyclerView): Boolean {
        return getLastItemPositionOnSameRow(view, parent) == parent.adapter!!.itemCount - 1
    }

    /**
     * Returns the position of the last item that is on the same row as input `view`.
     *
     * @param view   The view to inspect.
     * @param parent [RecyclerView] that contains the given view.
     */
    private fun getLastItemPositionOnSameRow(view: View, parent: RecyclerView): Int {
        val layoutManager = parent.layoutManager as GridLayoutManager
        val spanSizeLookup = layoutManager.spanSizeLookup
        val spanCount = layoutManager.spanCount
        val lastItemPosition = parent.adapter!!.itemCount - 1
        var currentChildPosition = parent.getChildAdapterPosition(view)
        val itemSpanIndex = (view.layoutParams as GridLayoutManager.LayoutParams).spanIndex
        var spanSum = itemSpanIndex + spanSizeLookup.getSpanSize(currentChildPosition)
        // Iterate to the end of the row starting from the current child position.
        while (currentChildPosition <= lastItemPosition && spanSum <= spanCount) {
            spanSum += spanSizeLookup.getSpanSize(currentChildPosition + 1)
            if (spanSum > spanCount)
                return currentChildPosition
            ++currentChildPosition
        }
        return lastItemPosition
    }
}
