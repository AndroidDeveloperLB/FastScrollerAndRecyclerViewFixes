package com.lb.fast_scroller_and_recycler_view_fixes

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lb.fast_scroller_and_recycler_view_fixes.databinding.ActivityMainBinding
import com.lb.fast_scroller_and_recycler_view_fixes.databinding.SimpleListItem1Binding
import com.lb.fast_scroller_and_recycler_view_fixes_library.BottomOffsetDecoration
import com.lb.fast_scroller_and_recycler_view_fixes_library.FastScrollerEx
import dev.chrisbanes.insetter.applySystemWindowInsetsToMargin
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(this)).also { setContentView(it.root) }
        setSupportActionBar(findViewById(R.id.toolbar))
        binding.fab.setOnClickListener {
            Toast.makeText(this, "This is just a sample...", Toast.LENGTH_SHORT).show()
        }
        val recyclerView = binding.recyclerView
        val numberOfColumns = 2
        val layoutManager =
                GridLayoutManagerEx(this, numberOfColumns, RecyclerView.VERTICAL, false)
        //Have first item take entire row, to make sure it works well with scrolling (some solutions had issues with this)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                    if (position == 0) layoutManager.spanCount else 1
        }
        recyclerView.setHasFixedSize(false)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemBinding = SimpleListItem1Binding.inflate(LayoutInflater.from(this@MainActivity), parent, false)
                return object : RecyclerView.ViewHolder(itemBinding.itemTextView) {
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val itemBinding = SimpleListItem1Binding.bind(holder.itemView)
                itemBinding.itemTextView.text = position.toString()
                itemBinding.itemTextView.setBackgroundColor(if (position % 2 == 0) 0xffff0000.toInt() else 0xff00ff00.toInt())
            }

            //Have plenty of items, to make sure it fixes issues that the thumb is too small
            override fun getItemCount(): Int = 1000
        }
        val thumbDrawable =
                ContextCompat.getDrawable(this, R.drawable.thumb_drawable) as StateListDrawable
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
        setTransparentNavBar()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            window.navigationBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        binding.appBarLayout.applySystemWindowInsetsToPadding(left = true, right = true, top = true)
        binding.fab.applySystemWindowInsetsToMargin(left = true, right = true, bottom = true)
        binding.recyclerView.applySystemWindowInsetsToPadding(left = true, right = true, bottom = true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var url: String? = null
        when (item.itemId) {
            R.id.menuItem_all_my_apps -> url =
                    "https://play.google.com/store/apps/developer?id=AndroidDeveloperLB"

            R.id.menuItem_all_my_repositories -> url = "https://github.com/AndroidDeveloperLB"
            R.id.menuItem_current_repository_website -> url =
                    "https://github.com/AndroidDeveloperLB/FastScrollerAndRecyclerViewFixes"
        }
        if (url == null)
            return true
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        @Suppress("DEPRECATION")
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        startActivity(intent)
        return true
    }

    companion object {
        fun Activity.setTransparentNavBar() {
            val rootView = findViewById<View>(android.R.id.content)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            } else
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    @Suppress("DEPRECATION")
                    rootView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                }
        }
    }
}
