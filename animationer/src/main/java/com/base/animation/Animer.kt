package com.base.animation

import android.os.Process
import com.base.animation.log.DefaultLog
import com.base.animation.log.ILog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * @author:zhouzechao
 * @date: 2/8/21
 * description：
 */
const val AnimThreadName: String = "anim_%d"
const val CalculationThreadName: String = "calculation_%d"

object Animer {
    var log: ILog = DefaultLog()

    val animDisplayId = AtomicLong(System.currentTimeMillis() / 1000L)
    val exceptionHandler by lazy {
        CoroutineExceptionHandler { context, throwable ->
            log.e("CoroutineException", "Coroutine exception occurred. $context", throwable)
        }
    }


    val animThreadFactory = object : ThreadFactory {
        private val mThreadId = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            val t = Thread({
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY)
                } catch (t: Throwable) {
                    // ignore
                }
                r.run()
            }, String.format(AnimThreadName, mThreadId.getAndIncrement()))
            return t
        }
    }

    val calculationThreadFactory = object : ThreadFactory {
        private val mThreadId = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            val t = Thread({
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY)
                } catch (t: Throwable) {
                    // ignore
                }
                r.run()
            }, String.format(CalculationThreadName, mThreadId.getAndIncrement()))
            return t
        }
    }

    fun initLog(iLog: ILog) {
        log = iLog
    }

    val calculationDispatcher by lazy {
        ThreadPoolExecutor(
            1, 1, 1000L, TimeUnit.MILLISECONDS, LinkedBlockingDeque(), calculationThreadFactory
        ).asCoroutineDispatcher()
    }
}