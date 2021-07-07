package com.j.controlcall

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
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

    private lateinit var currentMonth: String
    private lateinit var simAId: String
    private lateinit var simBId: String
    private lateinit var simA: Sim
    private lateinit var simB: Sim

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        spannableUtility = SpannableUtility(this)

        val currentDate = Calendar.getInstance().time
        currentMonth = SimpleDateFormat("MM/yyyy").format(currentDate)
        tvTitle?.text = spannableUtility.text(SpannableUtility.INDEX,
            R.string.total_call_time_in_this_month, currentMonth,
            colorInt = ContextCompat.getColor(this, R.color.blue))

        pref = getSharedPreferences("CallLog", MODE_PRIVATE)

        simAId = pref.getString("SimA", "")!!
        simBId = pref.getString("SimB", "")!!
        simA = Sim()
        simB = Sim()

        checkAndRequestPermissions()
        getCallDetails()
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

    private fun getCallDetails() {

        val managedCursor = managedQuery(CallLog.Calls.CONTENT_URI, null, null, null, null)
        val number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER)
        val type = managedCursor.getColumnIndex(CallLog.Calls.TYPE)
        val date = managedCursor.getColumnIndex(CallLog.Calls.DATE)
        val duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION)
        val phoneAccountId = managedCursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

        var hasSimA = false
        var hasSimB = false

        while (managedCursor.moveToNext()) {
            val phNumber = managedCursor.getString(number)
            val callType = managedCursor.getString(type)
            val callDate = managedCursor.getString(date)
            val callMonth = SimpleDateFormat("MM/yyyy").format(callDate.toLong())
            val callDuration = managedCursor.getString(duration)
            val callPhoneAccountId = managedCursor.getString(phoneAccountId)
            var dir: String? = null
            val dircode = callType.toInt()

            if (callPhoneAccountId == simAId)
                hasSimA = true
            if (callPhoneAccountId == simAId)
                hasSimB = true

            if (callMonth != currentMonth)
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

        tvIncomingA?.text = spannableUtility.text(SpannableUtility.COLON, R.string.incoming, simA.incoming.toString())
        tvOutgoingA?.text = spannableUtility.text(SpannableUtility.COLON, R.string.outgoing, simA.outgoing.toString())
        tvIncomingB?.text = spannableUtility.text(SpannableUtility.COLON, R.string.incoming, simB.incoming.toString())
        tvOutgoingB?.text = spannableUtility.text(SpannableUtility.COLON, R.string.outgoing, simB.outgoing.toString())
    }

    private fun checkAndRequestPermissions() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG
            ) -> {
                Log.d("PermissionLog", "PERMISSION_GRANTED")
            }
            else -> {
                Log.d("PermissionLog", "else")
                requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), REQUEST_CODE)
            }
        }
    }
}