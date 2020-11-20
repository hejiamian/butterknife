package com.butterknife.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.BindViews
import butterknife.OnClick
import butterknife.OnLongClick

class MainActivity : AppCompatActivity() {
    @BindView(R.id.title)
    lateinit var title: TextView

    @BindView(R.id.subtitle)
    lateinit var subtitle: TextView

    @BindView(R.id.hello)
    lateinit var hello: Button

    @BindView(R.id.list_of_things)
    lateinit var listOfThings: ListView

    @BindView(R.id.footer)
    lateinit var footer: TextView

    @BindViews(R.id.title, R.id.subtitle, R.id.hello)
    lateinit var headerViews: List<View>

    @OnClick(R.id.hello)
    fun sayHello() {
        Toast.makeText(this, "Hello, views!", Toast.LENGTH_SHORT).show()
    }

    @OnLongClick(R.id.hello)
    fun sayGetOffMe(): Boolean {
        Toast.makeText(this, "Let go of me!", Toast.LENGTH_SHORT).show()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MainActivity_ViewBinding(this)
        subtitle.text = "MainActivity_ViewBinding"
    }
}