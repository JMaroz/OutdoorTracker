package com.marozzi.tracker.outdoor.sdk.features.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.marozzi.tracker.outdoor.sdk.features.utils.GeoidHeightEstimator
import com.marozzi.tracker.outdoor.sdk.features.utils.KalmanFilter
import kotlin.math.roundToInt

/**
 * Created by amarozzi on 2020-07-19
 */
class AndroidLocationProvider(val context: Context, var listener: GpsLocationProviderInterface.OnGpsLocationProviderListener? = null) :
    GpsLocationProviderInterface {

    companion object {

        private const val TAG = "AndroidLocationProvider"
    }

    private var lastLocation: Location? = null

    private val kalmanFilter = KalmanFilter()
    private val geoidHeightEstimator = GeoidHeightEstimator.getInstance(context)

    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            location?.let {
                if (it.hasAltitude()) {
                    val geoidCm = geoidHeightEstimator.computeGeoidHeight(it.latitude, it.longitude)
                    it.altitude = it.altitude - (geoidCm / 100f).roundToInt()
                }

                if (kalmanFilter.isFirstPoint) {
                    Log.d(TAG, "FirstPoint of KalmanFilter, return")
                    kalmanFilter.filterData(it)
                    return
                }

                lastLocation = kalmanFilter.filterData(it)

                listener?.onLocationChanged(lastLocation!!)
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String?) {
        }

        override fun onProviderDisabled(provider: String?) {
        }
    }

    override fun getLastLocation(): Location? = lastLocation

    override fun start(): Boolean {
        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    GpsLocationFactory.GPS_UPDATE_INTERVAL,
                    GpsLocationFactory.GPS_DISTANCE_INTERVAL, locationListener)
            } catch (e:Exception) {
                Log.e(TAG, "Unable to start", e)
            }
            true
        } else {
            false
        }
    }

    override fun stop(): Boolean {
        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener)
            true
        } else {
            false
        }
    }

    override fun fetchLastKnowLocation(): LiveData<Location?> {
        val result: MutableLiveData<Location> = MutableLiveData()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            result.value = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else {
            result.value = null
        }
        return result
    }
}