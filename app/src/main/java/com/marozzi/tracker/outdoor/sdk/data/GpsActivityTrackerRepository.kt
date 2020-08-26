package com.marozzi.tracker.outdoor.sdk.data

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.marozzi.tracker.outdoor.sdk.data.local.GpsActivityTrackerStorage
import com.marozzi.tracker.outdoor.sdk.data.local.model.GpsActivity
import com.marozzi.tracker.outdoor.sdk.data.local.model.GpsActivityConfig
import com.marozzi.tracker.outdoor.sdk.data.local.model.GpsActivityHeartRatePoint
import com.marozzi.tracker.outdoor.sdk.data.local.model.GpsActivityPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by amarozzi on 2020-05-20
 */
class GpsActivityTrackerRepository(private val storage: GpsActivityTrackerStorage) {

    fun startNewActivity(config: GpsActivityConfig): GpsActivity {
        val activity = GpsActivity(config = config)
        CoroutineScope(Dispatchers.IO).launch {
            storage.addOrUpdateActivity(activity)
        }
        return activity
    }

    @WorkerThread
    fun updateActivity(activity: GpsActivity) {
        storage.addOrUpdateActivity(activity)
    }

    @WorkerThread
    fun getActivity(id: String): GpsActivity? {
        return storage.getActivity(id)
    }

    @WorkerThread
    fun getActivityPoints(activityId: String): List<GpsActivityPoint> {
        return storage.getActivityPoints(activityId)
    }

    @MainThread
    fun getActivityPointsAsync(activityId: String): LiveData<List<GpsActivityPoint>> {
        val result = MutableLiveData<List<GpsActivityPoint>>()
        CoroutineScope(Dispatchers.IO).launch {
            result.postValue(getActivityPoints(activityId))
        }
        return result
    }

    @WorkerThread
    fun addPoint(point: GpsActivityPoint) {
        storage.addPoint(point)
    }

    @WorkerThread
    fun getLastActivityPoints(activityId: String, count: Int): List<GpsActivityPoint> {
        return storage.getLastActivityPoints(activityId, count)
    }

    @MainThread
    fun addHearRateValue(heartRatePoint: GpsActivityHeartRatePoint) {
        CoroutineScope(Dispatchers.IO).launch {
            storage.addHearRatePoint(heartRatePoint)
        }
    }

    @WorkerThread
    fun getActivityHearRatePoints(activityId: String): List<GpsActivityHeartRatePoint> {
        return storage.getActivityHearRatePoints(activityId)
    }

    @MainThread
    fun getLastActivity(): LiveData<GpsActivity> {
        val result = MutableLiveData<GpsActivity>()
        CoroutineScope(Dispatchers.IO).launch {
            result.postValue(storage.getLastActivity())
        }
        return result
    }
}