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
    var title: TextView? = null

    @BindView(R.id.subtitle)
    var subtitle: TextView? = null

    @BindView(R.id.hello)
    var hello: Button? = null

    @BindView(R.id.list_of_things)
    var listOfThings: ListView? = null

    @BindView(R.id.footer)
    var footer: TextView? = null

    @BindViews(R.id.title, R.id.subtitle, R.id.hello)
    var headerViews: List<View>? = null

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

    }
}