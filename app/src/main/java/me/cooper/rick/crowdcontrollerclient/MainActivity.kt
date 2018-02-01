package me.cooper.rick.crowdcontrollerclient

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val text = intent.getStringExtra("message")
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}
