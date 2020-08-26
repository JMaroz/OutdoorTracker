package com.marozzi.tracker.outdoor.sdk.features.provider

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.marozzi.btle.heartrate.HeartRateDevicesManager

/**
 * Created by amarozzi on 2020-08-05
 */
class HRProvider(var listener: OnHRProviderListener? = null) {

    companion object {
        private const val TAG = "HRProvider"
    }

    var lastHRValue: Int = 0
    private var address: String = ""
    private val heartRateListener =
        object : HeartRateDevicesManager.SimpleHeartRateDevicesManagerListener() {

            override fun onHeartRateDeviceDisconnected(device: BluetoothDevice) {
                if (address.isNotBlank())
                    connect()
            }

            override fun onHeartRateDeviceValueChange(device: BluetoothDevice, value: Int) {
                if (device.address == address) {
                    Log.d(TAG, "received new hr value $value, save it")
                    lastHRValue = value
                    listener?.onHRValueChanged(value)
                }
            }
        }

    interface OnHRProviderListener {

        fun onHRValueChanged(value: Int)
    }

    fun start(context: Context, address: String) {
        Log.d(TAG, "start connecting to HeartRateDevice $address")

        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.d(TAG, "unable to connect to HeartRateDevice because $address is not a valid bluetooth address")
            return
        }

        this.address = address
        HeartRateDevicesManager
            .init(context.applicationContext, true)
            .addListener(heartRateListener)
        connect()
    }

    private fun connect() {
        HeartRateDevicesManager.connectTo(address)
    }

    fun stop() {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.d(TAG, "unable to stop because $address is not a valid bluetooth address")
            return
        }

        HeartRateDevicesManager
            .removeListener(heartRateListener)
            .disconnect(address)

        address = ""
        lastHRValue = 0
    }
}