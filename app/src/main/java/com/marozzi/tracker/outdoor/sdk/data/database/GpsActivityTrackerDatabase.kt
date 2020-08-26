package com.marozzi.tracker.outdoor.sdk.data.database

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.marozzi.tracker.outdoor.sdk.data.local.model.GpsActivity
import com.marozzi.tracker.outdoor.sdk.data.local.model.*
import java.util.*

/**
 * Created by amarozzi on 2019-05-20
 */
@Database(
    entities = [
        GpsActivity::class,
        GpsActivityPoint::class,
        GpsActivityHeartRatePoint::class
    ], version = 4, exportSchema = false
)
@TypeConverters(GpsTrackerConverter::class)
abstract class GpsTrackerDatabase : RoomDatabase() {

    companion object {
        @JvmStatic
        @Volatile
        private var instance: GpsTrackerDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): GpsTrackerDatabase = instance ?: synchronized(this) {
            return Room.databaseBuilder(context.applicationContext, GpsTrackerDatabase::class.java, GpsTrackerDatabase.javaClass.simpleName)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }

    abstract fun gpsTracker(): GpsTrackerDao

    @Dao
    abstract class GpsTrackerDao {

        /**
         * Add the user profile to the database
         */
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract fun addOrUpdateGpsActivity(gpsActivity: GpsActivity)

        @Query("SELECT * FROM GpsActivityPoint WHERE activityId = :activityId")
        abstract fun getActivityPoints(activityId: String): List<GpsActivityPoint>

        @Query("SELECT * FROM GpsActivityPoint WHERE activityId = :activityId ORDER BY timestamp LIMIT 1")
        abstract fun getLastActivityPoint(activityId: String): GpsActivityPoint?

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract fun addPoint(point: GpsActivityPoint)

        @Query("SELECT * FROM GpsActivityPoint WHERE activityId = :activityId ORDER BY timestamp LIMIT :count")
        abstract fun getLastActivityPoints(activityId: String, count: Int): List<GpsActivityPoint>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract fun addHearRateValue(heartRatePoint: GpsActivityHeartRatePoint)

        @Query("SELECT * FROM GpsActivity WHERE id = :id")
        abstract fun getActivity(id: String): GpsActivity?

        @Query("SELECT * FROM GpsActivityHeartRatePoint WHERE activityId = :activityId")
        abstract fun getActivityHearRatePoints(activityId: String): List<GpsActivityHeartRatePoint>

        @Query("DELETE FROM GpsActivity WHERE id = :activityId")
        abstract fun deleteActivity(activityId: String)

        @Query("DELETE FROM GpsActivityPoint WHERE activityId = :activityId")
        abstract fun deleteActivityPoints(activityId: String)

        @Query("DELETE FROM GpsActivityHeartRatePoint WHERE activityId = :activityId")
        abstract fun deleteActivityHeartRatePoints(activityId: String)

        @Query("SELECT * FROM GpsActivity WHERE serverId = :serverId")
        abstract fun getActivityByServerId(serverId: String): GpsActivity?

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract fun addPoints(points: List<GpsActivityPoint>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract fun addHearRatePoints(heartRatePoints: List<GpsActivityHeartRatePoint>)

        @Query("SELECT * FROM GpsActivity WHERE serverId = null OR serverId = ''")
        abstract fun getActivityToSync(): List<GpsActivity>

        @Query("SELECT * FROM GpsActivity ORDER BY createDate DESC LIMIT 1")
        abstract fun getLastActivityRunning(): GpsActivity?
    }
}

private class GpsTrackerConverter {

    @TypeConverter
    fun fromGpsActivityType(type: GpsActivityType?): String? = type?.toString()

    @TypeConverter
    fun toGpsActivityType(type: String?): GpsActivityType? =
        if (type.isNullOrBlank()) null else GpsActivityType.valueOf(type)

    @TypeConverter
    fun fromGpsActivityConfig(type: GpsActivityConfig?): String? = Gson().toJson(type)

    @TypeConverter
    fun toGpsActivityConfig(value: String?): GpsActivityConfig? {
        val listType = object : TypeToken<GpsActivityConfig>() {

        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return if (timestamp == null) null else Date(timestamp)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromString(value: String): MutableList<String> {
        val listType = object : TypeToken<MutableList<String>>() {

        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: MutableList<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringInt(value: String): MutableList<Int> {
        val listType = object : TypeToken<MutableList<Int>>() {

        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayListInt(list: MutableList<Int>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromJavaMap(value: LinkedHashMap<String, LinkedList<String>>?): String? = if (value == null) null else Gson().toJson(value)

    @TypeConverter
    fun toJavaMap(value: String?): LinkedHashMap<String, LinkedList<String>>? {
        val listType = object : TypeToken<LinkedHashMap<String, LinkedList<String>>>() {

        }.type
        return if (value.isNullOrBlank()) null else Gson().fromJson(value, listType)
    }
}