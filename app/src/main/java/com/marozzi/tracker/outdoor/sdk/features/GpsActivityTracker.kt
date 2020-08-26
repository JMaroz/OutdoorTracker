package com.marozzi.tracker.outdoor.sdk.features

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.marozzi.tracker.outdoor.sdk.data.GpsActivityTrackerRepository
import com.marozzi.tracker.outdoor.sdk.data.local.GpsActivityTrackerStorage
import com.marozzi.tracker.outdoor.sdk.data.local.model.*
import com.marozzi.tracker.outdoor.sdk.features.provider.GpsLocationFactory
import com.marozzi.tracker.outdoor.sdk.features.provider.GpsLocationProviderInterface
import com.marozzi.tracker.outdoor.sdk.features.provider.HRProvider
import com.marozzi.tracker.outdoor.sdk.features.utils.Formula
import com.marozzi.tracker.outdoor.sdk.features.utils.formatDate
import com.marozzi.tracker.outdoor.sdk.features.utils.GpsTrackerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs


/**
 * Created by amarozzi on 2020-05-20
 */
class GpsActivityTracker(context: Context) {

    companion object {

        private const val TAG = "GpsActivityTracker"

        /**
         * Min time in mills for store location updates
         */
        private const val GPS_POINT_MIN_TIME = 3000L

        /**
         * Min distance in meters for store location update
         */
        private const val DELTA_DISTANCE_FILTER = 1000 //in meters

        const val INTENT_ACTION = "com.marozzi.tracker.outdoor.data.features.GpsActivityTracker"
        const val INTENT_ACTIVITY_STATUS = "activityStatus"
        const val INTENT_ACTIVITY_TYPE = "activityType"

        @JvmStatic
        @Volatile
        private var instance: GpsActivityTracker? = null

        @JvmStatic
        fun getInstance(): GpsActivityTracker? = instance

        @JvmStatic
        fun createNewInstance(context: Context): GpsActivityTracker {
            synchronized(this) {
                return GpsActivityTracker(context.applicationContext).also { instance = it }
            }
        }
    }

    sealed class GpsTrackingObserver {

        class GpsTrackingStatusChange(val activity: GpsActivity) : GpsTrackingObserver()

        class GpsTrackingPointReceived(val point: GpsActivityPoint) : GpsTrackingObserver()

        class GpsTrackingHRReceived(val value: Int) : GpsTrackingObserver()
    }

    interface GpsTrackingListener {

        fun onGpsTrackingStatusChange(activity: GpsActivity)

        fun onGpsTrackingPointReceived(point: GpsActivityPoint)

        fun onGpsTrackingHRReceived(value: Int)
    }

    private var context: WeakReference<Context> = WeakReference(context)
    private val repository = GpsActivityTrackerRepository(GpsActivityTrackerStorage(context))

    private val handler = Handler(Looper.getMainLooper())
    private val handlerCallback: Runnable = Runnable {
        sendUpdate()
    }

    private var activity: GpsActivity? = null
    private var lastPoint: GpsActivityPoint? = null

    private val locationProvider = GpsLocationFactory(
        context,
        GpsLocationFactory.LocationProviderType.TGAndroid,
        object : GpsLocationProviderInterface.OnGpsLocationProviderListener {
            override fun onLocationChanged(location: Location) {
                onLocationUpdated(location)
            }
        })

    private val hrProvider = HRProvider(object : HRProvider.OnHRProviderListener {
        override fun onHRValueChanged(value: Int) {
            activity?.config?.let {
                if (it.status == GpsActivityStatus.STARTED) {
                    Log.d(TAG, "received new hr value $value, save it")
                    repository.addHearRateValue(
                        GpsActivityHeartRatePoint(
                            activityId = activity!!.id,
                            heartRate = value
                        )
                    )
                }
            }
        }
    })

    private var gpsTrackingListener: GpsTrackingListener? = null
    private var gpsTrackingObserver: MutableLiveData<GpsTrackingObserver> = MutableLiveData()


    private val mutex = Mutex()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    @WorkerThread
    private fun saveActivity() {
        require(activity != null) { "GpsActivity is not initialized, call initNewActivityTracking first" }

        Log.d(TAG, "saveActivity $activity")
        activity!!.updateDate = Calendar.getInstance().time
        repository.updateActivity(activity!!)
    }

    private fun notifyActivityUpdate(activity: GpsActivity) {
        runOnUI {
            gpsTrackingObserver.value = GpsTrackingObserver.GpsTrackingStatusChange(activity.copy())
            gpsTrackingListener?.onGpsTrackingStatusChange(activity.copy())
            context.get()?.sendBroadcast(
                Intent(INTENT_ACTION)
                    .putExtra(INTENT_ACTIVITY_STATUS, activity.config.status.name)
                    .putExtra(INTENT_ACTIVITY_TYPE, activity.config.type.name)
            )
        }
    }

    @WorkerThread
    private fun storePoint(point: GpsActivityPoint) {
        Log.d(TAG, "storePoint $point")
        repository.addPoint(point)
        lastPoint = point.copy()
    }

    fun getLastLocation(): LiveData<Location?> {
        return locationProvider.getLastLocation()
    }

    fun initNewActivityTracking(userWeight: Double, activityType: GpsActivityType) {
        gpsTrackingObserver.value = null
        activity = repository.startNewActivity(
            GpsActivityConfig(
                userWeight = userWeight,
                type = activityType
            )
        )
    }

    fun setPreviousActivityTracking(activity: GpsActivity) {
        require(activity.config.status != GpsActivityStatus.ENDED) { "You can't set an ended activity tracking, start a new one calling initNewActivityTracking" }
        this.activity = activity
    }

    private fun clearActivityTracking() {
        activity = null
        lastPoint = null
    }

    fun getActivity(): GpsActivity? = activity

    fun getLastPoint(): GpsActivityPoint? = lastPoint

    fun registerForGpsActivityTrackerUpdate(): LiveData<GpsTrackingObserver> = gpsTrackingObserver

    fun connectToHeartRateDevices(context: Context, address: String) {
        hrProvider.start(context, address)
    }

    fun setGpsTrackingListener(listener: GpsTrackingListener?) {
        this.gpsTrackingListener = listener
    }

    /**
     * Indicate if the tracker has a current activity set for tracking the user actvity
     */
    fun hasActivityRunning(): Boolean {
        return activity != null && activity?.config?.status != GpsActivityStatus.ENDED
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startOrResumeTracking() {
        require(activity != null) { "GpsActivity is not initialized, call initNewActivityTracking first" }
        Log.d(TAG, "startOrResumeTracking")
        runOnIO {
            val activity = activity!!

            // calculate pause duration
            val startTime = System.currentTimeMillis()
            val pauseTime = activity.config.pauseTime
            if (pauseTime != 0L) {
                // resume from pause
                var pauseDuration = activity.config.pauseDuration
                pauseDuration += startTime - pauseTime
                activity.config.pauseDuration = pauseDuration
            } else {
                // first start
                if (activity.config.startTime == 0L)
                    activity.config.startTime = startTime
            }

            activity.config.pauseTime = 0L
            activity.config.status = GpsActivityStatus.STARTED

            saveActivity()
            runOnUI {
                locationProvider.start()
                handler.postDelayed(handlerCallback, 1000L)
            }
            notifyActivityUpdate(activity.copy())
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun pauseTracking() {
        require(activity != null) { "GpsActivity is not initialized, call initNewActivityTracking first" }
        Log.d(TAG, "pauseTracking")
        locationProvider.stop()
        handler.removeCallbacks(handlerCallback)
        runOnIO {
            activity!!.run {
                config.status = GpsActivityStatus.PAUSED
                config.paused = true
                config.pauseTime = System.currentTimeMillis()
                saveActivity()
                notifyActivityUpdate(copy())
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun stopTracking() {
        require(activity != null) { "GpsActivity is not initialized, call initNewActivityTracking first" }
        Log.d(TAG, "stopTracking")
        locationProvider.stop()
        hrProvider.stop()
        handler.removeCallbacks(handlerCallback)
        runOnIO {
            val activity = activity!!

            val pauseTime = activity.config.pauseTime
            if (pauseTime != 0L) {
                // resume from pause
                var pauseDuration = activity.config.pauseDuration
                pauseDuration += System.currentTimeMillis() - pauseTime
                activity.config.pauseDuration = pauseDuration
            }

            activity.duration =
                System.currentTimeMillis() - activity.config.startTime - activity.config.pauseDuration

            val points = repository.getActivityPoints(activity.id)
            if (points.isNotEmpty()) {
                val lastPoint = points.last()
                activity.avgSpeed = if (activity.duration > 0 && lastPoint.distance > 0) {
                    (lastPoint.distance.toFloat() / TimeUnit.MILLISECONDS.toSeconds(activity.duration))
                } else {
                    0.0f
                }
                activity.calories = lastPoint.calories.toInt()
                activity.distance = lastPoint.distance
            }

            activity.config.status = GpsActivityStatus.ENDED

            saveActivity()
            notifyActivityUpdate(activity.copy(config = activity.config.copy()))
            clearActivityTracking()
        }
    }

    private fun onLocationUpdated(location: Location) {
        runOnUI {
            try {
                Log.d(TAG, "time ${Date().formatDate("ss")} onLocationUpdated $location")

                val activity = this.activity ?: return@runOnUI

                if (lastPoint == null) {
                    Log.d(TAG, "no point stored, add this")
                    val point = GpsActivityPoint(
                        activityId = activity.id,
                        timestamp = location.time,
                        duration = 0L,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        distance = 0,
                        speed = 0.0f,
                        calories = 0.0
                    )
                    storePoint(point)
                } else {
                    val lastPoint = lastPoint!!

                    val deltaDuration = location.time - lastPoint.timestamp
                    if (deltaDuration < GPS_POINT_MIN_TIME) {
                        Log.d(TAG, "duration is under $GPS_POINT_MIN_TIME, return")
                        return@runOnUI
                    }

                    if (!activity.config.paused) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            lastPoint.latitude,
                            lastPoint.longitude,
                            location.latitude,
                            location.longitude,
                            results
                        )
                        val deltaDistance = results[0].toInt()

                        val duration: Long = lastPoint.duration + deltaDuration
                        val distance: Int = lastPoint.distance + abs(deltaDistance)
                        val calories: Double

                        val deltaDurationSec = TimeUnit.MILLISECONDS.toSeconds(deltaDuration)
                            .toInt()    //deltaDuration [sec]
                        val speedMs = deltaDistance.toFloat() / deltaDurationSec.toFloat()
                        val speedKmH = Formula.convertSpeedFromMsToKmh(speedMs.toDouble())               // speed [km/h]

                        val weight = activity.config.userWeight

                        // update distance, calories of this point
                        if (GpsActivityType.BICYCLE == activity.config.type) {
                            val deltaHeight = location.altitude - lastPoint.altitude
                            val power = getPower(deltaDistance, deltaHeight, weight, speedMs);
                            val deltaCalories = Formula.CyclingFormula.calculateCaloriesForBike(power, deltaDurationSec)
                            val mets = Formula.calculateMetsFromCaloriesAndDuration(
                                deltaCalories,
                                deltaDurationSec,
                                weight
                            )

                            calories = lastPoint.calories + deltaCalories
                        } else {
                            val vo2 = Formula.WalkingFormula.calculateVO2Ascm(speedKmH, 0.0)
                            val mets = Formula.calculateMets(vo2)
                            val deltaCalories = Formula.calculateCalories(mets, deltaDurationSec, weight)

                            calories = lastPoint.calories + deltaCalories
                        }

                        val newPoint = GpsActivityPoint(
                            activityId = activity.id,
                            timestamp = location.time,
                            duration = duration,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            distance = distance,
                            speed = speedMs,
                            calories = calories
                        )

                        val lastPoints = repository.getLastActivityPoints(activity.id, 2)
                        val haveToAdd = GpsTrackerUtils.haveToAdd(lastPoints[1], lastPoints[0], GpsActivityPoint(latitude = location.latitude, longitude = location.longitude))
                        val point = if (lastPoints.size == 2 && haveToAdd) {
                            Log.d(TAG, "lastPoints.size == 2 && GpsTrackerUtils.haveToAdd")
                            newPoint
                        } else if (lastPoints.size < 2 || deltaDistance >= DELTA_DISTANCE_FILTER) {
                            Log.d(TAG, "lastPoints.size < 2 || deltaDistance >= DELTA_DISTANCE_FILTER")
                            newPoint
                        } else {
                            Log.d(TAG, "Update last gps path point because we are along a straight line")
                            newPoint.apply {
                                id = lastPoint.id
                            }
                        }
                        storePoint(point)
                    } else {
                        Log.d(TAG, "Coming from pause, store new point with data from last point and coordinate from new point")
                        val point = GpsActivityPoint(
                            activityId = activity.id,
                            timestamp = location.time,
                            duration = lastPoint.duration,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            distance = lastPoint.distance,
                            speed = lastPoint.speed,
                            calories = lastPoint.calories
                        )
                        storePoint(point)
                        activity.config.paused = false
                        saveActivity()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun sendUpdate() {
        require(activity != null) { "GpsActivity is not initialized, call initNewActivityTracking first" }

        val activity = activity!!

        val startTime = activity.config.startTime
        var pauseDuration = activity.config.pauseDuration
        val pauseTime = activity.config.pauseTime
        if (pauseTime != 0L) {
            pauseDuration += System.currentTimeMillis() - pauseTime
        }

        val duration = System.currentTimeMillis() - startTime - pauseDuration

        val point = if (lastPoint != null) {
            val avgSpeed =
                if (duration > 0 && lastPoint!!.distance > 0) lastPoint!!.distance.toDouble() / TimeUnit.MILLISECONDS.toSeconds(
                    duration
                ) else 0.0
            lastPoint!!.copy(duration = duration, speed = avgSpeed.toFloat())
        } else {
            GpsActivityPoint(
                activityId = activity.id,
                timestamp = Calendar.getInstance().timeInMillis,
                duration = duration,
                distance = 0,
                speed = 0.0f,
                calories = 0.0
            )
        }

        gpsTrackingListener?.onGpsTrackingPointReceived(point)
        gpsTrackingObserver.value = GpsTrackingObserver.GpsTrackingPointReceived(point)

        gpsTrackingListener?.onGpsTrackingHRReceived(hrProvider.lastHRValue)
        gpsTrackingObserver.value =
            GpsTrackingObserver.GpsTrackingHRReceived(hrProvider.lastHRValue)

        handler.postDelayed(handlerCallback, 1000L)
    }

    private fun getPower(deltaDistance: Int, deltaHeight: Double, weight: Double, speedMs: Float): Double {
        var slope: Double
        if (deltaDistance > 0 || deltaDistance < 0) {
            slope = deltaHeight / deltaDistance
            if (slope > 0.33)
                slope = 0.33
        } else {
            slope = 0.0
        }

        // Avoid calculating power while descending
        return if (slope < 0) {
            if (slope < -0.01)
                0.0
            else
                Formula.CyclingFormula.calculatePowerStep(weight, 0.0, speedMs.toDouble())
        } else {
            Formula.CyclingFormula.calculatePowerStep(weight, slope, speedMs.toDouble())
        }
    }

    private inline fun runOnUI(crossinline action: () -> Unit) {
        uiScope.launch {
            mutex.withLock {
                action()
            }
        }
    }

    private inline fun runOnIO(crossinline action: () -> Unit) {
        ioScope.launch {
            mutex.withLock {
                action()
            }
        }
    }
}