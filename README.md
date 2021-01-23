# FastScrollerAndRecyclerViewFixes
A collection of fixes for FastScroller and RecyclerView, based on:
- https://stackoverflow.com/q/47846873/878126
- https://stackoverflow.com/a/58652264/878126
- https://androidx.de/androidx/car/widget/itemdecorators/BottomOffsetDecoration.html
- https://androidx.de/androidx/car/util/GridLayoutManagerUtils.html

What it fixes:

1. When there are many items in RecyclerView, and you use a fast-scroller, the thumb (what you drag) can become extremely small, making it hard to touch.
2. Often when you have a RecyclerView, you want to have some space at the end for FAB and other Views. There are some tricks in doing it (written here about them, including disadvantages), but what I got works very well compared to the other solutions, and doesn't require much to write

Sample usage, of using both fast-scroller

    val thumbDrawable = ContextCompat.getDrawable(this, R.drawable.thumb_drawable) as StateListDrawable
    val lineDrawable = ContextCompat.getDrawable(this, R.drawable.line_drawable)!!
    val thickness = resources.getDimensionPixelSize(R.dimen.fastScrollThickness)
    val minRange = resources.getDimensionPixelSize(R.dimen.fastScrollMinimumRange)
    val margin = resources.getDimensionPixelSize(R.dimen.fastScrollMargin)
    val minThumbSize = resources.getDimensionPixelSize(R.dimen.fastScrollMinThumbSize)
    FastScrollerEx(recyclerView, thumbDrawable, lineDrawable,
            thumbDrawable, lineDrawable, thickness, minRange, margin, true, minThumbSize)
    recyclerView.addItemDecoration(BottomOffsetDecoration(
            resources.getDimensionPixelSize(R.dimen.bottom_list_padding), BottomOffsetDecoration.LayoutManagerType.GRID_LAYOUT_MANAGER)
    )
    
 
