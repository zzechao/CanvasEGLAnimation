package com.base.canvasanimation

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.base.animation.AnimationEx
import kotlinx.android.synthetic.main.activity_main.bt1
import kotlinx.android.synthetic.main.activity_main.bt2
import kotlinx.android.synthetic.main.activity_main.bt3

class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnimationEx.init(this.application, 200, 1, ImageBezierNode::class.java, ImageDouNode::class.java)
        AnimationEx.registerNode(ImageDouNode::class.java)
        setContentView(LayoutInflater.from(this).inflate(R.layout.activity_main, null))
        bt1.setOnClickListener {
            val transactionTooLargeException = supportFragmentManager.beginTransaction()
            transactionTooLargeException.replace(R.id.fl, TestAnimCanvasFragment())
            transactionTooLargeException.addToBackStack("TestAnimCanvasFragment")
            transactionTooLargeException.commitAllowingStateLoss()
        }
        bt2.setOnClickListener {
            val transactionTooLargeException = supportFragmentManager.beginTransaction()
            transactionTooLargeException.replace(R.id.fl, TestAnimCanvasFragment2())
            transactionTooLargeException.addToBackStack("TestAnimCanvasFragment2")
            transactionTooLargeException.commitAllowingStateLoss()
        }
        bt3.setOnClickListener {
            val transactionTooLargeException = supportFragmentManager.beginTransaction()
            transactionTooLargeException.replace(R.id.fl, TestAnimCanvasFragment3())
            transactionTooLargeException.addToBackStack("TestAnimCanvasFragment3")
            transactionTooLargeException.commitAllowingStateLoss()
        }
    }
}