package com.lb.fast_scroller_and_recycler_view_fixes

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager

//https://stackoverflow.com/a/33985508/878126
class GridLayoutManagerEx : GridLayoutManager {
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, spanCount: Int) : super(context, spanCount)
    constructor(context: Context, spanCount: Int, orientation: Int, reverseLayout: Boolean) : super(context, spanCount, orientation, reverseLayout)

    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }
}
