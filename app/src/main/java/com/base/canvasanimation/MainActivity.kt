package com.base.canvasanimation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.base.animation.AnimationEx


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnimationEx.init(this.application, 200, 2, ImageBezierNode::class.java, ImageDouNode::class.java)
        AnimationEx.registerNode(ImageDouNode::class.java)

        setContentView(R.layout.activity_main)
        findViewById<android.view.View>(R.id.bt1).setOnClickListener {
            val transactionTooLargeException = supportFragmentManager.beginTransaction()
            transactionTooLargeException.replace(R.id.fl, TestAnimCanvasFragment())
            transactionTooLargeException.addToBackStack("TestAnimCanvasFragment")
            transactionTooLargeException.commitAllowingStateLoss()
        }
        findViewById<android.view.View>(R.id.bt2).setOnClickListener {
            val transactionTooLargeException = supportFragmentManager.beginTransaction()
            transactionTooLargeException.replace(R.id.fl, TestAnimCanvasFragment2())
            transactionTooLargeException.addToBackStack("TestAnimCanvasFragment2")
            transactionTooLargeException.commitAllowingStateLoss()
        }
        findViewById<android.view.View>(R.id.bt3).setOnClickListener {
            val transactionTooLargeException = supportFragmentManager.beginTransaction()
            transactionTooLargeException.replace(R.id.fl, TestAnimCanvasFragment3())
            transactionTooLargeException.addToBackStack("TestAnimCanvasFragment3")
            transactionTooLargeException.commitAllowingStateLoss()
        }
    }
}