package com.xenia.ticket.utils.common

import android.content.*
import android.os.*
import android.util.Log

class PlutusServiceManager(
    private val context: Context,
    private val callback: (String) -> Unit
) {

    private var serverMessenger: Messenger? = null
    private var isBound = false

    private val incomingHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val response = msg.data.getString(PlutusConstants.RESPONSE_TAG)
            response?.let { callback(it) }
        }
    }

    private val clientMessenger = Messenger(incomingHandler)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serverMessenger = Messenger(service)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serverMessenger = null
            isBound = false
        }
    }

    fun bindService() {
        val intent = Intent().apply {
            action = PlutusConstants.PLUTUS_ACTION
            setPackage(PlutusConstants.PLUTUS_PACKAGE)
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    fun sendRequest(json: String) {
        if (!isBound) return

        val msg = Message.obtain(null, PlutusConstants.MESSAGE_CODE)
        val bundle = Bundle()
        bundle.putString(PlutusConstants.REQUEST_TAG, json)
        msg.data = bundle
        msg.replyTo = clientMessenger

        serverMessenger?.send(msg)
    }
}
