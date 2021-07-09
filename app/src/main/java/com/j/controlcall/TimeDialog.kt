package com.j.controlcall

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_time.*

class TimeDialog : DialogFragment() {

    interface TimeListener {
        fun itemClick()
    }

    private lateinit var adapter: TimeAdapter
    private val times = ArrayList<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.dialog_time, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TimeAdapter(requireContext(), times)
        lvTime?.adapter = adapter

//        val c: View? = lvTime?.getChildAt(0)
//        val scrolly: Int = -c!!.top + lvTime.firstVisiblePosition * c.height
//        Log.d("DialogLog", "scrolly = $scrolly")
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun setTimesList(timesList: ArrayList<Int>): TimeDialog {
        times.clear()
        times.addAll(timesList)
        return this
    }
}