package com.yhc.xp.three.finger.screenshoter.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import de.robv.android.xposed.XposedBridge
import java.util.Arrays
import java.util.function.Consumer
import java.util.stream.Collectors

class BroadcastMediator internal constructor() {
    fun interface Receiver {
        fun onBroadcastReceived(context: Context?, intent: Intent?)
    }

    private class Subscriber internal constructor(
        var receiver: Receiver,
        var actions: List<String?>
    )

    private var mContext: Context? = null
    private val mSubscribers: MutableList<Subscriber> = ArrayList()
    private val mIntentFilter: IntentFilter = IntentFilter()
    private var mInternalReceiverRegistered = false
    var context: Context?
        get() {
            if (DEBUG) log("Send context")
            return if (mContext != null) {
                mContext
            } else {
                null
            }
        }
        set(context) {
            if (DEBUG) log("Received context")
            mContext = context
            if (mIntentFilter.countActions() > 0) {
                registerReceiverInternal()
            }
        }

    /**
     * Subscribes receiver to receive broadcasts represented by actions of interest
     * @param receiver - listener for receiving broadcast
     * @param actions - actions of interest
     */
    fun subscribe(receiver: Receiver, actions: List<String?>) {
        synchronized(mSubscribers) {
            val oldActionCount = mIntentFilter.countActions()
            for (action in actions) {
                if (!mIntentFilter.hasAction(action)) {
                    mIntentFilter.addAction(action)
                }
            }
            mSubscribers.add(Subscriber(receiver, actions))
            if (DEBUG) log("subscribing receiver: $receiver")
            if (oldActionCount != mIntentFilter.countActions()) {
                registerReceiverInternal()
            }
        }
    }

    private fun registerReceiverInternal() {
        if (mContext == null) return
        if (mInternalReceiverRegistered) {
            mContext!!.unregisterReceiver(mReceiverInternal)
            mInternalReceiverRegistered = false
            if (DEBUG) log("reisterReceiverInternal: old internal receiver unregistered")
        }
        mContext!!.registerReceiver(mReceiverInternal, mIntentFilter)
        mInternalReceiverRegistered = true
        if (DEBUG) log("reisterReceiverInternal: new internal receiver registered")
    }

    /**
     * Subscribes receiver to receive broadcasts represented by actions of interest
     * @param receiver - to receive broadcast
     * @param actions - actions of interest
     */
    fun subscribe(receiver: Receiver, vararg actions: String?) {
        subscribe(receiver, Arrays.asList(*actions))
    }

    /**
     * Unsubscribes receiver
     * @param receiver - receiver to unsubscribe
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun unsubscribe(receiver: Receiver) {
        if (DEBUG) log("unsubscribing receiver: $receiver")
        synchronized(mSubscribers) {
            val toRemove = mSubscribers.stream()
                .filter { s: Subscriber -> s.receiver === receiver }
                .collect(Collectors.toList())
            if (!toRemove.isEmpty()) {
                mSubscribers.removeAll(toRemove)
            }
        }
    }

    private val mReceiverInternal: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onReceive(context: Context, intent: Intent) {
            synchronized(mSubscribers) {
                val toNotify = mSubscribers.stream()
                    .filter { s: Subscriber -> s.actions.contains(intent.action) }
                    .map { s: Subscriber -> s.receiver }
                    .collect(Collectors.toList())
                toNotify.forEach(Consumer { r: Receiver ->
                    if (DEBUG) log(
                        "Notifying listener: " + r +
                                "; action=" + intent.action
                    )
                    r.onBroadcastReceived(context, intent)
                })
            }
        }
    }

    init {
        if (DEBUG) log("BroadcastMediator created")
    }

    companion object {
        const val TAG = "Screenshoter:BroadcastMediator"
        private const val DEBUG = false
        private fun log(msg: String) {
            XposedBridge.log(TAG + ": " + msg)
        }
    }
}