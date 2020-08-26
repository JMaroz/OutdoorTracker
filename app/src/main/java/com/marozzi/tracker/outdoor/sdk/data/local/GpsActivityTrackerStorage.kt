package com.marozzi.tracker.outdoor.sdk.data.local

import android.content.Context
import androidx.annotation.WorkerThread
import com.marozzi.tracker.outdoor.sdk.data.database.GpsTrackerDatabase
import com.marozzi.tracker.outdoor.sdk.data.local.model.GpsActivity
import com.marozzi.tracker.outdoor.sdk.data.local.model.GpsActivityHeartRatePoint
import com.marozzi.tracker.outdoor.sdk.data.local.model.GpsActivityPoint

/**
 * Created by amarozzi on 2020-05-20
 */
class GpsActivityTrackerStorage(context: Context) {

    private val database = GpsTrackerDatabase.getInstance(context)

    @WorkerThread
    fun clear() {
        database.clearAllTables()
    }

    @WorkerThread
    fun addOrUpdateActivity(activity: GpsActivity) = database.gpsTracker().addOrUpdateGpsActivity(activity)

    @WorkerThread
    fun getActivityPoints(id: String): List<GpsActivityPoint> = database.gpsTracker().getActivityPoints(id)

    @WorkerThread
    fun addPoint(point: GpsActivityPoint) = database.gpsTracker().addPoint(point)

    @WorkerThread
    fun getLastActivityPoints(activityId: String, count: Int): List<GpsActivityPoint> = database.gpsTracker().getLastActivityPoints(activityId, count)

    @WorkerThread
    fun addHearRatePoint(heartRatePoint: GpsActivityHeartRatePoint) = database.gpsTracker().addHearRateValue(heartRatePoint)

    @WorkerThread
    fun getActivity(id: String): GpsActivity? = database.gpsTracker().getActivity(id)

    @WorkerThread
    fun getActivityHearRatePoints(activityId: String): List<GpsActivityHeartRatePoint> = database.gpsTracker().getActivityHearRatePoints(activityId)

    @WorkerThread
    fun deleteActivity(activityId: String) {
        database.gpsTracker().run {
            deleteActivity(activityId)
            deleteActivityPoints(activityId)
            deleteActivityHeartRatePoints(activityId)
        }
    }

    @WorkerThread
    fun getActivityToSync(): List<GpsActivity> = database.gpsTracker().getActivityToSync()

    fun getLastActivity(): GpsActivity? = database.gpsTracker().getLastActivityRunning()

}