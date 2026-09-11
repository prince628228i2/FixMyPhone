package com.fixmyphone

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this)
        text.text = "Fix My Phone\nNative Launch OK"
        text.textSize = 28f
        text.setTextColor(Color.WHITE)
        text.gravity = Gravity.CENTER
        text.setBackgroundColor(Color.rgb(7,16,24))
        setContentView(text)
    }
}
