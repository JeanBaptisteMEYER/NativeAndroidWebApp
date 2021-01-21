package com.jbm.nativewebapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jbm.nativewebapp.ui.WebFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction().replace(R.id.content, WebFragment()).commit()
    }
}