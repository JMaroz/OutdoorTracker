package com.marozzi.tracker.outdoor.sdk.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * All the data of the activity performed by the user
 * @author amarozzi
 */
@Entity(tableName = "GpsActivity")
data class GpsActivity(
        @PrimaryKey var id: String = UUID.randomUUID().toString(),
        @ColumnInfo(index = true) var serverId: String = "",
        var createDate: Date = Date(),
        var updateDate: Date = Date(),
        var duration: Long = 0L,
        var distance: Int = 0,
        var avgSpeed: Float = 0.0f,
        var calories: Int = 0,
        val config: GpsActivityConfig
)