package com.j.controlcall

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.item_time.view.*

class TimeAdapter(private val context: Context, private val times: ArrayList<Int>) : BaseAdapter() {

    override fun getCount(): Int {
        return times.size
    }

    override fun getItem(position: Int): Any {
        return times[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_time, null)

        view.tvTime?.text = "${times[position]} phút"

        return view
    }
}