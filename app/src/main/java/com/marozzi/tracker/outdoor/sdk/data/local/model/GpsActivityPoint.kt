package com.marozzi.tracker.outdoor.sdk.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import java.util.*

/**
 * Single point tracked during the activity
 * @author amarozzi
 */
@Entity(tableName = "GpsActivityPoint")
data class GpsActivityPoint(
        @PrimaryKey var id: String = UUID.randomUUID().toString(),
        @ColumnInfo(index = true) var activityId: String? = null,
        var timestamp: Long = 0L,
        var duration: Long = 0L,
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var altitude: Double = 0.0,
        var distance: Int = 0,
        var speed: Float = 0.0f,
        var calories: Double = 0.0
) {

    fun toLatLng() = LatLng(latitude, longitude)
}
