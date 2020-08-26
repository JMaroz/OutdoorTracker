package com.marozzi.tracker.outdoor.sdk.data.local.model


/**
 * This class contain the main information about the Activity Tracking
 * @param userWeight the weight of the user, needed for calculate the calories
 * @param type the type of the activity, walking, cycling needed for calculate the calories
 * @param hrDeviceAddress the mac address of the heart rate device where fetch the heart rate value
 * @param startTime the timestamp when the activity was started
 * @param pauseTime the timestamp when the activity was paused
 * @param pauseDuration the delta from the paused time and the current time
 * @param paused true if the activity is suspended, false otherwise
 * @param status the status of the activity
 * Created by amarozzi on 2020-05-24
 */
data class GpsActivityConfig(
        val userWeight: Double,
        val type: GpsActivityType,
        var hrDeviceAddress: String = "",
        var startTime: Long = 0,
        var pauseTime: Long = 0,
        var pauseDuration: Long = 0,
        var paused: Boolean = false,
        var status: GpsActivityStatus = GpsActivityStatus.ENDED
)