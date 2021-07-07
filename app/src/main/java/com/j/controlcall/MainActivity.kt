package com.j.controlcall

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
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
        val datePickerDialog = DatePickerDialog(this,
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

        val managedCursor = managedQuery(CallLog.Calls.CONTENT_URI, null, null, null, null)
        val type = managedCursor.getColumnIndex(CallLog.Calls.TYPE)
        val date = managedCursor.getColumnIndex(CallLog.Calls.DATE)
        val duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION)
        val phoneAccountId = managedCursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

        var hasSimA = false
        var hasSimB = false

        while (managedCursor.moveToNext()) {
            val callType = managedCursor.getString(type)
            val callDate = managedCursor.getString(date).toLong()
            val callDuration = managedCursor.getString(duration)
            val callPhoneAccountId = managedCursor.getString(phoneAccountId)
            var dir: String? = null
            val dircode = callType.toInt()

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
        managedCursor.close()

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