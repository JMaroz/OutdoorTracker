package com.marozzi.tracker.outdoor.sdk.features.provider

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import com.marozzi.tracker.outdoor.sdk.features.provider.GpsLocationFactory.LocationProviderType.Google
import com.marozzi.tracker.outdoor.sdk.features.provider.GpsLocationFactory.LocationProviderType.TGAndroid

/**
 * Created by amarozzi on 2020-07-18
 */
@SuppressLint("MissingPermission")
class GpsLocationFactory(
    context: Context,
    private val type: LocationProviderType,
    var listener: GpsLocationProviderInterface.OnGpsLocationProviderListener? = null
) {

    companion object {

        /**
         * Update interval from Google Location
         */
        const val GPS_UPDATE_INTERVAL = 1000L

        /**
         * Update interval from Google Location
         */
        const val GPS_DISTANCE_INTERVAL = 1f

        /**
         * Max wait time for location update from Google Location
         */
        const val GPS_UPDATE_INTERVAL_MAX_WAIT_TIME = 3000L

        private const val TAG = "GpsLocationFactory"

    }

    enum class LocationProviderType {
        Google,
        TGAndroid
    }

    private var androidLocation: AndroidLocationProvider? = null
    private var googleLocation: GoogleLocationProvider? = null

    init {
        when (type) {
            TGAndroid -> androidLocation = AndroidLocationProvider(context, listener)
            Google -> googleLocation = GoogleLocationProvider(context, listener)
        }
    }

    fun getLastLocation(): LiveData<Location?> {
        return when (type) {
            Google -> {
                googleLocation!!.fetchLastKnowLocation()
            }
            TGAndroid -> {
                androidLocation!!.fetchLastKnowLocation()
            }
        }
    }

    fun start() {
        Log.d(TAG, "start $type")
        when (type) {
            Google -> {
                googleLocation!!.start()
            }
            TGAndroid -> {
                androidLocation!!.start()
            }
        }
    }

    fun stop() {
        Log.d(TAG, "stop $type")
        when (type) {
            Google -> {
                googleLocation!!.stop()
            }
            TGAndroid -> {
                androidLocation!!.stop()
            }
        }
    }
}