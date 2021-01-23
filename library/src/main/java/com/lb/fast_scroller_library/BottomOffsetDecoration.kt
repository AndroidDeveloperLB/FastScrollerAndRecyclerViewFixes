package com.lb.fast_scroller_library

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

//https://androidx.de/androidx/car/widget/itemdecorators/BottomOffsetDecoration.html
class BottomOffsetDecoration(private val mBottomOffset: Int, private val layoutManagerType: LayoutManagerType) : ItemDecoration() {
    enum class LayoutManagerType {
        GRID_LAYOUT_MANAGER, LINEAR_LAYOUT_MANAGER
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        when (layoutManagerType) {
            LayoutManagerType.LINEAR_LAYOUT_MANAGER -> {
                val position = parent.getChildAdapterPosition(view)
                outRect.bottom =
                        if (state.itemCount <= 0 || position != state.itemCount - 1)
                            0
                        else
                            mBottomOffset
            }
            LayoutManagerType.GRID_LAYOUT_MANAGER -> {
                val adapter = parent.adapter
                outRect.bottom =
                        if (adapter == null || adapter.itemCount == 0 || !GridLayoutManagerUtils.isOnLastRow(view, parent))
                            0
                        else
                            mBottomOffset
            }
        }
    }
}

