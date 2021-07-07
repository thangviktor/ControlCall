package com.j.controlcall

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log

class SpannableUtility(private val context: Context) {
    private var color: Int?= Color.BLACK


    companion object {
        const val TAG = "ErrorSpanLog"
        const val INDEX = "INDEX"
        const val COLON = "COLON"
    }

    fun text(style: String, resourceString: Int, specText: String, startIndex: Int ?=0,
             colorInt: Int?= Color.BLACK, colorResource: Int ?= 0): SpannableString {
        Log.d("SpanLog", "colorResource: $colorResource")
        color = if (colorResource != 0)
            Color.parseColor(context.resources.getString(colorResource!!))
        else
            colorInt

        return when (style) {
            INDEX -> textIndex(resourceString, specText, startIndex)
            COLON -> textColon(resourceString, specText)
            else -> textIndex(resourceString, specText, startIndex)
        }
    }

    fun text(style: String, normalText: String, specText: String, startIndex: Int ?=0,
             colorInt: Int?= Color.BLACK, colorResource: Int ?= 0): SpannableString {
        color = if (colorResource != 0)
            Color.parseColor(context.resources.getString(colorResource!!))
        else
            colorInt

        return when (style) {
            INDEX -> textIndex(normalText, specText, startIndex)
            COLON -> textColon(normalText, specText)
            else -> textIndex(normalText, specText, startIndex)
        }
    }

    private fun textIndex(resourceString: Int, specText: String, startIndex: Int?): SpannableString {
        val normalText = context.resources.getString(resourceString)

        return textIndex(normalText, specText, startIndex)
    }

    private fun textColon(resourceString: Int, specText: String): SpannableString {
        val normalText = context.resources.getString(resourceString)

        return textColon(normalText, specText)
    }

    private fun textColon(normalText: String, specText: String): SpannableString {
        val texts: List<String>
        if (normalText.contains(":")) {
            texts = normalText.split(":")
            val newText = texts[0] + ": $specText" + texts[1]
            val spannable = SpannableString(newText)
            spannable.setSpan(
                ForegroundColorSpan(color!!),
                texts[0].length+1, texts[0].length + 2 + specText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spannable
        }

        Log.d(TAG, "String is not contains colon")
        return SpannableString(normalText)
    }

    private fun textIndex(normalText: String, specText: String, startIndex: Int ?= 0): SpannableString {
        val newStart =
            if (startIndex != 0) startIndex
            else normalText.length
        val newText = normalText.substring(0, newStart!!) + " $specText" + normalText.substring(newStart)
        val spannable = SpannableString(newText)
        spannable.setSpan(
            ForegroundColorSpan(color!!),
            normalText.length, normalText.length + 1 + specText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }
}