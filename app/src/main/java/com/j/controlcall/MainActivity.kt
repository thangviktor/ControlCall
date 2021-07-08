package com.j.controlcall

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.M)
@SuppressLint("SimpleDateFormat")
class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE = 1
    }

    private lateinit var spannableUtility: SpannableUtility
    private lateinit var pref: SharedPreferences

    private val sdf = SimpleDateFormat("dd/MM/yyyy")
    private lateinit var calendar: Calendar
    private var startDate = 0L
    private lateinit var simAId: String
    private lateinit var simBId: String
    private lateinit var simA: Sim
    private lateinit var simB: Sim

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        spannableUtility = SpannableUtility(this)

        pref = getSharedPreferences("CallLog", MODE_PRIVATE)
        simAId = pref.getString("SimA", "")!!
        simBId = pref.getString("SimB", "")!!
        startDate = pref.getLong("Date", 0L)

        calendar = Calendar.getInstance()
        if (startDate == 0L) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH))
            startDate = calendar.timeInMillis
        }
        else
            calendar.timeInMillis = startDate

        tvStartTime?.text = spannableUtility.text(SpannableUtility.INDEX,
            R.string.start_from_, sdf.format(startDate),
            colorInt = ContextCompat.getColor(this, R.color.blue))
        tvRemindedTime?.text = "200 phút"
        tvRemindedSpaceTime?.text = "20 phút"

        simA = Sim()
        simB = Sim()

        tvStartTime?.setOnClickListener { setStartTime() }
        tvRemindedTime?.setOnClickListener { setTimesList() }
    }

    private fun setTimesList() {
        val times = arrayListOf(10, 20, 40, 60, 80, 100, 150, 200, 250, 280)
        val dialog = TimeDialog()
            .setTimesList(times)
            .show(supportFragmentManager, "")
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getCallDetails()
                } else {
                    Log.d("PermissionLog", "granted == false")
                    Toast.makeText(this,
                        "Không lấy được lịch sử cuộc gọi do quyền truy cập bị từ chối",
                        Toast.LENGTH_SHORT).show()
                }
                return
            }

            else -> {
                Log.d("PermissionLog", "onRequestPermissionsResult: else")
            }
        }
    }

    //-------------------------------------- Event Functions ---------------------------------------

    private fun setStartTime() {
        DatePickerDialog(this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                startDate = calendar.timeInMillis
                pref.edit().putLong("Date", startDate).apply()

                tvStartTime?.text = spannableUtility.text(SpannableUtility.INDEX,
                    R.string.start_from_, sdf.format(startDate),
                    colorInt = ContextCompat.getColor(this, R.color.blue))
                getCallDetails()
            }, calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DATE])
            .show()
    }

    private fun getCallDetails() {
        simA.incoming = 0
        simA.outgoing = 0
        simB.incoming = 0
        simB.outgoing = 0

        val query = contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, null)
        val type = query?.getColumnIndex(CallLog.Calls.TYPE)
        val date = query?.getColumnIndex(CallLog.Calls.DATE)
        val duration = query?.getColumnIndex(CallLog.Calls.DURATION)
        val phoneAccountId = query?.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

        var hasSimA = false
        var hasSimB = false

        if (query == null)
            return

        while (query.moveToNext()) {
            val callType = query.getString(type?:0)
            val callDate = query.getString(date?:0).toLong()
            val callDuration = query.getString(duration?:0)
            val callPhoneAccountId = query.getString(phoneAccountId?:0)
            val dircode = callType?.toInt()

            if (callPhoneAccountId == simAId)
                hasSimA = true
            if (callPhoneAccountId == simAId)
                hasSimB = true

            if (callDate < startDate)
                continue

            if (simAId.isEmpty()) {
                simAId = callPhoneAccountId
                pref.edit().putString("SimA", simAId).apply()
            } else if (simBId.isEmpty() && callPhoneAccountId != simAId) {
                simBId = callPhoneAccountId
                pref.edit().putString("SimB", simBId).apply()
            }

            if (callPhoneAccountId == simAId) {
                simA.name = "Sim A"
                when (dircode) {
                    CallLog.Calls.OUTGOING_TYPE -> simA.outgoing += callDuration.toLong()
                    CallLog.Calls.INCOMING_TYPE -> simA.incoming += callDuration.toLong()
                    CallLog.Calls.MISSED_TYPE -> {}
                }
            }
            if (callPhoneAccountId == simBId) {
                simB.name = "Sim B"
                when (dircode) {
                    CallLog.Calls.OUTGOING_TYPE -> simB.outgoing += callDuration.toLong()
                    CallLog.Calls.INCOMING_TYPE -> simB.incoming += callDuration.toLong()
                    CallLog.Calls.MISSED_TYPE -> {}
                }
            }
        }
        query.close()

        setCallTimeView(tvIncomingA, simA.incoming)
        setCallTimeView(tvOutgoingA, simA.outgoing)
        setCallTimeView(tvIncomingB, simB.incoming)
        setCallTimeView(tvOutgoingB, simB.outgoing)
    }

    private fun setCallTimeView(textView: TextView?, time: Long) {
        textView?.text = if (time % 60 == 0L)
            " ${time / 60} phút"
        else
            " ${time / 60} phút ${time % 60} giây"
    }

    private fun checkAndRequestPermissions() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG
            ) -> {
                getCallDetails()
            }
            else -> {
                Log.d("PermissionLog", "else")
                requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), REQUEST_CODE)
            }
        }
    }
}