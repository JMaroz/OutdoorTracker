package com.marozzi.tracker.outdoor.sdk.features.provider

import android.Manifest
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData

/**
 * Created by amarozzi on 2020-07-19
 */
interface GpsLocationProviderInterface {

    interface OnGpsLocationProviderListener {

        fun onLocationChanged(location: Location)
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun start() : Boolean

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun stop() : Boolean

    /**
     * Get the last location received
     */
    fun getLastLocation(): Location?

    /**
     * Request the last know location
     */
    fun fetchLastKnowLocation(): LiveData<Location?>

}