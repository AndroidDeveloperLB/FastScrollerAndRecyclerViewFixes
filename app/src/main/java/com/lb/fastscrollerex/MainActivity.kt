package com.lb.fastscrollerex

import android.content.Intent
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lb.fast_scroller_library.BottomOffsetDecoration
import com.lb.fast_scroller_library.FastScroller2
import com.lb.fast_scroller_library.GridLayoutManagerEx

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val bestNumberOfColumns = 2
        val layoutManager = GridLayoutManagerEx(this, bestNumberOfColumns, RecyclerView.VERTICAL, false)
        //Have first item take entire row, to make sure it works well with scrolling (some solutions had issues with this)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = if (position == 0) layoutManager.spanCount else 1
        }
        recyclerView.setHasFixedSize(false)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(this@MainActivity).inflate(android.R.layout.simple_list_item_1, parent, false)
                view.findViewById<TextView>(android.R.id.text1).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40f)
                return object : RecyclerView.ViewHolder(view) {
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                holder.itemView.findViewById<TextView>(android.R.id.text1).text = position.toString()
                holder.itemView.setBackgroundColor(if (position % 2 == 0) 0xffff0000.toInt() else 0xff00ff00.toInt())
            }

            //Have plenty of items, to make sure it fixes issues that the thumb is too small
            override fun getItemCount(): Int = 1000
        }
        val thumbDrawable = ContextCompat.getDrawable(this, R.drawable.thumb_drawable) as StateListDrawable
        val lineDrawable = ContextCompat.getDrawable(this, R.drawable.line_drawable)!!
        val thickness = resources.getDimensionPixelSize(R.dimen.fastScrollThickness)
        val minRange = resources.getDimensionPixelSize(R.dimen.fastScrollMinimumRange)
        val margin = resources.getDimensionPixelSize(R.dimen.fastScrollMargin)
        val minThumbSize = resources.getDimensionPixelSize(R.dimen.fastScrollMinThumbSize)
        FastScroller2(recyclerView, thumbDrawable, lineDrawable,
                thumbDrawable, lineDrawable, thickness, minRange, margin, false, minThumbSize)
        recyclerView.addItemDecoration(BottomOffsetDecoration(resources.getDimensionPixelSize(R.dimen.bottom_list_padding),
                BottomOffsetDecoration.LayoutManagerType.GRID_LAYOUT_MANAGER))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var url: String? = null
        when (item.itemId) {
            R.id.menuItem_all_my_apps -> url = "https://play.google.com/store/apps/developer?id=AndroidDeveloperLB"
            R.id.menuItem_all_my_repositories -> url = "https://github.com/AndroidDeveloperLB"
            R.id.menuItem_current_repository_website -> url = "https://github.com/AndroidDeveloperLB/FastScrollerEx"
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
}
