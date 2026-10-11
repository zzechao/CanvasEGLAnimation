package com.base.canvasanimation

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.base.animation.AnimationEx
class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnimationEx.init(this.application, 200, 1, ImageBezierNode::class.java, ImageDouNode::class.java)
        AnimationEx.registerNode(ImageDouNode::class.java)
        val root = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        setContentView(root)
        root.findViewById<android.view.View>(R.id.bt1).setOnClickListener {
            val transactionTooLargeException = supportFragmentManager.beginTransaction()
            transactionTooLargeException.replace(R.id.fl, TestAnimCanvasFragment())
            transactionTooLargeException.addToBackStack("TestAnimCanvasFragment")
            transactionTooLargeException.commitAllowingStateLoss()
        }
        root.findViewById<android.view.View>(R.id.bt2).setOnClickListener {
            val transactionTooLargeException = supportFragmentManager.beginTransaction()
            transactionTooLargeException.replace(R.id.fl, TestAnimCanvasFragment2())
            transactionTooLargeException.addToBackStack("TestAnimCanvasFragment2")
            transactionTooLargeException.commitAllowingStateLoss()
        }
        root.findViewById<android.view.View>(R.id.bt3).setOnClickListener {
            val transactionTooLargeException = supportFragmentManager.beginTransaction()
            transactionTooLargeException.replace(R.id.fl, TestAnimCanvasFragment3())
            transactionTooLargeException.addToBackStack("TestAnimCanvasFragment3")
            transactionTooLargeException.commitAllowingStateLoss()
        }
    }
}