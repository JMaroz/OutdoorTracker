package com.marozzi.tracker.outdoor.sdk.features.provider

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*

@SuppressLint("MissingPermission")
class GoogleLocationProvider(context: Context, var listener: GpsLocationProviderInterface.OnGpsLocationProviderListener? = null) :
    GpsLocationProviderInterface {

    private var lastLocation: Location? = null

    private val locationProvider: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationRequest: LocationRequest = LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(GpsLocationFactory.GPS_UPDATE_INTERVAL)
            .setFastestInterval(GpsLocationFactory.GPS_UPDATE_INTERVAL)
            .setMaxWaitTime(GpsLocationFactory.GPS_UPDATE_INTERVAL_MAX_WAIT_TIME)
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            p0?.let { result ->
                result.lastLocation?.let {
                    lastLocation = it
                    listener?.onLocationChanged(it)
                }
            }
        }
    }

    override fun start() : Boolean {
        locationProvider.requestLocationUpdates(locationRequest, locationCallback, null)
        return true
    }

    override fun stop() : Boolean {
        locationProvider.removeLocationUpdates(locationCallback)
        return true
    }

    override fun getLastLocation(): Location? = lastLocation

    override fun fetchLastKnowLocation(): LiveData<Location?> {
        val result: MutableLiveData<Location> = MutableLiveData()
        locationProvider.lastLocation.addOnSuccessListener {
            result.value = it
        }
        return result
    }
}