package com.marozzi.tracker.outdoor.sdk.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * Created by amarozzi on 2020-07-12
 */
@Entity(tableName = "GpsActivityHeartRatePoint")
data class GpsActivityHeartRatePoint(
        @PrimaryKey var id: String = UUID.randomUUID().toString(),
        @ColumnInfo(index = true) var activityId: String,
        var heartRate: Int,
        val timestamp: Long = Calendar.getInstance().timeInMillis
)